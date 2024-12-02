package com.cavetale.fam.advent;

public final class AdventDailyDummy extends AbstractAdventDaily {
    public AdventDailyDummy(final int day) {
        setDay(day);
    }

    @Override
    public String getWorldName() {
        return "";
    }
}
