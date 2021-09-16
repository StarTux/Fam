package com.cavetale.fam;

import com.cavetale.fam.sql.Database;
import com.cavetale.fam.util.Items;
import com.cavetale.fam.util.Text;
import com.cavetale.sidebar.PlayerSidebarEvent;
import com.cavetale.sidebar.Priority;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
        String message = bg + "Your " + hl + "/valentine" + bg + " score: " + hl
            + Database.getCachedScore(event.getPlayer().getUniqueId())
            + bg + ". Today's gift item: " + hl + Items.getDisplayName(plugin.getTodaysGift());
        List<String> text = Text.wrapLine(message, 18);
        List<Component> lines = new ArrayList<>(text.size());
        for (String line : text) {
            lines.add(Component.text(line));
        }
        event.add(plugin, Priority.DEFAULT, lines);
    }
}
