package org.jchien.shuffle.model;

import com.google.common.collect.ImmutableSet;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * @author jchien
 */
public enum Item {
    // using mobile costs, not going to worry about 3ds cost differences
    MOVES_PLUS_5(1000, ImmutableSet.of("m+5", "+5 moves")),
    MEGA_START(2500, ImmutableSet.of("ms", "mega start")),
    DISRUPTION_DELAY(2000, ImmutableSet.of("dd", "disruption delay")),
    ATTACK_POWER_UP(5000, ImmutableSet.of("apu", "ap+")),
    COMPLEXITY_MINUS_1(9500, ImmutableSet.of("c-1"));

    private static final Map<String, Item> ALIAS_MAP = new HashMap<>();
    static {
        for (Item item : Item.values()) {
            for (String alias : item.aliases) {
                ALIAS_MAP.put(removeSpaces(alias), item);
            }
        }
    }

    private final int cost;

    private final Set<String> aliases;

    Item(int cost, Set<String> aliases) {
        this.cost = cost;
        this.aliases = aliases;
    }

    public static Item get(String alias) {
        Item ret = ALIAS_MAP.get(removeSpaces(alias));
        if (ret == null) {
            throw new NoSuchElementException("No item matching \"" + alias + "\"");
        }
        return ret;
    }

    private static String removeSpaces(String s) {
        return s.replaceAll("\\s+", "");
    }
}
