package com.cavetale.fam;

import com.cavetale.core.event.connect.ConnectMessageEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@RequiredArgsConstructor
public final class ConnectListener implements Listener {
    private final FamPlugin plugin;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    private void onConnectMessage(ConnectMessageEvent event) {
        switch (event.getChannel()) {
        case FriendCommand.CONNECT_FRIEND_DID_ACCEPT: {
            final UUID uuid = UUID.fromString(event.getPayload());
            final Player player = Bukkit.getPlayer(uuid);
            if (player == null) return;
            plugin.getFriendCommand().onFriendDidAccept(player);
            break;
        }
        case UnfriendCommand.CONNECT_UNFRIENDED: {
            final UUID uuid = UUID.fromString(event.getPayload());
            final Player player = Bukkit.getPlayer(uuid);
            if (player == null) return;
            plugin.getUnfriendCommand().onUnfriended(player);
            break;
        }
        default: break;
        }
    }
}
