package com.cavetale.fam;

import com.cavetale.fam.sql.Database;
import java.util.Set;
import java.util.UUID;
import lombok.NonNull;

public final class Fam {
    private Fam() { }

    public static int increaseSingleFriendship(int amount, @NonNull UUID main, @NonNull Set<UUID> friends) {
        friends.remove(main);
        return Database.increaseSingleFriendship(main, friends, amount);
    }

    public static int increaseMutualFriendship(int amount, @NonNull Set<UUID> friends) {
        return Database.increaseMutualFriendship(friends, amount);
    }
}
