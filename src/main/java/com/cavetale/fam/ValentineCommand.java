package com.cavetale.fam;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
        if (args.length > 1) return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage("[Fam] Player expected");
            return true;
        }
        if (args.length == 0) {
            plugin.openRewardsGui((Player) sender);
            return true;
        }
        switch (args[0]) {
        case "hi": case "highscore":
            plugin.showHighscore((Player) sender, 1);
            return true;
        default:
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length != 1) return Collections.emptyList();
        return Stream.of("hi", "highscore")
            .filter(s -> s.startsWith(args[0]))
            .collect(Collectors.toList());
    }
}
