package com.cavetale.fam.sql;

import com.cavetale.fam.Timer;
import com.winthier.sql.SQLRow;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.UUID;
import lombok.Data;

@Data
@NotNull
@Name("birthdays")
public final class SQLBirthday implements SQLRow {
    @Id private Integer id;
    @Unique private UUID player;
    private int month; // 1-12
    private int day; // 1-31

    public SQLBirthday() { }

    public SQLBirthday(final UUID player, final int month, final int day) {
        this.player = player;
        this.month = month;
        this.day = day;
    }

    public String getBirthdayName() {
        Month theMonth = Month.of(month);
        return theMonth.getDisplayName(TextStyle.FULL, Locale.US) + " " + day;
    }

    public boolean isToday() {
        return Timer.getMonth() == month && Timer.getDay() == day;
    }
}
