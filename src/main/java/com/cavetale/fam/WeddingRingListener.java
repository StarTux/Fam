package com.cavetale.fam;

import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.fam.sql.Database;
import com.cavetale.fam.sql.SQLFriends;
import com.cavetale.fam.util.Colors;
import com.cavetale.fam.util.Fireworks;
import com.cavetale.mytems.Mytems;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

@RequiredArgsConstructor
public final class WeddingRingListener implements Listener {
    private final FamPlugin plugin;
    private final Map<UUID, UUID> requests = new HashMap<>();
    private NamespacedKey fireworkKey;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        fireworkKey = new NamespacedKey(plugin, "firework");
    }

    public void clearRequest(Player player) {
        requests.remove(player.getUniqueId());
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    void onPlayerInteratEntityEvent(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player)) return;
        Player player = event.getPlayer();
        if (Mytems.WEDDING_RING != Mytems.forItem(player.getInventory().getItemInMainHand())) return;
        Player target = (Player) event.getRightClicked();
        if (player.equals(target)) return;
        if (!player.hasPermission("fam.marriage")) return;
        if (!target.hasPermission("fam.marriage")) return;
        UUID uuid = player.getUniqueId();
        UUID uuid2 = target.getUniqueId();
        Database.db().scheduleAsyncTask(() -> {
                // Monogamy
                List<SQLFriends> married = Database.findFriendsList(uuid, Relation.MARRIED);
                if (!married.isEmpty()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                            UUID other = married.get(0).getOther(uuid);
                            if (other.equals(uuid2)) {
                                player.sendMessage(text("You two are already married. :)", RED));
                                return;
                            }
                            String name = PlayerCache.nameForUuid(married.get(0).getOther(uuid));
                            if (name == null) name = "somebody";
                            player.sendMessage(text("You're already married to " + name + "!", RED));
                        });
                    return;
                }
                List<SQLFriends> married2 = Database.findFriendsList(uuid2, Relation.MARRIED);
                if (!married2.isEmpty()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage(text(target.getName() + " is already married! :(", RED));
                        });
                    return;
                }
                SQLFriends row = Database.findFriends(uuid, uuid2);
                Bukkit.getScheduler().runTask(plugin, () -> callback(player, target, row));
            });
    }

    private void callback(Player player, Player target, SQLFriends row) {
        if (!player.isOnline() || !target.isOnline()) return;
        if (!Timer.isValentinesDay() && (row == null || row.getFriendship() < 100)) {
            player.sendMessage(textOfChildren(text("You need at least "),
                                              SQLFriends.getHeartsComponent(100),
                                              text(" with " + target.getName()))
                               .color(RED));
            return;
        }
        Relation relation = row.getRelationFor(player.getUniqueId());
        if (relation != null && relation != Relation.FRIEND) {
            player.sendMessage(text(target.getName() + " is already your " + relation.getYour() + "!", RED));
            return;
        }
        Mytems mytems1 = Mytems.forItem(player.getInventory().getItemInMainHand());
        Mytems mytems2 = Mytems.forItem(target.getInventory().getItemInMainHand());
        if (mytems1 != Mytems.WEDDING_RING || mytems2 != Mytems.WEDDING_RING) {
            player.sendMessage(text("Both of you must hold the wedding ring!", RED));
            return;
        }
        UUID request = requests.get(target.getUniqueId());
        if (Objects.equals(request, player.getUniqueId())) {
            requests.remove(target.getUniqueId());
            Database.setRelation(row, player.getUniqueId(), Relation.MARRIED);
            Database.friendLogAsync(player.getUniqueId(), target.getUniqueId(), Relation.MARRIED, "Married");
            Database.friendLogAsync(target.getUniqueId(), player.getUniqueId(), Relation.MARRIED, "Married");
            player.getInventory().getItemInMainHand().subtract(1);
            target.getInventory().getItemInMainHand().subtract(1);
            player.showTitle(Title.title(text("Married", Colors.HOTPINK),
                                         text("You married " + target.getName(), Colors.HOTPINK)));
            target.showTitle(Title.title(text("Married", Colors.HOTPINK),
                                         text("You married " + player.getName(), Colors.HOTPINK)));
            weddingTask(player, target);
            Database.fillCacheAsync(player);
            Database.fillCacheAsync(target);
            plugin.getLogger().info("Married: " + player.getName() + ", " + target.getName());
        } else {
            requests.put(player.getUniqueId(), target.getUniqueId());
            player.sendMessage(text("You ask " + target.getName() + " to get married."
                                    + " If they use their ring on you, the ceremony is complete!",
                                    Colors.HOTPINK));
            target.sendMessage(text(player.getName() + " asks you to get married."
                                    + " If you use your ring on them, the ceremony is complete!",
                                    Colors.HOTPINK));
        }
    }

    void weddingTask(Player a, Player b) {
        List<Player> players = Arrays.asList(a, b);
        long started = System.currentTimeMillis();
        Random random = new Random();
        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (System.currentTimeMillis() - started > 10000L) {
                    cancel();
                    return;
                }
                for (Player player : players) {
                    if (ticks % 5 == 0) {
                        Location loc = player.getEyeLocation();
                        loc.getWorld().spawnParticle(Particle.HEART, loc, 1 + random.nextInt(4), 1.0, 1.0, 1.0, 0.0);
                    }
                    if (ticks % 20 == 0) {
                        Fireworks.spawnFirework(player.getEyeLocation())
                            .getPersistentDataContainer().set(fireworkKey,
                                                              PersistentDataType.BYTE, (byte) 1);
                    }
                }
                ticks += 1;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager().getPersistentDataContainer().has(fireworkKey, PersistentDataType.BYTE))) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onPlayerQuit(PlayerQuitEvent event) {
        clearRequest(event.getPlayer());
    }
}
