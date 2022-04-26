package com.cavetale.fam.trophy;

import com.cavetale.core.editor.EditMenuAdapter;
import com.cavetale.core.editor.EditMenuButton;
import com.cavetale.core.editor.EditMenuItem;
import com.cavetale.core.editor.EditMenuNode;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.trophy.TrophyCategory;
import com.cavetale.mytems.item.trophy.TrophyQuality;
import com.cavetale.mytems.item.trophy.TrophyType;
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
public final class SQLTrophy implements EditMenuAdapter {
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
            } catch (IllegalArgumentException iae) { }
            if (trophyCategory != null) {
                TrophyType trophyType = null;
                for (TrophyType it : TrophyType.values()) {
                    if (it.category == trophyCategory) {
                        if (placement == 0) {
                            trophyType = it;
                            break;
                        } else if (it.quality.ordinal() <= placement - 1) {
                            trophyType = it;
                        }
                    }
                }
                if (trophyType != null) return trophyType.mytems.createItemStack();
            }
        }
            return Mytems.QUESTION_MARK.createIcon();
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
            });
    }
}
