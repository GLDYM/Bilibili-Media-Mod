package dev.polaris_light.bilibili_media.util;

import dev.polaris_light.bilibili_media.BilibiliMedia;
import org.watermedia.api.network.patchs.AbstractPatch.FixingURLException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

public class BilibiliShortLinkMediaPlayResolver {
    private static final String MOBILE_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) EdgiOS/121.0.2277.107 Version/17.0 Mobile/15E148 Safari/604.1";
    private static final Pattern SHORT_URL_PATTERN = Pattern.compile("https://(b23\\.tv)/\\S+");

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static URI expand(URI uri) throws FixingURLException {
        var matcher = SHORT_URL_PATTERN.matcher(uri.toString());
        if (!matcher.find()) {
            throw new FixingURLException(uri, new RuntimeException("不是短链"));
        }

        String shortUri = matcher.group();
        BilibiliMedia.LOGGER.info("[bilibili_media] 正在展开短链接: {}", shortUri);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", MOBILE_USER_AGENT)
                    .GET()
                    .build();

            HttpResponse<Void> response = CLIENT.send(request, HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String finalUrl = response.uri().toString();
                BilibiliMedia.LOGGER.info("[bilibili_media] 短链接 {} 展开为 -> {}", shortUri, finalUrl);
                return URI.create(finalUrl);
            }

            BilibiliMedia.LOGGER.warn("[bilibili_media] 展开短链接失败，状态码: {}", response.statusCode());
            throw new FixingURLException(uri, new RuntimeException("展开短链接失败，状态码: " + response.statusCode()));
        } catch (Exception e) {
            BilibiliMedia.LOGGER.error("[bilibili_media] 展开短链接时发生异常", e);
            throw new FixingURLException(uri, new RuntimeException("展开短链接时发生异常"));
        }
    }
}
