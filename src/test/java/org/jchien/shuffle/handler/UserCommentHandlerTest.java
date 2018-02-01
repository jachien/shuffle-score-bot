package org.jchien.shuffle.handler;

import org.jchien.shuffle.handler.UserCommentHandler;
import org.jchien.shuffle.model.Stage;
import org.jchien.shuffle.model.StageType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author jchien
 */
public class UserCommentHandlerTest {

    private static final String[] RUN_PATTERN_INPUTS = {
            "!comp !end",
            "!comp team: a !end",
            "!run meowth !end",
            "!run meowth team: a !end",
            "!eb 50 !end",
            "!eb 50 team: a !end",
    };

    private static final String[] RUN_PATTERN_LINE_BREAK_INPUTS = {
            "!comp\nteam: a\n!end",
            "\n!comp\nteam: a\n!end\n",
            "\n!run meowth team: a !end\n",
            "\n!run meowth\nteam: a\n!end\n",
            "\n!run\nmeowth\nteam: a\n!end\n",
            "\n!eb 50 team: a !end\n",
            "\n!eb 50\nteam: a\n!end\n",
            "\n!eb\n50\nteam: a\n!end\n",
    };

    @Test
    public void testRunPattern_Basic() {
        Stream.concat(Arrays.stream(RUN_PATTERN_INPUTS), Arrays.stream(RUN_PATTERN_LINE_BREAK_INPUTS)).peek(
            input -> {
                Matcher m = UserCommentHandler.PATTERN.matcher(input);
                assertTrue(m.find());
                assertEquals(0, m.start());
                assertFalse(m.find());
            }
        );
    }

    @Test
    public void testRunPattern_MidComment() {
        Stream.concat(Arrays.stream(RUN_PATTERN_INPUTS), Arrays.stream(RUN_PATTERN_LINE_BREAK_INPUTS)).peek(
                runDetails -> {
                    String input = "foo " + runDetails + " bar";
                    Matcher m = UserCommentHandler.PATTERN.matcher(input);
                    assertTrue(m.find());

                    // this is 3 and not 4 because we're simulating word boundary as (^|\\s)
                    assertEquals(3, m.start());

                    assertFalse(m.find());
                }
        );
    }

    @Test
    public void testRunPattern_Multiple() {
        Stream.concat(Arrays.stream(RUN_PATTERN_INPUTS), Arrays.stream(RUN_PATTERN_LINE_BREAK_INPUTS)).peek(
                runDetails -> {
                    String input = "foo " + runDetails + " bar " + runDetails + " baz";
                    Matcher m = UserCommentHandler.PATTERN.matcher(input);
                    assertTrue(m.find());
                    assertTrue(m.find());
                    assertFalse(m.find());
                }
        );
    }

    @Test
    public void testRunPattern_NoMatch() {
        for (String runDetails : RUN_PATTERN_INPUTS) {
            String input = "foo" + runDetails + "bar";
            Matcher m = UserCommentHandler.PATTERN.matcher(input);
            assertFalse(m.find());
        }
    }

    @Test
    public void testRunPattern_NoMatchMarkdownCodeWithoutSpace() {
        // maybe this should match, but I'd like inlined code (e.g. `!comp`) to not trigger the bot
        for (String runDetails : RUN_PATTERN_INPUTS) {
            String input = "`" + runDetails + "`";
            Matcher m = UserCommentHandler.PATTERN.matcher(input);
            assertFalse(m.find());
        }
    }

    @Test
    public void testRunPattern_MatchMarkdownCodeWithSpace() {
        Stream.concat(Arrays.stream(RUN_PATTERN_INPUTS), Arrays.stream(RUN_PATTERN_LINE_BREAK_INPUTS)).peek(
                runDetails -> {
                    String input = "` " + runDetails + " `";
                    Matcher m = UserCommentHandler.PATTERN.matcher(input);
                    assertTrue(m.find());
                }
        );
    }
}
