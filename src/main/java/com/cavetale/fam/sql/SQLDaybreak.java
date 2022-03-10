package com.cavetale.fam.sql;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

/**
 * This is a single-row table storing the last dayId for which a
 * daybreak (with friendship decay) was computed. Right afterward,
 * said row will be updated with the new dayId.
 */
@Data @Table(name = "daybreak")
public final class SQLDaybreak {
    @Id
    private Integer id;
    @Column(nullable = false)
    private int dayId;

    public SQLDaybreak() { }

    public SQLDaybreak(final int dayId) {
        this.dayId = dayId;
    }
}
