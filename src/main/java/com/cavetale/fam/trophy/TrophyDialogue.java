package com.cavetale.fam.trophy;

import com.cavetale.core.font.GuiOverlay;
import com.cavetale.core.font.Unicode;
import com.cavetale.fam.ProfileDialogue;
import com.cavetale.fam.session.Session;
import com.cavetale.fam.sql.Database;
import com.cavetale.fam.util.Gui;
import com.cavetale.mytems.Mytems;
import com.winthier.playercache.PlayerCache;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.fam.sql.Database.db;
import static com.cavetale.fam.trophy.InnerCategoryOrder.innerCategoryOrder;
import static com.cavetale.mytems.util.Items.text;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextColor.color;
import static org.bukkit.Sound.*;
import static org.bukkit.SoundCategory.*;

public final class TrophyDialogue {
    private static final int GUI_SIZE = 4 * 9;
    protected final Trophies trophies;
    protected final PlayerCache target;
    List<String> categoryList = new ArrayList<>();
    protected final Map<String, List<SQLTrophy>> trophyMap = new HashMap<>();
    protected int pageIndex;
    protected boolean owner;

    public TrophyDialogue(final Trophies trophies, final PlayerCache target) {
        this.trophies = trophies;
        this.target = target;
    }

    public void open(Player player) {
        owner = player.getUniqueId().equals(target.uuid);
        db().scheduleAsyncTask(() -> {
                Set<String> categorySet = new HashSet<>();
                for (SQLTrophy trophy : Trophies.findTrophies(target.uuid)) {
                    categorySet.add(trophy.category);
                    trophyMap.computeIfAbsent(trophy.category, t -> new ArrayList<>()).add(trophy);
                }
                for (List<SQLTrophy> list : trophyMap.values()) {
                    Collections.sort(list, innerCategoryOrder());
                }
                categoryList.addAll(categorySet);
                Collections.sort(categoryList, (a, b) -> {
                        int sort = innerCategoryOrder().compare(trophyMap.get(a).get(0),
                                                                trophyMap.get(b).get(0));
                        return sort != 0
                            ? sort
                            : String.CASE_INSENSITIVE_ORDER.compare(a, b);
                    });
                Bukkit.getScheduler().runTask(trophies.plugin, () -> openOverview(player));
            });
    }

    private void openOverview(Player player) {
        Gui gui = new Gui(trophies.plugin).size(GUI_SIZE);
        final int pageSize = GUI_SIZE - 9;
        final int pageOffset = pageIndex * pageSize;
        final int pageCount = (categoryList.size() - 1) / pageSize + 1;
        GuiOverlay.Builder builder = GuiOverlay.BLANK
            .builder(GUI_SIZE, GRAY)
            .layer(GuiOverlay.TOP_BAR, DARK_GRAY)
            .title(join(noSeparators(),
                        (pageCount > 0
                         ? text((pageIndex + 1) + "/" + pageCount + " ", GRAY)
                         : empty()),
                        (player.getUniqueId().equals(target.uuid)
                         ? text("Trophy Case", GRAY)
                         : text((target.name.endsWith("s")
                                 ? target.name + "' Trophy Case"
                                 : target.name + "'s Trophy Case"), GRAY))));
        for (int i = 0; i < pageSize; i += 1) {
            int categoryIndex = pageOffset + i;
            int guiIndex = i + 9;
            if (categoryIndex >= categoryList.size()) break;
            String category = categoryList.get(categoryIndex);
            List<SQLTrophy> trophyList = trophyMap.get(category);
            if (trophyList.isEmpty()) continue;
            SQLTrophy trophy = trophyList.get(0);
            if (owner && !trophy.seen) builder.highlightSlot(guiIndex, trophy.getQualityColor());
            ItemStack icon = text(trophy.getIcon(), trophy.getTooltip());
            gui.setItem(guiIndex, icon, click -> {
                    if (click.isLeftClick()) {
                        openCategory(player, category, 0);
                        click(player);
                    }
                });
        }
        if (pageIndex > 0) {
            ItemStack icon = text(Mytems.ARROW_LEFT.createIcon(),
                                        List.of(text("Previous Page", GRAY)));
            gui.setItem(0, icon, click -> {
                        if (!click.isLeftClick()) return;
                        pageIndex -= 1;
                        openOverview(player);
                        page(player);
                });
        }
        if (pageIndex < pageCount - 1) {
            ItemStack icon = text(Mytems.ARROW_RIGHT.createIcon(),
                                  List.of(text("Next Page", GRAY)));
            gui.setItem(8, icon, click -> {
                        if (!click.isLeftClick()) return;
                        pageIndex += 1;
                        openOverview(player);
                        page(player);
                });
        }
        gui.setItem(Gui.OUTSIDE, null, click -> {
                if (!click.isLeftClick()) return;
                if (owner) {
                    Session session = Session.of(player);
                    if (session.isReady()) {
                        new ProfileDialogue(trophies.plugin, session).open(player);
                    }
                } else {
                    trophies.plugin.openFriendGui(player, target.uuid, 1);
                }
                click(player);
            });
        gui.title(builder.build());
        gui.open(player);
    }

    private void openCategory(final Player player, final String category, final int thePageIndex) {
        Gui gui = new Gui(trophies.plugin).size(GUI_SIZE);
        final int pageSize = GUI_SIZE - 9;
        final int pageOffset = thePageIndex * pageSize;
        List<SQLTrophy> trophyList = trophyMap.get(category);
        final int pageCount = (trophyList.size() - 1) / pageSize + 1;
        GuiOverlay.Builder builder = GuiOverlay.BLANK
            .builder(GUI_SIZE, color(0x777777))
            .layer(GuiOverlay.TOP_BAR, color(0x222222))
            .title(join(noSeparators(),
                        (pageCount > 0
                         ? text((thePageIndex + 1) + "/" + pageCount + " ", GRAY)
                         : empty()),
                        trophyList.get(0).getTitleComponent()));
        for (int i = 0; i < pageSize; i += 1) {
            int trophyIndex = pageOffset + i;
            int guiIndex = i + 9;
            if (trophyIndex >= trophyList.size()) break;
            SQLTrophy trophy = trophyList.get(trophyIndex);
            if (owner && !trophy.seen) builder.highlightSlot(guiIndex, trophy.getQualityColor());
            List<Component> tooltip = new ArrayList<>();
            tooltip.addAll(trophy.getTooltip());
            if (owner && !trophy.seen) {
                tooltip.add(Math.min(tooltip.size() - 1, 1),
                            join(noSeparators(),
                                 text(Unicode.tiny("click"), GREEN),
                                 text(" Mark as seen", GRAY)));
            }
            if (player.hasPermission("fam.admin")) {
                tooltip.add(text("#" + trophy.id, DARK_GRAY));
            }
            ItemStack icon = text(trophy.getIcon(), tooltip);
            Consumer<InventoryClickEvent> clickHandler = owner
                ? (click -> {
                        if (!click.isLeftClick()) return;
                        if (trophy.seen) {
                            fail(player);
                            return;
                        }
                        click(player);
                        trophy.setSeen(true);
                        Database.db().updateAsync(trophy, r -> {
                                trophies.listener.refreshUnseenTrophies(List.of(player.getUniqueId()));
                            }, "seen");
                        openCategory(player, category, 0);
                    })
                : null;
            gui.setItem(guiIndex, icon, clickHandler);
        }
        if (owner) {
            int notSeenCount = 0;
            for (SQLTrophy it : trophyList) {
                if (!it.seen) notSeenCount += 1;
            }
            if (notSeenCount > 0) {
                gui.setItem(5, text(Mytems.CHECKED_CHECKBOX.createIcon(), List.of(text("Mark all as seen", GRAY))), click -> {
                        if (!click.isLeftClick()) return;
                        for (SQLTrophy it : trophyList) {
                            if (it.seen) continue;
                            it.setSeen(true);
                            Database.db().updateAsync(it, null, "seen");
                        }
                        trophies.listener.refreshUnseenTrophies(List.of(target.uuid));
                        openCategory(player, category, thePageIndex);
                        click(player);
                    });
            }
        }
        if (thePageIndex > 0) {
            ItemStack icon = text(Mytems.ARROW_LEFT.createIcon(),
                                        List.of(text("Previous Page", GRAY)));
            gui.setItem(0, icon, click -> {
                        if (!click.isLeftClick()) return;
                        openCategory(player, category, thePageIndex - 1);
                        page(player);
                });
        }
        if (thePageIndex < pageCount - 1) {
            ItemStack icon = text(Mytems.ARROW_RIGHT.createIcon(),
                                  List.of(text("Next Page", GRAY)));
            gui.setItem(8, icon, click -> {
                        if (!click.isLeftClick()) return;
                        openCategory(player, category, thePageIndex + 1);
                        page(player);
                });
        }
        Consumer<InventoryClickEvent> back = click -> {
            if (!click.isLeftClick()) return;
            click(player);
            openOverview(player);
        };
        gui.setItem(Gui.OUTSIDE, null, back);
        gui.setItem(4, text(Mytems.TURN_LEFT.createIcon(), List.of(text("Go Back", GRAY))), back);
        gui.title(builder.build());
        gui.open(player);
    }

    private void click(Player player) {
        player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 1.0f);
    }

    private void fail(Player player) {
        player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 0.5f);
    }

    private void page(Player player) {
        player.playSound(player.getLocation(), ITEM_BOOK_PAGE_TURN, MASTER, 1.0f, 1.0f);
    }
}
