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
import net.kyori.adventure.text.TextComponent;
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
        rootNode.addChild("friendship").arguments("<amount> <playerA> <playerB...>")
            .description("Increase friendship between 2 or more players")
            .completers(CommandArgCompleter.integer(i -> i > 0),
                        CommandArgCompleter.NULL,
                        CommandArgCompleter.REPEAT)
            .senderCaller(this::friendGroup);
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
        sender.sendMessage(TextComponent.ofChildren(new Component[] {
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

    boolean friendGroup(CommandSender sender, String[] args) {
        if (args.length < 3) return false;
        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch (IllegalArgumentException iae) {
            throw new CommandWarn("Not a number: " + args[0]);
        }
        if (amount < 1) throw new CommandWarn("Illegal amount: " + amount);
        List<PlayerCache> players = new ArrayList<>(args.length - 1);
        for (int i = 1; i < args.length; i += 1) {
            PlayerCache player = PlayerCache.forArg(args[i]);
            if (player == null) {
                throw new CommandWarn("Player not found: " + args[i]);
            }
            if (players.contains(player)) {
                throw new CommandWarn("Duplicate player: " + player.name);
            }
            players.add(player);
        }
        int count = Database.increaseFriendship(players.stream().map(PlayerCache::getUuid).collect(Collectors.toSet()), amount);
        sender.sendMessage(count + " friendships increased by " + amount + ": "
                           + players.stream().map(PlayerCache::getName).collect(Collectors.joining(", ")));
        return true;
    }
}
