package com.cavetale.fam.trophy;

import com.cavetale.core.editor.EditMenuAdapter;
import com.cavetale.core.editor.EditMenuButton;
import com.cavetale.core.editor.EditMenuException;
import com.cavetale.core.editor.EditMenuItem;
import com.cavetale.core.editor.EditMenuNode;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.trophy.TrophyCategory;
import com.cavetale.mytems.item.trophy.TrophyQuality;
import com.cavetale.mytems.item.trophy.TrophyType;
import com.winthier.sql.SQLRow;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import lombok.Data;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.mytems.util.Text.wrapLore;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson;

@Data
@Table(name = "trophies",
       indexes = {
           @Index(name = "owner", columnList = "owner", unique = false),
           @Index(name = "category", columnList = "category", unique = false),
       })
public final class SQLTrophy implements SQLRow, EditMenuAdapter, Cloneable {
    @Id @EditMenuItem(settable = false, deletable = true)
    protected Integer id;

    @Column(nullable = false)
    protected UUID owner;
    @Column(nullable = false, length = 63)
    protected String category;
    @Column(nullable = false)
    protected Date time;
    @Column(nullable = false)
    protected int placement;

    /**
     * Either a reference to a Mytems, or a TrophyCategory.
     */
    @Column(nullable = false)
    protected String iconType;

    /**
     * A component, possibly representing the whole category.
     */
    @Column(nullable = false, length = 1024)
    protected String title;
    /**
     * A string to be broken into lines.
     */
    @Column(nullable = true, length = 1024)
    protected String inscription;

    @Column(nullable = false)
    protected boolean seen;

    public SQLTrophy() { }

    public SQLTrophy(final SQLTrophy o) {
        this.id = o.id;
        this.owner = o.owner;
        this.category = o.category;
        this.time = o.time;
        this.placement = o.placement;
        this.iconType = o.iconType;
        this.title = o.title;
        this.inscription = o.inscription;
        this.seen = o.seen;
    }

    public SQLTrophy(final UUID owner,
                     final String category,
                     final int placement,
                     final Mytems mytems,
                     final Component title,
                     final String inscription) {
        this.owner = owner;
        this.category = category;
        this.time = new Date();
        this.placement = placement;
        this.iconType = "mytems:" + mytems.id;
        this.title = gson().serialize(title);
        this.inscription = inscription;
    }

    public SQLTrophy(final UUID owner,
                     final String category,
                     final int placement,
                     final TrophyCategory trophyCategory,
                     final Component title,
                     final String inscription) {
        this.owner = owner;
        this.category = category;
        this.time = new Date();
        this.placement = placement;
        this.iconType = "trophy:" + trophyCategory.name().toLowerCase();
        this.title = gson().serialize(title);
        this.inscription = inscription;
    }

    public SQLTrophy(final UUID owner,
                     final String category,
                     final int placement,
                     final ItemStack item,
                     final Component title,
                     final String inscription) {
        this.owner = owner;
        this.category = category;
        this.time = new Date();
        this.placement = placement;
        this.iconType = "item:" + item.getType().getKey().getKey() + (item.hasItemMeta() ? item.getItemMeta().getAsString() : "");
        this.title = gson().serialize(title);
        this.inscription = inscription;
    }

    @Override
    public SQLTrophy clone() {
        return new SQLTrophy(this);
    }

    public ItemStack getIcon() {
        if (iconType.startsWith("mytems:")) {
            final Mytems mytems = Mytems.forId(iconType.substring(7));
            if (mytems != null) {
                return mytems.createIcon();
            }
        }
        if (iconType.startsWith("trophy:")) {
            TrophyCategory trophyCategory = null;
            String name = iconType.substring(7);
            try {
                trophyCategory = TrophyCategory.valueOf(name.toUpperCase());
                List<TrophyType> types = TrophyType.of(trophyCategory);
                if (!types.isEmpty()) {
                    TrophyType trophyType = placement == 0
                        ? types.get(0)
                        : types.get(Math.min(placement, types.size()) - 1);
                    return trophyType.createItemStack();
                }
            } catch (IllegalArgumentException iae) { }
        }
        if (iconType.startsWith("item:")) {
            try {
                return Bukkit.getItemFactory().createItemStack(iconType.substring(5));
            } catch (IllegalArgumentException iae) { }
        }
        return Mytems.QUESTION_MARK.createIcon();
    }

    public void setIcon(ItemStack item) {
        this.iconType = "item:" + item.getType().getKey().getKey() + (item.hasItemMeta() ? item.getItemMeta().getAsString() : "");
    }

    public void setIcon(Material material) {
        this.iconType = "item:" + material.getKey().getKey();
    }

    public void setIcon(Mytems mytems) {
        this.iconType = "mytems:" + mytems.id;
    }

    public void setIcon(TrophyCategory trophyCategory) {
        this.iconType = "trophy:" + trophyCategory.name().toLowerCase();
    }

    private static String th(int in) {
        String out = Integer.toString(in);
        switch (out.charAt(out.length() - 1)) {
        case '1':
            return out.concat(out.endsWith("11") ? "th" : "st");
        case '2':
            return out.concat(out.endsWith("12") ? "th" : "nd");
        case '3':
            return out.concat(out.endsWith("13") ? "th" : "rd");
        default:
            return out.concat("th");
        }
    }

    @EditMenuItem(hidden = true)
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM dd yyyy");

    public TextColor getQualityColor() {
        TrophyQuality[] qualities = TrophyQuality.values();
        TrophyQuality quality = placement == 0
            ? qualities[0]
            : qualities[Math.min(placement - 1, qualities.length - 1)];
        return quality.textColor;
    }

    public List<Component> getTooltip() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(getTitleComponent());
        if (placement > 0) {
            tooltip.add(text(th(placement) + " Place", getQualityColor()));
        }
        tooltip.add(text(DATE_FORMAT.format(time), GRAY));
        if (inscription != null && !inscription.isEmpty()) {
            tooltip.addAll(wrapLore(inscription, c -> c.color(getQualityColor())));
        }
        return tooltip;
    }

    public void setTitleComponent(Component c) {
        this.title = gson().serialize(c);
    }

    public Component getTitleComponent() {
        return gson().deserialize(this.title);
    }

    public void setNow() {
        time = new Date();
    }

    @Override
    public List<EditMenuButton> getEditMenuButtons(EditMenuNode node) {
        return List.of(new EditMenuButton() {
                @Override
                public ItemStack getMenuIcon() {
                    return SQLTrophy.this.getIcon();
                }

                @Override
                public List<Component> getTooltip() {
                    return SQLTrophy.this.getTooltip();
                }

                @Override
                public void onClick(Player player, ClickType type) {
                    player.sendMessage("Meep meep!");
                }
            },
            new EditMenuButton() {
                @Override
                public ItemStack getMenuIcon() {
                    return Mytems.MAGNET.createIcon();
                }

                @Override
                public List<Component> getTooltip() {
                    return List.of(text("Copy item in hand", GRAY));
                }

                @Override
                public void onClick(Player player, ClickType type) {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    if (item == null || item.getType() == Material.AIR) {
                        throw new EditMenuException("Hand is empty!");
                    }
                    Mytems mytems = Mytems.forItem(item);
                    if (mytems != null) {
                        SQLTrophy.this.iconType = "mytems:" + mytems.id;
                        return;
                    }
                    SQLTrophy.this.iconType = "item:" + item.getType().getKey().getKey() + (item.hasItemMeta() ? item.getItemMeta().getAsString() : "");
                }
            });
    }
}
