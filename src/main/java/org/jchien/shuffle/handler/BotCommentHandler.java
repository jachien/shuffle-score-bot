package org.jchien.shuffle.handler;

import com.google.common.collect.Sets;
import net.dean.jraw.RedditClient;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Submission;
import net.dean.jraw.references.InboxReference;
import org.jchien.shuffle.bot.Formatter;
import org.jchien.shuffle.bot.InvalidRunFormatter;
import org.jchien.shuffle.bot.RedditUtils;
import org.jchien.shuffle.model.BotComment;
import org.jchien.shuffle.model.InvalidRuns;
import org.jchien.shuffle.model.Stage;
import org.jchien.shuffle.model.StageType;
import org.jchien.shuffle.model.UserRunDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
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
import static java.util.Comparator.nullsFirst;

/**
 * Unlike UserCommentHandler, this is intended to handle all the bot comments for a single sumbission.
 * Naming things is hard.
 * @author jchien
 */
public class BotCommentHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BotCommentHandler.class);

    private BotComment summaryComment = null;

    private static final Comparator<Stage> STAGE_ID_COMPARATOR =
            comparing(Stage::getStageType)
            .thenComparing(Stage::getStageId, nullsFirst(String.CASE_INSENSITIVE_ORDER));

    // map of all aggregate tables as they are BEFORE any writes or updates from this run
    private Map<Stage, BotComment> aggregateTableMap = new TreeMap<>(STAGE_ID_COMPARATOR);

    // user comment id -> bot reply
    private Map<String, BotComment> botReplyMap = new TreeMap<>();

    private Formatter formatter = new Formatter();

    private RedditClient redditClient;

    private Submission submission;

    public BotCommentHandler(RedditClient redditClient, Submission submission) {
        this.redditClient = redditClient;
        this.submission = submission;
    }

    public void processBotComment(String commentId, String commentBody, String parentId, String submissionId) {
        boolean isSummaryTable = cacheSummaryTable(commentId, commentBody);

        boolean isAggregateTable = cacheAggregateTable(commentId, commentBody, parentId, submissionId);

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
    private boolean cacheAggregateTable(String commentId, String commentBody, String parentId, String submissionId) {
        Stage stage = getAggregateStage(commentBody);

        if (stage == null) {
            return false;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("found stage " + stage + " from comment " + commentId);
        }

        aggregateTableMap.put(stage, new BotComment(commentId, commentBody));

        return true;
    }

    private void cacheBotReply(String commentId, String commentBody, String parentId) {
        botReplyMap.put(parentId, new BotComment(commentId, commentBody));
    }

    public void createOrUpdateBotComments(Map<Stage, List<UserRunDetails>> stageMap,
                                          Map<String, String> authorMap,
                                          Map<String, InvalidRuns> invalidRunMap) {
        writeAggregateTables(stageMap);

        removeEmptyAggregateTables(stageMap);

        writeBotReplies(authorMap, invalidRunMap);
    }

    private BotComment createSummaryTable() {
        Comment comment = redditClient.submission(submission.getId()).reply(Formatter.SUMMARY_HEADER);
        return new BotComment(comment.getId(), comment.getBody());
    }

    private void updateSummaryTable(Map<Stage, BotComment> latestAggregateTableMap) {
        String submissionUrl = submission.getUrl();
        String summaryTable = formatter.formatSummary(submissionUrl, latestAggregateTableMap);
        if (!Objects.equals(summaryComment.getContent(), summaryTable)) {
            if (LOG.isDebugEnabled()) {
                String url = RedditUtils.getCommentPermalink(submissionUrl, summaryComment.getCommentId());
                LOG.debug("updating summary comment at " + url);
            }
            redditClient.comment(summaryComment.getCommentId()).edit(summaryTable);
        } else {
            if (LOG.isDebugEnabled()) {
                String url = RedditUtils.getCommentPermalink(submissionUrl, summaryComment.getCommentId());
                LOG.debug("summary comment already up-to-date at " + url);
            }
        }
    }

    private void writeAggregateTables(Map<Stage, List<UserRunDetails>> stageMap) {
        if (!stageMap.isEmpty() && summaryComment == null) {
            summaryComment = createSummaryTable();
        }

        // map of all aggregate tables after writes and updates
        Map<Stage, BotComment> latestAggregateTableMap = new TreeMap<>(STAGE_ID_COMPARATOR);

        for (Map.Entry<Stage, List<UserRunDetails>> entry : stageMap.entrySet()) {
            Stage stage = entry.getKey();
            List<UserRunDetails> runs = entry.getValue();

            try {
                BotComment latestComment = writeAggregateTable(stage, runs);
                latestAggregateTableMap.put(stage, latestComment);
            } catch (Exception e) {
                LOG.error("failed to write table for stage " + stage + " at " + submission.getUrl(), e);
            }
        }

        if (summaryComment != null) {
            // if all the runs got deleted from a submission, we still need to update the summary table
            updateSummaryTable(latestAggregateTableMap);
        }
    }

    private BotComment writeAggregateTable(Stage stage,
                                           List<UserRunDetails> runs) {
        String submissionUrl = submission.getUrl();

        final String commentBody;
        if (stage.getStageType() == StageType.COMPETITION) {
            commentBody = formatter.formatCompetitionRun(runs, submissionUrl);
        } else {
            commentBody = formatter.formatStage(runs, stage, submissionUrl);
        }

        LOG.debug("generated comment:\n" + commentBody);

        BotComment existing = aggregateTableMap.get(stage);
        if (existing == null) {
            // no bot comment exists yet

            if (LOG.isDebugEnabled()) {
                LOG.debug("no comment for " + stage + " yet in " + submissionUrl + ", creating new comment");
            }
            Comment reply = redditClient.comment(summaryComment.getCommentId()).reply(commentBody);
            return new BotComment(reply.getId(), commentBody);
        } else if (!Objects.equals(existing.getContent(), commentBody)) {
            // we've already written a comment for this stage but it's outdated

            if (LOG.isDebugEnabled()) {
                LOG.debug("comment for " + stage + " outdated in " + submissionUrl +
                                  ", updating comment " + existing.getCommentId());
            }
            redditClient.comment(existing.getCommentId()).edit(commentBody);
            return new BotComment(existing.getCommentId(), commentBody);
        } else {
            // no need to write anything, existing bot comment already has correct content
            if (LOG.isDebugEnabled()) {
                LOG.debug("comment for " + stage + " already up to date in " + submissionUrl);
            }
            return existing;
        }
    }

    private void removeEmptyAggregateTables(Map<Stage, List<UserRunDetails>> stageMap) {
        Set<Stage> emptyStages = Sets.difference(aggregateTableMap.keySet(), stageMap.keySet());
        for (Stage stage : emptyStages) {
            BotComment comment = aggregateTableMap.get(stage);
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
        } else if (!Objects.equals(existing.getContent(), botReplyBody)) {
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

    private static final Pattern STAGE_PATTERN = Pattern.compile("^" + Formatter.STAGE_HEADER_PREFIX + "(.+)\n");
    static Stage getAggregateStage(String comment) {
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

    private boolean isSummaryComment(String comment) {
        // assumes the we've already checked that the configured bot user is the commenter
        if (comment.startsWith(Formatter.SUMMARY_HEADER)) {
            return true;
        }
        return false;
    }
}
