package com.cavetale.fam.sql;

import com.winthier.sql.SQLRow;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import lombok.Data;

/**
 * This is a single-row table storing the last dayId for which a
 * daybreak (with friendship decay) was computed. Right afterward,
 * said row will be updated with the new dayId.
 */
@Data
@NotNull
@Name("daybreak")
public final class SQLDaybreak implements SQLRow {
    @Id private Integer id;
    private int dayId;

    public SQLDaybreak() { }

    public SQLDaybreak(final int dayId) {
        this.dayId = dayId;
    }
}
