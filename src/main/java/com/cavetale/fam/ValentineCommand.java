package com.cavetale.fam;

import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class ValentineCommand implements TabExecutor {
    private final FamPlugin plugin;

    public void enable() {
        plugin.getCommand("valentine").setExecutor(this);
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length != 0) return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage("[Fam] Player expected");
            return true;
        }
        plugin.openRewardsGui((Player) sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        return Collections.emptyList();
    }
}
