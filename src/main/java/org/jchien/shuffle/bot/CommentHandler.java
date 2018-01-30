package org.jchien.shuffle.bot;

import org.jchien.shuffle.model.FormatException;
import org.jchien.shuffle.model.Pokemon;
import org.jchien.shuffle.model.RunDetails;
import org.jchien.shuffle.model.Stage;
import org.jchien.shuffle.model.StageType;
import org.jchien.shuffle.parser.ParseException;
import org.jchien.shuffle.parser.ParseExceptionUtils;
import org.jchien.shuffle.parser.RawRunDetails;
import org.jchien.shuffle.parser.RunParser;
import org.jchien.shuffle.parser.TokenMgrError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jchien
 */
public class CommentHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CommentHandler.class);

    private Canonicalizer canonicalizer = new Canonicalizer();

    private interface BlockConsumer {
        void accept(String comment) throws FormatException, ParseException;
    }

    public void processBlock(String comment,
                             Pattern pattern,
                             BlockConsumer consumer,
                             Supplier<FormatException> multiBlockExceptionSupplier) throws FormatException, ParseException {
        Matcher m = pattern.matcher(comment);

        boolean first = true;
        while (m.find()) {
            if (multiBlockExceptionSupplier != null && !first) {
                throw multiBlockExceptionSupplier.get();
            } else {
                first = false;
            }

            int start = m.start();
            int end = m.end();
            if (LOG.isDebugEnabled()) {
                LOG.debug("found match at (" + start + ", " + end + ") for pattern " + pattern);
            }

            String block = comment.substring(start, end).trim();
            consumer.accept(block);
        }
    }

    // word boundary matcher doesn't seem to trigger in front of an exclamation mark
    static final Pattern PATTERN = Pattern.compile("(?:^|\\s)(?:!comp|!eb|!run)\\b.*?!end\\b",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public List<RunDetails> getRunDetails(String comment, Map<String, Pokemon> roster) {
        final List<RunDetails> runs = new ArrayList<>();

        BlockConsumer runConsumer = block -> {
            RunParser p = new RunParser(new StringReader(block));
            Throwable throwable = null;
            try {
                p.start();
            } catch (ParseException e) {
                throwable = ParseExceptionUtils.getFormatException(e);
            } catch (FormatException e) {
                throwable = e;
            } catch (TokenMgrError e) {
                // todo make this more friendly (also find out why it's not just a ParseException)
                throwable = e;
            }

            RawRunDetails rawDetails = p.getDetails();
            LOG.debug(rawDetails.toString());
            RunDetails details = canonicalizer.canonicalize(rawDetails, roster, throwable);
            runs.add(details);
        };

        try {
            processBlock(comment, PATTERN, runConsumer, null);
        } catch (FormatException | ParseException e) {
            // we handle exceptions rather than bubble them up
            throw new IllegalStateException("no exceptions should have been thrown");
        }

        return runs;
    }

    static final Pattern ROSTER_PATTERN = Pattern.compile("(?:^|\\s)(?:!roster)\\b.*?!end\\b", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    public Map<String, Pokemon> getRoster(String comment) throws FormatException, ParseException {
        final Map<String, Pokemon> roster = new LinkedHashMap<>();

        BlockConsumer rosterConsumer = block -> {
            RunParser p = new RunParser(new StringReader(block));
            p.roster();
            // reuse RawRunDetails and canonicalizer to just get the team
            // not the cleanest but it's already written and should work
            RawRunDetails rawRoster = p.getDetails();
            RunDetails details = canonicalizer.canonicalize(rawRoster, null, null);
            details.getTeam().stream().forEach(pokemon -> roster.put(pokemon.getName().toLowerCase(), pokemon));
        };

        Supplier<FormatException> multiBlockExceptionSupplier = () ->
                new FormatException("You can only define one `!roster` block per comment.");

        processBlock(comment, ROSTER_PATTERN, rosterConsumer, multiBlockExceptionSupplier);

        return roster;
    }

    private static final Pattern STAGE_PATTERN = Pattern.compile("^" + Formatter.STAGE_HEADER_PREFIX + "(.+)\n");
    public Stage getAggregateStage(String comment) {
        // assumes the we've already checked that the configured bot user is the commenter

        if (comment.startsWith(Formatter.COMP_HEADER_PREFIX)) {
            return new Stage(StageType.COMPETITION, null);
        }

        Matcher m = STAGE_PATTERN.matcher(comment);
        if (m.find()) {
            String stageId = Stage.normalizeStageId(m.group(1));
            try {
                Integer.parseInt(stageId);
                return new Stage(StageType.ESCALATION_BATTLE, stageId);
            } catch (NumberFormatException e) {
                return new Stage(StageType.NORMAL, stageId);
            }
        }

        return null;
    }
}
