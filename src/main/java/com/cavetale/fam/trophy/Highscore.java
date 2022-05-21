package com.cavetale.fam.trophy;

import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.font.Glyph;
import com.cavetale.mytems.item.trophy.TrophyCategory;
import com.winthier.playercache.PlayerCache;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import static com.cavetale.core.font.Unicode.subscript;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class Highscore {
    public static final Highscore ZERO = new Highscore(new UUID(0L, 0L), 0);
    public final UUID uuid;
    public final int score;
    protected int placement;

    public Component name() {
        if (this == ZERO) return text("???", GRAY);
        Player player = Bukkit.getPlayer(uuid);
        return player != null
            ? player.displayName()
            : text(PlayerCache.nameForUuid(uuid), WHITE);
    }

    public Component sidebar() {
        return join(noSeparators(),
                    (placement > 0 ? Glyph.toComponent("" + placement) : Mytems.QUESTION_MARK.component),
                    text(subscript(score), WHITE),
                    Component.space(),
                    name());
    }

    public static List<Component> sidebar(List<Highscore> highscore) {
        List<Component> result = new ArrayList<>();
        for (int i = 0; i < 10; i += 1) {
            Highscore hi = i < highscore.size() ? highscore.get(i) : ZERO;
            result.add(hi.sidebar());
        }
        return result;
    }

    public static List<Highscore> of(Map<UUID, Integer> scoreMap) {
        List<Highscore> result = new ArrayList<>(scoreMap.size());
        for (Map.Entry<UUID, Integer> entry : scoreMap.entrySet()) {
            int value = entry.getValue();
            if (value == 0) continue;
            result.add(new Highscore(entry.getKey(), value));
        }
        Collections.sort(result, (a, b) -> Integer.compare(b.score, a.score));
        int lastScore = -1;
        int placement = 0;
        for (Highscore hi : result) {
            if (lastScore != hi.score) {
                lastScore = hi.score;
                placement += 1;
            }
            hi.placement = placement;
        }
        return result;
    }

    public static int reward(Map<UUID, Integer> scoreMap,
                             String category,
                             TrophyCategory trophyCategory,
                             Component title,
                             Function<Highscore, String> inscriptionMaker) {
        List<SQLTrophy> trophies = new ArrayList<>();
        for (Highscore hi : of(scoreMap)) {
            if (hi.score <= 0) break;
            trophies.add(new SQLTrophy(hi.uuid,
                                       category,
                                       hi.placement,
                                       trophyCategory,
                                       title,
                                       inscriptionMaker.apply(hi)));
        }
        if (trophies.isEmpty()) return 0;
        Trophies.insertTrophies(trophies);
        return trophies.size();
    }
}
