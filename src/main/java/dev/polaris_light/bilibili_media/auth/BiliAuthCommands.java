package dev.polaris_light.bilibili_media.auth;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.polaris_light.bilibili_media.util.BilibiliMediaUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class BiliAuthCommands {
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
            sendMessage(Component.literal("请用手机扫描二维码登录 bilibili:").withStyle(ChatFormatting.AQUA));
            String qrImageUrl = BiliQrCodeEncoder.toQrImageUrl(qrUrl);
            Component qrHover = buildQrHoverComponent(qrUrl);
            Style hoverStyle = Style.EMPTY
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, qrHover))
                .withColor(ChatFormatting.GREEN)
                .withUnderlined(true);

            Style fallbackStyle = Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, qrImageUrl))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击打开二维码图片").withStyle(ChatFormatting.GRAY)))
                .withColor(ChatFormatting.YELLOW)
                .withUnderlined(true);

            sendMessage(Component.empty()
                    .append(Component.literal("[悬停查看二维码]").setStyle(hoverStyle))
                    .append(Component.literal(" "))
                    .append(Component.literal("[备用打开二维码]").setStyle(fallbackStyle)));
        }).thenAccept(result -> {
            if (result.startsWith("已登录:")) {
                sendMessage(buildLoginSuccessComponent(result));
            } else {
                sendMessage(colorPrefix(result, ChatFormatting.RED));
            }
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

    private static Component buildQrHoverComponent(String qrUrl) {
        MutableComponent hover = Component.empty();
        hover.append(Component.literal("扫描二维码登录\n").withStyle(ChatFormatting.GRAY));

        java.util.List<Component> lines = BiliQrCodeEncoder.toChatQrComponents(qrUrl);
        for (int i = 0; i < lines.size(); i++) {
            hover.append(lines.get(i));
            if (i < lines.size() - 1) {
                hover.append(Component.literal("\n"));
            }
        }
        return hover;
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
