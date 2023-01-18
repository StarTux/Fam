package com.cavetale.fam;

import com.cavetale.core.friends.FriendsSupplier;
import org.bukkit.inventory.ItemStack;

public final class FamFriendsSupplier implements FriendsSupplier {
    @Override
    public ItemStack getDailyFriendshipGift() {
        return new ItemStack(FamPlugin.getInstance().getTodaysGift());
    }
}
