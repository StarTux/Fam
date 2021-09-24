package com.cavetale.fam;

import com.cavetale.core.font.DefaultFont;
import com.cavetale.fam.sql.Database;
import com.cavetale.fam.sql.SQLBirthday;
import com.cavetale.fam.sql.SQLFriends;
import com.cavetale.fam.util.Gui;
import com.cavetale.fam.util.Items;
import com.cavetale.mytems.Mytems;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.winthier.playercache.PlayerCache;
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
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Show the player their profile page.
 */
@RequiredArgsConstructor
public final class ProfileDialogue {
    private final FamPlugin plugin;
    private UUID uuid;
    private SQLBirthday birthday;
    private List<SQLFriends> friends;
    private SQLFriends married;
    private SQLFriends bestFriend;
    private List<SQLBirthday> birthdays;
    private int friendsCount = 0;

    public static final TextColor BG = TextColor.color(0xFF69B4);
    public static final TextColor COLOR = TextColor.color(0x366797);
    public static final TextColor TOOLTIP = TextColor.color(0xFF69B4);
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
                birthdays = Database.db().find(SQLBirthday.class)
                    .eq("month", Timer.getMonth())
                    .eq("day", Timer.getDay())
                    .findList();
                Bukkit.getScheduler().runTask(plugin, () -> openLoaded(player));
            });
    }

    private Gui openLoaded(Player player) {
        if (!player.isValid()) return null;
        int size = 27;
        Gui gui = new Gui(plugin)
            .size(size)
            .title(Component.text()
                   .append(DefaultFont.guiBlankOverlay(size, BG))
                   .append(Component.text("Your Profile", COLOR))
                   .build());
        gui.setItem(4, Items.makeSkull(player));
        int friendsIndex = 10;
        int marriedIndex = 11;
        int birthdayIndex = 15;
        // Friends
        ItemStack friendsIcon = null;
        if (bestFriend != null) {
            PlayerProfile friendProfile = Database.getCachedPlayerProfile(bestFriend.getOther(uuid));
            if (friendProfile != null) {
                friendsIcon = Items.makeSkull(friendProfile);
            }
        }
        if (friendsIcon == null) {
            friendsIcon = Mytems.QUESTION_MARK.createItemStack();
        }
        friendsIcon.setAmount(Math.max(1, Math.min(64, friendsCount)));
        friendsIcon.editMeta(meta -> {
                meta.displayName(Component.text().content("You have " + friendsCount + " friends")
                                 .color(TOOLTIP)
                                 .decoration(TextDecoration.ITALIC, false)
                                 .build());
                List<Component> lore = new ArrayList<>();
                if (bestFriend != null) {
                    String bestFriendName = PlayerCache.nameForUuid(bestFriend.getOther(uuid));
                    lore.add(Component.text().content("Best Friend: " + bestFriendName)
                             .color(TOOLTIP)
                             .decoration(TextDecoration.ITALIC, false)
                             .build());
                }
                meta.lore(lore);
            });
        gui.setItem(friendsIndex, friendsIcon, click -> {
                if (!click.isLeftClick()) return;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
                gui.close(player);
                plugin.openFriendsGui(player, friends, FriendsListView.FRIENDSHIPS, 1);
            });
        // Married
        ItemStack marriedIcon = null;
        if (married != null) {
            PlayerProfile marriedProfile = Database.getCachedPlayerProfile(married.getOther(uuid));
            if (marriedProfile != null) {
                marriedIcon = Items.makeSkull(marriedProfile);
            }
        }
        if (marriedIcon == null) {
            marriedIcon = Mytems.WEDDING_RING.createItemStack();
        }
        marriedIcon.editMeta(meta -> {
                meta.lore(Collections.emptyList());
                if (married != null) {
                    String marriedName = PlayerCache.nameForUuid(married.getOther(uuid));
                    meta.displayName(Component.text().content("Married to " + marriedName)
                                     .color(TOOLTIP)
                                     .decoration(TextDecoration.ITALIC, false)
                                     .build());
                } else {
                    meta.displayName(Component.text().content("Not married!")
                                     .color(TOOLTIP)
                                     .decoration(TextDecoration.ITALIC, false)
                                     .build());
                }
            });
        gui.setItem(marriedIndex, marriedIcon);
        // Birthday
        ItemStack birthdayIcon;
        if (birthday != null) {
            Month month = Month.of(birthday.getMonth());
            int day = birthday.getDay();
            String birthdayName = month.getDisplayName(TextStyle.FULL, LOCALE) + " " + day;
            birthdayIcon = Mytems.STAR.createItemStack();
            birthdayIcon.editMeta(meta -> {
                    meta.lore(Collections.emptyList());
                    meta.displayName(Component.text().content("Your birthday: " + birthdayName)
                                     .color(TOOLTIP)
                                     .decoration(TextDecoration.ITALIC, false)
                                     .build());
                });
        } else {
            birthdayIcon = Mytems.QUESTION_MARK.createItemStack();
            birthdayIcon.editMeta(meta -> {
                    meta.lore(Collections.emptyList());
                    meta.displayName(Component.text().content("Set your birthday?")
                                     .color(TOOLTIP)
                                     .decoration(TextDecoration.ITALIC, false)
                                     .build());
                });
        }
        gui.setItem(birthdayIndex, birthdayIcon, click -> {
                if (!click.isLeftClick()) return;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
                gui.close(player);
                new BirthdayDialogue(plugin).open(player);
            });
        // Birthdays
        for (int i = 0; i < birthdays.size() && i < 9; i += 1) {
            SQLBirthday theBirthday = birthdays.get(i);
            PlayerProfile birthdayProfile = Database.getCachedPlayerProfile(theBirthday.getPlayer());
            if (birthdayProfile == null) continue;
            ItemStack icon = Items.makeSkull(birthdayProfile);
            icon.editMeta(meta -> {
                    meta.displayName(Component.text().content("It's " + birthdayProfile.getName() + "'s birthday today!")
                                     .color(TOOLTIP)
                                     .decoration(TextDecoration.ITALIC, false)
                                     .build());
                });
            gui.setItem(18 + i, icon, click -> {
                    if (!click.isLeftClick()) return;
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
                    gui.close(player);
                    plugin.openFriendGui(player, theBirthday.getPlayer(), 1);
                });
        }
        gui.open(player);
        return gui;
    }
}
