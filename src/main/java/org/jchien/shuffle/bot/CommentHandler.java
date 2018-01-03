package org.jchien.shuffle.bot;

import org.jchien.shuffle.model.DetailException;
import org.jchien.shuffle.model.RunDetails;
import org.jchien.shuffle.parser.DupeSectionException;
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

    private RunParser p = new RunParser();

    private Canonicalizer canonicalizer = new Canonicalizer();

    // word boundary matcher doesn't seem to trigger in front of an exclamation mark
    private static final Pattern PATTERN = Pattern.compile("(?:!run|!eb)\\b.*!end\\b",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public List<RunDetails> getRunDetails(String comment) {
        List<RunDetails> runs = new ArrayList<>();

        Matcher m = PATTERN.matcher(comment);

        while (m.find()) {
            int start = m.regionStart();
            int end = m.regionEnd();
            LOG.info("found match at (" + start + ", " + end + ")");

            String region = comment.substring(start, end);

            try {
                p.ReInit(new StringReader(region));
                p.start();
                RawRunDetails rawDetails = p.getDetails();
                LOG.debug(rawDetails.toString());
                RunDetails details = canonicalizer.canonicalize(rawDetails);
                runs.add(details);
            } catch (ParseException e) {
                runs.add(new RunDetails(new DetailException("Unable to parse run details.", e)));
                LOG.warn("failed to parse:\n" + region, e);
            } catch (DupeSectionException e) {
                runs.add(new RunDetails(new DetailException("Duplicate section found: " + e.getDupeSection().toString().toLowerCase(), e)));
                LOG.warn("dupe section:\n" + region, e);
            }
        }

        return runs;
    }
}
