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
import java.util.concurrent.*;

/*swing 客户端的旧 TCP 协议入口监听器。
从桌面端的后端的runserver迁移而来
行为完全等价：
- 监听 20001 端口（与原 chat-server ServerSocket(20001) 一致）
- 每来一个 swing 连接 new 一个 ServerThread 处理（这里用 LegacyClientHandler）
- ServerThread.start() 启动线程

差异：
1. 不再独立main，由Spring Boot启动时通过CommandLineRunner触发
2. 用线程池替代无界 while(true)，便于控制并发数
3. 通过 Spring 注入依赖，避免 new XXXImpl()

port 默认 20001，可通过 application.yml 配置：
legacy:
    tcp-port: 20001*/
@Component
@Order(0)//优先启动，确保启动后客户端可以立刻连，Spring在没有@Order时，会按Bean名称的字母顺序来决定执行顺序
//CommandLineRunner是Spring Boot为"随应用启动而启动"的逻辑提供的标准化钩子（Hook）
//使用CommandLineRunner的原因：原来的桌面端的后端（Tcp服务器）整合到Spring Boot项目中，没有独立的main()方法留给TCP服务器了
public class LegacyTcpServer implements CommandLineRunner {

    //默认端口，和chat-server一致
    private static final int DEFAULT_PORT = 20001;

    //线程池创建
    private final ExecutorService pool =
            //Excutors类提供了一系列静态工厂方法，用于快速创建各种预配置好的线程池实现。返回类型就是ExecutorService
            Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r);
                t.setName("legacy-client-" + t.getId());//获取该Thread线程对象的唯一标识符（类型为Long）
                return t;
            });

    //新版线程池创建,自定义的，不用newCachedThreadPool便捷创建方法
//    private final ExecutorService pool =
//            new ThreadPoolExecutor(
//                    5,
//                    20,
//                    60L, TimeUnit.SECONDS,
//                    new LinkedBlockingDeque<>(1000),
//                    new ThreadFactory() {
//                        @Override
//                        public Thread newThread(Runnable r) {
//                            return null;
//                        }
//                    },
//                    new RejectedExecutionHandler() {
//                        @Override
//                        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
//
//                        }
//                    }
//            );

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
        acceptThread.start();//使用start方法运行这个线程，不会阻塞。
    }

    private void startAcceptLoop(int port) {
        //创建监听serverSocket
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Legacy] TCP 监听已启动，端口 " + port);
            //让20001端口保持一直启动的状态，
            while (true) {
                //serverSocket.accept()会让线程acceptThread阻塞在这一行，直到有客户端连接进来，才会进行后续操作：建立一个socket连接，
                //建立一个
                Socket socket = serverSocket.accept();
                System.out.println("[Legacy] serverSocket接受到客户端连接: " + socket.getRemoteSocketAddress());
                //LegacyClientHandler是一个继承了Runnable的类，所以可以用线程池pool提交这个任务。
                LegacyClientHandler handler = new LegacyClientHandler(socket, userService, contactService, messageService,
                        parser, responseBuilder, pushNotifier);
                pool.submit(handler);
            }
        } catch (IOException e) {
            // 关闭socket时会触发
            System.out.println("[Legacy] TCP 监听已关闭: " + e.getMessage());
        }
    }
}
