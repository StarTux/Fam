package com.cavetale.fam;

import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.font.VanillaItems;
import com.cavetale.fam.sql.Database;
import com.cavetale.fam.util.Colors;
import com.cavetale.mytems.Mytems;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import static com.cavetale.core.font.Unicode.tiny;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

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
        List<Component> lines = new ArrayList<>();
        lines.add(textOfChildren(Mytems.LOVE_LETTER, text("/valentine", Colors.HOTPINK)));
        lines.add(textOfChildren(Mytems.LOVE_LETTER, text(tiny("score "), GRAY),
                                 text(Database.getCachedScore(event.getPlayer().getUniqueId()), Colors.HOTPINK)));
        lines.add(textOfChildren(Mytems.LOVE_LETTER, text(tiny("gift "), GRAY), VanillaItems.componentOf(plugin.getTodaysGift())));
        event.sidebar(PlayerHudPriority.DEFAULT, lines);
    }
}
