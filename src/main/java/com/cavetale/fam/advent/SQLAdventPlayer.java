package com.cavetale.fam.advent;

import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow.UniqueKey;
import com.winthier.sql.SQLRow;
import java.util.Date;
import java.util.UUID;
import lombok.Data;

@Data
@NotNull
@Name("advent_players")
@UniqueKey({"player", "year", "day"})
public final class SQLAdventPlayer implements SQLRow {
    @Id private Integer id;
    private UUID player;
    private int year;
    private int day;
    private boolean opened;
    private boolean solved;
    private boolean rewarded;
    @Nullable private Date openedTime;
    @Nullable private Date solvedTime;
    @Nullable private Date rewardedTime;

    public SQLAdventPlayer() { }

    public SQLAdventPlayer(final UUID uuid, final int year, final int day) {
        this.player = uuid;
        this.year = year;
        this.day = day;
    }
}
