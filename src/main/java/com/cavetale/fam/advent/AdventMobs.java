package com.cavetale.fam.advent;

import com.cavetale.core.struct.Vec3i;
import com.cavetale.mytems.util.Entities;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Mob;
import org.bukkit.entity.PufferFish;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Stray;
import org.bukkit.entity.boat.OakBoat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import static com.cavetale.fam.FamPlugin.famPlugin;

/**
 * Make sure that mobs are (re)spawned.
 */
public final class AdventMobs implements Listener {
    private final List<AdventMob> mobs = new ArrayList<>();

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, famPlugin());
        Bukkit.getScheduler().runTaskTimer(famPlugin(), this::tick, 1L, 1L);
        mobs.add(new AdventMob("advent_2024_01", // under first bridge
                               Vec3i.of(286, 65, 257),
                               this::spawnCreeper));
        mobs.add(new AdventMob("advent_2024_01", // first mountain pass
                               Vec3i.of(330, 73, 248),
                               this::spawnCreeper));
        mobs.add(new AdventMob("advent_2024_01", // straight from spawn to the right
                               Vec3i.of(310, 65, 275),
                               this::spawnCreeper));
        mobs.add(new AdventMob("advent_2024_01", // under 2nd bridge
                               Vec3i.of(325, 65, 235),
                               this::spawnCreeper));
        mobs.add(new AdventMob("advent_2024_01", // near spawn to the left
                               Vec3i.of(275, 65, 224),
                               this::spawnCreeper));
        mobs.add(new AdventMob("advent_2024_01", // behind spawn
                               Vec3i.of(210, 65, 253),
                               this::spawnCreeper));
        mobs.add(new AdventMob("advent_2024_01", // sewer
                               Vec3i.of(342, 66, 262),
                               this::spawnCreeper));
        mobs.add(new AdventMob("advent_2024_01", // behind ice palace
                               Vec3i.of(309, 65, 318),
                               this::spawnCreeper));

        mobs.add(new AdventMob("advent_2024_01", // under 2nd bridge
                               Vec3i.of(339, 90, 254),
                               location -> location.getWorld().spawn(location, IronGolem.class, e -> {
                                       e.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.125);
                                       e.getAttribute(Attribute.SCALE).setBaseValue(2.0);
                                       e.setHealth(3.0);
                                   })));
        mobs.add(new AdventMob("advent_2024_01",
                               Vec3i.of(297, 73, 232),
                               location -> location.getWorld().spawn(location, Snowman.class, e -> {
                                       e.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.0);
                                       e.setCollidable(false);
                                   })));

        mobs.add(new AdventMob("advent_2024_01",
                               Vec3i.of(287, 73, 269),
                               location -> location.getWorld().spawn(location, OakBoat.class)));

        mobs.add(new AdventMob("advent_2024_01", Vec3i.of(384, 79, 262),
                               location -> location.getWorld().spawn(location, Silverfish.class, e -> {
                                       e.setHealth(1.0);
                                   })));
        mobs.add(new AdventMob("advent_2024_01", Vec3i.of(384, 79, 268),
                               location -> location.getWorld().spawn(location, Endermite.class, e -> {
                                       e.setHealth(1.0);
                                   })));

        // Sewer pufferfish
        mobs.add(new AdventMob("advent_2024_01", Vec3i.of(368, 60, 257),
                               location -> location.getWorld().spawn(location, PufferFish.class, e -> e.setHealth(1.0))));
        mobs.add(new AdventMob("advent_2024_01", Vec3i.of(351, 60, 233),
                               location -> location.getWorld().spawn(location, PufferFish.class, e -> e.setHealth(1.0))));

        // Ice Palace Basement
        mobs.add(new AdventMob("advent_2024_01", Vec3i.of(308, 65, 307),
                               location -> location.getWorld().spawn(location, Stray.class, e -> {
                                       e.getEquipment().setItemInMainHand(null);
                                       e.setHealth(1.0);
                                       e.setSilent(true);
                                   })));
        mobs.add(new AdventMob("advent_2024_01", Vec3i.of(301, 65, 299),
                               location -> location.getWorld().spawn(location, Stray.class, e -> {
                                       e.getEquipment().setItemInMainHand(null);
                                       e.setHealth(1.0);
                                       e.setSilent(true);
                                   })));
        mobs.add(new AdventMob("advent_2024_01", Vec3i.of(308, 65, 292),
                               location -> location.getWorld().spawn(location, Stray.class, e -> {
                                       e.getEquipment().setItemInMainHand(null);
                                       e.setHealth(1.0);
                                       e.setSilent(true);
                                   })));
        mobs.add(new AdventMob("advent_2024_01", Vec3i.of(315, 65, 299),
                               location -> location.getWorld().spawn(location, Stray.class, e -> {
                                       e.getEquipment().setItemInMainHand(null);
                                       e.setHealth(1.0);
                                       e.setSilent(true);
                                   })));
    }

    public void disable() {
        for (AdventMob it : mobs) {
            it.remove();
        }
    }

    private void tick() {
        for (AdventMob it : mobs) {
            it.tick();
        }
    }

    @EventHandler
    private void onEntityDeath(EntityDeathEvent event) {
        died(event.getEntity().getUniqueId());
    }

    @EventHandler
    private void onEntityExplode(EntityExplodeEvent event) {
        died(event.getEntity().getUniqueId());
        event.blockList().clear();
    }

    private void died(UUID uuid) {
        for (AdventMob it : mobs) {
            if (it.mob != null && it.mob.getUniqueId().equals(uuid)) {
                it.died();
            }
        }
    }

    @RequiredArgsConstructor
    private static final class AdventMob {
        private final String worldName;
        private final Vec3i spawnVector;
        private final Function<Location, Entity> spawnFunction;
        private Entity mob;
        private boolean dead;
        private int deathTimer;

        private void died() {
            if (mob == null) return;
            famPlugin().getLogger().info("Died " + mob.getType() + " from " + worldName + " " + spawnVector);
            mob = null;
            dead = true;
            deathTimer = 0;
        }

        private void tick() {
            if (mob != null && mob.isDead()) {
                mob = null;
            }
            final World world = Bukkit.getWorld(worldName);
            if (world == null) return;
            if (dead) {
                deathTimer += 1;
                if (deathTimer >= 20 * 60) {
                    dead = false;
                } else if (deathTimer >= 20 * 50) {
                    world.spawnParticle(Particle.FLAME, spawnVector.toCenterLocation(world), 4, 0.25, 0.25, 0.25, 0.05);
                }
            } else if (mob == null) {
                if (!world.isChunkLoaded(spawnVector.x >> 4, spawnVector.z >> 4)) return;
                if (world.getChunkAt(spawnVector.x >> 4, spawnVector.z >> 4).getLoadLevel() != Chunk.LoadLevel.ENTITY_TICKING) {
                    return;
                }
                famPlugin().getLogger().info("Spawning mob at " + worldName + " " + spawnVector);
                mob = spawnFunction.apply(spawnVector.toCenterFloorLocation(world));
                if (mob == null) return;
                mob.setPersistent(false);
                Entities.setTransient(mob);
                if (mob instanceof Mob m) {
                    m.setRemoveWhenFarAway(false);
                }
            }
        }

        private void remove() {
            if (mob == null) return;
            mob.remove();
            mob = null;
        }
    }

    private Entity spawnCreeper(Location location) {
        return location.getWorld().spawn(location, Creeper.class, e -> {
                e.setHealth(3);
            });
    }
}
