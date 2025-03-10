package com.cavetale.fam;

import com.cavetale.core.font.GlyphPolicy;
import com.cavetale.core.font.GuiOverlay;
import com.cavetale.core.menu.MenuItemEvent;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.core.text.LineWrap;
import com.cavetale.fam.session.Session;
import com.cavetale.fam.sql.Database;
import com.cavetale.fam.sql.SQLBirthday;
import com.cavetale.fam.sql.SQLFriends;
import com.cavetale.fam.trophy.TrophyDialogue;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Gui;
import com.destroystokyo.paper.profile.PlayerProfile;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.fam.util.Items.makeSkull;
import static com.cavetale.mytems.util.Items.tooltip;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.event.ClickEvent.suggestCommand;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextColor.color;
import static net.kyori.adventure.text.format.TextDecoration.*;
import static org.bukkit.Sound.*;
import static org.bukkit.SoundCategory.*;

/**
 * Show the player their profile page.
 */
@RequiredArgsConstructor
public final class ProfileDialogue {
    private final FamPlugin plugin;
    private final Session session;
    private UUID uuid;
    private SQLBirthday birthday;
    private List<SQLFriends> friends;
    private SQLFriends married;
    private SQLFriends bestFriend;
    private int friendsCount = 0;

    public static final TextColor TOOLTIP = color(0xFF69B4);
    public static final Locale LOCALE = Locale.US;

    public void open(Player player) {
        uuid = player.getUniqueId();
        plugin.getDatabase().scheduleAsyncTask(() -> {
                birthday = Database.findBirthday(uuid);
                friends = Database.findFriendsList(uuid);
                Collections.sort(friends);
                for (SQLFriends it : friends) {
                    Relation relation = it.getRelationEnum();
                    if (relation == null) continue;
                    switch (relation) {
                    case FRIEND:
                        friendsCount += 1;
                        if (bestFriend == null) bestFriend = it;
                        break;
                    case MARRIED:
                        married = it;
                        break;
                    default: break;
                    }
                }
                Bukkit.getScheduler().runTask(plugin, () -> openLoaded(player));
            });
    }

    private Gui openLoaded(Player player) {
        if (!player.isValid()) return null;
        Gui gui = new Gui(plugin)
            .size(6 * 9)
            .layer(GuiOverlay.BLANK, BLUE)
            .layer(GuiOverlay.TOP_BAR, DARK_BLUE)
            .title(text("Your Profile", BLUE));
        gui.setItem(4, makeSkull(player));
        int friendsIndex = 19;
        int marriedIndex = 20;
        int statusIndex = 23;
        int birthdayIndex = 24;
        int trophyIndex = 25;
        // Friends
        ItemStack friendsIcon = null;
        if (bestFriend != null) {
            PlayerProfile friendProfile = Database.getCachedPlayerProfile(bestFriend.getOther(uuid));
            if (friendProfile != null) {
                friendsIcon = makeSkull(friendProfile);
            }
        }
        if (friendsIcon == null) {
            friendsIcon = Mytems.QUESTION_MARK.createItemStack();
        }
        friendsIcon.setAmount(Math.max(1, Math.min(64, friendsCount)));
        friendsIcon.editMeta(meta -> {
                meta.displayName(text().content("You have " + friendsCount + " friends")
                                 .color(TOOLTIP)
                                 .decoration(ITALIC, false)
                                 .build());
                List<Component> lore = new ArrayList<>();
                if (bestFriend != null) {
                    String bestFriendName = PlayerCache.nameForUuid(bestFriend.getOther(uuid));
                    lore.add(text().content("Best Friend: " + bestFriendName)
                             .color(TOOLTIP)
                             .decoration(ITALIC, false)
                             .build());
                }
                meta.lore(lore);
            });
        gui.setItem(friendsIndex, friendsIcon, click -> {
                if (!click.isLeftClick()) return;
                plugin.openFriendshipsGui(player, 1);
                click(player);
            });
        // Married
        ItemStack marriedIcon = null;
        if (married != null) {
            PlayerProfile marriedProfile = Database.getCachedPlayerProfile(married.getOther(uuid));
            if (marriedProfile != null) {
                marriedIcon = makeSkull(marriedProfile);
            }
        }
        if (marriedIcon == null) {
            marriedIcon = Mytems.WEDDING_RING.createItemStack();
        }
        marriedIcon.editMeta(meta -> {
                meta.lore(Collections.emptyList());
                if (married != null) {
                    String marriedName = PlayerCache.nameForUuid(married.getOther(uuid));
                    meta.displayName(text().content("Married to " + marriedName)
                                     .color(TOOLTIP)
                                     .decoration(ITALIC, false)
                                     .build());
                } else {
                    meta.displayName(text().content("Not married!")
                                     .color(TOOLTIP)
                                     .decoration(ITALIC, false)
                                     .build());
                }
            });
        gui.setItem(marriedIndex, marriedIcon);
        // Status
        if (player.hasPermission("fam.setstatus")) {
            final String statusMessage = session.getPlayerRow().getStatusMessage();
            final List<Component> statusLore = new ArrayList<>();
            statusLore.add(text("Status Message", AQUA));
            if (statusMessage != null) {
                statusLore.addAll(new LineWrap()
                                  .emoji(player.hasPermission("chat.emoji"))
                                  .glyphPolicy(GlyphPolicy.PUBLIC)
                                  .tooltip(false)
                                  .componentMaker(str -> text(str, WHITE))
                                  .wrap(statusMessage));
                statusLore.add(empty());
            }
            statusLore.addAll(new LineWrap()
                              .componentMaker(str -> text(str, GRAY, ITALIC))
                              .wrap(":mouse_left: Set your status message"));
            final ItemStack statusItem = tooltip(new ItemStack(Material.WRITABLE_BOOK), statusLore);
            gui.setItem(statusIndex, statusItem, click -> {
                    if (!click.isLeftClick()) return;
                    click(player);
                    player.sendMessage(textOfChildren(Mytems.MOUSE_LEFT, text(" Click here to edit your status message", GREEN, BOLD))
                                       .hoverEvent(text("/setstatus", GRAY))
                                       .clickEvent(suggestCommand("/setstatus "))
                                       .insertion(statusMessage != null ? statusMessage : ""));
                });
        }
        // Birthday
        ItemStack birthdayIcon;
        if (birthday != null) {
            Month month = Month.of(birthday.getMonth());
            int day = birthday.getDay();
            String birthdayName = month.getDisplayName(TextStyle.FULL, LOCALE) + " " + day;
            birthdayIcon = Mytems.STAR.createItemStack();
            birthdayIcon.editMeta(meta -> {
                    meta.lore(Collections.emptyList());
                    meta.displayName(text().content("Your birthday: " + birthdayName)
                                     .color(TOOLTIP)
                                     .decoration(ITALIC, false)
                                     .build());
                });
        } else {
            birthdayIcon = Mytems.QUESTION_MARK.createItemStack();
            birthdayIcon.editMeta(meta -> {
                    meta.lore(Collections.emptyList());
                    meta.displayName(text().content("Set your birthday?")
                                     .color(TOOLTIP)
                                     .decoration(ITALIC, false)
                                     .build());
                });
        }
        gui.setItem(birthdayIndex, birthdayIcon, click -> {
                if (!click.isLeftClick()) return;
                click(player);
                player.closeInventory();
                new BirthdayDialogue(plugin).open(player);
            });
        gui.setItem(trophyIndex,
                    Mytems.GOLDEN_CUP.createIcon(List.of(text("Trophies", GOLD))),
                    click -> {
                        if (!click.isLeftClick()) return;
                        click(player);
                        new TrophyDialogue(plugin.trophies, new PlayerCache(player.getUniqueId(), player.getName())).open(player);
                    });
        gui.setItem(Gui.OUTSIDE, null, click -> {
                if (!click.isLeftClick()) return;
                MenuItemEvent.openMenu(player);
            });
        gui.open(player);
        return gui;
    }

    private void click(Player player) {
        player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 1.0f);
    }
}
