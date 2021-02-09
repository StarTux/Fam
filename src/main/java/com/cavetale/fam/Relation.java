package com.cavetale.fam;

public enum Relation {
    FRIEND,
    PARENT,
    CHILD,
    MARRIED;

    public final String humanName;

    Relation() {
        this.humanName = name().substring(0, 1) + name().substring(1).toLowerCase();
    }

    public boolean isMutual() {
        return getInverse() == this;
    }

    public Relation getInverse() {
        switch (this) {
        case PARENT: return CHILD;
        case CHILD: return PARENT;
        default: return this;
        }
    }

    public String getYour() {
        if (this == MARRIED) return "spouse";
        return name().toLowerCase();
    }
}
