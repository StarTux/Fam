package com.cavetale.fam.sql;

import com.winthier.sql.SQLRow;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow.UniqueKey;
import java.util.Date;
import java.util.UUID;
import lombok.Data;

@Data
@NotNull
@Name("elo")
@UniqueKey(value = {"category", "player"}, name = "category_player")
public final class SQLElo implements SQLRow {
    public static final double DEFAULT_K = 32.0;
    public static final double DEFAULT_RATING = 500.0;
    private Integer id;
    private String category;
    private UUID player;
    private double rating;
    private int games;
    private Date lastUpdate;

    public SQLElo() { }

    public SQLElo(final String category, final UUID player) {
        this.category = category;
        this.player = player;
        this.rating = DEFAULT_RATING;
        this.games = 0;
        this.lastUpdate = new Date();
    }

    public double computeWinProbabilityAgainst(double opponent) {
        return 1.0 / (1.0 + Math.pow(10.0, (rating - opponent) / 400.0));
    }

    public void updateRatingAgainst(double opponent, double winResult, double k) {
        final double winChance = computeWinProbabilityAgainst(opponent);
        this.rating = rating + k * (winResult - winChance);
        this.lastUpdate = new Date();
    }

    public void updateRatingAgainst(double opponent, double winResult) {
        updateRatingAgainst(opponent, winResult, DEFAULT_K);
    }

    public void increaseGames() {
        games += 1;
    }
}
