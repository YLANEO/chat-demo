package com.ylaneo.chat_client_android.util;

import com.ylaneo.chat_client_android.network.ProtocolUtil;

import java.util.List;

/**
 * 业务响应处理工具（与桌面端 ResponseHandler 对应）
 *
 * 实际上响应解析都委托给 ProtocolUtil，
 * 这里只是为了让上层业务代码调用更直观，例如：
 *   List<String> parts = ResponseHandler.parse(rawResponse);
 */
public class ResponseHandler {

    private ResponseHandler() {
    }

    public static List<String> parse(String response) {
        return ProtocolUtil.parse(response);
    }

    public static ProtocolUtil.ParsedResponse parseResponse(String response) {
        return ProtocolUtil.parseResponse(response);
    }
}
