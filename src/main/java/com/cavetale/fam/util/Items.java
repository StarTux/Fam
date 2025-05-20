package com.cavetale.fam.util;

import com.cavetale.core.playercache.PlayerCache;
import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public final class Items {
    private Items() { }

    public static ItemStack makeSkull(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setPlayerProfile(player.getPlayerProfile());
        meta.displayName(Component.text().content(player.getName())
                         .color(NamedTextColor.WHITE)
                         .decoration(TextDecoration.ITALIC, false)
                         .build());
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack makeSkull(PlayerProfile profile) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setPlayerProfile(profile);
        String name = PlayerCache.nameForUuid(profile.getId());
        if (name == null) name = "?";
        meta.displayName(Component.text().content(name)
                         .color(NamedTextColor.WHITE)
                         .decoration(TextDecoration.ITALIC, false)
                         .build());
        item.setItemMeta(meta);
        return item;
    }
}
