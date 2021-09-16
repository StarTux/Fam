package com.cavetale.fam.util;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.ChatColor;

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
        return TextComponent.ofChildren(new Component[] {
                Component.text(full, Colors.HOTPINK),
                Component.text(empty, Colors.DARK_GRAY),
            });
    }

    public static List<String> wrapLine(String what, int maxLineLength) {
        String[] words = what.split("\\s+");
        List<String> lines = new ArrayList<>();
        if (words.length == 0) return lines;
        StringBuilder line = new StringBuilder(words[0]);
        int lineLength = ChatColor.stripColor(words[0]).length();
        String lastColors = "";
        for (int i = 1; i < words.length; ++i) {
            String word = words[i];
            int wordLength = ChatColor.stripColor(word).length();
            if (lineLength + wordLength + 1 > maxLineLength) {
                String lineStr = lastColors + line.toString();
                lines.add(lineStr);
                lastColors = ChatColor.getLastColors(lineStr);
                line = new StringBuilder(word);
                lineLength = wordLength;
            } else {
                line.append(" ");
                line.append(word);
                lineLength += wordLength + 1;
            }
        }
        if (line.length() > 0) lines.add(lastColors + line.toString());
        return lines;
    }

    public static String toCamelCase(String in) {
        return in.substring(0, 1).toUpperCase()
            + in.substring(1).toLowerCase();
    }

    public static String toCamelCase(String[] in) {
        String[] out = new String[in.length];
        for (int i = 0; i < in.length; i += 1) {
            out[i] = toCamelCase(in[i]);
        }
        return String.join(" ", out);
    }

    public static String toCamelCase(Enum en) {
        return toCamelCase(en.name().split("_"));
    }
}
