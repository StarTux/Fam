package com.cavetale.fam.sql;

import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.fam.Relation;
import com.cavetale.fam.Timer;
import com.winthier.sql.SQLRow;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow.UniqueKey;
import java.util.Date;
import java.util.UUID;
import lombok.Data;

@Data @NotNull @Name("friends")
@UniqueKey({"player_a", "player_b"})
public final class SQLFriends implements SQLRow, Comparable<SQLFriends> {
    @Id private Integer id;
    @Keyed private UUID playerA;
    @Keyed private UUID playerB;
    @Default("0") private int friendship;
    @Default("0") private int dailyGift;
    @Keyed @Nullable private String relation;
    // When did they change their status, especially marriage
    @Default("NOW()") private Date changed;
    private transient String cachedName; // Cached for sorting

    public SQLFriends() { }

    public SQLFriends(final UUID[] uuids) {
        if (uuids.length != 2) throw new IllegalStateException("length=" + uuids.length);
        this.playerA = uuids[0];
        this.playerB = uuids[1];
        this.changed = new Date();
    }

    /**
     * Helper for transfer.
     */
    public void setUuids(final UUID[] uuids) {
        if (uuids.length != 2) throw new IllegalStateException("length=" + uuids.length);
        this.playerA = uuids[0];
        this.playerB = uuids[1];
    }

    @Override
    public int compareTo(SQLFriends other) {
        int fr = Integer.compare(other.friendship, friendship); // highest first
        if (fr != 0 || cachedName == null || other.cachedName == null) return fr;
        return cachedName.compareToIgnoreCase(other.cachedName);
    }

    public UUID getOther(UUID you) {
        return you.equals(playerA) ? playerB : playerA;
    }

    public Relation getRelationEnum() {
        if (relation == null) return null;
        try {
            return Relation.valueOf(relation.toUpperCase());
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    public Relation getRelationFor(UUID you) {
        Relation result = getRelationEnum();
        if (result == null) return null;
        return you.equals(playerA)
            ? result
            : result.getInverse();
    }

    public boolean friendshipIsZero() {
        return friendship <= 0;
    }

    public boolean noRelation() {
        return relation == null;
    }

    public void setRelationFor(UUID you, Relation newRelation) {
        if (you.equals(playerA)) {
            relation = newRelation.name().toLowerCase();
        } else {
            relation = newRelation.getInverse().name().toLowerCase();
        }
    }

    public int getHearts() {
        return getHearts(friendship);
    }

    public static int getHearts(int friendship) {
        if (friendship < 1) return 0;
        if (friendship < 20) return 1;
        if (friendship < 40) return 2;
        if (friendship < 60) return 3;
        if (friendship < 80) return 4;
        return 5;
    }

    public boolean dailyGiftAvailable() {
        return dailyGift != Timer.getDayId();
    }

    public boolean dailyGiftGiven() {
        return dailyGift == Timer.getDayId();
    }

    public String getCachedName(UUID perspective) {
        if (cachedName == null) {
            cachedName = PlayerCache.nameForUuid(getOther(perspective));
        }
        return cachedName;
    }
}
