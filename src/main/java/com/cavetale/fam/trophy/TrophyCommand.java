package com.cavetale.fam.trophy;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.fam.FamPlugin;
import com.winthier.playercache.PlayerCache;
import org.bukkit.entity.Player;

public final class TrophyCommand extends AbstractCommand<FamPlugin> {
    private final Trophies trophies;

    public TrophyCommand(final Trophies trophies) {
        super(trophies.plugin, "trophy");
        this.trophies = trophies;
    }

    @Override
    protected void onEnable() {
        rootNode.description("View trophies")
            .completers(CommandArgCompleter.NULL)
            .playerCaller(this::trophy);
    }

    private boolean trophy(Player player, String[] args) {
        if (args.length > 1) return false;
        PlayerCache target = args.length == 0
            ? new PlayerCache(player.getUniqueId(), player.getName())
            : PlayerCache.forArg(args[0]);
        if (target == null) throw new CommandWarn("Player not found");
        new TrophyDialogue(trophies, target).open(player);
        return true;
    }
}
