package com.cavetale.fam.util;

import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public final class Text {
    public static final String HEART_ICON = "\u2764";
    private Text() { }

    public static HoverEvent hover(String msg) {
        BaseComponent[] lore = TextComponent.fromLegacyText(msg);
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, lore);
    }

    public static HoverEvent hover(BaseComponent[] lore) {
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, lore);
    }

    public static ClickEvent click(String cmd) {
        return new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd);
    }

    public static ComponentBuilder builder(String txt) {
        return new ComponentBuilder(txt);
    }

    public static TextComponent extra(BaseComponent[] extra) {
        return new TextComponent(extra);
    }

    public static BaseComponent[] toHeartString(int hearts) {
        ComponentBuilder cb = new ComponentBuilder("").italic(false);
        for (int i = 0; i < hearts; i += 1) {
            cb.append(HEART_ICON).color(Colors.PINK);
        }
        for (int i = hearts; i < 5; i += 1) {
            cb.append(HEART_ICON).color(Colors.DARK_GRAY);
        }
        return cb.create();
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
                lastColors = org.bukkit.ChatColor.getLastColors(lineStr);
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
