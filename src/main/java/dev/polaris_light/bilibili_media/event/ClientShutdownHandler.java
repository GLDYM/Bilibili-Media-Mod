package dev.polaris_light.bilibili_media.event;

import dev.polaris_light.bilibili_media.BiliBiliMedia;
import dev.polaris_light.bilibili_media.util.BilibiliMediaUtil;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = BiliBiliMedia.ModID)
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
