package dev.polaris_light.bilibili_media.util;

import dev.polaris_light.bilibili_media.BilibiliMedia;
import org.watermedia.api.network.patchs.AbstractPatch.FixingURLException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

public class VideoDownloader {

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

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new FixingURLException(directUri, new RuntimeException("下载失败，HTTP状态码: " + conn.getResponseCode()));
            }

            String fileName = null;
            String disposition = conn.getHeaderField("Content-Disposition");
            if (disposition != null && disposition.contains("filename=")) {
                fileName = disposition.split("filename=")[1].replace("\"", "").trim();
            }
            if (fileName == null || fileName.isEmpty()) {
                String path = url.getPath();
                fileName = path.substring(path.lastIndexOf('/') + 1);
            }

            File targetFile = downloadDir.resolve(fileName).toFile();

            try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }

            BilibiliMedia.LOGGER.info("[bilibili_media] 已下载视频到本地: {}", targetFile.getAbsolutePath());
            BilibiliMediaUtil.updateVideoFile(shortUri.toString(), fileName);
            return BilibiliMediaUtil.getUri(fileName);
        } catch (Exception e) {
            throw new FixingURLException(directUri, new RuntimeException("下载过程中出错: " + e.getMessage(), e));
        }
    }
}
