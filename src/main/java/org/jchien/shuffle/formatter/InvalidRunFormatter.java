package org.jchien.shuffle.formatter;

import org.jchien.shuffle.parser.exception.ItemException;
import org.jchien.shuffle.model.RunDetails;
import org.jchien.shuffle.model.StageType;
import org.jchien.shuffle.model.UserRunDetails;
import org.jchien.shuffle.parser.exception.SkillException;

import java.time.Instant;
import java.util.List;

/**
 * @author jchien
 */
public class InvalidRunFormatter {
    public static String formatInvalidRuns(Instant lastModTime, List<UserRunDetails> invalidRuns) {
        StringBuilder sb = new StringBuilder();

        sb.append("It looks like you were trying to write run details, but I couldn't parse what you wrote.  \n" +
                "[Check out the examples and syntax overview.](https://jachien.github.io/shuffle-score-bot/)  \n" +
                "*****\n\n");
        for (UserRunDetails urd : invalidRuns) {
            RunDetails run = urd.getRunDetails();
            appendStage(sb, run.getStageType(), run.getStage());
            for (Throwable t : run.getThrowables()) {
                appendMessage(sb, t);
            }
            sb.append("\n\n*****\n\n");
        }

        sb.append("You can edit your comment to fix your run details.  \n" +
                "The last time I saw it change was " + lastModTime);
        return sb.toString();
    }

    private static void appendMessage(StringBuilder sb, Throwable t) {
        sb.append(t.getMessage());

        if (t instanceof ItemException) {
            sb.append(" Check out the [item section syntax](https://jachien.github.io/shuffle-score-bot/#items-section).");
        } else if (t instanceof SkillException) {
            sb.append(" Check out the [Pokemon skill syntax](https://jachien.github.io/shuffle-score-bot/#pokemon-skill).");
        }

        sb.append("  \n");
    }

    private static void appendStage(StringBuilder sb, StageType stageType, String stageName) {
        String header = stageType.getHeader(stageName);
        sb.append("Your run details starting with `" + header + "`:  \n");
    }

    public static String getAllGoodMessage() {
        return "Everything is good now.";
    }

    public static String getPrivateMessageContent(String submissionUrl, String userCommentId, String replyBody) {
        String permalink = FormatterUtils.getCommentPermalink(submissionUrl, userCommentId);
        return "I saw the update to [your comment](" + permalink + ") and processed it. Here are my latest findings." +
                "\n\n*****\n\n" + replyBody;
    }
}
