package com.cavetale.fam.sql;

import com.cavetale.fam.Relation;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

@Data
@Table(name = "friend_log")
public final class SQLFriendLog {
    @Id
    private Integer id;
    @Column(nullable = false)
    private UUID player;
    @Column(nullable = false)
    private UUID target;
    @Column(nullable = false, length = 255)
    private String relation;
    @Column(nullable = false, length = 255)
    private String comment;
    @Column(nullable = false)
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
