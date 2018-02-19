package org.jchien.shuffle.handler;

import net.dean.jraw.RedditClient;
import net.dean.jraw.models.PublicContribution;
import net.dean.jraw.models.Submission;
import net.dean.jraw.tree.CommentNode;
import net.dean.jraw.tree.RootCommentNode;
import org.jchien.shuffle.formatter.FormatterUtils;
import org.jchien.shuffle.model.InvalidRuns;
import org.jchien.shuffle.model.ParsedComment;
import org.jchien.shuffle.model.RunDetails;
import org.jchien.shuffle.model.Stage;
import org.jchien.shuffle.model.UserRunDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static java.util.Comparator.*;

/**
 * Handles all the work for processing a submission.
 *
 * @author jchien
 */
public class SubmissionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SubmissionHandler.class);

    private static final Comparator<Stage> STAGE_ID_COMPARATOR =
            comparing(Stage::getStageType)
            .thenComparing(Stage::getStageId, nullsFirst(String.CASE_INSENSITIVE_ORDER));

    // stage -> runs
    private Map<Stage, List<UserRunDetails>> stageMap = new TreeMap<>(STAGE_ID_COMPARATOR);

    // user comment id -> author
    private Map<String, String> authorMap = new TreeMap<>();

    private Map<String, InvalidRuns> invalidRunMap = new TreeMap<>();

    private final RedditClient redditClient;

    private final Submission submission;

    private BotCommentHandler botCommentHandler;

    public SubmissionHandler(RedditClient redditClient, Submission submission) {
        this.redditClient = redditClient;
        this.submission = submission;
        this.botCommentHandler = new BotCommentHandler(redditClient, submission);
    }

    public void handleSubmission() {
        processComments(redditClient, submission);

        botCommentHandler.createOrUpdateBotComments(stageMap, authorMap, invalidRunMap);
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
                    processComment(node, botUser);
                } catch (Throwable t) {
                    String commentUrl = FormatterUtils.getCommentPermalink(submission.getUrl(), node.getSubject().getId());
                    LOG.error("unable to process comment " + commentUrl, t);
                }
                cnt++;
            }

            LOG.debug(cnt + " comments, " + submission.getCommentCount() + " comments according to submission");
            // it's possible comments were written between submission pagination and comment retrieval, I just want to see that these numbers are close
        }
    }

    private void processComment(CommentNode<PublicContribution<?>> commentNode, String botUser) {
        PublicContribution<?> comment = commentNode.getSubject();

        if (Objects.equals(comment.getId(), submission.getId())) {
            // seems like the submission itself gets included with a null body
            return;
        }

        String commentBody = comment.getBody();
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
            processUserComment(comment, commentBody);
        } else {
            botCommentHandler.processBotComment(comment.getId(), commentBody, parentId);
        }
    }

    private void processUserComment(PublicContribution<?> comment, String commentBody) {
        UserCommentHandler userCommentHandler = new UserCommentHandler(comment, commentBody);
        ParsedComment parsedComment = userCommentHandler.parseRuns();

        Instant lastModDate = getLastModifiedDate(comment.getCreated(), comment.getEdited());
        for (UserRunDetails urd : parsedComment.getValidRuns()) {
            addStageRun(urd);
        }

        List<UserRunDetails> invalidRuns = parsedComment.getInvalidRuns();
        if (invalidRuns.size() > 0) {
            String commentId = comment.getId();
            invalidRunMap.put(commentId, new InvalidRuns(commentId, lastModDate, invalidRuns));

            String commentPermalink = FormatterUtils.getCommentPermalink(submission.getUrl(), commentId);
            for (UserRunDetails run : invalidRuns) {
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

    private String getParentId(CommentNode<?> node) {
        CommentNode<?> parent = node.getParent();
        if (parent != null) {
            // for top level comments this seems to return the submission id
            return parent.getSubject().getId();
        }
        return null;
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
}
