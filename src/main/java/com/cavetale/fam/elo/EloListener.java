package com.cavetale.fam.elo;

import com.cavetale.core.event.minigame.MinigameMatchCompleteEvent;
import com.cavetale.core.event.minigame.MinigameMatchType;
import com.cavetale.core.event.player.PlayerTeamQuery;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.fam.sql.Database;
import com.cavetale.fam.sql.SQLElo;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import static com.cavetale.fam.FamPlugin.plugin;

public final class EloListener implements Listener {
    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin());
    }

    @EventHandler
    private void onMinigameMatchComplete(MinigameMatchCompleteEvent event) {
        final Set<UUID> players = event.getPlayerUuids();
        if (players.size() < 2) return;
        final Set<UUID> winners = event.getWinnerUuids();
        if (winners.size() >= players.size()) {
            throw new IllegalArgumentException("winners=" + winners.size() + " >= players=" + players.size());
        }
        final PlayerTeamQuery playerTeamQuery = new PlayerTeamQuery();
        playerTeamQuery.callEvent();
        Database.db().scheduleAsyncTask(() -> async(event.getType(), players, winners, playerTeamQuery));
    }

    private void async(MinigameMatchType type, Set<UUID> players, Set<UUID> winners, PlayerTeamQuery playerTeamQuery) {
        // Get or create all elos
        final Map<UUID, SQLElo> elos = new HashMap<>();
        for (SQLElo elo : Database.db().find(SQLElo.class).eq("category", type.name().toLowerCase()).in("player", players).findList()) {
            elos.put(elo.getPlayer(), elo);
        }
        for (UUID player : players) {
            if (elos.containsKey(player)) continue;
            final SQLElo elo = new SQLElo(type.name().toLowerCase(), player);
            Database.db().insert(elo);
            elos.put(player, elo);
        }
        // Update elos
        final boolean draw = winners.isEmpty();
        if (players.size() == 2) {
            final var iter = players.iterator();
            final UUID playerA = iter.next();
            final UUID playerB = iter.next();
            final SQLElo eloA = elos.get(playerA);
            final SQLElo eloB = elos.get(playerB);
            final double ratingA = eloA.getRating();
            final double ratingB = eloB.getRating();
            eloA.increaseGames();
            eloB.increaseGames();
            updateRating(type, eloA, ratingB, (draw ? 0.5 : (winners.contains(playerA) ? 1.0 : 0.0)));
            updateRating(type, eloB, ratingA, (draw ? 0.5 : (winners.contains(playerB) ? 1.0 : 0.0)));
        } else if (players.size() > 2 && !draw && playerTeamQuery.hasTeams()) {
            // We assume that all winners are in the same team.
            double winnerRating = 0.0;
            double loserRating = 0.0;
            double winnerCount = (double) winners.size();
            double loserCount = (double) players.size() - winnerCount;
            for (SQLElo elo : elos.values()) {
                if (winners.contains(elo.getPlayer())) {
                    winnerRating += elo.getRating() / winnerCount;
                } else {
                    loserRating += elo.getRating() / loserCount;
                }
            }
            for (SQLElo elo : elos.values()) {
                elo.increaseGames();
                if (winners.contains(elo.getPlayer())) {
                    updateRating(type, elo, loserRating, 1.0);
                } else {
                    updateRating(type, elo, winnerRating, 0.0);
                }
            }
        } else if (players.size() > 2) {
            // Maybe draw, maybe teams
            // We simplify this and adjust the elo against every player unless they are in the same team.
            final Map<UUID, Double> ratings = new HashMap<>();
            for (SQLElo elo : elos.values()) {
                ratings.put(elo.getPlayer(), elo.getRating());
            }
            for (SQLElo elo : elos.values()) {
                elo.increaseGames();
                final double oldRating = elo.getRating();
                for (SQLElo opponent : elos.values()) {
                    if (elo == opponent) continue;
                    if (playerTeamQuery.hasTeams() && playerTeamQuery.getTeam(elo.getPlayer()) == playerTeamQuery.getTeam(opponent.getPlayer())) {
                        continue;
                    }
                    final boolean weWin = winners.contains(elo.getPlayer());
                    final boolean theyWin = winners.contains(opponent.getPlayer());
                    final double opponentRating = ratings.get(opponent.getPlayer());
                    if (weWin == theyWin) {
                        elo.updateRatingAgainst(opponentRating, 0.5);
                    } else if (weWin) {
                        elo.updateRatingAgainst(opponentRating, 1.0);
                    } else {
                        elo.updateRatingAgainst(opponentRating, 0.0);
                    }
                }
                saveRating(elo);
                logRating(type, elo, oldRating);
            }
        } else {
            throw new IllegalStateException("players.size=" + players.size() + " draw=" + draw);
        }
    }

    private void updateRating(MinigameMatchType type, SQLElo elo, double opponent, double winResult) {
        final double oldRating = elo.getRating();
        elo.updateRatingAgainst(opponent, winResult);
        saveRating(elo);
        logRating(type, elo, oldRating);
    }

    private void saveRating(SQLElo elo) {
        Database.db().update(elo, "rating", "games", "lastUpdate");
    }

    private void logRating(MinigameMatchType type, SQLElo elo, double oldRating) {
        plugin().getLogger().info("[Elo] " + type.getDisplayName()
                                  + " " + PlayerCache.nameForUuid(elo.getPlayer())
                                  + " #" + elo.getGames()
                                  + " " + fmt(oldRating) + " => " + fmt(elo.getRating()));
    }

    private static String fmt(double rating) {
        return "" + ((int) rating);
    }
}
