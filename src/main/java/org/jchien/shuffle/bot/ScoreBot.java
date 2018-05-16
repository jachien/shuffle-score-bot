package org.jchien.shuffle.bot;

import net.dean.jraw.RedditClient;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.SubredditSort;
import net.dean.jraw.pagination.Paginator;
import org.jchien.shuffle.handler.SubmissionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Iterator;

/**
 * @author jchien
 */
@Component
public class ScoreBot {
    private static final Logger LOG = LoggerFactory.getLogger(ScoreBot.class);

    private ScoreBotPropsConfig config;

    private ConfigUtils configUtils;

    private RedditClient redditClient;

    @Autowired
    public ScoreBot(ScoreBotPropsConfig config,
                    ConfigUtils configUtils,
                    RedditClient redditClient) {
        this.config = config;
        this.configUtils = configUtils;
        this.redditClient = redditClient;
    }

    @Scheduled(fixedDelayString = "${shufflescorebot.pollDelayMillis}")
    public void poll() {
        for (String subreddit : config.getSubreddits()) {
            try {
                poll(subreddit);
            } catch (Exception e) {
                LOG.error("problem polling subreddit", e);
            }
        }
    }

    private void poll(String subreddit) {
        // We're not using CommentStream because there's no sort by last edit option.
        // We want to be able to see bot commands added to any comments in the last n days,
        // and listings only return up to 1000 things. So we need to do something much slower
        // and inefficient in order to get all comments in the desired time window.

        LocalDateTime endTime = configUtils.getEndTime();
        LOG.info("processing posts until " + endTime);

        Paginator<Submission> paginator = getPaginator(redditClient, subreddit);

        int totalComments = 0;
        int totalThreads = 0;

        long start = System.currentTimeMillis();
        boolean done = false;
        Iterator<Listing<Submission>> it = paginator.iterator();

        while (!done && it.hasNext()) {
            Listing<Submission> submissions = it.next();

            // todo parallelize this
            for (Submission submission : submissions) {
                LocalDateTime postTime = LocalDateTime.ofInstant(
                        submission.getCreated().toInstant(),
                        ZoneId.systemDefault());

                if (postTime.isBefore(endTime)) {
                    done = true;
                    break;
                }

                if (submission.isLocked()) {
                    continue;
                }

                SubmissionHandler submissionHandler = new SubmissionHandler(redditClient, submission);
                submissionHandler.handleSubmission();

                totalThreads++;
                totalComments += submission.getCommentCount();
            }
        }

        long elapsed = (System.currentTimeMillis() - start) / 1000;
        LOG.info(elapsed + " seconds, threads: " + totalThreads + ", comments: " + totalComments);
    }

    private Paginator<Submission> getPaginator(RedditClient redditClient, String subreddit) {
        return redditClient.subreddit(subreddit)
                .posts()
                .sorting(SubredditSort.NEW)
                .limit(100)
                .build();
    }
}
