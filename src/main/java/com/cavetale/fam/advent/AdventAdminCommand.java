package com.cavetale.fam.advent;

import com.cavetale.core.chat.Chat;
import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.fam.FamPlugin;
import com.cavetale.mytems.Mytems;
import java.util.Date;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static com.cavetale.fam.FamPlugin.plugin;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class AdventAdminCommand extends AbstractCommand<FamPlugin> {
    public AdventAdminCommand(final FamPlugin plugin) {
        super(plugin, "adventadmin");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("unlock").arguments("<player> <year> <day>")
            .completers(CommandArgCompleter.ONLINE_PLAYERS,
                        CommandArgCompleter.integer(i -> i > 0),
                        CommandArgCompleter.integer(i -> i > 0 && i <= Advent.MAX_DAY))
            .senderCaller(this::unlock);
        rootNode.addChild("start").arguments("<player> <day>")
            .description("Start an advent daily")
            .completers(CommandArgCompleter.ONLINE_PLAYERS,
                        CommandArgCompleter.integer(i -> i > 0 && i <= Advent.MAX_DAY))
            .senderCaller(this::start);
        rootNode.addChild("stop").arguments("<player>")
            .description("Start current advent daily")
            .completers(CommandArgCompleter.ONLINE_PLAYERS)
            .senderCaller(this::stop);
        rootNode.addChild("info").arguments("<info>")
            .description("Get player info")
            .completers(CommandArgCompleter.ONLINE_PLAYERS)
            .senderCaller(this::info);
        rootNode.addChild("music").denyTabCompletion()
            .description("Music test")
            .playerCaller(this::music);
    }

    private boolean unlock(CommandSender sender, String[] args) {
        if (args.length != 3) return false;
        final PlayerCache target = PlayerCache.require(args[0]);
        final int year = CommandArgCompleter.requireInt(args[1], i -> i > 0);
        final int day = CommandArgCompleter.requireInt(args[2], i -> i > 0 && i <= Advent.MAX_DAY);
        if (year != Advent.THIS_YEAR) {
            throw new CommandWarn("Not this year: " + year);
        }
        plugin().getDatabase().update(SQLAdventPlayer.class)
            .where(s -> s
                   .eq("player", target.uuid)
                   .eq("year", year)
                   .eq("day", day)
                   .eq("opened", true)
                   .eq("solved", false))
            .set("solved", true)
            .set("solvedTime", new Date())
            .async(res -> {
                    if (res == 0) {
                        sender.sendMessage(text("Opened unsolved row not found: " + target.name + ", " + year + ", " + day, RED));
                    } else {
                        sender.sendMessage(text("Solved: " + target.name + ", " + year + ", " + day, GREEN));
                        final Player player = Bukkit.getPlayer(target.uuid);
                        if (player != null) {
                            Chat.sendAndLog(player, textOfChildren(Mytems.CHRISTMAS_TOKEN,
                                                                   text("You completed day " + day + " of the Advent Calendar", GREEN))
                                            .hoverEvent(showText(text("/advent", GRAY)))
                                            .clickEvent(runCommand("/advent")));
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1f, 1.65f);
                        }
                    }
                });
        return true;
    }

    private boolean start(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        final Player player = CommandArgCompleter.requirePlayer(args[0]);
        final int day = CommandArgCompleter.requireInt(args[1], i -> i > 0 && i <= Advent.MAX_DAY);
        final AdventDaily daily = AdventDailies.getDaily(day);
        final AdventSession session = AdventSession.of(player);
        session.startDaily(daily);
        session.save();
        sender.sendMessage(text("Started advent daily for " + player.getName() + ": " + daily.getDay(), YELLOW));
        return true;
    }

    private boolean stop(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        final Player player = CommandArgCompleter.requirePlayer(args[0]);
        final AdventSession session = AdventSession.of(player);
        final AdventDaily daily = session.getDaily();
        if (daily == null) {
            throw new CommandWarn("Player does not have daily: " + player.getName());
        }
        session.stopDaily();
        session.save();
        sender.sendMessage(text("Stopped advent daily for " + player.getName() + ": " + daily.getDay(), YELLOW));
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        final Player player = CommandArgCompleter.requirePlayer(args[0]);
        final AdventSession session = AdventSession.of(player);
        final AdventDaily daily = session.getDaily();
        sender.sendMessage("" + player.getName()
                           + " enabled:" + session.isEnabled()
                           + " disabled:" + session.isDisabled()
                           + " day:" + session.getRow().getDay()
                           + " tag:" + session.getRow().getTag()
                           + " daily:" + (session.getDaily() != null ? session.getDaily().getDay() : "N/A")
                           + (session.getDaily() != null ? "/" + session.getDaily().getClass().getSimpleName() : ""));
        return true;
    }

    private void music(Player player) {
        AdventMusic.deckTheHalls(player);
    }
}
