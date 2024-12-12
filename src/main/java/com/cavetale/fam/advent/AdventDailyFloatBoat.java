package com.cavetale.fam.advent;

import com.cavetale.core.struct.Vec3i;
import com.cavetale.mytems.Mytems;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.entity.boat.OakBoat;
import org.bukkit.util.Vector;
import static com.cavetale.fam.FamPlugin.famPlugin;

@Data
@EqualsAndHashCode(callSuper = true)
public final class AdventDailyFloatBoat extends AbstractAdventDaily {
    private final String worldName;
    private final Vec3i starLocation;
    private final List<Vec3i> boatLocations = new ArrayList<>();

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
        tag.tickBoat(player);
        if (!tag.goalReached) {
            if (!(player.getVehicle() instanceof Boat boat)) {
                tag.boatIndex = 0;
            } else if (tag.boatIndex >= boatLocations.size()) {
                tag.goalReached = true;
                session.save(null);
                boat.eject();
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
            } else {
                final Vec3i boatVector = boatLocations.get(tag.boatIndex);
                final Location goalLocation = boatVector.toCenterLocation(player.getWorld());
                final Location playerLocation = boat.getLocation();
                final Location direction = goalLocation.subtract(playerLocation);
                final Vector velocity = boat.getVelocity();
                // Speed increase and maxima, horizontal and vertical.
                final double maxH = 0.25;
                final double maxV = 0.5;
                final double newX = direction.getX() / 20.0;
                final double newY = direction.getY() / 20.0;
                final double newZ = direction.getZ() / 20.0;
                boat.setVelocity(new Vector((newX > 0 ? Math.min(newX, maxH) : Math.max(newX, -maxH)),
                                            (newY > 0 ? Math.min(newY, maxV) : Math.max(newY, -maxV)),
                                            (newZ > 0 ? Math.min(newZ, maxH) : Math.max(newZ, -maxH))));
                if (Vec3i.of(playerLocation).maxDistance(boatVector) <= 2) {
                    tag.boatIndex += 1;
                }
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
        if (tag.ridingBoat != null) {
            tag.ridingBoat.remove();
            tag.ridingBoat = null;
        }
    }

    @Override
    public Class<? extends AdventDailyTag> getTagClass() {
        return Tag.class;
    }

    public AdventDailyFloatBoat addBoatLocation(int x, int y, int z) {
        boatLocations.add(Vec3i.of(x, y, z));
        return this;
    }

    static final class Tag extends AdventDailyTag {
        private transient ItemDisplayHolder starHolder;
        private transient int boatIndex;
        private transient boolean goalReached;
        private transient OakBoat ridingBoat;
        private transient int ridingBoatTicks;

        private void setBoat(OakBoat boat) {
            if (ridingBoat != null) {
                ridingBoat.remove();
            }
            ridingBoat = boat;
            ridingBoatTicks = 0;
        }

        private void tickBoat(Player player) {
            if (ridingBoat == null) return;
            if (ridingBoat.getPassengers().isEmpty()) {
                ridingBoat.remove();
                ridingBoat = null;
                player.stopSound(Sound.ITEM_ELYTRA_FLYING);
            } else {
                if (ridingBoatTicks++ % 80 == 0) {
                    player.playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, 0.25f, 2.0f);
                }
            }
        }
    }

    public void onClickBoat(Player player, AdventSession session, OakBoat boat) {
        if (!(session.getTag() instanceof Tag tag)) return;
        if (tag.goalReached) return;
        Bukkit.getScheduler().runTask(famPlugin(), () -> {
                final OakBoat boatCopy = player.getWorld().spawn(boat.getLocation().add(0.0, 1.0, 0.0), OakBoat.class, e -> {
                        e.setPersistent(false);
                    });
                tag.setBoat(boatCopy);
                boatCopy.addPassenger(player);
            });
    }
}
