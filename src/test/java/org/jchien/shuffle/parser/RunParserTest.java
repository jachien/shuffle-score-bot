package org.jchien.shuffle.parser;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.*;

/**
 * @author jchien
 */

public class RunParserTest {
    @Test
    public void testBasic() throws ParseException, DupeSectionException {
        String input = "!eb 100\n" +
                "team: mmy (Lv10, sl5 power of 4, 14/14), unown! (sl1), necrozma (Lv1), silvally (perfect)\n" +
                "items: m+5, dd\n" +
                "moves left: 5\n" +
                "!end\n";

        RunParser p = new RunParser(new StringReader(input));
        p.start();
        RawRunDetails d = p.getDetails();

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
                "5"
        );

        assertEquals(expected, d);
    }

    @Test
    public void testSkillAmbiguity() throws ParseException, DupeSectionException {
        String input = "!run\n" +
                "team: mmy (Lv1 Lv2)\n" +
                "!end\n";

        RunParser p = new RunParser(new StringReader(input));
        p.start();
        RawRunDetails d = p.getDetails();

        List<RawPokemon> team = Arrays.asList(
                new RawPokemon("mmy", null, "Lv1 Lv2", null, false)
        );

        List<String> items = Arrays.asList(
        );

        RawRunDetails expected = new RawRunDetails(
                team,
                items,
                null,
                null,
                null
        );

        assertEquals(expected, d);
    }
}
