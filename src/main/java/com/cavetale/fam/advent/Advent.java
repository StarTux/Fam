package com.cavetale.fam.advent;

import com.cavetale.core.connect.NetworkServer;
import com.cavetale.fam.Timer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Getter;
import org.bukkit.Bukkit;
import static com.cavetale.fam.FamPlugin.famPlugin;
import static com.cavetale.fam.sql.Database.db;

@Getter
public final class Advent {
    private boolean adventServer;
    private AdventMobs adventMobs;

    public void enable() {
        adventServer = switch (NetworkServer.current()) {
        case BETA -> true;
        case CHALLENGE -> true;
        default -> false;
        };
        new AdventCommand(famPlugin()).enable();
        new AdventAdminCommand(famPlugin()).enable();
        AdventDailies.enable();
        famPlugin().getLogger().info("Is Advent Server: " + adventServer);
        if (adventServer) {
            new AdventListener().enable();
            adventMobs = new AdventMobs();
            adventMobs.enable();
            AdventMusic.enable();
        }
    }

    public void disable() {
        AdventDailies.disable();
        if (adventMobs != null) {
            adventMobs.disable();
        }
    }

    public static final int MAX_DAY = 25;
    public static final int THIS_YEAR = 2024;

    public List<SQLAdventPlayer> loadAllSync(UUID uuid) {
        List<SQLAdventPlayer> result = new ArrayList<>();
        for (int i = 0; i < MAX_DAY; i += 1) result.add(null);
        for (SQLAdventPlayer row : famPlugin().getDatabase().find(SQLAdventPlayer.class)
                 .eq("player", uuid)
                 .eq("year", THIS_YEAR)
                 .findList()) {
            if (row.getDay() < 1 || row.getDay() > MAX_DAY) continue;
            result.set(row.getDay() - 1, row);
        }
        for (int i = 0; i < MAX_DAY; i += 1) {
            SQLAdventPlayer row = result.get(i);
            if (row == null) {
                row = new SQLAdventPlayer(uuid, THIS_YEAR, i + 1);
                result.set(i, row);
                famPlugin().getDatabase().insertIgnore(row);
            }
        }
        return result;
    }

    public void loadAllAsync(final UUID uuid, Consumer<List<SQLAdventPlayer>> callback) {
        famPlugin().getDatabase().scheduleAsyncTask(() -> {
                List<SQLAdventPlayer> result = loadAllSync(uuid);
                Bukkit.getScheduler().runTask(famPlugin(), () -> callback.accept(result));
            });
    }

    public static int getThisDay() {
        if (Timer.getYear() > Advent.THIS_YEAR) return 25;
        if (Timer.getYear() == Advent.THIS_YEAR && Timer.getMonth() == 12) {
            return Math.min(Timer.getDay(), 25);
        }
        return 0;
    }

    public static Advent advent() {
        return famPlugin().getAdvent();
    }

    public static void unlock(UUID uuid, int year, int day, Consumer<Boolean> callback) {
        db().update(SQLAdventPlayer.class)
            .where(s -> s
                   .eq("player", uuid)
                   .eq("year", year)
                   .eq("day", day)
                   .eq("opened", true)
                   .eq("solved", false))
            .set("solved", true)
            .set("solvedTime", new Date())
            .async(res -> {
                    if (res != 0) {
                        famPlugin().getLogger().info("Solved: " + uuid + ", " + year + ", " + day);
                    }
                    if (callback != null) {
                        callback.accept(res != 0);
                    }
                });
    }
}
