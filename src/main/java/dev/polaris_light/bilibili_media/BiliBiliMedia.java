package dev.polaris_light.bilibili_media;

import dev.polaris_light.bilibili_media.config.BiliBiliMediaConfig;
import dev.polaris_light.bilibili_media.auth.BiliCookieStore;
import dev.polaris_light.bilibili_media.util.BilibiliMediaUtil;
import dev.polaris_light.bilibili_media.util.SimpleFileServer;
import com.sun.net.httpserver.HttpServer;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Mod(BiliBiliMedia.ModID)
public class BiliBiliMedia
{

    public static final String ModID = "bilibili_media";
    public static final Logger LOGGER = LoggerFactory.getLogger(ModID);

    public static BiliBiliMediaConfig config;
    public static HttpServer server;

    public BiliBiliMedia(ModContainer container) {
        AutoConfig.register(BiliBiliMediaConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(BiliBiliMediaConfig.class).getConfig();
        BiliCookieStore.init();
        container.registerExtensionPoint(IConfigScreenFactory.class, (mc, parent) ->
                BiliBiliMediaConfig.getConfigScreen(parent));

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
    }
}
