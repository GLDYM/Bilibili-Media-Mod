package dev.polaris_light.bilibili_media.auth;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class BiliAuthQrScreen extends Screen {
    private static final int QR_SIZE = 240;

    private final String qrImageUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private CompletableFuture<NativeImage> loadFuture;
    private DynamicTexture qrTexture;
    private ResourceLocation qrTextureLocation;
    private int qrTextureWidth;
    private int qrTextureHeight;
    private String loadError;

    public BiliAuthQrScreen(String qrImageUrl) {
        super(Component.literal("Bilibili 登录二维码"));
        this.qrImageUrl = qrImageUrl;
    }

    @Override
    protected void init() {
        super.init();
        startLoadIfNeeded();
    }

    @Override
    public void removed() {
        if (loadFuture != null) {
            loadFuture.cancel(true);
            loadFuture = null;
        }
        releaseQrTexture();
        super.removed();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0xC0101010);

        int centerX = width / 2;
        int topY = Math.max(20, (height - QR_SIZE) / 2 - 36);
        int qrX = centerX - QR_SIZE / 2;
        int qrY = topY + 28;
        int panelLeft = qrX - 20;
        int panelTop = topY - 14;
        int panelRight = qrX + QR_SIZE + 20;
        int panelBottom = qrY + QR_SIZE + 30;

        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xB0101010);
        graphics.fill(panelLeft, panelTop, panelRight, panelTop + 1, 0x80FFFFFF);
        graphics.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, 0x80444444);

        drawCenteredNoShadow(graphics, title, centerX, topY, 0xFFFFFF);
        drawCenteredNoShadow(graphics, Component.literal("请使用哔哩哔哩 App 扫码登录"), centerX, topY + 14, 0x9FD9FF);

        if (loadError != null) {
            drawCenteredNoShadow(graphics, Component.literal(loadError), centerX, qrY + QR_SIZE / 2, 0xFF5555);
        } else if (qrTextureLocation != null) {
            graphics.fill(qrX - 4, qrY - 4, qrX + QR_SIZE + 4, qrY + QR_SIZE + 4, 0xFFFFFFFF);
            graphics.blit(qrTextureLocation, qrX, qrY, 0, 0.0F, 0.0F, QR_SIZE, QR_SIZE, qrTextureWidth, qrTextureHeight);
        } else {
            drawCenteredNoShadow(graphics, Component.literal("二维码加载中..."), centerX, qrY + QR_SIZE / 2, 0xAAAAAA);
        }

        drawCenteredNoShadow(graphics, Component.literal("ESC 关闭窗口，登录轮询将继续"), centerX, qrY + QR_SIZE + 12, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // no-op
    }

    private void startLoadIfNeeded() {
        if (loadFuture != null) {
            return;
        }

        loadFuture = CompletableFuture.supplyAsync(this::downloadQrImage)
                .whenComplete((image, error) -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.execute(() -> {
                        if (error != null) {
                            loadError = "二维码加载失败: " + rootMessage(error);
                            if (image != null) {
                                image.close();
                            }
                            return;
                        }

                        if (image == null) {
                            loadError = "二维码加载失败: 图片为空";
                            return;
                        }

                        registerQrTexture(image);
                    });
                });
    }

    private NativeImage downloadQrImage() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(qrImageUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("HTTP " + response.statusCode());
            }
            return NativeImage.read(response.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void registerQrTexture(NativeImage image) {
        releaseQrTexture();
        qrTextureWidth = image.getWidth();
        qrTextureHeight = image.getHeight();
        qrTexture = new DynamicTexture(image);
        qrTexture.setFilter(false, false);
        qrTexture.upload();
        qrTextureLocation = Minecraft.getInstance().getTextureManager().register("bilibili_media/login_qr", qrTexture);
    }

    private void releaseQrTexture() {
        if (qrTextureLocation != null && Minecraft.getInstance().getTextureManager() != null) {
            Minecraft.getInstance().getTextureManager().release(qrTextureLocation);
            qrTextureLocation = null;
        }
        if (qrTexture != null) {
            qrTexture.close();
            qrTexture = null;
        }
        qrTextureWidth = 0;
        qrTextureHeight = 0;
    }

    private void drawCenteredNoShadow(GuiGraphics graphics, Component text, int centerX, int y, int color) {
        int x = centerX - font.width(text) / 2;
        graphics.drawString(font, text, x, y, color, false);
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.toString() : current.getMessage();
    }
}
