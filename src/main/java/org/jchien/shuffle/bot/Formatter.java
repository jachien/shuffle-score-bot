package org.jchien.shuffle.bot;

import org.jchien.shuffle.model.Item;
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
    private static final String COMP_TABLE_HEADER = "\n" +
            "Username| Team | Items | Score\n" +
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
            appendScore(sb, submissionUrl, urd.getCommentId(), details.getMovesLeft());
        }
        return sb.toString();

    }

    public final static String EB_HEADER_PREFIX = "###Stage ";
    private static final String EB_TABLE_HEADER = "\n" +
            "Username| Team | Items | Moves Left\n" +
            "|:----------: | :----------: | :-----------: | :-----------:\n";
    public String formatEscalationBattle(List<UserRunDetails> runs, int stage, String submissionUrl) {
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
        sb.append(EB_HEADER_PREFIX).append(stage).append(EB_TABLE_HEADER);
        for (UserRunDetails urd : runs) {
            RunDetails details = urd.getRunDetails();

            appendUser(sb, urd.getUser());
            appendDelimiter(sb);
            appendTeam(sb, details.getTeam());
            appendDelimiter(sb);
            appendItems(sb, details.getItems());
            appendDelimiter(sb);
            appendMovesLeft(sb, submissionUrl, urd.getCommentId(), details.getMovesLeft());
        }
        return sb.toString();
    }

    private void appendDelimiter(StringBuilder sb) {
        sb.append(" | ");
    }

    private void appendUser(StringBuilder sb, String user) {
        sb.append("/u/").append(user);
    }

    private void appendTeam(StringBuilder sb, List<Pokemon> team) {
        for (Pokemon pokemon : team) {
            appendPokemon(sb, pokemon);
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

                sb.append(pokemon.getName());
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

        sb.append('[');
        if (score == null) {
            sb.append("Unknown");
        } else {
            sb.append(df.format(score));
        }
        sb.append("](");
        appendCommentUrl(sb, submissionUrl, commentId);
        sb.append(')');
    }

    private void appendMovesLeft(StringBuilder sb, String submissionUrl, String commentId, Integer movesLeft) {
        sb.append('[');
        if (movesLeft == null) {
            sb.append("Unknown");
        } else {
            sb.append(movesLeft).append(movesLeft).append(" moves left");
        }
        sb.append("](");
        appendCommentUrl(sb, submissionUrl, commentId);
        sb.append(')');
    }

    private void appendCommentUrl(StringBuilder sb, String submissionUrl, String commentId) {
        sb.append("https://www.reddit.com").append(submissionUrl).append("/").append(commentId);
    }
}
