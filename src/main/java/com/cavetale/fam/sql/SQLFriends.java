package com.cavetale.fam.sql;

import com.cavetale.fam.Relation;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Table(name = "friends",
       uniqueConstraints = {@UniqueConstraint(columnNames = {"player_a", "player_b"})})
public final class SQLFriends implements Comparable<SQLFriends> {
    @Id
    private Integer id;
    @Column(nullable = false)
    UUID playerA;
    @Column(nullable = false)
    UUID playerB;
    @Column(nullable = false, columnDefinition = "INT(3) DEFAULT 0")
    int friendship;
    @Column(nullable = false, columnDefinition = "INT(8) DEFAULT 0")
    int dailyGift;
    @Column(nullable = true)
    String relation;

    public SQLFriends() { }

    public SQLFriends(final UUID[] uuids) {
        if (uuids.length != 2) throw new IllegalStateException("length=" + uuids.length);
        this.playerA = uuids[0];
        this.playerB = uuids[1];
    }

    @Override
    public int compareTo(SQLFriends other) {
        return Integer.compare(friendship, other.friendship);
    }

    public UUID getOther(UUID you) {
        return you.equals(playerA) ? playerB : playerA;
    }

    public Relation getRelation() {
        if (relation == null) return null;
        try {
            return Relation.valueOf(relation.toUpperCase());
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    public Relation getRelationFor(UUID you) {
        Relation result = getRelation();
        if (result == null) return null;
        return you.equals(playerA)
            ? result
            : result.getInverse();
    }

    public boolean friendshipIsZero() {
        return friendship <= 0;
    }

    public void setRelationFor(UUID you, Relation newRelation) {
        if (you.equals(playerA)) {
            relation = newRelation.name().toLowerCase();
        } else {
            relation = newRelation.getInverse().name().toLowerCase();
        }
    }

    public int getHearts() {
        return Math.min(5, (friendship - 1) / 20 + 1);
    }
}
