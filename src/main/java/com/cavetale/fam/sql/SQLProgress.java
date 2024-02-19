package com.cavetale.fam.sql;

import com.cavetale.fam.FamPlugin;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow.UniqueKey;
import com.winthier.sql.SQLRow;
import java.util.UUID;
import lombok.Data;

/**
 * Player score in the Valentine Gift Giving event.
 */
@Data @NotNull
@Name("progress")
@UniqueKey({"player", "year"})
public final class SQLProgress implements SQLRow {
    @Id private Integer id;
    private UUID player;
    @Default("2020") private int year;
    @Default("0") private int score;
    @Default("0") private int claimed;

    public SQLProgress() { }

    public SQLProgress(final UUID player, final int year, final int score, final int claimed) {
        this.player = player;
        this.year = year;
        this.score = score;
        this.claimed = claimed;
    }

    public int getAvailable() {
        return Math.min(FamPlugin.getInstance().getRewardList().size(), score / 7);
    }

    public boolean isRewardAvailable() {
        return getAvailable() > claimed;
    }
}
