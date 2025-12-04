package com.example.platinumduel;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.Objects;

public class PlatinumDuelPlugin extends JavaPlugin {

    private DuelManager duelManager;

    @Override
    public void onEnable() {
        this.duelManager = new DuelManager(Duration.ofSeconds(30));

        DuelCommand duelCommand = new DuelCommand(duelManager);
        PluginCommand command = Objects.requireNonNull(getCommand("duel"), "Duel command not defined in plugin.yml");
        command.setExecutor(duelCommand);
        command.setTabCompleter(duelCommand);

        Bukkit.getPluginManager().registerEvents(new DuelListener(duelManager), this);

        getLogger().info("PlatinumDuel enabled. Players must mutually agree before PvP.");
    }

    @Override
    public void onDisable() {
        getLogger().info("PlatinumDuel disabled.");
    }

    public DuelManager getDuelManager() {
        return duelManager;
    }
}



