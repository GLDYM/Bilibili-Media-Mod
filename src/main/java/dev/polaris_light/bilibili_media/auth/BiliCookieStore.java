package dev.polaris_light.bilibili_media.auth;

import dev.polaris_light.bilibili_media.BiliBiliMedia;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class BiliCookieStore {
    private static final String COOKIE_KEY = "BILIBILI_COOKIE";
    private static final Path HOME_COOKIE_PATH = Path.of(System.getProperty("user.home"), ".bilimedia", "cookie.properties");
    private static final Path GAME_COOKIE_PATH = FMLPaths.GAMEDIR.get().resolve(".bilimedia").resolve("cookie.properties");

    private static volatile String cookie;
    private static volatile Path storagePath;

    private BiliCookieStore() {
    }

    public static synchronized void init() {
        storagePath = prepareStoragePath();
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(storagePath)) {
            props.load(in);
            cookie = props.getProperty(COOKIE_KEY, "").trim();
        } catch (IOException e) {
            BiliBiliMedia.LOGGER.warn("[bilibili_media] 读取 Cookie 失败", e);
            cookie = "";
        }
    }

    public static synchronized void saveCookie(@Nullable String value) {
        cookie = value == null ? "" : value.trim();
        if (storagePath == null) {
            storagePath = prepareStoragePath();
        }

        Properties props = new Properties();
        if (!cookie.isEmpty()) {
            props.setProperty(COOKIE_KEY, cookie);
        }

        try (OutputStream out = Files.newOutputStream(storagePath)) {
            props.store(out, "BiliBili Media Cookies");
        } catch (IOException e) {
            BiliBiliMedia.LOGGER.error("[bilibili_media] 保存 Cookie 失败", e);
        }
    }

    public static synchronized void clearCookie() {
        saveCookie("");
    }

    public static @Nullable String getCookie() {
        String local = cookie;
        return local == null || local.isBlank() ? null : local;
    }

    public static HttpRequest.Builder withCookie(HttpRequest.Builder builder) {
        String local = getCookie();
        if (local != null) {
            builder.header("Cookie", local);
        }
        return builder;
    }

    private static Path prepareStoragePath() {
        Path chosen = ensurePath(HOME_COOKIE_PATH);
        if (chosen != null) {
            return chosen;
        }

        chosen = ensurePath(GAME_COOKIE_PATH);
        if (chosen != null) {
            return chosen;
        }

        throw new RuntimeException("无法创建 Cookie 存储目录");
    }

    private static Path ensurePath(Path filePath) {
        try {
            Files.createDirectories(filePath.getParent());
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
            return filePath;
        } catch (IOException e) {
            BiliBiliMedia.LOGGER.warn("[bilibili_media] 无法使用 Cookie 路径: {}", filePath, e);
            return null;
        }
    }
}
