package dev.polaris_light.bilibili_media.config;

import dev.polaris_light.bilibili_media.BilibiliMedia;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = BilibiliMedia.ModID)
public class BiliBiliMediaConfig implements ConfigData {
    public boolean enable = true;
    public boolean enableCache = true;
    public boolean enableYhdm = false;
    public boolean enableYhdmCache = false;
    public boolean enableRangeRequests = false;
    public boolean clearOnStartAndStop = true;
    public int cacheMaxSize = 5;
}
