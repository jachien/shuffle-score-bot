package org.jchien.shuffle.bot;

import net.dean.jraw.RedditClient;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.TraversalMethod;
import org.jchien.shuffle.cache.CommentCache;
import org.jchien.shuffle.model.RunDetails;
import org.jchien.shuffle.model.UserRunDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author jchien
 */
public class SubmissionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SubmissionHandler.class);

    private CommentCache commentCache; // todo pass in from ScoreBot

    public void handleSubmission(RedditClient redditClient, Submission submission) {
        commentCache = new CommentCache(submission.getId()); // todo remove this when ScoreBot stores commentId -> commentCache
        parseComments(redditClient, submission);
        // todo write aggregate comments
    }

    private void parseComments(RedditClient redditClient, Submission submission) {
        if (submission.getCommentCount() > 0) {
            CommentNode root = redditClient.getSubmission(submission.getId()).getComments();
            int cnt = 0;
            root.loadFully(redditClient);
            Iterable<CommentNode> nodes = root.walkTree(TraversalMethod.PRE_ORDER);
            for (CommentNode node : nodes) {
                parseComment(submission, node);
                cnt++;
            }

            LOG.debug(cnt + " comments, " + submission.getCommentCount() + " comments according to submission");
            // it's possible comments were written between submission pagination and comment retrieval, I just want to see that these numbers are close
        }
    }

    private void parseComment(Submission submission, CommentNode node) {
        Comment comment = node.getComment();
        LOG.info(comment.getAuthor() + " | " + submission.getTitle() + " | c: " +  comment.getCreated() + " | e: " + comment.getEditDate());
        LOG.info(comment.getBody());

        CommentHandler handler = new CommentHandler();

        List<RunDetails> runs = handler.getRunDetails(comment.getBody());
        for (RunDetails run : runs) {
            if (run.hasException()) {
                // todo pm or reply to user that their syntax / format / grammar is incorrect
                LOG.warn("failed to parse run from comment " + RedditUtils.getCommentPermalink(submission.getUrl(), comment.getId()), run.getException());
            } else {
                LOG.info(run.toString());
            }
        }

        final long lastModTime;
        if (comment.getEditDate() != null) {
            lastModTime = comment.getEditDate().getTime();
        } else {
            lastModTime = comment.getCreated().getTime();
        }

        List<UserRunDetails> userRuns = runs.stream()
                .filter(run -> !run.hasException())
                .map(run -> new UserRunDetails(comment.getAuthor(), comment.getId(), run))
                .collect(Collectors.toList());

        commentCache.setCommentData(comment.getId(), lastModTime, userRuns);

        // todo pm or reply to user that their syntax / format / grammar is incorrect
        List<RunDetails> badRuns = runs.stream()
                .filter(run -> run.hasException())
                .collect(Collectors.toList());
    }
}
