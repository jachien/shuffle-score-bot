package org.jchien.shuffle.bot;

import net.dean.jraw.RedditClient;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.TraversalMethod;
import net.dean.jraw.paginators.Paginator;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.SubredditPaginator;
import org.jchien.shuffle.model.RunDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * @author jchien
 */
@Component
public class ScoreBot {
    private static final Logger LOG = LoggerFactory.getLogger(ScoreBot.class);

    private static final UserAgent USER_AGENT = UserAgent.of(
            "server",
            "org.jchien.shuffle",
            "0.1",
            "jcrixus");

    private ScoreBotConfig config;

    @Autowired
    public ScoreBot(ScoreBotConfig config) {
        this.config = config;
    }

    @Scheduled(fixedDelayString = "${shufflescorebot.pollDelayMillis}")
    public void poll() {
        try {
            for (String subreddit : config.getSubreddits()) {
                poll(subreddit);
            }
        } catch (OAuthException e) {
            LOG.error("problem authenticating", e);
        }
    }

    private void poll(String subreddit) throws OAuthException {
        // We're not using CommentStream because there's sort by last edit option.
        // We want to be able to see bot commands added to any comments in the last n days,
        // and listings only return up to 1000 things. So we need to do something much slower
        // and inefficient in order to get all comments in the desired time window.

        LocalDateTime pollEndTime = getEndTime();
        RedditClient redditClient = getClient();
        Paginator<Submission> paginator = getPaginator(redditClient, subreddit);

        int totalComments = 0;
        int totalThreads = 0;

        long start = System.currentTimeMillis();
        boolean done = false;
        while (!done && paginator.hasNext()) {
            Listing<Submission> posts = paginator.next();

            // todo parallelize this
            for (Submission post : posts) {
                LocalDateTime postTime = LocalDateTime.ofInstant(
                        post.getCreated().toInstant(),
                        ZoneId.systemDefault());

                if (postTime.isBefore(pollEndTime)) {
                    done = true;
                    break;
                }

                if (post.getCommentCount() > 0) {
                    CommentNode root = redditClient.getSubmission(post.getId()).getComments();
                    int cnt = 0;
                    root.loadFully(redditClient);
                    Iterable<CommentNode> nodes = root.walkTree(TraversalMethod.PRE_ORDER);
                    for (CommentNode node : nodes) {
                        Comment comment = node.getComment();
                        LOG.info(comment.getAuthor() + " | " + post.getTitle() + " | c: " +  comment.getCreated() + " | e: " + comment.getEditDate());
                        LOG.info(comment.getBody());

                        CommentHandler handler = new CommentHandler();
                        List<RunDetails> runs = handler.getRunDetails(comment.getBody());
                        for (RunDetails run : runs) {
                            if (run.getException() != null) {
                                LOG.warn("failed to parse run from comment " + comment.getId(), run.getException());
                            } else {
                                LOG.info(run.toString());
                            }
                        }

                        cnt++;
                    }
                    LOG.info(cnt + " comments, " + post.getCommentCount() + " comments according to submission");
                    // it's possible comments were written between submission pagination and comment retrieval, I just want to see that these numbers are close

                    totalThreads++;
                    totalComments += cnt;
                }
            }
        }

        long elapsed = (System.currentTimeMillis() - start) / 1000;
        LOG.info(elapsed + " seconds, threads: " + totalThreads + ", comments: " + totalComments);
    }

    private RedditClient getClient() throws OAuthException {
        RedditClient redditClient = new RedditClient(USER_AGENT);

        Credentials credentials = Credentials.script(
                config.getUsername(),
                config.getPassword(),
                config.getClientId(),
                config.getClientSecret());

        OAuthData authData = redditClient.getOAuthHelper().easyAuth(credentials);

        redditClient.authenticate(authData);

        return redditClient;
    }

    private LocalDateTime getEndTime() {
        int pollDays = config.getPollDays();
        return LocalDateTime.now().minusDays(pollDays);
    }

    private Paginator<Submission> getPaginator(RedditClient redditClient, String subreddit) {
        SubredditPaginator paginator = new SubredditPaginator(redditClient, subreddit);
        paginator.setSorting(Sorting.NEW);
        paginator.setLimit(100);
        return paginator;
    }
}
