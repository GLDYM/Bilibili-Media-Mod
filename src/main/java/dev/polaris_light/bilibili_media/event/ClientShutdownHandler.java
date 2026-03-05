package dev.polaris_light.bilibili_media.event;

import dev.polaris_light.bilibili_media.BiliBiliMedia;
import dev.polaris_light.bilibili_media.util.BilibiliMediaUtil;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BiliBiliMedia.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientShutdownHandler {

    @SubscribeEvent
    public static void onGameShuttingDown(GameShuttingDownEvent event) {
        if (BiliBiliMedia.config.clearOnStartAndStop) {
            try {
                BilibiliMediaUtil.clearCache(BiliBiliMedia.config.cacheMaxSize);
            } catch (Exception e) {
                BiliBiliMedia.LOGGER.error("清理缓存失败", e);
            }
        }
    }
}
