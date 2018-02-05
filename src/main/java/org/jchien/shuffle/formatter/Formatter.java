package org.jchien.shuffle.formatter;

import org.jchien.shuffle.bot.RedditUtils;
import org.jchien.shuffle.model.Item;
import org.jchien.shuffle.model.MoveType;
import org.jchien.shuffle.model.Pokemon;
import org.jchien.shuffle.model.RunDetails;
import org.jchien.shuffle.model.Stage;
import org.jchien.shuffle.model.StageType;
import org.jchien.shuffle.model.UserRunDetails;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import static java.util.Comparator.*;
import static org.jchien.shuffle.formatter.FormatterUtils.appendCapitalizedWords;

/**
 * @author jchien
 */
public class Formatter {
    // reddit max comment length is 10k chars, so we'll be a little conservative
    final static int MAX_COMMENT_LENGTH = 9900;

    // limit length of any single row to guard against abusrdly long input
    // looking at actual comp runs, a single row is around 240 chars long
    final static int MAX_ROW_LENGTH = 1024;

    public final static String COMP_HEADER_PREFIX = "###Competition Runs";
    static final String COMP_TABLE_HEADER = "\n\n" +
            "Username | Team | Items | Score\n" +
            "|:----------: | :----------: | :-----------: | :-----------:\n";
    public List<String> formatCompetitionRun(List<UserRunDetails> runs, String submissionUrl) {
        // inlining these lambdas into Comparator.comparing() makes intellij 2017.3.1 think it's a syntax error
        Function<UserRunDetails, Integer> score = (r) -> r.getRunDetails().getScore();

        // sort by score desc, username
        Comparator<UserRunDetails> comparator = comparing(score, nullsLast(reverseOrder()))
                .thenComparing(UserRunDetails::getUser, String.CASE_INSENSITIVE_ORDER);

        Collections.sort(runs, comparator);

        int partNum = 0;
        List<String> ret = new ArrayList<>();

        // username (link to /u/user) | team | items | score (link to comment)
        StringBuilder sb = new StringBuilder();
        appendCompetitionHeader(sb, partNum);

        StringBuilder rowBuilder = new StringBuilder();
        for (UserRunDetails urd : runs) {
            rowBuilder.setLength(0);
            appendCompetitionRow(rowBuilder, urd, submissionUrl);

            if (rowBuilder.length() > MAX_ROW_LENGTH) {
                // This row is really long so skip it. We won't tell the user.
                // I don't expect this to happen unless the user is filling
                // their run with garbage data.
                continue;
            }

            if (sb.length() + rowBuilder.length() > MAX_COMMENT_LENGTH) {
                ret.add(sb.toString());

                sb.setLength(0);
                partNum++;
                appendCompetitionHeader(sb, partNum);
            }

            sb.append(rowBuilder);
        }

        ret.add(sb.toString());
        return ret;
    }

    private void appendCompetitionHeader(StringBuilder sb, int partNum) {
        sb.append(COMP_HEADER_PREFIX);
        sb.append('\n');

        appendPartNumber(sb, partNum);
        appendAddRunInstructions(sb, StageType.COMPETITION, null);
        sb.append(COMP_TABLE_HEADER);
    }

    private void appendCompetitionRow(StringBuilder sb, UserRunDetails urd, String submissionUrl) {
        RunDetails run = urd.getRunDetails();
        appendUser(sb, urd.getUser());
        appendDelimiter(sb);
        appendTeam(sb, run.getTeam());
        appendDelimiter(sb);
        appendItems(sb, run.getItems());
        appendDelimiter(sb);
        appendScore(sb, submissionUrl, urd.getCommentId(), run.getScore());
        sb.append('\n');
    }

    public final static String STAGE_HEADER_PREFIX = "###Stage ";
    static final String STAGE_TABLE_HEADER = "\n\n" +
            "Username | Team | Items | Result\n" +
            "|:----------: | :----------: | :-----------: | :-----------:\n";

    public List<String> formatStage(List<UserRunDetails> runs, Stage stage, String submissionUrl) {
        // inlining these lambdas into Comparator.comparing() makes intellij 2017.3.1 think it's a syntax error
        Function<UserRunDetails, Integer> itemsCost = r -> r.getRunDetails().getItemsCost();
        Function<UserRunDetails, Integer> unitsLeft = urd -> {
            RunDetails r = urd.getRunDetails();
            if (r.getMoveType() == null) {
                return null;
            }
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
                .thenComparing(UserRunDetails::getUser, String.CASE_INSENSITIVE_ORDER);

        Collections.sort(runs, comparator);

        int partNum = 0;
        List<String> ret = new ArrayList<>();

        // username (link to /u/user) | team | items | score (link to comment)
        StringBuilder sb = new StringBuilder();
        appendStageHeader(sb, stage, partNum);

        StringBuilder rowBuilder = new StringBuilder();
        for (UserRunDetails urd : runs) {
            rowBuilder.setLength(0);
            appendStageRow(rowBuilder, urd, submissionUrl);

            if (rowBuilder.length() > MAX_ROW_LENGTH) {
                // This row is really long so skip it. We won't tell the user.
                // I don't expect this to happen unless the user is filling
                // their run with garbage data.
                continue;
            }

            if (sb.length() + rowBuilder.length() > MAX_COMMENT_LENGTH) {
                ret.add(sb.toString());

                sb.setLength(0);
                partNum++;
                appendStageHeader(sb, stage, partNum);
            }

            sb.append(rowBuilder);
        }

        ret.add(sb.toString());
        return ret;
    }

    private void appendStageHeader(StringBuilder sb, Stage stage, int partNum) {
        sb.append(STAGE_HEADER_PREFIX);
        appendCapitalizedWords(sb, stage.getStageId());
        sb.append('\n');

        appendPartNumber(sb, partNum);
        appendAddRunInstructions(sb, stage.getStageType(), stage.getStageId());
        sb.append(STAGE_TABLE_HEADER);
    }

    private void appendStageRow(StringBuilder sb, UserRunDetails urd, String submissionUrl) {
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

    public static final String PART_HEADER = "####Part ";
    private void appendPartNumber(StringBuilder sb, int partNum) {
        if (partNum == 0) {
            return;
        }

        int displayNum = partNum + 1;
        sb.append(PART_HEADER).append(displayNum).append("\n");
    }

    private void appendAddRunInstructions(StringBuilder sb, StageType stageType, String stageId) {
        sb.append("\nUse `")
                .append(stageType.getHeader(stageId))
                .append("` to add your run.  \n\n" +
                        "See [examples and syntax overview.](https://jachien.github.io/shuffle-score-bot/)  \n\n");
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
        final String formattedScore;
        if (score == null) {
            formattedScore = null;
        } else {
            DecimalFormat df = new DecimalFormat();
            df.setGroupingSize(3);
            df.setGroupingUsed(true);
            formattedScore = df.format(score);
        }
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
