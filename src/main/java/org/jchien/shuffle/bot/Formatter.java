package org.jchien.shuffle.bot;

import org.jchien.shuffle.model.Item;
import org.jchien.shuffle.model.MoveType;
import org.jchien.shuffle.model.Pokemon;
import org.jchien.shuffle.model.RunDetails;
import org.jchien.shuffle.model.UserRunDetails;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import static java.util.Comparator.*;

/**
 * @author jchien
 */
public class Formatter {
    public final static String COMP_HEADER_PREFIX = "###Runs";
    private static final String COMP_TABLE_HEADER = "\n\n" +
            "Username | Team | Items | Score\n" +
            "|:----------: | :----------: | :-----------: | :-----------:\n";
    public String formatCompetitionRun(List<UserRunDetails> runs, String submissionUrl) {
        // inlining these lambdas into Comparator.comparing() makes intellij 2017.3.1 think it's a syntax error
        Function<UserRunDetails, Integer> score = (r) -> r.getRunDetails().getScore();

        // sort by score desc, username
        Comparator<UserRunDetails> comparator = comparing(score, nullsLast(reverseOrder()))
                .thenComparing(UserRunDetails::getUser);

        Collections.sort(runs, comparator);

        // username (link to /u/user) | team | items | score (link to comment)
        StringBuilder sb = new StringBuilder();
        sb.append(COMP_HEADER_PREFIX).append(COMP_TABLE_HEADER);
        for (UserRunDetails urd : runs) {
            RunDetails details = urd.getRunDetails();

            appendUser(sb, urd.getUser());
            appendDelimiter(sb);
            appendTeam(sb, details.getTeam());
            appendDelimiter(sb);
            appendItems(sb, details.getItems());
            appendDelimiter(sb);
            appendScore(sb, submissionUrl, urd.getCommentId(), details.getScore());
        }

        sb.append('\n');
        return sb.toString();
    }

    public final static String STAGE_HEADER_PREFIX = "###Stage ";
    private static final String STAGE_TABLE_HEADER = "\n\n" +
            "Username | Team | Items | Result\n" +
            "|:----------: | :----------: | :-----------: | :-----------:\n";

    public String formatStage(List<UserRunDetails> runs, String stageId, String submissionUrl) {
        // inlining these lambdas into Comparator.comparing() makes intellij 2017.3.1 think it's a syntax error
        Function<UserRunDetails, Integer> itemsCost = (r) -> r.getRunDetails().getItemsCost();
        Function<UserRunDetails, Integer> movesLeft = (r) -> r.getRunDetails().getMovesLeft();

        // sort by item cost asc, moves left desc, username
        Comparator<UserRunDetails> comparator = comparing(itemsCost, nullsLast(naturalOrder()))
                .thenComparing(movesLeft, nullsLast(reverseOrder()))
                .thenComparing(UserRunDetails::getUser);

        Collections.sort(runs, comparator);

        // username (link to /u/user) | team | items | score (link to comment)

        StringBuilder sb = new StringBuilder();
        sb.append(STAGE_HEADER_PREFIX).append(stageId).append(STAGE_TABLE_HEADER);
        for (UserRunDetails urd : runs) {
            RunDetails details = urd.getRunDetails();

            appendUser(sb, urd.getUser());
            appendDelimiter(sb);
            appendTeam(sb, details.getTeam());
            appendDelimiter(sb);
            appendItems(sb, details.getItems());
            appendDelimiter(sb);

            appendResult(sb,
                    submissionUrl,
                    urd.getCommentId(),
                    details.getMoveType(),
                    details.getMovesLeft(),
                    details.getTimeLeft());

        }

        sb.append('\n');
        return sb.toString();
    }

    private void appendDelimiter(StringBuilder sb) {
        sb.append(" | ");
    }

    private void appendUser(StringBuilder sb, String user) {
        sb.append("/u/").append(user);
    }

    private void appendTeam(StringBuilder sb, List<Pokemon> team) {
        String delim = "";
        for (Pokemon pokemon : team) {
            sb.append(delim);
            appendPokemon(sb, pokemon);
            delim = ", ";
        }
    }

    private void appendPokemon(StringBuilder sb, Pokemon pokemon) {
        sb.append(pokemon.getName());
        sb.append(getPokemonStats(pokemon));
    }

    private String getPokemonStats(Pokemon pokemon) {
        StringBuilder sb = new StringBuilder(" (");
        String delim = "";
        if (pokemon.isPerfect()) {
            sb.append("perfect");
        } else {
            if (pokemon.getLevel() != null) {
                sb.append(pokemon.getLevel());
                delim = ", ";
            }
            if (pokemon.getSkillLevel() != null) {
                sb.append(delim).append("SL").append(pokemon.getSkillLevel());
                delim = ", ";
            }
            if (pokemon.getSkillName() != null) {
                if (pokemon.getSkillLevel() != null) {
                    // we already appended SL, so append a space instead of the normal delimiter
                    sb.append(" ");
                } else {
                    sb.append(delim);
                }

                sb.append(pokemon.getSkillName());
                delim = ", ";
            }
            if (pokemon.getMsus() != null && pokemon.getMaxMsus() != null) {
                sb.append(delim).append(pokemon.getMsus()).append("/").append(pokemon.getMaxMsus());
            }
        }
        sb.append(")");

        // if we have no stats specified, the string will be " ()"
        if (sb.length() > 3) {
            return sb.toString();
        }

        return "";
    }

    private void appendItems(StringBuilder sb, List<Item> items) {
        String delim = "";
        for (Item item : items) {
            sb.append(delim).append(item.toString());
            delim = ", ";
        }
    }

    private void appendScore(StringBuilder sb, String submissionUrl, String commentId, Integer score) {
        DecimalFormat df = new DecimalFormat();
        df.setGroupingSize(3);
        df.setGroupingUsed(true);
        String formattedScore = df.format(score);
        appendResult(sb, submissionUrl, commentId, formattedScore);
    }

    private void appendResult(StringBuilder sb,
                              String submissionUrl,
                              String commentId,
                              MoveType moveType,
                              Integer movesLeft,
                              Integer timeLeft) {
        String result;
        switch (moveType) {
            case MOVES:
                result = MoveType.MOVES.format(movesLeft);
                break;
            case TIME:
                result = MoveType.TIME.format(timeLeft);
                break;
            default:
                result = null;
        }
        appendResult(sb, submissionUrl, commentId, result);
    }


    private void appendResult(StringBuilder sb, String submissionUrl, String commentId, String result) {
        sb.append('[');
        if (result == null) {
            sb.append("Unknown");
        } else {
            sb.append(result);
        }
        sb.append("](");
        sb.append(RedditUtils.getCommentPermalink(submissionUrl, commentId));
        sb.append(')');
    }
}
