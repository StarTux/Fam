package com.cavetale.fam;

import com.cavetale.core.chat.Chat;
import com.cavetale.core.event.friends.PlayerShareFriendshipGiftEvent;
import com.cavetale.core.event.item.PlayerAbsorbItemEvent;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.fam.sql.Database;
import com.cavetale.fam.sql.SQLFriends;
import com.cavetale.fam.util.Colors;
import com.cavetale.mytems.Mytems;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;

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
        ItemStack itemStack = item.getItemStack();
        if (Mytems.forItem(itemStack) != null) return;
        if (itemStack.getType() != plugin.getTodaysGift()) return;
        if (item.getThrower() == null) return;
        Player thrower = Bukkit.getPlayer(item.getThrower());
        if (thrower == null) return;
        if (player.equals(thrower)) return;
        if (!player.hasPermission("fam.friends")) return;
        if (!thrower.hasPermission("fam.friends")) return;
        final UUID a = thrower.getUniqueId();
        final UUID b = player.getUniqueId();
        final boolean birthday = Database.getCache(player).isBirthday() || Database.getCache(thrower).isBirthday();
        Database.db().scheduleAsyncTask(() -> {
                boolean res = Database.dailyGift(a, b, Timer.getDayId());
                if (!res) return;
                final int amount = birthday ? 20 : 5;
                final SQLFriends oldRow = Database.findFriends(a, b);
                Database.increaseFriendship(a, b, amount);
                final SQLFriends newRow = Database.findFriends(a, b);
                if (Timer.isValentineSeason()) {
                    Database.addProgress(a);
                    Database.addProgress(b);
                }
                Bukkit.getScheduler().runTask(plugin, () -> callback(player, thrower, oldRow, newRow, itemStack));
            });
    }

    private void callback(Player player, Player thrower, SQLFriends oldRow, SQLFriends newRow, ItemStack itemStack) {
        plugin.getLogger().info(thrower.getName() + " and " + player.getName() + " shared friendship gifts: "
                                + oldRow.getFriendship() + " => " + newRow.getFriendship());
        if (player.isOnline() && !Chat.doesIgnore(player.getUniqueId(), thrower.getUniqueId())) {
            player.sendMessage(textOfChildren(text("Your friendship with " + thrower.getName() + " increased! ", Colors.HOTPINK),
                                              newRow.getHeartsComponent())
                               .hoverEvent(HoverEvent.showText(text("/friends", Colors.HOTPINK)))
                               .clickEvent(ClickEvent.runCommand("/friends")));
            if (player.hasPermission("fam.debug")) {
                player.sendMessage(text("Debug Friendship: " + oldRow.getFriendship() + " => " + newRow.getFriendship(), Colors.DARK_GRAY));
            }
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 2.0f);
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, player.getHeight() + 0.25, 0), 2, 0, 0, 0, 0);
        }
        if (thrower.isOnline() && !Chat.doesIgnore(thrower.getUniqueId(), player.getUniqueId())) {
            thrower.sendMessage(textOfChildren(text("Your friendship with " + player.getName() + " increased! ", Colors.HOTPINK),
                                               newRow.getHeartsComponent())
                                .hoverEvent(HoverEvent.showText(text("/friends", Colors.HOTPINK)))
                                .clickEvent(ClickEvent.runCommand("/friends")));
            if (thrower.hasPermission("fam.debug")) {
                thrower.sendMessage(text("Debug Friendship: " + oldRow.getFriendship() + " => " + newRow.getFriendship(), Colors.DARK_GRAY));
            }
            thrower.playSound(thrower.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 2.0f);
            thrower.getWorld().spawnParticle(Particle.HEART, thrower.getLocation().add(0, player.getHeight() + 0.25, 0), 2, 0, 0, 0, 0);
            PluginPlayerEvent.Name.SHARE_FRIENDSHIP_ITEM.call(plugin, thrower);
            new PlayerShareFriendshipGiftEvent(thrower, player, itemStack).callEvent();
        }
    }

    private void valentineRewardReminder(Player player) {
        player.sendMessage(text("A new valentine reward is available! See /valentine", Colors.HOTPINK)
                           .hoverEvent(HoverEvent.showText(text("/valentine", Colors.HOTPINK)))
                           .clickEvent(ClickEvent.runCommand("/valentine")));
    }
}
