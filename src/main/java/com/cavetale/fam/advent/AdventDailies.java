package com.cavetale.fam.advent;

import com.cavetale.core.struct.Vec3i;
import com.cavetale.mytems.Mytems;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.World;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class AdventDailies {
    private static List<AdventDaily> dailies = new ArrayList<>(25);
    private static Set<String> adventWorldNames = new HashSet<>();

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
                                                                            Vec3i.of(262, 67, 255),
                                                                            Mytems.RUBY_COIN.createItemStack(),
                                                                            Color.RED,
                                                                            List.of(Vec3i.of(289, 66, 254),
                                                                                    Vec3i.of(324, 66, 236),
                                                                                    Vec3i.of(251, 67, 225),
                                                                                    Vec3i.of(348, 75, 291),
                                                                                    Vec3i.of(274, 66, 280),
                                                                                    Vec3i.of(211, 65, 256)));
        daily02.setDescription(List.of(textOfChildren(Mytems.RUBY_COIN, text("Find the six ruby coins."))));
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
        adventWorldNames.add(daily.getWorldName());
    }

    public static List<Component> getPage(int day) {
        return getDaily(day).getDescription();
    }

    public static boolean isAdventWorld(World world) {
        return adventWorldNames.contains(world.getName());
    }

    private AdventDailies() { }
}
