package com.ylaneo.chatserverweb.perf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * chat-server-web 的并发 / 性能基线压测器。
 *
 * <p>这个类的唯一目的：<b>在改造之前，把系统的真实状态用数据摆出来</b>。
 * 没有基线数据，后面所有的"优化"都只是感觉。
 *
 * <p>它同时也是一个<b>故障复现器</b>，能稳定复现三个已知缺陷：
 * <ol>
 *   <li><b>A1 拆包/截断</b>：服务端用 {@code byte[65535]} 单次 read()，超过 64KB 的请求被静默截断。
 *       截断后解析时 {@code p.get(5)} 越界，被 catch 兜住后返回通用的「服务器内部错误」，
 *       真实原因（内容超长）被伪装掉了。见 {@link #runLargeMode(Config)}。</li>
 *   <li><b>A2 双写线程串包</b>：服务端心跳线程写 "pong" 时<b>没有加锁</b>，
 *       而业务推送线程写 &lt;push&gt; 时是 {@code synchronized(socket)} 加锁的 ——
 *       锁只保护了推送自己，心跳线程根本不在这个锁的范围内。
 *       两条写路径并发写同一个 TCP 流，客户端一次 read() 就可能同时读到 "pong" 和 "&lt;push&gt;..."，
 *       真实桌面端认不出这种消息就<b>静默丢弃</b>（连日志都没有）。
 *       见 {@link #runLongMode(Config)}。</li>
 *   <li><b>B3 线程数失控</b>：每个长连接占用 1 个常驻线程（HeartbeatHandler）。
 *       注意：在<b>当前重构后的版本</b>里，一个连接只占 <b>1 个</b>线程，不是 2 个 ——
 *       因为 {@code LegacyClientHandler.dispatch()} 在长连接分支返回后，
 *       {@code legacy-client-*} 线程就退出了，只有 HeartbeatHandler 一直守着。
 *       （原始 chat-server 的 ServerThread 是自己 while 循环读心跳的，所以才占 2 个。）
 *       <p>
 *       真正的隐患在于 {@code Executors.newCachedThreadPool()} 是<b>无界</b>的：
 *       短连接每来一个就临时占一个线程，线程数会跟着并发数无上限地涨。
 *       见 {@link ThreadSampler} 的实时曲线。</li>
 * </ol>
 *
 * <p><b>⚠ 长连接模式的架构约束（这个坑踩过）</b>：
 * 真实桌面端用的是<b>两条独立的 socket</b> ——
 * 长连接只负责「发心跳 + 收推送」，短连接负责「所有业务请求」。
 * 服务端一旦收到 {@code <registerPushSocket>}，就会把 socket 交给
 * {@code HeartbeatHandler} 独占读取，该线程只认 "ping" 和 "close"，<b>其他内容一律丢弃</b>。
 * <p>
 * 所以本类里，<b>业务消息必须走短连接</b>。如果图省事在长连接上直接发
 * {@code <sendMessage>}，服务端根本不会处理，推送也不会触发，A2 根本无法复现 ——
 * 那样的压测数据是假的。
 *
 * <p><b>前置条件</b>：MySQL 与 chat-server-web 服务端都要已启动（服务端监听 20001 端口）。
 *
 * <p><b>⚠ 心跳必须跑在独立线程上（这个坑踩过，导致 A2 一直测不出来）</b>：
 * 真实桌面端的心跳在 {@code HeartbeatSender} 线程，业务消息在 {@code RequestSender} 线程，
 * 两个节奏完全独立。服务端那边：
 * <ul>
 *   <li>{@code HeartbeatHandler} 收到 "ping" → <b>无锁</b>地写 "pong"</li>
 *   <li>{@code LegacyPushNotifier.pushTo()} → <b>加锁</b>地写 "&lt;push&gt;..."</li>
 * </ul>
 * 锁只保护了推送自己，心跳线程根本不在锁范围内 —— 这两条路径并发写同一个 TCP 流，
 * 客户端一次 read() 就可能同时读到 "pong" 和 "&lt;push&gt;..."，真实桌面端认不出就静默丢弃。
 * <p>
 * 之前心跳和消息写在同一个 for 循环里，而 {@code sendOneMessage()} 是阻塞的
 * （新建 TCP 连接 + 2 个新建的物理 MySQL 连接 + 等响应 ≈ 24ms），
 * 每次迭代都被消息拖住，心跳定时器根本轮不到 ——
 * 结果心跳次数 ≈ 消息次数，节奏完全锁死，<b>压根没机会撞车</b>，A2 永远是 0。
 * 那个 0 的含义是「没测到」，不是「没问题」。现在心跳已拆成独立线程（lt-hb-*）。
 * <p>
 * 压测器现在会显式打印「心跳有效性」（心跳回包数 vs 消息数），
 * 如果心跳回包数远少于消息数，会直接判定<b>本次数据无效</b>并提示重跑 ——
 * 不会再让你拿一个「没测到」的结果当成「没问题」的结论。
 *
 * <p><b>怎么跑</b>（三种方式，任选其一）：
 * <pre>
 * ① IDE：直接运行本类的 main 方法（推荐，参数在下方 CONFIG 常量里改）
 * ② Maven：
 *     mvn -pl chat-server-web test-compile
 *     mvn -pl chat-server-web exec:java -Dexec.classpathScope=test ^
 *         -Dexec.mainClass=com.ylaneo.chatserverweb.perf.LoadTest ^
 *         -Dexec.args="--mode all --threads 50 --iterations 20 --long-conns 20"
 * ③ 打包成 jar 后：java -cp chat-server-web/target/test-classes;chat-server-web/target/classes ... LoadTest
 * </pre>
 *
 * <p><b>注意</b>：本类位于 src/test/java 下，名字以 Test 结尾，会被 Maven Surefire 当作单元测试扫描。
 * 已在 chat-server-web/pom.xml 中通过 surefire 的 excludes 排除掉，不会影响 {@code mvn test}。
 */
public class LoadTest {

    // =====================================================================
    // 一、全局配置（默认值，命令行参数可覆盖）
    // =====================================================================

    /** 服务端 Legacy TCP 端口，对应 LegacyTcpServer.DEFAULT_PORT。 */
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 20001;

    /** 服务端主类名，用 jps 匹配进程 pid（jcmd 采样线程数需要）。 */
    public static final String DEFAULT_SERVER_MAIN = "com.ylaneo.chatserverweb.ChatServerWebApplication";

    /** MySQL 连接信息（与 application.properties 保持一致），用于采样活跃连接数。 */
    public static final String DEFAULT_MYSQL_URL =
            "jdbc:mysql://localhost:3306/chat_demo?serverTimezone=UTC&useSSL=false";
    public static final String DEFAULT_MYSQL_USER = "root";
    public static final String DEFAULT_MYSQL_PASSWORD = "123456";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // =====================================================================
    // 二、入口
    // =====================================================================

    public static void main(String[] args) throws Exception {
        Config cfg = Config.parse(args);

        printBanner(cfg);

        if (!preflight(cfg)) {
            return;
        }

        // 采样器：线程数（jcmd）+ MySQL 活跃连接数，整个测试期间持续采样
        ThreadSampler threadSampler = ThreadSampler.start(cfg);
        MySqlSampler mySqlSampler = MySqlSampler.start(cfg);

        try {
            switch (cfg.mode) {
                case "short" -> runShortMode(cfg);
                case "long" -> runLongMode(cfg);
                case "mixed" -> runMixedMode(cfg);
                case "large" -> runLargeMode(cfg);
                case "all" -> {
                    runShortMode(cfg);
                    runLargeMode(cfg);
                    runLongMode(cfg);
                }
                default -> System.out.println("未知模式: " + cfg.mode);
            }
        } finally {
            threadSampler.close();
            mySqlSampler.close();
        }

        System.out.println();
        System.out.println("全部完成。服务端线程 / MySQL 连接的最终采样值：");
        System.out.println("  " + threadSampler.summary());
        System.out.println("  " + mySqlSampler.summary());
    }

    // =====================================================================
    // 三、前置检查：确认服务端真的活着，并展示一次真实交互
    // =====================================================================

    private static boolean preflight(Config cfg) {
        System.out.println("【前置检查】连接 " + cfg.host + ":" + cfg.port + " ...");
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(cfg.host, cfg.port), 3000);
            s.setSoTimeout(5000);
            OutputStream out = s.getOutputStream();
            out.write(cfg.probeRequest().getBytes(StandardCharsets.UTF_8));
            out.flush();
            String resp = readUntilEof(s.getInputStream(), 64 * 1024);
            System.out.println("  服务端在线。样例请求: " + cfg.probeRequest());
            System.out.println("  样例响应: " + abbreviate(resp, 200));
            if (resp.isEmpty()) {
                System.out.println("  [!] 响应为空 —— 服务端可能抛异常了，请查看服务端日志。");
            }
            return true;
        } catch (IOException e) {
            System.out.println("  [X] 无法连接服务端: " + e.getMessage());
            System.out.println("      请确认 chat-server-web 已启动（mvn -pl chat-server-web spring-boot:run）。");
            return false;
        }
    }

    // =====================================================================
    // 四、模式一：短连接压测 —— 测服务端的处理能力与数据库连接压力
    // =====================================================================

    /**
     * 短连接压测：N 个线程并发循环做「发请求 → 读响应 → 关闭连接」。
     *
     * <p>预期能看到的现象：
     * <ul>
     *   <li>MySQL 活跃连接数 ≈ QPS —— 因为 DAO 每次查询都 new 一次物理连接（没有用 HikariCP）</li>
     *   <li>服务端线程数不会暴涨（短连接用完就释放）</li>
     * </ul>
     */
    private static void runShortMode(Config cfg) {
        header("模式一：短连接压测 (short)");

        LatencyRecorder latency = new LatencyRecorder(cfg.maxSamples);
        AtomicLong okCount = new AtomicLong();
        AtomicLong failCount = new AtomicLong();
        Map<String, AtomicInteger> errorTypes = new ConcurrentHashMap<>();

        long start = System.nanoTime();
        ExecutorService pool = Executors.newFixedThreadPool(cfg.threads);
        CountDownLatch latch = new CountDownLatch(cfg.threads);

        for (int t = 0; t < cfg.threads; t++) {
            pool.submit(() -> {
                try {
                    for (int i = 0; i < cfg.iterations; i++) {
                        // 1) 建连
                        long t0 = System.nanoTime();
                        try (Socket s = new Socket()) {
                            s.connect(new InetSocketAddress(cfg.host, cfg.port), 3000);
                            s.setSoTimeout(cfg.soTimeoutMs);
                            s.setTcpNoDelay(true);

                            OutputStream out = s.getOutputStream();
                            out.write(cfg.probeRequest().getBytes(StandardCharsets.UTF_8));
                            out.flush();

                            String resp = readUntilEof(s.getInputStream(), 256 * 1024);
                            latency.record(System.nanoTime() - t0);

                            if (resp.isEmpty()) {
                                failCount.incrementAndGet();
                                errorTypes.computeIfAbsent("空响应(服务端异常退出)", k -> new AtomicInteger()).incrementAndGet();
                            } else if (!resp.startsWith("<0>")) {
                                // 必须按业务码判定。只判断 startsWith("<") 会把
                                // "<1><用户不存在>" 这类错误响应也算成成功 —— 成功率会虚高到 100%，
                                // 基线数据就废了。业务成功码是 0（见 Result.success）。
                                failCount.incrementAndGet();
                                errorTypes.computeIfAbsent("业务返回非成功: " + abbreviate(resp, 60),
                                        k -> new AtomicInteger()).incrementAndGet();
                            } else {
                                okCount.incrementAndGet();
                            }
                        } catch (IOException e) {
                            failCount.incrementAndGet();
                            latency.record(System.nanoTime() - t0);
                            errorTypes.computeIfAbsent(e.getClass().getSimpleName() + ": " + e.getMessage(),
                                    k -> new AtomicInteger()).incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // 超时必须给足：默认 50 线程 × 20 次 = 1000 次请求，
        // 每次都要新建 TCP 连接 + 走一次 SQL，机器慢的时候 300ms 根本不够，
        // 一旦超时 latch 没走完，统计出来的「总请求数」就会偏小，
        // 基线数据（QPS、延迟分位）全部失真 —— 这比超时本身更糟。
        awaitQuietly(latch, Math.max(60_000, cfg.threads * cfg.iterations * 200L));
        pool.shutdown();
        long elapsedNanos = System.nanoTime() - start;
        double elapsedSec = elapsedNanos / 1_000_000_000.0;

        long total = okCount.get() + failCount.get();
        printLatencyReport(elapsedSec, total, okCount.get(), failCount.get(), latency);
        printErrorBreakdown(errorTypes);
    }

    // =====================================================================
    // 五、模式二：长连接压测 —— 复现 A2 串包 + 观察 B3 线程数
    // =====================================================================

    /**
     * 长连接压测：模拟 N 个桌面端在线用户。
     *
     * <p><b>每个虚拟用户用两条独立的 socket（与真实桌面端一致）</b>：
     * <ol>
     *   <li><b>长连接 A</b>（推送通道）：发 {@code <registerPushSocket>} 建立长连接，
     *       服务端为它启动一个 HeartbeatHandler 线程。
     *       后台读线程持续接收服务端推送；前台循环发心跳 "ping"
     *       （服务端回 "pong"，走<b>没有加锁</b>的写入路径）。</li>
     *   <li><b>短连接 B</b>（业务通道）：每次发消息都 {@code new Socket()} → 发 {@code <sendMessage>}
     *       → 读响应 → {@code close()}。服务端在 {@code sendMessage} 成功后，
     *       会通过 {@code pushNotifier.pushTo()} 向接收方的<b>长连接 A</b> 推送
     *       {@code <push><messageAdd>...}（走<b>加了锁</b>的写入路径）。</li>
     * </ol>
     *
     * <p><b>A2 串包就是在这两条写入路径撞车时发生的</b>：
     * 服务端心跳线程写 "pong" 不加锁，推送线程写 "&lt;push&gt;" 加锁，
     * 但这个锁<b>只保护了推送自己</b>，心跳线程不在锁范围内。
     * 两条路径并发写同一个 TCP 流 → 客户端一次 read() 可能同时读到 "pong" 和 "&lt;push&gt;..."。
     *
     * <p><b>丢消息的判据</b>（与真实桌面端 PushListener 的行为严格对齐）：
     * 真实端只认三种形态 —— 精确等于 "ping"、精确等于 "pong"、以 "&lt;push&gt;" 开头。
     * <b>其余形态全部被静默丢弃，连一行错误日志都不会有。</b>
     * 所以凡是"含 &lt;push&gt; 但不以 &lt;push&gt; 开头"的内容，在真实场景下就是一条丢失的消息。
     */
    private static void runLongMode(Config cfg) {
        header("模式二：长连接压测 (long) —— 复现 A2 串包 + 观察 B3 线程数");

        int conns = cfg.longConns;
        AtomicLong pushCount = new AtomicLong();          // 正常收到的 <push> 推送
        AtomicLong corruptedCount = new AtomicLong();      // 被心跳污染的推送（真实端会丢）
        AtomicLong fragmentCount = new AtomicLong();       // 被拆开的半截消息（真实端也会丢）
        AtomicLong heartbeatCount = new AtomicLong();      // 收到的 ping / pong
        AtomicLong unknownCount = new AtomicLong();        // 既不是心跳也不是推送的垃圾
        AtomicLong errorCount = new AtomicLong();
        AtomicLong sentCount = new AtomicLong();           // 实际发出的业务消息数
        AtomicLong sendFailCount = new AtomicLong();       // 业务消息发送失败数
        Map<String, AtomicInteger> errorTypes = new ConcurrentHashMap<>();
        List<String> samples = Collections.synchronizedList(new ArrayList<>());

        CountDownLatch latch = new CountDownLatch(conns);
        ExecutorService pool = Executors.newFixedThreadPool(conns * 2);

        for (int c = 0; c < conns; c++) {
            final int userId = cfg.baseUserId + c;
            // 每个用户都和"下一个用户"互发消息，形成环形，确保证务端一定有推送目标
            final int peerId = cfg.baseUserId + ((c + 1) % Math.max(1, conns));

            pool.submit(() -> {
                Socket pushSocket = null;
                Thread reader = null;
                Thread hbThread = null;
                try {
                    // ============================================================
                    // 通道 A：长连接（推送 + 心跳）
                    // ============================================================
                    pushSocket = new Socket();
                    pushSocket.connect(new InetSocketAddress(cfg.host, cfg.port), 3000);
                    pushSocket.setTcpNoDelay(true);
                    final OutputStream pushOut = pushSocket.getOutputStream();

                    // 1) 注册长连接
                    pushOut.write(("<registerPushSocket><" + userId + ">").getBytes(StandardCharsets.UTF_8));
                    pushOut.flush();

                    // 2) 读线程：专职接收推送（socket 的输入流只能有一个读者）
                    final Socket sock = pushSocket;
                    reader = new Thread(() -> {
                        try {
                            InputStream is = sock.getInputStream();
                            byte[] buf = new byte[64 * 1024];
                            while (!Thread.currentThread().isInterrupted()) {
                                int n = is.read(buf);
                                if (n <= 0) break;
                                String chunk = new String(buf, 0, n, StandardCharsets.UTF_8).trim();
                                classifyChunk(chunk, pushCount, corruptedCount, fragmentCount,
                                        heartbeatCount, unknownCount, samples);
                            }
                        } catch (IOException e) {
                            // 测试结束时我们主动 close(socket)，读线程被唤醒后抛异常是预期行为，
                            // 不能算成 IO 异常（否则每个虚拟用户都会虚增 1 次错误，误导结论）。
                            if (!sock.isClosed()) {
                                errorCount.incrementAndGet();
                                errorTypes.computeIfAbsent("读线程: " + e.getClass().getSimpleName() + ": " + e.getMessage(),
                                        k -> new AtomicInteger()).incrementAndGet();
                            }
                        }
                    }, "lt-reader-" + userId);
                    reader.setDaemon(true);
                    reader.start();

                    // ============================================================
                    // 通道 A'：心跳独立线程 —— 必须对齐真实桌面端的 HeartbeatSender
                    // ============================================================
                    // 【为什么必须拆成独立线程】
                    // 真实桌面端里，心跳在 HeartbeatSender 线程、业务消息在 RequestSender 线程，
                    // 两个线程的节奏完全独立。服务端那边：
                    //   · HeartbeatHandler 收到 "ping" → 无锁地写 "pong"
                    //   · LegacyPushNotifier.pushTo() → 加锁地写 "<push>..."
                    // 这两条写入路径互相独立，才会撞出 A2。
                    //
                    // 【之前踩的坑】
                    // 心跳和消息曾经写在同一个 for 循环里。问题是 sendOneMessage() 是阻塞的
                    // （新建 TCP 连接 + 2 个新建的物理 MySQL 连接 + 等响应 ≈ 24ms），
                    // 每次迭代都被消息拖住，10ms 的 ping 定时器根本轮不到。
                    // 结果：心跳次数 ≈ 消息次数，两者节奏完全锁死，压根没机会撞车，
                    //       A2 永远是 0 —— 这个 0 是「没测到」，不是「没问题」。
                    final AtomicBoolean hbStopped = new AtomicBoolean(false);
                    Thread hb = new Thread(() -> {
                        while (!hbStopped.get() && !sock.isClosed()
                                && !Thread.currentThread().isInterrupted()) {
                            try {
                                pushOut.write("ping".getBytes(StandardCharsets.UTF_8));
                                pushOut.flush();
                            } catch (IOException e) {
                                // 只记一次就直接退出线程：否则连接断开瞬间会刷出成百上千条同样的错误，
                                // 把「IO 异常」这个指标冲高到失去意义。
                                if (!sock.isClosed()) {
                                    errorCount.incrementAndGet();
                                    errorTypes.computeIfAbsent("心跳写: " + e.getClass().getSimpleName() + ": " + e.getMessage(),
                                            k -> new AtomicInteger()).incrementAndGet();
                                }
                                return;
                            }
                            try {
                                Thread.sleep(cfg.pingIntervalMs);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                    }, "lt-hb-" + userId);
                    hb.setDaemon(true);
                    hb.start();
                    hbThread = hb;

                    // ============================================================
                    // 通道 B：短连接（业务消息）—— 与真实桌面端的 RequestSender 一致
                    // ============================================================
                    long deadline = System.currentTimeMillis() + cfg.durationMs;
                    long nextMsg = System.currentTimeMillis();
                    int seq = 0;
                    while (System.currentTimeMillis() < deadline && !sock.isClosed()) {
                        long now = System.currentTimeMillis();

                        // 3-1) 业务消息：走【短连接】。服务端 sendMessage 成功后
                        //      会向 peerId 的长连接推送 <push><messageAdd>（加锁写）
                        //      → 与心跳线程的 "pong" 写（无锁）撞车 → A2 串包
                        if (now >= nextMsg) {
                            sendOneMessage(cfg, userId, peerId, seq++, sentCount, sendFailCount, errorTypes);
                            nextMsg = now + cfg.msgIntervalMs;
                        }

                        Thread.sleep(1);
                    }
                    hbStopped.set(true);
                    hb.interrupt();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    errorTypes.computeIfAbsent(e.getClass().getSimpleName() + ": " + e.getMessage(),
                            k -> new AtomicInteger()).incrementAndGet();
                } finally {
                    if (hbThread != null) {
                        hbThread.interrupt();
                    }
                    if (reader != null) {
                        reader.interrupt();
                    }
                    if (pushSocket != null) {
                        try {
                            pushSocket.close();
                        } catch (IOException ignored) {
                            // 关闭失败不影响统计
                        }
                    }
                    latch.countDown();
                }
            });
        }

        awaitQuietly(latch, cfg.durationMs + 60_000);
        pool.shutdownNow();

        System.out.println();
        System.out.println("  长连接数        : " + conns);
        System.out.println("  业务消息发送数  : " + sentCount.get()
                + (sendFailCount.get() > 0 ? "  (失败 " + sendFailCount.get() + ")" : ""));
        System.out.println("  收到的推送总数  : " + pushCount.get());
        System.out.println("  其中被心跳污染  : " + corruptedCount.get() + "   <-- A2 串包，真实桌面端会静默丢弃");
        System.out.println("  被拆开的半截消息: " + fragmentCount.get() + "   <-- A1 拆包，真实桌面端会静默丢弃");
        System.out.println("  心跳回包(ping/pong): " + heartbeatCount.get());
        System.out.println("  无法识别的内容  : " + unknownCount.get());
        System.out.println("  IO 异常         : " + errorCount.get());
        // 关键健康度指标：如果心跳回包数远少于消息数，说明心跳线程没跑起来，
        // 这时 A2=0 是「没测到」而不是「没问题」。必须显式报出来，避免误判。
        long hb = heartbeatCount.get();
        long msg = sentCount.get();
        boolean heartbeatEffective = msg == 0 || hb >= msg / 2;
        System.out.println("  心跳有效性      : " + (heartbeatEffective ? "正常" : "**心跳线程疑似未生效**")
                + "  (心跳回包 " + hb + " vs 消息数 " + msg + ")");
        if (!errorTypes.isEmpty()) {
            printErrorBreakdown(errorTypes);
        }
        if (!samples.isEmpty()) {
            System.out.println("  异常样本（最多 5 条）:");
            synchronized (samples) {
                for (int i = 0; i < Math.min(5, samples.size()); i++) {
                    System.out.println("    - " + abbreviate(samples.get(i), 220));
                }
            }
        }
        System.out.println();
        if (corruptedCount.get() > 0 || fragmentCount.get() > 0) {
            System.out.println("  >>> 结论：已复现消息被污染/被截断的问题（A1 拆包 + A2 双写线程串包）。");
            System.out.println("  >>> 这就是「消息偶发丢失但没有任何异常日志」的根因。");
        } else if (sentCount.get() == 0) {
            System.out.println("  >>> 结论：一条业务消息都没发出去，本次数据无效。");
            System.out.println("  >>>      请检查服务端是否已启动、--user-id 指定的 userId 是否能正常 insert。");
        } else if (!heartbeatEffective) {
            System.out.println("  >>> 结论：**本次数据无效，不能据此认为 A2 不存在**。");
            System.out.println("  >>>      心跳回包数(" + hb + ") 远少于消息数(" + msg + ")，");
            System.out.println("  >>>      说明心跳线程没能有效运行 —— 没有心跳流量，就没有机会与推送撞车。");
            System.out.println("  >>>      请加大 --ping-interval 频率（如 1ms）并延长 --duration 后重跑。");
        } else {
            System.out.println("  >>> 结论：本次未复现消息污染。");
            System.out.println("  >>>      心跳线程已确认在运行（心跳回包 " + hb + "），");
            System.out.println("  >>>      说明本次心跳与推送没有撞上。");
            System.out.println("  >>>      建议进一步加大 --ping-interval 频率、--long-conns 数量并延长 --duration。");
        }
    }

    /**
     * 通过一条<b>短连接</b>发送一条业务消息（与真实桌面端 RequestSender.sendRequest() 行为一致）。
     *
     * <p>为什么必须用短连接：服务端收到 {@code <registerPushSocket>} 后，
     * socket 的输入流就被 HeartbeatHandler 独占，那条线程只认 "ping"/"close"，
     * 业务请求写在长连接上会被直接丢弃，服务端根本不会触发推送。
     *
     * @return true = 服务端返回了业务成功码
     */
    private static boolean sendOneMessage(Config cfg, int senderId, int receiverId, int seq,
                                         AtomicLong sentCount, AtomicLong failCount,
                                         Map<String, AtomicInteger> errorTypes) {
        String content = "lt-" + senderId + "-" + seq;
        String request = "<sendMessage><" + senderId + "><" + receiverId + "><0><"
                + content + "><0>";
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(cfg.host, cfg.port), 3000);
            s.setSoTimeout(cfg.soTimeoutMs);
            OutputStream out = s.getOutputStream();
            out.write(request.getBytes(StandardCharsets.UTF_8));
            out.flush();

            String resp = readUntilEof(s.getInputStream(), 64 * 1024);
            // 注意：sentCount 统计的是「**尝试**发送的消息数」，不是成功数。
            // 之前只在成功路径递增，导致「业务消息发送数」小于实际尝试数，
            // 而「收到的推送总数」是按实际成功数算的 —— 两个数字对不上，
            // 让人误以为推送丢了。口径必须统一为「尝试发送数」。
            sentCount.incrementAndGet();

            // 成功响应格式：<code><message><messageId>...，code 为 0
            boolean ok = resp.startsWith("<0>");
            if (!ok) {
                failCount.incrementAndGet();
                if (failCount.get() <= 3) {
                    errorTypes.computeIfAbsent("业务响应非成功码: " + abbreviate(resp, 80),
                            k -> new AtomicInteger()).incrementAndGet();
                }
            }
            return ok;
        } catch (IOException e) {
            // 连接就失败 = 这条消息根本没发出去，同样要计入尝试数
            sentCount.incrementAndGet();
            failCount.incrementAndGet();
            errorTypes.computeIfAbsent("短连接发送失败: " + e.getClass().getSimpleName() + ": " + e.getMessage(),
                    k -> new AtomicInteger()).incrementAndGet();
            return false;
        }
    }

    /**
     * 对服务端一次 write 到达客户端的内容做分类统计。
     *
     * <p><b>判据严格对齐真实桌面端 PushListener</b>：
     * <pre>
     *   if ("ping".equals(msg)) { handlePing(); continue; }   // 心跳
     *   if (msg.startsWith("&lt;push&gt;&lt;statusUpdate&gt;"))      ...
     *   else if (msg.startsWith("&lt;push&gt;&lt;friendListUpdate&gt;")) ...
     *   else if (msg.startsWith("&lt;push&gt;&lt;messageAdd&gt;"))      ...
     *   else { return; }   // ← 静默丢弃，连日志都没有
     * </pre>
     * 所以：只要一条消息不是以这三种前缀之一开头，真实场景下它就<b>彻底消失</b>。
     * 这正是「用户说消息偶尔丢了，但服务端日志里一切正常」的机制。
     */
    private static void classifyChunk(String chunk,
                                       AtomicLong pushCount,
                                       AtomicLong corruptedCount,
                                       AtomicLong fragmentCount,
                                       AtomicLong heartbeatCount,
                                       AtomicLong unknownCount,
                                       List<String> samples) {
        if (chunk.isEmpty()) {
            return;
        }
        // ---- 防御：长连接注册失败时，服务端会回一个错误响应并关闭 socket ----
        // 例如 <registerPushSocket> 处理过程中抛异常，route() 兜住后返回
        // "<1><服务器内部错误>"，dispatch() 把它写出去就关连接了。
        // 这类内容不是"消息被拆包"，不能算进 A1 的账上，否则会得出错误结论。
        if (chunk.startsWith("<1>") || chunk.contains("服务器内部错误")) {
            unknownCount.incrementAndGet();
            if (samples.size() < 20) samples.add("[长连接注册失败] " + chunk);
            return;
        }
        boolean hasPush = chunk.contains("<push>");
        boolean startsWithPush = chunk.startsWith("<push>");
        boolean isHeartbeat = "ping".equals(chunk) || "pong".equals(chunk);

        if (isHeartbeat) {
            heartbeatCount.incrementAndGet();
            return;
        }
        if (startsWithPush) {
            // 以 <push> 开头 —— 前缀对上了，还得看参数是否完整可解析。
            // 真实端解析完还要 Integer.valueOf(params.get(2)) / params.get(3)，
            // 只要尾部粘进了心跳的 "pong"，参数就变成 "2pong" → NumberFormatException
            // → 消息被静默丢弃。所以"前缀对"不等于"真实端收得下"。
            if (isWellFormedPush(chunk)) {
                pushCount.incrementAndGet();
            } else {
                // 真实端同样会静默丢弃（参数解析失败）
                corruptedCount.incrementAndGet();
                if (samples.size() < 20) samples.add(chunk);
            }
            return;
        }
        if (hasPush) {
            // 心跳和推送粘在了一起 —— 真实端会整条丢弃
            corruptedCount.incrementAndGet();
            if (samples.size() < 20) samples.add(chunk);
            return;
        }
        if (chunk.contains("<")) {
            // 消息被拆开了（收到的是半截，比如 "<push><mess" 和 "ageAdd><1><2>"）
            // 真实端同样认不出，静默丢弃
            fragmentCount.incrementAndGet();
            if (samples.size() < 20) samples.add(chunk);
            return;
        }
        unknownCount.incrementAndGet();
        if (samples.size() < 20) samples.add(chunk);
    }

    /**
     * 判断一条以 &lt;push&gt; 开头的报文，真实桌面端<b>能不能真的处理</b>。
     *
     * <p>真实端的处理链是「先按前缀分派，再解析参数，再转 Integer」，
     * 三步里有任何一步失败都会静默丢弃。所以判定标准必须走完这三步：
     * <pre>
     *   statusUpdate      → params.size()&gt;=4 且 params[2], params[3] 都是整数
     *   friendListUpdate → params.size()&gt;=3 且 params[2] 是整数
     *   messageAdd       → params.size()&gt;=4 且 params[2], params[3] 都是整数
     * </pre>
     *
     * <p>典型被误判为「正常」的例子：{@code <push><messageAdd><1><2>pong}
     * —— 前缀完全正确，但真实端解析出的 messageId 是 "2pong"，
     * {@code Integer.valueOf} 抛异常，消息<b>照样丢了</b>。
     * 这正是 A2 最常见的形态，不能漏计。
     */
    private static boolean isWellFormedPush(String chunk) {
        List<String> params = splitAngle(chunk);
        try {
            if (chunk.startsWith("<push><messageAdd>")) {
                if (params.size() < 4) return false;
                Integer.parseInt(params.get(2));
                Integer.parseInt(params.get(3));
                return true;
            }
            if (chunk.startsWith("<push><statusUpdate>")) {
                if (params.size() < 4) return false;
                Integer.parseInt(params.get(2));
                Integer.parseInt(params.get(3));
                return true;
            }
            if (chunk.startsWith("<push><friendListUpdate>")) {
                if (params.size() < 3) return false;
                Integer.parseInt(params.get(2));
                return true;
            }
        } catch (NumberFormatException e) {
            // 真实端在这里会 catch 掉并静默 return —— 对我们来说就是"这条消息丢了"
            return false;
        }
        return false;   // 未知前缀，真实端也会丢弃
    }

    // =====================================================================
    // 六、模式三：大报文测试 —— 复现 A1 截断
    // =====================================================================

    /**
     * 大报文测试：验证服务端单次 read(byte[65535]) 造成的报文截断（A1）。
     *
     * <p><b>先说清楚 A1 的真实机制</b>（这一点很容易想错）：
     * 服务端 {@code is.read(byte[65535])} 只能拿到 65535 字节，
     * 所以<b>只要请求总长超过 65535，尾部就会被丢掉</b>（包括结尾的 "&lt;0&gt;" 状态参数）。
     * 于是 {@code parser.handleParams()} 解析出来的参数个数不足，
     * 后面 {@code p.get(5)} 越界 → 抛 IndexOutOfBoundsException →
     * 被 route() 里的通用 catch 兜住 → 客户端只看到一个<b>毫无信息量的</b>「服务器内部错误」。
     *
     * <p><b>注意本表结构下不会发生"静默截断"</b>：
     * {@code chat_message.content} 是 {@code varchar(4096)}，
     * 截断后的内容仍然远超 4096 → insert 直接失败并返回「Data too long」这类原始 SQL 错误。
     * <p>
     * 也就是说：<b>只有在把 content 列改成 TEXT/更大时，A1 才会退化成真正的"静默截断"</b>
     * （服务端存进去的是半截，还返回"成功"）。当前 schema 下它表现为「报错，但报错信息误导」。
     * 本测试把这两种情况分开测，让你看到区别。
     *
     * <p>三个子场景：
     * <ul>
     *   <li><b>oversize</b>：请求总长 &gt; 65535 → 尾部被 read() 截掉 → 解析越界 → 「服务器内部错误」</li>
     *   <li><b>split</b>：分两次写入 → 服务端只读到前半段 → 同样解析越界 → 「服务器内部错误」</li>
     *   <li><b>over-col</b>：请求总长 &lt; 65535，但 content 超过 varchar(4096)
     *       → 读到的是完整报文，但 insert 失败 → 返回原始 SQL 错误「Data too long」</li>
     * </ul>
     */
    private static void runLargeMode(Config cfg) {
        header("模式三：大报文测试 (large) —— 复现 A1 截断/拆包");

        int payloadKb = cfg.payloadKb;
        String content = "A".repeat(payloadKb * 1024);
        String request = "<sendMessage><" + cfg.baseUserId + "><" + (cfg.baseUserId + 1)
                + "><0><" + content + "><0>";

        System.out.println("  发送内容长度    : " + content.length() + " 字节 (" + payloadKb + " KB)");
        System.out.println("  完整请求长度    : " + request.length() + " 字节");
        System.out.println("  服务端缓冲区    : 65535 字节 (byte[65535])");
        System.out.println("  content 列长度  : varchar(" + cfg.contentColLimit + ")");
        System.out.println();

        // --- 子场景 1：一次性写入，总长超过 65535 ---
        System.out.println("  [子场景 1] 一次性写入整个请求（总长 " + request.length()
                + " > 65535，尾部会被 read() 截掉）");
        long t0 = System.nanoTime();
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(cfg.host, cfg.port), 3000);
            s.setSoTimeout(30_000);
            OutputStream out = s.getOutputStream();
            out.write(request.getBytes(StandardCharsets.UTF_8));
            out.flush();

            String resp = readUntilEof(s.getInputStream(), 1024 * 1024);
            long cost = (System.nanoTime() - t0) / 1_000_000;
            System.out.println("    耗时        : " + cost + " ms");
            System.out.println("    响应前 200 字符: " + abbreviate(resp, 200));

            List<String> parts = splitAngle(resp);
            // buildSendMessage 实际拼 11 段：
            // code/message/messageId/senderId/receiverId/type/content/readAt/status/createTime/updateTime
            if (parts.size() < 11) {
                System.out.println("    响应参数个数: " + parts.size() + "（成功响应应有 11 个参数）");
                if (resp.contains("服务器内部错误")) {
                    System.out.println("    >>> 结论：服务端返回「服务器内部错误」—— 报文尾部被 read() 截断，"
                            + "解析参数越界后被通用 catch 兜住了。");
                    System.out.println("    >>> 真实原因（报文超长）只出现在服务端日志的堆栈里，客户端完全看不到。");
                    System.out.println("    >>> 这就是「用户只看到一句莫名其妙的话」的机制。");
                } else {
                    System.out.println("    >>> 结论：服务端返回了非预期响应，见上方输出。");
                }
            } else {
                String code = parts.get(0);
                String gotContent = parts.get(6);
                System.out.println("    业务返回码  : " + code);
                System.out.println("    收到的内容长度: " + gotContent.length() + " 字节 (应为 " + content.length() + ")");
                if (gotContent.length() < content.length()) {
                    System.out.println("    >>> 结论：内容被静默截断 " + (content.length() - gotContent.length())
                            + " 字节，且服务端返回了" + ("0".equals(code) ? "成功" : "错误") + "。");
                    System.out.println("    >>> 这就是「消息发出去但对端看到的是半截」的原因（A1）。");
                } else {
                    System.out.println("    >>> 本次未截断。");
                }
            }
        } catch (IOException e) {
            System.out.println("    [X] 失败: " + e.getMessage());
        }

        // --- 子场景 2：分两次写入 ---
        System.out.println();
        System.out.println("  [子场景 2] 分两次写入（中间间隔 " + cfg.splitDelayMs + " ms）");
        int half = request.length() / 2;
        // 分两次写：服务端第一次 read() 只拿到前半段 → 解析越界 → 被 catch 兜住
        // → 写回 "<1><服务器内部错误>" → 立刻 socket.close()。
        // 客户端此时还没写第二段，对端已经 RST 了 —— 所以我们经常**连错误响应都收不到**。
        // 这是比子场景 1 更糟的形态：故障表现为「网络断了」而不是「服务器报错」。
        boolean gotResponse = false;
        String resp2 = "";
        IOException writeError = null;
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(cfg.host, cfg.port), 3000);
            s.setSoTimeout(30_000);
            OutputStream out = s.getOutputStream();
            try {
                out.write(request.substring(0, half).getBytes(StandardCharsets.UTF_8));
                out.flush();
                Thread.sleep(cfg.splitDelayMs);
                out.write(request.substring(half).getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (IOException e) {
                // 第二段写失败 = 服务端已经因为解析越界而关闭了连接
                writeError = e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                writeError = new IOException("等待分片写入时被中断", e);
            }
            if (writeError == null) {
                resp2 = readUntilEof(s.getInputStream(), 1024 * 1024);
                gotResponse = true;
            }
        } catch (IOException e) {
            writeError = e;
        }

        if (writeError != null) {
            System.out.println("    [X] 写第二段时连接被中止: " + writeError.getMessage());
            System.out.println("    >>> 结论：服务端只读到前半段报文，解析越界后把「服务器内部错误」写出去，"
                    + "随即 close() 了 socket。");
            System.out.println("    >>> 客户端因此<b>连那句错误都收不到</b>，用户看到的是「网络连接被重置」。");
            System.out.println("    >>> 这比子场景 1 更隐蔽：同一个根因，用户看到的现象完全不同。");
        } else if (!gotResponse) {
            System.out.println("    >>> 结论：连接在读完响应前就断开了，未取得任何响应。");
        } else {
            System.out.println("    响应前 200 字符: " + abbreviate(resp2, 200));
            if (resp2.contains("服务器内部错误")) {
                System.out.println("    >>> 结论：服务端返回「服务器内部错误」。");
                System.out.println("    >>> 真实原因是报文只读到前半段，解析时数组越界，被 catch 兜住了 —— 故障被伪装成了通用错误。");
            } else {
                System.out.println("    >>> 结论：本次服务端未报错（响应前 200 字符见上）。");
            }
        }

        // --- 子场景 3：报文完整但 content 超过列长度限制 ---
        System.out.println();
        System.out.println("  [子场景 3] 报文完整（总长 < 65535），但 content 超过 varchar("
                + cfg.contentColLimit + ")");
        int overColLen = cfg.contentColLimit + 1024;
        String overColContent = "B".repeat(overColLen);
        String overColRequest = "<sendMessage><" + cfg.baseUserId + "><" + (cfg.baseUserId + 1)
                + "><0><" + overColContent + "><0>";
        System.out.println("    content 长度   : " + overColLen + " 字节 (超出列限制 "
                + (overColLen - cfg.contentColLimit) + " 字节)");
        System.out.println("    完整请求长度   : " + overColRequest.length() + " 字节 (< 65535，不会被 read 截断)");
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(cfg.host, cfg.port), 3000);
            s.setSoTimeout(30_000);
            OutputStream out = s.getOutputStream();
            out.write(overColRequest.getBytes(StandardCharsets.UTF_8));
            out.flush();

            String resp = readUntilEof(s.getInputStream(), 1024 * 1024);
            System.out.println("    响应前 200 字符: " + abbreviate(resp, 200));
            if (resp.startsWith("<0>")) {
                System.out.println("    >>> 结论：插入成功（列长度与预期不符，请检查表结构）。");
            } else {
                System.out.println("    >>> 结论：服务端返回了失败响应，错误信息直接来自 MySQL。");
                System.out.println("    >>> 这是「数据库的原始错误直接透传给用户」—— 用户看到的是 SQL 报错，而不是「消息内容过长」。");
            }
        } catch (Exception e) {
            System.out.println("    [X] 失败: " + e.getMessage());
        }
    }

    // =====================================================================
    // 七、模式四：混合压测（短连接 + 长连接同时）
    // =====================================================================

    private static void runMixedMode(Config cfg) {
        header("模式四：混合压测 (mixed) —— 短连接与长连接同时施压");

        Thread t1 = new Thread(() -> runShortMode(cfg), "mixed-short");
        Thread t2 = new Thread(() -> runLongMode(cfg), "mixed-long");
        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // =====================================================================
    // 八、报告输出
    // =====================================================================

    private static void printLatencyReport(double elapsedSec, long total, long ok, long fail,
                                           LatencyRecorder latency) {
        System.out.println();
        System.out.println("  ─────────────────────── 短连接压测结果 ───────────────────────");
        System.out.printf(Locale.ROOT, "  总耗时          : %.2f s%n", elapsedSec);
        System.out.printf(Locale.ROOT, "  总请求数        : %d  (成功 %d / 失败 %d)%n", total, ok, fail);
        System.out.printf(Locale.ROOT, "  成功率          : %.2f %%%n",
                total == 0 ? 0.0 : ok * 100.0 / total);
        System.out.printf(Locale.ROOT, "  吞吐量 (QPS)    : %.2f%n", elapsedSec == 0 ? 0 : total / elapsedSec);
        System.out.println("  延迟 (ms)       : " + latency.percentileReport());
        System.out.println("  ──────────────────────────────────────────────────────────────");
    }

    private static void printErrorBreakdown(Map<String, AtomicInteger> errorTypes) {
        if (errorTypes == null || errorTypes.isEmpty()) {
            return;
        }
        System.out.println("  失败原因分布:");
        errorTypes.entrySet().stream()
                .sorted((a, b) -> b.getValue().get() - a.getValue().get())
                .limit(10)
                .forEach(e -> System.out.printf(Locale.ROOT, "    %-60s %d 次%n",
                        abbreviate(e.getKey(), 60), e.getValue().get()));
    }

    // =====================================================================
    // 九、工具方法
    // =====================================================================

    /** 读到 EOF 为止（服务端写完就关连接）。 */
    private static String readUntilEof(InputStream is, int maxBytes) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int total = 0;
        while (true) {
            int n = is.read(buf);
            if (n < 0) break;
            bos.write(buf, 0, n);
            total += n;
            if (total >= maxBytes) break;
        }
        return bos.toString(StandardCharsets.UTF_8);
    }

    /** 按 '&lt;' 切分尖括号协议，取出各段（与服务端 LegacyRequestParser 同样的规则）。 */
    static List<String> splitAngle(String request) {
        List<String> params = new ArrayList<>();
        if (request == null) return params;
        for (String a : request.split("<")) {
            if (!a.isEmpty()) {
                params.add(a.substring(0, a.length() - 1));
            }
        }
        return params;
    }

    static String abbreviate(String s, int max) {
        if (s == null) return "null";
        String flat = s.replace("\r", "\\r").replace("\n", "\\n");
        return flat.length() <= max ? flat : flat.substring(0, max) + "...(共" + s.length() + "字符)";
    }

    private static void awaitQuietly(CountDownLatch latch, long timeoutMs) {
        try {
            if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                System.out.println("  [!] 等待超时，部分任务可能未完成。");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("======================================================================");
        System.out.println("  " + title);
        System.out.println("======================================================================");
    }

    private static void printBanner(Config cfg) {
        System.out.println("======================================================================");
        System.out.println("  chat-server-web  并发 / 性能基线压测器");
        System.out.println("======================================================================");
        System.out.printf(Locale.ROOT, "  目标            : %s:%d%n", cfg.host, cfg.port);
        System.out.println("  模式            : " + cfg.mode);
        System.out.println("  短连接线程数    : " + cfg.threads + "  x  " + cfg.iterations + " 次请求");
        System.out.println("  长连接数        : " + cfg.longConns + "  持续 " + (cfg.durationMs / 1000) + " 秒");
        System.out.println("  大报文          : " + cfg.payloadKb + " KB");
        System.out.println("  服务端 pid 探测 : " + (cfg.pid != null ? "已指定 " + cfg.pid : "通过 jps 自动匹配"));
        System.out.println("  当前时间        : " + LocalTime.now().format(TIME_FMT));
        System.out.println();
    }

    // =====================================================================
    // 十、配置
    // =====================================================================

    /** 可配置的压测参数，全部可通过命令行覆盖。 */
    public static class Config {
        String mode = "all";
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        int threads = 50;
        int iterations = 20;
        int longConns = 20;
        int durationMs = 30_000;
        int payloadKb = 100;
        // 心跳现在跑在独立线程上，节奏不再被业务消息拖住，
        // 所以 1ms 是可达的。默认值故意调得激进 —— 目的是尽快把 A2 撞出来。
        int pingIntervalMs = 1;
        int msgIntervalMs = 20;
        int splitDelayMs = 50;
        int soTimeoutMs = 15_000;
        int maxSamples = 500_000;
        int baseUserId = 1;
        /**
         * chat_message.content 列的长度限制。
         * 当前表结构是 varchar(4096)，所以「报文完整但 content 超长」会得到
         * MySQL 的 Data too long 错误，而不是静默截断。
         */
        int contentColLimit = 4096;
        String pid = null;                 // 服务端进程 pid，null = 自动用 jps 找
        String serverMain = DEFAULT_SERVER_MAIN;
        boolean jcmdEnabled = true;
        boolean mysqlEnabled = true;
        String mysqlUrl = DEFAULT_MYSQL_URL;
        String mysqlUser = DEFAULT_MYSQL_USER;
        String mysqlPassword = DEFAULT_MYSQL_PASSWORD;

        /**
         * 用来探测服务端是否在线的请求。
         * 默认用 &lt;getUserData&gt;：一定会返回结果、只打一条 SQL、不写库，最适合做纯读压测。
         */
        String probeRequest() {
            return "<getUserData><" + baseUserId + ">";
        }

        static Config parse(String[] args) {
            Config c = new Config();
            for (int i = 0; i < args.length; i++) {
                String a = args[i];
                switch (a) {
                    case "--mode" -> c.mode = args[++i];
                    case "--host" -> c.host = args[++i];
                    case "--port" -> c.port = Integer.parseInt(args[++i]);
                    case "--threads" -> c.threads = Integer.parseInt(args[++i]);
                    case "--iterations" -> c.iterations = Integer.parseInt(args[++i]);
                    case "--long-conns" -> c.longConns = Integer.parseInt(args[++i]);
                    case "--duration" -> c.durationMs = Integer.parseInt(args[++i]) * 1000;
                    case "--payload-kb" -> c.payloadKb = Integer.parseInt(args[++i]);
                    case "--ping-interval" -> c.pingIntervalMs = Integer.parseInt(args[++i]);
                    case "--msg-interval" -> c.msgIntervalMs = Integer.parseInt(args[++i]);
                    case "--split-delay" -> c.splitDelayMs = Integer.parseInt(args[++i]);
                    case "--user-id" -> c.baseUserId = Integer.parseInt(args[++i]);
                    case "--content-col-limit" -> c.contentColLimit = Integer.parseInt(args[++i]);
                    case "--pid" -> c.pid = args[++i];
                    case "--jcmd" -> c.jcmdEnabled = !"off".equalsIgnoreCase(args[++i]);
                    case "--mysql" -> c.mysqlEnabled = !"off".equalsIgnoreCase(args[++i]);
                    case "--mysql-url" -> c.mysqlUrl = args[++i];
                    case "--mysql-user" -> c.mysqlUser = args[++i];
                    case "--mysql-password" -> c.mysqlPassword = args[++i];
                    case "--help", "-h" -> {
                        printUsage();
                        System.exit(0);
                    }
                    default -> {
                        System.out.println("未知参数: " + a + "（用 --help 查看用法）");
                    }
                }
            }
            return c;
        }

        static void printUsage() {
            System.out.println("""
                    用法: LoadTest [选项]

                      --mode <short|long|mixed|large|all>   压测模式，默认 short
                      --host <ip>              服务端地址，默认 127.0.0.1
                      --port <port>            服务端端口，默认 20001
                      --threads <n>            短连接并发线程数，默认 50
                      --iterations <n>         每线程请求次数，默认 20
                      --long-conns <n>         长连接数量，默认 20
                      --duration <秒>          长连接持续时间，默认 30
                      --payload-kb <n>         大报文大小(KB)，默认 100
                      --ping-interval <ms>     心跳发送间隔，默认 1（独立线程，节奏不会被业务消息拖住）
                      --msg-interval <ms>      消息发送间隔，默认 20
                      --split-delay <ms>       分片写入间隔，默认 50
                      --user-id <id>           基准用户 id，默认 1
                      --content-col-limit <n>  chat_message.content 列长度，默认 4096
                      --pid <pid>              指定服务端 pid（默认用 jps 自动找）
                      --jcmd <on|off>          是否采样服务端线程数，默认 on
                      --mysql <on|off>         是否采样 MySQL 活跃连接数，默认 on
                      --mysql-url <url>        MySQL 连接串
                      --mysql-user <user>      MySQL 用户名
                      --mysql-password <pwd>   MySQL 密码

                    示例:
                      --mode all --threads 50 --iterations 200 --long-conns 50 --duration 60
                      --mode long --long-conns 50 --duration 60 --ping-interval 1 --msg-interval 5
                    """);
        }
    }

    // =====================================================================
    // 十一、延迟统计
    // =====================================================================

    /** 延迟记录器：记录所有样本并计算分位数。 */
    public static class LatencyRecorder {
        private final int capacity;
        private final long[] samples;
        private final AtomicInteger count = new AtomicInteger();
        private final LongAdder totalNanos = new LongAdder();

        LatencyRecorder(int capacity) {
            this.capacity = capacity;
            this.samples = new long[capacity];
        }

        void record(long nanos) {
            totalNanos.add(nanos);
            int i = count.getAndIncrement();
            if (i < capacity) {
                samples[i] = nanos;
            }
        }

        int size() {
            return Math.min(count.get(), capacity);
        }

        boolean truncated() {
            return count.get() > capacity;
        }

        String percentileReport() {
            int n = size();
            if (n == 0) {
                return "无样本";
            }
            long[] copy = new long[n];
            System.arraycopy(samples, 0, copy, 0, n);
            java.util.Arrays.sort(copy);
            double avgMs = totalNanos.sum() / 1_000_000.0 / count.get();
            return String.format(Locale.ROOT,
                    "avg=%.2f  p50=%.2f  p90=%.2f  p95=%.2f  p99=%.2f  max=%.2f%s",
                    avgMs,
                    pct(copy, 0.50), pct(copy, 0.90), pct(copy, 0.95), pct(copy, 0.99),
                    copy[n - 1] / 1_000_000.0,
                    truncated() ? "  (样本已截断)" : "");
        }

        private static double pct(long[] sorted, double p) {
            if (sorted.length == 0) return 0;
            int idx = (int) Math.ceil(p * sorted.length) - 1;
            idx = Math.max(0, Math.min(sorted.length - 1, idx));
            return sorted[idx] / 1_000_000.0;
        }
    }

    // =====================================================================
    // 十二、服务端线程数采样（jcmd）
    // =====================================================================

    /**
     * 通过 jcmd 采样服务端的线程数，实时打印，并统计 legacy-* 线程的数量。
     *
     * <p><b>预期现象</b>：长连接数 = N 时，服务端的 HeartbeatHandler 线程数会增长到约 N
     * （<b>不是 2N</b>）。原因见类注释里对 B3 的说明：
     * 当前版本的长连接分支在 {@code dispatch()} 返回后就退出了 {@code legacy-client-*} 线程，
     * 只剩 HeartbeatHandler 常驻。
     * <p>
     * 真正要盯的是：短连接压测时 {@code legacy-client-*} 线程数会随并发数<b>无上限</b>地涨
     *（因为 {@code newCachedThreadPool} 是无界的）。
     *
     * <p><b>设计笔记（final 字段的实际价值）</b>：
     * 另一个采样器 MySqlSampler 里的 {@code cfg} 声明为 final，必须在构造函数中完成初始化，
     * 否则无法编译。如果它不是 final，那个类<b>能编译通过</b>，但 cfg 永远是 null，
     * 运行到 {@code cfg.mysqlEnabled} 时会直接 NPE。
     * <p>
     * final 在这里的作用，就是把「忘了赋值」从运行期的神秘 NPE
     * 提前成编译期的明确报错 —— 这是 final 字段很实际的一个价值。
     */
    public static class ThreadSampler implements AutoCloseable {
        private final List<Sample> samples = Collections.synchronizedList(new ArrayList<>());
        private volatile boolean running = true;
        private Thread worker;
        private String pid;

        record Sample(int second, int total, int legacyClient, int legacyHeartbeat) {}

        /**
         * 私有构造函数：只在内部的静态工厂方法 start() 里创建实例。
         * <p>
         * 为什么设为 private：这个采样器的生命周期必须由 start() 统一管理，
         * 因为 start() 里还做了 pid 探测和采样线程启动这些初始化动作。
         * 用私有构造函数把「绕过 start() 直接 new」这条路堵死。
         */
        private ThreadSampler() {
        }

        static ThreadSampler start(Config cfg) {
            ThreadSampler s = new ThreadSampler();
            s.pid = cfg.pid != null ? cfg.pid : findServerPid(cfg.serverMain);
            if (s.pid == null) {
                System.out.println("[线程采样] 未找到服务端进程（jps 匹配 " + cfg.serverMain + "），"
                        + "线程数采样已禁用。可以用 --pid 指定，或用 --jcmd off 关闭。");
                return s;
            }
            System.out.println("[线程采样] 已锁定服务端 pid = " + s.pid);
            s.worker = new Thread(s::loop, "thread-sampler");
            s.worker.setDaemon(true);
            s.worker.start();
            return s;
        }

        private void loop() {
            int sec = 0;
            while (running) {
                Sample s = readSample(sec++);
                if (s != null) {
                    samples.add(s);
                    System.out.printf(Locale.ROOT, "  [T+%-3ds] 服务端线程数=%-5d  legacy-client(短连接在途)=%-4d  heartbeat(长连接常驻)=%-4d%n",
                            s.second(), s.total(), s.legacyClient(), s.legacyHeartbeat());
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        /**
         * 采样一次服务端线程快照。
         *
         * <p><b>为什么心跳线程要用栈帧来识别，而不是线程名</b>：
         * {@code LegacyTcpServer} 给业务线程显式命名了 {@code "legacy-client-" + id}，
         * 但 {@code HeartbeatHandler} 是 {@code new HeartbeatHandler(...).start()} 启动的，
         * <b>从头到尾没有调用过 setName()</b>，所以它的线程名是 JDK 默认的 "Thread-123"。
         * <p>
         * 如果按线程名去数 "legacy-heartbeat-"，永远是 0 —— 采样器会安静地给出错误结论。
         * 正确做法是看线程栈里有没有 {@code HeartbeatHandler.run} 这个栈帧，
         * 栈帧里带的是类名+方法名，不受线程名影响。
         */
        private Sample readSample(int sec) {
            try {
                Process p = new ProcessBuilder("jcmd", pid, "Thread.print")
                        .redirectErrorStream(true).start();
                String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                p.waitFor();

                int total = 0, client = 0, heartbeat = 0;
                // jcmd Thread.print 的输出按「线程块」组织：
                //   "线程名" #12 prio=5 os_prio=0 cpu=.. tid=.. nid=.. waiting on condition
                //       java.lang.Thread.State: RUNNABLE
                //       at com.ylaneo...HeartbeatHandler.run(HeartbeatHandler.java:36)
                // 一个块 = 一行以 " 开头的表头 + 其后的若干行。
                boolean inBlock = false;
                boolean blockIsHeartbeat = false;
                for (String line : out.split("\\R")) {
                    if (line.startsWith("\"")) {
                        // 新线程块开始 —— 先结算上一个块
                        if (inBlock && blockIsHeartbeat) {
                            heartbeat++;
                        }
                        inBlock = true;
                        blockIsHeartbeat = false;
                        total++;
                        if (line.contains("legacy-client-")) {
                            client++;
                        }
                    } else if (inBlock && line.contains("HeartbeatHandler.run")) {
                        blockIsHeartbeat = true;
                    }
                }
                // 结算最后一个块
                if (inBlock && blockIsHeartbeat) {
                    heartbeat++;
                }
                return new Sample(sec, total, client, heartbeat);
            } catch (Exception e) {
                return null;
            }
        }

        String summary() {
            synchronized (samples) {
                if (samples.isEmpty()) return "[线程采样] 无数据";
                int max = 0, maxClient = 0, maxHeartbeat = 0;
                for (Sample s : samples) {
                    max = Math.max(max, s.total());
                    maxClient = Math.max(maxClient, s.legacyClient());
                    maxHeartbeat = Math.max(maxHeartbeat, s.legacyHeartbeat());
                }
                StringBuilder sb = new StringBuilder();
                sb.append(String.format(Locale.ROOT,
                        "[线程采样] 峰值线程数=%d  峰值 legacy-client(短连接在途)=%d  峰值 heartbeat(长连接常驻)=%d  采样点=%d",
                        max, maxClient, maxHeartbeat, samples.size()));
                sb.append(System.lineSeparator()).append("    线程数曲线: ").append(sparkline(samples));
                return sb.toString();
            }
        }

        /** 把线程数采样画成 sparkline，一眼看出是否线性增长。 */
        private String sparkline(List<Sample> list) {
            String[] bars = {"▁", "▂", "▃", "▄", "▅", "▆", "▇", "█"};
            int max = 1;
            for (Sample s : list) max = Math.max(max, s.total());
            StringBuilder sb = new StringBuilder();
            int step = Math.max(1, list.size() / 60);
            for (int i = 0; i < list.size(); i += step) {
                int v = list.get(i).total();
                int idx = Math.min(bars.length - 1, v * bars.length / max);
                sb.append(bars[idx]);
            }
            return sb.toString() + "  (峰值 " + max + ")";
        }

        @Override
        public void close() {
            running = false;
            if (worker != null) worker.interrupt();
        }
    }

    /** 用 jps 找到服务端 pid。 */
    static String findServerPid(String mainClass) {
        try {
            Process p = new ProcessBuilder("jps", "-l").redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            p.waitFor();
            for (String line : out.split("\\R")) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2 && parts[1].equals(mainClass)) {
                    return parts[0];
                }
            }
        } catch (Exception ignored) {
            // 找不到就算了，不影响压测本身
        }
        return null;
    }

    // =====================================================================
    // 十三、MySQL 活跃连接数采样
    // =====================================================================

    /**
     * 采样 MySQL 的活跃连接数。
     *
     * <p>预期现象：因为 DAO 用的是 {@code DriverManager.getConnection()} 而不是 HikariCP 连接池，
     * 每次查询都会新建一条物理连接，所以这个数字会飙到接近 QPS。
     * 这是「无连接池」最直观的证据。
     */
    public static class MySqlSampler implements AutoCloseable {
        /* Field 'cfg' might not have been initialized：
        这个 bug 的本质是"编译器帮你兜住了"
        如果 cfg 不是 final，这个类能编译通过，但 cfg 永远是 null → 运行到 cfg.mysqlEnabled 直接 NPE。
        final 在这里帮你把一个运行期崩溃提前到了编译期。
        这就是 final 字段的实际价值之一：它把"忘了赋值"变成编译错误，而不是运行时的神秘 NPE。 */
        private final Config cfg;
        private final List<Integer> samples = Collections.synchronizedList(new ArrayList<>());
        private volatile boolean running = true;
        private Thread worker;
        private boolean available = false;

        /**
         * 私有构造函数：只在内部的静态工厂方法 start() 里创建实例。
         * <p>
         * cfg 是 final，必须在构造时完成初始化，否则无法编译。
         * <p>
         * 为什么设为 private：start() 里会做 Class.forName() 驱动加载。
         * 如果外部直接 new MySqlSampler(cfg)，就会跳过驱动加载，
         * DriverManager.getConnection() 会抛 "No suitable driver found"。
         * 用私有构造函数强制走 start()，避免这个坑。
         */
        private MySqlSampler(Config cfg) {
            this.cfg = cfg;
        }

        static MySqlSampler start(Config cfg) {
            MySqlSampler s = new MySqlSampler(cfg);
            if (!cfg.mysqlEnabled) {
                System.out.println("[MySQL采样] 已关闭（--mysql off）");
                return s;
            }
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                Connection c = DriverManager.getConnection(cfg.mysqlUrl, cfg.mysqlUser, cfg.mysqlPassword);
                c.close();
                s.available = true;
                System.out.println("[MySQL采样] 已连接，采样 information_schema.processlist");
            } catch (Exception e) {
                System.out.println("[MySQL采样] 连接失败，本次跳过（不影响压测）: " + e.getMessage());
            }
            if (!s.available) return s;
            s.worker = new Thread(s::loop, "mysql-sampler");
            s.worker.setDaemon(true);
            s.worker.start();
            return s;
        }

        private void loop() {
            while (running) {
                int count = readCount();
                if (count >= 0) {
                    samples.add(count);
                    System.out.printf(Locale.ROOT, "  [T+%-3ds] MySQL 活跃连接数 = %d%n", samples.size() - 1, count);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        private int readCount() {
            try (Connection conn = DriverManager.getConnection(cfg.mysqlUrl, cfg.mysqlUser, cfg.mysqlPassword);
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM information_schema.processlist")) {
                return rs.next() ? rs.getInt(1) : -1;
            } catch (Exception e) {
                return -1;
            }
        }

        String summary() {
            synchronized (samples) {
                if (samples.isEmpty()) return "[MySQL采样] 无数据";
                int max = 0;
                for (Integer v : samples) max = Math.max(max, v);
                return String.format(Locale.ROOT, "[MySQL采样] 峰值活跃连接数=%d  采样点=%d  曲线=%s",
                        max, samples.size(), spark(samples));
            }
        }

        private String spark(List<Integer> list) {
            String[] bars = {"▁", "▂", "▃", "▄", "▅", "▆", "▇", "█"};
            int max = 1;
            for (Integer v : list) max = Math.max(max, v);
            StringBuilder sb = new StringBuilder();
            int step = Math.max(1, list.size() / 60);
            for (int i = 0; i < list.size(); i += step) {
                int idx = Math.min(bars.length - 1, list.get(i) * bars.length / max);
                sb.append(bars[idx]);
            }
            return sb.toString() + "  (峰值 " + max + ")";
        }

        @Override
        public void close() {
            running = false;
            if (worker != null) worker.interrupt();
        }
    }
}
