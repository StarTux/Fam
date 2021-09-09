package com.cavetale.fam;

import com.cavetale.core.event.player.PluginPlayerQuery;
import com.cavetale.fam.sql.Database;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public final class PlayerListener implements Listener {
    private final FamPlugin plugin;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Database.fillCacheAsync(player);
        Database.storePlayerProfileAsync(player).fetchPlayerSkinAsync();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getFriendCommand().clearRequest(player);
        Database.clearCache(player.getUniqueId());
    }

    @EventHandler
    void onPluginPlayer(PluginPlayerQuery query) {
        if (query.getName() == PluginPlayerQuery.Name.FRIEND_COUNT) {
            PluginPlayerQuery.Name.FRIEND_COUNT.respond(query, plugin, Database.countFriendsCached(query.getPlayer()));
        }
    }
}
