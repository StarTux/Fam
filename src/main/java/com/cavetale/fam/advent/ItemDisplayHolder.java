package com.cavetale.fam.advent;

import com.cavetale.core.struct.Vec3i;
import com.cavetale.mytems.util.Entities;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.fam.FamPlugin.famPlugin;

@RequiredArgsConstructor
public final class ItemDisplayHolder {
    private final ItemStack itemStack;
    private final String worldName;
    private final Vec3i vector;
    private final Color glowColor;
    private ItemDisplay itemDisplay;

    public void update(Player player) {
        if (itemDisplay != null) {
            if (itemDisplay.isDead()) {
                itemDisplay = null;
            } else {
                final Location location = itemDisplay.getLocation();
                location.setYaw(location.getYaw() + 10.0f);
                itemDisplay.teleport(location);
            }
        } else {
            final World world = Bukkit.getWorld(worldName);
            if (world == null) return;
            if (!world.isChunkLoaded(vector.x >> 4, vector.z >> 4)) return;
            final Location location = vector.toCenterFloorLocation(world);
            itemDisplay = world.spawn(location, ItemDisplay.class, e -> {
                    e.setPersistent(false);
                    Entities.setTransient(e);
                    e.setItemStack(itemStack);
                    e.setVisibleByDefault(true);
                    if (glowColor != null) {
                        e.setGlowing(true);
                        e.setGlowColorOverride(glowColor);
                    }
                });
            player.showEntity(famPlugin(), itemDisplay);
        }
    }

    public void remove() {
        if (itemDisplay == null) return;
        itemDisplay.remove();
        itemDisplay = null;
    }
}
