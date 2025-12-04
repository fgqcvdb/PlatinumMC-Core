package com.example.platinumduel;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuelListener implements Listener {

    private final DuelManager duelManager;
    private final Map<UUID, Long> lastWarning = new HashMap<>();
    private final long warningCooldownMillis = 2_000L;

    public DuelListener(DuelManager duelManager) {
        this.duelManager = duelManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = findAttackingPlayer(event.getDamager());
        if (attacker == null || attacker.equals(victim)) {
            return;
        }

        if (!duelManager.canDamage(attacker, victim)) {
            event.setCancelled(true);

            long now = System.currentTimeMillis();
            Long lastSent = lastWarning.get(attacker.getUniqueId());
            if (lastSent == null || (now - lastSent) >= warningCooldownMillis) {
                lastWarning.put(attacker.getUniqueId(), now);
                Messages.send(attacker, "&cYou must have an accepted duel with &f" + victim.getName() + " &cbefore fighting.");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (duelManager.isInActiveDuel(player.getUniqueId())) {
            duelManager.endDuel(player.getUniqueId(), DuelManager.DuelEndReason.DEATH);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        handleDisconnect(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        handleDisconnect(event.getPlayer());
    }

    private void handleDisconnect(Player player) {
        if (duelManager.isInActiveDuel(player.getUniqueId())) {
            duelManager.endDuel(player.getUniqueId(), DuelManager.DuelEndReason.LEFT_SERVER);
        }
        duelManager.removePendingRequests(player.getUniqueId());
        lastWarning.remove(player.getUniqueId());
    }

    private Player findAttackingPlayer(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }

        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) {
                return player;
            }
        }
        return null;
    }
}

