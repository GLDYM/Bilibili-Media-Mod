package dev.polaris_light.bilibili_media.event;

import dev.polaris_light.bilibili_media.BiliBiliMedia;
import dev.polaris_light.bilibili_media.auth.BiliAuthCommands;
import dev.polaris_light.bilibili_media.util.SimpleFileServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BiliBiliMedia.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
