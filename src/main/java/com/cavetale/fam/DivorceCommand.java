package com.cavetale.fam;

import com.cavetale.core.font.DefaultFont;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.fam.sql.Database;
import com.cavetale.fam.sql.SQLFriends;
import com.cavetale.fam.util.Gui;
import com.cavetale.fam.util.Items;
import com.cavetale.mytems.Mytems;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

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
                            player.sendMessage(Component.text("You're not married!", NamedTextColor.RED));
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
        Component title = Component.text()
            .append(Component.empty())
            .append(DefaultFont.guiBlankOverlay(size, TextColor.color(0xFF0000)))
            .append(Component.text("Really Divorce " + name + "?", NamedTextColor.WHITE))
            .build();
        gui.title(title);
        gui.size(size);
        gui.setItem(size - 8, Items.button(Mytems.OK, Component.text("Yes, Divorce " + name, NamedTextColor.GREEN)), click -> {
                if (!click.isLeftClick()) return;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 1.0f);
                Database.db().update(SQLFriends.class)
                    .row(row)
                    .atomic("relation", null)
                    .async(success -> {
                            // sync
                            if (success <= 0) {
                                player.sendMessage(Component.text("Something went wrong.", NamedTextColor.RED));
                                return;
                            }
                            Database.friendLogAsync(uuid, other, Relation.MARRIED, "Divorced");
                            player.sendMessage(Component.text("You and " + name + " are now divorced!", NamedTextColor.AQUA));
                            Player otherPlayer = Bukkit.getPlayer(other);
                            if (otherPlayer != null) {
                                otherPlayer.sendMessage(Component.text("You and " + player.getName() + " are now divorced!",
                                                                       NamedTextColor.AQUA));
                            }
                        });
                player.closeInventory();
            });
        gui.setItem(size - 2, Items.button(Mytems.NO, Component.text("No, stay married to " + name, NamedTextColor.RED)), click -> {
                if (!click.isLeftClick()) return;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 1.0f);
                player.closeInventory();
            });
        gui.setItem(4, plugin.makeSkull(player, row, null));
        gui.open(player);
    }
}
