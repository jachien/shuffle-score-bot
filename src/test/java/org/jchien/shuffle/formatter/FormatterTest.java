package org.jchien.shuffle.formatter;

import org.jchien.shuffle.model.Pokemon;
import org.jchien.shuffle.model.RunDetails;
import org.jchien.shuffle.model.RunDetailsBuilder;
import org.jchien.shuffle.model.Stage;
import org.jchien.shuffle.model.StageType;
import org.jchien.shuffle.model.UserRunDetails;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author jchien
 */
public class FormatterTest {
    @Test
    public void testFormatCompetitionRun_OneComment() {
        Formatter f = new Formatter();
        List<UserRunDetails> runs = generateUserRunDetails(1, 200);
        List<String> comments = f.formatCompetitionRun(runs, "");
        assertEquals(1, comments.size());
        
        assertTrue(comments.get(0).startsWith(Formatter.COMP_HEADER_PREFIX));
    }

    @Test
    public void testFormatCompetitionRun_MultiComment() {
        Formatter f = new Formatter();
        int numRuns = 20;
        int minCharsPerRow = (int)Math.ceil(Formatter.MAX_COMMENT_LENGTH / numRuns) + 1;
        List<UserRunDetails> runs = generateUserRunDetails(numRuns, minCharsPerRow);
        List<String> comments = f.formatCompetitionRun(runs, "");
        assertTrue(comments.size() > 1);

        for (String comment : comments) {
            assertTrue(comment.startsWith(Formatter.COMP_HEADER_PREFIX));
        }
    }

    @Test
    public void testFormatCompetitionRun_SkipLongComment() {
        Formatter f = new Formatter();
        List<UserRunDetails> runs = generateUserRunDetails(1, Formatter.MAX_ROW_LENGTH);
        List<String> comments = f.formatCompetitionRun(runs, "");
        assertEquals(1, comments.size());

        // If all runs are skipped, then we return an empty table rather than no comments.
        // This is expected to be extremely unlikely to happen so we won't try to handle it nicely.
        assertTrue(comments.get(0).endsWith(Formatter.COMP_TABLE_HEADER));
    }

    @Test
    public void testFormatCompetitionRun_SkipManyLongComments() {
        Formatter f = new Formatter();
        List<UserRunDetails> runs = generateUserRunDetails(100, Formatter.MAX_ROW_LENGTH);
        List<String> comments = f.formatCompetitionRun(runs, "");
        assertEquals(1, comments.size());

        // If all runs are skipped, then we return an empty table rather than no comments.
        // This is expected to be extremely unlikely to happen so we won't try to handle it nicely.
        assertTrue(comments.get(0).endsWith(Formatter.COMP_TABLE_HEADER));
    }

    @Test
    public void testFormatStage_OneComment() {
        Formatter f = new Formatter();
        List<UserRunDetails> runs = generateUserRunDetails(1, 200);
        List<String> comments = f.formatStage(runs, new Stage(StageType.ESCALATION_BATTLE, "50"), "");
        assertEquals(1, comments.size());

        assertTrue(comments.get(0).startsWith(Formatter.STAGE_HEADER_PREFIX));
    }

    @Test
    public void testFormatStage_MultiComment() {
        Formatter f = new Formatter();
        int numRuns = 20;
        int minCharsPerRow = (int)Math.ceil(Formatter.MAX_COMMENT_LENGTH / numRuns) + 1;
        List<UserRunDetails> runs = generateUserRunDetails(numRuns, minCharsPerRow);
        List<String> comments = f.formatStage(runs, new Stage(StageType.ESCALATION_BATTLE, "50"), "");
        assertTrue(comments.size() > 1);

        for (String comment : comments) {
            assertTrue(comment.startsWith(Formatter.STAGE_HEADER_PREFIX));
        }
    }

    @Test
    public void testFormatStage_SkipLongComment() {
        Formatter f = new Formatter();
        List<UserRunDetails> runs = generateUserRunDetails(1, Formatter.MAX_ROW_LENGTH);
        List<String> comments = f.formatStage(runs, new Stage(StageType.ESCALATION_BATTLE, "50"), "");
        assertEquals(1, comments.size());

        // If all runs are skipped, then we return an empty table rather than no comments.
        // This is expected to be extremely unlikely to happen so we won't try to handle it nicely.
        assertTrue(comments.get(0).endsWith(Formatter.STAGE_TABLE_HEADER));
    }

    @Test
    public void testFormatStage_SkipManyLongComments() {
        Formatter f = new Formatter();
        List<UserRunDetails> runs = generateUserRunDetails(100, Formatter.MAX_ROW_LENGTH);
        List<String> comments = f.formatStage(runs, new Stage(StageType.ESCALATION_BATTLE, "50"), "");
        assertEquals(1, comments.size());

        // If all runs are skipped, then we return an empty table rather than no comments.
        // This is expected to be extremely unlikely to happen so we won't try to handle it nicely.
        assertTrue(comments.get(0).endsWith(Formatter.STAGE_TABLE_HEADER));
    }
    
    private List<UserRunDetails> generateUserRunDetails(int numRuns, int minCharsPerRow) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < minCharsPerRow; i++) {
            sb.append("a");
        }

        Pokemon pokemon = new Pokemon(sb.toString(),
                                      null,
                                      null,
                                      null,
                                      null,
                                      null,
                                      false);

        RunDetails run = new RunDetailsBuilder()
                .setTeam(Arrays.asList(pokemon))
                .build();

        List<UserRunDetails> ret = new ArrayList<>(numRuns);

        DecimalFormat df = new DecimalFormat("000000");
        for (int i=0; i < numRuns; i++) {
            String user = "run-" + df.format(i);
            UserRunDetails urd = new UserRunDetails(user, "abc", run);
            ret.add(urd);
        }
        return ret;
    }
}
