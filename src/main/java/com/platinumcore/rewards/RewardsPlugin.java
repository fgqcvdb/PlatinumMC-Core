package com.platinumcore.rewards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class RewardsPlugin extends JavaPlugin implements CommandExecutor, Listener {

    private static final String INVENTORY_TITLE = ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Rewards";
    private static final int INVENTORY_SIZE = 27;
    private static final int DAILY_REWARD_SLOT = 11;
    private static final int WEEKLY_REWARD_SLOT = 13;
    private static final int STREAK_REWARD_SLOT = 15;
    private static final int CLOSE_BUTTON_SLOT = 22;
    private Inventory rewardsInventory;

    @Override
    public void onEnable() {
        rewardsInventory = Bukkit.createInventory(null, INVENTORY_SIZE, INVENTORY_TITLE);
        initialiseInventory();

        Objects.requireNonNull(getCommand("rewards"), "Command /rewards not defined in plugin.yml")
                .setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("RewardsPlugin enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("RewardsPlugin disabled.");
    }

    private void initialiseInventory() {
        if (rewardsInventory == null || rewardsInventory.getSize() != INVENTORY_SIZE) {
            rewardsInventory = Bukkit.createInventory(null, INVENTORY_SIZE, INVENTORY_TITLE);
        } else {
            rewardsInventory.clear();
        }

        ItemStack fillerPane = createFillerPane();
        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            if (isBorderSlot(slot)) {
                rewardsInventory.setItem(slot, fillerPane.clone());
            }
        }

        rewardsInventory.setItem(DAILY_REWARD_SLOT, createItem(
                Material.SUNFLOWER,
                ChatColor.GOLD + "Daily Reward",
                Arrays.asList(
                        ChatColor.YELLOW + "→ " + ChatColor.GRAY + "Claim once every day",
                        ChatColor.GREEN + "+250 Coins " + ChatColor.GRAY + "| " + ChatColor.AQUA + "+500 XP"
                )));

        rewardsInventory.setItem(WEEKLY_REWARD_SLOT, createItem(
                Material.CLOCK,
                ChatColor.AQUA + "Weekly Reward",
                Arrays.asList(
                        ChatColor.YELLOW + "→ " + ChatColor.GRAY + "Play at least once per week",
                        ChatColor.GREEN + "1x Epic Crate Key",
                        ChatColor.GRAY + "Renews every Sunday"
                )));

        rewardsInventory.setItem(STREAK_REWARD_SLOT, createItem(
                Material.RED_CANDLE,
                ChatColor.RED + "Streak Reward",
                Arrays.asList(
                        ChatColor.YELLOW + "→ " + ChatColor.GRAY + "Maintain a 7-day login streak",
                        ChatColor.LIGHT_PURPLE + "Unlock exclusive cosmetics"
                )));

        rewardsInventory.setItem(CLOSE_BUTTON_SLOT, createItem(
                Material.BARRIER,
                ChatColor.RED + "Close Menu",
                Collections.singletonList(ChatColor.GRAY + "Return to the game")));
    }

    private ItemStack createItem(Material material, String displayName, List<String> loreLines) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(new ArrayList<>(loreLines));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can open the rewards GUI.");
            return true;
        }

        initialiseInventory();
        player.openInventory(createInventoryView());
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isRewardsInventory(event.getView())) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() == null) {
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getView().getTopInventory().getSize()) {
            return;
        }

        handleRewardSelection(player, rawSlot);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isRewardsInventory(event.getView())) {
            event.setCancelled(true);
        }
    }

    private Inventory createInventoryView() {
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, INVENTORY_TITLE);
        if (rewardsInventory == null) {
            return inventory;
        }

        ItemStack[] template = rewardsInventory.getContents();
        ItemStack[] copy = new ItemStack[template.length];
        for (int i = 0; i < template.length; i++) {
            copy[i] = template[i] == null ? null : template[i].clone();
        }
        inventory.setContents(copy);
        return inventory;
    }

    private ItemStack createFillerPane() {
        return createItem(
                Material.GRAY_STAINED_GLASS_PANE,
                ChatColor.DARK_GRAY + " ",
                Collections.singletonList(ChatColor.DARK_GRAY + " "));
    }

    private boolean isBorderSlot(int slot) {
        int row = slot / 9;
        int column = slot % 9;
        return row == 0 || row == 2 || column == 0 || column == 8;
    }

    private boolean isRewardsInventory(InventoryView view) {
        String providedTitle = ChatColor.stripColor(view.getTitle());
        String expectedTitle = ChatColor.stripColor(INVENTORY_TITLE);
        return Objects.equals(providedTitle, expectedTitle);
    }

    private void handleRewardSelection(Player player, int slot) {
        switch (slot) {
            case DAILY_REWARD_SLOT -> {
                player.sendMessage(ChatColor.GREEN + "Daily reward claimed! (feature coming soon)");
                player.closeInventory();
            }
            case WEEKLY_REWARD_SLOT -> {
                player.sendMessage(ChatColor.GREEN + "Weekly reward claimed! (feature coming soon)");
                player.closeInventory();
            }
            case STREAK_REWARD_SLOT -> {
                player.sendMessage(ChatColor.GREEN + "Streak reward claimed! (feature coming soon)");
                player.closeInventory();
            }
            case CLOSE_BUTTON_SLOT -> player.closeInventory();
            default -> {
            }
        }
    }
}
