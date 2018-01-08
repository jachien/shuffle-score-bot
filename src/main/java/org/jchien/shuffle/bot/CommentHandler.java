package org.jchien.shuffle.bot;

import org.jchien.shuffle.model.FormatException;
import org.jchien.shuffle.model.RunDetails;
import org.jchien.shuffle.parser.ParseException;
import org.jchien.shuffle.parser.RawRunDetails;
import org.jchien.shuffle.parser.RunParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jchien
 */
public class CommentHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CommentHandler.class);

    private Canonicalizer canonicalizer = new Canonicalizer();

    // word boundary matcher doesn't seem to trigger in front of an exclamation mark
    private static final Pattern PATTERN = Pattern.compile("(?:!comp|!eb|!run)\\b.*?!end\\b",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public List<RunDetails> getRunDetails(String comment) {
        List<RunDetails> runs = new ArrayList<>();

        Matcher m = PATTERN.matcher(comment);

        while (m.find()) {
            int start = m.start();
            int end = m.end();
            LOG.info("found match at (" + start + ", " + end + ")");

            String region = comment.substring(start, end);

            try {
                RunParser p = new RunParser(new StringReader(region));
                p.start();
                RawRunDetails rawDetails = p.getDetails();
                LOG.debug(rawDetails.toString());
                RunDetails details = canonicalizer.canonicalize(rawDetails);
                runs.add(details);
            } catch (ParseException e) {
                runs.add(new RunDetails(new FormatException("Unable to parse run details.", e)));
                LOG.warn("failed to parse:\n" + region, e);
            } catch (FormatException e) {
                runs.add(new RunDetails(e));
                LOG.warn("format exception", e);
            }
        }

        return runs;
    }

    private static final Pattern STAGE_PATTERN = Pattern.compile("^" + Formatter.EB_HEADER_PREFIX + " (\\d+)");
    private Integer getAggregateStage(String comment) {
        // assumes the we've already checked that the configured bot user is the commenter
        Matcher m = STAGE_PATTERN.matcher(comment);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return null;
    }

    private boolean isAggregateRuns(String comment) {
        // assumes the we've already checked that the configured bot user is the commenter
        return comment.startsWith(Formatter.COMP_HEADER_PREFIX);
    }
}
