package com.cavetale.fam.advent;

import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Entities;
import java.util.function.Function;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import static com.cavetale.fam.FamPlugin.famPlugin;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Data
@EqualsAndHashCode(callSuper = true)
public final class AdventDailyEscortEntity extends AbstractAdventDaily {
    private final String worldName;
    private final Vec3i starLocation;
    private final Vec3i entityVector;
    private final Cuboid entityGoal;
    private final Function<Location, Mob> entitySpawner;

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
        if (!tag.goalReached) {
            if (tag.entity == null) {
                updateEntity(player, tag);
            }
            pathEntity(player, tag);
        } else {
            if (tag.entity != null) {
                tag.entity.remove();
                tag.entity = null;
            }
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

    private void updateEntity(Player player, Tag tag) {
        if (tag.entity != null && !tag.entity.isDead()) return;
        final World world = getWorld();
        if (world == null || !world.isChunkLoaded(entityVector.x >> 4, entityVector.z >> 4)) {
            return;
        }
        if (world.getChunkAt(entityVector.x >> 4, entityVector.z >> 4).getLoadLevel() != Chunk.LoadLevel.ENTITY_TICKING) {
            return;
        }
        tag.didFollow = false;
        tag.entity = entitySpawner.apply(entityVector.toCenterFloorLocation(world));
        tag.entity.setPersistent(false);
        Entities.setTransient(tag.entity);
        Bukkit.getMobGoals().removeAllGoals(tag.entity);
        tag.entity.setVisibleByDefault(false);
        player.showEntity(famPlugin(), tag.entity);
    }

    private void pathEntity(Player player, Tag tag) {
        if (tag.entity == null || tag.entity.isDead()) return;
        final Location entityLocation = tag.entity.getLocation();
        if (entityGoal.contains(entityLocation)) {
            tag.goalReached = true;
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
            return;
        }
        final int distance = Vec3i.of(player.getLocation()).maxDistance(Vec3i.of(entityLocation));
        if (distance > 32) {
            if (entityVector.maxDistance(Vec3i.of(entityLocation)) < 2) {
                return;
            }
            tag.entity.remove();
            tag.entity = null;
            if (tag.didFollow) {
                player.sendMessage(text("Back to the start", RED));
                tag.didFollow = false;
            }
            AdventMusic.grinch(player);
        }
        if (distance > 8) {
            tag.soundTimer += 1;
            if (tag.soundTimer >= 60) {
                tag.soundTimer = 0;
                player.playSound(entityLocation, tag.entity.getHurtSound(), 1f, 1f);
                player.spawnParticle(Particle.NOTE, tag.entity.getEyeLocation().add(0.0, 1.0, 0.0), 1, 0.0, 0.0, 0.0, 0.0);
                famPlugin().getLogger().info("[AdventDailyFollowEntity] [" + player.getName() + "] squeak");
            }
            return;
        }
        if (tag.pathTimer > 20) {
            tag.pathTimer -= 1;
            return;
        } else {
            final Location playerLocation = player.getLocation();
            if (tag.entity.getPathfinder().moveTo(playerLocation)) {
                tag.didFollow = true;
            }
            tag.pathTimer = 20;
        }
    }

    @Override
    public void unload(AdventSession session) {
        Tag tag = (Tag) session.getTag();
        tag.starHolder.remove();
        if (tag.entity != null) {
            tag.entity.remove();
            tag.entity = null;
        }
    }

    @Override
    public Class<? extends AdventDailyTag> getTagClass() {
        return Tag.class;
    }

    static final class Tag extends AdventDailyTag {
        private transient Mob entity;
        private transient boolean goalReached;
        private transient ItemDisplayHolder starHolder;
        private transient int soundTimer;
        private transient int pathTimer;
        private transient boolean didFollow;
    }
}
