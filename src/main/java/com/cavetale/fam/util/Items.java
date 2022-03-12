package com.cavetale.fam.util;

import com.cavetale.mytems.Mytems;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.winthier.playercache.PlayerCache;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public final class Items {
    private Items() { }

    public static Component nonItalic(Component in) {
        return Component.text().decoration(TextDecoration.ITALIC, false).append(in).build();
    }

    public static ItemStack button(Material material, String text) {
        ItemStack itemStack = new ItemStack(material);
        itemStack.editMeta(meta -> {
                meta.displayName(Component.text(text));
                meta.addItemFlags(ItemFlag.values());
            });
        return itemStack;
    }

    public static ItemStack button(Mytems mytems, String text) {
        ItemStack itemStack = mytems.createItemStack();
        itemStack.editMeta(meta -> {
                meta.displayName(nonItalic(Component.text(text)));
                meta.addItemFlags(ItemFlag.values());
            });
        return itemStack;
    }

    public static ItemStack button(Mytems mytems, Component text) {
        ItemStack itemStack = mytems.createItemStack();
        itemStack.editMeta(meta -> {
                meta.displayName(nonItalic(text));
                meta.addItemFlags(ItemFlag.values());
            });
        return itemStack;
    }

    public static ItemStack button(ItemStack itemStack, List<Component> text) {
        itemStack.editMeta(meta -> {
                if (!text.isEmpty()) {
                    Component displayName = text.get(0);
                    List<Component> lore = text.subList(1, text.size());
                    meta.displayName(nonItalic(displayName));
                    meta.lore(lore.stream()
                              .map(c -> nonItalic(c))
                              .collect(Collectors.toList()));
                }
                meta.addItemFlags(ItemFlag.values());
            });
        return itemStack;
    }

    public static ItemStack button(Material material, List<Component> text) {
        return button(new ItemStack(material), text);
    }

    public static ItemStack button(Material material, Component text) {
        return button(material, List.of(text));
    }

    public static void text(ItemMeta meta, List<Component> text) {
        if (text.isEmpty()) return;
        Component displayName = text.get(0);
        List<Component> lore = text.subList(1, text.size());
        meta.displayName(nonItalic(displayName));
        meta.lore(lore.stream()
                  .map(c -> nonItalic(c))
                  .collect(Collectors.toList()));
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
        String name = profile.getName();
        if (name == null) name = PlayerCache.nameForUuid(profile.getId());
        if (name == null) name = "?";
        meta.displayName(Component.text().content(name)
                         .color(NamedTextColor.WHITE)
                         .decoration(TextDecoration.ITALIC, false)
                         .build());
        item.setItemMeta(meta);
        return item;
    }
}
