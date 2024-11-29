package com.cavetale.fam;

import com.cavetale.core.font.DefaultFont;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.fam.sql.Database;
import com.cavetale.fam.sql.SQLFriends;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Gui;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class DivorceCommand implements TabExecutor {
    private final FamPlugin plugin;

    public void enable() {
        plugin.getCommand("divorce").setExecutor(this);
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length != 0) return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage("[fam:divorce] Player expected");
            return true;
        }
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        Database.db().scheduleAsyncTask(() -> {
                List<SQLFriends> rows = Database.findFriendsList(uuid, Relation.MARRIED);
                if (rows.isEmpty()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage(text("You're not married!", RED));
                        });
                    return;
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                        openDivorceGui(player, rows.get(0));
                    });
            });
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        return Collections.emptyList();
    }

    void openDivorceGui(Player player, SQLFriends row) {
        if (!player.isValid()) return;
        final UUID uuid = player.getUniqueId();
        final UUID other = row.getOther(uuid);
        final String name = PlayerCache.nameForUuid(other);
        Gui gui = new Gui(plugin);
        int size = 18;
        final Component title = textOfChildren(DefaultFont.guiBlankOverlay(size, TextColor.color(0xFF0000)),
                                               text("Really Divorce " + name + "?", WHITE));
        gui.title(title);
        gui.size(size);
        gui.setItem(size - 8, Mytems.OK.createIcon(List.of(text("Yes, Divorce " + name, GREEN))), click -> {
                if (!click.isLeftClick()) return;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 1.0f);
                Database.db().update(SQLFriends.class)
                    .row(row)
                    .atomic("relation", null)
                    .async(success -> {
                            // sync
                            if (success <= 0) {
                                player.sendMessage(text("Something went wrong.", RED));
                                return;
                            }
                            Database.friendLogAsync(uuid, other, Relation.MARRIED, "Divorced");
                            player.sendMessage(text("You and " + name + " are now divorced!", AQUA));
                            Player otherPlayer = Bukkit.getPlayer(other);
                            if (otherPlayer != null) {
                                otherPlayer.sendMessage(text("You and " + player.getName() + " are now divorced!", AQUA));
                            }
                        });
                player.closeInventory();
            });
        gui.setItem(size - 2, Mytems.NO.createIcon(List.of(text("No, stay married to " + name, RED))), click -> {
                if (!click.isLeftClick()) return;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 1.0f);
                player.closeInventory();
            });
        gui.setItem(4, plugin.makeSkull(player, row, null));
        gui.open(player);
    }
}
