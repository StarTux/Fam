package com.cavetale.fam;

import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.item.ItemKinds;
import com.cavetale.fam.sql.Database;
import com.cavetale.fam.util.Text;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public final class SidebarListener implements Listener {
    private final FamPlugin plugin;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    void onPlayerHud(PlayerHudEvent event) {
        if (!Timer.isValentineSeason()) return;
        if (!event.getPlayer().hasPermission("fam.valentine")) return;
        ChatColor bg = ChatColor.LIGHT_PURPLE;
        ChatColor hl = ChatColor.GRAY;
        String message = bg + "Your " + hl + "/valentine" + bg + " score: " + hl
            + Database.getCachedScore(event.getPlayer().getUniqueId())
            + bg + ". Today's gift item: " + hl + ItemKinds.name(new ItemStack(plugin.getTodaysGift()));
        List<String> text = Text.wrapLine(message, 18);
        List<Component> lines = new ArrayList<>(text.size());
        for (String line : text) {
            lines.add(Component.text(line));
        }
        event.sidebar(PlayerHudPriority.DEFAULT, lines);
    }
}
