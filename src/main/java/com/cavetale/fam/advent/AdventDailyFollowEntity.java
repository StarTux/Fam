package com.cavetale.fam.advent;

import com.cavetale.core.struct.Vec3i;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Entities;
import java.util.ArrayList;
import java.util.List;
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

@Data
@EqualsAndHashCode(callSuper = true)
public final class AdventDailyFollowEntity extends AbstractAdventDaily {
    private final String worldName;
    private final Vec3i starLocation;
    private final Vec3i entityLocation;
    private final Function<Location, Mob> entitySpawner;
    private final List<Vec3i> entityGoals = new ArrayList<>();

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
        updateEntity(player, tag);
        if (!tag.goalReached) {
            pathEntity(player, tag);
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

    private void updateEntity(Player player, Tag tag) {
        if (tag.entity != null && !tag.entity.isDead()) return;
        final World world = getWorld();
        if (world == null || !world.isChunkLoaded(entityLocation.x >> 4, entityLocation.z >> 4)) {
            return;
        }
        if (world.getChunkAt(entityLocation.x >> 4, entityLocation.z >> 4).getLoadLevel() != Chunk.LoadLevel.ENTITY_TICKING) {
            return;
        }
        tag.entity = entitySpawner.apply(entityLocation.toCenterFloorLocation(world));
        tag.entity.setPersistent(false);
        Entities.setTransient(tag.entity);
        Bukkit.getMobGoals().removeAllGoals(tag.entity);
        tag.entity.setVisibleByDefault(false);
        player.showEntity(famPlugin(), tag.entity);
        tag.didPath = false;
        tag.goalIndex = 0;
    }

    private void pathEntity(Player player, Tag tag) {
        if (tag.entity == null || tag.entity.isDead()) return;
        if (tag.goalIndex >= entityGoals.size()) {
            tag.goalReached = true;
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
            return;
        }
        final Vec3i entityGoal = entityGoals.get(tag.goalIndex);
        if (tag.didPath) {
            if (Vec3i.of(tag.entity.getLocation()).maxDistance(entityGoal) <= 2) {
                tag.goalIndex += 1;
                tag.didPath = false;
                tag.entity.getPathfinder().stopPathfinding();
                famPlugin().getLogger().info("[AdventDailyFollowEntity] [" + player.getName() + "] new goal index " + tag.goalIndex);
            } else {
                tag.pathTimer += 1;
                if (tag.pathTimer >= 20) {
                    tag.entity.getPathfinder().moveTo(entityGoal.toCenterFloorLocation(tag.entity.getWorld()));
                    tag.pathTimer = 0;
                }
            }
        } else {
            if (Vec3i.of(player.getLocation()).maxDistance(Vec3i.of(tag.entity.getLocation())) <= 4) {
                if (!tag.entity.getPathfinder().moveTo(entityGoal.toCenterFloorLocation(tag.entity.getWorld()))) {
                    famPlugin().getLogger().severe("[AdventDailyFollowEntity] [" + player.getName() + "] Cannot path"
                                                 + " from " + Vec3i.of(tag.entity.getLocation())
                                                 + " to " + entityGoal);
                    return;
                }
                tag.soundTimer = 0;
                tag.didPath = true;
                famPlugin().getLogger().info("[AdventDailyFollowEntity] [" + player.getName() + "] new path to " + entityGoal);
            } else {
                tag.soundTimer += 1;
                if (tag.soundTimer >= 60) {
                    tag.soundTimer = 0;
                    player.playSound(tag.entity.getLocation(), tag.entity.getHurtSound(), 1f, 1f);
                    player.spawnParticle(Particle.NOTE, tag.entity.getEyeLocation().add(0.0, 1.0, 0.0), 1, 0.0, 0.0, 0.0, 0.0);
                    famPlugin().getLogger().info("[AdventDailyFollowEntity] [" + player.getName() + "] squeak");
                }
            }
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

    public void addEntityGoal(int x, int y, int z) {
        entityGoals.add(Vec3i.of(x, y, z));
    }

    @Override
    public Class<? extends AdventDailyTag> getTagClass() {
        return Tag.class;
    }

    static final class Tag extends AdventDailyTag {
        private transient Mob entity;
        private transient boolean didPath;
        private transient boolean goalReached;
        private transient ItemDisplayHolder starHolder;
        private transient int goalIndex;
        private transient int soundTimer;
        private transient int pathTimer;
    }
}
