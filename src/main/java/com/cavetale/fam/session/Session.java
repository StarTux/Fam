package com.cavetale.fam.session;

import com.cavetale.fam.sql.SQLPlayer;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import static com.cavetale.fam.sql.Database.db;

@Getter
public final class Session {
    protected final UUID uuid;
    protected String name;
    protected SQLPlayer playerRow;
    protected boolean ready;

    protected Session(final UUID uuid, final String name) {
        this.uuid = uuid;
        this.name = name;
    }

    protected Session(final Player player) {
        this(player.getUniqueId(), player.getName());
    }

    protected void enable() {
        reload();
    }

    protected void disable() {
    }

    public void reload() {
        ready = false;
        db().find(SQLPlayer.class).eq("uuid", uuid).findUniqueAsync(row -> {
                if (row == null) {
                    this.playerRow = new SQLPlayer(uuid);
                    db().insertAsync(playerRow, null);
                } else {
                    this.playerRow = row;
                }
                ready = true;
            });
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public static Session of(Player player) {
        return Sessions.sessionOf(player);
    }

    public static Session of(UUID uuid) {
        return Sessions.sessionOf(uuid);
    }
}
