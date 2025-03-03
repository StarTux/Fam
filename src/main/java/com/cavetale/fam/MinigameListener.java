package com.cavetale.fam;

import com.cavetale.core.chat.Chat;
import com.cavetale.core.event.minigame.MinigameMatchCompleteEvent;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.fam.sql.Database;
import com.cavetale.fam.sql.SQLFriends;
import com.cavetale.fam.util.Colors;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;

@RequiredArgsConstructor
public final class MinigameListener implements Listener {
    private final FamPlugin plugin;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onMinigameMatchComplete(MinigameMatchCompleteEvent event) {
        final Set<UUID> uuids = event.getPlayerUuids();
        final int dayId = Timer.getDayId();
        Database.db().scheduleAsyncTask(() -> {
                for (UUID a : uuids) {
                    for (UUID b : uuids) {
                        if (a == b) continue;
                        boolean res = Database.dailyMinigame(a, b, dayId);
                        if (!res) continue;
                        final int amount = 5;
                        final SQLFriends oldRow = Database.findFriends(a, b);
                        Database.increaseFriendship(a, b, amount);
                        final SQLFriends newRow = Database.findFriends(a, b);
                        Bukkit.getScheduler().runTask(plugin, () -> callback(oldRow, newRow, event));
                    }
                }
            });
    }

    private void callback(SQLFriends oldRow, SQLFriends newRow, MinigameMatchCompleteEvent event) {
        final PlayerCache a = PlayerCache.forUuid(newRow.getPlayerA());
        final PlayerCache b = PlayerCache.forUuid(newRow.getPlayerB());
        plugin.getLogger().info(a.getName() + " and " + b.getName() + " played minigame " + event.getType().getDisplayName()
                                + oldRow.getFriendship() + " => " + newRow.getFriendship());
        final Player pa = Bukkit.getPlayer(a.getUuid());
        if (pa != null && !Chat.doesIgnore(a.getUuid(), b.getUuid())) {
            pa.sendMessage(textOfChildren(text("Your friendship with " + b.getName() + " increased! ", Colors.HOTPINK),
                                          newRow.getHeartsComponent())
                           .hoverEvent(HoverEvent.showText(text("/friends", Colors.HOTPINK)))
                           .clickEvent(ClickEvent.runCommand("/friends")));
        }
        final Player pb = Bukkit.getPlayer(b.getUuid());
        if (pb != null && !Chat.doesIgnore(b.getUuid(), a.getUuid())) {
            pb.sendMessage(textOfChildren(text("Your friendship with " + a.getName() + " increased! ", Colors.HOTPINK),
                                          newRow.getHeartsComponent())
                           .hoverEvent(HoverEvent.showText(text("/friends", Colors.HOTPINK)))
                           .clickEvent(ClickEvent.runCommand("/friends")));
        }
    }
}
