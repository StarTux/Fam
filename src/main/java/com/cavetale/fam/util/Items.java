package com.cavetale.fam.util;

import com.cavetale.mytems.Mytems;
import com.destroystokyo.paper.profile.PlayerProfile;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public final class Items {
    private Items() { }

    public static ItemStack button(Material material, String text) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(text);
        meta.addItemFlags(ItemFlag.values());
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static ItemStack button(Mytems mytems, String text) {
        ItemStack itemStack = mytems.createItemStack();
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(text);
        meta.addItemFlags(ItemFlag.values());
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static ItemStack button(Mytems mytems, Component text) {
        ItemStack itemStack = mytems.createItemStack();
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(text);
        meta.addItemFlags(ItemFlag.values());
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static ItemStack button(Material material, List<BaseComponent[]> text) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (!text.isEmpty()) {
            meta.setDisplayNameComponent(text.get(0));
            meta.setLoreComponents(text.subList(1, text.size()));
        }
        meta.addItemFlags(ItemFlag.values());
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static String getDisplayName(Material material) {
        if (material.isItem()) {
            return new ItemStack(material).getI18NDisplayName();
        }
        return Text.toCamelCase(material);
    }

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
        meta.displayName(Component.text().content(profile.getName())
                         .color(NamedTextColor.WHITE)
                         .decoration(TextDecoration.ITALIC, false)
                         .build());
        item.setItemMeta(meta);
        return item;
    }
}
