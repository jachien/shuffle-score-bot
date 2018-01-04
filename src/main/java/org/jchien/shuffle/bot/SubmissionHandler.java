package org.jchien.shuffle.bot;

import net.dean.jraw.RedditClient;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.TraversalMethod;
import org.jchien.shuffle.model.RunDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author jchien
 */
public class SubmissionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SubmissionHandler.class);

    public void handleSubmission(RedditClient redditClient, Submission submission) {
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
                Comment comment = node.getComment();
                LOG.info(comment.getAuthor() + " | " + submission.getTitle() + " | c: " +  comment.getCreated() + " | e: " + comment.getEditDate());
                LOG.info(comment.getBody());

                CommentHandler handler = new CommentHandler();
                List<RunDetails> runs = handler.getRunDetails(comment.getBody());
                for (RunDetails run : runs) {
                    if (run.hasException()) {
                        LOG.warn("failed to parse run from comment " + comment.getId(), run.getException());
                    } else {
                        LOG.info(run.toString());
                    }
                }

                cnt++;
            }

            LOG.debug(cnt + " comments, " + submission.getCommentCount() + " comments according to submission");
            // it's possible comments were written between submission pagination and comment retrieval, I just want to see that these numbers are close
        }
    }
}
