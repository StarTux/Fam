package com.cavetale.fam;

import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.fam.sql.Database;
import com.cavetale.fam.sql.SQLFriends;
import com.cavetale.fam.util.Colors;
import com.cavetale.fam.util.Text;
import com.cavetale.mytems.Mytems;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class FriendCommand implements TabExecutor {
    private final FamPlugin plugin;
    private final Map<UUID, UUID> requests = new HashMap<>(); // requestor -> requestee

    public void enable() {
        plugin.getCommand("friend").setExecutor(this);
    }

    public void clearRequest(Player player) {
        requests.remove(player.getUniqueId());
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length != 1) return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage("[Fam] Player expected");
            return true;
        }
        Player player = (Player) sender;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
            return true;
        }
        if (player.equals(target)) {
            player.sendMessage(Component.text("You cannot friend yourself", NamedTextColor.RED));
            return true;
        }
        UUID a = player.getUniqueId();
        UUID b = target.getUniqueId();
        Database.db().scheduleAsyncTask(() -> {
                SQLFriends friends = Database.findFriends(a, b);
                Bukkit.getScheduler().runTask(plugin, () -> callback(player, target, friends));
            });
        return true;
    }

    private void callback(Player player, Player target, SQLFriends row) {
        if (!player.isOnline() || !target.isOnline()) return;
        if (row == null || row.getHearts() < 3) {
            player.sendMessage(Component.text("You need at least 3" + Text.HEART_ICON + " with " + target.getName(),
                                              NamedTextColor.RED));
            return;
        }
        Relation relation = row.getRelationFor(player.getUniqueId());
        if (relation != null) {
            player.sendMessage(Component.text(target.getName() + " is already your " + relation.getYour() + "!",
                                              NamedTextColor.RED));
            return;
        }
        UUID request = requests.get(target.getUniqueId());
        if (Objects.equals(request, player.getUniqueId())) {
            // Accept request
            requests.remove(target.getUniqueId());
            Database.setRelation(row, player.getUniqueId(), Relation.FRIEND);
            player.sendMessage(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                        Mytems.HEART.component,
                        Component.text("You and " + target.getName() + " are now friends!", Colors.HOTPINK),
                    })
                .hoverEvent(HoverEvent.showText(Component.text("/friends", Colors.HOTPINK)))
                .clickEvent(ClickEvent.runCommand("/friends")));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 2.0f);
            target.sendMessage(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                        Mytems.HEART.component,
                        Component.text("You and " + player.getName() + " are now friends!", Colors.HOTPINK),
                    })
                .hoverEvent(HoverEvent.showText(Component.text("/friends", Colors.HOTPINK)))
                .clickEvent(ClickEvent.runCommand("/friends")));
            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 2.0f);
            PluginPlayerEvent.Name.MAKE_FRIEND.call(plugin, player);
            PluginPlayerEvent.Name.MAKE_FRIEND.call(plugin, target);
            Database.fillCacheAsync(player);
            Database.fillCacheAsync(target);
        } else {
            requests.put(player.getUniqueId(), target.getUniqueId());
            target.sendMessage(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                        Mytems.HALF_HEART.component,
                        Component.text(player.getName() + " sent you a friend request! Click here to accept", Colors.HOTPINK),
                    })
                .hoverEvent(HoverEvent.showText(Component.text("/friend " + player.getName(), Colors.HOTPINK)))
                .clickEvent(ClickEvent.runCommand("/friend " + player.getName())));
            player.sendMessage(Component.text("Friend request sent to " + target.getName(), Colors.HOTPINK));
        }
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        return null;
    }
}
