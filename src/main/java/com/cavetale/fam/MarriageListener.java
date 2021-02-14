package com.cavetale.fam;

import com.cavetale.fam.sql.Database;
import com.cavetale.fam.util.Colors;
import com.cavetale.fam.util.Text;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public final class MarriageListener implements Listener {
    private final FamPlugin plugin;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player)) return;
        Player player = event.getPlayer();
        if (player.isSneaking()) return;
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand != null && hand.getAmount() > 0) return;
        Player target = (Player) event.getRightClicked();
        if (player.equals(target)) return;
        if (!player.hasPermission("fam.marriage")) return;
        if (!target.hasPermission("fam.marriage")) return;
        if (!Database.isMarriageCached(player, target)) return;
        if (!player.getPassengers().isEmpty()) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isValid()) return;
                if (!target.isValid()) return;
                if (!player.getPassengers().isEmpty()) return;
                if (target.getVehicle() != null) target.leaveVehicle();
                player.addPassenger(target);
                player.getWorld().spawnParticle(Particle.HEART, player.getEyeLocation(), 3, 0.5f, 0.5f, 0.5f, 0.0);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_HORSE_SADDLE, SoundCategory.PLAYERS, 1.0f, 2.0f);
                player.sendActionBar(Text.builder("You lift " + target.getName() + ". Sneak to drop them!").color(Colors.PINK).create());
            });
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        for (Entity e : player.getPassengers()) {
            if (!(e instanceof Player)) continue;
            Player target = (Player) e;
            if (!Database.isMarriageCached(player, target)) continue;
            target.leaveVehicle();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("fam.marriage")) return;
        Player target = Database.getCachedMarriage(player);
        if (target == null) return;
        if (!target.hasPermission("fam.marriage")) return;
        if (!player.getWorld().equals(target.getWorld())) return;
        if (player.getLocation().distanceSquared(target.getLocation()) > 16.0) return;
        int food = player.getFoodLevel();
        float saturation = player.getSaturation();
        Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isValid()) return;
                if (!target.isValid()) return;
                int foodGain = player.getFoodLevel() - food;
                float saturationGain = player.getSaturation() - saturation;
                if (foodGain <= 0 && saturationGain < 0.01f) return;
                if (foodGain > 0) {
                    target.setFoodLevel(Math.min(20, target.getFoodLevel() + foodGain));
                }
                if (saturationGain > 0) {
                    target.setSaturation(Math.min((float) target.getFoodLevel(), target.getSaturation() + saturationGain));
                }
                target.getWorld().spawnParticle(Particle.HEART, target.getEyeLocation(), 3, 0.5f, 0.5f, 0.5f, 0.0);
                player.getWorld().spawnParticle(Particle.HEART, player.getEyeLocation(), 3, 0.5f, 0.5f, 0.5f, 0.0);
                player.sendActionBar(Text.builder("You feed " + target.getName()).color(Colors.PINK).create());
                target.sendActionBar(Text.builder(player.getName() + " feeds you").color(Colors.PINK).create());
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 0.5f);
                target.playSound(target.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 0.5f);
            });
    }
}
