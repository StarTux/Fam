package com.cavetale.fam;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.fam.sql.Database;
import com.winthier.playercache.PlayerCache;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        var friendshipNode = rootNode.addChild("friendship")
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
        sender.sendMessage(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                    Component.text("Cache of " + target.getName(), NamedTextColor.YELLOW),
                    Component.newline(),
                    Component.text("Friends ", NamedTextColor.GRAY),
                    Component.text("" + Database.countFriendsCached(target) + " ",
                                   NamedTextColor.YELLOW),
                    Component.text(friends.stream()
                                   .map(uuid -> PlayerCache.nameForUuid(uuid))
                                   .collect(Collectors.joining(" ")),
                                   NamedTextColor.WHITE),
                    Component.newline(),
                    Component.text("Married ", NamedTextColor.GRAY),
                    Component.text(married != null
                                   ? PlayerCache.nameForUuid(married)
                                   : "No",
                                   NamedTextColor.GRAY),
                }));
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
}
