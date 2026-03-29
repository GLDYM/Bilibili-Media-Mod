package dev.polaris_light.bilibili_media;

import dev.polaris_light.bilibili_media.config.BiliBiliMediaConfig;
import dev.polaris_light.bilibili_media.util.BilibiliMediaUtil;
import dev.polaris_light.bilibili_media.util.SimpleFileServer;
import com.sun.net.httpserver.HttpServer;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class BilibiliMedia implements ClientModInitializer {
    public static final String ModID = "bilibili_media";
    public static final Logger LOGGER = LoggerFactory.getLogger(ModID);
    public static BiliBiliMediaConfig config;
    public static HttpServer server;
    public static final UUID _5112151111121 = UUID.fromString("91bd580f-5f17-4e30-872f-2e480dd9a220");
    @Override
    public void onInitializeClient() {
        AutoConfig.register(BiliBiliMediaConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(BiliBiliMediaConfig.class).getConfig();

        server = SimpleFileServer.startServer();
        if (server != null) {
            LOGGER.info("BilibiliMedia SimpleFileServer 已启动");
        } else {
            LOGGER.warn("BilibiliMedia SimpleFileServer 启动失败，本地视频缓存功能已禁用");
        }

        try {
            BilibiliMediaUtil.loadJson();
        } catch (Exception e) {
            LOGGER.error("加载缓存 JSON 失败", e);
        }

        if (config.clearOnStartAndStop) {
            try {
                BilibiliMediaUtil.clearCache(config.cacheMaxSize);
            } catch (Exception e) {
                LOGGER.error("清理缓存失败", e);
            }
        }

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                SimpleFileServer.flushPendingMessage());

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            if (config.clearOnStartAndStop) {
                try {
                    BilibiliMediaUtil.clearCache(config.cacheMaxSize);
                } catch (Exception e) {
                    LOGGER.error("清理缓存失败", e);
                }
            }
        });
    }
}
