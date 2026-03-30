package dev.polaris_light.bilibili_media.auth;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.polaris_light.bilibili_media.util.BilibiliMediaUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

public final class BiliAuthCommands {
    private static BiliAuthQrScreen currentQrScreen;

    private BiliAuthCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> bilimediaNode = Commands.literal("bilimedia")
                .then(Commands.literal("login")
                        .executes(ctx -> {
                            startLogin();
                            return 1;
                        })
                        .then(Commands.literal("force").executes(ctx -> {
                            startLogin();
                            return 1;
                        }))
                )
                .then(Commands.literal("account").executes(ctx -> {
                    String status = BiliAuthService.getInstance().checkLoginStatus();
                    if (status.startsWith("已登录:")) {
                        sendMessage(buildLoginSuccessComponent(status));
                    } else if (status.startsWith("检查登录状态失败")) {
                        sendMessage(colorPrefix(status, ChatFormatting.RED));
                    } else {
                        sendMessage(Component.literal(status).withStyle(ChatFormatting.YELLOW));
                    }
                    return 1;
                }))
                .then(Commands.literal("logout").executes(ctx -> {
                    BiliAuthService.getInstance().logout();
                    sendMessage(Component.literal("已清除 bilibili 本地登录信息").withStyle(ChatFormatting.YELLOW));
                    return 1;
                }))
                .then(Commands.literal("cache")
                        .then(Commands.literal("remove")
                                .then(Commands.argument("key", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            String remain = builder.getRemainingLowerCase();
                                            for (String key : BilibiliMediaUtil.getCacheKeys()) {
                                                String quotedKey = "\"" + key + "\"";
                                                if (quotedKey.toLowerCase().startsWith(remain) || key.toLowerCase().startsWith(remain)) {
                                                    builder.suggest(quotedKey);
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            String key = StringArgumentType.getString(ctx, "key");
                                            BilibiliMediaUtil.CacheRemoveResult result = BilibiliMediaUtil.removeCacheByKey(key);
                                            if (!result.found()) {
                                                sendMessage(Component.literal("未找到缓存键: " + key).withStyle(ChatFormatting.RED));
                                                return 0;
                                            }

                                            if (result.fileDeleted()) {
                                                sendMessage(Component.literal("已清除缓存: " + key + " -> " + result.fileName()).withStyle(ChatFormatting.GREEN));
                                            } else {
                                                sendMessage(Component.literal("已移除缓存映射，但文件删除失败: " + result.fileName()).withStyle(ChatFormatting.YELLOW));
                                            }
                                            return 1;
                                        })
                                )
                        )
                    );

        dispatcher.register(bilimediaNode);
    }

    private static void startLogin() {
        BiliAuthService.getInstance().loginByQrAsync(qrUrl -> {
            String qrImageUrl = BiliQrCodeEncoder.toQrImageUrl(qrUrl);
            openQrScreen(qrImageUrl);
            sendMessage(Component.literal("请扫描屏幕上显示的二维码以登陆。").withStyle(ChatFormatting.AQUA));
            sendMessage(Component.literal("如果无法扫描，请点击下方按钮在浏览器内打开。").withStyle(ChatFormatting.AQUA));
            Style style = Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, qrImageUrl))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击打开二维码图片").withStyle(ChatFormatting.GRAY)))
                .withColor(ChatFormatting.GREEN)
                .withUnderlined(true);
            sendMessage(Component.literal("[点我在浏览器打开]").setStyle(style));
        }).thenAccept(result -> {
            closeQrScreenIfOpen();
            if (result.startsWith("已登录:")) {
                sendMessage(buildLoginSuccessComponent(result));
            } else {
                sendMessage(colorPrefix(result, ChatFormatting.RED));
            }
        });
    }

    private static void openQrScreen(String qrImageUrl) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            currentQrScreen = new BiliAuthQrScreen(qrImageUrl);
            mc.setScreen(currentQrScreen);
        });
    }

    private static void closeQrScreenIfOpen() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            Screen current = mc.screen;
            if (currentQrScreen != null && current == currentQrScreen) {
                mc.setScreen(null);
            }
            currentQrScreen = null;
        });
    }

    private static Component buildLoginSuccessComponent(String status) {
        int index = status.indexOf(':');
        if (index < 0 || index == status.length() - 1) {
            return Component.literal(status).withStyle(ChatFormatting.GREEN);
        }

        String prefix = status.substring(0, index + 1);
        String rest = status.substring(index + 1).trim();
        String vipTag = "[大会员]";

        if (rest.endsWith(vipTag)) {
            String username = rest.substring(0, rest.length() - vipTag.length()).trim();
            return Component.empty()
                    .append(Component.literal(prefix).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" " + username))
                    .append(Component.literal(" " + vipTag).withStyle(ChatFormatting.GOLD));
        }

        return Component.empty()
                .append(Component.literal(prefix).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" " + rest));
    }

    private static Component colorPrefix(String text, ChatFormatting prefixColor) {
        int index = text.indexOf(':');
        if (index < 0 || index == text.length() - 1) {
            return Component.literal(text).withStyle(prefixColor);
        }

        return Component.empty()
                .append(Component.literal(text.substring(0, index + 1)).withStyle(prefixColor))
                .append(Component.literal(" " + text.substring(index + 1).trim()));
    }

    private static void sendMessage(String text) {
        sendMessage(Component.literal(text));
    }

    private static void sendMessage(Component text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.execute(() -> {
                if (mc.player != null) {
                    mc.player.displayClientMessage(text, false);
                }
            });
        }
    }
}
