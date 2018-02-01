package org.jchien.shuffle.model;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * @author jchien
 */
public enum Item {
    // using mobile costs, not going to worry about 3ds cost differences

    // first alias element is canonical name for toString() purposes
    MOVES_PLUS_5(1000, new String[] { "Moves +5", "+5 Moves", "M+5", "+5" }),
    TIME_PLUS_10(1000, new String[] { "Time +10", "T+10", "+10 Secs", "+10 seconds", "+10" }),
    MEGA_START(2500, new String[] { "MS", "mega start" }),
    DISRUPTION_DELAY(2000, new String[] { "DD", "disruption delay" }),
    ATTACK_POWER_UP(5000, new String[] { "APU", "ap+", "attack power up", "attack up" }),
    COMPLEXITY_MINUS_1(9500, new String[] { "C-1" }),
    JEWEL(20000, new String[] { "Jewel" }); // since you can buy all items with one jewel, we'll call it the sum of those items

    private static final Set<Item> FULL_ITEMS_MOVES = EnumSet.of(
            MOVES_PLUS_5,
            MEGA_START,
            DISRUPTION_DELAY,
            ATTACK_POWER_UP,
            COMPLEXITY_MINUS_1);

    private static final Set<Item> FULL_ITEMS_TIMED = EnumSet.of(
            MOVES_PLUS_5,
            MEGA_START,
            DISRUPTION_DELAY,
            ATTACK_POWER_UP,
            COMPLEXITY_MINUS_1);

    private static final Map<String, Item> ALIAS_MAP = new HashMap<>();
    static {
        for (Item item : Item.values()) {
            for (String alias : item.aliases) {
                ALIAS_MAP.put(removeSpaces(alias.toLowerCase()), item);
            }
        }
    }

    private final int cost;

    private final String[] aliases;

    Item(int cost, String[] aliases) {
        if (aliases == null || aliases.length == 0) {
            throw new IllegalArgumentException("need at least one alias");
        }

        this.cost = cost;
        this.aliases = aliases;
    }

    public int getCost() {
        return cost;
    }

    public String toString() {
        return aliases[0];
    }

    public static Item get(String alias) {
        if (alias == null) {
            throw new NoSuchElementException("No null item");
        }
        Item ret = ALIAS_MAP.get(removeSpaces(alias.toLowerCase()));
        if (ret == null) {
            throw new NoSuchElementException("No item matching \"" + alias + "\"");
        }
        return ret;
    }

    private static String removeSpaces(String s) {
        return s.replaceAll("\\s+", "");
    }
}
