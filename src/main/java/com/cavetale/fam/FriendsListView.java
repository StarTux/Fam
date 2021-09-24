package com.cavetale.fam;

import com.cavetale.fam.util.Colors;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.format.TextColor;

@RequiredArgsConstructor
public enum FriendsListView {
    FRIENDSHIPS("Friendships", Colors.LIGHT_BLUE),
    ONLINE_NOT_GIFTED("To Do: Friendship Gift", Colors.PALE_VIOLET_RED),
    FRIENDS("Friends", Colors.HOTPINK),
    BIRTHDAYS("Birthdays", Colors.GOLD);

    public final String menuTitle;
    public final TextColor menuColor;
}
