package com.cavetale.fam.elo;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.event.minigame.MinigameMatchType;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.fam.FamPlugin;
import com.cavetale.fam.sql.Database;
import com.cavetale.fam.sql.SQLElo;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import org.bukkit.command.CommandSender;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class EloAdminCommand extends AbstractCommand<FamPlugin> {
    public EloAdminCommand(final FamPlugin plugin) {
        super(plugin, "eloadmin");
    }

    @Override
    protected void onEnable() {
        rootNode.description("Elo admin command");
        rootNode.addChild("rank").arguments("<category> [page]")
            .description("Print the Elo ranking")
            .completers(CommandArgCompleter.enumLowerList(MinigameMatchType.class),
                        CommandArgCompleter.integer(i -> i > 0))
            .senderCaller(this::rank);
        rootNode.addChild("set").arguments("<category> <player> <rating>")
            .description("Set the ELO of a player")
            .completers(CommandArgCompleter.enumLowerList(MinigameMatchType.class),
                        CommandArgCompleter.PLAYER_CACHE,
                        CommandArgCompleter.doubles(r -> r >= 0.0))
            .senderCaller(this::set);
        rootNode.addChild("adjust").arguments("<category> <player1> <player2> <result>")
            .description("Adjust the rating of 2 players")
            .completers(CommandArgCompleter.enumLowerList(MinigameMatchType.class),
                        CommandArgCompleter.PLAYER_CACHE,
                        CommandArgCompleter.PLAYER_CACHE,
                        CommandArgCompleter.doubles(r -> r >= 0.0 && r <= 1.0))
            .senderCaller(this::adjust);
    }

    private static String fmt(double rating) {
        return "" + ((int) rating);
    }

    private boolean rank(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 2) return false;
        final String category = args[0];
        final int page = args.length >= 2
            ? CommandArgCompleter.requireInt(args[1], i -> i > 0)
            : 1;
        final List<SQLElo> elos = Database.db().find(SQLElo.class).eq("category", category).findList();
        if (elos.isEmpty()) {
            throw new CommandWarn("No ranking available: " + category);
        }
        final int pageLen = 20;
        final int offset = pageLen * (page - 1);
        if (elos.size() <= offset) {
            throw new CommandWarn("Page not available: " + page);
        }
        Collections.sort(elos, Comparator.comparing(SQLElo::getRating).reversed());
        for (int i = 0; i < pageLen; i += 1) {
            final int index = offset + i;
            if (index >= elos.size()) break;
            final SQLElo elo = elos.get(index);
            sender.sendMessage(textOfChildren(text((index + 1), GRAY),
                                              text(" " + fmt(elo.getRating()), YELLOW),
                                              text(" " + PlayerCache.nameForUuid(elo.getPlayer())),
                                              text("(" + elo.getGames() + ")", DARK_GRAY)));
        }
        return true;
    }

    private boolean set(CommandSender sender, String[] args) {
        if (args.length != 3) return false;
        final String category = args[0];
        final PlayerCache player = CommandArgCompleter.requirePlayerCache(args[1]);
        final double rating = CommandArgCompleter.requireDouble(args[2], r -> r >= 0.0);
        SQLElo elo = Database.db().find(SQLElo.class).eq("category", category).eq("player", player.uuid).findUnique();
        if (elo == null) {
            elo = new SQLElo(category, player.uuid);
            Database.db().insert(elo);
        }
        elo.setRating(rating);
        elo.setLastUpdate(new Date());
        Database.db().update(elo, "rating", "lastUpdate");
        sender.sendMessage(text(player.name + " rating was set to " + elo.getRating(), YELLOW));
        return true;
    }

    private boolean adjust(CommandSender sender, String[] args) {
        if (args.length != 4) return false;
        final String category = args[0];
        final PlayerCache player1 = CommandArgCompleter.requirePlayerCache(args[1]);
        final PlayerCache player2 = CommandArgCompleter.requirePlayerCache(args[2]);
        if (player1.equals(player2)) {
            throw new CommandWarn("Players must be different");
        }
        final double result = CommandArgCompleter.requireDouble(args[3], r -> r >= 0.0 && r <= 1.0);
        SQLElo elo1 = Database.db().find(SQLElo.class).eq("category", category).eq("player", player1.uuid).findUnique();
        SQLElo elo2 = Database.db().find(SQLElo.class).eq("category", category).eq("player", player2.uuid).findUnique();
        if (elo1 == null) {
            elo1 = new SQLElo(category, player1.uuid);
            Database.db().insert(elo1);
        }
        if (elo2 == null) {
            elo2 = new SQLElo(category, player2.uuid);
            Database.db().insert(elo2);
        }
        final double rating1 = elo1.getRating();
        final double rating2 = elo2.getRating();
        elo1.increaseGames();
        elo1.updateRatingAgainst(rating2, result);
        elo2.increaseGames();
        elo2.updateRatingAgainst(rating1, 1.0 - result);
        Database.db().update(elo1, "rating", "games", "lastUpdate");
        Database.db().update(elo2, "rating", "games", "lastUpdate");
        sender.sendMessage(text(player1.name + " #" + elo1.getGames() + " " + fmt(rating1) + " => " + fmt(elo1.getRating()), YELLOW));
        sender.sendMessage(text(player2.name + " #" + elo2.getGames() + " " + fmt(rating2) + " => " + fmt(elo2.getRating()), YELLOW));
        return true;
    }
}
