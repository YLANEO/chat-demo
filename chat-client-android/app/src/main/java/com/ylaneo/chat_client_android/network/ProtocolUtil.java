package com.ylaneo.chat_client_android.network;

import java.util.ArrayList;
import java.util.List;

/**
 * 协议解析工具类
 *
 * 协议格式（与桌面端一致）：
 *   请求：<cmd><arg1><arg2>...
 *   响应：<code><message><data...>
 *
 * 注意：服务器返回的整条消息以 '>' 结尾，参数本身可能不含 '<'，所以按 '<' 分割后
 * 每个段最后一个字符（'）'）需要去掉。
 */
public class ProtocolUtil {

    /**
     * 解析服务器响应为字段列表
     *
     * 原始格式示例：
     *   "<0><登录成功><123>"
     * 解析后：
     *   ["0", "登录成功", "123"]
     *
     * @param response 服务器返回的整条消息
     * @return 字段列表
     */
    public static List<String> parse(String response) {
        List<String> result = new ArrayList<>();
        if (response == null || response.isEmpty()) {
            return result;
        }
        // 按 '<' 分割；split 会自动去掉空首段（因为原始字符串以 '<' 开头时首段是空字符串）
        String[] parts = response.split("<");
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            // 去掉每个段末尾的 '>'（最后一段的 '>' 就是协议结尾，非最后一段的 '>' 是参数分隔）
            // 实际上每个段都只有一个结尾 '>'，因为 '<' 已经是分隔符了
            String trimmed = part;
            if (trimmed.endsWith(">")) {
                //只选取有效部分
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            result.add(trimmed);
        }
        return result;
    }

    /**
     * 把响应解析成 Result<String[]>，code=0 表示成功
     */
    public static class ParsedResponse {
        public final int code;
        public final String message;
        public final List<String> data; // 第三个字段开始的所有业务数据

        public ParsedResponse(int code, String message, List<String> data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() {
            return code == 0;
        }
    }

    /**
     * 解析标准响应：<code><message><...data>
     */
    public static ParsedResponse parseResponse(String response) {
        List<String> params = parse(response);
        if (params.size() < 2) {
            return new ParsedResponse(-1, "响应格式错误", new ArrayList<>());
        }
        int code;
        try {
            code = Integer.parseInt(params.get(0));
        } catch (NumberFormatException e) {
            return new ParsedResponse(-1, "响应 code 解析失败", new ArrayList<>());
        }
        String message = params.get(1);
        List<String> data = new ArrayList<>();
        if (params.size() > 2) {
            data.addAll(params.subList(2, params.size()));
        }
        return new ParsedResponse(code, message, data);
    }
}
