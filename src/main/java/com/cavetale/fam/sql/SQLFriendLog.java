package com.cavetale.fam.sql;

import com.cavetale.fam.Relation;
import com.winthier.sql.SQLRow;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import java.util.Date;
import java.util.UUID;
import lombok.Data;

@Data
@NotNull
@Name("friend_log")
public final class SQLFriendLog implements SQLRow {
    @Id private Integer id;
    private UUID player;
    private UUID target;
    @VarChar(255) private String relation;
    @VarChar(255) private String comment;
    private Date date;

    public SQLFriendLog() { }

    public SQLFriendLog(final UUID player, final UUID target, final Relation relation, final String comment, final Date date) {
        this.player = player;
        this.target = target;
        this.relation = relation.name().toLowerCase();
        this.comment = comment;
        this.date = date;
    }
}
