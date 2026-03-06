package dev.polaris_light.bilibili_media;

import dev.polaris_light.bilibili_media.config.BiliBiliMediaConfig;
import dev.polaris_light.bilibili_media.util.BilibiliMediaUtil;
import dev.polaris_light.bilibili_media.util.SimpleFileServer;
import com.mojang.logging.LogUtils;
import com.sun.net.httpserver.HttpServer;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;


@Mod(BiliBiliMedia.MODID)
public class BiliBiliMedia
{

    public static final String MODID = "bilibili_media";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static BiliBiliMediaConfig config;
    public static HttpServer server;

    public BiliBiliMedia(FMLJavaModLoadingContext context) {
        // 1. 注册配置 (AutoConfig + GsonConfigSerializer)
        AutoConfig.register(BiliBiliMediaConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(BiliBiliMediaConfig.class).getConfig();

        // 2. 注册 ConfigScreen (Forge 1.20.1 写法)
        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (mc, parent) -> BiliBiliMediaConfig.getConfigScreen(parent))
        );

        // 3. 启动本地 SimpleFileServer
        server = SimpleFileServer.startServer();
        if (server != null) {
            LOGGER.info("BilibiliMedia SimpleFileServer 已启动");
        } else {
            LOGGER.warn("BilibiliMedia SimpleFileServer 启动失败，本地视频缓存功能已禁用");
        }

        // 4. 读取缓存 JSON
        try {
            BilibiliMediaUtil.loadJson();
        } catch (Exception e) {
            LOGGER.error("加载缓存 JSON 失败", e);
        }

        // 5. 清理过期缓存
        if (config.clearOnStartAndStop) {
            try {
                BilibiliMediaUtil.clearCache(config.cacheMaxSize);
            } catch (Exception e) {
                LOGGER.error("清理缓存失败", e);
            }
        }
    }
}
