package com.finkkk.bilibili_media.util;

import com.finkkk.bilibili_media.BiliBiliMedia;
import org.watermedia.api.network.patchs.AbstractPatch.FixingURLException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class VideoDownloader {

    /**
     * 下载远程 mp4 文件到本地，并返回本地服务器的 URI
     * @param shortUri 稳定的短链 (BV号 + 分P)
     * @param directUri 直链 (临时)
     */
    public static URI downloadToLocal(URI shortUri, URI directUri) throws FixingURLException {
        Path downloadDir = BilibiliMediaUtil.getDownloadPath();

        try {
            URL url = directUri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new FixingURLException(directUri, new RuntimeException("下载失败，HTTP状态码: " + conn.getResponseCode()));
            }

            // 从响应头获取文件名
            String fileName = null;
            String disposition = conn.getHeaderField("Content-Disposition");
            if (disposition != null && disposition.contains("filename=")) {
                fileName = disposition.split("filename=")[1].replace("\"", "").trim();
            }
            if (fileName == null || fileName.isEmpty()) {
                // 如果没有 Content-Disposition，就用 URL path 最后一段
                String path = url.getPath();
                fileName = path.substring(path.lastIndexOf('/') + 1);
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

            // 使用短链作为 key，更新映射表
            BilibiliMediaUtil.updateVideoFile(shortUri.toString(), targetFile);

            // 返回本地服务器可访问的 URI
            return BilibiliMediaUtil.getUri(targetFile);

        } catch (Exception e) {
            throw new FixingURLException(directUri, new RuntimeException("下载过程中出错: " + e.getMessage(), e));
        }
    }
}
