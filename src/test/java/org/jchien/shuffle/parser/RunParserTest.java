package org.jchien.shuffle.parser;

import org.jchien.shuffle.model.FormatException;
import org.jchien.shuffle.model.MoveType;
import org.jchien.shuffle.model.StageType;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.*;

/**
 * @author jchien
 */

public class RunParserTest {
    private final List<RawPokemon> EMPTY_TEAM = Collections.EMPTY_LIST;
    private final List<String> EMPTY_ITEMS = Collections.EMPTY_LIST;

    @Test
    public void testBasic() throws ParseException, FormatException {
        String input = "!eb 100\n" +
                "team: mmy (Lv10, sl5 power of 4, 14/14), unown! (sl1), necrozma (Lv1), silvally (perfect)\n" +
                "items: m+5, dd\n" +
                "moves left: 5\n" +
                "!end\n";

        List<RawPokemon> team = Arrays.asList(
                new RawPokemon("mmy", "Lv10", "sl5 power of 4", "14/14", false),
                new RawPokemon("unown!", null, "sl1", null, false),
                new RawPokemon("necrozma", "Lv1", null, null, false),
                new RawPokemon("silvally", null, null, null, true)
        );

        List<String> items = Arrays.asList(
                "m+5",
                "dd"
        );

        RawRunDetails expected = new RawRunDetails(
                team,
                items,
                null,
                "100",
                "5",
                null,
                StageType.ESCALATION_BATTLE,
                MoveType.MOVES
        );

        testParse(input, expected);
    }

    @Test
    public void testSkillAmbiguity() throws ParseException, FormatException {
        String input = "!comp\n" +
                "team: mmy (Lv1 Lv2)\n" +
                "!end\n";

        List<RawPokemon> team = Arrays.asList(
                new RawPokemon("mmy", null, "Lv1 Lv2", null, false)
        );

        RawRunDetails expected = new RawRunDetails(
                team,
                EMPTY_ITEMS,
                null,
                null,
                null,
                null,
                StageType.COMPETITION,
                MoveType.MOVES
        );

        testParse(input, expected);
    }

    @Test
    public void testPokemonName() throws ParseException, FormatException {
        String input = "!comp team: diancie !end";

        List<RawPokemon> team = Arrays.asList(
                new RawPokemon("diancie", null, null, null, false)
        );

        RawRunDetails expected = new RawRunDetails(
                team,
                EMPTY_ITEMS,
                null,
                null,
                null,
                null,
                StageType.COMPETITION,
                MoveType.MOVES
        );

        testParse(input, expected);
    }

    @Test
    public void testPokemonMultiToken() throws ParseException, FormatException {
        String input = "!comp team: shiny mega charizard x !end";

        List<RawPokemon> team = Arrays.asList(
                new RawPokemon("shiny mega charizard x", null, null, null, false)
        );

        RawRunDetails expected = new RawRunDetails(
                team,
                EMPTY_ITEMS,
                null,
                null,
                null,
                null,
                StageType.COMPETITION,
                MoveType.MOVES
        );

        testParse(input, expected);
    }

    @Test
    public void testMultiplePokemon() throws ParseException, FormatException {
        String input = "!comp team: shiny diancie, azumarill !end";

        List<RawPokemon> team = Arrays.asList(
                new RawPokemon("shiny diancie", null, null, null, false),
                new RawPokemon("azumarill", null, null, null, false)
        );

        RawRunDetails expected = new RawRunDetails(
                team,
                EMPTY_ITEMS,
                null,
                null,
                null,
                null,
                StageType.COMPETITION,
                MoveType.MOVES
        );

        testParse(input, expected);
    }

    @Test
    public void testLevel() throws ParseException, FormatException {
        String inputPrefix = "!comp team: ms-diancie (";
        String inputSuffix = ") !end";
        String[] inputs = {
                "15",
                "15",
                "  15\n  ",
                " 15",
                "15 ",
                "lv15",
                " lv15 ",
                "lv 15",
                " Lv 15 ",
                "lv  15",
                "lv\t15",
                "Lv15",
                "LV 15",
                "LV  15",
                "lV\t15",
        };

        for (String inputMiddle : inputs) {
            String input = inputPrefix + inputMiddle + inputSuffix;

            String expectedLevel = inputMiddle.replaceAll("\\s+", " ").trim();

            List<RawPokemon> team = Arrays.asList(
                    new RawPokemon("ms-diancie", expectedLevel, null, null, false)
            );

            RawRunDetails expected = new RawRunDetails(
                    team,
                    EMPTY_ITEMS,
                    null,
                    null,
                    null,
                    null,
                    StageType.COMPETITION,
                    MoveType.MOVES
            );

            testParse(input, expected);
        }
    }

    @Test
    public void testMsus() throws ParseException, FormatException {
        String inputPrefix = "!comp team: ms-diancie (";
        String inputSuffix = ") !end";
        String[] inputs = {
                "1/1",
                "1 / 1",
                "1 /1",
                "1/ 1",
                " 1/1 ",
                "10/15",
                "15/1",
                "\t15/1\n",
        };

        for (String inputMiddle : inputs) {
            String input = inputPrefix + inputMiddle + inputSuffix;

            String expectedMsus = inputMiddle.replaceAll("\\s+", " ")
                    .replaceAll("\\s?/\\s?", "/")
                    .trim();

            List<RawPokemon> team = Arrays.asList(
                    new RawPokemon("ms-diancie", null, null, expectedMsus, false)
            );

            RawRunDetails expected = new RawRunDetails(
                    team,
                    EMPTY_ITEMS,
                    null,
                    null,
                    null,
                    null,
                    StageType.COMPETITION,
                    MoveType.MOVES
            );

            testParse(input, expected);
        }
    }

    @Test
    public void testSkill() throws ParseException, FormatException {
        String inputPrefix = "!comp team: ms-diancie (";
        String inputSuffix = ") !end";
        String[] inputs = {
                "sl5",
                "sl5 rock shot",
                "sl4",
                "sl 3",
                "rock shot",
                "4 up",
                " 4 up\n ",
                "demolish",
        };

        for (String inputMiddle : inputs) {
            String input = inputPrefix + inputMiddle + inputSuffix;

            String expectedSkill = inputMiddle.replaceAll("\\s+", " ")
                    .trim();

            List<RawPokemon> team = Arrays.asList(
                    new RawPokemon("ms-diancie", null, expectedSkill, null, false)
            );

            RawRunDetails expected = new RawRunDetails(
                    team,
                    EMPTY_ITEMS,
                    null,
                    null,
                    null,
                    null,
                    StageType.COMPETITION,
                    MoveType.MOVES
            );

            testParse(input, expected);
        }
    }

    private void testParse(String input, RawRunDetails expected) throws ParseException, FormatException {
        RunParser p = new RunParser(new StringReader(input));
        p.start();
        RawRunDetails d = p.getDetails();
        assertEquals("input: " + input, expected, d);
    }

    @Test
    public void testIsLevel() {
        RunParser p = new RunParser();

        // doesn't validate a pokemon can be the level, just that the String looks like a level
        for (int i=0; i <= 30; i++) {
            assertTrue(p.isLevel(new StringBuilder(Integer.toString(i))));
            assertTrue(p.isLevel(new StringBuilder("lv" + Integer.toString(i))));
            assertTrue(p.isLevel(new StringBuilder("Lv" + Integer.toString(i))));
            assertTrue(p.isLevel(new StringBuilder("lv " + Integer.toString(i))));
            assertTrue(p.isLevel(new StringBuilder("Lv " + Integer.toString(i))));
            // any additional whitespace should be truncated by the tokenization before isLevel is called
        }
        assertTrue(p.isLevel(new StringBuilder("01")));
        assertTrue(p.isLevel(new StringBuilder("lv01")));
        assertTrue(p.isLevel(new StringBuilder("lv 01")));
        assertTrue(p.isLevel(new StringBuilder("123456")));
        assertTrue(p.isLevel(new StringBuilder("lv123456")));
        assertTrue(p.isLevel(new StringBuilder("lv 123456")));

        String[] notLevels = new String[] {
            "sl1",
            "4 up",
            "4up",
            "lv1 lv2",
        };

        for (String notLevel : notLevels) {
            assertFalse(p.isLevel(new StringBuilder(notLevel)));
        }
    }
}
