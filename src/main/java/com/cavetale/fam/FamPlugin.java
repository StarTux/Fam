package com.cavetale.fam;

import com.cavetale.core.connect.Connect;
import com.cavetale.core.connect.NetworkServer;
import com.cavetale.core.event.item.PlayerReceiveItemsEvent;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.core.font.GuiOverlay;
import com.cavetale.core.font.Unicode;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.fam.advent.Advent;
import com.cavetale.fam.core.CorePlayerSkinProvider;
import com.cavetale.fam.elo.EloAdminCommand;
import com.cavetale.fam.elo.EloListener;
import com.cavetale.fam.eventhost.EventHosts;
import com.cavetale.fam.session.Session;
import com.cavetale.fam.session.Sessions;
import com.cavetale.fam.sql.Database;
import com.cavetale.fam.sql.SQLBirthday;
import com.cavetale.fam.sql.SQLDaybreak;
import com.cavetale.fam.sql.SQLFriends;
import com.cavetale.fam.sql.SQLProgress;
import com.cavetale.fam.trophy.Trophies;
import com.cavetale.fam.trophy.TrophyDialogue;
import com.cavetale.fam.util.Colors;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Gui;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.winthier.sql.SQLDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import static com.cavetale.core.font.Unicode.tiny;
import static com.cavetale.mytems.util.Items.tooltip;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static org.bukkit.Sound.*;
import static org.bukkit.SoundCategory.*;

@Getter
public final class FamPlugin extends JavaPlugin {
    @Getter protected static FamPlugin instance;
    private final FamCommand famCommand = new FamCommand(this);
    private final FriendsCommand friendsCommand = new FriendsCommand(this);
    private final ValentineCommand valentineCommand = new ValentineCommand(this);
    private final FriendCommand friendCommand = new FriendCommand(this);
    private final UnfriendCommand unfriendCommand = new UnfriendCommand(this);
    private final LoveCommand loveCommand = new LoveCommand(this);
    private final DivorceCommand divorceCommand = new DivorceCommand(this);
    private final ProfileCommand profileCommand = new ProfileCommand(this);
    private final SetStatusCommand setStatusCommand = new SetStatusCommand(this);
    private final PlayerListener playerListener = new PlayerListener(this);
    private final MarriageListener marriageListener = new MarriageListener(this);
    private final ConnectListener connectListener = new ConnectListener(this);
    protected final Trophies trophies = new Trophies(this);
    private final EventHosts eventHosts = new EventHosts();
    private final SQLDatabase database = new SQLDatabase(this);
    private List<Reward> rewardList;
    private boolean doDaybreak;
    private final FamFriendsSupplier famFriendsSupplier = new FamFriendsSupplier();
    private final Sessions sessions = new Sessions();
    private final Advent advent = null;
    private final CorePlayerSkinProvider skinProvider = new CorePlayerSkinProvider();

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        famCommand.enable();
        friendsCommand.enable();
        valentineCommand.enable();
        friendCommand.enable();
        unfriendCommand.enable();
        loveCommand.enable();
        divorceCommand.enable();
        profileCommand.enable();
        setStatusCommand.enable();
        playerListener.enable();
        connectListener.enable();
        trophies.enable();
        eventHosts.enable();
        Database.init();
        Timer.enable();
        if (advent != null) advent.enable();
        sessions.enable();
        NetworkServer networkServer = NetworkServer.current();
        if (networkServer.category.isSurvival()) {
            marriageListener.enable();
            new GiftListener(this).enable();
            new WeddingRingListener(this).enable();
            getLogger().info("Survival features enabled: Gifts, Wedding Ring");
            if (Timer.isValentineSeason()) {
                getLogger().info("Valentine sidebar enabled");
                new SidebarListener(this).enable();
            }
        }
        new MinigameListener(this).enable();
        if (networkServer == NetworkServer.HUB || networkServer == NetworkServer.BETA) {
            computePossibleDaybreak();
            doDaybreak = true;
            getLogger().info("Daybreak enabled");
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            Database.fillCacheAsync(player);
            Database.storePlayerProfileAsync(player).fetchPlayerSkinAsync();
        }
        famFriendsSupplier.register();
        skinProvider.register();
        new EloListener().enable();
        new EloAdminCommand(this).enable();
        new MenuListener().enable();
    }

    public List<Reward> getRewardList() {
        if (rewardList == null) {
            rewardList = new ArrayList<>(10);
            // 1
            rewardList.add(new Reward()
                           .item(Mytems.LOVE_LETTER.createItemStack())
                           .item(new ItemStack(Material.DIAMOND, 4), 4));
            // 2
            rewardList.add(new Reward()
                           .item(Mytems.LOVE_LETTER.createItemStack())
                           .item(new ItemStack(Material.MELON_SLICE, 16), 4)
                           .item(new ItemStack(Material.APPLE, 16), 6));
            // 3
            rewardList.add(new Reward()
                           .item(Mytems.LOVE_LETTER.createItemStack())
                           .item(new ItemStack(Material.COOKIE, 16), 4));
            // 4
            rewardList.add(new Reward()
                           .item(Mytems.LOVE_LETTER.createItemStack())
                           .item(new ItemStack(Material.PUMPKIN_PIE, 16), 4));
            // 5
            rewardList.add(new Reward()
                           .item(Mytems.LOVE_LETTER.createItemStack())
                           .item(Mytems.RUBY.createItemStack(), 4));
            // 6
            rewardList.add(new Reward()
                           .item(Mytems.LOVE_LETTER.createItemStack())
                           .item(new ItemStack(Material.CAKE), 10));
            // 7
            rewardList.add(new Reward()
                           .item(Mytems.LOVE_LETTER.createItemStack())
                           .item(Mytems.KITTY_COIN.createItemStack(), 4)
                           .item(new ItemStack(Material.MELON_SLICE, 16), 6));
            // 8
            rewardList.add(new Reward()
                           .item(Mytems.LOVE_LETTER.createItemStack())
                           .item(new ItemStack(Material.GOLDEN_APPLE), 26));
            // 9
            rewardList.add(new Reward()
                           .item(Mytems.LOVE_LETTER.createItemStack())
                           .item(new ItemStack(Material.NETHERITE_INGOT), 4));
            // 10
            rewardList.add(new Reward()
                           .item(Mytems.LOVE_LETTER.createItemStack())
                           .item(Mytems.WEDDING_RING.createItemStack()));
            for (int i = 0; i < 4; i += 1) {
                rewardList.add(new Reward()
                               .item(Mytems.LOVE_LETTER.createItemStack())
                               .item(new ItemStack(Material.DIAMOND), 4));
                rewardList.add(new Reward()
                               .item(Mytems.LOVE_LETTER.createItemStack())
                               .item(new ItemStack(Material.EMERALD), 4));
            }
        }
        return rewardList;
    }

    @Override
    public void onDisable() {
        sessions.disable();
        skinProvider.unregister();
        famFriendsSupplier.unregister();
        eventHosts.disable();
        if (advent != null) advent.disable();
        database.waitForAsyncTask();
        database.close();
    }

    public static final Material[] GIFTS = {
        Material.MELON_SLICE, // Monday
        Material.APPLE, // Tuesday
        Material.COOKIE, // Wednesday
        Material.PUMPKIN_PIE, // Thursday
        Material.SWEET_BERRIES, // Friday
        Material.CAKE, // Saturday
        Material.GOLDEN_APPLE // Sunday
    };

    public static Material getTodaysGift() {
        return GIFTS[Timer.getDayOfWeek()];
    }

    public static ItemStack makeTodaysGiftIcon(boolean withClick) {
        final List<Component> lines = List.of(text("Today's Friendship Gift", Colors.HOTPINK),
                                              text("One Point per Player.", Colors.YELLOW),
                                              text("New Item every Day.", Colors.YELLOW),
                                              Component.empty(),
                                              text("Click to view only people", Colors.SILVER),
                                              text("missing a gift from you.", Colors.SILVER));
        return tooltip(new ItemStack(getTodaysGift()), withClick ? lines : lines.subList(0, 3));
    }

    public static ItemStack makeSkull(Player perspective, SQLFriends row, SQLBirthday birthday) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        UUID friendUuid = row.getOther(perspective.getUniqueId());
        PlayerProfile profile = Database.getCachedPlayerProfile(friendUuid);
        meta.setPlayerProfile(profile);
        Relation relation = row.getRelationFor(perspective.getUniqueId());
        String name = row.getCachedName(perspective.getUniqueId());
        if (name == null) name = PlayerCache.nameForUuid(friendUuid);
        if (name == null) name = profile.getName();
        if (name == null) name = "?";
        TextColor color;
        if (relation == null) {
            color = WHITE;
        } else {
            switch (relation) {
            case FRIEND: color = Colors.HOTPINK; break;
            case MARRIED: color = Colors.GOLD; break;
            case CHILD: case PARENT: color = Colors.BLUE; break;
            default: color = WHITE;
            }
        }
        List<Component> text = new ArrayList<>();
        text.add(text(name, color));
        text.add(row.getHeartsComponent());
        if (perspective.hasPermission("fam.debug")) {
            text.add(text("Debug Friendship: " + row.getFriendship(), Colors.DARK_GRAY));
        }
        if (relation != null) {
            text.add(text(relation.humanName, Colors.HOTPINK));
        }
        if (row.dailyGiftGiven()) {
            text.add(text("\u2611 Daily Gift", Colors.HOTPINK));
        } else {
            text.add(text("\u2610 Daily Gift", Colors.DARK_GRAY));
        }
        if (row.dailyMinigamePlayed()) {
            text.add(text("\u2611 Daily Minigame", Colors.HOTPINK));
        } else {
            text.add(text("\u2610 Daily Minigame", Colors.DARK_GRAY));
        }
        if (birthday != null) {
            text.add(text("Birthday ", DARK_GRAY)
                     .append(text(birthday.getBirthdayName(), Colors.GOLD)));
        }
        tooltip(meta, text);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack makeSkull(UUID uuid) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        PlayerProfile profile = Database.getCachedPlayerProfile(uuid);
        meta.setPlayerProfile(profile);
        String name = PlayerCache.nameForUuid(uuid);
        if (name == null) name = profile.getName();
        tooltip(meta, List.of(text(name, WHITE)));
        item.setItemMeta(meta);
        return item;
    }

    public static void openFriendshipsGui(Player player, int page) {
        instance.database.scheduleAsyncTask(() -> {
                List<SQLFriends> friendsList = Database.findFriendsList(player.getUniqueId());
                friendsList.removeIf(SQLFriends::friendshipIsZero);
                Collections.sort(friendsList);
                Map<UUID, SQLBirthday> birthdays = Database.findBirthdayMap();
                Bukkit.getScheduler().runTask(instance, () -> {
                        if (!player.isOnline()) return;
                        openFriendsGui(player, friendsList, birthdays, FriendsListView.FRIENDSHIPS, page);
                    });
            });
    }

    public static void openOnlineNotGiftedGui(Player player, int page) {
        UUID uuid = player.getUniqueId();
        instance.database.scheduleAsyncTask(() -> {
                List<SQLFriends> friendsList = Database.findFriendsList(player.getUniqueId());
                Map<UUID, SQLFriends> friendsMap = new HashMap<>();
                for (SQLFriends row : friendsList) {
                    friendsMap.put(row.getOther(uuid), row);
                }
                Set<UUID> onlineSet = new HashSet<>();
                for (UUID online : Connect.get().getOnlinePlayers()) {
                    if (uuid.equals(online)) continue;
                    friendsMap.computeIfAbsent(online, uuid2 -> new SQLFriends(Database.sorted(uuid, uuid2)));
                    onlineSet.add(online);
                }
                friendsMap.keySet().retainAll(onlineSet);
                List<SQLFriends> newFriendsList = new ArrayList<>(friendsMap.values());
                newFriendsList.removeIf(SQLFriends::dailyGiftGiven);
                Collections.sort(newFriendsList);
                Map<UUID, SQLBirthday> birthdays = Database.findBirthdayMap();
                Bukkit.getScheduler().runTask(instance, () -> {
                        if (!player.isOnline()) return;
                        openFriendsGui(player, newFriendsList, birthdays, FriendsListView.ONLINE_NOT_GIFTED, 1);
                    });
            });
    }

    public static void openFriendsOnlyGui(Player player, int page) {
        instance.database.scheduleAsyncTask(() -> {
                List<SQLFriends> friendsList = Database.findFriendsList(player.getUniqueId());
                friendsList.removeIf(SQLFriends::noRelation);
                Collections.sort(friendsList);
                Map<UUID, SQLBirthday> birthdays = Database.findBirthdayMap();
                Bukkit.getScheduler().runTask(instance, () -> {
                        if (!player.isOnline()) return;
                        openFriendsGui(player, friendsList, birthdays, FriendsListView.FRIENDS, page);
                    });
            });
    }

    public static void openBirthdaysGui(Player player, int page) {
        final UUID uuid = player.getUniqueId();
        instance.database.scheduleAsyncTask(() -> {
                Map<UUID, SQLBirthday> birthdays = Database.findTodaysBirthdayMap();
                List<SQLFriends> friendsList;
                if (birthdays.isEmpty()) {
                    friendsList = List.of();
                } else {
                    friendsList = Database.findFriendsList(player.getUniqueId());
                    Map<UUID, SQLFriends> friendsMap = new HashMap<>();
                    for (SQLFriends row : friendsList) {
                        friendsMap.put(row.getOther(uuid), row);
                    }
                    for (UUID uuid2 : birthdays.keySet()) {
                        friendsMap.computeIfAbsent(uuid2, u -> new SQLFriends(Database.sorted(uuid, u)));
                    }
                    friendsMap.keySet().retainAll(birthdays.keySet());
                    List<SQLFriends> newFriendsList = new ArrayList<>(friendsMap.values());
                    Collections.sort(friendsList);
                    Bukkit.getScheduler().runTask(instance, () -> {
                            if (!player.isOnline()) return;
                            openFriendsGui(player, newFriendsList, birthdays, FriendsListView.BIRTHDAYS, page);
                        });
                }
            });
    }

    public static Gui openFriendsGui(Player player, List<SQLFriends> friendsList, Map<UUID, SQLBirthday> birthdays, FriendsListView type, int pageNumber) {
        if (!player.isValid()) return null;
        final UUID uuid = player.getUniqueId();
        int pageSize = 5 * 9;
        int pageCount = (friendsList.size() - 1) / pageSize + 1;
        int pageIndex = Math.max(0, Math.min(pageNumber - 1, pageCount - 1));
        int offset = pageIndex * pageSize;
        Gui gui = new Gui(instance)
            .size(pageSize + 9)
            .layer(GuiOverlay.BLANK, type.menuColor)
            .layer(GuiOverlay.TOP_BAR, TextColor.lerp(0.5f, type.menuColor, BLACK))
            .title(pageCount > 1
                   ? text(type.menuTitle + " " + pageNumber + "/" + pageCount, WHITE)
                   : text(type.menuTitle, WHITE));
        for (int i = 0; i < pageSize; i += 1) {
            int friendsIndex = offset + i;
            if (friendsIndex >= friendsList.size()) break;
            SQLFriends row = friendsList.get(friendsIndex);
            ItemStack itemStack = makeSkull(player, row, birthdays.get(row.getOther(uuid)));
            gui.setItem(i + 9, itemStack, click -> {
                    openFriendGui(player, row.getOther(uuid), pageNumber);
                    click(player);
                });
        }
        if (pageIndex > 0) {
            int to = pageNumber - 1;
            gui.setItem(0, Mytems.ARROW_LEFT.createIcon(List.of(text("Previous Page", GRAY))), c -> {
                    openFriendsGui(player, friendsList, birthdays, type, to);
                    click(player);
                });
        }
        if (pageIndex < pageCount - 1) {
            int to = pageNumber + 1;
            gui.setItem(8, Mytems.ARROW_RIGHT.createIcon(List.of(text("Next Page", GRAY))), c -> {
                    openFriendsGui(player, friendsList, birthdays, type, to);
                    click(player);
                });
        }
        gui.setItem(4, makeTodaysGiftIcon(type != FriendsListView.ONLINE_NOT_GIFTED), c -> {
                if (!c.isLeftClick()) return;
                if (type == FriendsListView.ONLINE_NOT_GIFTED) {
                    player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 0.5f);
                    return;
                }
                player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 1.0f);
                openOnlineNotGiftedGui(player, 1);
            });
        gui.setItem(3,
                    Mytems.HEART.createIcon(List.of(text("Friends", Colors.HOTPINK))),
                    click -> {
                        if (!click.isLeftClick()) return;
                        if (type == FriendsListView.FRIENDS) {
                            player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 0.5f);
                            return;
                        }
                        openFriendsOnlyGui(player, 1);
                        click(player);
                    });
        List<String> birthdayNameList = birthdays.values().stream()
            .filter(SQLBirthday::isToday)
            .map(SQLBirthday::getPlayer)
            .map(PlayerCache::nameForUuid)
            .collect(Collectors.toList());
        List<Component> birthdayTooltip = new ArrayList<>();
        Collections.sort(birthdayNameList);
        birthdayTooltip.add(text("Birthdays " + Timer.getTodaysName(), Colors.GOLD));
        for (String name : birthdayNameList) {
            birthdayTooltip.add(text(Unicode.BULLET_POINT.character + " ", GRAY)
                                .append(text(name, WHITE)));
        }
        gui.setItem(5,
                    Mytems.STAR.createIcon(birthdayTooltip),
                    click -> {
                        if (!click.isLeftClick()) return;
                        if (type == FriendsListView.BIRTHDAYS) {
                            player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 0.5f);
                            return;
                        }
                        openBirthdaysGui(player, 1);
                        click(player);
                    });
        if (type != FriendsListView.FRIENDSHIPS) {
            gui.setItem(Gui.OUTSIDE, null, click -> {
                    if (!click.isLeftClick()) return;
                    openFriendshipsGui(player, 1);
                    click(player);
                });
        } else {
            gui.setItem(Gui.OUTSIDE, null, click -> {
                    Session session = Session.of(player);
                    if (session.isReady()) {
                        new ProfileDialogue(instance, session).open(player);
                    }
                    click(player);
                });
        }
        gui.open(player);
        PluginPlayerEvent.Name.VIEW_FRIENDS_LIST.call(instance, player);
        return gui;
    }

    public static void openFriendGui(Player player, UUID friendUuid, int page) {
        instance.database.scheduleAsyncTask(() -> {
                SQLFriends row = Database.findFriends(player.getUniqueId(), friendUuid);
                final SQLFriends row2 = row != null ? row : new SQLFriends(Database.sorted(player.getUniqueId(), friendUuid));
                final SQLBirthday birthday = Database.findBirthday(friendUuid);
                Bukkit.getScheduler().runTask(instance, () -> openFriendGui(player, row2, birthday, page));
            });
    }

    public static Gui openFriendGui(Player player, SQLFriends row, SQLBirthday birthday, int page) {
        final UUID friendUuid = row.getOther(player.getUniqueId());
        final PlayerProfile profile = Database.getCachedPlayerProfile(friendUuid);
        final PlayerCache playerCache = PlayerCache.forUuid(friendUuid);
        final TextColor color;
        final int friendship = row.getFriendship();
        if (friendship < 20) {
            color = DARK_GRAY;
        } else if (friendship < 40) {
            color = GRAY;
        } else if (friendship < 60) {
            color = WHITE;
        } else if (friendship < 80) {
            color = BLUE;
        } else if (friendship < 100) {
            color = RED;
        } else {
            color = GOLD;
        }
        final Gui gui = new Gui(instance)
            .size(6 * 9)
            .layer(GuiOverlay.BLANK, DARK_GRAY)
            .layer(GuiOverlay.TOP_BAR, color)
            .title(text(playerCache.name, WHITE));
        final var hearts = row.getHeartIcons();
        for (int i = 0; i < 5; i += 1) {
            final ItemStack heart = hearts.get(i).createItemStack();
            heart.editMeta(meta -> meta.setHideTooltip(true));
            gui.setItem(2 + i, heart);
        }
        gui.setItem(18 + 2, Mytems.TURN_RIGHT.createIcon(List.of(text("/tpa " + playerCache.name, LIGHT_PURPLE))), click -> {
                Bukkit.dispatchCommand(player, "tpa " + playerCache.name);
            });
        if (birthday != null) {
            gui.setItem(18 + 3, Mytems.STAR.createIcon(List.of(text("Birthday: " + birthday.getBirthdayName(), Colors.HOTPINK))));
        }
        gui.setItem(18 + 4, makeSkull(player, row, birthday));
        if (row.dailyGiftAvailable()) {
            gui.setItem(18 + 5, makeTodaysGiftIcon(true));
        }
        gui.setItem(18 + 6, Mytems.GOLDEN_CUP.createIcon(List.of(text("Trophies", GOLD))), click -> {
                new TrophyDialogue(instance.trophies, playerCache).open(player);
                click(player);
            });
        gui.setItem(Gui.OUTSIDE, null, click -> {
                openFriendshipsGui(player, page);
                click(player);
            });
        gui.open(player);
        return gui;
    }

    public void openRewardsGui(Player player) {
        Database.findProgress(player.getUniqueId(), row -> openRewardsGui(player, row));
    }

    public Gui openRewardsGui(Player player, SQLProgress row) {
        if (!player.isValid()) return null;
        int claimed = row != null ? row.getClaimed() : 0;
        int score = row != null ? row.getScore() : 0;
        int available = row != null ? row.getAvailable() : 0;
        final int size = instance.getRewardList().size();
        final int guiSize = ((size - 1) / 9) * 9 + 18;
        Gui gui = new Gui(instance)
            .size(guiSize)
            .layer(GuiOverlay.BLANK, Colors.HOTPINK)
            .title(text("Valentine Score " + score, WHITE));
        for (int i = 0; i < size; i += 1) {
            final int index = i;
            List<Component> text = new ArrayList<>();
            text.add(textOfChildren(text("Reward ", GRAY), Mytems.HEART, text("" + (i + 1), Colors.HOTPINK)));
            ItemStack icon;
            boolean canClaim;
            if (claimed > i) {
                text.add(textOfChildren(Mytems.CHECKED_CHECKBOX, text(" Claimed", Colors.HOTPINK)));
                icon = Mytems.CHECKED_CHECKBOX.createIcon();
                canClaim = false;
            } else if (available > i) {
                text.add(text("Available", Colors.HOTPINK));
                if (claimed != i) {
                    text.add(text("Claim your previous", YELLOW));
                    text.add(text("rewards first.", YELLOW));
                    text.add(empty());
                    text.add(textOfChildren(Mytems.MOUSE_LEFT, text(" Open preview", GRAY)));
                    canClaim = false;
                } else {
                    text.add(empty());
                    text.add(textOfChildren(Mytems.MOUSE_LEFT, text(" Claim this reward", GRAY)));
                    canClaim = true;
                }
                icon = Mytems.CHECKBOX.createIcon();
            } else {
                text.add(textOfChildren(text(tiny("required score "), GRAY), text(((i + 1) * 7), RED)));
                text.add(empty());
                text.add(textOfChildren(Mytems.MOUSE_LEFT, text(" Open preview", GRAY)));
                canClaim = false;
                icon = Mytems.NO.createIcon();
            }
            gui.setItem(i, tooltip(icon, text), click -> {
                    if (!player.isValid()) return;
                    if (!click.isLeftClick()) return;
                    if (!NetworkServer.current().isSurvival()) {
                        player.sendMessage(text("You can only open rewards in survival mode", RED));
                        return;
                    }
                    if (!canClaim) {
                        player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 0.5f);
                        giveReward(player, index, false);
                        return;
                    }
                    player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 1.0f);
                    Database.claimProgress(row, success -> {
                            if (success) {
                                giveReward(player, index, true);
                            }
                        });
                });
        }
        gui.setItem(guiSize - 5, makeTodaysGiftIcon(true));
        gui.open(player);
        player.playSound(player.getLocation(), BLOCK_CHEST_OPEN, MASTER, 0.5f, 1.2f);
        return gui;
    }

    private static int slotDist(int slot, int cx, int cy) {
        int x = slot % 9;
        int y = slot / 9;
        return Math.abs(x - cx) + Math.abs(y - cy);
    }

    /**
     * If isForReal=true, this must be called AFTER the progress was
     * claimed in the database.
     */
    public Gui giveReward(Player player, int index, boolean isForReal) {
        final int size = 3 * 9;
        Reward reward = instance.getRewardList().get(index);
        Gui gui = new Gui(instance)
            .size(size)
            .layer(GuiOverlay.BLANK, (isForReal ? WHITE : Colors.HOTPINK))
            .title(isForReal
                   ? textOfChildren(text("Valentine Reward ", WHITE), Mytems.HEART, text((index + 1), WHITE))
                   : textOfChildren(text("Valentine Reward Preview ", WHITE), Mytems.HEART, text((index + 1), WHITE)));
        if (isForReal) gui.layer(GuiOverlay.HOLES, Colors.HOTPINK);
        if (reward.getItems().size() == 1) {
            gui.getInventory().setItem(9 + 4, reward.getItems().get(0).clone());
        } else {
            List<Integer> slots = new ArrayList<>(size);
            for (int i = 0; i < size; i += 1) slots.add(i);
            Collections.sort(slots, (a, b) -> Integer.compare(slotDist(a, 4, 1), slotDist(b, 4, 1)));
            for (int i = 0; i < reward.getItems().size(); i += 1) {
                gui.getInventory().setItem(slots.get(i), reward.getItems().get(i).clone());
            }
        }
        gui.setEditable(isForReal);
        if (isForReal) {
            gui.onClose(event -> {
                    PlayerReceiveItemsEvent itemsEvent = new PlayerReceiveItemsEvent(player, gui.getInventory());
                    itemsEvent.giveItems();
                    itemsEvent.callEvent();
                    itemsEvent.dropItems();
                    if (index == 9) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "titles unlockset " + player.getName() + " Cupid");
                    }
                    player.playSound(player.getLocation(), ENTITY_PLAYER_LEVELUP, MASTER, 0.5f, 2.0f);
                    player.sendMessage(text("Reward #" + (index + 1) + " claimed", Colors.HOTPINK));
                    getLogger().info(player.getName() + " claimed Valentine reward " + (index + 1));
                });
        }
        gui.setItem(Gui.OUTSIDE, null, evt -> {
                openRewardsGui(player);
                click(player);
            });
        gui.open(player);
        return gui;
    }

    public static void showHighscore(Player player, int page) {
        instance.database.scheduleAsyncTask(() -> {
                List<SQLProgress> list = instance.database.find(SQLProgress.class)
                    .eq("year", Timer.getYear())
                    .gt("score", 0)
                    .orderByDescending("score")
                    .findList();
                Bukkit.getScheduler().runTask(instance, () -> showHighscore(player, list, page));
            });
    }

    public static Gui showHighscore(Player player, List<SQLProgress> list, int pageNumber) {
        if (!player.isValid()) return null;
        if (list.isEmpty()) {
            player.sendMessage(text("No highscores to show", RED));
            return null;
        }
        final int pageSize = 3 * 9;
        final int pageCount = (list.size() - 1) / pageSize + 1;
        final int pageIndex = Math.max(0, Math.min(pageNumber - 1, pageCount - 1));
        final int offset = pageIndex * pageSize;
        final int size = pageSize + 9;
        final Gui gui = new Gui(instance)
            .size(size)
            .layer(GuiOverlay.BLANK, Colors.HOTPINK)
            .title(text("Valentine Highscore " + pageNumber + "/" + pageCount, WHITE));
        int score = -1;
        int rank = 0;
        for (int i = 0; i < list.size(); i += 1) {
            int listIndex = i;
            int menuIndex = i - offset;
            if (listIndex >= list.size()) break;
            if (menuIndex >= pageSize) break;
            SQLProgress row = list.get(listIndex);
            if (row.getScore() != score) {
                score = row.getScore();
                rank += 1;
            }
            if (menuIndex < 0) continue;
            ItemStack itemStack = makeSkull(row.getPlayer());
            ItemMeta meta = itemStack.getItemMeta();
            List<Component> text = new ArrayList<>();
            text.add(text(PlayerCache.nameForUuid(row.getPlayer())));
            text.add(text("Rank #", Colors.BLUE).append(text("" + rank, WHITE)));
            text.add(text("Score ", Colors.BLUE).append(text("" + score, WHITE)));
            tooltip(meta, text);
            itemStack.setItemMeta(meta);
            gui.setItem(menuIndex, itemStack);
        }
        if (pageIndex > 0) {
            int to = pageNumber - 1;
            gui.setItem(3 * 9, Mytems.ARROW_LEFT.createIcon(List.of(text("Previous Page", GRAY))), c -> {
                    player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 1.0f);
                    showHighscore(player, list, to);
                });
        }
        if (pageIndex < pageCount - 1) {
            int to = pageNumber + 1;
            gui.setItem(3 * 9 + 8, Mytems.ARROW_RIGHT.createIcon(List.of(text("Next Page", GRAY))), c -> {
                    player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 1.0f);
                    showHighscore(player, list, to);
                });
        }
        gui.open(player);
        return gui;
    }

    /**
     * This is called once when the plugin is first enabled, and once
     * more on every daybreak, by Timer.
     */
    public void computePossibleDaybreak() {
        if (!doDaybreak) return;
        getLogger().info("Computing possible daybreak");
        database.scheduleAsyncTask(() -> {
                SQLDaybreak row = database.find(SQLDaybreak.class).findUnique();
                if (row == null) {
                    row = new SQLDaybreak();
                    database.insert(row);
                }
                int dayId = Timer.getDayId();
                if (row.getDayId() == dayId) return; // no daybreak!
                row.setDayId(dayId);
                database.update(row);
                onDaybreak();
            });
    }

    /**
     * Called from an async thread, except for testing!
     */
    public void onDaybreak() {
        getLogger().info("Daybreak!");
        // Friendship decay for non-friends, down to 0.
        int nones = database.update(SQLFriends.class)
            .subtract("friendship", 1)
            .where(c -> c.isNull("relation").gt("friendship", 0))
            .sync();
        // Friendship decay for friends, down to 40.
        int friends = database.update(SQLFriends.class)
            .subtract("friendship", 1)
            .where(c -> c.eq("relation", "friend").gt("friendship", 40))
            .sync();
        // Delete empty rows
        int deleted = database.find(SQLFriends.class)
            .isNull("relation")
            .lte("friendship", 0)
            .delete();
        getLogger().info("Friendship decay:"
                         + " nones=" + nones
                         + " friends=" + friends
                         + " deleted=" + deleted);
    }

    private static void click(Player player) {
        player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 1.0f);
    }

    public static FamPlugin plugin() {
        return instance;
    }

    public static FamPlugin famPlugin() {
        return instance;
    }
}
