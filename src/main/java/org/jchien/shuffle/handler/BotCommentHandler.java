package org.jchien.shuffle.handler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import net.dean.jraw.RedditClient;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Submission;
import net.dean.jraw.references.InboxReference;
import org.jchien.shuffle.formatter.FormatterUtils;
import org.jchien.shuffle.formatter.RunFormatter;
import org.jchien.shuffle.formatter.InvalidRunFormatter;
import org.jchien.shuffle.formatter.SummaryFormatter;
import org.jchien.shuffle.model.BotComment;
import org.jchien.shuffle.model.InvalidRuns;
import org.jchien.shuffle.model.Stage;
import org.jchien.shuffle.model.StageType;
import org.jchien.shuffle.model.TablePartId;
import org.jchien.shuffle.model.UserRunDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

/**
 * Unlike UserCommentHandler, this is intended to handle all the bot comments for a single sumbission.
 * Naming things is hard.
 * @author jchien
 */
public class BotCommentHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BotCommentHandler.class);

    private static final Comparator<Stage> STAGE_COMPARATOR =
            comparing(Stage::getStageType)
            .thenComparing(Stage::getStageId, nullsFirst(String.CASE_INSENSITIVE_ORDER));

    private static final Comparator<TablePartId> TABLE_PART_ID_COMPARATOR =
            comparing(TablePartId::getStage, STAGE_COMPARATOR)
            .thenComparing(TablePartId::getPart, naturalOrder());

    private RedditClient redditClient;

    private Submission submission;

    private BotComment summaryComment = null;

    // map of all aggregate tables as they are BEFORE any writes or updates from this run
    private Map<TablePartId, BotComment> aggregateTableMap = new TreeMap<>(TABLE_PART_ID_COMPARATOR);

    // user comment id -> bot reply
    private Map<String, BotComment> botReplyMap = new TreeMap<>();

    private RunFormatter runFormatter = new RunFormatter();

    private SummaryFormatter summaryFormatter = new SummaryFormatter();

    public BotCommentHandler(RedditClient redditClient, Submission submission) {
        this.redditClient = redditClient;
        this.submission = submission;
    }

    @VisibleForTesting
    BotCommentHandler(RedditClient redditClient,
                      Submission submission,
                      BotComment summaryComment,
                      Map<TablePartId, BotComment> aggregateTableMap,
                      Map<String, BotComment> botReplyMap, RunFormatter runFormatter) {
        this.redditClient = redditClient;
        this.submission = submission;
        this.summaryComment = summaryComment;
        this.aggregateTableMap = aggregateTableMap;
        this.botReplyMap = botReplyMap;
        this.runFormatter = runFormatter;
    }

    public void processBotComment(String commentId, String commentBody, String parentId) {
        boolean isSummaryTable = cacheSummaryTable(commentId, commentBody);

        boolean isAggregateTable = cacheAggregateTable(commentId, commentBody);

        if (!isSummaryTable && !isAggregateTable) {
            cacheBotReply(commentId, commentBody, parentId);
        }
    }

    // return true if this comment was a summary table
    private boolean cacheSummaryTable(String commentId, String commentBody) {
        if (isSummaryComment(commentBody)) {
            summaryComment = new BotComment(commentId, commentBody);

            if (LOG.isDebugEnabled()) {
                LOG.debug("found summary table, id = " + commentId);
            }

            return true;
        }

        return false;
    }

    // return true if this comment was an aggregate table
    private boolean cacheAggregateTable(String commentId, String commentBody) {
        TablePartId partId = getTablePartId(commentBody);

        if (partId == null) {
            return false;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("found " + partId + " from comment " + commentId);
        }

        aggregateTableMap.put(partId, new BotComment(commentId, commentBody));

        return true;
    }

    private void cacheBotReply(String commentId, String commentBody, String parentId) {
        botReplyMap.put(parentId, new BotComment(commentId, commentBody));
    }

    public void createOrUpdateBotComments(Map<Stage, List<UserRunDetails>> stageMap,
                                          Map<String, String> authorMap,
                                          Map<String, InvalidRuns> invalidRunMap) {
        Map<TablePartId, BotComment> latestTableMap = writeAggregateTables(stageMap);

        removeEmptyAggregateTables(latestTableMap);

        writeBotReplies(authorMap, invalidRunMap);
    }

    private BotComment createSummaryTable() {
        Comment comment = redditClient.submission(submission.getId()).reply(SummaryFormatter.SUMMARY_HEADER);
        return new BotComment(comment.getId(), comment.getBody());
    }

    private void updateSummaryTable(Map<TablePartId, BotComment> latestAggregateTableMap) {
        String submissionUrl = submission.getUrl();
        String summaryTable = summaryFormatter.formatSummary(submissionUrl, latestAggregateTableMap);
        if (!contentEquals(summaryComment.getContent(), summaryTable)) {
            if (LOG.isDebugEnabled()) {
                String url = FormatterUtils.getCommentPermalink(submissionUrl, summaryComment.getCommentId());
                LOG.debug("updating summary comment at " + url);
            }
            redditClient.comment(summaryComment.getCommentId()).edit(summaryTable);
        } else {
            if (LOG.isDebugEnabled()) {
                String url = FormatterUtils.getCommentPermalink(submissionUrl, summaryComment.getCommentId());
                LOG.debug("summary comment already up-to-date at " + url);
            }
        }
    }

    private Map<TablePartId, BotComment> writeAggregateTables(Map<Stage, List<UserRunDetails>> stageMap) {
        if (!stageMap.isEmpty() && summaryComment == null) {
            summaryComment = createSummaryTable();
        }

        // map of all aggregate tables after writes and updates
        Map<TablePartId, BotComment> latestTableMap = new TreeMap<>(TABLE_PART_ID_COMPARATOR);

        for (Map.Entry<Stage, List<UserRunDetails>> entry : stageMap.entrySet()) {
            Stage stage = entry.getKey();
            List<UserRunDetails> runs = entry.getValue();

            try {
                List<BotComment> comments = writeAggregateTable(stage, runs);

                for (int part=0; part < comments.size(); part++) {
                    BotComment comment = comments.get(part);
                    latestTableMap.put(new TablePartId(stage, part), comment);
                }
            } catch (Exception e) {
                LOG.error("failed to write table for stage " + stage + " at " + submission.getUrl(), e);
            }
        }

        if (summaryComment != null) {
            // if all the runs got deleted from a submission, we still need to update the summary table
            updateSummaryTable(latestTableMap);
        }

        return latestTableMap;
    }

    @VisibleForTesting
    List<BotComment> writeAggregateTable(Stage stage,
                                         List<UserRunDetails> runs) {
        String submissionUrl = submission.getUrl();

        final List<String> commentBodies = runFormatter.formatRuns(runs, stage, submissionUrl);

        if (LOG.isDebugEnabled()) {
            LOG.debug("generated comments:\n" + Arrays.asList(commentBodies));
        }

        List<BotComment> botComments = new ArrayList<>(commentBodies.size());

        String parentId = summaryComment.getCommentId();
        for (int partNum=0; partNum < commentBodies.size(); partNum++) {
            TablePartId partId = new TablePartId(stage, partNum);
            String commentBody = commentBodies.get(partNum);
            BotComment comment = writeTablePart(partId, parentId, commentBody);
            botComments.add(comment);

            parentId = comment.getCommentId();
        }

        return botComments;
    }

    /**
     * @param partId            table part id
     * @param parentId          reply to parentId if no table for partId exists already
     * @param commentBody       comment content body
     * @return                  BotComment that was written, updated, or already existing for this partId
     */
    @VisibleForTesting
    BotComment writeTablePart(TablePartId partId, String parentId, String commentBody) {
        String submissionUrl = submission.getUrl();
        BotComment existing = aggregateTableMap.get(partId);

        if (existing == null) {
            // no bot comment exists yet

            if (LOG.isDebugEnabled()) {
                LOG.debug("no comment for " + partId + " yet in " + submissionUrl + ", creating new comment");
            }
            Comment reply = redditClient.comment(parentId).reply(commentBody);
            return new BotComment(reply.getId(), commentBody);
        } else if (!contentEquals(existing.getContent(), commentBody)) {
            // we've already written a comment for this part but it's outdated

            if (LOG.isDebugEnabled()) {
                LOG.debug("comment for " + partId + " outdated in " + submissionUrl +
                                  ", updating comment " + existing.getCommentId());
            }
            redditClient.comment(existing.getCommentId()).edit(commentBody);
            return new BotComment(existing.getCommentId(), commentBody);
        } else {
            // no need to write anything, existing bot comment already has correct content
            if (LOG.isDebugEnabled()) {
                LOG.debug("comment for " + partId + " already up to date in " + submissionUrl);
            }
            return existing;
        }
    }

    private void removeEmptyAggregateTables(Map<TablePartId, BotComment> latestTableMap) {
        Set<TablePartId> emptyParts = Sets.difference(aggregateTableMap.keySet(), latestTableMap.keySet());
        for (TablePartId part : emptyParts) {
            BotComment comment = aggregateTableMap.get(part);
            String commentId = comment.getCommentId();

            if (LOG.isDebugEnabled()) {
                LOG.debug("removing empty aggregate table with comment id " + commentId);
            }

            redditClient.comment(commentId).delete();
        }
    }

    private void writeBotReplies(Map<String, String> authorMap, Map<String, InvalidRuns> invalidRunMap) {
        Set<String> badCommentIds = new TreeSet<>();

        for (Map.Entry<String, InvalidRuns> entry : invalidRunMap.entrySet()) {
            String userCommentId = entry.getKey();
            badCommentIds.add(userCommentId);

            InvalidRuns invalidRuns = entry.getValue();
            Instant lastModDate = invalidRuns.getLastModifiedDate();
            List<UserRunDetails> urds = invalidRuns.getRuns();
            String botReplyBody = InvalidRunFormatter.formatInvalidRuns(lastModDate, urds);
            createOrUpdateReply(userCommentId, botReplyBody, authorMap);
        }

        // now update replies for people who fixed their previously bad comment
        Set<String> okCommentIds = Sets.difference(botReplyMap.keySet(), badCommentIds);
        for (String okCommentId : okCommentIds) {
            String botReplyBody = InvalidRunFormatter.getAllGoodMessage();
            createOrUpdateReply(okCommentId, botReplyBody, authorMap);
        }
    }

    private void createOrUpdateReply(String userCommentId, String botReplyBody, Map<String, String> authorMap) {
        String submissionUrl = submission.getUrl();

        BotComment existing = botReplyMap.get(userCommentId);
        if (existing == null) {
            // no bot comment exists yet

            if (LOG.isDebugEnabled()) {
                LOG.debug("no reply for " + userCommentId + " yet in " + submissionUrl + ", creating new reply");
            }
            redditClient.comment(userCommentId).reply(botReplyBody);
        } else if (!contentEquals(existing.getContent(), botReplyBody)) {
            // we've already written a reply for these runs but it's outdated

            if (LOG.isDebugEnabled()) {
                LOG.debug("reply for " + userCommentId + " outdated in " + submissionUrl +
                                  ", updating comment " + existing.getCommentId());
            }
            redditClient.comment(existing.getCommentId()).edit(botReplyBody);

            String username = authorMap.get(userCommentId);
            InboxReference inbox = redditClient.me().inbox();
            String pmBody = InvalidRunFormatter.getPrivateMessageContent(submissionUrl, userCommentId, botReplyBody);
            inbox.compose(username, "status update", pmBody);
        } else {
            // no need to write anything, existing bot comment already has correct content
            if (LOG.isDebugEnabled()) {
                LOG.debug("reply for " + userCommentId + " already up to date in " + submissionUrl);
            }
        }
    }

    private static final Pattern EB_STAGE_PATTERN = Pattern.compile("^" + RunFormatter.EB_STAGE_HEADER_PREFIX + "(\\d+)\n");

    // main / expert / special stage pattern
    private static final Pattern MES_STAGE_PATTERN = Pattern.compile("^" + RunFormatter.MES_STAGE_HEADER_PREFIX + "(.+)\n");

    static TablePartId getTablePartId(String comment) {
        // assumes the we've already checked that the configured bot user is the commenter

        if (comment.startsWith(RunFormatter.COMP_HEADER_PREFIX)) {
            int partNum = getPartNum(comment);
            return new TablePartId(new Stage(StageType.COMPETITION, null), partNum);
        }

        Matcher ebMatcher = EB_STAGE_PATTERN.matcher(comment);
        if (ebMatcher.find()) {
            String stageId = Stage.normalizeStageId(ebMatcher.group(1));
            int partNum = getPartNum(comment);
            return new TablePartId(new Stage(StageType.ESCALATION_BATTLE, stageId), partNum);
        }

        Matcher mesMatcher = MES_STAGE_PATTERN.matcher(comment);
        if (mesMatcher.find()) {
            String stageId = Stage.normalizeStageId(mesMatcher.group(1));
            int partNum = getPartNum(comment);
            return new TablePartId(new Stage(StageType.NORMAL, stageId), partNum);
        }

        return null;
    }

    private static final Pattern PART_NUM_PATTERN = Pattern.compile("^" + RunFormatter.PART_HEADER + "(\\d+)\n",
                                                                    Pattern.MULTILINE);
    private static int getPartNum(String comment) {
        // assumes the we've already checked that the configured bot user is the commenter
        // and that we identified the comment as a table

        int start = comment.indexOf('\n');
        if (start < 0) {
            throw new IllegalStateException("This comment is messed up.");
        }
        start++;

        Matcher m = PART_NUM_PATTERN.matcher(comment);
        if (m.find(start) && m.start() == start) {
            return Integer.parseInt(m.group(1)) - 1;
        }

        return 0;
    }

    @VisibleForTesting
    static boolean isSummaryComment(String comment) {
        // assumes the we've already checked that the configured bot user is the commenter
        // need to remove trailing whitespace from header because either reddit or jraw is trimming the comment
        if (comment.startsWith(SummaryFormatter.SUMMARY_HEADER.trim())) {
            return true;
        }
        return false;
    }

    /**
     * @param a string 1
     * @param b string 2
     * @return true if both args are null or their trimmed values are equal, false otherwise
     */
    @VisibleForTesting
    static boolean contentEquals(@Nullable String a, @Nullable String b) {
        if (a != null && b != null) {
            // make sure we don't NPE when trimming
            return Objects.equals(a.trim(), b.trim());
        }

        // at least one argument is null, so don't bother trimming for comparison
        return Objects.equals(a, b);
    }
}
