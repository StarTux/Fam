package com.cavetale.fam;

import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.core.font.DefaultFont;
import com.cavetale.fam.sql.Database;
import com.cavetale.fam.sql.SQLBirthday;
import com.cavetale.fam.util.Gui;
import com.cavetale.fam.util.Items;
import com.cavetale.mytems.Mytems;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import static org.bukkit.Sound.*;
import static org.bukkit.SoundCategory.*;

@RequiredArgsConstructor
public final class BirthdayDialogue {
    private final FamPlugin plugin;
    private Month month = null;
    private int day = 0; // 1-31

    public static final TextColor BG = TextColor.color(0xC99868);
    public static final TextColor COLOR = TextColor.color(0x366797);
    public static final Locale LOCALE = Locale.US;

    public void open(Player player) {
        plugin.getDatabase().find(SQLBirthday.class)
            .eq("player", player.getUniqueId())
            .deleteAsync(null);
        Database.fillCacheAsync(player);
        openMonthGui(player);
    }

    private Gui openMonthGui(Player player) {
        int size = 36;
        Gui gui = new Gui(plugin)
            .size(size)
            .title(Component.text()
                   .append(DefaultFont.guiBlankOverlay(size, BG))
                   .append(Component.text("Birthday Month", COLOR))
                   .build());
        for (int i = 0; i < 12; i += 1) {
            final Month theMonth = Month.of(i + 1);
            ItemStack icon = Items.button(Mytems.CHECKBOX, theMonth.getDisplayName(TextStyle.FULL, LOCALE));
            icon.setAmount(i + 1);
            gui.setItem(9 + i, icon, click -> {
                    if (!click.isLeftClick()) return;
                    month = theMonth;
                    openDayGui(player);
                    click(player);
                });
        }
        gui.setItem(Gui.OUTSIDE, null, click -> {
                if (!click.isLeftClick()) return;
                new ProfileDialogue(plugin).open(player);
            });
        gui.open(player);
        return gui;
    }

    private Gui openDayGui(Player player) {
        int length = Objects.requireNonNull(month).maxLength();
        int size = Math.max(36, 9 * ((length - 1) / 9 + 1));
        String monthName = month.getDisplayName(TextStyle.FULL, LOCALE);
        Gui gui = new Gui(plugin)
            .size(size)
            .title(Component.text()
                   .append(DefaultFont.guiBlankOverlay(size, BG))
                   .append(Component.text("Birthday in " + monthName, COLOR))
                   .build());
        for (int i = 0; i < length; i += 1) {
            int theDay = i + 1;
            ItemStack icon = Items.button(Mytems.CHECKBOX, monthName + " " + theDay);
            icon.setAmount(theDay);
            gui.setItem(i, icon, click -> {
                    if (!click.isLeftClick()) return;
                    day = theDay;
                    openConfirmGui(player);
                    click(player);
                });
        }
        gui.setItem(Gui.OUTSIDE, null, click -> {
                if (!click.isLeftClick()) return;
                month = null;
                openMonthGui(player);
                click(player);
            });
        gui.open(player);
        return gui;
    }

    private Gui openConfirmGui(Player player) {
        int size = 36;
        String birthdayName = Objects.requireNonNull(month).getDisplayName(TextStyle.FULL, LOCALE) + " " + day;
        Gui gui = new Gui(plugin)
            .size(size)
            .title(Component.text()
                   .append(DefaultFont.guiBlankOverlay(size, BG))
                   .append(Component.text("Your birthday is on " + birthdayName + "?", COLOR))
                   .build());
        gui.setItem(size - 8, Items.button(Mytems.CHECKED_CHECKBOX, ChatColor.GREEN + "Yes, my birthday is on " + birthdayName), click -> {
                if (!click.isLeftClick()) return;
                confirm(player, false);
                gui.close(player);
                click(player);
            });
        gui.setItem(size - 6, Items.button(Mytems.BOMB, ChatColor.AQUA + "Yes, but keep it a secret"), click -> {
                if (!click.isLeftClick()) return;
                gui.close(player);
                confirm(player, true);
                click(player);
            });
        gui.setItem(size - 2, Items.button(Mytems.CROSSED_CHECKBOX, ChatColor.RED + "No, go back!"), click -> {
                if (!click.isLeftClick()) return;
                gui.close(player);
                month = null;
                day = 0;
                open(player);
                click(player);
            });
        gui.setItem(4, Items.makeSkull(player));
        gui.setItem(Gui.OUTSIDE, null, click -> {
                if (!click.isLeftClick()) return;
                day = 0;
                openDayGui(player);
                click(player);
            });
        gui.open(player);
        return gui;
    }

    private void confirm(Player player, boolean secret) {
        if (secret) {
            PluginPlayerEvent.Name.ENTER_BIRTHDAY.call(plugin, player);
            return;
        }
        SQLBirthday row = new SQLBirthday(player.getUniqueId(), month.getValue(), day);
        plugin.getDatabase().saveAsync(row, count -> {
                if (count <= 0) {
                    player.sendMessage(Component.text("Something went wrong!", NamedTextColor.RED));
                    return;
                }
                String birthdayName = Objects.requireNonNull(month).getDisplayName(TextStyle.FULL, LOCALE) + " " + day;
                player.sendMessage(Component.text("Your birthday was set to " + birthdayName + "!", COLOR));
                player.playSound(player.getLocation(), ENTITY_PLAYER_LEVELUP, MASTER, 0.25f, 2.0f);
                Database.fillCacheAsync(player);
                PluginPlayerEvent.Name.ENTER_BIRTHDAY.call(plugin, player);
            });
    }

    private static void click(Player player) {
        player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 1.0f);
    }
}
