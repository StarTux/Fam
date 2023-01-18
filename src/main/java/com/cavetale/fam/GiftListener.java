package com.cavetale.fam;

import com.cavetale.core.event.item.PlayerAbsorbItemEvent;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.fam.sql.Database;
import com.cavetale.fam.sql.SQLFriends;
import com.cavetale.fam.sql.SQLProgress;
import com.cavetale.fam.util.Colors;
import com.cavetale.fam.util.Text;
import com.cavetale.mytems.Mytems;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

@RequiredArgsConstructor
public final class GiftListener implements Listener {
    private final FamPlugin plugin;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        onPickup(player, event.getItem());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerAbsorbItem(PlayerAbsorbItemEvent event) {
        onPickup(event.getPlayer(), event.getItem());
    }

    private void onPickup(Player player, Item item) {
        if (Mytems.forItem(item.getItemStack()) != null) return;
        if (item.getItemStack().getType() != plugin.getTodaysGift()) return;
        if (item.getThrower() == null) return;
        Player thrower = Bukkit.getPlayer(item.getThrower());
        if (thrower == null) return;
        if (player.equals(thrower)) return;
        if (!player.hasPermission("fam.friends")) return;
        if (!thrower.hasPermission("fam.friends")) return;
        UUID a = thrower.getUniqueId();
        UUID b = player.getUniqueId();
        final boolean birthday = Database.getCache(player).isBirthday() || Database.getCache(thrower).isBirthday();
        Database.db().scheduleAsyncTask(() -> {
                boolean res = Database.dailyGift(a, b, Timer.getDayId());
                if (!res) return;
                final int amount = birthday ? 20 : 5;
                SQLFriends row = Database.findFriends(a, b);
                Database.increaseFriendship(a, b, amount);
                final int oldFriendship = row.getFriendship();
                final int newFriendship = oldFriendship + amount;
                final int heartCount = row.getHearts(newFriendship);
                boolean won = row == null || row.getHearts() != heartCount;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < heartCount - (won ? 1 : 0); i += 1) {
                    sb.append(Text.HEART_ICON);
                }
                Component hearts = won
                    ? Component.text().content(sb.toString()).color(Colors.HOTPINK).append(Component.text(Text.HEART_ICON, Colors.GOLD)).build()
                    : Component.text(sb.toString(), Colors.HOTPINK);
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
                Bukkit.getScheduler().runTask(plugin, () -> callback(player, playerProgress,
                                                                     thrower, throwerProgress,
                                                                     hearts, oldFriendship, newFriendship));
            });
    }

    private void callback(Player player, SQLProgress playerProgress, Player thrower, SQLProgress throwerProgress,
                          Component hearts, int oldFriendship, int newFriendship) {
        if (player.isOnline()) {
            player.sendMessage(Component.text().color(Colors.HOTPINK)
                               .content("Your friendship with " + thrower.getName() + " increased! ")
                               .append(hearts)
                               .hoverEvent(HoverEvent.showText(Component.text("/friends", Colors.HOTPINK)))
                               .clickEvent(ClickEvent.runCommand("/friends")));
            if (player.hasPermission("fam.debug")) {
                player.sendMessage(Component.text("Debug Friendship: " + oldFriendship + " => " + newFriendship,
                                                  Colors.DARK_GRAY));
            }
            if (playerProgress != null && playerProgress.isRewardAvailable()) {
                valentineRewardReminder(player);
            }
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 2.0f);
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, player.getHeight() + 0.25, 0), 2, 0, 0, 0, 0);
        }
        if (thrower.isOnline()) {
            thrower.sendMessage(Component.text().color(Colors.HOTPINK)
                                .content("Your friendship with " + player.getName() + " increased! ")
                                .append(hearts)
                                .hoverEvent(HoverEvent.showText(Component.text("/friends", Colors.HOTPINK)))
                                .clickEvent(ClickEvent.runCommand("/friends")));
            if (thrower.hasPermission("fam.debug")) {
                thrower.sendMessage(Component.text("Debug Friendship: " + oldFriendship + " => " + newFriendship,
                                                   Colors.DARK_GRAY));
            }
            thrower.playSound(thrower.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 2.0f);
            thrower.getWorld().spawnParticle(Particle.HEART, thrower.getLocation().add(0, player.getHeight() + 0.25, 0), 2, 0, 0, 0, 0);
            if (throwerProgress != null && throwerProgress.isRewardAvailable()) {
                valentineRewardReminder(thrower);
            }
            PluginPlayerEvent.Name.SHARE_FRIENDSHIP_ITEM.call(plugin, thrower);
        }
    }

    private void valentineRewardReminder(Player player) {
        player.sendMessage(Component.text()
                           .append(Component.text("A new valentine reward is available! See /valentine", Colors.HOTPINK))
                           .hoverEvent(HoverEvent.showText(Component.text("/valentine", Colors.HOTPINK)))
                           .clickEvent(ClickEvent.runCommand("/valentine")));
    }
}
