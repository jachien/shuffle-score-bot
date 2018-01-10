package org.jchien.shuffle.bot;

import org.jchien.shuffle.model.FormatException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author jchien
 */
public class CanonicalizerTest {
    @Test
    public void testGetLevel_NumOnly() throws FormatException {
        Canonicalizer c = new Canonicalizer();
        for (int i=1; i < 50; i++) {
            String s = String.valueOf(i);
            Assertions.assertEquals(Integer.valueOf(i), c.getLevel(s));
        }
    }

    @Test
    public void testGetLevel_LvPrefix() throws FormatException {
        Canonicalizer c = new Canonicalizer();
        // not currently validating level 0
        for (int i=0; i < 50; i++) {
            Integer lvl = Integer.valueOf(i);

            String s = "lv" + String.valueOf(lvl);
            assertEquals(lvl, c.getLevel(s));

            // space between is okay
            s = "lv " + String.valueOf(lvl);
            assertEquals(lvl, c.getLevel(s));

            // multiple spaces between is okay
            s = "lv  " + String.valueOf(lvl);
            assertEquals(lvl, c.getLevel(s));

            // case insensitive
            s = "LV " + String.valueOf(lvl);
            assertEquals(lvl, c.getLevel(s));
        }
    }

    @Test
    public void testGetLevel_BadValues() {
        Canonicalizer c = new Canonicalizer();
        assertThrows(FormatException.class, () -> c.getLevel("-1"));
        assertThrows(FormatException.class, () -> c.getLevel("lvl 1"));
        assertThrows(FormatException.class, () -> c.getLevel("one"));
        assertThrows(FormatException.class, () -> c.getLevel(""));
        assertThrows(FormatException.class, () -> c.getLevel("(1)"));
    }
}
