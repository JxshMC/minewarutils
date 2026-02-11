package com.jxsh.misc;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;
import java.time.Instant;
import java.time.Duration;

public class ChatManager implements Listener {

    private final JxshMisc plugin;
    private boolean enabled;
    private boolean clickableLinks;
    private String linkFormat;
    private String defaultFormat;
    private final Map<String, String> groupFormats = new LinkedHashMap<>();

    private boolean chatMuted = false;
    private int slowChatSeconds = 0;
    private final Map<UUID, Instant> lastMessageTimes = new HashMap<>();

    private boolean mentionsEnabled;
    private String mentionFormat;
    private String mentionSound;
    private float mentionVolume;
    private float mentionPitch;

    public ChatManager(JxshMisc plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        this.enabled = plugin.getConfigManager().getConfig().getBoolean("chat.enabled", true);
        this.clickableLinks = plugin.getConfigManager().getConfig().getBoolean("chat.clickable-links", true);
        this.linkFormat = plugin.getConfigManager().getConfig().getString("chat.link-format",
                "<aqua><u>%link%</u></aqua>");
        this.defaultFormat = plugin.getConfigManager().getConfig().getString("chat.format",
                "<white>%player%</white>: <gray>%message%</gray>");

        groupFormats.clear();
        if (plugin.getConfigManager().getConfig().contains("chat.group-formats")) {
            dev.dejvokep.boostedyaml.block.implementation.Section section = plugin.getConfigManager().getConfig()
                    .getSection("chat.group-formats");
            if (section != null) {
                for (Object key : section.getKeys()) {
                    String group = key.toString();
                    String val = section.getString(group);
                    if (val != null && !val.isEmpty()) {
                        groupFormats.put(group.toLowerCase(), val);
                    }
                }
            }
        }

        this.mentionsEnabled = plugin.getConfigManager().getConfig().getBoolean("chat.mentions.enabled", true);
        this.mentionFormat = plugin.getConfigManager().getMessages().getString("mentions.format",
                "<yellow>@%player%</yellow>");
        this.mentionSound = plugin.getConfigManager().getConfig().getString("chat.mentions.sound",
                "minecraft:entity.villager.ambient");
        this.mentionVolume = plugin.getConfigManager().getConfig().getDouble("chat.mentions.volume", 1.0).floatValue();
        this.mentionPitch = plugin.getConfigManager().getConfig().getDouble("chat.mentions.pitch", 1.0).floatValue();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!enabled) {
            return;
        }

        Player player = event.getPlayer();

        // 1. Staff Chat Priority: Block global chat if staff chat is toggled
        if (plugin.isStaffChatToggled(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // 2. Muted Chat Check (Bypassed if player has permission)
        if (chatMuted && !plugin.hasPermission(player, "mutechat-bypass")) {
            player.sendMessage(
                    plugin.parseText(plugin.getConfigManager().getMessages().getString("chat-control.mute-active",
                            "<red>You cannot talk while the chat is muted!</red>"), player));
            event.setCancelled(true);
            return;
        }

        // 3. Slow Chat Check (Bypassed if player has permission)
        if (slowChatSeconds > 0 && !plugin.hasPermission(player, "slowchat-bypass")) {
            Instant now = Instant.now();
            Instant lastMsg = lastMessageTimes.get(player.getUniqueId());

            if (lastMsg != null) {
                long secondsPassed = Duration.between(lastMsg, now).getSeconds();
                if (secondsPassed < slowChatSeconds) {
                    long remaining = slowChatSeconds - secondsPassed;
                    String slowMsg = plugin.getConfigManager().getMessages().getString("chat-control.slow-mode-active",
                            "<red>Please wait <white>%seconds%s</white> before sending another message!</red>");
                    player.sendMessage(
                            plugin.parseText(slowMsg.replace("%seconds%", String.valueOf(remaining)), player));
                    event.setCancelled(true);
                    return;
                }
            }
            lastMessageTimes.put(player.getUniqueId(), now);
        }

        // Log to console to verify the event is firing
        plugin.getLogger().info("Processing chat for " + player.getName());

        // GLOBAL CHAT SYNC: Forward to Proxy
        // We do NOT cancel the event anymore. We let Paper handle local chat.
        // event.setCancelled(true); // <--- REMOVED

        // Send Plugin Message to Proxy
        // SubChannel: "GlobalChat"
        // Data: Sender Name, Message Content

        // We get the raw text from the event
        String rawMessage = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(event.message());

        com.google.common.io.ByteArrayDataOutput out = com.google.common.io.ByteStreams.newDataOutput();
        out.writeUTF("GlobalChat");
        out.writeUTF(player.getName()); // Sender Name
        out.writeUTF(rawMessage); // Message Content

        // Determine the format to use
        String formatToUse = defaultFormat;

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && !groupFormats.isEmpty()) {
            String lpGroup = PlaceholderAPI.setPlaceholders(player, "%luckperms_primary_group_name%");
            String vaultGroup = PlaceholderAPI.setPlaceholders(player, "%vault_group%");

            if (groupFormats.containsKey(lpGroup.toLowerCase())) {
                formatToUse = groupFormats.get(lpGroup.toLowerCase());
            } else if (groupFormats.containsKey(vaultGroup.toLowerCase())) {
                formatToUse = groupFormats.get(vaultGroup.toLowerCase());
            }
        }

        // Support PAPI and custom placeholders in the format string
        formatToUse = plugin.processStrings(formatToUse, player);
        formatToUse = plugin.translateLegacyToMiniMessage(formatToUse);

        if (clickableLinks) {
            rawMessage = wrapLinks(rawMessage);
        }

        Component formattedMessage = plugin.parseText(rawMessage, player);

        // Use native MiniMessage placeholders for style inheritance
        String finalFormatString = formatToUse.replace("%message%", "<chat_message>")
                .replace("%player%", "<chat_player>");

        TagResolver placeholders = TagResolver.builder()
                .resolver(Placeholder.component("chat_message", formattedMessage))
                .resolver(Placeholder.component("chat_player", player.displayName()))
                .build();

        Component finalComponent = MiniMessage.miniMessage().deserialize(finalFormatString, placeholders);

        event.renderer((source, sourceDisplayName, message, viewer) -> {
            if (mentionsEnabled && viewer instanceof Player viewerPlayer && plugin.hasPermission(player, "mentions")) {
                if (!plugin.isMentionEnabled(viewerPlayer.getUniqueId())) {
                    return finalComponent;
                }
                String viewerName = viewerPlayer.getName();
                String rawMsg = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(message);

                // Simple check if the viewer is mentioned in the message
                // Using case-insensitive contains for better UX
                if (rawMsg.toLowerCase().contains(viewerName.toLowerCase())) {
                    // Highlight the mention for this viewer
                    String highlightedMsg = rawMsg.replaceAll("(?i)" + java.util.regex.Pattern.quote(viewerName),
                            mentionFormat.replace("%player%", viewerName));

                    Component highlightedContent = plugin.parseText(highlightedMsg, player);

                    TagResolver mentionPlaceholders = TagResolver.builder()
                            .resolver(Placeholder.component("chat_message", highlightedContent))
                            .resolver(Placeholder.component("chat_player", Component.text(player.getName())))
                            .build();

                    // Play sound to the mentioned player
                    viewerPlayer.playSound(viewerPlayer.getLocation(), mentionSound, mentionVolume, mentionPitch);

                    return MiniMessage.miniMessage().deserialize(finalFormatString, mentionPlaceholders);
                }
            }
            return finalComponent;
        });

        player.sendPluginMessage(plugin, "minewar:globalchat", out.toByteArray());
    }

    public boolean isChatMuted() {
        return chatMuted;
    }

    public void setChatMuted(boolean chatMuted) {
        this.chatMuted = chatMuted;
    }

    public int getSlowChatSeconds() {
        return slowChatSeconds;
    }

    public void setSlowChatSeconds(int slowChatSeconds) {
        this.slowChatSeconds = slowChatSeconds;
    }

    private String wrapLinks(String text) {
        if (text == null || text.isEmpty())
            return text;
        // Simple regex for URLs starting with http://, https://, or www.
        String urlRegex = "(https?://\\S+|www\\.\\S+)";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(urlRegex,
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(text);

        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            sb.append(text, lastEnd, matcher.start());
            String url = matcher.group();
            String link = url;
            if (url.toLowerCase().startsWith("www.")) {
                link = "http://" + url;
            }
            sb.append("<click:open_url:'").append(link).append("'>")
                    .append("<hover:show_text:'<gray>Click to open link</gray>'>")
                    .append(linkFormat.replace("%link%", url))
                    .append("</hover></click>");
            lastEnd = matcher.end();
        }
        sb.append(text.substring(lastEnd));
        return sb.toString();
    }
}
