package com.cavetale.fam.advent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import static com.cavetale.fam.FamPlugin.plugin;

public final class Advent {
    public void enable() {
        new AdventCommand(plugin()).enable();
        new AdventAdminCommand(plugin()).enable();
    }

    public static final int MAX_DAY = 2;
    public static final int THIS_YEAR = 2023;

    public List<SQLAdventPlayer> loadAllSync(UUID uuid) {
        List<SQLAdventPlayer> result = new ArrayList<>();
        for (int i = 0; i < MAX_DAY; i += 1) result.add(null);
        for (SQLAdventPlayer row : plugin().getDatabase().find(SQLAdventPlayer.class)
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
                plugin().getDatabase().insertIgnore(row);
            }
        }
        return result;
    }

    public void loadAllAsync(final UUID uuid, Consumer<List<SQLAdventPlayer>> callback) {
        plugin().getDatabase().scheduleAsyncTask(() -> {
                List<SQLAdventPlayer> result = loadAllSync(uuid);
                Bukkit.getScheduler().runTask(plugin(), () -> callback.accept(result));
            });
    }

    public static Advent advent() {
        return plugin().getAdvent();
    }
}
