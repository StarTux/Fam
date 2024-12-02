package com.cavetale.fam.advent;

import com.cavetale.core.bungee.Bungee;
import com.cavetale.core.item.ItemKinds;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.mytems.Mytems;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Data
@EqualsAndHashCode(callSuper = true)
public final class AdventDailyCollectItems extends AbstractAdventDaily {
    private final String worldName;
    private final Vec3i starLocation;
    private final ItemStack collectible;
    private final Color glowColor;
    private final List<Vec3i> itemLocations;

    @Override
    public void enable() {
    }

    @Override
    public void start(AdventSession session) {
        final Tag tag = new Tag();
        tag.itemsFound = new ArrayList<>();
        session.setTag(tag);
    }

    @Override
    public void load(AdventSession session) {
        Tag tag = (Tag) session.getTag();
        tag.starHolder = new ItemDisplayHolder(Mytems.STAR.createItemStack(),
                                               worldName,
                                               starLocation,
                                               Color.YELLOW);
        tag.itemHolders = new ArrayList<>();
        for (int i = 0; i < itemLocations.size(); i += 1) {
            final Vec3i vec = itemLocations.get(i);
            if (tag.itemsFound.size() <= i) {
                tag.itemsFound.add(false);
            }
            if (tag.itemHolders.size() <= i) {
                tag.itemHolders.add(new ItemDisplayHolder(collectible.clone(),
                                                          worldName,
                                                          vec,
                                                          glowColor));
            }
        }
    }

    @Override
    public void tick(AdventSession session) {
        Tag tag = (Tag) session.getTag();
        final Player player = session.getPlayer();
        if (!tag.hasAllItems) {
            for (int i = 0; i < itemLocations.size(); i += 1) {
                final Vec3i vector = itemLocations.get(i);
                final boolean found = tag.itemsFound.get(i);
                final ItemDisplayHolder holder = tag.itemHolders.get(i);
                if (found) {
                    holder.remove();
                } else {
                    holder.update(player);
                    if (Vec3i.of(player.getLocation()).maxDistance(vector) < 2) {
                        tag.itemFoundCount += 1;
                        tag.itemsFound.set(i, true);
                        player.spawnParticle(Particle.DUST, vector.toCenterLocation(player.getWorld()),
                                             16, 1.0, 1.0, 1.0, 0.125,
                                             new Particle.DustOptions(Color.GREEN, 1f));
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                        player.sendMessage(textOfChildren(ItemKinds.icon(collectible),
                                                          text(tag.itemFoundCount, GREEN),
                                                          text("/", GRAY),
                                                          text(itemLocations.size(), GREEN)));
                        holder.remove();
                        session.save(null);
                    }
                }
            }
            if (tag.itemFoundCount >= itemLocations.size()) {
                tag.hasAllItems = true;
                session.save(null);
                player.sendMessage(textOfChildren(text("Collection complete! Now find the ", GREEN),
                                                  Mytems.STAR));
            }
        } else if (!tag.hasStar) {
            tag.starHolder.update(player);
            if (Vec3i.of(player.getLocation()).maxDistance(starLocation) < 2) {
                tag.hasStar = true;
                tag.starHolder.remove();
                session.save(null);
                AdventMusic.deckTheHalls(player);
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
        for (ItemDisplayHolder it : tag.itemHolders) {
            it.remove();
        }
    }

    @Override
    public Class<? extends AdventDailyTag> getTagClass() {
        return Tag.class;
    }

    static final class Tag extends AdventDailyTag {
        private List<Boolean> itemsFound;
        private int itemFoundCount;
        private transient List<ItemDisplayHolder> itemHolders;
        private transient ItemDisplayHolder starHolder;
        private boolean hasAllItems;
        private boolean hasStar;
        private transient int hasStarTicks = 0;
        private transient boolean complete;
    }
}
