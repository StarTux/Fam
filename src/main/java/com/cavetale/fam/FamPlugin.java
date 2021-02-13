package com.cavetale.fam;

import com.cavetale.fam.sql.Database;
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
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
    private PlayerListener eventListener = new PlayerListener(this);
    private WeddingRingListener weddingRingListener = new WeddingRingListener(this);
    private MarriageListener marriageListener = new MarriageListener(this);
    private SQLDatabase database = new SQLDatabase(this);
    private List<Reward> rewards;

    @Override
    public void onEnable() {
        instance = this;
        famCommand.enable();
        friendsCommand.enable();
        valentineCommand.enable();
        friendCommand.enable();
        loveCommand.enable();
        eventListener.enable();
        weddingRingListener.enable();
        marriageListener.enable();
        new SidebarListener(this).enable();
        Database.init();
        Timer.enable();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Database.fillCacheAsync(player);
            Database.storePlayerProfileAsync(player);
        }
        Gui.enable(this);
        rewards = new ArrayList<>(10);
        // 1
        rewards.add(new Reward().item(new ItemStack(Material.DIAMOND, 4), 5));
        // 2
        rewards.add(new Reward()
                    .item(new ItemStack(Material.MELON_SLICE, 16), 5)
                    .item(new ItemStack(Material.APPLE, 16), 6));
        // 3
        rewards.add(new Reward().item(new ItemStack(Material.COOKIE, 16), 5));
        // 4
        rewards.add(new Reward().item(new ItemStack(Material.PUMPKIN_PIE, 16), 5));
        // 5
        rewards.add(new Reward()
                    .item(new ItemStack(Material.NETHER_STAR))
                    .item(new ItemStack(Material.OBSIDIAN), 4));
        // 6
        rewards.add(new Reward().item(new ItemStack(Material.CAKE), 11));
        // 7
        rewards.add(new Reward()
                    .item(new ItemStack(Material.DIAMOND))
                    .item(Mytems.KITTY_COIN.getMytem().getItem(), 4)
                    .item(new ItemStack(Material.MELON_SLICE, 16), 6));
        // 8
        rewards.add(new Reward().item(new ItemStack(Material.GOLDEN_APPLE), 27));
        // 9
        rewards.add(new Reward().item(new ItemStack(Material.NETHERITE_INGOT), 5));
        // 10
        rewards.add(new Reward().item(Mytems.WEDDING_RING.getMytem().getItem()));
        for (int i = 0; i < 4; i += 1) {
            rewards.add(new Reward().item(new ItemStack(Material.DIAMOND), 3 * 9));
            rewards.add(new Reward().item(new ItemStack(Material.EMERALD), 3 * 9));
        }
    }

    @Override
    public void onDisable() {
        Gui.disable(this);
    }

    public static final Material[] FOODS = {
        Material.MELON_SLICE, // Monday
        Material.APPLE, // Tuesday
        Material.COOKIE, // Wednesday
        Material.PUMPKIN_PIE, // Thursday
        Material.SWEET_BERRIES, // Friday
        Material.CAKE, // Saturday
        Material.GOLDEN_APPLE // Sunday
    };

    public static Material getTodaysFood() {
        return FOODS[Timer.getDayOfWeek()];
    }

    public static ItemStack makeTodaysFoodIcon() {
        return Items.button(getTodaysFood(),
                            Arrays.asList(Text.builder("Today's Friendship Gift").color(Colors.PINK).create(),
                                          Text.builder("One Point per Player.").color(Colors.YELLOW).create(),
                                          Text.builder("New Item every Day.").color(Colors.YELLOW).create()));
    }

    public static ItemStack makeSkull(SQLFriends row, UUID perspective) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        UUID friendUuid = row.getOther(perspective);
        PlayerProfile profile = Database.getCachedPlayerProfile(friendUuid);
        meta.setPlayerProfile(profile);
        Relation relation = row.getRelationFor(perspective);
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
        if (relation != null) {
            lore.add(Text.builder(relation.humanName).color(Colors.PINK).italic(false).create());
        }
        if (row.getDailyGift() == Timer.getDayId()) {
            lore.add(Text.builder("\u2611 Daily Gift").color(Colors.PINK).italic(false).create());
        } else {
            lore.add(Text.builder("\u2610 Daily Gift").color(Colors.DARK_GRAY).italic(false).create());
        }
        meta.setLoreComponents(lore);
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
                Collections.sort(friendsList);
                Bukkit.getScheduler().runTask(instance, () -> openFriendsGui(player, friendsList, page));
            });
    }

    public static Gui openFriendsGui(Player player, List<SQLFriends> friendsList, int pageNumber) {
        if (!player.isValid()) return null;
        friendsList.removeIf(SQLFriends::friendshipIsZero);
        Gui gui = new Gui(instance);
        int pageSize = 3 * 9;
        int pageCount = (friendsList.size() - 1) / pageSize + 1;
        int pageIndex = Math.min(pageNumber - 1, pageCount - 1);
        int offset = pageIndex * pageSize;
        gui.size(pageSize + 9);
        gui.title(pageCount > 1 ? ChatColor.RED + "Friends " + pageNumber + "/" + pageCount : ChatColor.RED + "Friends");
        for (int i = 0; i < pageSize; i += 1) {
            int friendsIndex = offset + i;
            if (friendsIndex >= friendsList.size()) break;
            SQLFriends row = friendsList.get(friendsIndex);
            ItemStack itemStack = makeSkull(row, player.getUniqueId());
            gui.setItem(i, itemStack, click -> openFriendGui(player, row, pageNumber));
        }
        if (pageIndex > 0) {
            int to = pageNumber - 1;
            gui.setItem(3 * 9, Items.button(Material.ARROW, ChatColor.GRAY + "Previous Page"), c -> openFriendsGui(player, friendsList, to));
        }
        if (pageIndex < pageCount - 1) {
            int to = pageNumber + 1;
            gui.setItem(3 * 9 + 8, Items.button(Material.ARROW, ChatColor.GRAY + "Next Page"), c -> openFriendsGui(player, friendsList, to));
        }
        gui.setItem(3 * 9 + 4, Items.button(getTodaysFood(), ChatColor.GREEN + "Today's Friendship Gift"));
        gui.open(player);
        return gui;
    }

    public static void openFriendGui(Player player, UUID friendUuid, int page) {
        instance.database.scheduleAsyncTask(() -> {
                SQLFriends row = Database.findFriends(player.getUniqueId(), friendUuid);
                final SQLFriends row2 = row != null ? row : new SQLFriends(Database.sorted(player.getUniqueId(), friendUuid));
                Bukkit.getScheduler().runTask(instance, () -> openFriendGui(player, row2, page));
            });
    }

    public static Gui openFriendGui(Player player, SQLFriends row, int page) {
        UUID friendUuid = row.getOther(player.getUniqueId());
        PlayerProfile profile = Database.getCachedPlayerProfile(friendUuid);
        String name = PlayerCache.nameForUuid(friendUuid);
        if (name == null) name = profile.getName();
        Gui gui = new Gui(instance);
        gui.title(name);
        gui.size(3 * 9);
        gui.setItem(9 + 4, makeSkull(row, player.getUniqueId()));
        if (row.getDailyGift() != Timer.getDayId()) {
            gui.setItem(9 + 5, makeTodaysFoodIcon());
        }
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
        int size = instance.rewards.size();
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
        gui.setItem(guiSize - 5, makeTodaysFoodIcon());
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
        Reward reward = instance.rewards.get(index);
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
}
