package com.cavetale.fam;

import com.cavetale.fam.sql.Database;
import com.cavetale.fam.util.Text;
import com.cavetale.sidebar.PlayerSidebarEvent;
import com.cavetale.sidebar.Priority;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@RequiredArgsConstructor
public final class SidebarListener implements Listener {
    private final FamPlugin plugin;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    void onPlayerSidebar(PlayerSidebarEvent event) {
        if (!Timer.isValentineSeason()) return;
        if (!event.getPlayer().hasPermission("fam.friends")) return;
        ChatColor bg = ChatColor.LIGHT_PURPLE;
        ChatColor hl = ChatColor.GRAY;
        String text = bg + "Your " + hl + "/valentine" + bg + " score: " + hl
            + Database.getCachedScore(event.getPlayer().getUniqueId())
            + bg + ". Today's gift item: " + hl + Text.toCamelCase(plugin.getTodaysFood());
        List<String> lines = Text.wrapLine(text, 18);
        event.addLines(plugin, Priority.DEFAULT, lines);
    }
}
