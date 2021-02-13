package com.cavetale.fam;

import com.cavetale.fam.sql.Database;
import com.cavetale.fam.sql.SQLFriends;
import com.cavetale.fam.sql.SQLProgress;
import com.cavetale.fam.util.Colors;
import com.cavetale.fam.util.Text;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
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
        Database.storePlayerProfileAsync(player);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getFriendCommand().clearRequest(player);
        plugin.getWeddingRingListener().clearRequest(player);
        Database.clearCacheAsync(player);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getItem().getItemStack().getType() != plugin.getTodaysFood()) return;
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getItem().getThrower() == null) return;
        Player thrower = Bukkit.getPlayer(event.getItem().getThrower());
        if (thrower == null) return;
        Player player = (Player) event.getEntity();
        if (player.equals(thrower)) return;
        if (!player.hasPermission("fam.friends")) return;
        if (!thrower.hasPermission("fam.friends")) return;
        UUID a = thrower.getUniqueId();
        UUID b = player.getUniqueId();
        Database.db().scheduleAsyncTask(() -> {
                boolean res = Database.dailyGift(a, b, Timer.getDayId());
                if (!res) return;
                final int amount = 4;
                SQLFriends row = Database.findFriends(a, b);
                Database.increaseFriendship(a, b, amount);
                ComponentBuilder hearts = new ComponentBuilder();
                int heartCount = row.getHearts(row.getFriendship() + 4);
                boolean won = row == null || row.getHearts() != heartCount;
                for (int i = 0; i < heartCount - 1; i += 1) {
                    hearts.append(Text.HEART_ICON).color(Colors.PINK);
                }
                hearts.append(Text.HEART_ICON).color(won ? Colors.ORANGE : Colors.PINK);
                final SQLProgress playerProgress;
                final SQLProgress throwerProgress;
                if (Timer.isValentineSeason()) {
                    Database.addProgress(a);
                    Database.addProgress(b);
                    playerProgress = Database.findProgress(b);
                    throwerProgress = Database.findProgress(a);
                } else {
                    playerProgress = null;
                    throwerProgress = null;
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            player.sendMessage(Text.builder("Your friendship with " + thrower.getName() + " increased!").color(Colors.PINK)
                                               .append(" ").append(hearts.create())
                                               .event(Text.hover(Text.builder("/friends").color(Colors.PINK).create()))
                                               .event(Text.click("/friends"))
                                               .create());
                            if (playerProgress != null && playerProgress.isRewardAvailable()) {
                                player.sendMessage(Text.builder("A new valentine reward is available! See ").color(Colors.PINK)
                                                   .append("/valentine")
                                                   .event(Text.hover(Text.builder("/valentine").color(Colors.PINK).create()))
                                                   .event(Text.click("/valentine"))
                                                   .create());
                            }
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 2.0f);
                            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, player.getHeight() + 0.25, 0), 2, 0, 0, 0, 0);
                        }
                        if (thrower.isOnline()) {
                            thrower.sendMessage(Text.builder("Your friendship with " + player.getName() + " increased!").color(Colors.PINK)
                                                .append(" ").append(hearts.create())
                                                .event(Text.hover(Text.builder("/friends").color(Colors.PINK).create()))
                                                .event(Text.click("/friends"))
                                                .create());
                            thrower.playSound(thrower.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 2.0f);
                            thrower.getWorld().spawnParticle(Particle.HEART, thrower.getLocation().add(0, player.getHeight() + 0.25, 0), 2, 0, 0, 0, 0);
                            if (throwerProgress != null && throwerProgress.isRewardAvailable()) {
                                thrower.sendMessage(Text.builder("A new valentine reward is available! See").color(Colors.PINK)
                                                    .append("/valentine")
                                                    .event(Text.hover(Text.builder("/valentine").color(Colors.PINK).create()))
                                                    .event(Text.click("/valentine"))
                                                    .create());
                            }
                        }
                    });
            });
    }
}
