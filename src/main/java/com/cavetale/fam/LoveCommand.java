package com.cavetale.fam;

import com.cavetale.fam.sql.Database;
import com.cavetale.fam.util.Colors;
import com.cavetale.fam.util.Text;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class LoveCommand implements TabExecutor {
    private final FamPlugin plugin;

    public void enable() {
        plugin.getCommand("love").setExecutor(this);
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length != 0) return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage("[fam:love] Player expected");
            return true;
        }
        Player player = (Player) sender;
        Player target = Database.getCachedMarriage(player);
        if (target == null) {
            player.sendMessage(Component.text("No spouse in sight", NamedTextColor.RED));
            return true;
        }
        player.getWorld().spawnParticle(Particle.HEART, player.getEyeLocation(), 4, 0.5f, 0.5f, 0.5f, 0.0);
        target.getWorld().spawnParticle(Particle.HEART, target.getEyeLocation(), 4, 0.5f, 0.5f, 0.5f, 0.0);
        player.sendMessage(Component.text("You send " + target.getName() + " your love " + Text.HEART_ICON, Colors.HOTPINK));
        target.sendActionBar(Component.text(player.getName() + " sends you their love " + Text.HEART_ICON, Colors.HOTPINK));
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        return Collections.emptyList();
    }
}
