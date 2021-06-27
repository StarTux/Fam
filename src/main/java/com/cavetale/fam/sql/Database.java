package com.cavetale.fam.sql;

import com.cavetale.fam.FamPlugin;
import com.cavetale.fam.Relation;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.winthier.sql.SQLDatabase;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class Database {
    private Database() { }
    private static Map<UUID, SQLProfile> profileCache = new HashMap<>(); // never flushed
    private static Map<UUID, Integer> scoreCache = new HashMap<>();
    private static Map<UUID, UUID> marriedCache = new HashMap<>();

    public static SQLDatabase db() {
        return FamPlugin.getInstance().getDatabase();
    }

    public static boolean init() {
        db().registerTables(SQLFriends.class,
                            SQLProfile.class,
                            SQLProgress.class,
                            SQLFriendLog.class,
                            SQLPlayerSkin.class,
                            SQLDaybreak.class,
                            SQLBirthday.class);
        boolean res = db().createAllTables();
        if (!res) return false;
        loadProfileCacheAsync();
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
        SQLProfile row = profileCache.get(uuid);
        if (row != null) {
            if (!row.load(player.getPlayerProfile())) return row;
            row.pack();
            db().updateAsync(row, null, "name", "json", "texture_url", "updated");
            return row;
        }
        row = new SQLProfile(uuid, player.getName());
        row.load(player.getPlayerProfile());
        profileCache.put(uuid, row);
        row.pack();
        db().insertIgnoreAsync(row, null);
        return row;
    }

    public static void loadProfileCacheAsync() {
        db().find(SQLProfile.class).findListAsync(list -> {
                for (SQLProfile row : list) {
                    profileCache.put(row.getUuid(), row);
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

    public static void fillCacheAsync(Player player) {
        UUID uuid = player.getUniqueId();
        db().scheduleAsyncTask(() -> {
                List<SQLFriends> married = findFriendsList(uuid, Relation.MARRIED);
                if (!married.isEmpty()) marriedCache.put(uuid, married.get(0).getOther(uuid));
                SQLProgress progress = findProgress(uuid);
                scoreCache.put(uuid, progress != null ? progress.getScore() : 0);
            });
    }

    public static void clearCacheAsync(Player player) {
        UUID uuid = player.getUniqueId();
        marriedCache.remove(uuid);
        scoreCache.remove(uuid);
    }

    public static boolean isMarriageCached(Player a, Player b) {
        return Objects.equals(marriedCache.get(a.getUniqueId()), b.getUniqueId());
    }

    public static Player getCachedMarriage(Player player) {
        UUID uuid = marriedCache.get(player.getUniqueId());
        if (uuid == null) return null;
        return Bukkit.getPlayer(uuid);
    }

    public static void friendLogAsync(UUID player, UUID target, Relation relation, String comment) {
        db().insertAsync(new SQLFriendLog(player, target, relation, comment, new Date()), null);
    }

    public static SQLBirthday findBirthday(Player player) {
        return db().find(SQLBirthday.class).eq("player", player.getUniqueId()).findUnique();
    }
}
