package com.ylaneo.chatserverweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatServerWebApplication {

	public static void main(String[] args) {

		SpringApplication.run(ChatServerWebApplication.class, args);
		//  这一行干 3 件事：
		//  1. 启动 Tomcat 内嵌服务器（默认 8080）
		//  2. 扫描 controller / service / dao，交给 Spring 管理
		//  3. 监听 HTTP 请求
	}

}
