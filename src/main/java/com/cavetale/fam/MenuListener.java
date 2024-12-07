package com.cavetale.fam;

import com.cavetale.core.menu.MenuItemClickEvent;
import com.cavetale.core.menu.MenuItemEvent;
import com.cavetale.fam.session.Session;
import com.cavetale.fam.util.Colors;
import com.cavetale.mytems.Mytems;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class MenuListener implements Listener {
    public static final String MENU_KEY = "fam:profile";
    public static final String MENU_PERMISSION = "fam.friends";

    protected void enable() {
        Bukkit.getPluginManager().registerEvents(this, FamPlugin.famPlugin());
    }

    @EventHandler
    private void onMenuItem(MenuItemEvent event) {
        if (event.getPlayer().hasPermission(MENU_PERMISSION)) {
            event.addItem(builder -> builder
                          .key(MENU_KEY)
                          .icon(Mytems.HEART.createIcon(List.of(text("Profile", Colors.BLUE),
                                                                text("Friends", Colors.HOTPINK),
                                                                text("Trophies", Colors.GOLD),
                                                                text("Birthdays", Colors.LIGHT_BLUE)))));
        }
        if (event.getPlayer().hasPermission("fam.advent")) {
            event.addItem(builder -> builder
                          .key("fam:advent")
                          .command("advent")
                          .icon(Mytems.CHRISTMAS_TOKEN.createIcon(List.of(text("Advent Calendar", RED)))));
        }
    }

    @EventHandler
    private void onMenuItemClick(MenuItemClickEvent event) {
        if (MENU_KEY.equals(event.getEntry().getKey())) {
            if (!event.getPlayer().hasPermission(MENU_PERMISSION)) {
                return;
            }
            final Session session = Session.of(event.getPlayer());
            if (!session.isReady()) {
                return;
            }
            new ProfileDialogue(FamPlugin.famPlugin(), session).open(event.getPlayer());
        }
    }
}
