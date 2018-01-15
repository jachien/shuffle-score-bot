package org.jchien.shuffle.bot;

import org.jchien.shuffle.model.FormatException;
import org.jchien.shuffle.model.Item;
import org.jchien.shuffle.model.MoveType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

            // space before and after ok
            s = " sl " + i + " ";
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

    @Test
    public void testGetSkillName_EmptyStringsReturnNull() {
        Canonicalizer c = new Canonicalizer();

        String[] inputs = {
                "",
                " ",
                "  ",
                "\t",
                "\t ",
                "\n",
        };

        for (String input : inputs) {
            assertNull(c.getSkillName(input));
        }
    }

    @Test
    public void testGetSkillName_NormalizeWhitespace() {
        Canonicalizer c = new Canonicalizer();

        String[][] tests = {
                { " 4 up  ", "4 up" },
                { "4  up", "4 up" },
                { "4\tup", "4 up" },
                { " 4\tup  ", "4 up" },
                { " 4\nup  ", "4 up" },
                { "\tdemolish\n", "demolish" },
        };

        for (String[] test : tests) {
            String input = test[0];
            String expected = test[1];
            assertEquals(expected, c.getSkillName(input));
        }
    }

    @Test
    public void testGetSkillName_Prefix() {
        Canonicalizer c = new Canonicalizer();

        String[][] tests = {
                { "demolish sl 1", "demolish" },
                { "Shot Out SL5", "Shot Out" },
                { "Shot Out  SL5", "Shot Out" },
                { "Shot Out \tSL5", "Shot Out" },
        };

        for (String[] test : tests) {
            String input = test[0];
            String expected = test[1];
            assertEquals(expected, c.getSkillName(input));
        }
    }

    @Test
    public void testGetSkillName_Suffix() {
        Canonicalizer c = new Canonicalizer();

        String[][] tests = {
                { "sl 1 demolish", "demolish" },
                { "SL5 Shot Out", "Shot Out" },
                { "SL5  Shot Out", "Shot Out" },
                { "SL5 \tShot Out", "Shot Out" },
        };

        for (String[] test : tests) {
            String input = test[0];
            String expected = test[1];
            assertEquals(expected, c.getSkillName(input));
        }
    }

    @Test
    public void testGetSkillLevel_MiddleShouldFail() {
        Canonicalizer c = new Canonicalizer();

        String[] inputs = {
                "a sl 1 b",
                "a sl1 b",
                "shot sl5 out",
        };

        for (String input : inputs) {
            assertThrows(FormatException.class, () -> c.getSkillLevel(input));
        }
    }

    @Test
    public void testGetSkillLevel_MultipleShouldFail() {
        Canonicalizer c = new Canonicalizer();

        String[] inputs = {
                "sl1 sl2",
                "sl1 sl 2",
                "sl 1 sl2",
                "sl 1 sl 2",
                "sl1 makes no sense sl2",
                "sl1 sl2 makes no sense",
                "sl1 makes no sl2 sense",
                "sl1 sl2 sl3",
                "sl1 makes no sl2 sense sl3",
        };

        for (String input : inputs) {
            assertThrows(FormatException.class, () -> c.getSkillLevel(input));
        }
    }

    @Test
    public void testGetMsuCount() throws FormatException {
        Canonicalizer c = new Canonicalizer();
        Object[][] tests = {
                { null, null },
                { "0/3", 0 },
                { "1/3", 1 },
                { "2/3", 2 },
                { "3/3", 3 },
                { "0/20", 0 },
                { "0 / 20", 0 },
                { "14/20", 14 },
                { "14 / 20", 14 },
                { " 14\t  /\t20 ", 14 },
                { "20/20", 20 },
                { "20 / 20", 20 },
                { "000 / 20", 0 },
        };

        for (Object[] test : tests) {
            String input = (String) test[0];
            Integer expected = (Integer) test[1];
            assertEquals(expected, c.getMsuCount(input));
        }
    }

    @Test
    public void testGetMsuCount_ShouldFail() {
        Canonicalizer c = new Canonicalizer();
        String[] inputs = {
                "",
                "1",
                "14",
                "a/b",
                "a/1",
                "1/a",
                "I am 12 and what is this / 12",
        };

        for (String input : inputs) {
            assertThrows(FormatException.class, () -> c.getMsuCount(input));
        }
    }

    @Test
    public void testGetMaxMsus() throws FormatException {
        Canonicalizer c = new Canonicalizer();
        Object[][] tests = {
                { null, null },
                { "0/3", 3 },
                { "1/3", 3 },
                { "2/3", 3 },
                { "3/3", 3 },
                { "0/20", 20 },
                { "0 / 20", 20 },
                { "14/20", 20 },
                { "14 / 20", 20 },
                { " 14\t  /\t20 ", 20 },
                { "20/20", 20 },
                { "20 / 20", 20 },
                { "000 / 20", 20 },
        };

        for (Object[] test : tests) {
            String input = (String) test[0];
            Integer expected = (Integer) test[1];
            assertEquals(expected, c.getMaxMsus(input));
        }
    }

    @Test
    public void testGetMaxMsus_ShouldFail() {
        Canonicalizer c = new Canonicalizer();
        String[] inputs = {
                "",
                "1",
                "14",
                "a/b",
                "a/1",
                "1/a",
                "I am 12 and what is this / 12",
        };

        for (String input : inputs) {
            assertThrows(FormatException.class, () -> c.getMaxMsus(input));
        }
    }

    @Test
    public void testGetItems() throws FormatException {
        Canonicalizer c = new Canonicalizer();

        Object[][] tests = {
                { MoveType.MOVES, null, null },
                { MoveType.MOVES, Arrays.asList(), Arrays.asList() },
                { MoveType.MOVES, Arrays.asList("m+5"), Arrays.asList(Item.MOVES_PLUS_5)},
                { MoveType.MOVES, Arrays.asList("m + 5"), Arrays.asList(Item.MOVES_PLUS_5)},
                { MoveType.MOVES, Arrays.asList("M+5"), Arrays.asList(Item.MOVES_PLUS_5)},
                { MoveType.MOVES, Arrays.asList("Moves +5"), Arrays.asList(Item.MOVES_PLUS_5)},
                { MoveType.MOVES, Arrays.asList("+5 moves"), Arrays.asList(Item.MOVES_PLUS_5)},
        };

        for (Object[] test : tests) {
            MoveType moveType = (MoveType) test[0];
            List<String> input = (List<String>) test[1];
            List<Item> expected = (List<Item>) test[2];
            // order does matter
            assertEquals(expected, c.getItems(input, moveType));
        }
    }

    @Test
    public void testGetItems_None() throws FormatException {
        Canonicalizer c = new Canonicalizer();

        String[] noneAliases = {
                "none",
                "itemless",
                "no items",
        };

        final List<Item> noItems = new ArrayList<>();

        for (String alias : noneAliases) {
            assertEquals(noItems, c.getItems(Arrays.asList(alias), MoveType.MOVES));
            assertEquals(noItems, c.getItems(Arrays.asList(alias), MoveType.TIME));
        }
    }

    @Test
    public void testGetItems_Full() throws FormatException {
        Canonicalizer c = new Canonicalizer();

        String[] fullAliases = {
                "full",
                "full items",
                "full item run",
        };

        final List<Item> fullItemsMoves = new ArrayList<>(Item.getFullItems(MoveType.MOVES));
        final List<Item> fullItemsTime = new ArrayList<>(Item.getFullItems(MoveType.TIME));

        for (String alias : fullAliases) {
            assertEquals(fullItemsMoves, c.getItems(Arrays.asList(alias), MoveType.MOVES));
            assertEquals(fullItemsTime, c.getItems(Arrays.asList(alias), MoveType.TIME));
        }
    }
}
