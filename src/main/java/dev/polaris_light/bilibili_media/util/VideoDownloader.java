package dev.polaris_light.bilibili_media.util;

import dev.polaris_light.bilibili_media.BiliBiliMedia;
import dev.polaris_light.bilibili_media.auth.BiliCookieStore;
import org.watermedia.api.network.patchs.AbstractPatch.FixingURLException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoDownloader {
    private static final Pattern FILENAME_STAR_PATTERN = Pattern.compile("filename\\*\\s*=\\s*(?:UTF-8''|utf-8''|\\\")?([^;\\\"]+)");
    private static final Pattern FILENAME_PATTERN = Pattern.compile("filename\\s*=\\s*\\\"?([^;\\\"]+)\\\"?");


    /**
     * 下载远程文件到本地，并返回本地服务器的 URI
     * @param shortUri 短链 (BV号 + 分P)
     * @param directUri 直链 (临时)
     */
    public static URI downloadToLocal(URI shortUri, URI directUri) throws FixingURLException {
        Path downloadDir = BilibiliMediaUtil.getDownloadPath();

        try {
            URL url = directUri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            conn.setRequestProperty("Referer", "https://www.bilibili.com/");
            conn.setRequestProperty("Origin", "https://www.bilibili.com");
            String cookie = BiliCookieStore.getCookie();
            if (cookie != null && !cookie.isBlank()) {
                conn.setRequestProperty("Cookie", cookie);
            }

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new FixingURLException(directUri, new RuntimeException("下载失败，HTTP状态码: " + conn.getResponseCode()));
            }

            // 优先解析 RFC5987 的 filename*，其次 fallback 到 filename。
            String fileName = null;
            String disposition = conn.getHeaderField("Content-Disposition");
            if (disposition != null && !disposition.isBlank()) {
                fileName = extractFileNameFromDisposition(disposition);
            }
            if (fileName == null || fileName.isEmpty()) {
                // if not found in header, extract from URL
                String path = url.getPath();
                fileName = path.substring(path.lastIndexOf('/') + 1);
            }

            fileName = sanitizeFileName(fileName);
            if (fileName.isEmpty()) {
                fileName = "video_" + System.currentTimeMillis() + ".mp4";
            }

            File targetFile = downloadDir.resolve(fileName).toFile();

            try (InputStream in = conn.getInputStream();
                FileOutputStream out = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }

            BiliBiliMedia.LOGGER.info("[bilibili_media] 已下载视频到本地: {}", targetFile.getAbsolutePath());

            BilibiliMediaUtil.updateVideoFile(shortUri.toString(), fileName);

            return BilibiliMediaUtil.getUri(fileName);

        } catch (Exception e) {
            throw new FixingURLException(directUri, new RuntimeException("下载过程中出错: " + e.getMessage(), e));
        }
    }

    private static String extractFileNameFromDisposition(String disposition) {
        Matcher filenameStarMatcher = FILENAME_STAR_PATTERN.matcher(disposition);
        if (filenameStarMatcher.find()) {
            String encoded = filenameStarMatcher.group(1).trim();
            try {
                return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                return encoded;
            }
        }

        Matcher filenameMatcher = FILENAME_PATTERN.matcher(disposition);
        if (filenameMatcher.find()) {
            return filenameMatcher.group(1).trim();
        }

        return null;
    }

    private static String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "";
        }

        String normalized = fileName.trim();
        int slash = Math.max(normalized.lastIndexOf('/'), normalized.lastIndexOf('\\'));
        if (slash >= 0 && slash < normalized.length() - 1) {
            normalized = normalized.substring(slash + 1);
        }

        // Windows 非法文件名字符全部替换为下划线。
        normalized = normalized.replaceAll("[\\\\/:*?\"<>|;]", "_");
        return normalized;
    }
}
