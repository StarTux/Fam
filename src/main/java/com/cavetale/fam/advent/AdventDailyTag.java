package com.cavetale.fam.advent;

import com.cavetale.core.util.Json;
import java.io.Serializable;

/**
 * Superclass of all daily tags.
 * Daily tags store per player information, which will be saved to the
 * database unless marked transient.
 */
public class AdventDailyTag implements Serializable {
    public final String serialize() {
        return Json.serialize(this);
    }
}
