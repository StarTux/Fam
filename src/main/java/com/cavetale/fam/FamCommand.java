package com.cavetale.fam;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.fam.session.Session;
import com.cavetale.fam.sql.Database;
import com.cavetale.fam.sql.SQLBirthday;
import com.cavetale.fam.sql.SQLFriends;
import com.cavetale.fam.sql.SQLPlayer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class FamCommand extends AbstractCommand<FamPlugin> {
    protected FamCommand(final FamPlugin plugin) {
        super(plugin, "fam");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("info").denyTabCompletion()
            .description("Dump debug info")
            .senderCaller(this::info);
        rootNode.addChild("daybreak").denyTabCompletion()
            .description("Simulate daybreak")
            .senderCaller(this::daybreak);
        rootNode.addChild("compute").denyTabCompletion()
            .description("Compute possible daybreak")
            .senderCaller(this::daybreak);
        rootNode.addChild("cache").arguments("<player>")
            .description("Dump the cache")
            .senderCaller(this::cache);
        rootNode.addChild("transfer").arguments("<from> <to>")
            .description("Account transfer")
            .completers(PlayerCache.NAME_COMPLETER, PlayerCache.NAME_COMPLETER)
            .senderCaller(this::transfer);
        CommandNode friendshipNode = rootNode.addChild("friendship")
            .description("Friendship increase options");
        friendshipNode.addChild("mutual")
            .arguments("<amount> <playerA> <playerB...>")
            .description("Mutual friend bonus between 2 or more players")
            .completers(CommandArgCompleter.integer(i -> i > 0),
                        CommandArgCompleter.NULL,
                        CommandArgCompleter.REPEAT)
            .senderCaller(this::friendshipMutual);
        friendshipNode.addChild("single")
            .arguments("<amount> <player> <friends...>")
            .description("Friend bonus between a player with one or more others")
            .completers(CommandArgCompleter.integer(i -> i > 0),
                        CommandArgCompleter.NULL,
                        CommandArgCompleter.REPEAT)
            .senderCaller(this::friendshipSingle);
        friendshipNode.addChild("fixtodayprogress").denyTabCompletion()
            .description("Fix progress from today's missed scores")
            .senderCaller(this::friendshipFixTodayProgress);
        CommandNode statusNode = rootNode.addChild("status")
            .description("Status message commands");
        statusNode.addChild("get").arguments("<player>")
            .description("Get player status message")
            .completers(PlayerCache.NAME_COMPLETER)
            .senderCaller(this::statusGet);
        statusNode.addChild("reset").arguments("<player>")
            .description("Reset player status message")
            .completers(PlayerCache.NAME_COMPLETER)
            .senderCaller(this::statusReset);
    }

    boolean info(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        sender.sendMessage("" + Timer.getDayId() + " day=" + Timer.getDayOfWeek()
                           + "\n" + Timer.getYear() + "-" + Timer.getMonth() + "-" + Timer.getDay()
                           + " " + Timer.getHour() + "h"
                           + "\nValentineSeason=" + Timer.isValentineSeason()
                           + "\nValentinesDay=" + Timer.isValentinesDay());
        return true;
    }

    boolean daybreak(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        sender.sendMessage("Simulating daybreak (see console)...");
        plugin.onDaybreak();
        return true;
    }

    boolean compute(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        sender.sendMessage("Computing possible daybreak (see console)...");
        plugin.computePossibleDaybreak();
        return true;
    }

    boolean cache(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) throw new CommandWarn("Player not online: " + args[0]);
        Set<UUID> friends = Database.getFriendsCached(target);
        UUID married = Database.getMarriageCached(target);
        sender.sendMessage(join(noSeparators(), new Component[] {
                    text("Cache of " + target.getName(), YELLOW),
                    newline(),
                    text("Friends ", GRAY),
                    text("" + Database.countFriendsCached(target) + " ",
                         YELLOW),
                    text(friends.stream()
                         .map(uuid -> PlayerCache.nameForUuid(uuid))
                         .collect(Collectors.joining(" ")),
                         WHITE),
                    newline(),
                    text("Married ", GRAY),
                    text(married != null
                         ? PlayerCache.nameForUuid(married)
                         : "No",
                         GRAY),
                }));
        return true;
    }

    private boolean transfer(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        PlayerCache from = PlayerCache.forArg(args[0]);
        if (from == null) throw new CommandWarn("Player not found: " + args[0]);
        PlayerCache to = PlayerCache.forArg(args[1]);
        if (to == null) throw new CommandWarn("Player not found: " + args[1]);
        if (from.equals(to)) throw new CommandWarn("Players are identical: " + from.getName());
        List<SQLFriends> friendsList = Database.findFriendsList(from.uuid);
        SQLBirthday birthday = Database.findBirthday(from.uuid);
        if (friendsList.isEmpty() && birthday == null) {
            throw new CommandWarn(from.name + " does not have any friendship data");
        }
        for (SQLFriends row : friendsList) {
            plugin.getDatabase().delete(row);
            UUID other = row.getOther(from.uuid);
            if (other.equals(to.uuid)) continue;
            row.setId(null);
            row.setUuids(Database.sorted(other, to.uuid));
            plugin.getDatabase().save(row);
        }
        if (birthday != null) {
            plugin.getDatabase().delete(birthday);
            birthday.setId(null);
            birthday.setPlayer(to.uuid);
            plugin.getDatabase().save(birthday);
        }
        int trophies = plugin.trophies.transfer(from.uuid, to.uuid);
        sender.sendMessage(text("Transferred friendship data from " + from.name + " to " + to.name + ":"
                                + " friends=" + friendsList.size()
                                + " birthday=" + (birthday != null ? 1 : 0)
                                + " trophies=" + trophies,
                                YELLOW));
        return true;
    }

    private PlayerCache requirePlayerCache(String arg) {
        PlayerCache result = PlayerCache.forArg(arg);
        if (result == null) throw new CommandWarn("Player not found: " + arg);
        return result;
    }

    private int requireInt(String arg) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException nfe) {
            throw new CommandWarn("Number expected: " + arg);
        }
    }

    boolean friendshipMutual(CommandSender sender, String[] args) {
        if (args.length < 3) return false;
        int amount = requireInt(args[0]);
        if (amount < 1) throw new CommandWarn("Illegal amount: " + amount);
        List<PlayerCache> players = new ArrayList<>(args.length - 1);
        for (int i = 1; i < args.length; i += 1) {
            PlayerCache player = requirePlayerCache(args[i]);
            if (players.contains(player)) {
                throw new CommandWarn("Duplicate player: " + player.name);
            }
            players.add(player);
        }
        Set<UUID> set = players.stream().map(PlayerCache::getUuid).collect(Collectors.toSet());
        int count = Database.increaseMutualFriendship(set, amount);
        sender.sendMessage(count + " friendships increased by " + amount + ": "
                           + players.stream().map(PlayerCache::getName).collect(Collectors.joining(", ")));
        return true;
    }

    boolean friendshipSingle(CommandSender sender, String[] args) {
        if (args.length < 3) return false;
        int amount = requireInt(args[0]);
        if (amount < 1) throw new CommandWarn("Illegal amount: " + amount);
        PlayerCache main = requirePlayerCache(args[1]);
        List<PlayerCache> friends = new ArrayList<>(args.length - 1);
        for (int i = 2; i < args.length; i += 1) {
            PlayerCache player = requirePlayerCache(args[i]);
            if (main.equals(player) || friends.contains(player)) {
                throw new CommandWarn("Duplicate player: " + player.name);
            }
            friends.add(player);
        }
        Set<UUID> set = friends.stream().map(PlayerCache::getUuid).collect(Collectors.toSet());
        int count = Database.increaseSingleFriendship(main.uuid, set, amount);
        sender.sendMessage(count + " friendships of " + main.name + " increased by " + amount + ": "
                           + friends.stream().map(PlayerCache::getName).collect(Collectors.joining(", ")));
        return true;
    }

    private void friendshipFixTodayProgress(CommandSender sender) {
        final int today = Timer.getDayId();
        Database.db().find(SQLFriends.class)
            .eq("dailyGift", today)
            .findListAsync((List<SQLFriends> list) -> {
                    final Set<UUID> players = new HashSet<>();
                    for (SQLFriends row : list) {
                        Database.addProgress(row.getPlayerA());
                        Database.addProgress(row.getPlayerB());
                        players.add(row.getPlayerA());
                        players.add(row.getPlayerB());
                    }
                    sender.sendMessage(text("Increased " + (2 * list.size()) + " scores"
                                            + " for " + players.size() + " players", YELLOW));
                });
    }

    private boolean statusGet(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        PlayerCache target = PlayerCache.require(args[0]);
        SQLPlayer row = Database.db().find(SQLPlayer.class).eq("uuid", target.uuid).findUnique();
        if (row == null || row.getStatusMessage() == null) {
            throw new CommandWarn("Player does not set status message: " + target.name);
        }
        sender.sendMessage(textOfChildren(text("Status message of " + target.name + ": ", YELLOW),
                                          text(row.getStatusMessage(), GRAY)));
        return true;
    }

    private boolean statusReset(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        PlayerCache target = PlayerCache.require(args[0]);
        int result = Database.db().update(SQLPlayer.class)
            .where(w -> w.eq("uuid", target.uuid).isNotNull("statusMessage"))
            .set("statusMessage", null)
            .sync();
        if (result == 0) {
            throw new CommandWarn("Player did not set status message: " + target.name);
        }
        sender.sendMessage(text("Status message of " + target.name + " was reset", YELLOW));
        Session session = Session.of(target.uuid);
        if (session != null) session.reload();
        return true;
    }
}
