package com.cavetale.fam.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

@Getter @RequiredArgsConstructor
public final class GuiPaginator {
    private final JavaPlugin plugin;
    @Setter private int pageSize = 27;
    private List<Slot> slots = new ArrayList<>();
    private Supplier<Component> titleSupplier;
    private int pageCount = 0;
    private int pageNumber = 0;
    private int pageIndex = 0;

    @RequiredArgsConstructor
    public final class Slot {
        private final ItemStack itemStack;
        private final Consumer<InventoryClickEvent> onClick;
    }

    public GuiPaginator add(Slot slot) {
        slots.add(slot);
        return this;
    }

    public GuiPaginator add(List<Slot> newSlots) {
        slots.addAll(newSlots);
        return this;
    }

    public GuiPaginator add(ItemStack itemStack, Consumer<InventoryClickEvent> onClick) {
        slots.add(new Slot(itemStack, onClick));
        return this;
    }

    public GuiPaginator clear() {
        slots.clear();
        return this;
    }

    public GuiPaginator title(Supplier<Component> supplier) {
        this.titleSupplier = supplier;
        return this;
    }

    public Gui open(Player player) {
        return null;
    }
}
