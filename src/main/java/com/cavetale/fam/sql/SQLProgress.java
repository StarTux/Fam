package com.cavetale.fam.sql;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import com.cavetale.fam.FamPlugin;

@Data
@Table(name = "progress")
public final class SQLProgress {
    @Id
    private Integer id;
    @Column(nullable = false, unique = true)
    UUID player;
    @Column(nullable = false, columnDefinition = "INT(3) DEFAULT 0")
    int score;
    @Column(nullable = false, columnDefinition = "INT(3) DEFAULT 0")
    int claimed;

    public SQLProgress() { }

    public SQLProgress(final UUID player) {
        this.player = player;
    }

    public int getAvailable() {
        return Math.min(FamPlugin.getInstance().getRewards().size() - 1, score / 10);
    }

    public boolean isRewardAvailable() {
        return getAvailable() > claimed;
    }
}
