package com.cavetale.fam.trophy;

import com.cavetale.core.money.Money;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.font.Glyph;
import com.cavetale.mytems.item.trophy.TrophyCategory;
import com.cavetale.mytems.item.trophy.TrophyType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;
import static com.cavetale.core.font.Unicode.subscript;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Getter @RequiredArgsConstructor
public final class Highscore {
    public static final Highscore ZERO = new Highscore(new UUID(0L, 0L), 0);
    public final UUID uuid;
    public final int score;
    protected int placement;

    public Component name() {
        return this == ZERO
            ? text("???", GRAY)
            : text(PlayerCache.nameForUuid(uuid));
    }

    public Component sidebar() {
        return join(noSeparators(),
                    (placement > 0 ? Glyph.toComponent("" + placement) : Mytems.QUESTION_MARK.component),
                    text(subscript(score), WHITE),
                    space(),
                    name());
    }

    public Component sidebar(TrophyCategory trophyCategory) {
        List<TrophyType> types = TrophyType.of(trophyCategory);
        final TrophyType trophyType = placement > 0 && !types.isEmpty()
            ? types.get(Math.min(placement, types.size()) - 1)
            : null;
        return join(noSeparators(),
                    (placement > 0 ? Glyph.toComponent("" + placement) : Mytems.QUESTION_MARK.component),
                    text(subscript(score), WHITE),
                    (trophyType != null ? trophyType : space()),
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

    public static List<Component> sidebar(List<Highscore> highscore, TrophyCategory trophyCategory) {
        List<Component> result = new ArrayList<>();
        for (int i = 0; i < 10; i += 1) {
            Highscore hi = i < highscore.size() ? highscore.get(i) : ZERO;
            result.add(hi.sidebar(trophyCategory));
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
                             Function<Highscore, String> inscriptionMaker,
                             Consumer<SQLTrophy> editor) {
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
        if (editor != null) {
            for (SQLTrophy trophy : trophies) {
                editor.accept(trophy);
            }
        }
        Trophies.insertTrophies(trophies);
        return trophies.size();
    }

    public static int reward(Map<UUID, Integer> scoreMap,
                             String category,
                             TrophyCategory trophyCategory,
                             Component title,
                             Function<Highscore, String> inscriptionMaker) {
        return reward(scoreMap, category, trophyCategory, title, inscriptionMaker, null);
    }

    public static Map<Integer, List<UUID>> rewardMoney(Plugin plugin, List<Highscore> list, String message) {
        final List<Integer> rewards = List.of(1_000_000,
                                              500_000,
                                              250_000);
        final List<List> ranks = List.of(new ArrayList<>(),
                                         new ArrayList<>(),
                                         new ArrayList<>());
        for (Highscore hi : list) {
            if (hi.placement > 2) continue;
            ranks.get(hi.placement - 1).add(hi);
        }
        final Map<Integer, List<UUID>> result = new LinkedHashMap<>();
        for (int rankIndex = 0; rankIndex < ranks.size(); rankIndex += 1) {
            final List<Highscore> rank = ranks.get(rankIndex);
            final int rewardSum = rank.isEmpty()
                ? rewards.get(rankIndex)
                : rewards.get(rankIndex) / rank.size();
            final List<UUID> resultList = new ArrayList<>(rank.size());
            result.put(rewardSum, resultList);
            for (Highscore hi : rank) {
                resultList.add(hi.uuid);
                plugin.getLogger().info("Giving " + PlayerCache.nameForUuid(hi.uuid) + " " + Money.get().format(rewardSum));
                Money.get().give(hi.uuid, (double) rewardSum, plugin, message);
            }
        }
        return result;
    }

    public static List<Component> rewardMoneyWithFeedback(Plugin plugin, Map<UUID, Integer> scoreMap, String message) {
        final Map<Integer, List<UUID>> map = rewardMoney(plugin, of(scoreMap), message);
        final List<Component> result = new ArrayList<>();
        for (Map.Entry<Integer, List<UUID>> rank : map.entrySet()) {
            result.add(Money.get().toComponent(rank.getKey()));
            for (UUID uuid : rank.getValue()) {
                result.add(text(" " + PlayerCache.nameForUuid(uuid), WHITE));
            }
        }
        return result;
    }
}
