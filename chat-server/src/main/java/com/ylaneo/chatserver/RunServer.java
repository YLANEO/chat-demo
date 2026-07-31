package com.ylaneo.chatserver;

import com.ylaneo.chatserver.socket.Server;

public class RunServer {
    public static void main(String[] args) {
        System.out.println("启动!");
        try {
            new Server();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}