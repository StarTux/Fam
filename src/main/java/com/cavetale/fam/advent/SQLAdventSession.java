package com.cavetale.fam.advent;

import com.winthier.sql.SQLRow;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow.UniqueKey;
import java.util.Date;
import java.util.UUID;
import lombok.Data;

@Data
@NotNull
@Name("advent_sessions")
@UniqueKey({"player"})
public final class SQLAdventSession implements SQLRow {
    @Id private Integer id;
    private UUID player;
    private int day;
    private String tag;
    private Date updated;

    public SQLAdventSession() { }

    public SQLAdventSession(final UUID uuid) {
        this.player = uuid;
        updated = new Date();
        tag = "{}";
    }
}
