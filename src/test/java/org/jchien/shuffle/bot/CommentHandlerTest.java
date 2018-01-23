package org.jchien.shuffle.bot;

import org.jchien.shuffle.model.Stage;
import org.jchien.shuffle.model.StageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
