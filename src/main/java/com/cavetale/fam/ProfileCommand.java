package com.cavetale.fam;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.fam.session.Session;
import org.bukkit.entity.Player;

public final class ProfileCommand extends AbstractCommand<FamPlugin> {
    public ProfileCommand(final FamPlugin plugin) {
        super(plugin, "profile");
    }

    @Override
    protected void onEnable() {
        rootNode.description("View your profile page")
            .denyTabCompletion()
            .playerCaller(this::profile);
    }

    private void profile(Player player) {
        Session session = Session.of(player);
        if (!session.isReady()) {
            throw new CommandWarn("Session not ready. Try again later!");
        }
        new ProfileDialogue(plugin, session).open(player);
    }
}
