package org.jchien.shuffle.bot;

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
public class CommentHandlerTest {
    @Test
    public void testGetAggregateStage_CompetitionStage() {
        CommentHandler ch = new CommentHandler();
        String comment = "###Competition Runs\n" +
                "\n" +
                "Username | Team | Items | Score\n" +
                "|:----------: | :----------: | :-----------: | :-----------:\n" +
                "/u/jcrixus |  | Moves +5, MS, DD, APU, C-1 | [250,123](https://www.reddit.com/r/shufflescorebottest/comments/7ncsuk/test/ds42v9w)\n";
        Stage expected = new Stage(StageType.COMPETITION, null);
        Stage actual = ch.getAggregateStage(comment);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetAggregateStage_EBStage() {
        CommentHandler ch = new CommentHandler();
        String comment = "###Stage 50\n" +
                "\n" +
                "Username | Team | Items | Result\n" +
                "|:----------: | :----------: | :-----------: | :-----------:\n" +
                "/u/jcrixus | vanilluxe (15, SL5), rayquaza (10, SL5), azumarill (perfect) |  | [Unknown](https://www.reddit.com/r/shufflescorebottest/comments/7ncsuk/test/ds4bny2)\n";
        Stage expected = new Stage(StageType.ESCALATION_BATTLE, "50");
        Stage actual = ch.getAggregateStage(comment);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetAggregateStage_NormalStage() {
        CommentHandler ch = new CommentHandler();
        String comment = "###Stage Meowth\n" +
                "\n" +
                "Username | Team | Items | Result\n" +
                "|:----------: | :----------: | :-----------: | :-----------:\n" +
                "/u/jcrixus | vanilluxe (15, SL5), rayquaza (10, SL5), azumarill (perfect) |  | [Unknown](https://www.reddit.com/r/shufflescorebottest/comments/7ncsuk/test/ds4bny2)\n";
        Stage expected = new Stage(StageType.NORMAL, "meowth");
        Stage actual = ch.getAggregateStage(comment);
        assertEquals(expected, actual);
    }

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
                Matcher m = CommentHandler.PATTERN.matcher(input);
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
                    Matcher m = CommentHandler.PATTERN.matcher(input);
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
                    Matcher m = CommentHandler.PATTERN.matcher(input);
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
            Matcher m = CommentHandler.PATTERN.matcher(input);
            assertFalse(m.find());
        }
    }

    @Test
    public void testRunPattern_NoMatchMarkdownCodeWithoutSpace() {
        // maybe this should match, but I'd like inlined code (e.g. `!comp`) to not trigger the bot
        for (String runDetails : RUN_PATTERN_INPUTS) {
            String input = "`" + runDetails + "`";
            Matcher m = CommentHandler.PATTERN.matcher(input);
            assertFalse(m.find());
        }
    }

    @Test
    public void testRunPattern_MatchMarkdownCodeWithSpace() {
        Stream.concat(Arrays.stream(RUN_PATTERN_INPUTS), Arrays.stream(RUN_PATTERN_LINE_BREAK_INPUTS)).peek(
                runDetails -> {
                    String input = "` " + runDetails + " `";
                    Matcher m = CommentHandler.PATTERN.matcher(input);
                    assertTrue(m.find());
                }
        );
    }
}
