package com.cavetale.fam.advent;

import com.cavetale.core.util.Json;
import com.cavetale.fam.session.Session;
import java.util.Date;
import java.util.Set;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import static com.cavetale.fam.FamPlugin.famPlugin;
import static com.cavetale.fam.sql.Database.db;

@Getter
public final class AdventSession {
    private final Session session;
    private BukkitTask task;
    private SQLAdventSession row;
    private boolean enabled;
    private boolean disabled;
    @Setter private AdventDailyTag tag = new AdventDailyTag();
    private final boolean adventServer;

    public AdventSession(final Session session) {
        this.session = session;
        this.adventServer = Advent.advent().isAdventServer();
    }

    public void enable() {
        if (adventServer) {
            task = Bukkit.getScheduler().runTaskTimer(famPlugin(), this::tick, 1L, 1L);
        }
        db().scheduleAsyncTask(() -> {
                row = db().find(SQLAdventSession.class).eq("player", session.getUuid()).findUnique();
                if (row == null) {
                    db().insertIgnore(new SQLAdventSession(session.getUuid()));
                    row = db().find(SQLAdventSession.class).eq("player", session.getUuid()).findUnique();
                }
                Bukkit.getScheduler().runTask(famPlugin(), () -> {
                        if (disabled) return;
                        enabled = true;
                        if (adventServer) {
                            tag = new AdventDailyTag();
                            ifDaily(daily -> {
                                    tag = Json.deserialize(row.getTag(), daily.getTagClass());
                                    if (tag == null) {
                                        enabled = false;
                                        throw new IllegalStateException("Invalid tag: " + row);
                                    }
                                    daily.load(this);
                                });
                        }
                    });
            });
    }

    public void save(Runnable callback) {
        if (!enabled || disabled) return;
        row.setTag(tag.serialize());
        row.setUpdated(new Date());
        db().updateAsync(row, Set.of("day", "tag", "updated"), i -> {
                if (callback != null) callback.run();
            });
    }

    public void save() {
        save(null);
    }

    public AdventDaily getDaily() {
        if (!enabled || disabled) return null;
        if (row.getDay() == 0) return null;
        return AdventDailies.getDaily(row.getDay());
    }

    public void ifDaily(Consumer<AdventDaily> callback) {
        AdventDaily daily = getDaily();
        if (daily != null) callback.accept(daily);
    }

    public void disable() {
        if (task != null) { // if (adventServer)
            try {
                task.cancel();
            } catch (Exception e) { }
            task = null;
        }
        if (adventServer) {
            ifDaily(daily -> daily.unload(this));
        }
        enabled = false;
        disabled = true;
    }

    /**
     * Only called if adventServer is true.
     */
    private void tick() {
        ifDaily(daily -> daily.tick(this));
        final Player player = getPlayer();
        if (AdventDailies.isAdventWorld(player.getWorld()) && player.getLocation().getY() < 32.0) {
            player.damage(4.0);
        }
    }

    public void startDaily(AdventDaily daily) {
        stopDaily();
        tag = new AdventDailyTag();
        row.setDay(daily.getDay());
        daily.start(this);
        if (adventServer) {
            daily.load(this);
        }
    }

    public void stopDaily() {
        ifDaily(daily -> {
                daily.stop(this);
                if (adventServer) {
                    daily.unload(this);
                }
            });
        row.setDay(0);
        tag = new AdventDailyTag();
    }

    public Player getPlayer() {
        return session.getPlayer();
    }

    public static AdventSession of(Player player) {
        return Session.of(player).getAdventSession();
    }
}
