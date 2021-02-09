package com.cavetale.fam;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.bukkit.Bukkit;
import lombok.Getter;

public final class Timer {
    @Getter private static int dayId;
    @Getter private static int year;
    @Getter private static int month;
    @Getter private static int day;
    @Getter private static int dayOfWeek;

    private Timer() { }

    static void update() {
        Instant instant = Instant.now();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        LocalDate localDate = localDateTime.toLocalDate();
        year = localDate.getYear();
        month = localDate.getMonth().getValue();
        day = localDate.getDayOfMonth();
        dayOfWeek = localDate.getDayOfWeek().getValue() - 1; // 0-6
        dayId = year * 10000 + month * 100 + day;
    }

    public static void enable() {
        update();
        Bukkit.getScheduler().runTaskTimer(FamPlugin.getInstance(), Timer::update, 200L, 200L);
    }

    public static boolean isValentineSeason() {
        return month == 2 && day <= 14;
    }

    public static boolean isValentinesDay() {
        return month == 2 && day == 14;
    }
}
