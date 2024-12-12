package com.cavetale.fam.advent;

import com.cavetale.core.font.VanillaItems;
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
import org.bukkit.entity.Rabbit;
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
                                                                  EntityType.IRON_GOLEM, 1);
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

        // 07
        final AdventDailyFloatBoat daily07 = new AdventDailyFloatBoat("advent_2024_01", Vec3i.of(374, 208, 253));
        daily07.addBoatLocation(282, 82, 266)
            .addBoatLocation(264, 90, 212)
            .addBoatLocation(217, 95, 216)
            .addBoatLocation(196, 102, 271)
            .addBoatLocation(214, 102, 331)
            .addBoatLocation(259, 102, 323)
            .addBoatLocation(293, 128, 262)
            .addBoatLocation(346, 171, 223)
            .addBoatLocation(402, 200, 220)
            .addBoatLocation(412, 211, 259)
            .addBoatLocation(377, 219, 274)
            .addBoatLocation(372, 219, 247)
            .addBoatLocation(380, 212, 238);
        daily07.setDescription(List.of(textOfChildren(VanillaItems.OAK_BOAT, text("Go on a boat ride."))));
        setDaily(7, daily07);

        // 08
        final AdventDailyGetStar daily08 = new AdventDailyGetStar("advent_2024_01", Vec3i.of(308, 88, 292));
        daily08.setDescription(List.of(textOfChildren(Mytems.STAR, text("Explore the ice palace."))));
        setDaily(8, daily08);

        // 09
        final AdventDailyShrinkStar daily09 = new AdventDailyShrinkStar("advent_2024_01", Vec3i.of(376, 81, 267));
        daily09.setDescription(List.of(text("Fix the castle plumbing.")));
        daily09.addShrinkLocation(357, 79, 262);
        setDaily(9, daily09);

        // 10
        final AdventDailyFollowEntity daily10 = new AdventDailyFollowEntity("advent_2024_01", Vec3i.of(369, 69, 288),
                                                                            Vec3i.of(262, 66, 255),
                                                                            location -> location.getWorld().spawn(location, Rabbit.class, e -> {
                                                                                    e.setRabbitType(Rabbit.Type.WHITE);
                                                                                    e.setAdult();
                                                                                    e.setAgeLock(true);
                                                                                }));
        daily10.setDescription(List.of(textOfChildren(VanillaItems.CARROT, text("Follow the white rabbit."))));
        daily10.addEntityGoal(270, 65, 255);
        daily10.addEntityGoal(296, 65, 270);
        daily10.addEntityGoal(297, 65, 283);
        daily10.addEntityGoal(282, 65, 301);
        daily10.addEntityGoal(299, 65, 318);
        daily10.addEntityGoal(324, 65, 316);
        daily10.addEntityGoal(331, 65, 299);
        daily10.addEntityGoal(324, 65, 285);
        daily10.addEntityGoal(320, 65, 252);
        daily10.addEntityGoal(326, 65, 231);
        daily10.addEntityGoal(356, 65, 211);
        daily10.addEntityGoal(390, 65, 215);
        daily10.addEntityGoal(401, 65, 232);
        daily10.addEntityGoal(404, 65, 255);
        daily10.addEntityGoal(401, 65, 282);
        daily10.addEntityGoal(385, 65, 302);
        daily10.addEntityGoal(369, 65, 297);
        daily10.addEntityGoal(369, 67, 291);
        setDaily(10, daily10);

        // 11
        final AdventDailyGetStar daily11 = new AdventDailyGetStar("advent_2024_01", Vec3i.of(260, 105, 307));
        daily11.setDescription(List.of(text("Take a leap of faith.")));
        setDaily(11, daily11);

        // 12
        final AdventDailyGetStar daily12 = new AdventDailyGetStar("advent_2024_01", Vec3i.of(343, 67, 239));
        daily12.setDescription(List.of(text("Find the little Christmas tree.")));
        setDaily(12, daily12);

        // 13
        final AdventDailyShrinkStar daily13 = new AdventDailyShrinkStar("advent_2024_01", Vec3i.of(381, 77, 250));
        daily13.setDescription(List.of(text("Become a sewer rat.")));
        daily13.addShrinkLocation(357, 79, 262);
        daily13.addShrinkLocation(378, 79, 257);
        setDaily(13, daily13);

        // 14
        final AdventDailyGetStar daily14 = new AdventDailyGetStar("advent_2024_01", Vec3i.of(249, 93, 230));
        daily14.setDescription(List.of(text("Fix the clockwork.")));
        setDaily(14, daily14);

        // 15
        final AdventDailyGetStar daily15 = new AdventDailyGetStar("advent_2024_01", Vec3i.of(363, 68, 217));
        daily15.setDescription(List.of(text("Walk through the chimney.")));
        setDaily(15, daily15);

        // 16
        final AdventDailyGetStar daily16 = new AdventDailyGetStar("advent_2024_01", Vec3i.of(368, 60, 246));
        daily16.setDescription(List.of(text("Dive in the sewers.")));
        setDaily(16, daily16);

        // 17
        final AdventDailyKillMob daily17 = new AdventDailyKillMob("advent_2024_01",
                                                                  Vec3i.of(308, 67, 299),
                                                                  EntityType.STRAY, 4);
        daily17.setDescription(List.of(text("Explore the cellar of the Ice Palace.")));
        setDaily(17, daily17);

        // Finis
        for (int i = 0; i < dailies.size(); i += 1) {
            AdventDaily daily = dailies.get(i);
            if (i < 14) {
                daily.setWarp("Advent2024-01");
            } else {
                daily.setWarp("Advent2024-02");
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
