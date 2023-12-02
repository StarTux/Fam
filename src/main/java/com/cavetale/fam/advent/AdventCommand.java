package com.cavetale.fam.advent;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.fam.FamPlugin;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class AdventCommand extends AbstractCommand<FamPlugin> {
    public AdventCommand(final FamPlugin plugin) {
        super(plugin, "advent");
    }

    @Override
    protected void onEnable() {
        rootNode.description("Advent player command")
            .playerCaller(this::advent);
    }

    private void advent(Player player) {
        AdventCalendar.open(player);
    }
}
