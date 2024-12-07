package com.cavetale.fam.advent;

import com.cavetale.core.connect.NetworkServer;
import com.cavetale.core.event.item.PlayerReceiveItemsEvent;
import com.cavetale.core.font.GuiOverlay;
import com.cavetale.core.menu.MenuItemEvent;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Gui;
import com.cavetale.mytems.util.Items;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.fam.FamPlugin.famPlugin;
import static com.cavetale.fam.advent.AdventDailies.getPage;
import static com.cavetale.mytems.util.Items.tooltip;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class AdventCalendar {
    private final Player player;
    private final List<SQLAdventPlayer> rows;
    private boolean bigLock = false;

    public static void open(final Player player) {
        Advent.advent().loadAllAsync(player.getUniqueId(), list -> new AdventCalendar(player, list).open());
    }

    private void open() {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        final int size = 6 * 9;
        Gui gui = new Gui(famPlugin()).size(size);
        GuiOverlay.Builder builder = GuiOverlay.BLANK.builder(size, RED)
            .title(text("Advent Calendar", GREEN));
        final int maxDay = NetworkServer.BETA.isThisServer()
            ? Advent.MAX_DAY
            : Math.min(Advent.MAX_DAY, Advent.getThisDay());
        int openedUntil = 0;
        for (int i = 0; i < maxDay; i += 1) {
            SQLAdventPlayer row = rows.get(i);
            if (!row.isRewarded()) break;
            openedUntil = row.getDay(); // == i + 1
        }
        for (int i = 0; i < 25; i += 1) {
            final int guix = 1 + i % 7;
            final int guiy = 1 + i / 7;
            final int day = i + 1;
            final SQLAdventPlayer row = day <= maxDay
                ? rows.get(i)
                : null;
            if (day > openedUntil + 1 || day > maxDay) {
                gui.setItem(guix, guiy, lockedItem(day), click -> {
                        if (!click.isLeftClick()) return;
                        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 0.9f);
                    });
            } else if (!row.isOpened()) {
                gui.setItem(guix, guiy, openableItem(day), click -> {
                        if (!click.isLeftClick()) return;
                        if (bigLock) return;
                        bigLock = true;
                        famPlugin().getDatabase().update(SQLAdventPlayer.class)
                            .row(row)
                            .atomic("opened", true)
                            .set("openedTime", new Date())
                            .async(r -> open());
                    });
            } else if (!row.isSolved()) {
                gui.setItem(guix, guiy, unsolvedItem(day), click -> {
                        if (!click.isLeftClick()) return;
                        start(day);
                    });
            } else if (!row.isRewarded()) {
                gui.setItem(guix, guiy, solvedItem(day), click -> {
                        if (!click.isLeftClick()) return;
                        if (bigLock) return;
                        if (!NetworkServer.current().isSurvival()) {
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.5f);
                            player.sendMessage(textOfChildren(Mytems.CHRISTMAS_TOKEN,
                                                              text("Please return to Survival to open your present", RED)));
                            return;
                        }
                        bigLock = true;
                        famPlugin().getLogger().info("[Advent] " + player.getName() + " is opening reward for day " + day);
                        famPlugin().getDatabase().update(SQLAdventPlayer.class)
                            .row(row)
                            .atomic("rewarded", true)
                            .set("rewardedTime", new Date())
                            .async(r -> {
                                    openReward(day);
                                    famPlugin().getLogger().info("[Advent] " + player.getName() + " opened reward for day " + day);
                                });
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    });
            } else {
                gui.setItem(guix, guiy, rewardedItem(day), click -> {
                        if (!click.isLeftClick()) return;
                        start(day);
                    });
            }
        }
        gui.title(builder.build());
        gui.setItem(Gui.OUTSIDE, null, click -> {
                if (!click.isLeftClick()) return;
                MenuItemEvent.openMenu(player);
            });
        gui.open(player);
    }

    private static ItemStack lockedItem(int day) {
        return tooltip(Mytems.CHRISTMAS_TOKEN.createItemStack(),
                       List.of(text("Door " + day, GRAY),
                               text("Locked", DARK_GRAY)));
    }

    private static ItemStack openableItem(int day) {
        return Items.iconize(tooltip(Items.colorized(Mytems.PLAY_BUTTON.createItemStack(), Color.LIME),
                                     List.of(text("Door " + day, GREEN),
                                             textOfChildren(Mytems.MOUSE_LEFT, text(" Open", GRAY)))));
    }

    private static ItemStack unsolvedItem(int day) {
        List<Component> text = new ArrayList<>();
        text.add(text("Door " + day, GREEN));
        for (Component line : getPage(day)) {
            text.add(line.color(WHITE));
        }
        text.add(textOfChildren(Mytems.MOUSE_LEFT, text(" Play", GRAY)));
        return tooltip(Mytems.CHECKBOX.createItemStack(), text);
    }

    private static ItemStack solvedItem(int day) {
        List<Component> text = new ArrayList<>();
        text.add(text("Door " + day, GREEN));
        for (Component line : getPage(day)) {
            text.add(line.color(WHITE));
        }
        text.add(textOfChildren(Mytems.MOUSE_LEFT, text(" Open Present", GRAY)));
        return tooltip(Mytems.STAR.createItemStack(), text);
    }

    private static ItemStack rewardedItem(int day) {
        List<Component> text = new ArrayList<>();
        text.add(text("Door " + day + " Complete!", GREEN));
        for (Component line : getPage(day)) {
            text.add(line.color(GRAY));
        }
        text.add(textOfChildren(Mytems.MOUSE_LEFT, text(" Repeat (no reward)", GRAY)));
        return tooltip(Mytems.CHECKED_CHECKBOX.createItemStack(), text);
    }

    private void openReward(int day) {
        Gui gui = new Gui(famPlugin()).size(27);
        GuiOverlay.Builder builder = GuiOverlay.HOLES.builder(27, RED)
            .title(text("Advent Calendar Day " + day, GREEN));
        gui.setEditable(true);
        gui.setItem(2, 1, Mytems.KITTY_COIN.createItemStack(), null);
        gui.setItem(6, 1, Mytems.KITTY_COIN.createItemStack(), null);

        gui.setItem(1, 1, Mytems.RUBY.createItemStack(), null);
        gui.setItem(5, 1, Mytems.RUBY.createItemStack(), null);
        gui.setItem(3, 1, Mytems.RUBY.createItemStack(), null);
        gui.setItem(7, 1, Mytems.RUBY.createItemStack(), null);
        gui.setItem(2, 0, Mytems.RUBY.createItemStack(), null);
        gui.setItem(6, 0, Mytems.RUBY.createItemStack(), null);
        gui.setItem(2, 2, Mytems.RUBY.createItemStack(), null);
        gui.setItem(6, 2, Mytems.RUBY.createItemStack(), null);

        gui.setItem(13, switch (day) {
            case 22 -> Mytems.SANTA_BOOTS.createItemStack();
            case 23 -> Mytems.SANTA_PANTS.createItemStack();
            case 24 -> Mytems.SANTA_JACKET.createItemStack();
            case 25 -> Mytems.SANTA_HAT.createItemStack();
            default -> null;
            });
        gui.onClose(close -> {
                PlayerReceiveItemsEvent.receiveInventory(player, gui.getInventory());
            });
        gui.title(builder.build());
        gui.open(player);
    }

    private void start(int day) {
        if (NetworkServer.current() == NetworkServer.CHALLENGE) return;
        final AdventSession session = AdventSession.of(player);
        final AdventDaily daily = AdventDailies.getDaily(day);
        session.startDaily(daily);
        session.save(() -> {
                final String cmd = "warpadmin send " + player.getName() + " " + daily.getWarp();
                famPlugin().getLogger().info("Dispatching console command: " + cmd);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            });
    }
}
