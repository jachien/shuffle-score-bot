package org.jchien.shuffle.handler;

import net.dean.jraw.models.PublicContribution;
import org.jchien.shuffle.parser.exception.FormatException;
import org.jchien.shuffle.model.ParsedComment;
import org.jchien.shuffle.model.Pokemon;
import org.jchien.shuffle.model.RunDetails;
import org.jchien.shuffle.model.RunDetailsBuilder;
import org.jchien.shuffle.model.StageType;
import org.jchien.shuffle.model.UserRunDetails;
import org.jchien.shuffle.parser.Canonicalizer;
import org.jchien.shuffle.parser.ParseException;
import org.jchien.shuffle.parser.ParseExceptionUtils;
import org.jchien.shuffle.parser.RawRunDetails;
import org.jchien.shuffle.parser.RunParser;
import org.jchien.shuffle.parser.TokenMgrError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parses runs from a single comment.
 *
 * @author jchien
 */
public class UserCommentHandler {
    private static final Logger LOG = LoggerFactory.getLogger(UserCommentHandler.class);

    private Canonicalizer canonicalizer = new Canonicalizer();

    private final PublicContribution<?> comment;

    private final String commentBody;

    public UserCommentHandler(PublicContribution<?> comment, String commentBody) {
        this.comment = comment;
        this.commentBody = commentBody;
    }

    private interface BlockConsumer {
        void accept(String comment) throws FormatException;
    }

    public ParsedComment parseRuns() {
        Throwable rosterThrowable = null;
        Map<String, Pokemon> roster;
        try {
            roster = getRoster(commentBody);
        } catch (Throwable t) {
            roster = new LinkedHashMap<>();
            rosterThrowable = t;
        }

        List<RunDetails> runs = getRunDetails(commentBody, roster);

        List<UserRunDetails> validRuns = getValidRuns(runs, comment.getAuthor(), comment.getId());

        List<UserRunDetails> invalidRuns = getInvalidRuns(runs, comment.getAuthor(), comment.getId(), rosterThrowable);

        return new ParsedComment(validRuns, invalidRuns);
    }

    private List<UserRunDetails> getValidRuns(List<RunDetails> runs,
                                              String commentAuthor,
                                              String commentId) {
        return runs.stream()
                .filter(run -> !run.hasThrowable())
                .map(run -> new UserRunDetails(commentAuthor, commentId, run))
                .collect(Collectors.toList());
    }

    private List<UserRunDetails> getInvalidRuns(List<RunDetails> runs,
                                                String commentAuthor,
                                                String commentId,
                                                Throwable rosterThrowable) {

        List<UserRunDetails> ret = new ArrayList<>();

        if (rosterThrowable != null) {
            RunDetails rosterDetails = new RunDetailsBuilder()
                    .setStageType(StageType.ROSTER)
                    .setThrowables(Arrays.asList(rosterThrowable))
                    .build();
            UserRunDetails rosterUrd = new UserRunDetails(commentAuthor, commentId, rosterDetails);
            ret.add(rosterUrd);
        }

        runs.stream()
                .filter(RunDetails::hasThrowable)
                .map(run -> new UserRunDetails(commentAuthor, commentId, run))
                .forEach(ret::add);

        return ret;
    }

    public void processBlock(String comment,
                             Pattern pattern,
                             BlockConsumer consumer,
                             Supplier<FormatException> multiBlockExceptionSupplier) throws FormatException {
        Matcher m = pattern.matcher(comment);

        boolean first = true;
        while (m.find()) {
            if (multiBlockExceptionSupplier != null && !first) {
                // throw an exception if you've defined multiple rosters
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
                // grammar definition should prevent this from happening, but catch it just in case
                throwable = e;
            }

            RawRunDetails rawDetails = p.getDetails();
            LOG.debug(rawDetails.toString());
            RunDetails details = canonicalizer.canonicalize(rawDetails, roster, throwable);
            runs.add(details);
        };

        try {
            processBlock(comment, PATTERN, runConsumer, null);
        } catch (FormatException e) {
            // we handle exceptions rather than bubble them up
            throw new IllegalStateException("no exceptions should have been thrown");
        }

        return runs;
    }

    static final Pattern ROSTER_PATTERN = Pattern.compile("(?:^|\\s)(?:!roster)\\b.*?!end\\b", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    public Map<String, Pokemon> getRoster(String comment) throws FormatException {
        final Map<String, Pokemon> roster = new LinkedHashMap<>();

        BlockConsumer rosterConsumer = block -> {
            RunParser p = new RunParser(new StringReader(block));
            try {
                p.roster();
            } catch (ParseException e) {
                throw ParseExceptionUtils.getFormatException(e);
            }
            // reuse RawRunDetails and canonicalizer to just get the team
            // not the cleanest but it's already written and should work
            RawRunDetails rawRoster = p.getDetails();
            RunDetails details = canonicalizer.canonicalize(rawRoster, null, null);

            if (!details.getThrowables().isEmpty()) {
                Throwable firstThrowable = details.getThrowables().get(0);
                if (firstThrowable instanceof FormatException) {
                    throw (FormatException) firstThrowable;
                }
                throw new FormatException(firstThrowable.getMessage(), firstThrowable);
            }

            details.getTeam().stream().forEach(pokemon -> roster.put(pokemon.getName().toLowerCase(), pokemon));
        };

        Supplier<FormatException> multiBlockExceptionSupplier = () ->
                new FormatException("You can only define one `!roster` block per comment.");

        processBlock(comment, ROSTER_PATTERN, rosterConsumer, multiBlockExceptionSupplier);

        return roster;
    }
}
