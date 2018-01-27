package org.jchien.shuffle.bot;

import org.jchien.shuffle.model.RunDetails;
import org.jchien.shuffle.model.StageType;
import org.jchien.shuffle.model.UserRunDetails;

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
            for (Exception e : run.getExceptions()) {
                // todo less technical message for ParseExceptions since they're not very useful right now
                sb.append(e.getMessage()).append("  \n");
            }
            sb.append("\n\n*****\n\n");
        }

        sb.append("You can edit your comment to fix your run details.  \n" +
                "The last time I saw it change was " + lastModTime);
        return sb.toString();
    }

    private static void appendStage(StringBuilder sb, StageType stageType, String stageName) {
        String header = stageType.getHeader(stageName);
        sb.append("Your run details starting with `" + header + "`:  \n");
    }

    public static String getAllGoodMessage() {
        return "Everything is good now.";
    }

    public static String getPrivateMessageContent(String submissionUrl, String userCommentId, String replyBody) {
        String permalink = RedditUtils.getCommentPermalink(submissionUrl, userCommentId);
        return "I saw the update to [your comment](" + permalink + ") and processed it. Here are my latest findings." +
                "\n\n*****\n\n" + replyBody;
    }
}
