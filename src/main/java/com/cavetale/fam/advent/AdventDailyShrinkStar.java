package com.cavetale.fam.advent;

import com.cavetale.core.connect.NetworkServer;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.mytems.Mytems;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import static com.cavetale.fam.FamPlugin.famPlugin;

/**
 * Interacting with a shrinklocation (in order) shrinks you by half
 * each time.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public final class AdventDailyShrinkStar extends AbstractAdventDaily {
    private final String worldName;
    private final Vec3i starLocation;
    private final List<Vec3i> shrinkLocations = new ArrayList<>();

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
        if (tag.doShrink) {
            double scale = player.getAttribute(Attribute.SCALE).getBaseValue();
            if (scale <= tag.toScale) {
                tag.doShrink = false;
            } else {
                scale = Math.max(tag.toScale, scale - 0.025);
                player.getAttribute(Attribute.SCALE).setBaseValue(scale);
                player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.1 * scale);
                player.getAttribute(Attribute.JUMP_STRENGTH).setBaseValue(0.42 * scale);
                player.getAttribute(Attribute.GRAVITY).setBaseValue(0.08 * scale);
                player.getAttribute(Attribute.STEP_HEIGHT).setBaseValue(0.6 * scale);
            }
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

    @Override
    public void unload(AdventSession session) {
        Tag tag = (Tag) session.getTag();
        tag.starHolder.remove();
        if (NetworkServer.BETA.isThisServer()) {
            Player player = session.getPlayer();
            player.getAttribute(Attribute.SCALE).setBaseValue(1.0);
            player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.1);
            player.getAttribute(Attribute.JUMP_STRENGTH).setBaseValue(0.42);
            player.getAttribute(Attribute.GRAVITY).setBaseValue(0.08);
            player.getAttribute(Attribute.STEP_HEIGHT).setBaseValue(0.6);
        }
    }

    @Override
    public Class<? extends AdventDailyTag> getTagClass() {
        return Tag.class;
    }

    public void addShrinkLocation(int x, int y, int z) {
        shrinkLocations.add(Vec3i.of(x, y, z));
    }

    public void onInteract(Player player, AdventSession session, Block block) {
        if (!(session.getTag() instanceof Tag tag)) return;
        if (tag.shrinkIndex >= shrinkLocations.size()) return;
        if (!shrinkLocations.get(tag.shrinkIndex).equals(Vec3i.of(block))) return;
        tag.shrinkIndex += 1;
        double scale = 1.0;
        for (int i = 0; i < tag.shrinkIndex; i += 1) {
            scale *= 0.5;
        }
        tag.toScale = scale;
        tag.doShrink = true;
        famPlugin().getLogger().info("[Advent] Shrink " + player.getName() + " to " + tag.toScale);
        player.playSound(block.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.5f);
    }

    static final class Tag extends AdventDailyTag {
        private transient ItemDisplayHolder starHolder;
        private transient int shrinkIndex;
        private boolean doShrink;
        private transient double toScale;
    }
}
