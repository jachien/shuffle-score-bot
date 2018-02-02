package org.jchien.shuffle.handler;

import org.jchien.shuffle.model.Stage;
import org.jchien.shuffle.model.StageType;
import org.jchien.shuffle.model.TablePartId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author jchien
 */
public class BotCommentHandlerTest {
    @Test
    public void testGetTablePartId_CompetitionStage() {
        String comment = "###Competition Runs\n" +
                "\n" +
                "Username | Team | Items | Score\n" +
                "|:----------: | :----------: | :-----------: | :-----------:\n" +
                "/u/jcrixus |  | Moves +5, MS, DD, APU, C-1 | [250,123](https://www.reddit.com/r/shufflescorebottest/comments/7ncsuk/test/ds42v9w)\n";
        TablePartId expected = new TablePartId(new Stage(StageType.COMPETITION, null), 0);
        TablePartId actual = BotCommentHandler.getTablePartId(comment);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetTablePartId_CompetitionStagePart2() {
        String comment = "###Competition Runs\n" +
                "####Part 2\n" +
                "\n" +
                "Username | Team | Items | Score\n" +
                "|:----------: | :----------: | :-----------: | :-----------:\n" +
                "/u/jcrixus |  | Moves +5, MS, DD, APU, C-1 | [250,123](https://www.reddit.com/r/shufflescorebottest/comments/7ncsuk/test/ds42v9w)\n";
        TablePartId expected = new TablePartId(new Stage(StageType.COMPETITION, null), 1);
        TablePartId actual = BotCommentHandler.getTablePartId(comment);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetTablePartId_EBStage() {
        String comment = "###Stage 50\n" +
                "\n" +
                "Username | Team | Items | Result\n" +
                "|:----------: | :----------: | :-----------: | :-----------:\n" +
                "/u/jcrixus | vanilluxe (15, SL5), rayquaza (10, SL5), azumarill (perfect) |  | [Unknown](https://www.reddit.com/r/shufflescorebottest/comments/7ncsuk/test/ds4bny2)\n";
        TablePartId expected = new TablePartId(new Stage(StageType.ESCALATION_BATTLE, "50"), 0);
        TablePartId actual = BotCommentHandler.getTablePartId(comment);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetTablePartId_EBStagePart2() {
        String comment = "###Stage 50\n" +
                "####Part 2\n" +
                "\n" +
                "Username | Team | Items | Result\n" +
                "|:----------: | :----------: | :-----------: | :-----------:\n" +
                "/u/jcrixus | vanilluxe (15, SL5), rayquaza (10, SL5), azumarill (perfect) |  | [Unknown](https://www.reddit.com/r/shufflescorebottest/comments/7ncsuk/test/ds4bny2)\n";
        TablePartId expected = new TablePartId(new Stage(StageType.ESCALATION_BATTLE, "50"), 1);
        TablePartId actual = BotCommentHandler.getTablePartId(comment);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetTablePartId_NormalStage() {
        String comment = "###Stage Meowth\n" +
                "\n" +
                "Username | Team | Items | Result\n" +
                "|:----------: | :----------: | :-----------: | :-----------:\n" +
                "/u/jcrixus | vanilluxe (15, SL5), rayquaza (10, SL5), azumarill (perfect) |  | [Unknown](https://www.reddit.com/r/shufflescorebottest/comments/7ncsuk/test/ds4bny2)\n";
        TablePartId expected = new TablePartId(new Stage(StageType.NORMAL, "meowth"), 0);
        TablePartId actual = BotCommentHandler.getTablePartId(comment);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetTablePartId_NormalStagePart2() {
        String comment = "###Stage Meowth\n" +
                "####Part 2\n" +
                "\n" +
                "Username | Team | Items | Result\n" +
                "|:----------: | :----------: | :-----------: | :-----------:\n" +
                "/u/jcrixus | vanilluxe (15, SL5), rayquaza (10, SL5), azumarill (perfect) |  | [Unknown](https://www.reddit.com/r/shufflescorebottest/comments/7ncsuk/test/ds4bny2)\n";
        TablePartId expected = new TablePartId(new Stage(StageType.NORMAL, "meowth"), 1);
        TablePartId actual = BotCommentHandler.getTablePartId(comment);
        assertEquals(expected, actual);
    }
}
