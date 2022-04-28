package com.cavetale.fam.trophy;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.editor.EditMenuDelegate;
import com.cavetale.core.editor.EditMenuNode;
import com.cavetale.core.editor.Editor;
import com.cavetale.fam.FamPlugin;
import com.cavetale.fam.sql.Database;
import com.winthier.playercache.PlayerCache;
import java.text.SimpleDateFormat;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class TrophyAdminCommand extends AbstractCommand<FamPlugin> {
    public TrophyAdminCommand(final FamPlugin plugin) {
        super(plugin, "trophyadmin");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("create").arguments("<owner>")
            .completers(CommandArgCompleter.NULL)
            .description("Create a trophy")
            .playerCaller(this::create);
        rootNode.addChild("list").arguments("<owner> [category]")
            .completers(PlayerCache.NAME_COMPLETER,
                        CommandArgCompleter.EMPTY)
            .description("List player trophies")
            .senderCaller(this::list);
        rootNode.addChild("edit").arguments("<id>")
            .completers(CommandArgCompleter.integer(i -> i > 0))
            .description("Edit a trophy")
            .playerCaller(this::edit);
    }

    private boolean create(Player player, String[] args) {
        if (args.length != 1) return false;
        PlayerCache target = PlayerCache.forArg(args[0]);
        if (target == null) throw new CommandWarn("Player not found: " + args[0]);
        SQLTrophy trophy = new SQLTrophy();
        trophy.setOwner(target.uuid);
        trophy.setCategory("");
        trophy.setNow();
        trophy.setIconType("trophy:cup");
        trophy.setTitleComponent(text("test"));
        Editor.get().open(plugin, player, trophy, new EditMenuDelegate() {
                @Override
                public Runnable getSaveFunction(EditMenuNode node) {
                    return () -> {
                        Trophies.insertTrophies(List.of(trophy));
                        player.sendMessage(text("Trophy created", GREEN));
                    };
                }
            });
        return true;
    }

    private boolean list(CommandSender sender, String[] args) {
        if (args.length != 1 && args.length != 2) return false;
        final PlayerCache target = PlayerCache.require(args[0]);
        final String category = args.length >= 2 ? args[1] : null;
        if (category != null) {
            Trophies.findTrophiesAsync(target.uuid, category, l -> listCallback(sender, l));
        } else {
            Trophies.findTrophiesAsync(target.uuid, l -> listCallback(sender, l));
        }
        return true;
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yy/mm/dd");

    private void listCallback(CommandSender sender, List<SQLTrophy> trophies) {
        if (trophies.isEmpty()) {
            sender.sendMessage(text("No trophies found!", RED));
            return;
        }
        for (SQLTrophy it : trophies) {
            String cmd = "/trophyadmin edit " + it.id;
            sender.sendMessage(join(separator(space()),
                                    text("id=" + it.id
                                         + " " + it.category
                                         + " #" + it.placement
                                         + " " + DATE_FORMAT.format(it.time)),
                                    it.getTitleComponent())
                               .clickEvent(runCommand(cmd))
                               .hoverEvent(showText(text(cmd, YELLOW))));
        }
    }

    private boolean edit(Player player, String[] args) {
        if (args.length != 1) return false;
        final int id = CommandArgCompleter.requireInt(args[0], i -> i > 0);
        Trophies.findTrophyAsync(id, row -> editCallback(player, row, id));
        return true;
    }

    private void editCallback(Player player, SQLTrophy trophy, int id) {
        if (trophy == null) {
            player.sendMessage(text("Trophy not found: " + id, RED));
            return;
        }
        Editor.get().open(plugin, player, trophy, new EditMenuDelegate() {
                @Override
                public Runnable getSaveFunction(EditMenuNode node) {
                    return () -> {
                        Database.db().saveAsync(trophy, result -> {
                                if (result == 0) {
                                    player.sendMessage(text("Saving failed: " + result, RED));
                                } else {
                                    player.sendMessage(text("Trophy saved", GREEN));
                                }
                            });
                    };
                }
            });
    }
}
