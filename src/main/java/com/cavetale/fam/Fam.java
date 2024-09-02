package com.cavetale.fam;

import com.cavetale.fam.sql.Database;
import com.cavetale.fam.sql.SQLFriends;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.Value;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class Fam {
    private Fam() { }

    public static int increaseSingleFriendship(int amount, @NonNull UUID main, @NonNull Set<UUID> friends) {
        friends.remove(main);
        return Database.increaseSingleFriendship(main, friends, amount);
    }

    public static int increaseMutualFriendship(int amount, @NonNull Set<UUID> friends) {
        return Database.increaseMutualFriendship(friends, amount);
    }

    public static void relationshipsOf(UUID player, Consumer<List<Relationship>> callback) {
        Database.db().scheduleAsyncTask(() -> {
                List<Relationship> list = new ArrayList<>();
                for (SQLFriends row : Database.findFriendsList(player)) {
                    Relation relation = row.getRelationEnum();
                    list.add(new Relationship(row.getOther(player),
                                              row.getFriendship(),
                                              relation == Relation.FRIEND,
                                              relation == Relation.MARRIED));
                }
                Bukkit.getScheduler().runTask(FamPlugin.instance, () -> {
                        callback.accept(list);
                    });
            });
    }

    @Value
    public static final class Relationship {
        public final UUID uuid;
        public final int friendship;
        public final boolean friend;
        public final boolean married;

        public Player getPlayer() {
            return Bukkit.getPlayer(uuid);
        }

        public boolean isOnline() {
            return getPlayer() != null;
        }
    }
}
