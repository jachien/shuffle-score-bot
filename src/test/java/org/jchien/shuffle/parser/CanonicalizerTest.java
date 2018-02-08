package org.jchien.shuffle.parser;

import org.jchien.shuffle.parser.exception.FormatException;
import org.jchien.shuffle.model.Item;
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

            s = "lvl" + String.valueOf(lvl);
            assertEquals(lvl, c.getLevel(s));

            // space between is okay
            s = "lv " + String.valueOf(lvl);
            assertEquals(lvl, c.getLevel(s));

            s = "lvl " + String.valueOf(lvl);
            assertEquals(lvl, c.getLevel(s));

            // multiple spaces between is okay
            s = "lv  " + String.valueOf(lvl);
            assertEquals(lvl, c.getLevel(s));

            s = "lvl  " + String.valueOf(lvl);
            assertEquals(lvl, c.getLevel(s));

            // case insensitive
            s = "LV " + String.valueOf(lvl);
            assertEquals(lvl, c.getLevel(s));

            s = "LVL " + String.valueOf(lvl);
            assertEquals(lvl, c.getLevel(s));
        }
    }

    @Test
    public void testGetLevel_BadValues() {
        Canonicalizer c = new Canonicalizer();
        assertThrows(FormatException.class, () -> c.getLevel("-1"));
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
                { null, null },
                { Arrays.asList(), Arrays.asList() },
                { Arrays.asList("m+5"), Arrays.asList(Item.MOVES_PLUS_5)},
                { Arrays.asList("m + 5"), Arrays.asList(Item.MOVES_PLUS_5)},
                { Arrays.asList("M+5"), Arrays.asList(Item.MOVES_PLUS_5)},
                { Arrays.asList("Moves +5"), Arrays.asList(Item.MOVES_PLUS_5)},
                { Arrays.asList("+5 moves"), Arrays.asList(Item.MOVES_PLUS_5)},
                { Arrays.asList("dd"), Arrays.asList(Item.DISRUPTION_DELAY)},
                { Arrays.asList("ms"), Arrays.asList(Item.MEGA_START)},
                { Arrays.asList("apu"), Arrays.asList(Item.ATTACK_POWER_UP)},
                { Arrays.asList("ap+"), Arrays.asList(Item.ATTACK_POWER_UP)},
                { Arrays.asList("c-1"), Arrays.asList(Item.COMPLEXITY_MINUS_1)},

                {
                    // no validation done even though t+10 and m+5 don't make sense together
                    Arrays.asList("+5 moves", "time +10"),
                    Arrays.asList(Item.MOVES_PLUS_5, Item.TIME_PLUS_10)
                },
                {
                    Arrays.asList("m+5", "dd"),
                    Arrays.asList(Item.MOVES_PLUS_5, Item.DISRUPTION_DELAY)
                },
                {
                    Arrays.asList("m+5", "ms", "dd"),
                    Arrays.asList(Item.MOVES_PLUS_5, Item.MEGA_START, Item.DISRUPTION_DELAY)
                },
                {
                    Arrays.asList("m+5", "apu"),
                    Arrays.asList(Item.MOVES_PLUS_5, Item.ATTACK_POWER_UP)
                },
                {
                    Arrays.asList("ms", "c-1"),
                    Arrays.asList(Item.MEGA_START, Item.COMPLEXITY_MINUS_1)
                },
                {
                    Arrays.asList("m+5", "ms", "dd", "ap+", "c-1", "jewel"),
                    Arrays.asList(Item.MOVES_PLUS_5,
                            Item.MEGA_START,
                            Item.DISRUPTION_DELAY,
                            Item.ATTACK_POWER_UP,
                            Item.COMPLEXITY_MINUS_1,
                            Item.JEWEL)
                },
                {
                    // multiple jewels allowed
                    Arrays.asList("m+5", "ms", "dd", "apu", "c-1", "jewel", "jewel", "jewel"),
                    Arrays.asList(Item.MOVES_PLUS_5,
                            Item.MEGA_START,
                            Item.DISRUPTION_DELAY,
                            Item.ATTACK_POWER_UP,
                            Item.COMPLEXITY_MINUS_1,
                            Item.JEWEL,
                            Item.JEWEL,
                            Item.JEWEL)
                },
        };

        for (Object[] test : tests) {
            List<String> input = (List<String>) test[0];
            List<Item> expected = (List<Item>) test[1];
            // order does matter
            // MoveType argument not important for this test
            assertEquals(expected, c.getItems(input));
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
            assertEquals(noItems, c.getItems(Arrays.asList(alias)));
            assertEquals(noItems, c.getItems(Arrays.asList(alias)));
        }
    }

    @Test
    public void testGetItems_ReturnOrderMatters() throws FormatException {
        Canonicalizer c = new Canonicalizer();

        Object[][] tests = {
                {
                        Arrays.asList("dd", "m+5"),
                        Arrays.asList(Item.MOVES_PLUS_5, Item.DISRUPTION_DELAY)
                },
                {
                        Arrays.asList("m+5", "dd", "ms"),
                        Arrays.asList(Item.MOVES_PLUS_5, Item.MEGA_START, Item.DISRUPTION_DELAY)
                },
                {
                        Arrays.asList("apu", "m+5"),
                        Arrays.asList(Item.MOVES_PLUS_5, Item.ATTACK_POWER_UP)
                },
                {
                        Arrays.asList("c-1", "ms"),
                        Arrays.asList(Item.MEGA_START, Item.COMPLEXITY_MINUS_1)
                },
                {
                        Arrays.asList("m+5", "dd", "ms", "ap+", "c-1", "jewel"),
                        Arrays.asList(Item.MOVES_PLUS_5,
                                Item.MEGA_START,
                                Item.DISRUPTION_DELAY,
                                Item.ATTACK_POWER_UP,
                                Item.COMPLEXITY_MINUS_1,
                                Item.JEWEL)
                },
                {
                        // multiple jewels allowed
                        Arrays.asList("jewel", "jewel", "jewel", "m+5", "ms", "dd", "apu", "c-1"),
                        Arrays.asList(Item.MOVES_PLUS_5,
                                Item.MEGA_START,
                                Item.DISRUPTION_DELAY,
                                Item.ATTACK_POWER_UP,
                                Item.COMPLEXITY_MINUS_1,
                                Item.JEWEL,
                                Item.JEWEL,
                                Item.JEWEL)
                },
        };

        for (Object[] test : tests) {
            List<String> input = (List<String>) test[0];
            List<Item> expected = (List<Item>) test[1];
            // order does matter
            // MoveType argument not important for this test
            assertEquals(expected, c.getItems(input));
        }
    }

    @Test
    public void testGetItems_BadItems() {
        Canonicalizer c = new Canonicalizer();

        String[][] inputs = {
                { "" },
                { "ms+5" },
                { null },
                { "m+5", null, "ms" },
                { "m+5", "full" },
                { "m+5", "none" },
                { "full", "m+5" },
                { "none", "m+5" },
                { "itemless", "m+5" },
        };

        for (String[] input : inputs) {
            assertThrows(FormatException.class, () -> c.getItems(Arrays.asList(input)));
        }
    }

    @Test
    public void testGetNonNegativeInteger() throws FormatException {
        Canonicalizer c = new Canonicalizer();
        Object[][] tests = {
                { null, null },
                { "0", 0 },
                { "123", 123 },
                { "123456", 123456 },
                { "100123456", 100123456 },
        };

        for (Object[] test : tests) {
            String input = (String) test[0];
            Integer expected = (Integer) test[1];
            assertEquals(expected, c.getNonNegativeInteger(input, ""));
        }
    }

    @Test
    public void testGetNonNegativeInteger_BadInputs() {
        Canonicalizer c = new Canonicalizer();
        String[] inputs = {
                "",
                "this is text",
                "123,456", // commas should be removed on initial comment parse
                "123.456", // periods should be removed on initial comment parse
                "123k", // no support for thousands abbreviation
                "12.3k", // no support for thousands abbreviation
                "2m", // no support for millions abbreviation (also nobody does this)
                "-123",
        };
        for (String input : inputs) {
            assertThrows(FormatException.class, () -> c.getNonNegativeInteger(input, ""));
        }
    }

    @Test
    public void testGetNotes() {
        Canonicalizer c = new Canonicalizer();

        String s = "";
        assertEquals(s, c.getNotes(s));

        s = "This is general form of a score link [123,456](http://www.google.com)";
        assertEquals(s, c.getNotes(s));

        s = "This is [a] free / form (notes) #section." +
                " You *should* be able to write **almost anything** here without causing trouble," +
                " including numbers like 0123456789!";
        assertEquals(s, c.getNotes(s));
    }

    @Test
    public void testGetNotes_Trim() {
        Canonicalizer c = new Canonicalizer();

        String input = " No untrimmed whitespace please ";
        String expected = "No untrimmed whitespace please";
        assertEquals(expected, c.getNotes(input));

        input = "  No untrimmed whitespace please  ";
        expected = "No untrimmed whitespace please";
        assertEquals(expected, c.getNotes(input));
    }

    @Test
    public void testGetNotes_NormalizeWhitespace() {
        Canonicalizer c = new Canonicalizer();

        String input = "Multiple  spaces   get normalized";
        String expected = "Multiple spaces get normalized";
        assertEquals(expected, c.getNotes(input));

        input = "Convert\tchars\nto\rspaces";
        expected = "Convert chars to spaces";
        assertEquals(expected, c.getNotes(input));

        input = "Convert\t\t\nchars \n\nto \r\n spaces";
        expected = "Convert chars to spaces";
        assertEquals(expected, c.getNotes(input));
    }
}
