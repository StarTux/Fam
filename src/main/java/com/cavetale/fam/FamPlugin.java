package com.cavetale.fam;

import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.core.font.GuiOverlay;
import com.cavetale.core.font.Unicode;
import com.cavetale.fam.sql.Database;
import com.cavetale.fam.sql.SQLBirthday;
import com.cavetale.fam.sql.SQLDaybreak;
import com.cavetale.fam.sql.SQLFriends;
import com.cavetale.fam.sql.SQLProgress;
import com.cavetale.fam.util.Colors;
import com.cavetale.fam.util.Gui;
import com.cavetale.fam.util.Items;
import com.cavetale.fam.util.Text;
import com.cavetale.mytems.Mytems;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.winthier.connect.Connect;
import com.winthier.playercache.PlayerCache;
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
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class FamPlugin extends JavaPlugin {
    @Getter protected static FamPlugin instance;
    private FamCommand famCommand = new FamCommand(this);
    private FriendsCommand friendsCommand = new FriendsCommand(this);
    private ValentineCommand valentineCommand = new ValentineCommand(this);
    private FriendCommand friendCommand = new FriendCommand(this);
    private LoveCommand loveCommand = new LoveCommand(this);
    private DivorceCommand divorceCommand = new DivorceCommand(this);
    private ProfileCommand profileCommand = new ProfileCommand(this);
    private PlayerListener eventListener = new PlayerListener(this);
    private MarriageListener marriageListener = new MarriageListener(this);
    private SQLDatabase database = new SQLDatabase(this);
    private List<Reward> rewardList;
    private boolean doDaybreak;

    @Override
    public void onEnable() {
        instance = this;
        famCommand.enable();
        friendsCommand.enable();
        valentineCommand.enable();
        friendCommand.enable();
        loveCommand.enable();
        divorceCommand.enable();
        profileCommand.enable();
        eventListener.enable();
        marriageListener.enable();
        new SidebarListener(this).enable();
        Database.init();
        Timer.enable();
        final String serverName = Connect.getInstance().getServerName();
        if (List.of("cavetale", "raid").contains(serverName)) {
            new GiftListener(this).enable();
            new WeddingRingListener(this).enable();
            getLogger().info("Survival features enabled: Gifts, Wedding Ring");
        }
        if (serverName.equals("cavetale")) {
            computePossibleDaybreak();
            doDaybreak = true;
            getLogger().info("Daybreak enabled");
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            Database.fillCacheAsync(player);
            Database.storePlayerProfileAsync(player).fetchPlayerSkinAsync();
        }
        Gui.enable(this);
    }

    public List<Reward> getRewardList() {
        if (rewardList == null) {
            rewardList = new ArrayList<>(10);
            // 1
            rewardList.add(new Reward().item(new ItemStack(Material.DIAMOND, 4), 5));
            // 2
            rewardList.add(new Reward()
                        .item(new ItemStack(Material.MELON_SLICE, 16), 5)
                        .item(new ItemStack(Material.APPLE, 16), 6));
            // 3
            rewardList.add(new Reward().item(new ItemStack(Material.COOKIE, 16), 5));
            // 4
            rewardList.add(new Reward().item(new ItemStack(Material.PUMPKIN_PIE, 16), 5));
            // 5
            rewardList.add(new Reward()
                        .item(new ItemStack(Material.NETHER_STAR))
                        .item(new ItemStack(Material.OBSIDIAN), 4));
            // 6
            rewardList.add(new Reward().item(new ItemStack(Material.CAKE), 11));
            // 7
            rewardList.add(new Reward()
                        .item(new ItemStack(Material.DIAMOND))
                        .item(Mytems.KITTY_COIN.createItemStack(), 4)
                        .item(new ItemStack(Material.MELON_SLICE, 16), 6));
            // 8
            rewardList.add(new Reward().item(new ItemStack(Material.GOLDEN_APPLE), 27));
            // 9
            rewardList.add(new Reward().item(new ItemStack(Material.NETHERITE_INGOT), 5));
            // 10
            rewardList.add(new Reward().item(Mytems.WEDDING_RING.createItemStack()));
            for (int i = 0; i < 4; i += 1) {
                rewardList.add(new Reward().item(new ItemStack(Material.DIAMOND), 3 * 9));
                rewardList.add(new Reward().item(new ItemStack(Material.EMERALD), 3 * 9));
            }
        }
        return rewardList;
    }

    @Override
    public void onDisable() {
        Gui.disable(this);
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
        final List<Component> lines = List.of(Component.text("Today's Friendship Gift", Colors.HOTPINK),
                                              Component.text("One Point per Player.", Colors.YELLOW),
                                              Component.text("New Item every Day.", Colors.YELLOW),
                                              Component.empty(),
                                              Component.text("Click to view only people", Colors.SILVER),
                                              Component.text("missing a gift from you.", Colors.SILVER));
        return Items.button(getTodaysGift(), withClick ? lines : lines.subList(0, 3));
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
            color = NamedTextColor.WHITE;
        } else {
            switch (relation) {
            case FRIEND: color = Colors.HOTPINK; break;
            case MARRIED: color = Colors.GOLD; break;
            case CHILD: case PARENT: color = Colors.BLUE; break;
            default: color = NamedTextColor.WHITE;
            }
        }
        List<Component> text = new ArrayList<>();
        text.add(Component.text(name, color));
        text.add(Text.toHeartString(row.getHearts()));
        if (perspective.hasPermission("fam.debug")) {
            text.add(Component.text("Debug Friendship: " + row.getFriendship(), Colors.DARK_GRAY));
        }
        if (relation != null) {
            text.add(Component.text(relation.humanName, Colors.HOTPINK));
        }
        if (row.dailyGiftGiven()) {
            text.add(Component.text("\u2611 Daily Gift", Colors.HOTPINK));
        } else {
            text.add(Component.text("\u2610 Daily Gift", Colors.DARK_GRAY));
        }
        if (birthday != null) {
            text.add(Component.text("Birthday ", NamedTextColor.DARK_GRAY)
                     .append(Component.text(birthday.getBirthdayName(), Colors.GOLD)));
        }
        Items.text(meta, text);
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
        Items.text(meta, List.of(Component.text(name, NamedTextColor.WHITE)));
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
                for (var online : Connect.getInstance().getOnlinePlayers()) {
                    if (uuid.equals(online.getUuid())) continue;
                    friendsMap.computeIfAbsent(online.getUuid(), uuid2 -> new SQLFriends(Database.sorted(uuid, uuid2)));
                    onlineSet.add(online.getUuid());
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
        Gui gui = new Gui(instance);
        int pageSize = 3 * 9;
        int pageCount = (friendsList.size() - 1) / pageSize + 1;
        int pageIndex = Math.max(0, Math.min(pageNumber - 1, pageCount - 1));
        int offset = pageIndex * pageSize;
        gui.size(pageSize + 9);
        gui.title(GuiOverlay.builder(pageSize + 9)
                  .layer(GuiOverlay.BLANK, type.menuColor)
                  .layer(GuiOverlay.TOP_BAR, TextColor.lerp(0.5f, type.menuColor, NamedTextColor.BLACK))
                  .title(pageCount > 1
                         ? Component.text(type.menuTitle + " " + pageNumber + "/" + pageCount, NamedTextColor.WHITE)
                         : Component.text(type.menuTitle, NamedTextColor.WHITE))
                  .build());
        for (int i = 0; i < pageSize; i += 1) {
            int friendsIndex = offset + i;
            if (friendsIndex >= friendsList.size()) break;
            SQLFriends row = friendsList.get(friendsIndex);
            ItemStack itemStack = makeSkull(player, row, birthdays.get(row.getOther(uuid)));
            gui.setItem(i + 9, itemStack, click -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
                    openFriendGui(player, row.getOther(uuid), pageNumber);
                });
        }
        if (pageIndex > 0) {
            int to = pageNumber - 1;
            gui.setItem(0, Items.button(Mytems.ARROW_LEFT, Component.text("Previous Page", NamedTextColor.GRAY)), c -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
                    openFriendsGui(player, friendsList, birthdays, type, to);
                });
        }
        if (pageIndex < pageCount - 1) {
            int to = pageNumber + 1;
            gui.setItem(8, Items.button(Mytems.ARROW_RIGHT, Component.text("Next Page", NamedTextColor.GRAY)), c -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
                    openFriendsGui(player, friendsList, birthdays, type, to);
                });
        }
        gui.setItem(4, makeTodaysGiftIcon(type != FriendsListView.ONLINE_NOT_GIFTED), c -> {
                if (!c.isLeftClick()) return;
                if (type == FriendsListView.ONLINE_NOT_GIFTED) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 0.5f);
                    return;
                }
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
                openOnlineNotGiftedGui(player, 1);
            });
        gui.setItem(3,
                    Items.button(Mytems.HEART.createItemStack(),
                                 List.of(Component.text("Friends", Colors.HOTPINK))),
                    click -> {
                        if (!click.isLeftClick()) return;
                        if (type == FriendsListView.FRIENDS) {
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 0.5f);
                            return;
                        }
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
                        openFriendsOnlyGui(player, 1);
                    });
        List<String> birthdayNameList = birthdays.values().stream()
            .filter(SQLBirthday::isToday)
            .map(SQLBirthday::getPlayer)
            .map(PlayerCache::nameForUuid)
            .collect(Collectors.toList());
        List<Component> birthdayTooltip = new ArrayList<>();
        Collections.sort(birthdayNameList);
        birthdayTooltip.add(Component.text("Birthdays " + Timer.getTodaysName(), Colors.GOLD));
        for (String name : birthdayNameList) {
            birthdayTooltip.add(Component.text(Unicode.BULLET_POINT.character + " ", NamedTextColor.GRAY)
                                .append(Component.text(name, NamedTextColor.WHITE)));
        }
        gui.setItem(5,
                    Items.button(Mytems.STAR.createItemStack(), birthdayTooltip),
                    click -> {
                        if (!click.isLeftClick()) return;
                        if (type == FriendsListView.BIRTHDAYS) {
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 0.5f);
                            return;
                        }
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
                        openBirthdaysGui(player, 1);
                    });
        if (type != FriendsListView.FRIENDSHIPS) {
            gui.setItem(Gui.OUTSIDE, null, click -> {
                    if (!click.isLeftClick()) return;
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
                    openFriendshipsGui(player, 1);
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
        UUID friendUuid = row.getOther(player.getUniqueId());
        PlayerProfile profile = Database.getCachedPlayerProfile(friendUuid);
        String name = PlayerCache.nameForUuid(friendUuid);
        if (name == null) name = profile.getName();
        final String finalName = name;
        Gui gui = new Gui(instance);
        int size = 3 * 9;
        TextColor color;
        switch (row.getHearts()) {
        case 0: color = NamedTextColor.DARK_GRAY; break;
        case 1: color = NamedTextColor.GRAY; break;
        case 2: color = NamedTextColor.WHITE; break;
        case 3: color = NamedTextColor.BLUE; break;
        case 4: color = NamedTextColor.RED; break;
        case 5: color = NamedTextColor.GOLD; break;
        default: color = NamedTextColor.WHITE;
        }
        gui.title(GuiOverlay.BLANK.make(size, color, Component.text(name, NamedTextColor.WHITE)));
        gui.size(size);
        gui.setItem(9 + 4, makeSkull(player, row, birthday));
        for (int i = 0; i < row.getHearts(); i += 1) {
            gui.setItem(2 + i, Mytems.HEART.createItemStack());
        }
        for (int i = row.getHearts(); i < 5; i += 1) {
            gui.setItem(2 + i, Mytems.EMPTY_HEART.createItemStack());
        }
        if (birthday != null) {
            gui.setItem(9 + 3, Items.button(Mytems.STAR, Component.text("Birthday: " + birthday.getBirthdayName(), Colors.HOTPINK)));
        }
        if (row.dailyGiftAvailable()) {
            gui.setItem(9 + 5, makeTodaysGiftIcon(true));
        }
        gui.setItem(18 + 4, Items.button(Material.ENDER_PEARL, Component.text("/tpa " + finalName, NamedTextColor.LIGHT_PURPLE)), click -> {
                Bukkit.dispatchCommand(player, "tpa " + finalName);
            });
        gui.setItem(Gui.OUTSIDE, null, click -> openFriendshipsGui(player, page));
        gui.open(player);
        return gui;
    }

    public static void openRewardsGui(Player player) {
        instance.database.scheduleAsyncTask(() -> {
                SQLProgress progress = Database.findProgress(player.getUniqueId());
                Bukkit.getScheduler().runTask(instance, () -> openRewardsGui(player, progress));
            });
    }

    public static Gui openRewardsGui(Player player, SQLProgress row) {
        if (!player.isValid()) return null;
        int claimed = row != null ? row.getClaimed() : 0;
        int score = row != null ? row.getScore() : 0;
        int available = row != null ? row.getAvailable() : 0;
        Gui gui = new Gui(instance);
        int size = instance.getRewardList().size();
        int guiSize = ((size - 1) / 9) * 9 + 18;
        gui.size(guiSize);
        gui.title(Component.text("Valentine Score " + score, Colors.HOTPINK));
        for (int i = 0; i < size; i += 1) {
            final int index = i;
            List<Component> text = new ArrayList<>();
            text.add(Component.text("Reward #" + (i + 1), Colors.HOTPINK));
            Material material;
            boolean canClaim;
            if (claimed > i) {
                text.add(Component.text("Claimed", Colors.HOTPINK));
                material = Material.PINK_SHULKER_BOX;
                canClaim = false;
            } else if (available > i) {
                text.add(Component.text("Available", Colors.HOTPINK));
                if (claimed != i) {
                    text.add(Component.text("Claim previous rewards first", NamedTextColor.YELLOW));
                    canClaim = false;
                } else {
                    canClaim = true;
                }
                material = Material.CHEST;
            } else {
                text.add(Component.text("Required: " + (i * 10 + 10), NamedTextColor.RED));
                material = Material.ENDER_CHEST;
                canClaim = false;
            }
            gui.setItem(i, Items.button(material, text), click -> {
                    if (!player.isValid()) return;
                    if (!click.isLeftClick()) return;
                    if (!canClaim) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 0.5f);
                        giveReward(player, index, false);
                        return;
                    }
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
                    if (Database.claimProgress(row)) {
                        giveReward(player, index, true);
                    }
                });
        }
        gui.setItem(guiSize - 5, makeTodaysGiftIcon(true));
        gui.open(player);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.MASTER, 0.5f, 1.2f);
        return gui;
    }

    private static int slotDist(int slot, int cx, int cy) {
        int x = slot % 9;
        int y = slot / 9;
        return Math.abs(x - cx) + Math.abs(y - cy);
    }

    /**
     * Must be called AFTER the progress was claimed in the database.
     */
    public static Gui giveReward(Player player, int index, boolean isForReal) {
        Reward reward = instance.getRewardList().get(index);
        Gui gui = new Gui(instance);
        gui.size(3 * 9);
        gui.title(isForReal
                  ? Component.text("Valentine Reward #" + (index + 1), Colors.HOTPINK)
                  : Component.text("Valentine Reward Preview #" + (index + 1), Colors.HOTPINK));
        if (reward.getItems().size() == 1) {
            gui.getInventory().setItem(9 + 4, reward.getItems().get(0).clone());
        } else {
            List<Integer> slots = new ArrayList<>(3 * 9);
            for (int i = 0; i < 3 * 9; i += 1) slots.add(i);
            Collections.sort(slots, (a, b) -> Integer.compare(slotDist(a, 4, 1), slotDist(b, 4, 1)));
            for (int i = 0; i < reward.getItems().size(); i += 1) {
                gui.getInventory().setItem(slots.get(i), reward.getItems().get(i).clone());
            }
        }
        gui.setEditable(isForReal);
        if (isForReal) {
            gui.onClose(event -> {
                    for (ItemStack item : gui.getInventory()) {
                        if (item == null || item.getAmount() == 0) continue;
                        for (ItemStack drop : player.getInventory().addItem(item).values()) {
                            player.getWorld().dropItem(player.getEyeLocation(), drop);
                        }
                    }
                    if (index == 9) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "titles unlockset " + player.getName() + " Cupid");
                    }
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.5f, 2.0f);
                    player.sendMessage(Component.text("Reward #" + (index + 1) + " claimed", Colors.HOTPINK));
                });
        } else {
            gui.setItem(Gui.OUTSIDE, null, evt -> openRewardsGui(player));
        }
        gui.open(player);
        return gui;
    }

    public static void showHighscore(Player player, int page) {
        instance.database.scheduleAsyncTask(() -> {
                List<SQLProgress> list = instance.database.find(SQLProgress.class)
                    .gt("score", 0)
                    .orderByDescending("score")
                    .findList();
                Bukkit.getScheduler().runTask(instance, () -> showHighscore(player, list, page));
            });
    }

    public static Gui showHighscore(Player player, List<SQLProgress> list, int pageNumber) {
        if (!player.isValid()) return null;
        if (list.isEmpty()) {
            player.sendMessage(Component.text("No highscores to show", NamedTextColor.RED));
            return null;
        }
        Gui gui = new Gui(instance);
        int pageSize = 3 * 9;
        int pageCount = (list.size() - 1) / pageSize + 1;
        int pageIndex = Math.max(0, Math.min(pageNumber - 1, pageCount - 1));
        int offset = pageIndex * pageSize;
        gui.size(pageSize + 9);
        gui.title(Component.text("Valentine Highscore " + pageNumber + "/" + pageCount, Colors.HOTPINK));
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
            text.add(Component.text(PlayerCache.nameForUuid(row.getPlayer())));
            text.add(Component.text("Rank #", Colors.BLUE).append(Component.text("" + rank, NamedTextColor.WHITE)));
            text.add(Component.text("Score ", Colors.BLUE).append(Component.text("" + score, NamedTextColor.WHITE)));
            Items.text(meta, text);
            itemStack.setItemMeta(meta);
            gui.setItem(menuIndex, itemStack);
        }
        if (pageIndex > 0) {
            int to = pageNumber - 1;
            gui.setItem(3 * 9, Items.button(Mytems.ARROW_LEFT, Component.text("Previous Page", NamedTextColor.GRAY)), c -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
                    showHighscore(player, list, to);
                });
        }
        if (pageIndex < pageCount - 1) {
            int to = pageNumber + 1;
            gui.setItem(3 * 9 + 8, Items.button(Mytems.ARROW_RIGHT, Component.text("Next Page", NamedTextColor.GRAY)), c -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
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
}
