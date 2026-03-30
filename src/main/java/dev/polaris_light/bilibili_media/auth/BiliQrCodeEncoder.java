package dev.polaris_light.bilibili_media.auth;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class BiliQrCodeEncoder {
    private BiliQrCodeEncoder() {
    }

    public static List<Component> toChatQrComponents(String qrContentUrl) {
        List<Component> lines = new ArrayList<>();
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = writer.encode(qrContentUrl, BarcodeFormat.QR_CODE, 1, 1, hints);
            for (int y = 0; y < matrix.getHeight(); y++) {
                MutableComponent line = Component.empty();
                for (int x = 0; x < matrix.getWidth(); x++) {
                    line.append(Component.literal(matrix.get(x, y) ? "⬛" : "⬜"));
                }
                lines.add(line);
            }
        } catch (WriterException e) {
            lines.add(Component.literal("二维码编码失败: " + e.getMessage()));
        }
        return lines;
    }

    public static String toQrImageUrl(String qrContentUrl) {
        String encoded = URLEncoder.encode(qrContentUrl, StandardCharsets.UTF_8);
        return "https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=" + encoded;
    }
}
