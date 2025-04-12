package com.cavetale.fam;

import com.cavetale.fam.sql.Database;
import com.cavetale.fam.sql.SQLFriends;
import com.cavetale.fam.util.Colors;
import com.cavetale.mytems.Mytems;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import static java.time.Duration.ofSeconds;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.title.Title.Times.times;
import static net.kyori.adventure.title.Title.title;

@RequiredArgsConstructor
public final class LovePotionListener implements Listener {
    private static final String PERMISSION = "fam.lovepotion";
    private final FamPlugin plugin;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        if (Mytems.forItem(event.getItem()) != Mytems.LOVE_POTION) return;
        final Player player = event.getPlayer();
        if (!player.hasPermission(PERMISSION)) return;
        Player tmpTarget = null;
        double minDistance = 0;
        final Location playerLocation = player.getLocation();
        for (Player p : player.getWorld().getPlayers()) {
            if (p.equals(player)) continue;
            if (!p.hasPermission(PERMISSION)) continue;
            final double distance = p.getLocation().distanceSquared(playerLocation);
            if (tmpTarget == null || minDistance > distance) {
                tmpTarget = p;
                minDistance = distance;
            }
        }
        final Player target = tmpTarget;
        if (target == null) return;
        Database.db().scheduleAsyncTask(() -> {
                final SQLFriends oldRow = Database.findFriends(player.getUniqueId(), target.getUniqueId());
                Database.increaseFriendship(player.getUniqueId(), target.getUniqueId(), 100);
                final SQLFriends newRow = Database.findFriends(player.getUniqueId(), target.getUniqueId());
                Bukkit.getScheduler().runTask(plugin, () -> callback(player, target, oldRow, newRow));
            });
    }

    private void callback(Player player, Player target, SQLFriends oldRow, SQLFriends newRow) {
        if (oldRow == null) oldRow = new SQLFriends(player.getUniqueId(), target.getUniqueId());
        showEffect(player, target, oldRow, newRow);
        showEffect(target, player, oldRow, newRow);
    }

    private void showEffect(Player player, Player other, SQLFriends oldRow, SQLFriends newRow) {
        player.sendMessage(textOfChildren(Mytems.LOVE_POTION,
                                          text("Your friendship with " + other.getName() + " increased! ", Colors.HOTPINK),
                                          newRow.getHeartsComponent()));
        player.showTitle(title(textOfChildren(Mytems.LOVE_POTION, text(other.getName(), Colors.HOTPINK)),
                               oldRow.getHeartsComponent(),
                               times(ofSeconds(1), ofSeconds(3), ofSeconds(0))));
        player.getWorld().spawnParticle(Particle.HEART, player.getEyeLocation().add(0.0, 0.5, 0.0), 5, 0.35, 0.35, 0.35, 0.0);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.showTitle(title(textOfChildren(Mytems.LOVE_POTION, text(other.getName(), Colors.HOTPINK)),
                                       newRow.getHeartsComponent(),
                                       times(ofSeconds(0), ofSeconds(2), ofSeconds(1))));
                player.getWorld().spawnParticle(Particle.HEART, player.getEyeLocation().add(0.0, 0.5, 0.0), 5, 0.35, 0.35, 0.35, 0.0);
            }, 40L);
    }
}
