package com.cavetale.fam.advent;

import com.cavetale.core.struct.Vec3i;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.music.Semitone;
import com.cavetale.mytems.item.music.Touch;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.bukkit.Color;
import org.bukkit.Note;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

@Data
@EqualsAndHashCode(callSuper = true)
public final class AdventDailyPlayMusic extends AbstractAdventDaily {
    private final String worldName;
    private final Vec3i starLocation;
    private final EntityType nearbyEntityType;
    private final List<MusicTag> musicTags = new ArrayList<>();

    @Override
    public void enable() {
    }

    @Override
    public void start(AdventSession session) {
        session.setTag(new Tag());
    }

    @Override
    public void load(AdventSession session) {
        Tag tag = (Tag) session.getTag();
        tag.starHolder = new ItemDisplayHolder(Mytems.STAR.createItemStack(),
                                               worldName,
                                               starLocation,
                                               Color.YELLOW);
        session.getPlayer().getInventory().addItem(Mytems.MUSICAL_BELL.createItemStack());
    }

    @Override
    public void tick(AdventSession session) {
        Tag tag = (Tag) session.getTag();
        final Player player = session.getPlayer();
        if (!tag.musicPlayed) {
            if (tag.musicIndex >= musicTags.size()) {
                tag.musicPlayed = true;
                session.save(null);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
                player.closeInventory();
            }
        } else {
            if (Vec3i.of(player.getLocation()).maxDistance(starLocation) < 2) {
                tag.starHolder.remove();
                session.stopDaily();
                session.save(null);
                Advent.unlock(player.getUniqueId(), Advent.THIS_YEAR, getDay(), result -> {
                        new AdventCelebration(player, worldName, starLocation, getDay()).start();
                    });
            } else {
                tag.starHolder.update(player);
            }
        }
    }

    @Override
    public void unload(AdventSession session) {
        Tag tag = (Tag) session.getTag();
        tag.starHolder.remove();
    }

    @Override
    public Class<? extends AdventDailyTag> getTagClass() {
        return Tag.class;
    }

    static final class Tag extends AdventDailyTag {
        private transient ItemDisplayHolder starHolder;
        private transient int musicIndex;
        private boolean musicPlayed;
    }

    @Value
    static final class MusicTag {
        private final Note.Tone tone;
        private final Semitone semitone;
    }

    public AdventDailyPlayMusic addMusicTag(Note.Tone tone, Semitone semitone) {
        musicTags.add(new MusicTag(tone, semitone));
        return this;
    }

    public AdventDailyPlayMusic addMusicTag(Note.Tone tone) {
        return addMusicTag(tone, Semitone.NATURAL);
    }

    public void onPlayTouch(Player player, Touch touch) {
        if (!(AdventSession.of(player).getTag() instanceof Tag tag)) {
            return;
        }
        if (tag.musicPlayed) return;
        if (tag.musicIndex >= musicTags.size()) return;
        if (nearbyEntityType != null) {
            boolean entityIsNearby = false;
            for (Entity nearby : player.getNearbyEntities(16, 16, 16)) {
                if (nearby.getType() == nearbyEntityType) {
                    entityIsNearby = true;
                    break;
                }
            }
            if (!entityIsNearby) return;
        }
        final MusicTag musicTag = musicTags.get(tag.musicIndex);
        if (touch.tone == musicTag.tone && touch.semitone == musicTag.semitone) {
            tag.musicIndex += 1;
        } else {
            tag.musicIndex = 0;
        }
    }
}
