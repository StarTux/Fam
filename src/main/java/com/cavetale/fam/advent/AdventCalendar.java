package com.cavetale.fam.advent;

import com.cavetale.core.connect.NetworkServer;
import com.cavetale.core.event.item.PlayerReceiveItemsEvent;
import com.cavetale.core.font.DefaultFont;
import com.cavetale.core.font.GuiOverlay;
import com.cavetale.core.font.Unicode;
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
import org.bukkit.inventory.meta.BookMeta;
import static com.cavetale.fam.FamPlugin.plugin;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
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
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        Gui gui = new Gui(plugin()).size(27);
        GuiOverlay.Builder builder = GuiOverlay.BLANK.builder(27, RED)
            .title(text("Advent Calendar", GREEN));
        int openedUntil = 0;
        final int maxDay = Math.min(Advent.MAX_DAY, Advent.getThisDay());
        for (int i = 0; i < maxDay; i += 1) {
            SQLAdventPlayer row = rows.get(i);
            if (!(row.isOpened() && row.isSolved())) break;
            openedUntil = row.getDay(); // == i + 1
        }
        for (int i = 0; i < 25; i += 1) {
            final int day = i + 1;
            if (day > openedUntil + 1 || day > maxDay) {
                gui.setItem(i, lockedItem(day), click -> {
                        if (!click.isLeftClick()) return;
                        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 0.9f);
                    });
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
                            .async(r -> read(day));
                    });
            } else if (!row.isSolved()) {
                gui.setItem(i, unsolvedItem(day), click -> {
                        if (!click.isLeftClick()) return;
                        read(day);
                    });
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
                gui.setItem(i, rewardedItem(day), click -> {
                        if (!click.isLeftClick()) return;
                        read(day);
                    });
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
                          textOfChildren(xmasParkour(text("/warp XmasParkour", BLUE))));
        case 2 -> List.of(text("The second gift of"),
                          text("Christmas is hidden in the"),
                          textOfChildren(text("Flamingo Zone at "), spawn(text("/spawn", BLUE))));
        case 3 -> List.of(text("Find the third gift of"),
                          text("Christmas in the big"),
                          text("snow globe at "),
                          textOfChildren(xmasChallenge(text("/warp XmasChallenge", BLUE))));
        case 4 -> List.of(text("Win the fourth gift of"),
                          text("Christmas from a snowball"),
                          textOfChildren(text("fight at "), spawn(text("/spawn", BLUE))));
        case 5 -> List.of(text("Explore the Secret Cave" + Unicode.TRADEMARK.string),
                          textOfChildren(text("at "), xmasParkour(text("/warp XmasParkour", BLUE))));
        case 6 -> List.of(text("Dive under the ice at"),
                          textOfChildren(spawn(text("/spawn", BLUE))));
        case 7 -> List.of(text("Explore the wrecked pirate"),
                          textOfChildren(text("ship at "), xmasChallenge(text("/warp", BLUE))),
                          textOfChildren(xmasChallenge(text("XmasChallenge", BLUE))));
        case 8 -> List.of(text("Play Merry Christmas under"),
                          textOfChildren(text("the big tree at "), spawn(text("/spawn", BLUE))));
        case 9 -> List.of(text("Dive under the ice at"),
                          textOfChildren(xmasParkour(text("/warp XmasParkour", BLUE))));
        case 10 -> List.of(text("Find the tenth gift of"),
                           text("Christmas in the jail"),
                           textOfChildren(text("cell at "), spawn(text("/spawn", BLUE))));
        case 11 -> List.of(text("Explore past the clock"),
                           textOfChildren(text("tower at "), xmasChallenge(text("/warp", BLUE))),
                           textOfChildren(xmasChallenge(text("XmasChallenge", BLUE))));
        case 12 -> List.of(text("Scale the icy cliffs at"),
                           textOfChildren(northPole(text("/warp NorthPole", BLUE))));
        case 13 -> List.of(text("Get inside the giant"),
                           text("Christmas tree at"),
                           textOfChildren(northPole(text("/warp NorthPole", BLUE))));
        case 14 -> List.of(text("Find the Santa's Sleigh"),
                           text("hangar bay at"),
                           textOfChildren(northPole(text("/warp NorthPole", BLUE))));
        case 15 -> List.of(text("Look behind Santa's"),
                           text("big head at"),
                           textOfChildren(northPole(text("/warp NorthPole", BLUE))));
        case 16 -> List.of(text("Climb the giant"),
                           text("Christmas Tree at"),
                           textOfChildren(xmasParkour(text("/warp XmasParkour", BLUE))));
        case 17 -> List.of(text("Go through the giant snow"),
                           text("globe and follow the long"),
                           text("path leading North at"),
                           textOfChildren(xmasChallenge(text("/warp XmasChallenge", BLUE))));
        case 18 -> List.of(text("Venture through the"),
                           text("Polar Express at"),
                           textOfChildren(northPole(text("/warp NorthPole", BLUE))));
        case 19 -> List.of(text("Get to the top of the"),
                           textOfChildren(text("Wizard tower at "), spawn(text("/spawn", BLUE))));
        case 20 -> List.of(text("Follow the path that"),
                           text("leads away from the"),
                           text("wrecked pirate ship"),
                           textOfChildren(text("at "), xmasChallenge(text("/warp XmasChallenge", BLUE))));
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

    private static Component cmd(Component component, String cmd) {
        return component.hoverEvent(showText(text(cmd, BLUE)))
            .clickEvent(runCommand(cmd));
    }

    private static Component xmasParkour(Component component) {
        return cmd(component, "/warp XmasParkour");
    }

    private static Component xmasChallenge(Component component) {
        return cmd(component, "/warp XmasChallenge");
    }

    private static Component northPole(Component component) {
        return cmd(component, "/warp NorthPole");
    }

    private static Component spawn(Component component) {
        return cmd(component, "/spawn");
    }

    private void read(int day) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        book.editMeta(m -> {
                if (!(m instanceof BookMeta meta)) return;
                List<Component> lines = new ArrayList<>();
                lines.add(text("Door " + day, BLUE));
                lines.add(empty());
                lines.add(join(separator(space()), getPage(day)));
                lines.add(empty());
                lines.add(DefaultFont.BACK_BUTTON.component
                          .hoverEvent(showText(text("/advent", GREEN)))
                          .clickEvent(runCommand("/advent")));
                meta.author(text("Cavetale"));
                meta.title(text("Advent"));
                meta.pages(List.of(join(separator(newline()), lines)));
            });
        player.closeInventory();
        player.openBook(book);
    }
}
