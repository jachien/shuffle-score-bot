package org.jchien.shuffle.bot;

import com.google.common.collect.Sets;
import net.dean.jraw.RedditClient;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.PublicContribution;
import net.dean.jraw.models.Submission;
import net.dean.jraw.references.InboxReference;
import net.dean.jraw.tree.CommentNode;
import net.dean.jraw.tree.RootCommentNode;
import org.jchien.shuffle.model.BotComment;
import org.jchien.shuffle.model.InvalidRuns;
import org.jchien.shuffle.model.Pokemon;
import org.jchien.shuffle.model.RunDetails;
import org.jchien.shuffle.model.RunDetailsBuilder;
import org.jchien.shuffle.model.Stage;
import org.jchien.shuffle.model.StageType;
import org.jchien.shuffle.model.UserRunDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Comparator.*;

/**
 * @author jchien
 */
public class SubmissionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SubmissionHandler.class);

    private static final Comparator<Stage> STAGE_ID_COMPARATOR =
            comparing(Stage::getStageType)
            .thenComparing(Stage::getStageId, nullsFirst(String.CASE_INSENSITIVE_ORDER));

    // stage -> runs
    private Map<Stage, List<UserRunDetails>> stageMap = new TreeMap<>(STAGE_ID_COMPARATOR);

    private BotComment summaryComment = null;

    // stage -> bot comment data
    private Map<Stage, BotComment> aggregateTableMap = new TreeMap<>(STAGE_ID_COMPARATOR);

    // temporary list for removing old tables
    private List<BotComment> deprecatedAggregateTableMap = new ArrayList<>();

    // user comment id -> bot reply
    private Map<String, BotComment> botReplyMap = new TreeMap<>();

    // user comment id -> author
    private Map<String, String> authorMap = new TreeMap<>();

    private Map<String, InvalidRuns> invalidRunMap = new TreeMap<>();

    private CommentHandler commentHandler = new CommentHandler();

    private Formatter formatter = new Formatter();

    public void handleSubmission(RedditClient redditClient, Submission submission) {
        processComments(redditClient, submission);

        writeAggregateTables(redditClient, submission.getId(), submission.getUrl());

        removeEmptyAggregateTables(redditClient);

        removeDeprecatedAggregateTables(redditClient);

        writeBotReplies(redditClient, submission.getUrl());
    }

    private void processComments(RedditClient redditClient, Submission submission) {
        final String botUser = redditClient.me().getUsername();

        if (submission.getCommentCount() > 0) {
            RootCommentNode root = redditClient.submission(submission.getId()).comments();
            root.loadFully(redditClient);

            int cnt = 0;
            Iterator<CommentNode<PublicContribution<?>>> it = root.walkTree().iterator();
            while (it.hasNext()) {
                CommentNode<PublicContribution<?>> node = it.next();
                try {
                    processComment(submission, node, botUser);
                } catch (Throwable t) {
                    String commentUrl = RedditUtils.getCommentPermalink(submission.getUrl(), node.getSubject().getId());
                    LOG.error("unable to process comment " + commentUrl, t);
                }
                cnt++;
            }

            LOG.debug(cnt + " comments, " + submission.getCommentCount() + " comments according to submission");
            // it's possible comments were written between submission pagination and comment retrieval, I just want to see that these numbers are close
        }
    }

    private String getParentId(CommentNode<?> node) {
        CommentNode<?> parent = node.getParent();
        if (parent != null) {
            // for top level comments this seems to return the submission id
            return parent.getSubject().getId();
        }
        return null;
    }

    private void processComment(Submission submission, CommentNode<PublicContribution<?>> commentNode, String botUser) {
        PublicContribution<?> comment = commentNode.getSubject();

        if (Objects.equals(comment.getId(), submission.getId())) {
            // seems like the submission itself gets included with a null body
            return;
        }

        // unescaping html entities is a workaround until https://github.com/mattbdean/JRAW/issues/225 is fixed
        String commentBody = unescapeHtmlEntities(comment.getBody());
        String parentId = getParentId(commentNode);

        if (LOG.isDebugEnabled()) {
            LOG.debug("id: " + comment.getId() +
                    " | pid: " + parentId +
                    " | author: " + comment.getAuthor() +
                    " | " + submission.getTitle() +
                    " | c: " + comment.getCreated() +
                    " | e: " + comment.getEdited());
            LOG.debug(commentBody);
        }

        // we only care about a subset for PMing but it's simplest to just store this for every comment
        authorMap.put(comment.getId(), comment.getAuthor());

        if (!botUser.equals(comment.getAuthor())) {
            parseRuns(submission.getUrl(),
                      comment.getId(),
                      comment.getAuthor(),
                      commentBody,
                      comment.getCreated(),
                      comment.getEdited());
        } else {
            cacheComment(comment.getId(), commentBody, parentId, submission.getId());
        }
    }

    private void parseRuns(String submissionUrl,
                           String commentId,
                           String commentAuthor,
                           String commentBody,
                           Date createTime,
                           Date editTime) {
        Throwable rosterThrowable = null;
        Map<String, Pokemon> roster;
        try {
            roster = commentHandler.getRoster(commentBody);
        } catch (Throwable t) {
            roster = new LinkedHashMap<>();
            rosterThrowable = t;
        }

        List<RunDetails> runs = commentHandler.getRunDetails(commentBody, roster);

        Instant lastModDate = getLastModifiedDate(createTime, editTime);

        List<UserRunDetails> userRuns = getValidRuns(runs, commentAuthor, commentId);

        for (UserRunDetails urd : userRuns) {
            addStageRun(urd);
        }

        List<UserRunDetails> badRuns = getInvalidRuns(runs, commentAuthor, commentId, rosterThrowable);

        if (badRuns.size() > 0) {
            invalidRunMap.put(commentId, new InvalidRuns(commentId, lastModDate, badRuns));

            String commentPermalink = RedditUtils.getCommentPermalink(submissionUrl, commentId);
            for (UserRunDetails run : badRuns) {
                for (Throwable t : run.getRunDetails().getThrowables()) {
                    LOG.warn("bad run for " + commentPermalink, t);
                }
            }
        }
    }

    private Instant getLastModifiedDate(Date createTime, Date editTime) {
        if (editTime != null) {
            return editTime.toInstant();
        }
        return createTime.toInstant();
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

    private void addStageRun(UserRunDetails urd) {
        RunDetails run = urd.getRunDetails();
        String stageId = Stage.normalizeStageId(run.getStage());
        Stage stage = new Stage(run.getStageType(), stageId);

        List<UserRunDetails> stageRuns = stageMap.get(stage);
        if (stageRuns == null) {
            stageRuns = new ArrayList<>();
            stageMap.put(stage, stageRuns);
        }
        stageRuns.add(urd);
    }

    private void cacheComment(String commentId, String commentBody, String parentId, String submissionId) {
        boolean isSummaryTable = cacheSummaryTable(commentId, commentBody);

        boolean isAggregateTable = cacheAggregateTable(commentId, commentBody, parentId, submissionId);

        if (!isSummaryTable && !isAggregateTable) {
            cacheBotReply(commentId, commentBody, parentId);
        }
    }

    // return true if this comment was a summary table
    private boolean cacheSummaryTable(String commentId, String commentBody) {
        if (commentHandler.isSummaryComment(commentBody)) {
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
        Stage stage = commentHandler.getAggregateStage(commentBody);

        if (stage == null) {
            return false;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("found stage " + stage + " from comment " + commentId);
        }

        if (Objects.equals(parentId, submissionId)) {
            deprecatedAggregateTableMap.add(new BotComment(commentId, commentBody));
        } else {
            aggregateTableMap.put(stage, new BotComment(commentId, commentBody));
        }

        return true;
    }

    private void cacheBotReply(String commentId, String commentBody, String parentId) {
        botReplyMap.put(parentId, new BotComment(commentId, commentBody));
    }

    private BotComment createSummaryTable(RedditClient redditClient, String submissionId) {
        Comment comment = redditClient.submission(submissionId).reply(Formatter.SUMMARY_HEADER);
        return new BotComment(comment.getId(), comment.getBody());
    }

    private void updateSummaryTable(RedditClient redditClient, String submissionUrl) {
        Map<Stage, BotComment> nonEmptyTableMap = aggregateTableMap.keySet()
                .stream()
                .filter(key -> stageMap.containsKey(key))
                .collect(Collectors.toMap(Function.identity(), key -> aggregateTableMap.get(key)));

        String summaryTable = formatter.formatSummary(submissionUrl, nonEmptyTableMap);
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

    private void writeAggregateTables(RedditClient redditClient, String submissionId, String submissionUrl) {
        if (!stageMap.isEmpty() && summaryComment == null) {
            summaryComment = createSummaryTable(redditClient, submissionId);
        }

        for (Map.Entry<Stage, List<UserRunDetails>> entry : stageMap.entrySet()) {
            Stage stage = entry.getKey();
            List<UserRunDetails> runs = entry.getValue();
            try {
                writeAggregateTable(redditClient, submissionUrl, stage, runs);
            } catch (Exception e) {
                LOG.error("failed to write table for stage " + stage + " at " + submissionUrl);
            }
        }

        if (summaryComment != null) {
            // if all the runs got deleted from a submission, we still need to update the summary table
            updateSummaryTable(redditClient, submissionUrl);
        }
    }

    private void writeAggregateTable(RedditClient redditClient,
                                     String submissionUrl,
                                     Stage stage,
                                     List<UserRunDetails> runs) {
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
            redditClient.comment(summaryComment.getCommentId()).reply(commentBody);
        } else if (!Objects.equals(existing.getContent(), commentBody)) {
            // we've already written a comment for this stage but it's outdated

            if (LOG.isDebugEnabled()) {
                LOG.debug("comment for " + stage + " outdated in " + submissionUrl +
                                  ", updating comment " + existing.getCommentId());
            }
            redditClient.comment(existing.getCommentId()).edit(commentBody);
        } else {
            // no need to write anything, existing bot comment already has correct content
            if (LOG.isDebugEnabled()) {
                LOG.debug("comment for " + stage + " already up to date in " + submissionUrl);
            }
        }
    }

    private void removeEmptyAggregateTables(RedditClient redditClient) {
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

    private void removeDeprecatedAggregateTables(RedditClient redditClient) {
        for (BotComment comment : deprecatedAggregateTableMap) {
            String commentId = comment.getCommentId();

            if (LOG.isDebugEnabled()) {
                LOG.debug("removing deprecated aggregate table with comment id " + commentId);
            }

            redditClient.comment(commentId).delete();
        }
    }

    private void writeBotReplies(RedditClient redditClient, String submissionUrl) {
        Set<String> badCommentIds = new TreeSet<>();

        for (Map.Entry<String, InvalidRuns> entry : invalidRunMap.entrySet()) {
            String userCommentId = entry.getKey();
            badCommentIds.add(userCommentId);

            InvalidRuns invalidRuns = entry.getValue();
            Instant lastModDate = invalidRuns.getLastModifiedDate();
            List<UserRunDetails> urds = invalidRuns.getRuns();
            String botReplyBody = InvalidRunFormatter.formatInvalidRuns(lastModDate, urds);
            createOrUpdateReply(redditClient, userCommentId, botReplyBody, submissionUrl);
        }

        // now update replies for people who fixed their previously bad comment
        Set<String> okCommentIds = Sets.difference(botReplyMap.keySet(), badCommentIds);
        for (String okCommentId : okCommentIds) {
            String botReplyBody = InvalidRunFormatter.getAllGoodMessage();
            createOrUpdateReply(redditClient, okCommentId, botReplyBody, submissionUrl);
        }
    }

    private void createOrUpdateReply(RedditClient redditClient, String userCommentId, String botReplyBody, String submissionUrl) {
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

    private String unescapeHtmlEntities(String s) {
        return s.replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&");
    }
}
