package org.jchien.shuffle.parser;

import org.jchien.shuffle.model.FormatException;
import org.jchien.shuffle.model.MoveType;
import org.jchien.shuffle.model.StageType;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author jchien
 */

public class RunParserTest {
    private class ExpectedResult<T> {
        String message;
        T expected;
        Function<RawRunDetails, T> getter;

        public ExpectedResult(String message, T expected, Function<RawRunDetails, T> getter) {
            this.message = message;
            this.expected = expected;
            this.getter = getter;
        }

        public ExpectedResult(T expected, Function<RawRunDetails, T> getter) {
            this(null, expected, getter);
        }
    }

    private void testParse(String input, RawRunDetails expected) throws ParseException, FormatException {
        testParse(input, Arrays.asList(new ExpectedResult<>("input: " + input, expected, Function.identity())));
    }

    private <T> void testParse(String input, T expected, Function<RawRunDetails, T> getter) throws ParseException, FormatException {
        testParse(input, Arrays.asList(new ExpectedResult<>("input: " + input, expected, getter)));
    }

    private void testParse(String input, List<ExpectedResult<?>> expectedResults) throws ParseException, FormatException {
        RunParser p = new RunParser(new StringReader(input));
        p.start();
        RawRunDetails d = p.getDetails();

        for (ExpectedResult<?> er : expectedResults) {
            Object result = er.getter.apply(d);
            if (er.message != null) {
                assertEquals(er.expected, result, er.message);
            } else {
                assertEquals(er.expected, result);
            }
        }
    }

    @Test
    public void testBasic() throws ParseException, FormatException {
        String input = "!eb 100\n" +
                "notes: basic notes about the run\n" +
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

        List<RawPokemon> expected = Arrays.asList(
                new RawPokemon("mmy", null, "Lv1 Lv2", null, false)
        );

        testParse(input, expected, RawRunDetails::getTeam);
    }

    @Test
    public void testPokemonName() throws ParseException, FormatException {
        String input = "!comp team: diancie !end";

        List<RawPokemon> expected = Arrays.asList(
                new RawPokemon("diancie", null, null, null, false)
        );

        testParse(input, expected, RawRunDetails::getTeam);
    }

    @Test
    public void testPokemonMultiToken() throws ParseException, FormatException {
        String input = "!comp team: shiny mega charizard x !end";

        List<RawPokemon> expected = Arrays.asList(
                new RawPokemon("shiny mega charizard x", null, null, null, false)
        );

        testParse(input, expected, RawRunDetails::getTeam);
    }

    @Test
    public void testMultiplePokemon() throws ParseException, FormatException {
        String input = "!comp team: shiny diancie, azumarill !end";

        List<RawPokemon> expected = Arrays.asList(
                new RawPokemon("shiny diancie", null, null, null, false),
                new RawPokemon("azumarill", null, null, null, false)
        );

        testParse(input, expected, RawRunDetails::getTeam);
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
                "lvl15",
                "lvl 15",
                "lvl\t15",
        };

        for (String inputMiddle : inputs) {
            String input = inputPrefix + inputMiddle + inputSuffix;

            String expectedLevel = inputMiddle.replaceAll("\\s+", " ").trim();

            List<RawPokemon> expected = Arrays.asList(
                    new RawPokemon("ms-diancie", expectedLevel, null, null, false)
            );

            testParse(input, expected, RawRunDetails::getTeam);
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

            List<RawPokemon> expected = Arrays.asList(
                    new RawPokemon("ms-diancie", null, null, expectedMsus, false)
            );

            testParse(input, expected, RawRunDetails::getTeam);
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

            List<RawPokemon> expected = Arrays.asList(
                    new RawPokemon("ms-diancie", null, expectedSkill, null, false)
            );

            testParse(input, expected, RawRunDetails::getTeam);
        }
    }

    @Test
    public void testPokemonStats_AnyOrder() throws ParseException, FormatException {
        List<RawPokemon> expectedTeam = Arrays.asList(
                new RawPokemon("m-gengar", "10", null, "1/1", false)
        );

        testParse("!comp team: m-gengar (10, 1/1) !end", expectedTeam, RawRunDetails::getTeam);
        testParse("!comp team: m-gengar (1/1, 10) !end", expectedTeam, RawRunDetails::getTeam);

        expectedTeam = Arrays.asList(
                new RawPokemon("m-gengar", null, "power of 5", "1/1", false)
        );

        testParse("!comp team: m-gengar (power of 5, 1/1) !end", expectedTeam, RawRunDetails::getTeam);
        testParse("!comp team: m-gengar (1/1, power of 5) !end", expectedTeam, RawRunDetails::getTeam);
    }

    @Test
    public void testIsLevel() {
        RunParser p = new RunParser();

        // doesn't validate a pokemon can be the level, just that the String looks like a level
        for (int i=0; i <= 30; i++) {
            assertTrue(p.isLevel(Integer.toString(i)));
            assertTrue(p.isLevel("lv" + Integer.toString(i)));
            assertTrue(p.isLevel("Lv" + Integer.toString(i)));
            assertTrue(p.isLevel("Lvl" + Integer.toString(i)));
            assertTrue(p.isLevel("lv " + Integer.toString(i)));
            assertTrue(p.isLevel("Lv " + Integer.toString(i)));
            assertTrue(p.isLevel("Lvl " + Integer.toString(i)));
            // any additional whitespace should be truncated by the tokenization before isLevel is called
        }
        assertTrue(p.isLevel("01"));
        assertTrue(p.isLevel("lv01"));
        assertTrue(p.isLevel("lvl01"));
        assertTrue(p.isLevel("lv 01"));
        assertTrue(p.isLevel("lvl 01"));
        assertTrue(p.isLevel("123456"));
        assertTrue(p.isLevel("lv123456"));
        assertTrue(p.isLevel("lvl123456"));
        assertTrue(p.isLevel("lv 123456"));
        assertTrue(p.isLevel("lvl 123456"));

        String[] notLevels = new String[] {
            "sl1",
            "4 up",
            "4up",
            "lv1 lv2",
        };

        for (String notLevel : notLevels) {
            assertFalse(p.isLevel(notLevel));
        }
    }

    @Test
    public void testEBStage() throws ParseException, FormatException {
        String input = "!eb 150 team: a !end";
        testParse(input, Arrays.asList(
                new ExpectedResult<>("150", RawRunDetails::getStage),
                new ExpectedResult<>(StageType.ESCALATION_BATTLE, RawRunDetails::getStageType)
        ));
    }

    @Test
    public void testNormalStage() throws ParseException, FormatException {
        String input = "!run kommo-o team: a !end";
        testParse(input, Arrays.asList(
                new ExpectedResult<>("kommo-o", RawRunDetails::getStage),
                new ExpectedResult<>(StageType.NORMAL, RawRunDetails::getStageType)
        ));
    }

    @Test
    public void testNormalStage_MultiWord() throws ParseException, FormatException {
        String input = "!run Mr. Mime team: a !end";
        testParse(input, Arrays.asList(
                new ExpectedResult<>("Mr. Mime", RawRunDetails::getStage),
                new ExpectedResult<>(StageType.NORMAL, RawRunDetails::getStageType)
        ));
    }

    @Test
    public void testCompStage() throws ParseException, FormatException {
        String input = "!comp team: a !end";
        testParse(input, Arrays.asList(
                new ExpectedResult<>(null, RawRunDetails::getStage),
                new ExpectedResult<>(StageType.COMPETITION, RawRunDetails::getStageType)
        ));
    }

    @Test
    public void testItems_Single() throws ParseException, FormatException {
        String input = "!run foo items: m+5 !end";
        testParse(input, Arrays.asList("m+5"), RawRunDetails::getItems);
    }

    @Test
    public void testItems_Multiple() throws ParseException, FormatException {
        String input = "!run foo items: m+5, dd, apu !end";
        testParse(input, Arrays.asList("m+5", "dd", "apu"), RawRunDetails::getItems);
    }

    @Test
    public void testItems_MultiWord() throws ParseException, FormatException {
        String input = "!run foo items: moves +5 !end";
        testParse(input, Arrays.asList("moves +5"), RawRunDetails::getItems);
    }

    @Test
    public void testMovesLeft() throws ParseException, FormatException {
        String input = "!eb 100 moves left: 1 !end";
        testParse(input, Arrays.asList(
                new ExpectedResult<>("1", RawRunDetails::getMovesLeft),
                new ExpectedResult<>(MoveType.MOVES, RawRunDetails::getMoveType)
        ));
    }

    @Test
    public void testMovesLeft_MultiLine() throws ParseException, FormatException {
        String input = "!eb 100 moves\nleft: 1 !end";
        testParse(input, Arrays.asList(
                new ExpectedResult<>("1", RawRunDetails::getMovesLeft),
                new ExpectedResult<>(MoveType.MOVES, RawRunDetails::getMoveType)
        ));
    }

    @Test
    public void testTimeLeft() throws ParseException, FormatException {
        String input = "!eb 100 time left: 1 !end";
        testParse(input, Arrays.asList(
                new ExpectedResult<>("1", RawRunDetails::getTimeLeft),
                new ExpectedResult<>(MoveType.TIME, RawRunDetails::getMoveType)
        ));
    }

    @Test
    public void testScore() throws ParseException, FormatException {
        String input = "!eb 100 score: 123456 !end";
        // comma isn't part of word production, so it's not part of the parsed score
        testParse(input, "123456", RawRunDetails::getScore);
    }

    @Test
    public void testScore_WithComma() throws ParseException, FormatException {
        String input = "!eb 100 score: 123,456 !end";
        // comma isn't part of word production, so it's not part of the parsed score
        testParse(input, "123456", RawRunDetails::getScore);
    }

    @Test
    public void testScore_WithPeriod() throws ParseException, FormatException {
        String input = "!eb 100 score: 123.456 !end";
        // period is part of the word production, so it's included in the parsed score
        testParse(input, "123.456", RawRunDetails::getScore);
    }

    @Test
    public void testSkipChars_Asterisk() throws ParseException, FormatException {
        String input = "!eb 100 team: pidgey, *blank* !end";
        List<RawPokemon> expected = Arrays.asList(
                new RawPokemon("pidgey", null, null, null, false),
                new RawPokemon("blank", null, null, null, false)
        );
        testParse(input, expected, RawRunDetails::getTeam);
    }

    @Test
    public void testSkipChars_Hash() throws ParseException, FormatException {
        String input = "!eb 100 #team: pidgey, blank !end";
        List<RawPokemon> expected = Arrays.asList(
                new RawPokemon("pidgey", null, null, null, false),
                new RawPokemon("blank", null, null, null, false)
        );
        testParse(input, expected, RawRunDetails::getTeam);
    }

    @Test
    public void testUnicodeSpaces() throws ParseException, FormatException {
        String input =
                "!eb\u00a065  \n" +
                "team:\u00a0MDiance-S,\u00a0Hoopa-U,\u00a0Dusknoir,\u00a0Genesect-S  \n" +
                "moves\u00a0left:\u00a02  \n" +
                "items:\u00a0none  \n" +
                "!end";

        List<RawPokemon> expectedTeam = Arrays.asList(
                new RawPokemon("MDiance-S", null, null, null, false),
                new RawPokemon("Hoopa-U", null, null, null, false),
                new RawPokemon("Dusknoir", null, null, null, false),
                new RawPokemon("Genesect-S", null, null, null, false)
        );

        testParse(input, Arrays.asList(
                new ExpectedResult<>(StageType.ESCALATION_BATTLE, RawRunDetails::getStageType),
                new ExpectedResult<>("65", RawRunDetails::getStage),
                new ExpectedResult<>(expectedTeam, RawRunDetails::getTeam),
                new ExpectedResult<>("2", RawRunDetails::getMovesLeft),
                new ExpectedResult<>(Arrays.asList("none"), RawRunDetails::getItems)
        ));
    }

    @Test
    public void testNotes() throws ParseException, FormatException {
        String input = "!comp" +
                " notes: This is [a] free / form (notes) #section." +
                " You *should* be able to write **almost anything** here without causing trouble," +
                " including numbers like 0123456789!" +
                " !end";
        testParse(input, Arrays.asList());
    }

    @Test
    public void testNotes_WorksWithOtherSections() throws ParseException, FormatException {
        String input = "!comp" +
                " notes: This is [a] free / form (notes) #section." +
                " team: a"+
                " !end";
        testParse(input, Arrays.asList(new RawPokemon("a", null, null, null, false)), RawRunDetails::getTeam);

        input = "!comp" +
                " notes: This is [a] free / form (notes) #section." +
                " items: none"+
                " !end";
        testParse(input, Arrays.asList("none"), RawRunDetails::getItems);

        input = "!comp" +
                " notes: This is [a] free / form (notes) #section." +
                " score: 100"+
                " !end";
        testParse(input, "100", RawRunDetails::getScore);

        input = "!comp" +
                " notes: This is [a] free / form (notes) #section." +
                " moves left: 5"+
                " !end";
        testParse(input, "5", RawRunDetails::getMovesLeft);

        input = "!comp" +
                " notes: This is [a] free / form (notes) #section." +
                " time left: 5"+
                " !end";
        testParse(input, "5", RawRunDetails::getTimeLeft);
    }
}
