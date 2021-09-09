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
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public final class BirthdayDialogue {
    private final FamPlugin plugin;
    private Month month = null;
    private int day = 0; // 1-31

    public static final TextColor BG = TextColor.color(0xC99868);
    public static final TextColor COLOR = TextColor.color(0x366797);
    public static final Locale LOCALE = Locale.US;

    public void open(Player player) {
        openMonthGui(player);
    }

    private Gui openMonthGui(Player player) {
        int size = 18;
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
            gui.setItem(i, icon, click -> {
                    if (!click.isLeftClick()) return;
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
                    month = theMonth;
                    gui.close(player);
                    openDayGui(player);
                });
        }
        gui.open(player);
        return gui;
    }

    private Gui openDayGui(Player player) {
        int length = Objects.requireNonNull(month).maxLength();
        int size = 9 * ((length - 1) / 9 + 1);
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
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
                    day = theDay;
                    gui.close(player);
                    openConfirmGui(player);
                });
        }
        gui.open(player);
        return gui;
    }

    private Gui openConfirmGui(Player player) {
        int size = 18;
        String birthdayName = Objects.requireNonNull(month).getDisplayName(TextStyle.FULL, LOCALE) + " " + day;
        Gui gui = new Gui(plugin)
            .size(size)
            .title(Component.text()
                   .append(DefaultFont.guiBlankOverlay(size, BG))
                   .append(Component.text("Your birthday is on " + birthdayName + "?", COLOR))
                   .build());
        gui.setItem(size - 8, Items.button(Mytems.OK, ChatColor.GREEN + "Yes, my birthday is on " + birthdayName), click -> {
                if (!click.isLeftClick()) return;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
                gui.close(player);
                confirm(player);
            });
        gui.setItem(size - 2, Items.button(Mytems.NO, ChatColor.RED + "No, go back!"), click -> {
                if (!click.isLeftClick()) return;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
                gui.close(player);
                month = null;
                day = 0;
                open(player);
            });
        gui.setItem(4, Items.makeSkull(player));
        gui.open(player);
        return gui;
    }

    private void confirm(Player player) {
        SQLBirthday row = new SQLBirthday(player.getUniqueId(), month.getValue(), day);
        plugin.getDatabase().saveAsync(row, count -> {
                if (count <= 0) {
                    player.sendMessage(Component.text("Something went wrong!", NamedTextColor.RED));
                    return;
                }
                String birthdayName = Objects.requireNonNull(month).getDisplayName(TextStyle.FULL, LOCALE) + " " + day;
                player.sendMessage(Component.text("Your birthday was set to " + birthdayName + "!", COLOR));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.25f, 2.0f);
                Database.fillCacheAsync(player);
                PluginPlayerEvent.Name.ENTER_BIRTHDAY.call(plugin, player);
            });
    }
}
