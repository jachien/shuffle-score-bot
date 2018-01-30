package org.jchien.shuffle.bot;

import com.google.common.collect.Sets;
import net.dean.jraw.RedditClient;
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

    // stage -> bot comment data
    private Map<Stage, BotComment> aggregateTableMap = new TreeMap<>(STAGE_ID_COMPARATOR);

    // user comment id -> bot reply
    private Map<String, BotComment> botReplyMap = new TreeMap<>();

    // user comment id -> author
    private Map<String, String> authorMap = new TreeMap<>();

    private Map<String, InvalidRuns> invalidRunMap = new TreeMap<>();

    private CommentHandler commentHandler = new CommentHandler();

    public void handleSubmission(RedditClient redditClient, Submission submission) {
        processComments(redditClient, submission);

        writeAggregateTables(redditClient, submission.getId(), submission.getUrl());

        removeEmptyAggregateTables(redditClient);

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
                } catch (Exception e) {
                    String commentUrl = RedditUtils.getCommentPermalink(submission.getUrl(), node.getSubject().getId());
                    LOG.warn("unable to process comment " + commentUrl, e);
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
            cacheAggregateTables(comment.getId(), commentBody);
            cacheBotReplies(comment.getId(), commentBody, parentId, submission.getId());
        }
    }

    private void parseRuns(String submissionUrl,
                           String commentId,
                           String commentAuthor,
                           String commentBody,
                           Date createTime,
                           Date editTime) {
        Exception rosterException = null;
        Map<String, Pokemon> roster;
        try {
            roster = commentHandler.getRoster(commentBody);
        } catch (Exception e) {
            roster = new LinkedHashMap<>();
            rosterException = e;
        }

        List<RunDetails> runs = commentHandler.getRunDetails(commentBody, roster);

        Instant lastModDate = getLastModifiedDate(createTime, editTime);

        List<UserRunDetails> userRuns = getValidRuns(runs, commentAuthor, commentId);

        for (UserRunDetails urd : userRuns) {
            addStageRun(urd);
        }

        List<UserRunDetails> badRuns = getInvalidRuns(runs, commentAuthor, commentId, rosterException);

        if (badRuns.size() > 0) {
            invalidRunMap.put(commentId, new InvalidRuns(commentId, lastModDate, badRuns));

            String commentPermalink = RedditUtils.getCommentPermalink(submissionUrl, commentId);
            for (UserRunDetails run : badRuns) {
                for (Exception e : run.getRunDetails().getExceptions()) {
                    LOG.warn("bad run for " + commentPermalink, e);
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
                .filter(run -> !run.hasException())
                .map(run -> new UserRunDetails(commentAuthor, commentId, run))
                .collect(Collectors.toList());
    }

    private List<UserRunDetails> getInvalidRuns(List<RunDetails> runs,
                                                String commentAuthor,
                                                String commentId,
                                                Exception rosterException) {

        List<UserRunDetails> ret = new ArrayList<>();

        if (rosterException != null) {
            RunDetails rosterDetails = new RunDetailsBuilder()
                    .setStageType(StageType.ROSTER)
                    .setExceptions(Arrays.asList(rosterException))
                    .build();
            UserRunDetails rosterUrd = new UserRunDetails(commentAuthor, commentId, rosterDetails);
            ret.add(rosterUrd);
        }

        runs.stream()
                .filter(RunDetails::hasException)
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

    private void cacheAggregateTables(String commentId, String commentBody) {
        Stage stage = commentHandler.getAggregateStage(commentBody);

        if (stage == null) {
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("found stage " + stage + " from comment " + commentId);
        }

        aggregateTableMap.put(stage, new BotComment(commentId, commentBody));
    }

    private void cacheBotReplies(String commentId, String commentBody, String parentId, String submissionId) {
        if (Objects.equals(parentId, submissionId) || parentId == null) {
            // this is a top level reply, so it's an aggregate table
            return;
        }

        botReplyMap.put(parentId, new BotComment(commentId, commentBody));
    }

    private void writeAggregateTables(RedditClient redditClient, String submissionId, String submissionUrl) {
        for (Map.Entry<Stage, List<UserRunDetails>> entry : stageMap.entrySet()) {
            Stage stage = entry.getKey();
            List<UserRunDetails> runs = entry.getValue();
            try {
                writeAggregateTable(redditClient, submissionId, submissionUrl, stage, runs);
            } catch (Exception e) {
                LOG.error("failed to write table for stage " + stage + " at " + submissionUrl);
            }
        }
    }

    private void writeAggregateTable(RedditClient redditClient,
                                     String submissionId,
                                     String submissionUrl,
                                     Stage stage,
                                     List<UserRunDetails> runs) {
        Formatter f = new Formatter();
        final String commentBody;
        if (stage.getStageType() == StageType.COMPETITION) {
            commentBody = f.formatCompetitionRun(runs, submissionUrl);
        } else {
            commentBody = f.formatStage(runs, stage, submissionUrl);
        }

        LOG.debug("generated comment:\n" + commentBody);

        BotComment existing = aggregateTableMap.get(stage);
        if (existing == null) {
            // no bot comment exists yet

            if (LOG.isDebugEnabled()) {
                LOG.debug("no comment for " + stage + " yet in " + submissionUrl + ", creating new comment");
            }
            redditClient.submission(submissionId).reply(commentBody);
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
