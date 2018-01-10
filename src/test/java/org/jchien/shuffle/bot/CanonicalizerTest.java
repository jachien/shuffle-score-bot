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
    public void testGetLevel_Null() throws FormatException {
        Canonicalizer c = new Canonicalizer();
        assertNull(c.getLevel(null));
    }

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

    @Test
    public void testGetSkillLevel_Basic() throws FormatException {
        Canonicalizer c = new Canonicalizer();
        assertNull(c.getSkillLevel(null));

        for (int i=1; i <= 5; i++) {
            String s = "sl" + i;
            assertEquals(Integer.valueOf(i), c.getSkillLevel(s));

            // space ok
            s = "sl " + i;
            assertEquals(Integer.valueOf(i), c.getSkillLevel(s));

            // multiple spaces ok
            s = "sl  " + i;
            assertEquals(Integer.valueOf(i), c.getSkillLevel(s));

            // case insensitive
            s = "SL " + i;
            assertEquals(Integer.valueOf(i), c.getSkillLevel(s));
        }
    }

    @Test
    public void testGetSkillLevel_BasicBadValues() {
        Canonicalizer c = new Canonicalizer();

        assertThrows(FormatException.class, () -> c.getSkillLevel("sl0"));

        for (int i=6; i <= 15; i++) {
            final String s = "sl" + i;
            assertThrows(FormatException.class, () -> c.getSkillLevel(s));
        }
    }

    @Test
    public void testGetSkillLevel_Prefix() throws FormatException {
        Canonicalizer c = new Canonicalizer();
        for (int i=1; i <= 5; i++) {
            String s = "sl" + i + " ss";
            assertEquals(Integer.valueOf(i), c.getSkillLevel(s));

            // space ok
            s = "sl " + i + " ss";
            assertEquals(Integer.valueOf(i), c.getSkillLevel(s));

            // multiple spaces ok
            s = "sl  " + i + " ss";
            assertEquals(Integer.valueOf(i), c.getSkillLevel(s));

            // case insensitive
            s = "SL " + i + " ss";
            assertEquals(Integer.valueOf(i), c.getSkillLevel(s));
        }
    }

    @Test
    public void testGetSkillLevel_Suffix() throws FormatException {
        Canonicalizer c = new Canonicalizer();
        for (int i=1; i <= 5; i++) {
            String s = "ss sl" + i;
            assertEquals(Integer.valueOf(i), c.getSkillLevel(s));

            // space ok
            s = "ss sl " + i;
            assertEquals(Integer.valueOf(i), c.getSkillLevel(s));

            // multiple spaces ok
            s = "ss sl  " + i;
            assertEquals(Integer.valueOf(i), c.getSkillLevel(s));

            // case insensitive
            s = "ss SL " + i;
            assertEquals(Integer.valueOf(i), c.getSkillLevel(s));
        }
    }

    @Test
    public void testGetSkillLevel_Middle() throws FormatException {
        Canonicalizer c = new Canonicalizer();
        for (int i=1; i <= 5; i++) {
            String s = "ss sl" + i + " shot out";
            assertEquals(Integer.valueOf(i), c.getSkillLevel(s));

            // space ok
            s = "ss sl " + i + " shot out";
            assertEquals(Integer.valueOf(i), c.getSkillLevel(s));

            // multiple spaces ok
            s = "ss sl  " + i + " shot out";
            assertEquals(Integer.valueOf(i), c.getSkillLevel(s));

            // case insensitive
            s = "ss SL " + i + " shot out";
            assertEquals(Integer.valueOf(i), c.getSkillLevel(s));
        }
    }

    @Test
    public void testGetSkillLevel_Omitted() throws FormatException {
        Canonicalizer c = new Canonicalizer();
        String[] inputs = {
                "4 up",
                "4up",
                "sl",
                "sleep charm",
                "power of 5",
                "sl5butnospace",
        };

        for (String input : inputs) {
            assertNull(c.getSkillLevel(input));
        }
    }

    @Test
    public void testGetSkillName_Basic() {
        Canonicalizer c = new Canonicalizer();
        assertNull(c.getSkillName(null));

        String[] inputs = {
                "4 up",
                "4up",
                "sl",
                "sleep charm",
                "power of 5",
                "sl5butnospace",
        };

        for (String input : inputs) {
            assertEquals(input, c.getSkillName(input));
        }
    }
}
