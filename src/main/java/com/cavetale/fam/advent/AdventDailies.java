package com.cavetale.fam.advent;

import com.cavetale.core.font.VanillaPaintings;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Items;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Note;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import static com.cavetale.fam.FamPlugin.famPlugin;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class AdventDailies {
    @Getter
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
        final AdventDailyCollectItems daily02 = new AdventDailyCollectItems("advent_2024_01", Vec3i.of(262, 67, 255),
                                                                            Mytems.RUBY.createItemStack(),
                                                                            Color.RED);
        daily02.addItemLocation(289, 66, 254)
            .addItemLocation(324, 66, 236)
            .addItemLocation(251, 67, 225)
            .addItemLocation(348, 75, 291)
            .addItemLocation(274, 66, 280)
            .addItemLocation(211, 65, 256);
        daily02.setDescription(List.of(textOfChildren(Mytems.RUBY, text("Find the six red rubies."))));
        setDaily(2, daily02);
        // 03
        final AdventDailyKillMob daily03 = new AdventDailyKillMob("advent_2024_01",
                                                                  Vec3i.of(347, 93, 254),
                                                                  EntityType.IRON_GOLEM);
        daily03.setDescription(List.of(text("Defeat the Iron Golem.")));
        setDaily(3, daily03);
        // 04
        final AdventDailyGetStar daily04 = new AdventDailyGetStar("advent_2024_01", Vec3i.of(377, 117, 253));
        daily04.setDescription(List.of(textOfChildren(text("Jump through the "),
                                                      VanillaPaintings.HUMBLE,
                                                      text(" painting."))));
        setDaily(4, daily04);
        // 05
        final AdventDailyPlayMusic daily05 = new AdventDailyPlayMusic("advent_2024_01", Vec3i.of(301, 75, 231), EntityType.SNOW_GOLEM);
        daily05.setDescription(List.of(textOfChildren(Mytems.C_NOTE, text("Play the clocktower song to the snowman."))));
        daily05.addMusicTag(Note.Tone.A)
            .addMusicTag(Note.Tone.D)
            .addMusicTag(Note.Tone.F)
            .addMusicTag(Note.Tone.A)
            .addMusicTag(Note.Tone.D)
            .addMusicTag(Note.Tone.F);
        setDaily(5, daily05);
        // 06
        final Color pink = Color.fromRGB(0xFF69B4);
        final AdventDailyCollectItems daily06 = new AdventDailyCollectItems("advent_2024_01", Vec3i.of(262, 67, 255),
                                                                            Items.colorized(Mytems.C_NOTE.createItemStack(), pink),
                                                                            pink);
        daily06.addItemLocation(262, 67, 255) // near spawn
            .addItemLocation(249, 100, 228) // clock tower
            .addItemLocation(249, 107, 232) // above clock tower
            .addItemLocation(245, 107, 228)
            .addItemLocation(249, 107, 224)
            .addItemLocation(253, 107, 228)
            .addItemLocation(239, 118, 307) // ice tower x 4
            .addItemLocation(230, 117, 299)
            .addItemLocation(222, 117, 307)
            .addItemLocation(230, 117, 316)
            .addItemLocation(366, 126, 232) // castle
            .addItemLocation(379, 136, 266) // bottom of climbable
            .addItemLocation(385, 156, 267) // top of climbable tower
            .addItemLocation(374, 154, 261) // directional hint
            .addItemLocation(362, 175, 248) // cactus
            .addItemLocation(371, 162, 236) // gold block
            .addItemLocation(377, 183, 241); // tallest  tower
        daily06.setDescription(List.of(textOfChildren(Mytems.C_NOTE, text("Find the 17 musical notes."))));
        setDaily(6, daily06);
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
        if (Advent.advent().isAdventServer()) {
            for (String worldName : adventWorldNames) {
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;
                // We want to change some game rules from the creative server.
                famPlugin().getLogger().info("[Advent] Changing game rules in world " + world.getName());
                world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, false);
                world.setGameRule(GameRule.NATURAL_REGENERATION, false);
                world.setGameRule(GameRule.MOB_GRIEFING, true);
                world.setDifficulty(Difficulty.NORMAL);
            }
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
