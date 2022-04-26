package com.cavetale.fam.sql;

import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.fam.FamPlugin;
import com.cavetale.fam.Relation;
import com.cavetale.fam.Timer;
import com.cavetale.fam.trophy.SQLTrophy;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.winthier.sql.SQLDatabase;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class Database {
    private Database() { }
    private static final Map<UUID, SQLProfile> PROFILE_CACHE = new HashMap<>(); // never flushed
    private static final Map<UUID, Cache> PLAYER_CACHE = new HashMap<>();

    public static SQLDatabase db() {
        return FamPlugin.getInstance().getDatabase();
    }

    public static final class Cache {
        protected int score;
        protected UUID married;
        protected final Set<UUID> friends = new HashSet<>();
        protected int birthdayMonth;
        protected int birthdayDay;

        public boolean isBirthday() {
            return birthdayMonth > 0 && birthdayDay > 0
                && Timer.getMonth() == birthdayMonth
                && Timer.getDay() == birthdayDay;
        }
    }

    public static boolean init() {
        db().registerTables(SQLFriends.class,
                            SQLProfile.class,
                            SQLProgress.class,
                            SQLFriendLog.class,
                            SQLPlayerSkin.class,
                            SQLDaybreak.class,
                            SQLBirthday.class,
                            SQLTrophy.class);
        boolean res = db().createAllTables();
        if (!res) return false;
        loadProfileCacheAsync();
        return res;
    }

    public static boolean increaseFriendship(@NonNull UUID a, @NonNull UUID b, int amount) {
        if (Objects.equals(a, b)) throw new IllegalArgumentException("Duplicate UUID: " + a);
        UUID[] arr = sorted(a, b);
        String sql = "INSERT INTO `" + db().getTable(SQLFriends.class).getTableName() + "`"
            + " (player_a, player_b, friendship)"
            + " VALUES ('" + arr[0] + "', '" + arr[1] + "', " + Math.min(100, amount) + ")"
            + " ON DUPLICATE KEY UPDATE `friendship` = LEAST(100, `friendship` + " + amount + ")";
        return 0 != db().executeUpdate(sql);
    }

    /**
     * Increase mutual friendship in a group, exactly once unique pair
     * of players.
     */
    public static int increaseMutualFriendship(final Set<UUID> set, final int amount) {
        int count = 0;
        UUID[] array = set.toArray(new UUID[0]);
        for (int i = 0; i < array.length - 1; i += 1) {
            for (int j = i + 1; j < array.length; j += 1) {
                UUID a = array[i];
                UUID b = array[j];
                if (increaseFriendship(a, b, amount)) {
                    count += 1;
                }
            }
        }
        return count;
    }

    /**
     * Increase friendship between a main player and a group of friends.
     */
    public static int increaseSingleFriendship(final UUID main, final Set<UUID> friends, final int amount) {
        if (friends.contains(main)) throw new IllegalArgumentException("Duplicate UUID: " + main);
        int count = 0;
        for (UUID friend : friends) {
            if (increaseFriendship(main, friend, amount)) {
                count += 1;
            }
        }
        return count;
    }

    public static void setRelation(SQLFriends row, UUID you, Relation relation) {
        row.setRelationFor(you, relation);
        db().update(row, "relation");
    }

    public static SQLFriends findFriends(UUID a, UUID b) {
        UUID[] arr = sorted(a, b);
        return db().find(SQLFriends.class)
            .eq("player_a", arr[0])
            .eq("player_b", arr[1])
            .findUnique();
    }

    public static List<SQLFriends> findFriendsList(UUID uuid) {
        return db().find(SQLFriends.class)
            .eq("player_a", uuid)
            .or()
            .eq("player_b", uuid)
            .findList();
    }

    public static List<SQLFriends> findFriendsList(UUID uuid, Relation relation) {
        return db().find(SQLFriends.class)
            .openParen()
            .eq("player_a", uuid)
            .or()
            .eq("player_b", uuid)
            .closeParen()
            .eq("relation", relation.name().toLowerCase())
            .findList();
    }

    public static boolean dailyGift(UUID a, UUID b, int day) {
        SQLFriends row = findFriends(a, b);
        if (row == null) {
            row = new SQLFriends(sorted(a, b));
            row.setDailyGift(day);
            // If in the meantime a row was inserted with the same key
            // but non-identical days, the players are out of luck and
            // have to try again.
            return 0 != db().insertIgnore(row);
        } else {
            if (row.getDailyGift() == day) return false;
            String sql = "UPDATE `" + db().getTable(SQLFriends.class).getTableName() + "`"
                + " SET `daily_gift` = " + day
                + " WHERE `id` = " + row.getId()
                + " AND `daily_gift` != " + day;
            return 0 != db().executeUpdate(sql);
        }
    }

    public static UUID[] sorted(UUID a, UUID b) {
        UUID[] result = new UUID[2];
        result[0] = a;
        result[1] = b;
        Arrays.sort(result);
        return result;
    }

    public static SQLProfile storePlayerProfileAsync(Player player) {
        UUID uuid = player.getUniqueId();
        SQLProfile row = PROFILE_CACHE.get(uuid);
        if (row != null) {
            if (!row.load(player.getPlayerProfile())) return row;
            row.pack();
            db().updateAsync(row, null, "name", "json", "texture_url", "updated");
            return row;
        }
        row = new SQLProfile(uuid, player.getName());
        row.load(player.getPlayerProfile());
        PROFILE_CACHE.put(uuid, row);
        row.pack();
        db().insertIgnoreAsync(row, null);
        return row;
    }

    public static void loadProfileCacheAsync() {
        db().find(SQLProfile.class).findListAsync(list -> {
                for (SQLProfile row : list) {
                    PROFILE_CACHE.put(row.getUuid(), row);
                }
            });
    }

    public static void fetchPlayerSkinAsync(final String textureUrl) {
        db().scheduleAsyncTask(() -> {
                SQLPlayerSkin row = db().find(SQLPlayerSkin.class)
                    .eq("texture_url", textureUrl)
                    .findUnique();
                if (row != null) return;
                row = new SQLPlayerSkin(textureUrl);
                row.loadTexture(); // may throw
                if (row.getTextureBase64() == null) return;
                db().insertIgnore(row);
            });
    }

    public static PlayerProfile getCachedPlayerProfile(UUID uuid) {
        PlayerProfile profile = Bukkit.createProfile(uuid);
        SQLProfile row = PROFILE_CACHE.get(uuid);
        if (row == null) return profile;
        row.fill(profile);
        return profile;
    }

    public static SQLProgress findProgress(UUID uuid) {
        SQLProgress row = db().find(SQLProgress.class)
            .eq("player", uuid)
            .findUnique();
        if (row != null) {
            Bukkit.getScheduler().runTask(FamPlugin.getInstance(), () -> {
                    Cache cache = PLAYER_CACHE.get(uuid);
                    if (cache != null) cache.score = row.getScore();
                });
        }
        return row;
    }

    public static void addProgress(UUID uuid) {
        String sql = "INSERT INTO `" + db().getTable(SQLProgress.class).getTableName() + "`"
            + " (player, score, claimed)"
            + " VALUES ('" + uuid + "', 1, 0)"
            + " ON DUPLICATE KEY UPDATE `score` = `score` + 1";
        db().executeUpdate(sql);
    }

    public static boolean claimProgress(SQLProgress row) {
        String sql = "UPDATE `" + db().getTable(SQLProgress.class).getTableName() + "`"
            + " SET `claimed` = " + (row.getClaimed() + 1)
            + " WHERE `id` = " + row.getId()
            + " AND `claimed` = " + row.getClaimed();
        return 0 != db().executeUpdate(sql);
    }

    public static void fillCacheAsync(Player player) {
        fillCacheAsync(player.getUniqueId());
    }

    public static void fillCacheAsync(UUID uuid) {
        db().scheduleAsyncTask(() -> {
                final Cache cache = new Cache();
                for (SQLFriends row : findFriendsList(uuid)) {
                    Relation relation = row.getRelationEnum();
                    if (relation == Relation.FRIEND) {
                        cache.friends.add(row.getOther(uuid));
                    } else if (relation == Relation.MARRIED) {
                        cache.married = row.getOther(uuid);
                    }
                }
                SQLBirthday birthday = findBirthday(uuid);
                if (birthday != null) {
                    cache.birthdayMonth = birthday.getMonth();
                    cache.birthdayDay = birthday.getDay();
                }
                SQLProgress progress = findProgress(uuid);
                cache.score = progress != null ? progress.getScore() : 0;
                Bukkit.getScheduler().runTask(FamPlugin.getInstance(), () -> {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player == null) return;
                        PLAYER_CACHE.put(uuid, cache);
                        PluginPlayerEvent.Name.PLAYER_SESSION_LOADED.call(FamPlugin.getInstance(), player);
                    });
            });
    }

    public static void clearCache(UUID uuid) {
        PLAYER_CACHE.remove(uuid);
    }

    public static Cache getCache(Player player) {
        return PLAYER_CACHE.computeIfAbsent(player.getUniqueId(), u -> new Cache());
    }

    public static int getCachedScore(UUID uuid) {
        Cache cache = PLAYER_CACHE.get(uuid);
        return cache != null ? cache.score : 0;
    }

    public static boolean isMarriageCached(Player a, Player b) {
        return Objects.equals(getCache(a).married, b.getUniqueId());
    }

    public static UUID getMarriageCached(Player a) {
        return getCache(a).married;
    }

    public static boolean isFriendsCached(Player a, Player b) {
        return getCache(a).friends.contains(b.getUniqueId());
    }

    public static Set<UUID> getFriendsCached(Player player) {
        return getCache(player).friends;
    }

    public static int countFriendsCached(Player player) {
        return getFriendsCached(player).size();
    }

    public static Player getCachedMarriage(Player player) {
        UUID uuid = getMarriageCached(player);
        return uuid != null
            ? Bukkit.getPlayer(uuid)
            : null;
    }

    public static boolean isBirthdayCached(Player player) {
        return getCache(player).birthdayMonth > 0;
    }

    public static void friendLogAsync(UUID player, UUID target, Relation relation, String comment) {
        db().insertAsync(new SQLFriendLog(player, target, relation, comment, new Date()), null);
    }

    public static SQLBirthday findBirthday(UUID uuid) {
        return db().find(SQLBirthday.class).eq("player", uuid).findUnique();
    }

    public static Map<UUID, SQLBirthday> findTodaysBirthdayMap() {
        Map<UUID, SQLBirthday> result = new HashMap<>();
        for (SQLBirthday row :db().find(SQLBirthday.class)
                 .eq("month", Timer.getMonth())
                 .eq("day", Timer.getDay())
                 .findList()) {
            result.put(row.getPlayer(), row);
        }
        return result;
    }

    public static Map<UUID, SQLBirthday> findBirthdayMap() {
        Map<UUID, SQLBirthday> result = new HashMap<>();
        for (SQLBirthday row : db().find(SQLBirthday.class).findList()) {
            result.put(row.getPlayer(), row);
        }
        return result;
    }
}
