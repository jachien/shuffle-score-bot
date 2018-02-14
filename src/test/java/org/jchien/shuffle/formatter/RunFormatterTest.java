package org.jchien.shuffle.formatter;

import org.jchien.shuffle.model.Stage;
import org.jchien.shuffle.model.StageType;
import org.jchien.shuffle.model.UserRunDetails;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.jchien.shuffle.model.UserRunDetailsTestUtils.generateUserRunDetails;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author jchien
 */
public class RunFormatterTest {
    @Test
    public void testFormatCompetitionRun_OneComment() {
        RunFormatter f = new RunFormatter();
        List<UserRunDetails> runs = generateUserRunDetails(1, 200);
        List<String> comments = f.formatCompetitionRun(runs, "");
        assertEquals(1, comments.size());

        assertTrue(comments.get(0).startsWith(RunFormatter.COMP_HEADER_PREFIX));
    }

    @Test
    public void testFormatCompetitionRun_MultiComment() {
        RunFormatter f = new RunFormatter();
        int numRuns = 20;
        int minCharsPerRow = (int)Math.ceil(RunFormatter.MAX_COMMENT_LENGTH / numRuns) + 1;
        List<UserRunDetails> runs = generateUserRunDetails(numRuns, minCharsPerRow);
        List<String> comments = f.formatCompetitionRun(runs, "");
        assertTrue(comments.size() > 1);

        for (String comment : comments) {
            assertTrue(comment.startsWith(RunFormatter.COMP_HEADER_PREFIX));
        }
    }

    @Test
    public void testFormatCompetitionRun_SkipLongComment() {
        RunFormatter f = new RunFormatter();
        List<UserRunDetails> runs = generateUserRunDetails(1, RunFormatter.MAX_ROW_LENGTH);
        List<String> comments = f.formatCompetitionRun(runs, "");
        assertEquals(1, comments.size());

        // If all runs are skipped, then we return an empty table rather than no comments.
        // This is expected to be extremely unlikely to happen so we won't try to handle it nicely.
        assertTrue(comments.get(0).endsWith(RunFormatter.COMP_TABLE_HEADER));
    }

    @Test
    public void testFormatCompetitionRun_SkipManyLongComments() {
        RunFormatter f = new RunFormatter();
        List<UserRunDetails> runs = generateUserRunDetails(100, RunFormatter.MAX_ROW_LENGTH);
        List<String> comments = f.formatCompetitionRun(runs, "");
        assertEquals(1, comments.size());

        // If all runs are skipped, then we return an empty table rather than no comments.
        // This is expected to be extremely unlikely to happen so we won't try to handle it nicely.
        assertTrue(comments.get(0).endsWith(RunFormatter.COMP_TABLE_HEADER));
    }

    @Test
    public void testFormatEBStage_OneComment() {
        RunFormatter f = new RunFormatter();
        List<UserRunDetails> runs = generateUserRunDetails(1, 200);
        List<String> comments = f.formatRuns(runs, new Stage(StageType.ESCALATION_BATTLE, "50"), "");
        assertEquals(1, comments.size());

        assertTrue(comments.get(0).startsWith(RunFormatter.EB_STAGE_HEADER_PREFIX));
    }

    @Test
    public void testFormatEBStage_MultiComment() {
        RunFormatter f = new RunFormatter();
        int numRuns = 20;
        int minCharsPerRow = (int)Math.ceil(RunFormatter.MAX_COMMENT_LENGTH / numRuns) + 1;
        List<UserRunDetails> runs = generateUserRunDetails(numRuns, minCharsPerRow);
        List<String> comments = f.formatRuns(runs, new Stage(StageType.ESCALATION_BATTLE, "50"), "");
        assertTrue(comments.size() > 1);

        for (String comment : comments) {
            assertTrue(comment.startsWith(RunFormatter.EB_STAGE_HEADER_PREFIX));
        }
    }

    @Test
    public void testFormatEBStage_SkipLongComment() {
        RunFormatter f = new RunFormatter();
        List<UserRunDetails> runs = generateUserRunDetails(1, RunFormatter.MAX_ROW_LENGTH);
        List<String> comments = f.formatRuns(runs, new Stage(StageType.ESCALATION_BATTLE, "50"), "");
        assertEquals(1, comments.size());

        // If all runs are skipped, then we return an empty table rather than no comments.
        // This is expected to be extremely unlikely to happen so we won't try to handle it nicely.
        assertTrue(comments.get(0).endsWith(RunFormatter.STAGE_TABLE_HEADER));
    }

    @Test
    public void testFormatEBStage_SkipManyLongComments() {
        RunFormatter f = new RunFormatter();
        List<UserRunDetails> runs = generateUserRunDetails(100, RunFormatter.MAX_ROW_LENGTH);
        List<String> comments = f.formatRuns(runs, new Stage(StageType.ESCALATION_BATTLE, "50"), "");
        assertEquals(1, comments.size());

        // If all runs are skipped, then we return an empty table rather than no comments.
        // This is expected to be extremely unlikely to happen so we won't try to handle it nicely.
        assertTrue(comments.get(0).endsWith(RunFormatter.STAGE_TABLE_HEADER));
    }
}
