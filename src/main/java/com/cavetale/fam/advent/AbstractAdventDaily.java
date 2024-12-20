package com.cavetale.fam.advent;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

@Getter
@Setter
public abstract class AbstractAdventDaily implements AdventDaily {
    private int day;
    private List<Component> description = List.of();
    private String warp = "";

    public AbstractAdventDaily() { }

    static final boolean isInBoat(Player player) {
        return player.isInsideVehicle()
            && player.getVehicle() instanceof Boat;
    }
}
