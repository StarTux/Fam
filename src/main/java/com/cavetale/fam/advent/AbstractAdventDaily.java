package com.cavetale.fam.advent;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;

@Getter
@Setter
public abstract class AbstractAdventDaily implements AdventDaily {
    private int day;
    private List<Component> description = List.of();
    private String warp = "";

    public AbstractAdventDaily() { }
}
