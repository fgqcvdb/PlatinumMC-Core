package com.example.platinumduel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class DuelCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUB_COMMANDS = Arrays.asList("accept", "agree", "decline", "deny", "cancel");

    private final DuelManager duelManager;

    public DuelCommand(DuelManager duelManager) {
        this.duelManager = duelManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "&cOnly players can use /" + label + ".");
            return true;
        }

        if (!player.hasPermission("platinumduel.duel")) {
            Messages.send(player, "&cYou do not have permission to duel players.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player, label);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "accept", "agree" -> handleAccept(player);
            case "decline", "deny" -> handleDecline(player);
            case "cancel" -> handleCancel(player);
            default -> handleRequest(player, args[0]);
        }

        return true;
    }

    private void handleRequest(Player challenger, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            Messages.send(challenger, "&cThat player is not online.");
            return;
        }

        DuelManager.RequestResult result = duelManager.requestDuel(challenger, target);
        switch (result) {
            case SENT -> {
                long timeout = Math.max(1, duelManager.getRequestTimeoutSeconds());
                Messages.send(challenger, "&aYou challenged &f" + target.getName() + " &ato a duel. They have &f" + timeout + " seconds &ato respond.");
                Messages.send(target, "&e" + challenger.getName() + " challenged you to a duel! Type &a/duel accept &eto fight or &c/duel decline &eto refuse.");
            }
            case SELF -> Messages.send(challenger, "&cYou cannot duel yourself.");
            case ALREADY_PENDING -> Messages.send(challenger, "&eYou already have a pending request with that player.");
            case TARGET_HAS_PENDING -> Messages.send(challenger, "&eThat player already has a pending duel request.");
            case CHALLENGER_HAS_OTHER_PENDING -> Messages.send(challenger, "&eYou already have a pending duel request with another player.");
            case CHALLENGER_BUSY -> Messages.send(challenger, "&eYou are already in an active duel.");
            case TARGET_BUSY -> Messages.send(challenger, "&eThat player is already in an active duel.");
        }
    }

    private void handleAccept(Player player) {
        DuelManager.AcceptResult result = duelManager.acceptRequest(player);
        switch (result) {
            case ACCEPTED -> {
                Optional<Player> opponent = duelManager.getOpponent(player.getUniqueId()).map(Bukkit::getPlayer);
                String opponentName = opponent.filter(Player::isOnline).map(Player::getName).orElse("your opponent");
                Messages.send(player, "&aYou accepted a duel with &f" + opponentName + "&a. Fight!");
                opponent.filter(Player::isOnline)
                        .ifPresent(op -> Messages.send(op, "&e" + player.getName() + " accepted your duel request!"));
            }
            case NO_REQUEST -> Messages.send(player, "&cYou do not have any pending duel requests.");
            case CHALLENGER_OFFLINE -> Messages.send(player, "&cThat player is no longer online.");
            case ALREADY_IN_DUEL -> Messages.send(player, "&cEither you or the challenger is already in a duel.");
        }
    }

    private void handleDecline(Player player) {
        DuelManager.DeclineResult result = duelManager.declineRequest(player);
        switch (result) {
            case DECLINED -> Messages.send(player, "&aYou declined the duel request.");
            case NO_REQUEST -> Messages.send(player, "&cYou do not have any pending duel requests.");
        }
    }

    private void handleCancel(Player player) {
        boolean removed = duelManager.removePendingRequests(player.getUniqueId());
        if (removed) {
            Messages.send(player, "&aYou cleared your pending duel requests.");
        } else {
            Messages.send(player, "&eYou do not have any pending duel requests to cancel.");
        }
    }

    private void sendUsage(Player player, String label) {
        Messages.send(player, "&fUsage: ");
        player.sendMessage("  " + ChatColor.GRAY + "/" + label + " <player> " + ChatColor.WHITE + "- Challenge a player to a duel.");
        player.sendMessage("  " + ChatColor.GRAY + "/" + label + " accept " + ChatColor.WHITE + "- Accept the pending duel request.");
        player.sendMessage("  " + ChatColor.GRAY + "/" + label + " decline " + ChatColor.WHITE + "- Decline the pending duel request.");
        player.sendMessage("  " + ChatColor.GRAY + "/" + label + " cancel " + ChatColor.WHITE + "- Cancel your outgoing duel request.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            List<String> completions = new ArrayList<>();
            for (String sub : SUB_COMMANDS) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(input)) {
                    completions.add(online.getName());
                }
            }
            return completions;
        }
        return Collections.emptyList();
    }
}
