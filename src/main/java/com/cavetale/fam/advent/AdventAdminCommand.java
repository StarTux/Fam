package com.cavetale.fam.advent;

import com.cavetale.core.chat.Chat;
import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
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
        rootNode.addChild("unlock").arguments("<player> <day>")
            .completers(PlayerCache.NAME_COMPLETER,
                        CommandArgCompleter.integer(i -> i > 0 && i <= Advent.MAX_DAY))
            .senderCaller(this::unlock);
    }

    private boolean unlock(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        final PlayerCache target = PlayerCache.require(args[0]);
        final int day = CommandArgCompleter.requireInt(args[1], i -> i > 0 && i <= Advent.MAX_DAY);
        plugin().getDatabase().update(SQLAdventPlayer.class)
            .where(s -> s
                   .eq("player", target.uuid)
                   .eq("year", Advent.THIS_YEAR)
                   .eq("day", day)
                   .eq("opened", true)
                   .eq("solved", false))
            .set("solved", true)
            .set("solvedTime", new Date())
            .async(res -> {
                    if (res == 0) {
                        sender.sendMessage(text("Opened unsolved row not found: " + target.name + ", " + Advent.THIS_YEAR + ", " + day, RED));
                    } else {
                        sender.sendMessage(text("Solved: " + target.name + ", " + Advent.THIS_YEAR + ", " + day, GREEN));
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
}
