package com.cavetale.fam.sql;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

@Data @Table(name = "birthdays")
public final class SQLBirthday {
    @Id
    private Integer id;
    @Column(nullable = false, unique = true)
    private UUID player;
    @Column(nullable = false, length = 2)
    private int month; // 1-12
    @Column(nullable = false, length = 2)
    private int day; // 1-31

    public SQLBirthday() { }

    public SQLBirthday(final UUID player, final int month, final int day) {
        this.player = player;
        this.month = month;
        this.day = day;
    }
}
