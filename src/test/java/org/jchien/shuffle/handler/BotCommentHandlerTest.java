package org.jchien.shuffle.handler;

import net.dean.jraw.RedditClient;
import net.dean.jraw.models.Submission;
import org.jchien.shuffle.formatter.RunFormatter;
import org.jchien.shuffle.model.BotComment;
import org.jchien.shuffle.model.Stage;
import org.jchien.shuffle.model.StageType;
import org.jchien.shuffle.model.TablePartId;
import org.jchien.shuffle.model.UserRunDetails;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
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
                "###Part 2\n" +
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
        String comment = "###EB Stage 50\n" +
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
        String comment = "###EB Stage 50\n" +
                "###Part 2\n" +
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
                "###Part 2\n" +
                "\n" +
                "Username | Team | Items | Result\n" +
                "|:----------: | :----------: | :-----------: | :-----------:\n" +
                "/u/jcrixus | vanilluxe (15, SL5), rayquaza (10, SL5), azumarill (perfect) |  | [Unknown](https://www.reddit.com/r/shufflescorebottest/comments/7ncsuk/test/ds4bny2)\n";
        TablePartId expected = new TablePartId(new Stage(StageType.NORMAL, "meowth"), 1);
        TablePartId actual = BotCommentHandler.getTablePartId(comment);
        assertEquals(expected, actual);
    }

    @Test
    public void testWriteAggregateTable_OnePart() {
        RedditClient redditClient = mock(RedditClient.class);
        Submission submission = mock(Submission.class);
        BotComment summaryContent = new BotComment("summaryId", "");
        Map<TablePartId, BotComment> tableMap = mock(Map.class);
        Map<String, BotComment> replyMap = mock(Map.class);
        RunFormatter runFormatter = mock(RunFormatter.class);

        BotCommentHandler bch = spy(new BotCommentHandler(
                redditClient,
                submission,
                summaryContent,
                tableMap,
                replyMap,
                runFormatter
        ));

        Stage stage = new Stage(StageType.COMPETITION, null);
        List<UserRunDetails> runs = mock(List.class);

        doReturn(Arrays.asList("0"))
                .when(runFormatter)
                .formatRuns(any(), any(), isNull());

        doReturn(new BotComment("commentId", "0"))
                .when(bch)
                .writeTablePart(new TablePartId(stage, 0),
                                "summaryId",
                                "0");

        List<BotComment> botComments = bch.writeAggregateTable(stage, runs);

        assertEquals(1, botComments.size());
        assertEquals("commentId", botComments.get(0).getCommentId());
    }

    @Test
    public void testWriteAggregateTable_MultiPart() {
        RedditClient redditClient = mock(RedditClient.class);
        Submission submission = mock(Submission.class);
        BotComment summaryContent = new BotComment("summaryId", "");
        Map<TablePartId, BotComment> tableMap = mock(Map.class);
        Map<String, BotComment> replyMap = mock(Map.class);
        RunFormatter runFormatter = mock(RunFormatter.class);

        BotCommentHandler bch = spy(new BotCommentHandler(
                redditClient,
                submission,
                summaryContent,
                tableMap,
                replyMap,
                runFormatter
        ));

        Stage stage = new Stage(StageType.COMPETITION, null);
        List<UserRunDetails> runs = mock(List.class);

        doReturn(Arrays.asList("0", "1", "2"))
                .when(runFormatter)
                .formatRuns(any(), any(), isNull());

        doReturn(new BotComment("id0", "0"))
                .when(bch)
                .writeTablePart(new TablePartId(stage, 0),
                                "summaryId",
                                "0");

        doReturn(new BotComment("id1", "1"))
                .when(bch)
                .writeTablePart(new TablePartId(stage, 1),
                                "id0",
                                "1");

        doReturn(new BotComment("id2", "2"))
                .when(bch)
                .writeTablePart(new TablePartId(stage, 2),
                                "id1",
                                "2");


        List<BotComment> botComments = bch.writeAggregateTable(stage, runs);

        assertEquals(3, botComments.size());
        for (int i=0; i < botComments.size(); i++) {
            BotComment comment = botComments.get(i);
            assertEquals("id" + i, comment.getCommentId());
            assertEquals(Integer.toString(i), comment.getContent());
        }
    }
}
