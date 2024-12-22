package com.cavetale.fam.advent;

import com.cavetale.core.font.VanillaItems;
import com.cavetale.core.font.VanillaPaintings;
import com.cavetale.core.struct.Cuboid;
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
import org.bukkit.entity.Chicken;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Rabbit;
import static com.cavetale.fam.FamPlugin.famPlugin;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class AdventDailies {
    public static final String ADVENT_WORLD_1 = "advent_2024_01";
    public static final String ADVENT_WORLD_3 = "advent_2024_03";
    public static final String ADVENT_WORLD_4 = "advent_2024_04";
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
        final AdventDailyGetStar daily01 = new AdventDailyGetStar(ADVENT_WORLD_1, Vec3i.of(364, 127, 262));
        daily01.setDescription(List.of(textOfChildren(Mytems.STAR, text("Climb the gingerbread castle."))));
        setDaily(1, daily01);

        // 02
        final AdventDailyCollectItems daily02 = new AdventDailyCollectItems(ADVENT_WORLD_1, Vec3i.of(262, 67, 255),
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
        final AdventDailyKillMob daily03 = new AdventDailyKillMob(ADVENT_WORLD_1,
                                                                  Vec3i.of(347, 93, 254),
                                                                  EntityType.IRON_GOLEM, 1);
        daily03.setDescription(List.of(text("Defeat the Iron Golem.")));
        setDaily(3, daily03);
        // 04
        final AdventDailyGetStar daily04 = new AdventDailyGetStar(ADVENT_WORLD_1, Vec3i.of(377, 117, 253));
        daily04.setDescription(List.of(textOfChildren(text("Jump through the "),
                                                      VanillaPaintings.HUMBLE,
                                                      text(" painting."))));
        setDaily(4, daily04);
        // 05
        final AdventDailyPlayMusic daily05 = new AdventDailyPlayMusic(ADVENT_WORLD_1, Vec3i.of(301, 75, 231), EntityType.SNOW_GOLEM);
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
        final AdventDailyCollectItems daily06 = new AdventDailyCollectItems(ADVENT_WORLD_1, Vec3i.of(262, 67, 255),
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
        final AdventDailyFloatBoat daily07 = new AdventDailyFloatBoat(ADVENT_WORLD_1, Vec3i.of(374, 208, 253));
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
        final AdventDailyGetStar daily08 = new AdventDailyGetStar(ADVENT_WORLD_1, Vec3i.of(308, 88, 292));
        daily08.setDescription(List.of(textOfChildren(Mytems.STAR, text("Explore the ice palace."))));
        setDaily(8, daily08);

        // 09
        final AdventDailyShrinkStar daily09 = new AdventDailyShrinkStar(ADVENT_WORLD_1, Vec3i.of(376, 81, 267));
        daily09.setDescription(List.of(text("Fix the castle plumbing.")));
        daily09.addShrinkLocation(357, 79, 262);
        setDaily(9, daily09);

        // 10
        final AdventDailyFollowEntity daily10 = new AdventDailyFollowEntity(ADVENT_WORLD_1, Vec3i.of(369, 69, 288),
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
        final AdventDailyGetStar daily11 = new AdventDailyGetStar(ADVENT_WORLD_1, Vec3i.of(260, 105, 307));
        daily11.setDescription(List.of(text("Take a leap of faith.")));
        setDaily(11, daily11);

        // 12
        final AdventDailyGetStar daily12 = new AdventDailyGetStar(ADVENT_WORLD_1, Vec3i.of(343, 67, 239));
        daily12.setDescription(List.of(text("Find the little Christmas tree.")));
        setDaily(12, daily12);

        // 13
        final AdventDailyShrinkStar daily13 = new AdventDailyShrinkStar(ADVENT_WORLD_1, Vec3i.of(381, 77, 250));
        daily13.setDescription(List.of(text("Become a sewer rat.")));
        daily13.addShrinkLocation(357, 79, 262);
        daily13.addShrinkLocation(378, 79, 257);
        setDaily(13, daily13);

        // 14
        final AdventDailyGetStar daily14 = new AdventDailyGetStar(ADVENT_WORLD_1, Vec3i.of(249, 93, 230));
        daily14.setDescription(List.of(text("Fix the clockwork.")));
        setDaily(14, daily14);

        // 15
        final AdventDailyGetStar daily15 = new AdventDailyGetStar(ADVENT_WORLD_1, Vec3i.of(363, 68, 217));
        daily15.setDescription(List.of(text("Walk through the chimney.")));
        setDaily(15, daily15);

        // 16
        final AdventDailyGetStar daily16 = new AdventDailyGetStar(ADVENT_WORLD_1, Vec3i.of(368, 60, 246));
        daily16.setDescription(List.of(text("Dive in the sewers.")));
        setDaily(16, daily16);

        // 17
        final AdventDailyKillMob daily17 = new AdventDailyKillMob(ADVENT_WORLD_1,
                                                                  Vec3i.of(308, 67, 299),
                                                                  EntityType.STRAY, 4);
        daily17.setDescription(List.of(text("Explore the cellar of the Ice Palace.")));
        setDaily(17, daily17);

        // 18
        final AdventDailyCollectItems daily18 = new AdventDailyCollectItems(ADVENT_WORLD_3, Vec3i.of(128, 67, 126),
                                                                            Mytems.KITTY_COIN.createItemStack(),
                                                                            Color.YELLOW);
        daily18.addItemLocation(215, 68, 126);
        daily18.addItemLocation(191, 68, 185);
        daily18.addItemLocation(125, 74, 214); // Ladder
        daily18.addItemLocation(7, 69, 118);
        daily18.addItemLocation(66, 68, 52);
        daily18.addItemLocation(127, 66, 90); // Lake
        daily18.addItemLocation(131, 66, 145); // Lake
        daily18.addItemLocation(198, 65, 50); // Underground
        daily18.setDescription(List.of(textOfChildren(Mytems.KITTY_COIN, text("Find the eight Kitty Coins."))));
        setDaily(18, daily18);

        // 19
        final AdventDailyGetStar daily19 = new AdventDailyGetStar(ADVENT_WORLD_3, Vec3i.of(127, 62, 118));
        daily19.setDescription(List.of(textOfChildren(Mytems.STAR, text("Dive under the ice."))));
        setDaily(19, daily19);

        // 20
        final AdventDailyShrinkStar daily20 = new AdventDailyShrinkStar(ADVENT_WORLD_3, Vec3i.of(33, 112, 44));
        daily20.setDescription(List.of(text("Climb the tallest tree.")));
        daily20.addShrinkLocation(34, 69, 41);
        setDaily(20, daily20);

        // 21
        final AdventDailyGetStar daily21 = new AdventDailyGetStar(ADVENT_WORLD_4, Vec3i.of(271, 71, 262));
        daily21.setBoatRequired(true);
        daily21.setDescription(List.of(text("Slip slidin' away through the chimney.")));
        setDaily(21, daily21);

        // 22
        final AdventDailyCollectItems daily22 = new AdventDailyCollectItems(ADVENT_WORLD_4, Vec3i.of(345, 135, 231),
                                                                            Mytems.KITTY_COIN.createItemStack(), null);
        daily22.setDescription(List.of(textOfChildren(Mytems.KITTY_COIN, text("Collect the 25 Kitty Coins."))));
        daily22.addItemLocation(263, 184, 266); // Backwards
        daily22.addItemLocation(260, 186, 259); // Staircase
        daily22.addItemLocation(252, 190, 259);
        daily22.addItemLocation(233, 190, 262);
        daily22.addItemLocation(214, 189, 263); // Snowman
        daily22.addItemLocation(214, 182, 247);
        daily22.addItemLocation(205, 177, 207);
        daily22.addItemLocation(200, 171, 161);
        daily22.addItemLocation(173, 170, 151);
        daily22.addItemLocation(178, 165, 175);
        daily22.addItemLocation(178, 160, 195);
        daily22.addItemLocation(172, 154, 219);
        daily22.addItemLocation(181, 146, 251);
        daily22.addItemLocation(178, 139, 279);
        daily22.addItemLocation(178, 137, 293);
        daily22.addItemLocation(191, 137, 292); // Snowman bottom
        daily22.addItemLocation(205, 132, 305);
        daily22.addItemLocation(225, 127, 307); // Air bridge start
        daily22.addItemLocation(226, 133, 286);
        daily22.addItemLocation(251, 134, 286);
        daily22.addItemLocation(270, 133, 286);
        daily22.addItemLocation(317, 133, 286);
        daily22.addItemLocation(341, 133, 274);
        daily22.addItemLocation(348, 133, 271);
        daily22.addItemLocation(348, 133, 251); // Bridge
        setDaily(22, daily22);

        // 23
        final AdventDailyEscortEntity daily23 = new AdventDailyEscortEntity(ADVENT_WORLD_4, Vec3i.of(274, 72, 301),
                                                                            Vec3i.of(246, 192, 261),
                                                                            new Cuboid(272, 69, 294,
                                                                                       276, 77, 298),
                                                                            location -> location.getWorld().spawn(location, Chicken.class));
        daily23.setDescription(List.of(textOfChildren(Mytems.CHICKEN_FACE, text("Reunite the chicken and mother penguin."))));
        setDaily(23, daily23);

        // 24
        final AdventDailyCollectItems daily24 = new AdventDailyCollectItems(ADVENT_WORLD_4, Vec3i.of(265, 72, 268),
                                                                            Mytems.GREEN_MOON.createItemStack(), null);
        daily24.setDescription(List.of(textOfChildren(Mytems.GREEN_MOON, text("Collect 78 moons on the slippin' slide."))));
        daily24.setBoatRequired(true);
        // First slide
        daily24.addItemLocation(244, 173, 253);
        daily24.addItemLocation(244, 172, 245);
        daily24.addItemLocation(244, 171, 237);
        daily24.addItemLocation(244, 170, 229);
        daily24.addItemLocation(244, 169, 221);
        daily24.addItemLocation(244, 168, 213);
        daily24.addItemLocation(244, 167, 205);
        daily24.addItemLocation(244, 166, 197);
        daily24.addItemLocation(244, 165, 188);
        // First turn
        daily24.addItemLocation(250, 163, 178);
        daily24.addItemLocation(262, 161, 174);
        daily24.addItemLocation(267, 161, 175);
        daily24.addItemLocation(276, 159, 187);
        // 2nd slope
        daily24.addItemLocation(277, 157, 198);
        daily24.addItemLocation(277, 154, 209);
        daily24.addItemLocation(277, 150, 225);
        // Turn left
        daily24.addItemLocation(257, 145, 239);
        daily24.addItemLocation(247, 143, 242);
        daily24.addItemLocation(242, 141, 250);
        daily24.addItemLocation(244, 140, 258);
        daily24.addItemLocation(252, 139, 262);
        daily24.addItemLocation(261, 137, 264);
        daily24.addItemLocation(265, 136, 267);
        daily24.addItemLocation(273, 134, 264);
        // Into hidden tunnel
        daily24.addItemLocation(281, 132, 261);
        daily24.addItemLocation(284, 131, 258);
        daily24.addItemLocation(286, 131, 256);
        daily24.addItemLocation(288, 131, 255);
        daily24.addItemLocation(290, 131, 255);
        // Secret tunnel
        daily24.addItemLocation(299, 127, 254);
        daily24.addItemLocation(317, 118, 254);
        daily24.addItemLocation(328, 113, 257);
        daily24.addItemLocation(324, 108, 271);
        daily24.addItemLocation(314, 103, 263);
        daily24.addItemLocation(304, 99, 258);
        daily24.addItemLocation(293, 94, 258);
        daily24.addItemLocation(285, 90, 258);
        daily24.addItemLocation(276, 85, 258);
        daily24.addItemLocation(269, 82, 259);
        daily24.addItemLocation(269, 82, 258);
        // Past tunnel
        daily24.addItemLocation(286, 129, 243);
        daily24.addItemLocation(286, 128, 235);
        daily24.addItemLocation(286, 127, 227);
        daily24.addItemLocation(286, 126, 219);
        daily24.addItemLocation(286, 125, 211);
        // Turn
        daily24.addItemLocation(283, 124, 204);
        daily24.addItemLocation(275, 123, 204);
        daily24.addItemLocation(267, 122, 207);
        daily24.addItemLocation(259, 121, 209);
        daily24.addItemLocation(251, 120, 207);
        daily24.addItemLocation(246, 118, 198);
        daily24.addItemLocation(247, 117, 190);
        daily24.addItemLocation(246, 116, 182);
        daily24.addItemLocation(248, 115, 174);
        // Turn
        daily24.addItemLocation(262, 113, 175);
        daily24.addItemLocation(279, 111, 175);
        daily24.addItemLocation(302, 95, 174);
        daily24.addItemLocation(310, 94, 175);
        daily24.addItemLocation(318, 93, 175);
        // Turn
        daily24.addItemLocation(325, 91, 186);
        daily24.addItemLocation(325, 90, 194);
        daily24.addItemLocation(325, 89, 202);
        daily24.addItemLocation(326, 88, 210);
        daily24.addItemLocation(325, 87, 218);
        // Final tunnel
        daily24.addItemLocation(326, 84, 242);
        daily24.addItemLocation(325, 83, 250);
        daily24.addItemLocation(326, 82, 258);
        // Final turn
        daily24.addItemLocation(318, 80, 263);
        daily24.addItemLocation(310, 78, 263);
        daily24.addItemLocation(302, 76, 262);
        daily24.addItemLocation(294, 74, 263);
        daily24.addItemLocation(286, 72, 262);
        // Final bridge
        daily24.addItemLocation(282, 71, 263);
        daily24.addItemLocation(280, 71, 263);
        daily24.addItemLocation(278, 71, 263);
        daily24.addItemLocation(276, 71, 263);
        daily24.addItemLocation(274, 71, 263);
        daily24.addItemLocation(272, 71, 263);
        setDaily(24, daily24);

        // 25
        final AdventDailyGetStar daily25 = new AdventDailyGetStar(ADVENT_WORLD_4, Vec3i.of(233, 88, 290));
        daily25.setDescription(List.of(textOfChildren(Mytems.STAR, text("Land on the floating island."))));
        setDaily(25, daily25);

        // Finis
        for (int i = 0; i < dailies.size(); i += 1) {
            AdventDaily daily = dailies.get(i);
            switch (daily.getWorldName()) {
            case ADVENT_WORLD_1:
                daily.setWarp("Advent2024-01");
                break;
            case ADVENT_WORLD_3:
                daily.setWarp("Advent2024-03");
                break;
            case ADVENT_WORLD_4:
                daily.setWarp("Advent2024-04");
                break;
            default:
                break;
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
