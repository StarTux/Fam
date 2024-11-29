package com.cavetale.fam.advent;

import java.util.List;
import net.kyori.adventure.text.Component;

/**
 * An interface for the entire Advent Daily lifecycle.
 */
public interface AdventDaily {
    int getDay();

    void setDay(int day);

    String getWarp();

    void setWarp(String warp);

    List<Component> getDescription();

    /**
     * When the plugin is enabled.
     */
    default void enable() { }

    /**
     * When the plugin is disabled.
     */
    default void disable() { }

    /**
     * When a player first starts the daily.
     */
    default void start(AdventSession session) { };

    /**
     * When the daily is loaded from the database.
     * Only called on the Advent Server.
     */
    default void load(AdventSession session) { };

    /**
     * Called once per tick while active.
     * Only called on the Advent Server.
     */
    default void tick(AdventSession session) { };

    /**
     * When the daily is removed for any reason.
     * Only called on the Advent Server.
     */
    default void unload(AdventSession session) { };

    /**
     * When a player cancels or completes the daily.
     */
    default void stop(AdventSession session) { };

    default Class<? extends AdventDailyTag> getTagClass() {
        return AdventDailyTag.class;
    }
}
