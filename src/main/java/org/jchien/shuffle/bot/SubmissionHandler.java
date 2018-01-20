package org.jchien.shuffle.bot;

import net.dean.jraw.RedditClient;
import net.dean.jraw.models.PublicContribution;
import net.dean.jraw.models.Submission;
import net.dean.jraw.tree.CommentNode;
import net.dean.jraw.tree.RootCommentNode;
import org.jchien.shuffle.model.BotComment;
import org.jchien.shuffle.model.RunDetails;
import org.jchien.shuffle.model.Stage;
import org.jchien.shuffle.model.StageType;
import org.jchien.shuffle.model.UserRunDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
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
    private Map<Stage, BotComment> botCommentMap = new TreeMap<>(STAGE_ID_COMPARATOR);

    private List<UserRunDetails> invalidRuns = new ArrayList<>();

    private CommentHandler commentHandler = new CommentHandler();

    public void handleSubmission(RedditClient redditClient, Submission submission) {
        processComments(redditClient, submission);

        writeBotComments(redditClient, submission.getId(), submission.getUrl());

        // todo pm or reply to users with bad comments
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

    private void processComment(Submission submission, CommentNode<PublicContribution<?>> commentNode, String botUser) {
        PublicContribution<?> comment = commentNode.getSubject();

        if (Objects.equals(comment.getId(), submission.getId())) {
            // seems like the submission itself gets included with a null body
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(comment.getId() +
                    " | " + comment.getAuthor() +
                    " | " + submission.getTitle() +
                    " | c: " + comment.getCreated() +
                    " | e: " + comment.getEdited());
            LOG.debug(comment.getBody());
        }

        if (!botUser.equals(comment.getAuthor())) {
            parseRuns(submission, comment);
        } else {
            cacheBotComments(comment.getId(), comment.getBody());
        }
    }

    private void parseRuns(Submission submission, PublicContribution<?> comment) {
        List<RunDetails> runs = commentHandler.getRunDetails(comment.getBody());

        for (RunDetails run : runs) {
            if (run.hasException()) {
                LOG.warn("failed to parse run from comment " + RedditUtils.getCommentPermalink(submission.getUrl(), comment.getId()), run.getException());
            } else {
                LOG.info(run.toString());
            }
        }

        List<UserRunDetails> userRuns = getValidRuns(runs, comment.getAuthor(), comment.getId());

        for (UserRunDetails urd : userRuns) {
            addStageRun(urd);
        }

        List<UserRunDetails> badRuns = getInvalidRuns(runs, comment.getAuthor(), comment.getId());
        invalidRuns.addAll(badRuns);
    }

    private List<UserRunDetails> getValidRuns(List<RunDetails> runs, String commentAuthor, String commentId) {
        return runs.stream()
                .filter(run -> !run.hasException())
                .map(run -> new UserRunDetails(commentAuthor, commentId, run))
                .collect(Collectors.toList());
    }

    private List<UserRunDetails> getInvalidRuns(List<RunDetails> runs, String commentAuthor, String commentId) {
        return runs.stream()
                .filter(RunDetails::hasException)
                .map(run -> new UserRunDetails(commentAuthor, commentId, run))
                .collect(Collectors.toList());
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

    private void cacheBotComments(String commentId, String commentBody) {
        Stage stage = commentHandler.getAggregateStage(commentBody);

        if (stage == null) {
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("found stage " + stage + " from comment " + commentId);
        }

        botCommentMap.put(stage, new BotComment(commentId, commentBody));
    }

    private void writeBotComments(RedditClient redditClient, String submissionId, String submissionUrl) {
        Formatter f = new Formatter();

        for (Map.Entry<Stage, List<UserRunDetails>> entry : stageMap.entrySet()) {
            Stage stage = entry.getKey();
            List<UserRunDetails> runs = entry.getValue();
            final String commentBody;
            if (stage.getStageType() == StageType.COMPETITION) {
                commentBody = f.formatCompetitionRun(runs, submissionUrl);
            } else {
                commentBody = f.formatStage(runs, stage, submissionUrl);
            }

            LOG.debug("generated comment:\n" + commentBody);

            BotComment existing = botCommentMap.get(stage);
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
    }
}
