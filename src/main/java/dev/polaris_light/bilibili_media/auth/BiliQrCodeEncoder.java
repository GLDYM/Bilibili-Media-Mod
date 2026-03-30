package dev.polaris_light.bilibili_media.auth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class BiliQrCodeEncoder {
    private BiliQrCodeEncoder() {
    }

    public static String toQrImageUrl(String qrContentUrl) {
        String encoded = URLEncoder.encode(qrContentUrl, StandardCharsets.UTF_8);
        return "https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=" + encoded;
    }
}
