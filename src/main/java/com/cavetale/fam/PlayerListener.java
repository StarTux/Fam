package com.cavetale.fam;

import com.cavetale.core.event.player.PluginPlayerQuery;
import com.cavetale.fam.session.Session;
import com.cavetale.fam.sql.Database;
import com.winthier.chat.event.ChatMessageEvent;
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
        } else if (query.getName() == PluginPlayerQuery.Name.DID_ENTER_BIRTHDAY) {
            PluginPlayerQuery.Name.DID_ENTER_BIRTHDAY.respond(query, plugin, Database.isBirthdayCached(query.getPlayer()));
        }
    }

    @EventHandler
    private void onChatMessage(ChatMessageEvent event) {
        if (event.getMessage().getSender() == null) return;
        Session session = Session.of(event.getMessage().getSender());
        if (session == null || !session.isReady() || session.getPlayerRow().getStatusMessage() == null) return;
        event.getMessage().setStatusMessage(session.getPlayerRow().getStatusMessage());
    }
}
