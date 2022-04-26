package com.cavetale.fam.trophy;

import com.cavetale.fam.FamPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import static com.cavetale.fam.sql.Database.db;

public final class Trophies {
    private static Trophies instance;
    protected final FamPlugin plugin;
    protected final TrophyCommand command;
    protected final TrophyAdminCommand adminCommand;
    protected final TrophyListener listener = new TrophyListener(this);

    public Trophies(final FamPlugin plugin) {
        this.plugin = plugin;
        command = new TrophyCommand(this);
        adminCommand = new TrophyAdminCommand(plugin);
    }

    public void enable() {
        instance = this;
        command.enable();
        adminCommand.enable();
        listener.enable();
    }

    public static void findTrophyAsync(int id, Consumer<SQLTrophy> callback) {
        db().find(SQLTrophy.class).eq("id", id).findUniqueAsync(callback);
    }

    public static List<SQLTrophy> findTrophies(UUID owner) {
        return db().find(SQLTrophy.class).eq("owner", owner).findList();
    }

    public static void findTrophiesAsync(UUID owner, Consumer<List<SQLTrophy>> callback) {
        db().find(SQLTrophy.class).eq("owner", owner).findListAsync(callback);
    }

    public static List<SQLTrophy> findTrophies(UUID owner, String category) {
        return db().find(SQLTrophy.class)
            .eq("owner", owner)
            .eq("category", category)
            .findList();
    }

    public static void findTrophiesAsync(UUID owner, String category, Consumer<List<SQLTrophy>> callback) {
        db().find(SQLTrophy.class)
            .eq("owner", owner)
            .eq("category", category)
            .findListAsync(callback);
    }

    public static List<SQLTrophy> findTrophies(String category) {
        return db().find(SQLTrophy.class)
            .eq("category", category)
            .findList();
    }

    public static void insertTrophies(List<SQLTrophy> trophies) {
        db().insertAsync(trophies, null);
        List<UUID> uuids = new ArrayList<>();
        for (SQLTrophy row : trophies) {
            if (row.owner != null && !row.seen) uuids.add(row.owner);
        }
        if (!uuids.isEmpty()) {
            instance.listener.refreshUnseenTrophies(uuids);
        }
    }


    public static void findUnseenTrophiesAsync(List<UUID> owners, Consumer<List<SQLTrophy>> callback) {
        db().find(SQLTrophy.class)
            .in("owner", owners)
            .eq("seen", 0)
            .findListAsync(callback);
    }

    public static int transfer(UUID from, UUID to) {
        return db().update(SQLTrophy.class)
            .where(q -> q.eq("owner", from))
            .set("owner", to)
            .sync();
    }
}
