package com.cavetale.fam;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

@RequiredArgsConstructor
public final class FamCommand implements TabExecutor {
    private final FamPlugin plugin;

    public void enable() {
        plugin.getCommand("fam").setExecutor(this);
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 0) return false;
        switch (args[0]) {
        case "info":
            sender.sendMessage("" + Timer.getDayId() + " day=" + Timer.getDayOfWeek()
                               + "\n" + Timer.getYear() + "-" + Timer.getMonth() + "-" + Timer.getDay()
                               + " " + Timer.getHour() + "h"
                               + "\nValentineSeason=" + Timer.isValentineSeason()
                               + "\nValentinesDay=" + Timer.isValentinesDay());
            return true;
        default: return false;
        }
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        return Arrays.asList("info");
    }
}
