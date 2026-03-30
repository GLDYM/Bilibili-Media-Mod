package dev.polaris_light.bilibili_media.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.polaris_light.bilibili_media.auth.BiliCookieStore;
import dev.polaris_light.bilibili_media.BiliBiliMedia;
import org.watermedia.api.network.patchs.AbstractPatch;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BilibiliBangumiPatch extends AbstractPatch {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Pattern EP_ID_PATTERN = Pattern.compile("/ep(\\d+)");
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 2000L;

    @Override
    public String platform() {
        return "bilibili-bangumi";
    }

    @Override
    public boolean isValid(URI uri) {
        if (!BiliBiliMedia.config.enable) {
            return false;
        }
        return uri.toString().contains("bilibili.com/bangumi/play/");
    }

    @Override
    public Result patch(URI uri, Quality prefQuality) throws FixingURLException {
        super.patch(uri, prefQuality);
        String url = uri.toString();
        String epId = parseEpisodeId(url);
        if (epId == null) {
            throw new FixingURLException(uri, new RuntimeException("无法解析番剧 EP ID"));
        }

        // int qn = mapQuality(prefQuality);
        String cacheKey = buildCacheKey(uri);

        String cachedFileName = BilibiliMediaUtil.tryGetLocalFile(cacheKey);
        if (cachedFileName != null) {
            URI localUri = BilibiliMediaUtil.getUri(cachedFileName);
            BilibiliMediaUtil.updateVideoFile(cacheKey, cachedFileName);
            BiliBiliMedia.LOGGER.info("[bilibili_media] 番剧缓存命中: {} -> {}", uri, localUri);
            return new Result(localUri, false, true);
        }

        URI playableUri = null;
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                playableUri = getBangumiMp4Uri(epId, uri);
                BiliBiliMedia.LOGGER.info("[bilibili_media] 番剧直链解析成功: {} -> {}", uri, playableUri);
                break;
            } catch (FixingURLException e) {
                if (i == MAX_RETRIES - 1) {
                    throw e;
                }
                BiliBiliMedia.LOGGER.warn("[bilibili_media] 番剧直链解析失败，正在重试... ({} / {})", i + 1, MAX_RETRIES);
                sleepForRetry(uri);
            }
        }

        if (playableUri == null) {
            throw new FixingURLException(uri, new RuntimeException("无法解析番剧直链"));
        }

        if (!BiliBiliMedia.config.enableCache) {
            return new Result(playableUri, true, false);
        }

        try {
            URI localUri = VideoDownloader.downloadToLocal(URI.create(cacheKey), playableUri);
            BiliBiliMedia.LOGGER.info("[bilibili_media] 番剧缓存下载成功: {} -> {}", uri, localUri);
            return new Result(localUri, false, false);
        } catch (Exception e) {
            BiliBiliMedia.LOGGER.warn("[bilibili_media] 番剧缓存下载失败，回退直链", e);
            return new Result(playableUri, true, false);
        }
    }

    private static String buildCacheKey(URI uri) {
        return URI.create(uri.getScheme() + "://" + uri.getAuthority() + uri.getPath()).toString();
    }

    private static void sleepForRetry(URI sourceUri) throws FixingURLException {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new FixingURLException(sourceUri, new RuntimeException("解析过程中被中断"));
        }
    }

    private static String parseEpisodeId(String url) {
        Matcher matcher = EP_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static int mapQuality(Quality prefQuality) {
        if (prefQuality == null) {
            return 64;
        }
        return switch (prefQuality) {
            case LOWEST -> 16;
            case LOW -> 32;
            case MIDDLE -> 64;
            case HIGH -> 80;
            default -> 64;
        };
    }

    private static URI getBangumiMp4Uri(String epId, URI sourceUri) throws FixingURLException {
        String playApi = "https://api.bilibili.com/pgc/player/web/playurl?ep_id=" + epId
                + "&qn=116&type=&otype=json&platform=html5&high_quality=1";

        try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(playApi))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Referer", "https://www.bilibili.com/")
                    .header("Origin", "https://www.bilibili.com");
                BiliCookieStore.withCookie(requestBuilder);
                HttpRequest request = requestBuilder.build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            if (root.get("code").getAsInt() != 0) {
                throw new FixingURLException(sourceUri, new RuntimeException("番剧 API 返回失败: " + root));
            }

            JsonObject result = root.getAsJsonObject("result");
            if (result == null) {
                throw new FixingURLException(sourceUri, new RuntimeException("番剧 API 无 result 字段"));
            }

            JsonArray durl = result.getAsJsonArray("durl");
            if (durl == null || durl.isEmpty()) {
                throw new FixingURLException(sourceUri, new RuntimeException("番剧 API 未返回 MP4 durl"));
            }

            JsonObject first = durl.get(0).getAsJsonObject();
            return URI.create(first.get("url").getAsString());
        } catch (FixingURLException e) {
            throw e;
        } catch (Exception e) {
            throw new FixingURLException(sourceUri, e);
        }
    }
}
