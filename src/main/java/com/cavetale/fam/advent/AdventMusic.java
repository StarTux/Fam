package com.cavetale.fam.advent;

import com.cavetale.mytems.item.music.Melody;
import com.cavetale.mytems.item.music.Semitone;
import java.util.Map;
import org.bukkit.Instrument;
import org.bukkit.entity.Player;
import static com.cavetale.fam.FamPlugin.famPlugin;
import static org.bukkit.Note.Tone.*;

public final class AdventMusic {
    private static Melody deckTheHalls;
    private static Melody grinch;

    public static void deckTheHalls(Player player) {
        deckTheHalls.play(famPlugin(), player);
    }

    public static void grinch(Player player) {
        grinch.play(famPlugin(), player);
    }

    public static void enable() {
        deckTheHalls = Melody
            .builder(Instrument.BELL, 50L)
            .keys(Map.of(B, Semitone.FLAT))
            .beat(6, C, 1)
            .beat(2, B, 1)
            .beat(4, A, 1)
            .beat(4, G, 1)
            .beat(4, F, 0)
            .beat(4, G, 1)
            .beat(4, A, 1)
            .beat(4, F, 0)
            .beat(2, G, 1)
            .beat(2, A, 1)
            .beat(2, B, 1)
            .beat(2, G, 1)
            .beat(6, A, 1)
            .beat(2, G, 1)
            .beat(4, F, 0)
            .beat(4, E, 0)
            .beat(8, F, 0)
            .extra(Instrument.CHIME)
            .beat(0, A, 1).beat(6, C, 1)
            .beat(0, G, 1).beat(2, B, 1)
            .beat(0, F, 0).beat(4, A, 1)
            .beat(0, C, 0).beat(4, G, 1)
            .beat(0, A, 0).beat(4, F, 0)
            .beat(0, C, 0).beat(4, G, 1)
            .beat(0, F, 0).beat(4, A, 1)
            .beat(0, A, 0).beat(4, F, 0)
            .beat(0, D, 0).beat(2, G, 1)
            .beat(2, A, 1)
            .beat(0, D, 0).beat(2, B, 1)
            .beat(2, G, 1)
            .beat(0, F, 0).beat(0, C, 0).beat(4, A, 1)
            .beat(2, D, 0)
            .beat(2, G, 1)
            .beat(0, A, 0).beat(4, F, 0)
            .beat(0, C, 0).beat(2, E, 0)
            .beat(2, B, 0)
            .beat(0, A, 0).beat(8, F, 0)
            .parent().extra(Instrument.GUITAR)
            .beat(4, F, 1).beat(4, F, 1).beat(4, F, 1).beat(4, C, 1)
            .beat(4, F, 1).beat(4, C, 1).beat(8, F, 1)
            .beat(4, B, 1).beat(4, G, 1).beat(4, C, 1).beat(4, B, 1)
            .beat(4, C, 1).beat(4, C, 0).beat(8, F, 1)
            .parent()
            .build();
        grinch = Melody
            .builder(Instrument.DIDGERIDOO, 100L)
            .keys(Map.of())
            .beat(0, F, Semitone.SHARP, 0).beat(2, F, Semitone.SHARP, 1)
            .beat(0, G, 0).beat(2, G, 1)
            .beat(0, G, Semitone.SHARP, 0).beat(2, G, Semitone.SHARP, 1)
            .beat(0, A, 1).beat(2, A, 0)
            .build();
    }

    private AdventMusic() { }
}
