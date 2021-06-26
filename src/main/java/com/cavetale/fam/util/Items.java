package com.cavetale.fam.util;

import com.cavetale.mytems.Mytems;
import java.util.List;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
}
