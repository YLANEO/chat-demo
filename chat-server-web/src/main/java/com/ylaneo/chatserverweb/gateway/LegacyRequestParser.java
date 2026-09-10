package com.ylaneo.chatserverweb.gateway;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析 Swing 客户端发来的尖括号协议字符串。
 *
 * 来源：chat-server/src/main/java/com/ylaneo/chatserver/util/RequestHandler.java
 * 迁移说明：
 *   - 完全照搬 handleParams() 的实现逻辑
 *   - 包名改为 com.ylaneo.chatserverweb.gateway
 *   - 加 @Component 方便注入
 *
 * 协议示例：
 *   输入: <login><username><password>
 *   输出: ["login", "username", "password"]
 *   规律：以 '<' 分段，每段去掉末尾的 '>' 即为参数值
 *   索引 0 是命令名（如 login / sendMessage），从 1 起是参数
 */
@Component
public class LegacyRequestParser {

    /**
     * 把尖括号格式的请求字符串解析为参数列表。
     *
     * request 完整原始请求，例如 "<login><userA><123456>"
     * 返回参数列表，[0] 是命令名，[1..] 是参数值
     */
    public List<String> handleParams(String request) {
        String str = request;
        String[] strlist = str.split("<");
        List<String> params = new ArrayList<>();
        System.out.println("[Legacy] 解析请求: " + request);
        for (String a : strlist) {
            if (!a.isEmpty()) {
                // 去掉每段末尾的 '>'
                String b = a.substring(0, a.length() - 1);
                params.add(b);
            }
        }
        System.out.println("[Legacy] 解析结果: " + params);
        return params;
    }
}
