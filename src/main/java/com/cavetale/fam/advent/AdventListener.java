package com.cavetale.fam.advent;

import com.cavetale.core.bungee.Bungee;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import static com.cavetale.fam.FamPlugin.famPlugin;

public final class AdventListener implements Listener {
    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, famPlugin());
    }

    @EventHandler
    private void onPlayerDeath(PlayerDeathEvent event) {
        final Player player = event.getEntity();
        if (!AdventDailies.isAdventWorld(player.getWorld())) return;
        event.setKeepInventory(false);
        event.setKeepLevel(false);
        event.getDrops().clear();
        event.setDroppedExp(0);
        final AdventSession session = AdventSession.of(player);
        session.stopDaily();
        session.save(() -> Bungee.send(player, "hub"));
    }

    @EventHandler
    private void onPlayerRespawn(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        if (!AdventDailies.isAdventWorld(player.getWorld())) return;
        event.setRespawnLocation(player.getWorld().getSpawnLocation());
    }
}
