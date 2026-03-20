package dev.polaris_light.bilibili_media.config;

import dev.polaris_light.bilibili_media.BiliBiliMedia;
import dev.polaris_light.bilibili_media.util.SimpleFileServer;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;


@Config(name = BiliBiliMedia.ModID)
public class BiliBiliMediaConfig implements ConfigData {
    public boolean enable = true;
    public boolean enableCache = true;
    public boolean enableYhdm = false;
    public boolean enableYhdmCache = false;
    public boolean enableRangeRequests = false;
    public boolean clearOnStartAndStop = true;
    public int cacheMaxSize = 5;

    public static Screen getConfigScreen(Screen parent){
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.bilibili_media.config"))
                .setDefaultBackgroundTexture(ResourceLocation.fromNamespaceAndPath(
                        BiliBiliMedia.ModID,
                        "icon.png"
                ));
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("title.bilibili_media.config"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.bilibili_media.enable"), BiliBiliMedia.config.enable)
                .setSaveConsumer(x->BiliBiliMedia.config.enable = x)
                .setDefaultValue(true)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.bilibili_media.enable_cache"), BiliBiliMedia.config.enableCache)
                .setSaveConsumer(x->BiliBiliMedia.config.enableCache = x)
                .setTooltip(Component.translatable("config.bilibili_media.enable_cache.tip"))
                .setDefaultValue(true)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.bilibili_media.enable_yhdm"), BiliBiliMedia.config.enableYhdm)
                .setSaveConsumer(x->BiliBiliMedia.config.enableYhdm = x)
                .setDefaultValue(true)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.bilibili_media.enable_yhdm_cache"), BiliBiliMedia.config.enableYhdmCache)
                .setSaveConsumer(x->BiliBiliMedia.config.enableYhdmCache = x)
                .setTooltip(Component.translatable("config.bilibili_media.enable_yhdm_cache.tip"))
                .setDefaultValue(true)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.bilibili_media.enable_range_requests"), BiliBiliMedia.config.enableRangeRequests)
                .setSaveConsumer(x->BiliBiliMedia.config.enableRangeRequests = x)
                .setTooltip(Component.translatable("config.bilibili_media.enable_range_requests.tip"))
                .setDefaultValue(false)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.bilibili_media.clear_on_start_and_stop"), BiliBiliMedia.config.clearOnStartAndStop)
                .setSaveConsumer(x->BiliBiliMedia.config.clearOnStartAndStop = x)
                .setTooltip(Component.translatable("config.bilibili_media.clear_on_start_and_stop.tip"))
                .setDefaultValue(true)
                .requireRestart()
                .build());
        general.addEntry(entryBuilder.startIntField(Component.translatable("config.bilibili_media.cache_max_size"), BiliBiliMedia.config.cacheMaxSize)
                .setSaveConsumer(x->BiliBiliMedia.config.cacheMaxSize = x)
                .setTooltip(Component.translatable("config.bilibili_media.cache_max_size.tip"))
                .setDefaultValue(5)
                .build());
        builder.setSavingRunnable(() -> {
            AutoConfig.getConfigHolder(BiliBiliMediaConfig.class).setConfig(BiliBiliMedia.config);
            AutoConfig.getConfigHolder(BiliBiliMediaConfig.class).save();
            BiliBiliMedia.config = AutoConfig.getConfigHolder(BiliBiliMediaConfig.class).getConfig();
            SimpleFileServer.enableRangeRequests = BiliBiliMedia.config.enableRangeRequests;
        });
        return builder.build();
    }
}
