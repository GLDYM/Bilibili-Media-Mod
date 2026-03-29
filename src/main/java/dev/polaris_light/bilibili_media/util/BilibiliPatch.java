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

public class BilibiliPatch extends AbstractPatch {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @Override
    public String platform() {
        return "bilibili";
    }

    @Override
    public boolean isValid(URI uri) {
        if (!BilibiliMedia.config.enable) {
            return false;
        }
        return (uri.toString().contains("bilibili.com") && !uri.toString().contains("live")) || uri.toString().contains("b23.tv");
    }

    @Override
    public Result patch(URI uri, Quality prefQuality) throws FixingURLException {
        super.patch(uri, prefQuality);
        URI longUri;
        if (uri.toString().contains("b23.tv")) {
            longUri = BilibiliShortLinkMediaPlayResolver.expand(uri);
        } else {
            longUri = uri;
        }

        String shortUrl = extractUrl(longUri);
        if (BilibiliMediaUtil.tryGetLocalFile(shortUrl) != null) {
            String fileName = BilibiliMediaUtil.tryGetLocalFile(shortUrl);
            URI localUri = BilibiliMediaUtil.getUri(fileName);
            BilibiliMediaUtil.updateVideoFile(shortUrl, fileName);
            return new Result(localUri, false, true);
        }

        URI directUri;

        for (int i = 0; i < 5; i++) {
            try {
                directUri = patchUri(longUri);

                if (directUri != null) {
                    try {
                        if (!BilibiliMedia.config.enableCache) {
                            BilibiliMedia.LOGGER.info("[bilibili_media] 使用远程链接，很可能无法播放: {}", directUri);
                            return new Result(directUri, true, false);
                        }
                        URI localUri = VideoDownloader.downloadToLocal(URI.create(shortUrl), directUri);
                        return new Result(localUri, false, false);
                    } catch (Exception e) {
                        BilibiliMedia.LOGGER.warn("[bilibili_media] 下载文件时发生错误，使用远程链接", e);
                        return new Result(directUri, true, false);
                    }
                } else {
                    BilibiliMedia.LOGGER.warn("[bilibili_media] 解析链接失败，正在重试... ({} / 5)", i + 1);
                    if (i == 4) {
                        throw new FixingURLException(uri, new RuntimeException("无法获取直接链接"));
                    } else {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new FixingURLException(uri, new RuntimeException("解析过程中被中断"));
                        }
                    }
                }
            } catch (FixingURLException e) {
                BilibiliMedia.LOGGER.warn("[bilibili_media] 解析链接失败，正在重试... ({} / 5)", i + 1);
                if (i == 4) {
                    throw new FixingURLException(uri, new RuntimeException("无法获取直接链接"));
                } else {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new FixingURLException(uri, new RuntimeException("解析过程中被中断"));
                    }
                }
            }
        }

        throw new FixingURLException(uri, new RuntimeException("无法获取直接链接"));
    }

    public static int parsePage(String url) {
        try {
            Pattern pattern = Pattern.compile("[?&]p=(\\d+)");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            } else {
                return 1;
            }
        } catch (Exception e) {
            return 1;
        }
    }

    public static String parseBvid(String url) {
        Pattern pattern = Pattern.compile("(BV[a-zA-Z0-9]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static String extractUrl(URI uri) throws FixingURLException {
        String sUri = uri.toString();
        String bvid = parseBvid(sUri);
        if (bvid == null) {
            throw new FixingURLException(uri, new RuntimeException("无法解析BV号"));
        }
        int page = parsePage(sUri);

        return "https://www.bilibili.com/video/" + bvid + "?p=" + page;
    }

    public URI patchUri(URI uri) throws FixingURLException {
        String sUri = uri.toString();
        String bvid = parseBvid(sUri);
        if (bvid == null) {
            throw new FixingURLException(uri, new RuntimeException("无法解析BV号"));
        }

        int page = parsePage(sUri);

        try {
            Long cid = getCid(bvid, page);
            if (cid == 0L) {
                throw new FixingURLException(uri, new RuntimeException("无法获取cid"));
            }
            URI directUri = getDirectUri(bvid, cid);
            if (directUri == null) {
                throw new FixingURLException(uri, new RuntimeException("无法获取直接链接"));
            }
            BilibiliMedia.LOGGER.info("[bilibili_media] 成功解析链接: {} -> {}", uri, directUri);
            return directUri;
        } catch (Exception e) {
            throw new FixingURLException(uri, new RuntimeException("解析失败，请尝试重新加载。" + e.getMessage()));
        }
    }

    public Long getCid(String bvid, int page) throws FixingURLException {
        Long cid = 0L;
        String viewApi = "https://api.bilibili.com/x/web-interface/wbi/view?bvid=" + bvid;

        try {
            HttpRequest viewRequest = HttpRequest.newBuilder().uri(URI.create(viewApi)).header("User-Agent", "Mozilla/5.0").build();
            HttpResponse<String> viewResponse = CLIENT.send(viewRequest, HttpResponse.BodyHandlers.ofString());
            JsonObject viewJson = JsonParser.parseString(viewResponse.body()).getAsJsonObject();

            if (viewJson.get("code").getAsInt() == 0) {
                JsonObject viewData = viewJson.getAsJsonObject("data");

                JsonArray pagesArray = viewData.get("pages").getAsJsonArray();
                if (pagesArray != null && pagesArray.size() > 1) {
                    if (page > 0 && page <= pagesArray.size()) {
                        JsonObject currentPageData = pagesArray.get(page - 1).getAsJsonObject();
                        cid = currentPageData.get("cid").getAsLong();
                    } else {
                        JsonObject currentPageData = pagesArray.get(0).getAsJsonObject();
                        cid = currentPageData.get("cid").getAsLong();
                    }
                } else {
                    cid = viewData.get("cid").getAsLong();
                }
            }
        } catch (Exception e) {
            BilibiliMedia.LOGGER.error("[bilibili_media] 获取cid失败: {}", e.getMessage(), e);
            throw new FixingURLException(viewApi, e);
        }
        return cid;
    }

    public static URI getDirectUri(String bvid, Long cid) throws FixingURLException {
        String playApi = "https://api.bilibili.com/x/player/wbi/playurl?bvid=" + bvid + "&cid=" + cid + "&qn=116&type=&otype=json&platform=html5&high_quality=1";

        try {
            HttpRequest playUrlRequest = HttpRequest.newBuilder().uri(URI.create(playApi))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Referer", "https://www.bilibili.com/")
                    .header("Origin", "https://www.bilibili.com")
                    .build();

            HttpResponse<String> playUrlResponse = CLIENT.send(playUrlRequest, HttpResponse.BodyHandlers.ofString());
            JsonObject playUrlJson = JsonParser.parseString(playUrlResponse.body()).getAsJsonObject();

            if (playUrlJson.get("code").getAsInt() == 0) {
                JsonObject playUrlData = playUrlJson.getAsJsonObject("data");
                JsonArray durlArray = playUrlData.get("durl").getAsJsonArray();
                if (durlArray != null && durlArray.size() > 0) {
                    JsonObject firstDurl = durlArray.get(0).getAsJsonObject();
                    return URI.create(firstDurl.get("url").getAsString());
                }
            }
        } catch (Exception e) {
            BilibiliMedia.LOGGER.error("[bilibili_media] 获取直接链接失败: {}", e.getMessage(), e);
            throw new FixingURLException(playApi, e);
        }

        return null;
    }
}
