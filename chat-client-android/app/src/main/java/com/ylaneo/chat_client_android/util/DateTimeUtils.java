package com.ylaneo.chat_client_android.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期工具
 */
public class DateTimeUtils {

    private DateTimeUtils() {
    }

    /**
     * 把字符串解析为 LocalDateTime，失败或为空返回 null
     */
    public static LocalDateTime parse(String s) {
        if (s == null || s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return null;
        }
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 格式化为本地时间字符串，null 显示为 "-"
     */
    public static String format(LocalDateTime dt) {
        if (dt == null) {
            return "-";
        }
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 短格式（聊天列表用）：HH:mm
     */
    public static String formatShort(LocalDateTime dt) {
        if (dt == null) {
            return "";
        }
        return dt.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
