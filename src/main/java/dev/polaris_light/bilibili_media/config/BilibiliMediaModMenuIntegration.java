package dev.polaris_light.bilibili_media.config;

import dev.polaris_light.bilibili_media.BilibiliMedia;
import dev.polaris_light.bilibili_media.util.BilibiliMediaUtil;
import dev.polaris_light.bilibili_media.util.SimpleFileServer;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BilibiliMediaModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.translatable("title.bilibili_media.config"))
                    .setDefaultBackgroundTexture(new Identifier(BilibiliMedia.ModID,  BilibiliMediaUtil.isGLY() ? "textures/gui/gly091020.png" : "icon.png"));
            ConfigCategory general = builder.getOrCreateCategory(Text.translatable("title.bilibili_media.config"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            general.addEntry(entryBuilder.startBooleanToggle(Text.translatable("config.bilibili_media.enable"), BilibiliMedia.config.enable)
                    .setSaveConsumer(x->BilibiliMedia.config.enable = x)
                    .setTooltip(Text.translatable("config.bilibili_media.enable.tip"))
                    .setDefaultValue(true)
                    .build());
            general.addEntry(entryBuilder.startBooleanToggle(Text.translatable("config.bilibili_media.enable_cache"), BilibiliMedia.config.enableCache)
                    .setSaveConsumer(x->BilibiliMedia.config.enableCache = x)
                    .setTooltip(Text.translatable("config.bilibili_media.enable_cache.tip"))
                    .setDefaultValue(true)
                    .build());
            general.addEntry(entryBuilder.startBooleanToggle(Text.translatable("config.bilibili_media.enable_yhdm"), BilibiliMedia.config.enableYhdm)
                    .setSaveConsumer(x->BilibiliMedia.config.enableYhdm = x)
                    .setTooltip(Text.translatable("config.bilibili_media.enable_yhdm.tip"))
                    .setDefaultValue(false)
                    .build());
            general.addEntry(entryBuilder.startBooleanToggle(Text.translatable("config.bilibili_media.enable_yhdm_cache"), BilibiliMedia.config.enableYhdmCache)
                    .setSaveConsumer(x->BilibiliMedia.config.enableYhdmCache = x)
                    .setTooltip(Text.translatable("config.bilibili_media.enable_yhdm_cache.tip"))
                    .setDefaultValue(false)
                    .build());
            general.addEntry(entryBuilder.startBooleanToggle(Text.translatable("config.bilibili_media.enable_range_requests"), BilibiliMedia.config.enableRangeRequests)
                    .setSaveConsumer(x->BilibiliMedia.config.enableRangeRequests = x)
                    .setTooltip(Text.translatable("config.bilibili_media.enable_range_requests.tip"))
                    .setDefaultValue(false)
                    .build());
            general.addEntry(entryBuilder.startBooleanToggle(Text.translatable("config.bilibili_media.clear_on_start_and_stop"), BilibiliMedia.config.clearOnStartAndStop)
                    .setSaveConsumer(x->BilibiliMedia.config.clearOnStartAndStop = x)
                    .setTooltip(Text.translatable("config.bilibili_media.clear_on_start_and_stop.tip"))
                    .setDefaultValue(true)
                    .requireRestart()
                    .build());
            general.addEntry(entryBuilder.startIntField(Text.translatable("config.bilibili_media.cache_max_size"), BilibiliMedia.config.cacheMaxSize)
                    .setSaveConsumer(x->BilibiliMedia.config.cacheMaxSize = x)
                    .setTooltip(Text.translatable("config.bilibili_media.cache_max_size.tip"))
                    .setDefaultValue(5)
                    .build());
            builder.setSavingRunnable(() -> {
                AutoConfig.getConfigHolder(BiliBiliMediaConfig.class).setConfig(BilibiliMedia.config);
                AutoConfig.getConfigHolder(BiliBiliMediaConfig.class).save();
                BilibiliMedia.config = AutoConfig.getConfigHolder(BiliBiliMediaConfig.class).getConfig();
                SimpleFileServer.enableRangeRequests = BilibiliMedia.config.enableRangeRequests;
            });
            return builder.build();
        };
    }
}
