package com.cavetale.fam;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.command.RemotePlayer;
import com.cavetale.core.connect.Connect;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.fam.sql.Database;
import com.cavetale.fam.sql.SQLFriends;
import com.cavetale.fam.util.Colors;
import com.cavetale.mytems.Mytems;
import com.winthier.connect.Redis;
import java.util.UUID;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

public final class FriendCommand extends AbstractCommand<FamPlugin> {
    private static final String REDIS_PREFIX = "friend_request.";
    public static final String CONNECT_FRIEND_DID_ACCEPT = "connect:friend_did_accept";

    protected FriendCommand(final FamPlugin plugin) {
        super(plugin, "friend");
    }

    @Override
    protected void onEnable() {
        rootNode.description("Friend request")
            .arguments("<player>")
            .completers(CommandArgCompleter.ONLINE_PLAYERS)
            .playerCaller(this::friend);
    }

    private boolean friend(Player player, String[] args) {
        if (args.length != 1) return false;
        final RemotePlayer target = CommandArgCompleter.requireRemotePlayer(args[0]);
        final UUID a = player.getUniqueId();
        final UUID b = target.getUniqueId();
        if (a.equals(b)) {
            throw new CommandWarn("You cannot friend yourself");
        }
        Database.db().scheduleAsyncTask(() -> {
                SQLFriends friends = Database.findFriends(a, b);
                Bukkit.getScheduler().runTask(plugin, () -> callback(player, target, friends));
            });
        return true;
    }

    private void callback(Player player, RemotePlayer target, SQLFriends row) {
        if (!player.isOnline()) return;
        if (row == null || row.getFriendship() < 60) {
            player.sendMessage(textOfChildren(text("You need at least "),
                                              SQLFriends.getHeartsComponent(60),
                                              text(" with " + target.getName()))
                               .color(RED));
            return;
        }
        final Relation relation = row.getRelationFor(player.getUniqueId());
        if (relation != null) {
            player.sendMessage(text(target.getName() + " is already your " + relation.getYour() + "!", RED));
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                // If this is a response, the target will have sent us
                // a request, so their request will be stored in
                // redis, with our UUID as a value.
                final String redisKey = REDIS_PREFIX + target.getUniqueId();
                final String request = Redis.get(redisKey);
                if (request == null || !request.equals(player.getUniqueId().toString())) {
                    Redis.set(REDIS_PREFIX + player.getUniqueId(), target.getUniqueId().toString(), 60L);
                    Bukkit.getScheduler().runTask(plugin, () -> didMakeRequest(player, target));
                } else {
                    Redis.del(redisKey);
                    Bukkit.getScheduler().runTask(plugin, () -> didAcceptRequest(player, target, row));
                }
            });
    }

    private void didAcceptRequest(Player player, RemotePlayer target, SQLFriends row) {
        Database.setRelation(row, player.getUniqueId(), Relation.FRIEND);
        player.sendMessage(textOfChildren(Mytems.HEART.component,
                                          text("You and " + target.getName() + " are now friends!", Colors.HOTPINK))
                           .hoverEvent(HoverEvent.showText(text("/friends", Colors.HOTPINK)))
                           .clickEvent(ClickEvent.runCommand("/friends")));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 2.0f);
        target.sendMessage(textOfChildren(Mytems.HEART.component,
                                          text("You and " + player.getName() + " are now friends!", Colors.HOTPINK))
                           .hoverEvent(HoverEvent.showText(text("/friends", Colors.HOTPINK)))
                           .clickEvent(ClickEvent.runCommand("/friends")));
        PluginPlayerEvent.Name.MAKE_FRIEND.call(plugin, player);
        Database.fillCacheAsync(player);
        if (target.isPlayer()) {
            final Player targetPlayer = target.getPlayer();
            onFriendDidAccept(target.getPlayer());
        } else {
            Connect.get().broadcastMessage(CONNECT_FRIEND_DID_ACCEPT, target.getUniqueId().toString());
        }
    }

    /**
     * Called by didAcceptRequest() and ConnectListener.
     */
    public void onFriendDidAccept(Player player) {
        Database.fillCacheAsync(player);
        PluginPlayerEvent.Name.MAKE_FRIEND.call(plugin, player);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 2.0f);
    }

    private void didMakeRequest(Player player, RemotePlayer target) {
        target.sendMessage(textOfChildren(Mytems.HALF_HEART.component,
                                          text(player.getName() + " sent you a friend request! Click here to accept", Colors.HOTPINK))
                           .hoverEvent(HoverEvent.showText(text("/friend " + player.getName(), Colors.HOTPINK)))
                           .clickEvent(ClickEvent.runCommand("/friend " + player.getName())));
        player.sendMessage(textOfChildren(Mytems.HALF_HEART.component,
                                          text("Friend request sent to " + target.getName(), Colors.HOTPINK)));
    }
}
