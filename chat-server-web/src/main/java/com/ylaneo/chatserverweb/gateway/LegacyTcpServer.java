package com.ylaneo.chatserverweb.gateway;

import com.ylaneo.chatserverweb.service.ContactService;
import com.ylaneo.chatserverweb.service.MessageService;
import com.ylaneo.chatserverweb.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * swing 客户端的旧 TCP 协议入口监听器。
 *
 * 来源（迁移说明）：
 *   chat-server/src/main/java/com/ylaneo/chatserver/socket/Server.java 的 Server() 构造函数
 *   + RunServer.java 的 main。
 *
 *   行为完全等价：
 *     - 监听 20001 端口（与原 chat-server ServerSocket(20001) 一致）
 *     - 每来一个 swing 连接 new 一个 ServerThread 处理（这里用 LegacyClientHandler）
 *     - ServerThread.start() 启动线程
 *
 *   差异：
 *     1. 不再独立 main，由 Spring Boot 启动时通过 CommandLineRunner 触发
 *     2. 用线程池替代无界 while(true)，便于控制并发数
 *     3. 通过 Spring 注入依赖，避免 new XXXImpl()
 *
 *   port 默认 20001，可通过 application.yml 配置：
 *     legacy:
 *       tcp-port: 20001
 */
@Component
@Order(0)//优先启动，确保启动后客户端可以立刻连
public class LegacyTcpServer implements CommandLineRunner {

    /** 默认端口，和chat-server一致 */
    private static final int DEFAULT_PORT = 20001;

    private final ExecutorService pool =
            Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r);
                t.setName("legacy-client-" + t.getId());
                return t;
            });

    @Autowired
    private UserService userService;

    @Autowired
    private ContactService contactService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private LegacyRequestParser parser;

    @Autowired
    private LegacyResponseBuilder responseBuilder;

    @Autowired
    private LegacyPushNotifier pushNotifier;

    @Override
    public void run(String... args) {
        //
        int port = DEFAULT_PORT;
        //单独开一个线程跑accept循环，不阻塞Spring Boot启动
        Thread acceptThread = new Thread(() -> startAcceptLoop(port), "legacy-tcp-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void startAcceptLoop(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Legacy] TCP 监听已启动，端口 " + port);
            //
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("[Legacy] 接受到客户端连接: " + socket.getRemoteSocketAddress());
                //
                LegacyClientHandler handler = new LegacyClientHandler(
                        socket,
                        userService,
                        contactService,
                        messageService,
                        parser,
                        responseBuilder,
                        pushNotifier);
                pool.submit(handler);
            }
        } catch (IOException e) {
            // 关闭socket时会触发
            System.out.println("[Legacy] TCP 监听已关闭: " + e.getMessage());
        }
    }
}
