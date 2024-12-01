package com.cavetale.fam.advent;

import com.cavetale.core.bungee.Bungee;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.mytems.Mytems;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

@Data
@EqualsAndHashCode(callSuper = true)
public final class AdventDailyGetStar extends AbstractAdventDaily {
    private final String worldName;
    private final Vec3i starLocation;

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
    }

    @Override
    public void tick(AdventSession session) {
        Tag tag = (Tag) session.getTag();
        final Player player = session.getPlayer();
        if (!tag.hasStar) {
            tag.starHolder.update(player);
            if (Vec3i.of(player.getLocation()).maxDistance(starLocation) < 2) {
                tag.hasStar = true;
                session.save(null);
                tag.starHolder.remove();
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.75f);
            }
        } else if (!tag.complete) {
            tag.hasStarTicks += 1;
            player.spawnParticle(Particle.DUST, starLocation.toCenterLocation(player.getWorld()),
                                 16, 1.0, 1.0, 1.0, 0.125,
                                 new Particle.DustOptions(Color.YELLOW, 1f));
            if (tag.hasStarTicks > 200) {
                tag.complete = true;
                session.stopDaily();
                session.save(null);
                Advent.unlock(player.getUniqueId(), Advent.THIS_YEAR, getDay(), result -> {
                        Bungee.send(player, "hub");
                    });
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
        private boolean hasStar;
        private transient int hasStarTicks = 0;
        private transient boolean complete;
    }
}
