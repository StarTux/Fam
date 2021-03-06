package com.cavetale.fam;

import com.cavetale.core.font.DefaultFont;
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
import com.winthier.playercache.PlayerCache;
import com.winthier.sql.SQLDatabase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
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
    @Getter private static FamPlugin instance;
    private FamCommand famCommand = new FamCommand(this);
    private FriendsCommand friendsCommand = new FriendsCommand(this);
    private ValentineCommand valentineCommand = new ValentineCommand(this);
    private FriendCommand friendCommand = new FriendCommand(this);
    private LoveCommand loveCommand = new LoveCommand(this);
    private DivorceCommand divorceCommand = new DivorceCommand(this);
    private ProfileCommand profileCommand = new ProfileCommand(this);
    private PlayerListener eventListener = new PlayerListener(this);
    private WeddingRingListener weddingRingListener = new WeddingRingListener(this);
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
        weddingRingListener.enable();
        marriageListener.enable();
        new SidebarListener(this).enable();
        Database.init();
        Timer.enable();
        doDaybreak = getConfig().getBoolean("DoDaybreak");
        if (doDaybreak) {
            getLogger().info("Daybreak computation enabled!");
            computePossibleDaybreak();
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

    public static ItemStack makeTodaysGiftIcon() {
        return Items.button(getTodaysGift(),
                            Arrays.asList(Text.builder("Today's Friendship Gift").color(Colors.PINK).italic(false).create(),
                                          Text.builder("One Point per Player.").color(Colors.YELLOW).italic(false).create(),
                                          Text.builder("New Item every Day.").color(Colors.YELLOW).italic(false).create(),
                                          Text.builder("").create(),
                                          Text.builder("Click to view only people").color(Colors.SILVER).italic(false).create(),
                                          Text.builder("missing a gift from you.").color(Colors.SILVER).italic(false).create()));
    }

    public static ItemStack makeSkull(Player player, SQLFriends row) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        UUID friendUuid = row.getOther(player.getUniqueId());
        PlayerProfile profile = Database.getCachedPlayerProfile(friendUuid);
        meta.setPlayerProfile(profile);
        Relation relation = row.getRelationFor(player.getUniqueId());
        String name = row.getCachedName();
        if (name == null) name = PlayerCache.nameForUuid(friendUuid);
        if (name == null) name = profile.getName();
        ChatColor color;
        if (relation == null) {
            color = Colors.WHITE;
        } else {
            switch (relation) {
            case FRIEND: color = Colors.PINK; break;
            case MARRIED: color = Colors.ORANGE; break;
            case CHILD: case PARENT: color = Colors.BLUE; break;
            default: color = Colors.WHITE;
            }
        }
        meta.setDisplayNameComponent(Text.builder(name).color(color).italic(false).create());
        List<BaseComponent[]> lore = new ArrayList<>();
        lore.add(Text.toHeartString(row.getHearts()));
        if (player.hasPermission("fam.debug")) {
            lore.add(Text.builder("Debug Friendship: " + row.getFriendship()).color(Colors.DARK_GRAY).create());
        }
        if (relation != null) {
            lore.add(Text.builder(relation.humanName).color(Colors.PINK).italic(false).create());
        }
        if (row.dailyGiftGiven()) {
            lore.add(Text.builder("\u2611 Daily Gift").color(Colors.PINK).italic(false).create());
        } else {
            lore.add(Text.builder("\u2610 Daily Gift").color(Colors.DARK_GRAY).italic(false).create());
        }
        meta.setLoreComponents(lore);
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
        meta.setDisplayNameComponent(Text.builder(name).color(Colors.WHITE).italic(false).create());
        item.setItemMeta(meta);
        return item;
    }

    public static void openFriendsGui(Player player, int page) {
        instance.database.scheduleAsyncTask(() -> {
                List<SQLFriends> friendsList = Database.findFriendsList(player.getUniqueId());
                for (SQLFriends row : friendsList) {
                    UUID uuid = row.getOther(player.getUniqueId());
                    String name = PlayerCache.nameForUuid(uuid);
                    row.setCachedName(name != null ? name : "");
                }
                friendsList.removeIf(SQLFriends::friendshipIsZero);
                Collections.sort(friendsList);
                Bukkit.getScheduler().runTask(instance, () -> openFriendsGui(player, friendsList, page));
            });
    }

    public static Gui openFriendsGui(Player player, List<SQLFriends> friendsList, int pageNumber) {
        if (!player.isValid()) return null;
        if (friendsList.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No friends to show");
            return null;
        }
        Gui gui = new Gui(instance);
        int pageSize = 3 * 9;
        int pageCount = (friendsList.size() - 1) / pageSize + 1;
        int pageIndex = Math.max(0, Math.min(pageNumber - 1, pageCount - 1));
        int offset = pageIndex * pageSize;
        gui.size(pageSize + 9);
        Component title = Component.text()
            .append(DefaultFont.guiBlankOverlay(pageSize + 9, TextColor.color(0x8080FF)))
            .append(Component.text(pageCount > 1
                                   ? ChatColor.RED + "Friends " + pageNumber + "/" + pageCount
                                   : ChatColor.RED + "Friends"))
            .build();
        gui.title(title);
        for (int i = 0; i < pageSize; i += 1) {
            int friendsIndex = offset + i;
            if (friendsIndex >= friendsList.size()) break;
            SQLFriends row = friendsList.get(friendsIndex);
            ItemStack itemStack = makeSkull(player, row);
            gui.setItem(i, itemStack, click -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
                    openFriendGui(player, row.getOther(player.getUniqueId()), pageNumber);
                });
        }
        if (pageIndex > 0) {
            int to = pageNumber - 1;
            gui.setItem(3 * 9, Items.button(Mytems.ARROW_LEFT, ChatColor.GRAY + "Previous Page"), c -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
                    openFriendsGui(player, friendsList, to);
                });
        }
        if (pageIndex < pageCount - 1) {
            int to = pageNumber + 1;
            gui.setItem(3 * 9 + 8, Items.button(Mytems.ARROW_RIGHT, ChatColor.GRAY + "Next Page"), c -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
                    openFriendsGui(player, friendsList, to);
                });
        }
        gui.setItem(3 * 9 + 4, makeTodaysGiftIcon(), c -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
                List<SQLFriends> list = new ArrayList<>(friendsList);
                list.removeIf(SQLFriends::dailyGiftGiven);
                list.removeIf(r -> Bukkit.getPlayer(r.getOther(player.getUniqueId())) == null);
                openFriendsGui(player, list, 1);
            });
        gui.open(player);
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
        Component title = Component.text()
            .append(DefaultFont.guiBlankOverlay(size, color))
            .append(Component.text(name, NamedTextColor.WHITE))
            .build();
        gui.title(title);
        gui.size(size);
        gui.setItem(9 + 4, makeSkull(player, row));
        for (int i = 0; i < row.getHearts(); i += 1) {
            gui.setItem(2 + i, Mytems.HEART.createItemStack());
        }
        for (int i = row.getHearts(); i < 5; i += 1) {
            gui.setItem(2 + i, Mytems.EMPTY_HEART.createItemStack());
        }
        if (birthday != null) {
            gui.setItem(9 + 3, Items.button(Mytems.STAR, Component.text("Birthday: " + birthday.getBirthdayName())
                                            .color(Colors.HOTPINK).decoration(TextDecoration.ITALIC, false)));
        }
        if (row.dailyGiftAvailable()) {
            gui.setItem(9 + 5, makeTodaysGiftIcon());
        }
        gui.setItem(18 + 4, Items.button(Material.ENDER_PEARL, ChatColor.LIGHT_PURPLE + "/tpa " + finalName), click -> {
                Bukkit.dispatchCommand(player, "tpa " + finalName);
            });
        gui.setItem(Gui.OUTSIDE, null, click -> openFriendsGui(player, page));
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
        gui.title(ChatColor.RED + "Valentine Score " + score);
        for (int i = 0; i < size; i += 1) {
            final int index = i;
            List<BaseComponent[]> text = new ArrayList<>();
            text.add(Text.builder("Reward #" + (i + 1)).color(Colors.PINK).italic(false).create());
            Material material;
            boolean canClaim;
            if (claimed > i) {
                text.add(Text.builder("Claimed").color(Colors.PINK).italic(false).create());
                material = Material.PINK_SHULKER_BOX;
                canClaim = false;
            } else if (available > i) {
                text.add(Text.builder("Available").color(Colors.PINK).italic(false).create());
                if (claimed != i) {
                    text.add(Text.builder("Claim previous rewards first").color(ChatColor.YELLOW).italic(false).create());
                    canClaim = false;
                } else {
                    canClaim = true;
                }
                material = Material.CHEST;
            } else {
                text.add(Text.builder("Required: " + (i * 10 + 10)).color(ChatColor.RED).italic(false).create());
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
        gui.setItem(guiSize - 5, makeTodaysGiftIcon());
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
                  ? ChatColor.RED + "Valentine Reward #" + (index + 1)
                  : ChatColor.RED + "Valentine Reward Preview #" + (index + 1));
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
                    player.sendMessage(Text.builder("Reward #" + (index + 1) + " claimed").color(Colors.PINK).create());
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
            player.sendMessage(ChatColor.RED + "No highscores to show");
            return null;
        }
        Gui gui = new Gui(instance);
        int pageSize = 3 * 9;
        int pageCount = (list.size() - 1) / pageSize + 1;
        int pageIndex = Math.max(0, Math.min(pageNumber - 1, pageCount - 1));
        int offset = pageIndex * pageSize;
        gui.size(pageSize + 9);
        gui.title(ChatColor.RED + "Valentine Highscore " + pageNumber + "/" + pageCount);
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
            List<BaseComponent[]> lore = new ArrayList<>();
            lore.add(Text.builder("Rank #").color(Colors.BLUE).append("" + rank).color(Colors.WHITE).create());
            lore.add(Text.builder("Score ").color(Colors.BLUE).append("" + score).color(Colors.WHITE).create());
            meta.setLoreComponents(lore);
            itemStack.setItemMeta(meta);
            gui.setItem(menuIndex, itemStack);
        }
        if (pageIndex > 0) {
            int to = pageNumber - 1;
            gui.setItem(3 * 9, Items.button(Mytems.ARROW_LEFT, ChatColor.GRAY + "Previous Page"), c -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.0f);
                    showHighscore(player, list, to);
                });
        }
        if (pageIndex < pageCount - 1) {
            int to = pageNumber + 1;
            gui.setItem(3 * 9 + 8, Items.button(Mytems.ARROW_RIGHT, ChatColor.GRAY + "Next Page"), c -> {
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
