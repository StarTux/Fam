package com.cavetale.fam;

import com.cavetale.core.playercache.PlayerCache;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;

@RequiredArgsConstructor
public final class FriendsCommand implements TabExecutor {
    private final FamPlugin plugin;

    public void enable() {
        plugin.getCommand("friends").setExecutor(this);
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length > 1) return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage("[Fam] Player expected");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            plugin.openFriendshipsGui(player, 1);
            return true;
        }
        String name = args[0];
        UUID uuid = PlayerCache.uuidForName(name);
        if (uuid == null) {
            player.sendMessage(text("Player not found: " + name, NamedTextColor.RED));
            return true;
        }
        if (uuid.equals(player.getUniqueId())) {
            player.sendMessage(text(name + " is you :)", NamedTextColor.RED));
            return true;
        }
        plugin.openFriendGui(player, uuid, 1);
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        return args.length == 1 ? null : Collections.emptyList();
    }
}
