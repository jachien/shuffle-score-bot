package org.jchien.shuffle.bot;

import org.jchien.shuffle.model.UserRunDetails;

import java.time.Instant;
import java.util.List;

/**
 * @author jchien
 */
public class InvalidRunFormatter {
    public String formatInvalidRuns(List<UserRunDetails> invalidRuns) {
        StringBuilder sb = new StringBuilder();
        Instant lastModTime = null;

        sb.append("It looks like you were trying to write run details, but I couldn't parse what you wrote.\n\n" +
                "*****\n\n");
        for (UserRunDetails urd : invalidRuns) {
            lastModTime = urd.getLastModifiedTime(); // this sucks, we shouldn't bother duplicating this
            sb.append(urd.getRunDetails().getException().getMessage());
            sb.append("\n\n*****\n\n");
        }

        sb.append("You can edit your comment to fix your run details.  \n" +
                "The last time I saw it change was " + lastModTime);
        return sb.toString();
    }
}
