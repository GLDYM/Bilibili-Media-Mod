package dev.polaris_light.bilibili_media.event;

import dev.polaris_light.bilibili_media.BiliBiliMedia;
import dev.polaris_light.bilibili_media.auth.BiliAuthCommands;
import dev.polaris_light.bilibili_media.util.SimpleFileServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = BiliBiliMedia.ModID, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        BiliAuthCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        SimpleFileServer.flushPendingMessage();
    }
}
