package com.cavetale.fam.eventhost;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.event.minigame.MinigameMatchType;
import com.cavetale.fam.FamPlugin;
import org.bukkit.command.CommandSender;

public final class EventHostCommand extends AbstractCommand<FamPlugin> {
    public EventHostCommand(final FamPlugin plugin) {
        super(plugin, "eventhost");
    }

    @Override
    protected void onEnable() {
        rootNode.description("Event Hosting Commands");
        rootNode.addChild("start").arguments("<type>")
            .description("Start an event")
            .completers(CommandArgCompleter.enumLowerList(MinigameMatchType.class))
            .senderCaller(this::start);
    }

    private boolean start(CommandSender sender, String[] args) {
        return false;
    }
}
