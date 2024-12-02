package com.cavetale.fam.advent;

import com.cavetale.core.bungee.Bungee;
import com.cavetale.core.connect.NetworkServer;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.mytems.Mytems;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import static com.cavetale.fam.FamPlugin.famPlugin;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class AdventCelebration extends BukkitRunnable {
    private final Player player;
    private final String worldName;
    private final Vec3i starVector;
    private final int completedDay;
    private int ticks;

    public void start() {
        AdventMusic.deckTheHalls(player);
        runTaskTimer(famPlugin(), 1L, 1L);
    }

    @Override
    public void run() {
        final World world = Bukkit.getWorld(worldName);
        if (ticks > 160 || world == null || !player.isOnline() || !player.getWorld().equals(world)) {
            cancel();
            if (player.isOnline()) {
                if (completedDay != 0) {
                    player.sendMessage(textOfChildren(Mytems.CHRISTMAS_TOKEN,
                                                      text("You completed day " + completedDay + " of the Advent Calendar", GREEN))
                                       .hoverEvent(showText(text("/advent", GRAY)))
                                       .clickEvent(runCommand("/advent")));
                }
                if (!NetworkServer.BETA.isThisServer()) {
                    Bungee.send(player, "hub");
                }
            }
            return;
        }
        player.spawnParticle(Particle.DUST, starVector.toCenterLocation(world),
                             16, 1.0, 1.0, 1.0, 0.125,
                             new Particle.DustOptions(Color.YELLOW, 1f));
        ticks += 1;
    }
}
