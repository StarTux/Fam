package com.cavetale.fam.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;

public final class Text {
    public static final String HEART_ICON = "\u2764";
    public static final String HEARTS;

    private Text() { }

    static {
        String h = "";
        for (int i = 0; i < 10; i += 1) {
            h += HEART_ICON;
        }
        HEARTS = h;
    }

    public static Component toHeartString(int hearts) {
        if (hearts < 0 || hearts > 5) throw new IllegalArgumentException("hearts=" + hearts);
        String full = HEARTS.substring(0, hearts);
        String empty = HEARTS.substring(0, 5 - hearts);
        return Component.join(JoinConfiguration.noSeparators(), new Component[] {
                Component.text(full, Colors.HOTPINK),
                Component.text(empty, Colors.DARK_GRAY),
            });
    }
}
