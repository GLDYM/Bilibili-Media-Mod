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

public class BilibiliLivePatch extends AbstractPatch {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Pattern ROOM_ID_PATTERN = Pattern.compile("live\\.bilibili\\.com/(\\d+)");
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 2000L;

    @Override
    public String platform() {
        return "bilibili-live";
    }

    @Override
    public boolean isValid(URI uri) {
        if (!BiliBiliMedia.config.enable) {
            return false;
        }
        return uri.toString().contains("live.bilibili.com/");
    }

    @Override
    public Result patch(URI uri, Quality prefQuality) throws FixingURLException {
        super.patch(uri, prefQuality);
        long roomId = parseRoomId(uri);

        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                long realRoomId = resolveRealRoomId(roomId, uri);
                URI streamUri = getLiveStreamUri(realRoomId, prefQuality, uri);
                BiliBiliMedia.LOGGER.info("[bilibili_media] 直播直链解析成功: {} -> {}", uri, streamUri);
                return new Result(streamUri, true, false);
            } catch (FixingURLException e) {
                if (i == MAX_RETRIES - 1) {
                    throw e;
                }
                BiliBiliMedia.LOGGER.warn("[bilibili_media] 直播直链解析失败，正在重试... ({} / {})", i + 1, MAX_RETRIES);
                sleepForRetry(uri);
            }
        }

        throw new FixingURLException(uri, new RuntimeException("无法解析直播直链"));
    }

    private static void sleepForRetry(URI sourceUri) throws FixingURLException {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new FixingURLException(sourceUri, new RuntimeException("解析过程中被中断"));
        }
    }

    private static long parseRoomId(URI uri) throws FixingURLException {
        Matcher matcher = ROOM_ID_PATTERN.matcher(uri.toString());
        if (!matcher.find()) {
            throw new FixingURLException(uri, new RuntimeException("无法解析直播间号"));
        }
        return Long.parseLong(matcher.group(1));
    }

    private static long resolveRealRoomId(long roomId, URI sourceUri) throws FixingURLException {
        String roomInitApi = "https://api.live.bilibili.com/room/v1/Room/room_init?id=" + roomId;
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(roomInitApi))
                    .header("User-Agent", "Mozilla/5.0");
            BiliCookieStore.withCookie(requestBuilder);
            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            if (root.get("code").getAsInt() != 0) {
                throw new FixingURLException(sourceUri, new RuntimeException("room_init 返回失败: " + root));
            }
            JsonObject data = root.getAsJsonObject("data");
            if (data == null || !data.has("room_id")) {
                throw new FixingURLException(sourceUri, new RuntimeException("room_init 未返回 room_id"));
            }
            return data.get("room_id").getAsLong();
        } catch (FixingURLException e) {
            throw e;
        } catch (Exception e) {
            throw new FixingURLException(sourceUri, e);
        }
    }

    private static URI getLiveStreamUri(long roomId, Quality prefQuality, URI sourceUri) throws FixingURLException {
        String apiUrl = "https://api.live.bilibili.com/xlive/web-room/v1/playUrl/playUrl?cid=" + roomId
                + "&platform=h5&qn=10000";

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Referer", "https://live.bilibili.com/");
            BiliCookieStore.withCookie(requestBuilder);
            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            if (root.get("code").getAsInt() != 0) {
                throw new FixingURLException(sourceUri, new RuntimeException("playUrl 返回失败: " + root));
            }

            JsonObject data = root.getAsJsonObject("data");
            JsonArray durl = data == null ? null : data.getAsJsonArray("durl");
            if (durl == null || durl.isEmpty()) {
                throw new FixingURLException(sourceUri, new RuntimeException("直播 API 未返回可播放流"));
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
