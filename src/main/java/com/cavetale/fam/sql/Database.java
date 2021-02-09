package com.cavetale.fam.sql;

import com.cavetale.fam.FamPlugin;
import com.cavetale.fam.Relation;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.winthier.sql.SQLDatabase;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class Database {
    private Database() { }
    private static Map<UUID, SQLProfile> profileCache = new HashMap<>();
    private static Map<UUID, Integer> scoreCache = new HashMap<>();

    public static SQLDatabase db() {
        return FamPlugin.getInstance().getDatabase();
    }

    public static boolean init() {
        db().registerTables(SQLFriends.class, SQLProfile.class, SQLProgress.class);
        boolean res = db().createAllTables();
        if (!res) return false;
        loadProfileCacheAsync();
        loadScoresAsync();
        return res;
    }

    public static boolean increaseFriendship(UUID a, UUID b, int amount) {
        UUID[] arr = sorted(a, b);
        String sql = "INSERT INTO `" + db().getTable(SQLFriends.class).getTableName() + "`"
            + " (player_a, player_b, friendship)"
            + " VALUES ('" + arr[0] + "', '" + arr[1] + "', " + Math.min(100, amount) + ")"
            + " ON DUPLICATE KEY UPDATE `friendship` = LEAST(100, `friendship` + " + amount + ")";
        return 0 != db().executeUpdate(sql);
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
            .orderByDescending("friendship")
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

    public static boolean storePlayerProfileAsync(Player player) {
        UUID uuid = player.getUniqueId();
        SQLProfile row = profileCache.get(uuid);
        if (row != null) {
            if (!row.load(player.getPlayerProfile())) return false;
            row.pack();
            db().updateAsync(row, null, "name", "json", "updated");
            return true;
        }
        row = new SQLProfile(uuid, player.getName());
        row.load(player.getPlayerProfile());
        profileCache.put(uuid, row);
        row.pack();
        db().insertIgnoreAsync(row, null);
        return true;
    }

    public static void loadProfileCacheAsync() {
        db().find(SQLProfile.class).findListAsync(list -> {
                for (SQLProfile row : list) {
                    profileCache.put(row.getUuid(), row);
                }
            });
    }

    public static PlayerProfile getCachedPlayerProfile(UUID uuid) {
        PlayerProfile profile = Bukkit.createProfile(uuid);
        SQLProfile row = profileCache.get(uuid);
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
                    scoreCache.put(uuid, row.getScore());
                });
        }
        return row;
    }

    public static void loadScoresAsync() {
        db().find(SQLProgress.class)
            .findListAsync(list -> {
                    for (SQLProgress row : list) {
                        scoreCache.put(row.getPlayer(), row.getScore());
                    }
                });
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

    public static int getCachedScore(UUID uuid) {
        return scoreCache.computeIfAbsent(uuid, u -> 0);
    }
}
