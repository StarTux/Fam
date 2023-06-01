package com.cavetale.fam;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.font.GlyphPolicy;
import com.cavetale.core.perm.Rank;
import com.cavetale.core.text.LineWrap;
import com.cavetale.fam.session.Session;
import com.winthier.chat.util.Filter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import static com.cavetale.fam.sql.Database.db;
import static com.cavetale.mytems.util.Items.text;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class SetStatusCommand extends AbstractCommand<FamPlugin> {
    public SetStatusCommand(final FamPlugin plugin) {
        super(plugin, "setstatus");
    }

    @Override
    protected void onEnable() {
        rootNode.description("Set your status message")
            .denyTabCompletion()
            .arguments("<message>")
            .playerCaller(this::setStatus);
    }

    private boolean setStatus(Player player, String[] args) {
        if (args.length == 0) {
            resetStatus(player);
            return true;
        }
        String msg = String.join(" ", args);
        if (msg.length() > 256) return false;
        setStatus(player, msg);
        return true;
    }

    public void setStatus(Player player, String message) {
        Session session = Session.of(player);
        if (!session.isReady()) throw new CommandWarn("Session not ready. Try again later");
        message = Filter.filterUnicode(message);
        for (Rank it : Rank.all()) {
            if (player.hasPermission("group." + it.getKey())) continue;
            message = message.replace(":" + it.getKey() + ":", "");
        }
        session.getPlayerRow().setStatusMessage(message);
        db().updateAsync(session.getPlayerRow(), null, "statusMessage");
        Component displayMessage = new LineWrap()
            .emoji(player.hasPermission("chat.emoji"))
            .glyphPolicy(GlyphPolicy.PUBLIC)
            .tooltip(false)
            .componentMaker(str -> text(str, WHITE))
            .format(message);
        player.sendMessage(textOfChildren(text("Status Message Updated: ", GREEN), displayMessage));
    }

    public void resetStatus(Player player) {
        Session session = Session.of(player);
        if (!session.isReady()) throw new CommandWarn("Session not ready. Try again later");
        if (session.getPlayerRow().getStatusMessage() == null) {
            throw new CommandWarn("You did not set a status message");
        }
        session.getPlayerRow().setStatusMessage(null);
        db().updateAsync(session.getPlayerRow(), null, "statusMessage");
        player.sendMessage(text("Status message cleared", GREEN));
    }
}
