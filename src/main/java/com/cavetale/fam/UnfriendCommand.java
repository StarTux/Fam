package com.cavetale.fam;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandContext;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.connect.Connect;
import com.cavetale.core.font.GuiOverlay;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.fam.sql.Database;
import com.cavetale.fam.sql.SQLFriends;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Gui;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

public final class UnfriendCommand extends AbstractCommand<FamPlugin> {
    public static final String CONNECT_UNFRIENDED = "connect:unfriended";

    protected UnfriendCommand(final FamPlugin plugin) {
        super(plugin, "unfriend");
    }

    @Override
    protected void onEnable() {
        rootNode.description("Remove a friend")
            .completers(this::completeFriends)
            .playerCaller(this::unfriend);
    }

    private List<String> completeFriends(CommandContext context, CommandNode node, String arg) {
        if (!context.isPlayer()) return List.of();
        final List<String> result = new ArrayList<>();
        final String lower = arg.toLowerCase();
        for (UUID uuid : Database.getCache(context.player).getFriends()) {
            final String name = PlayerCache.nameForUuid(uuid);
            if (name.toLowerCase().contains(lower)) {
                result.add(name);
            }
        }
        return result;
    }

    private boolean unfriend(Player player, String[] args) {
        if (args.length != 1) return false;
        final PlayerCache target = CommandArgCompleter.requirePlayerCache(args[0]);
        Database.db().scheduleAsyncTask(() -> {
                final SQLFriends row = Database.findFriends(player.getUniqueId(), target.getUniqueId());
                Bukkit.getScheduler().runTask(plugin, () -> callback(player, target, row));
            });
        return true;
    }

    private void callback(Player player, PlayerCache target, SQLFriends row) {
        if (!player.isOnline()) return;
        if (row == null || row.getRelationEnum() != Relation.FRIEND) {
            player.sendMessage(text("You and " + target.name + " are not friends", RED));
            return;
        }
        openUnfriendGui(player, target, row);
    }

    private void openUnfriendGui(Player player, PlayerCache target, SQLFriends row) {
        if (!player.isValid()) return;
        final UUID uuid = player.getUniqueId();
        final int size = 18;
        final Gui gui = new Gui(plugin)
            .size(size)
            .layer(GuiOverlay.BLANK, TextColor.color(0xFF0000))
            .title(text("Really Unfriend " + target.name + "?", WHITE));
        gui.setItem(size - 8, Mytems.OK.createIcon(List.of(text("Yes, unfriend " + target.name, GREEN))), click -> {
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
                            player.sendMessage(text("You and " + target.name + " are no longer friends", AQUA));
                            final Player otherPlayer = Bukkit.getPlayer(target.uuid);
                            if (otherPlayer != null) {
                                Database.fillCacheAsync(otherPlayer);
                            } else {
                                Connect.get().broadcastMessage(CONNECT_UNFRIENDED, target.uuid.toString());
                            }
                        });
                player.closeInventory();
            });
        gui.setItem(size - 2, Mytems.NO.createIcon(List.of(text("No, stay friends with " + target.name, RED))), click -> {
                if (!click.isLeftClick()) return;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 1.0f);
                player.closeInventory();
            });
        gui.setItem(4, plugin.makeSkull(player, row, null));
        gui.open(player);
    }

    protected void onUnfriended(Player player) {
        Database.fillCacheAsync(player);
    }
}
