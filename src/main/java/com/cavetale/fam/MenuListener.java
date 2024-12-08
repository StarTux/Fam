package com.cavetale.fam;

import com.cavetale.core.menu.MenuItemEvent;
import com.cavetale.fam.util.Colors;
import com.cavetale.mytems.Mytems;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import static com.cavetale.fam.util.Items.makeSkull;
import static com.cavetale.mytems.util.Items.tooltip;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class MenuListener implements Listener {
    public static final String MENU_KEY = "fam:profile";

    protected void enable() {
        Bukkit.getPluginManager().registerEvents(this, FamPlugin.famPlugin());
    }

    @EventHandler
    private void onMenuItem(MenuItemEvent event) {
        if (event.getPlayer().hasPermission("fam.friends")) {
            event.addItem(builder -> builder
                          .key("fam:profile")
                          .command("profile")
                          .icon(tooltip(makeSkull(event.getPlayer()),
                                        List.of(text("Profile", Colors.BLUE),
                                                text("Friends", Colors.HOTPINK),
                                                text("Trophies", Colors.GOLD),
                                                text("Birthdays", Colors.LIGHT_BLUE)))));
            event.addItem(builder -> builder
                          .key("fam:friends")
                          .command("friends")
                          .icon(Mytems.HEART.createIcon(List.of(text("Friends", Colors.HOTPINK),
                                                                text("Birthdays", Colors.LIGHT_BLUE)))));
            event.addItem(builder -> builder
                          .key("fam:trophy")
                          .command("trophy")
                          .icon(Mytems.GOLDEN_CUP.createIcon(List.of(text("Trophies", Colors.GOLD)))));
        }
        if (event.getPlayer().hasPermission("fam.advent")) {
            event.addItem(builder -> builder
                          .key("fam:advent")
                          .command("advent")
                          .icon(Mytems.CHRISTMAS_TOKEN.createIcon(List.of(text("Advent Calendar", RED)))));
        }
    }
}
