package com.finkkk.bilibili_media.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.watermedia.WaterMedia;
import org.watermedia.api.media.MediaAPI;
import org.watermedia.api.media.platform.IPlatform;
import com.finkkk.bilibili_media.util.BilibiliPatch;

import java.util.LinkedList;

@Mixin(MediaAPI.class)
public abstract class MediaAPIMixin {
    @Shadow(remap = false) @Final private static LinkedList<IPlatform> PLATFORMS;

    @Inject(method = "start", at = @At("TAIL"), remap = false)
    private static void appendBilibili(final WaterMedia instance, CallbackInfoReturnable<Boolean> cir){
        PLATFORMS.addFirst(new BilibiliPatch());
    }
}
