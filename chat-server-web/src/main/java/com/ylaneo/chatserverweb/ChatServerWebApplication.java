package com.ylaneo.chatserverweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatServerWebApplication {

	public static void main(String[] args) {

		SpringApplication.run(ChatServerWebApplication.class, args);
		//这一行的作用：启动tomcat内嵌服务器（默认8080端口）；扫描controller/servier/dao交给spring管理；监听http请求
	}

}
