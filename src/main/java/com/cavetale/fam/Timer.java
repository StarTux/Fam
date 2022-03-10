package com.cavetale.fam;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Locale;
import lombok.Getter;
import org.bukkit.Bukkit;

public final class Timer {
    @Getter private static int dayId;
    @Getter private static int year;
    @Getter private static int month;
    @Getter private static int day;
    @Getter private static int hour;
    @Getter private static int dayOfWeek;

    private Timer() { }

    static void update() {
        Instant instant = Instant.now();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        LocalDate localDate = localDateTime.toLocalDate();
        year = localDate.getYear();
        month = localDate.getMonth().getValue();
        day = localDate.getDayOfMonth();
        hour = localDateTime.getHour();
        dayOfWeek = localDate.getDayOfWeek().getValue() - 1; // 0-6
        dayId = year * 10000 + month * 100 + day;
    }

    public static void enable() {
        update();
        Bukkit.getScheduler().runTaskTimer(FamPlugin.getInstance(), Timer::tick, 20L, 20L);
    }

    public static boolean isValentineSeason() {
        return false; // isValentinesDay() || (month == 2 && day <= 14);
    }

    public static boolean isValentinesDay() {
        return month == 2
            && (day == 14 || (day == 15 && hour <= 12));
    }

    private static void tick() {
        int oldDayId = dayId;
        update();
        if (oldDayId != dayId) {
            FamPlugin.getInstance().computePossibleDaybreak();
        }
    }

    public static String getTodaysName() {
        Month theMonth = Month.of(month);
        return theMonth.getDisplayName(TextStyle.FULL, Locale.US) + " " + day;
    }
}
