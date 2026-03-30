package dev.polaris_light.bilibili_media.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.polaris_light.bilibili_media.BiliBiliMedia;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class BiliAuthService {
    public record LoginQr(String qrUrl, String qrKey) {}

    private static final BiliAuthService INSTANCE = new BiliAuthService();
    private static final int LOGIN_TIMEOUT_SECONDS = 180;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private BiliAuthService() {
    }

    public static BiliAuthService getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<LoginQr> requestQrAsync() {
        return CompletableFuture.supplyAsync(this::requestQr);
    }

    public CompletableFuture<String> loginByQrAsync(java.util.function.Consumer<String> onQrUrl) {
        return requestQrAsync().thenCompose(qr -> CompletableFuture.supplyAsync(() -> {
            onQrUrl.accept(qr.qrUrl());
            waitForQrLogin(qr.qrKey());
            return checkLoginStatus();
        })).exceptionally(err -> "登录失败: " + getRootMessage(err));
    }

    public String checkLoginStatus() {
        String cookie = BiliCookieStore.getCookie();
        if (cookie == null || cookie.isBlank()) {
            return "未登录（无本地 Cookie）";
        }

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.bilibili.com/x/web-interface/nav"))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET();
            BiliCookieStore.withCookie(builder);

            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

            if (json.get("code").getAsInt() == 0 && json.getAsJsonObject("data").get("isLogin").getAsBoolean()) {
                JsonObject data = json.getAsJsonObject("data");
                String uname = data.get("uname").getAsString();
                boolean vip = data.has("vipStatus") && data.get("vipStatus").getAsInt() == 1;
                return "已登录: " + (vip ? uname + " [大会员]" : uname);
            }

            BiliCookieStore.clearCookie();
            return "登录已失效";
        } catch (Exception e) {
            BiliBiliMedia.LOGGER.error("[bilibili_media] 检查登录状态失败", e);
            return "检查登录状态失败: " + e.getMessage();
        }
    }

    public void logout() {
        BiliCookieStore.clearCookie();
    }

    private LoginQr requestQr() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://passport.bilibili.com/x/passport-login/web/qrcode/generate?source=main-fe-header"))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (json.get("code").getAsInt() != 0) {
                throw new RuntimeException("生成二维码失败: " + json);
            }

            JsonObject data = json.getAsJsonObject("data");
            String url = data.get("url").getAsString();
            String key = data.get("qrcode_key").getAsString();
            return new LoginQr(url, key);
        } catch (Exception e) {
            throw new RuntimeException("请求二维码失败", e);
        }
    }

    private void waitForQrLogin(String qrKey) {
        long deadline = System.currentTimeMillis() + LOGIN_TIMEOUT_SECONDS * 1000L;

        while (System.currentTimeMillis() < deadline) {
            try {
                String pollUrl = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=" + qrKey + "&source=main-fe-header";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(pollUrl))
                        .header("User-Agent", "Mozilla/5.0")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                if (root.get("code").getAsInt() != 0) {
                    throw new RuntimeException("轮询二维码失败: " + root);
                }

                JsonObject data = root.getAsJsonObject("data");
                int code = data.get("code").getAsInt();

                if (code == 0) {
                    List<String> cookieHeaders = response.headers().allValues("Set-Cookie");
                    if (cookieHeaders.isEmpty()) {
                        throw new RuntimeException("登录成功但未返回 Cookie");
                    }

                    String cookies = cookieHeaders.stream()
                            .map(header -> header.split(";", 2)[0])
                            .distinct()
                            .collect(Collectors.joining("; "));
                    BiliCookieStore.saveCookie(cookies);
                    return;
                }

                if (code == 86101 || code == 86090) {
                    Thread.sleep(1500L);
                    continue;
                }

                if (code == 86038) {
                    throw new RuntimeException("二维码已过期，请重新执行登录命令");
                }

                throw new RuntimeException("扫码失败: " + data);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("登录流程被中断", e);
            } catch (Exception e) {
                throw new RuntimeException("扫码登录失败", e);
            }
        }

        throw new RuntimeException("登录超时，请重试");
    }

    private static String getRootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.toString() : current.getMessage();
    }
}
