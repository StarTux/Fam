package com.cavetale.fam.advent;

import com.cavetale.core.bungee.Bungee;
import com.cavetale.core.connect.NetworkServer;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.mytems.item.music.PlayerPlayInstrumentEvent;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.boat.OakBoat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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
        session.save(null);
        AdventMusic.grinch(player);
    }

    @EventHandler
    private void onPlayerRespawn(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        if (!AdventDailies.isAdventWorld(player.getWorld())) return;
        event.setRespawnLocation(player.getWorld().getSpawnLocation());
        if (!NetworkServer.BETA.isThisServer()) {
            Bungee.send(player, "hub");
        }
    }

    @EventHandler
    private void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!AdventDailies.isAdventWorld(event.getEntity().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler
    private void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!AdventDailies.isAdventWorld(event.getEntity().getWorld())) return;
        switch (event.getRegainReason()) {
        case SATIATED:
            event.setCancelled(true);
            break;
        default:
            break;
        }
    }

    @EventHandler
    private void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!AdventDailies.isAdventWorld(mob.getWorld())) return;
        final Player killer = mob.getKiller();
        final AdventSession session = AdventSession.of(killer);
        session.ifDaily(daily -> {
                if (daily instanceof AdventDailyKillMob killMob) {
                    killMob.onKillMob(killer, mob);
                }
            });
    }

    @EventHandler
    private void onPlayerHud(PlayerHudEvent event) {
        final Player player = event.getPlayer();
        if (!AdventDailies.isAdventWorld(player.getWorld())) return;
        final AdventSession session = AdventSession.of(player);
        session.ifDaily(daily -> {
                if (daily.getDescription().isEmpty()) return;
                event.bossbar(PlayerHudPriority.HIGH, daily.getDescription().get(0), BossBar.Color.RED, BossBar.Overlay.PROGRESS, 1f);
            });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!AdventDailies.isAdventWorld(event.getEntity().getWorld())) return;
        if (event.getEntity().getType() == EntityType.IRON_GOLEM) {
            event.setCancelled(false);
        }
    }

    @EventHandler
    private void onPlayerPlayInstrument(PlayerPlayInstrumentEvent event) {
        final Player player = event.getPlayer();
        if (!AdventDailies.isAdventWorld(player.getWorld())) return;
        final AdventSession session = AdventSession.of(player);
        session.ifDaily(daily -> {
                if (daily instanceof AdventDailyPlayMusic playMusic) {
                    playMusic.onPlayTouch(player, event.getTouch());
                }
            });
    }

    @EventHandler
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        if (!AdventDailies.isAdventWorld(player.getWorld())) return;
        if (!(event.getRightClicked() instanceof OakBoat boat)) return;
        event.setCancelled(true);
        final AdventSession session = AdventSession.of(player);
        if (session.getDaily() instanceof AdventDailyFloatBoat floatBoat) {
            floatBoat.onClickBoat(player, session, boat);
        }
    }

    @EventHandler
    private void onPlayerInteractBlock(PlayerInteractEvent event) {
        switch (event.getAction()) {
        case RIGHT_CLICK_BLOCK: break;
        default: return;
        }
        final Player player = event.getPlayer();
        if (!AdventDailies.isAdventWorld(player.getWorld())) return;
        final AdventSession session = AdventSession.of(player);
        if (session.getDaily() instanceof AdventDailyShrinkStar shrinkStar) {
            shrinkStar.onInteract(player, session, event.getClickedBlock());
        }
    }
}
