package com.cavetale.fam.advent;

import com.cavetale.core.connect.NetworkServer;
import com.cavetale.core.event.item.PlayerReceiveItemsEvent;
import com.cavetale.core.font.GuiOverlay;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Gui;
import com.cavetale.mytems.util.Items;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.fam.FamPlugin.plugin;
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

    public void open() {
        Gui gui = new Gui(plugin()).size(27);
        GuiOverlay.Builder builder = GuiOverlay.BLANK.builder(27, RED)
            .title(text("Advent Calendar", GREEN));
        int openedUntil = 0;
        for (int i = 0; i < Advent.MAX_DAY; i += 1) {
            SQLAdventPlayer row = rows.get(i);
            if (!(row.isOpened() && row.isSolved())) break;
            openedUntil = row.getDay(); // == i + 1
        }
        for (int i = 0; i < 25; i += 1) {
            final int day = i + 1;
            if (day > openedUntil + 1 || day > Advent.MAX_DAY) {
                gui.setItem(i, lockedItem(day));
                continue;
            }
            final SQLAdventPlayer row = rows.get(i);
            if (!row.isOpened()) {
                gui.setItem(i, openableItem(day), click -> {
                        if (!click.isLeftClick()) return;
                        if (bigLock) return;
                        bigLock = true;
                        plugin().getDatabase().update(SQLAdventPlayer.class)
                            .row(row)
                            .atomic("opened", true)
                            .set("openedTime", new Date())
                            .async(r -> open(player));
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    });
            } else if (!row.isSolved()) {
                gui.setItem(i, unsolvedItem(day));
            } else if (!row.isRewarded()) {
                gui.setItem(i, solvedItem(day), click -> {
                        if (!click.isLeftClick()) return;
                        if (bigLock) return;
                        if (!NetworkServer.current().isSurvival()) {
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.5f);
                            player.sendMessage(textOfChildren(Mytems.CHRISTMAS_TOKEN,
                                                              text("Please return to Survival to open your present", RED)));
                            return;
                        }
                        bigLock = true;
                        plugin().getLogger().info("[Advent] " + player.getName() + " is opening reward for day " + day);
                        plugin().getDatabase().update(SQLAdventPlayer.class)
                            .row(row)
                            .atomic("rewarded", true)
                            .set("rewardedTime", new Date())
                            .async(r -> {
                                    openReward(day);
                                    plugin().getLogger().info("[Advent] " + player.getName() + " opened reward for day " + day);
                                });
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    });
            } else {
                gui.setItem(i, rewardedItem(day));
            }
        }
        gui.title(builder.build());
        gui.open(player);
    }

    private static ItemStack lockedItem(int day) {
        return Items.text(new ItemStack(Material.IRON_DOOR),
                          List.of(text("Door " + day, GRAY),
                                  text("Locked", DARK_GRAY)));
    }

    private static ItemStack openableItem(int day) {
        return Items.text(new ItemStack(Material.OAK_DOOR),
                          List.of(text("Door " + day, GREEN),
                                  textOfChildren(Mytems.MOUSE_LEFT, text(" Open", GRAY))));
    }

    private static ItemStack unsolvedItem(int day) {
        List<Component> text = new ArrayList<>();
        text.add(text("Door " + day, GREEN));
        for (Component line : getPage(day)) {
            text.add(line.color(WHITE));
        }
        return Items.text(Mytems.CHECKBOX.createIcon(), text);
    }

    private static ItemStack solvedItem(int day) {
        return Items.text(Mytems.CHRISTMAS_TOKEN.createIcon(),
                          List.of(text("Door " + day, GREEN),
                                  textOfChildren(Mytems.MOUSE_LEFT, text(" Open Present", GRAY))));
    }

    private static ItemStack rewardedItem(int day) {
        List<Component> text = new ArrayList<>();
        text.add(text("Door " + day + " Complete!", GREEN));
        for (Component line : getPage(day)) {
            text.add(line.color(GRAY));
        }
        return Items.text(Mytems.CROSSED_CHECKBOX.createIcon(), text);
    }

    public static List<Component> getPage(int day) {
        return switch (day) {
        case 1 -> List.of(text("The first gift of"),
                          text("Christmas awaits if you"),
                          text("complete the parkour at "),
                          textOfChildren(text("/warp XmasParkour", GREEN)));
        case 2 -> List.of(text("The second gift of"),
                          text("Christmas is hidden in the"),
                          text("Flamingo Zone at Spawn"));
        case 3 -> List.of(text("Find the third gift of"),
                          text("Christmas in the big"),
                          text("snow globe at "),
                          textOfChildren(text("/warp XmasChallenge", YELLOW)));
        case 4 -> List.of(text("Win the fourth gift of"),
                          text("from a snowball fight"),
                          text("at the Spawn."));
        default -> List.of();
        };
    }

    private void openReward(int day) {
        Gui gui = new Gui(plugin()).size(27);
        GuiOverlay.Builder builder = GuiOverlay.HOLES.builder(27, RED)
            .title(text("Advent Calendar Day " + day, GREEN));
        gui.setEditable(true);
        gui.setItem(12, Mytems.KITTY_COIN.createItemStack());
        gui.setItem(14, Mytems.KITTY_COIN.createItemStack());
        gui.onClose(close -> {
                PlayerReceiveItemsEvent.receiveInventory(player, gui.getInventory());
            });
        gui.title(builder.build());
        gui.open(player);
    }
}
