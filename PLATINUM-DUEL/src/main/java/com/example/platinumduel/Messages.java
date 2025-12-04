package com.example.platinumduel;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
/**
 * Utility methods for sending chat messages with a consistent prefix.
 */
public final class Messages {

    private static final String PREFIX = ChatColor.DARK_GRAY + "[" + ChatColor.AQUA + "Duel" + ChatColor.DARK_GRAY + "] " + ChatColor.RESET;

    private Messages() {
        throw new IllegalStateException("Utility class");
    }

    public static String prefix(String message) {
        return PREFIX + ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(prefix(message));
    }
}

