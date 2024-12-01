package com.cavetale.fam.advent;

import com.cavetale.core.struct.Vec3i;
import com.cavetale.mytems.Mytems;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class AdventDailies {
    private static List<AdventDaily> dailies = new ArrayList<>(25);

    static {
        for (int i = 0; i < 25; i += 1) {
            dailies.add(new AdventDailyDummy(i + 1));
        }
    }

    protected static void enable() {
        // 01
        final AdventDailyGetStar daily01 = new AdventDailyGetStar("advent_2024_01", Vec3i.of(364, 127, 262));
        daily01.setDescription(List.of(textOfChildren(Mytems.STAR, text("Climb the gingerbread castle."))));
        setDaily(1, daily01);
        // 02
        final AdventDailyCollectItems daily02 = new AdventDailyCollectItems("advent_2024_01",
                                                                            Vec3i.of(256, 65, 253),
                                                                            Mytems.GOLDEN_COIN.createItemStack(),
                                                                            List.of(Vec3i.of(263, 65, 252),
                                                                                    Vec3i.of(268, 65, 259),
                                                                                    Vec3i.of(263, 65, 256)));
        setDaily(2, daily02);
        // Finis
        for (int i = 0; i < dailies.size(); i += 1) {
            AdventDaily daily = dailies.get(i);
            if (i < 7) {
                daily.setWarp("Advent2024-01");
            } else if (i < 14) {
                daily.setWarp("Advent2024-02");
            } else if (i < 21) {
                daily.setWarp("Advent2024-03");
            } else {
                daily.setWarp("Advent2024-04");
            }
        }
        for (AdventDaily daily : dailies) {
            daily.enable();
        }
    }

    protected static void disable() {
        for (AdventDaily daily : dailies) {
            daily.disable();
        }
    }

    public static AdventDaily getDaily(int day) {
        int index = day - 1;
        if (index < 0 || index >= dailies.size()) return null;
        return dailies.get(index);
    }

    public static void setDaily(int day, AdventDaily daily) {
        dailies.set(day - 1, daily);
        daily.setDay(day);
    }

    public static List<Component> getPage(int day) {
        return getDaily(day).getDescription();
    }

    private AdventDailies() { }
}
