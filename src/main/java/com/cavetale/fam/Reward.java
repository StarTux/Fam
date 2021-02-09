package com.cavetale.fam;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.bukkit.inventory.ItemStack;

@Data
public final class Reward {
    List<ItemStack> items = new ArrayList<>();

    public Reward item(ItemStack item) {
        items.add(item);
        return this;
    }

    public Reward item(ItemStack item, int times) {
        for (int i = 0; i < times; i += 1) {
            items.add(item);
        }
        return this;
    }
}
