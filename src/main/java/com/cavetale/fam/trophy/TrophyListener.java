package com.cavetale.fam.trophy;

import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class TrophyListener implements Listener {
    private final Trophies trophies;
    private final Set<UUID> playersWithUnseenTrophies = new HashSet<>();

    protected TrophyListener enable() {
        Bukkit.getPluginManager().registerEvents(this, trophies.plugin);
        Bukkit.getScheduler().runTaskTimer(trophies.plugin, () -> {
                List<UUID> uuids = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    uuids.add(player.getUniqueId());
                }
                if (!uuids.isEmpty()) refreshUnseenTrophies(uuids);
            }, 0L, 20L * 60L);
        return this;
    }

    @EventHandler
    void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        refreshUnseenTrophies(List.of(player.getUniqueId()));
    }

    @EventHandler
    void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playersWithUnseenTrophies.remove(player.getUniqueId());
    }

    @EventHandler
    void onPlayerHud(PlayerHudEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("fam.trophy")) return;
        if (playersWithUnseenTrophies.contains(player.getUniqueId())) {
            event.sidebar(PlayerHudPriority.DEFAULT, List.of(//text("You have a", AQUA),
                                                             join(noSeparators(),
                                                                  text("You have a ", AQUA),
                                                                  text("/trophy", YELLOW))));
        }
    }

    protected void refreshUnseenTrophies(List<UUID> players) {
        Trophies.findUnseenTrophiesAsync(players, rows -> {
                Set<UUID> nay = new HashSet<>();
                nay.addAll(players);
                for (SQLTrophy trophy : rows) {
                    UUID it = trophy.getOwner();
                    nay.remove(it);
                    playersWithUnseenTrophies.add(it);
                }
                playersWithUnseenTrophies.removeAll(nay);
            });
    }
}
