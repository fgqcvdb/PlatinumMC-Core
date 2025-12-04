package com.example.platinumduel;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Keeps track of pending duel requests and active duels.
 */
public class DuelManager {

    private final Map<UUID, DuelRequest> pendingRequests = new HashMap<>();
    private final Map<UUID, UUID> activeDuels = new HashMap<>();
    private final Duration requestTtl;

    public DuelManager(Duration requestTtl) {
        this.requestTtl = Objects.requireNonNull(requestTtl, "requestTtl");
    }

    public RequestResult requestDuel(Player challenger, Player target) {
        cleanupExpiredRequests();

        UUID challengerId = challenger.getUniqueId();
        UUID targetId = target.getUniqueId();

        if (challengerId.equals(targetId)) {
            return RequestResult.SELF;
        }

        if (isInActiveDuel(challengerId)) {
            return RequestResult.CHALLENGER_BUSY;
        }

        if (isInActiveDuel(targetId)) {
            return RequestResult.TARGET_BUSY;
        }

        if (hasOutgoingRequest(challengerId)) {
            DuelRequest existing = getOutgoingRequest(challengerId).orElseThrow();
            if (existing.targetId().equals(targetId)) {
                return RequestResult.ALREADY_PENDING;
            }
            return RequestResult.CHALLENGER_HAS_OTHER_PENDING;
        }

        DuelRequest currentPending = pendingRequests.get(targetId);
        if (currentPending != null) {
            if (currentPending.challengerId().equals(challengerId)) {
                return RequestResult.ALREADY_PENDING;
            }
            return RequestResult.TARGET_HAS_PENDING;
        }

        pendingRequests.put(targetId, new DuelRequest(challengerId, targetId, Instant.now()));
        return RequestResult.SENT;
    }

    public AcceptResult acceptRequest(Player target) {
        cleanupExpiredRequests();
        UUID targetId = target.getUniqueId();

        DuelRequest request = pendingRequests.remove(targetId);
        if (request == null) {
            return AcceptResult.NO_REQUEST;
        }

        Player challenger = Bukkit.getPlayer(request.challengerId());
        if (challenger == null || !challenger.isOnline()) {
            return AcceptResult.CHALLENGER_OFFLINE;
        }

        if (isInActiveDuel(targetId) || isInActiveDuel(request.challengerId())) {
            return AcceptResult.ALREADY_IN_DUEL;
        }

        registerDuel(request.challengerId(), targetId);
        return AcceptResult.ACCEPTED;
    }

    public DeclineResult declineRequest(Player target) {
        cleanupExpiredRequests();
        UUID targetId = target.getUniqueId();
        DuelRequest request = pendingRequests.remove(targetId);
        if (request == null) {
            return DeclineResult.NO_REQUEST;
        }

        Player challenger = Bukkit.getPlayer(request.challengerId());
        if (challenger != null && challenger.isOnline()) {
            challenger.sendMessage(Messages.prefix("&e" + target.getName() + " declined your duel request."));
        }
        return DeclineResult.DECLINED;
    }

    public boolean canDamage(Player attacker, Player victim) {
        cleanupExpiredRequests();
        return isMutualDuel(attacker.getUniqueId(), victim.getUniqueId());
    }

    public Optional<UUID> getOpponent(UUID playerId) {
        return Optional.ofNullable(activeDuels.get(playerId));
    }

    public void endDuel(UUID playerId, DuelEndReason reason) {
        UUID opponentId = activeDuels.remove(playerId);
        if (opponentId == null) {
            return;
        }

        activeDuels.remove(opponentId);

        Player player = Bukkit.getPlayer(playerId);
        Player opponent = Bukkit.getPlayer(opponentId);

        String reasonMessage = switch (reason) {
            case LEFT_SERVER -> "left the server.";
            case DEATH -> "was defeated.";
            case MANUAL -> "ended the duel.";
        };

        if (player != null && player.isOnline()) {
            player.sendMessage(Messages.prefix("&eYour duel has ended."));
        }

        if (opponent != null && opponent.isOnline()) {
            opponent.sendMessage(Messages.prefix("&eYour opponent " + (player != null ? player.getName() : "someone") + " " + reasonMessage));
        }
    }

    public boolean removePendingRequests(UUID playerId) {
        cleanupExpiredRequests();
        boolean removedAny = false;

        Iterator<Map.Entry<UUID, DuelRequest>> iterator = pendingRequests.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, DuelRequest> entry = iterator.next();
            DuelRequest request = entry.getValue();
            if (request.challengerId().equals(playerId) || request.targetId().equals(playerId)) {
                iterator.remove();
                removedAny = true;

                UUID otherId = request.challengerId().equals(playerId) ? request.targetId() : request.challengerId();
                Player other = Bukkit.getPlayer(otherId);
                if (other != null && other.isOnline()) {
                    Player player = Bukkit.getPlayer(playerId);
                    String playerName = player != null ? player.getName() : "A player";
                    if (request.challengerId().equals(playerId)) {
                        other.sendMessage(Messages.prefix("&e" + playerName + " cancelled their duel request."));
                    } else {
                        other.sendMessage(Messages.prefix("&e" + playerName + " is no longer available for that duel request."));
                    }
                }
            }
        }

        return removedAny;
    }

    public boolean isInActiveDuel(UUID playerId) {
        return activeDuels.containsKey(playerId);
    }

    private void registerDuel(UUID challengerId, UUID targetId) {
        activeDuels.put(challengerId, targetId);
        activeDuels.put(targetId, challengerId);
    }

    private boolean isMutualDuel(UUID a, UUID b) {
        UUID opponent = activeDuels.get(a);
        return opponent != null && opponent.equals(b);
    }

    private void cleanupExpiredRequests() {
        if (pendingRequests.isEmpty()) {
            return;
        }

        Instant cutoff = Instant.now().minus(requestTtl);
        HashSet<UUID> expiredTargets = new HashSet<>();
        for (Map.Entry<UUID, DuelRequest> entry : pendingRequests.entrySet()) {
            if (entry.getValue().createdAt().isBefore(cutoff)) {
                expiredTargets.add(entry.getKey());
            }
        }
        for (UUID targetId : expiredTargets) {
            DuelRequest removed = pendingRequests.remove(targetId);
            if (removed != null) {
                Player challenger = Bukkit.getPlayer(removed.challengerId());
                Player target = Bukkit.getPlayer(removed.targetId());
                if (challenger != null && challenger.isOnline()) {
                    challenger.sendMessage(Messages.prefix("&cYour duel request to " + (target != null ? target.getName() : "that player") + " expired."));
                }
            }
        }
    }

    private boolean hasOutgoingRequest(UUID challengerId) {
        return getOutgoingRequest(challengerId).isPresent();
    }

    private Optional<DuelRequest> getOutgoingRequest(UUID challengerId) {
        return pendingRequests.values().stream()
                .filter(req -> req.challengerId().equals(challengerId))
                .findFirst();
    }

    public long getRequestTimeoutSeconds() {
        return requestTtl.toSeconds();
    }

    public enum RequestResult {
        SENT,
        SELF,
        ALREADY_PENDING,
        TARGET_HAS_PENDING,
        CHALLENGER_HAS_OTHER_PENDING,
        CHALLENGER_BUSY,
        TARGET_BUSY
    }

    public enum AcceptResult {
        ACCEPTED,
        NO_REQUEST,
        CHALLENGER_OFFLINE,
        ALREADY_IN_DUEL
    }

    public enum DeclineResult {
        DECLINED,
        NO_REQUEST
    }

    public enum DuelEndReason {
        LEFT_SERVER,
        DEATH,
        MANUAL
    }

    private record DuelRequest(UUID challengerId, UUID targetId, Instant createdAt) {
    }
}

