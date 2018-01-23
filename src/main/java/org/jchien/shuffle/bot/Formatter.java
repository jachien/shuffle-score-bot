package org.jchien.shuffle.bot;

import org.jchien.shuffle.model.Item;
import org.jchien.shuffle.model.MoveType;
import org.jchien.shuffle.model.Pokemon;
import org.jchien.shuffle.model.RunDetails;
import org.jchien.shuffle.model.Stage;
import org.jchien.shuffle.model.StageType;
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
    public final static String COMP_HEADER_PREFIX = "###Competition Runs";
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
        sb.append(COMP_HEADER_PREFIX);
        appendAddRunInstructions(sb, StageType.COMPETITION, null);
        sb.append(COMP_TABLE_HEADER);
        for (UserRunDetails urd : runs) {
            RunDetails details = urd.getRunDetails();

            appendUser(sb, urd.getUser());
            appendDelimiter(sb);
            appendTeam(sb, details.getTeam());
            appendDelimiter(sb);
            appendItems(sb, details.getItems());
            appendDelimiter(sb);
            appendScore(sb, submissionUrl, urd.getCommentId(), details.getScore());
            sb.append('\n');
        }

        return sb.toString();
    }

    public final static String STAGE_HEADER_PREFIX = "###Stage ";
    private static final String STAGE_TABLE_HEADER = "\n\n" +
            "Username | Team | Items | Result\n" +
            "|:----------: | :----------: | :-----------: | :-----------:\n";

    public String formatStage(List<UserRunDetails> runs, Stage stage, String submissionUrl) {
        // inlining these lambdas into Comparator.comparing() makes intellij 2017.3.1 think it's a syntax error
        Function<UserRunDetails, Integer> itemsCost = r -> r.getRunDetails().getItemsCost();
        Function<UserRunDetails, Integer> unitsLeft = urd -> {
            RunDetails r = urd.getRunDetails();
            switch (r.getMoveType()) {
                case MOVES: return r.getMovesLeft();
                case TIME: return r.getTimeLeft();
                default: return null;
            }
        };

        // sort by item cost asc, moves / time left desc, username
        // We're not going to validate whether this was supposed to be a moves stage or a time stage.
        // If people have conflicting moves types then that's too bad,
        // later on we'll implement excluding runs for comments below some reddit score threshold, probably 0 or 1.
        Comparator<UserRunDetails> comparator = comparing(itemsCost, nullsLast(naturalOrder()))
                .thenComparing(unitsLeft, nullsLast(reverseOrder()))
                .thenComparing(UserRunDetails::getUser);

        Collections.sort(runs, comparator);

        // username (link to /u/user) | team | items | score (link to comment)

        StringBuilder sb = new StringBuilder();
        sb.append(STAGE_HEADER_PREFIX).append(stage.getStageId());
        appendAddRunInstructions(sb, stage.getStageType(), stage.getStageId());
        sb.append(STAGE_TABLE_HEADER);
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

            sb.append('\n');
        }

        return sb.toString();
    }

    private void appendAddRunInstructions(StringBuilder sb, StageType stageType, String stageId) {
        sb.append("\n\nUse `").append(stageType.getHeader(stageId)).append("` to add your run.");
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
        appendCapitalizedWords(sb, pokemon.getName());
        sb.append(getPokemonStats(pokemon));
    }

    private void appendCapitalizedWords(StringBuilder sb, String str) {
        for (int i=0; i < str.length();) {
            int codePoint = str.codePointAt(i);
            if (i == 0 || Character.isWhitespace(str.codePointBefore(i))) {
                sb.appendCodePoint(Character.toUpperCase(codePoint));
            } else {
                // don't lowercase stuff so we still handle things like "MMY" nicely
                sb.appendCodePoint(codePoint);
            }
            i += Character.charCount(codePoint);
        }
    }

    private String getPokemonStats(Pokemon pokemon) {
        StringBuilder sb = new StringBuilder(" (");
        String delim = "";
        if (pokemon.isPerfect()) {
            sb.append("Perfect");
        } else {
            if (pokemon.getLevel() != null) {
                sb.append("Lv").append(pokemon.getLevel());
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

                appendCapitalizedWords(sb, pokemon.getSkillName());
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
        if (items == null) {
            sb.append("Unknown");
        } else if (items.size() == 0) {
            sb.append("Itemless");
        } else {
            String delim = "";
            for (Item item : items) {
                sb.append(delim).append(item.toString());
                delim = ", ";
            }
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
        final String result;
        if (moveType == null) {
            result = null;
        } else {
            switch (moveType) {
                case MOVES:
                    result = MoveType.MOVES.format(movesLeft);
                    break;
                case TIME:
                    result = MoveType.TIME.format(timeLeft);
                    break;
                default:
                    throw new IllegalArgumentException("unsupported MoveType: " + moveType);
            }
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
