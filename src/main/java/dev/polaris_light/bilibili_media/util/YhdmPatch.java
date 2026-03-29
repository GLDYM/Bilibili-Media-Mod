package dev.polaris_light.bilibili_media.util;

import dev.polaris_light.bilibili_media.BilibiliMedia;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.watermedia.api.network.patchs.AbstractPatch;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YhdmPatch extends AbstractPatch {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Pattern URL_PATTERN = Pattern.compile("yhdm\\.one/vod-play/([^/]+)/([^.]+)\\.html");

    @Override
    public String platform() {
        return "yhdm";
    }

    @Override
    public boolean isValid(URI uri) {
        if (!BilibiliMedia.config.enableYhdm) {
            return false;
        }
        return URL_PATTERN.matcher(uri.toString()).find();
    }

    @Override
    public Result patch(URI uri, Quality prefQuality) throws FixingURLException {
        super.patch(uri, prefQuality);
        String url = uri.toString();

        Matcher matcher = URL_PATTERN.matcher(url);
        if (!matcher.find()) {
            throw new FixingURLException(url, new RuntimeException("URL is sus: " + url));
        }

        String id = matcher.group(1);
        String vid = matcher.group(2);
        String apiUrl = String.format("https://yhdm.one/_get_plays/%s/%s", id, vid);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .build();
        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new FixingURLException(url, new RuntimeException("Failed to get video info from YHDM API, status code: " + response.statusCode()));
            }
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray videoPlays = json.getAsJsonArray("video_plays");
            if (videoPlays == null || videoPlays.size() == 0) {
                throw new FixingURLException(url, new RuntimeException("No video plays found for URL: " + url));
            }
            JsonObject videoPlay = videoPlays.get(0).getAsJsonObject();
            String videoUrl = videoPlay.get("play_data").getAsString();
            return new Result(URI.create(videoUrl), true, false);
        } catch (Exception e) {
            BilibiliMedia.LOGGER.error("[bilibili_media] YHDM 解析失败", e);
            throw new FixingURLException(url, e);
        }
    }
}
