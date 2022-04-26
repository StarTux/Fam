package com.cavetale.fam.trophy;

import java.util.Comparator;

public final class InnerCategoryOrder implements Comparator<SQLTrophy> {
    public static final InnerCategoryOrder INSTANCE = new InnerCategoryOrder();

    /**
     * We assume that all trophies are sorted within the same
     * category.  Thus we sort by:
     * - Seen (ascending)
     * - Placement (ascending)
     * - Date (descending)
     */
    @Override
    public int compare(SQLTrophy a, SQLTrophy b) {
        if (a.seen != b.seen) {
            return a.seen ? 1 : -1;
        }
        int placement = Integer.compare(a.placement, b.placement);
        if (placement != 0) return placement;
        return Long.compare(b.time.getTime(), a.time.getTime());
    }

    public static InnerCategoryOrder innerCategoryOrder() {
        return INSTANCE;
    }
}
