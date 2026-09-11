package com.ylaneo.chat_client_android.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 头像加载工具
 *
 * 模仿 web 端 utils/avatar.ts 的逻辑，把服务器返回的 avatar_url 转成可加载的 HTTP URL，
 * 用 HttpURLConnection 下载并缓存在内存里。
 *
 * 服务器字段（来自 User.avatar_url / FriendRequestDto.senderAvatar）可能是：
 *   - "D:/files_D/avatar_chat_demo/user_1.png"  本地磁盘绝对路径
 *   - "/uploads/user_1.png"                      已经是 HTTP 路径
 *   - ""                                         没头像，用默认
 *   - "http://xxx"                               完整 HTTP URL
 */
public class AvatarLoader {

    private static final String TAG = "AvatarLoader";

    /** 内存缓存：url -> Bitmap */
    private static final ConcurrentHashMap<String, Bitmap> cache = new ConcurrentHashMap<>();

    /** 单线程池就够了，图片请求不密集 */
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);

    /** 主线程 Handler，用于回填 ImageView */
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** ImageView.setTag 的 key，避免 RecyclerView 复用时旧请求回填错图 */
    private static final int TAG_URL = 0x7F0A_0001;

    /**
     * 把服务器存的 avatar_url 转成真正可访问的 HTTP URL
     *
     * @param rawUrl 服务器返回的 avatar_url
     * @return 可访问的 HTTP URL；null 表示用默认头像
     */
    public static String resolveUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isEmpty()) {
            return null;
        }
        // 已经是 HTTP URL
        if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            return rawUrl;
        }
        // 已经是 /uploads/xxx 形式
        if (rawUrl.startsWith("/uploads/")) {
            return Constants.HTTP_BASE_URL + rawUrl;
        }
        // Windows 磁盘路径（D:/xxx、C:/xxx，也兼容 D:\xxx）
        if (rawUrl.matches("^[A-Za-z]:[\\\\/].*")) {
            // 优先按 / 切（数据库里通常存正斜杠路径，如 D:/files_D/avatar_chat_demo/user_2.png）
            String fileName = "";
            int slashIdx = rawUrl.lastIndexOf('/');
            if (slashIdx >= 0 && slashIdx + 1 < rawUrl.length()) {
                fileName = rawUrl.substring(slashIdx + 1);
            }
            // 兜底按 \ 切（兼容反斜杠路径）
            if (fileName.isEmpty()) {
                int bsIdx = rawUrl.lastIndexOf('\\');
                if (bsIdx >= 0 && bsIdx + 1 < rawUrl.length()) {
                    fileName = rawUrl.substring(bsIdx + 1);
                }
            }
            // 最后兜底：实在切不出来就用整个路径，避免静默失败
            if (fileName.isEmpty()) {
                fileName = rawUrl;
                Log.w(TAG, "无法从磁盘路径里提取文件名: " + rawUrl);
            }
            return Constants.HTTP_BASE_URL + "/uploads/" + fileName;
        }
        // 兜底：当作相对路径处理
        return Constants.HTTP_BASE_URL + "/" + rawUrl;
    }

    /**
     * 异步加载头像并显示到 ImageView
     *
     * 会自动处理：
     *   - 内存缓存
     *   - 默认头像
     *   - 加载失败回退到默认头像
     *   - ImageView 复用时旧请求回填错图（用 tag 锁）
     */
    public static void load(ImageView view, String rawUrl) {
        String url = resolveUrl(rawUrl);

        // 先把当前请求的 URL 写到 tag 上，用于 ImageView 复用校验
        view.setTag(TAG_URL, url);

        if (url == null) {
            view.setImageResource(getDefaultAvatarResId(view));
            return;
        }

        // 1. 命中缓存
        Bitmap cached = cache.get(url);
        if (cached != null) {
            view.setImageBitmap(cached);
            return;
        }

        // 2. 设置占位图
        view.setImageResource(getDefaultAvatarResId(view));

        final String finalUrl = url;
        executor.execute(() -> {
            Bitmap bmp = downloadBitmap(finalUrl);
            if (bmp != null) {
                cache.put(finalUrl, bmp);
                mainHandler.post(() -> {
                    // 二次校验：tag 还是同一个 URL 才回填，避免 RecyclerView 复用时错图
                    Object current = view.getTag(TAG_URL);
                    if (finalUrl.equals(current)) {
                        view.setImageBitmap(bmp);
                    }
                });
            }
        });
    }

    /** 取出 vector 默认头像的资源 id，找不到就退化到系统自带 */
    private static int getDefaultAvatarResId(ImageView view) {
        int id = view.getContext().getResources().getIdentifier(
                "ic_default_avatar", "drawable", view.getContext().getPackageName());
        return id != 0 ? id : android.R.drawable.ic_menu_gallery;
    }

    private static Bitmap downloadBitmap(String urlStr) {
        HttpURLConnection conn = null;
        InputStream is = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            if (code != 200) {
                Log.w(TAG, "下载头像失败，code=" + code + ", url=" + urlStr);
                return null;
            }
            is = conn.getInputStream();
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (bmp == null) {
                Log.w(TAG, "BitmapFactory.decodeStream 返回 null, url=" + urlStr);
            }
            return bmp;
        } catch (IOException e) {
            Log.w(TAG, "下载头像异常: " + e.getClass().getSimpleName() + ": "
                    + e.getMessage() + ", url=" + urlStr);
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignore) {
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 清空缓存（退出登录时调用）
     */
    public static void clearCache() {
        cache.clear();
    }
}
