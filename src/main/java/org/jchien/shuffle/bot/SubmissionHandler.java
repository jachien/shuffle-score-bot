package org.jchien.shuffle.bot;

import net.dean.jraw.RedditClient;
import net.dean.jraw.models.PublicContribution;
import net.dean.jraw.models.Submission;
import net.dean.jraw.tree.CommentNode;
import net.dean.jraw.tree.RootCommentNode;
import org.jchien.shuffle.cache.BotComment;
import org.jchien.shuffle.model.RunDetails;
import org.jchien.shuffle.model.Stage;
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

/**
 * @author jchien
 */
public class SubmissionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SubmissionHandler.class);

    private static final Comparator<String> STAGE_ID_COMPARATOR = Comparator.nullsFirst(Comparator.naturalOrder());

    // stage id -> runs
    private Map<String, List<UserRunDetails>> stageMap = new TreeMap<>(STAGE_ID_COMPARATOR);

    // stage id -> bot comment data
    private Map<String, BotComment> botCommentMap = new TreeMap<>(STAGE_ID_COMPARATOR);

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
                PublicContribution<?> comment = it.next().getSubject();
                if (Objects.equals(comment.getId(), submission.getId())) {
                    // seems like the submission itself gets included with a null body
                    continue;
                }

                try {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(comment.getId() +
                                " | " + comment.getAuthor() +
                                " | " + submission.getTitle() +
                                " | c: " + comment.getCreated() +
                                " | e: " + comment.getEdited());
                        LOG.debug(comment.getBody());
                    }

                    if (botUser.equals(comment.getAuthor())) {
                        cacheBotComments(comment.getId(), comment.getBody());
                    }
                } catch (Exception e) {
                    String commentUrl = RedditUtils.getCommentPermalink(submission.getUrl(), comment.getId());
                    LOG.warn("failed to handle comment " + commentUrl, e);
                }
                cnt++;
            }

            LOG.debug(cnt + " comments, " + submission.getCommentCount() + " comments according to submission");
            // it's possible comments were written between submission pagination and comment retrieval, I just want to see that these numbers are close
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
        String stageId = Stage.normalizeStageId(urd.getRunDetails().getStage());
        List<UserRunDetails> stageRuns = stageMap.get(stageId);
        if (stageRuns == null) {
            stageRuns = new ArrayList<>();
            stageMap.put(stageId, stageRuns);
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

        botCommentMap.put(stage.getStageId(), new BotComment(commentId, commentBody));
    }

    private void writeBotComments(RedditClient redditClient, String submissionId, String submissionUrl) {
        Formatter f = new Formatter();

        for (Map.Entry<String, List<UserRunDetails>> entry : stageMap.entrySet()) {
            String stageId = entry.getKey();
            List<UserRunDetails> runs = entry.getValue();
            // todo make stageMap a mapping of Stage -> runs and make this check based off StageType
            final String commentBody;
            if (stageId == null) {
                commentBody = f.formatCompetitionRun(runs, submissionUrl);
            } else {
                commentBody = f.formatStage(runs, stageId, submissionUrl);
            }

            LOG.debug("generated comment:\n" + commentBody);

            BotComment existing = botCommentMap.get(stageId);
            if (existing == null) {
                // no bot comment exists yet

                if (LOG.isDebugEnabled()) {
                    LOG.debug("no comment for " + stageId + " yet in " + submissionUrl + ", creating new comment");
                }

                // hack to get around newlines being stripped by okhttp's HttpUrl.canonicalize()
                String jrawComment = f.getStringForJrawCommenting(commentBody);

                redditClient.submission(submissionId)
                        .reply(jrawComment);
            } else if (!Objects.equals(existing.getContent(), commentBody)) {
                // we've already written a comment for this stage but it's outdated

                if (LOG.isDebugEnabled()) {
                    LOG.debug("comment for " + stageId + " outdated in " + submissionUrl +
                            ", updating comment " + existing.getCommentId());
                }

                // hack to get around newlines being stripped by okhttp's HttpUrl.canonicalize()
                String jrawComment = f.getStringForJrawCommenting(commentBody);

                redditClient.comment(existing.getCommentId())
                        .edit(jrawComment);
            } else {
                // no need to write anything, existing bot comment already has correct content
                if (LOG.isDebugEnabled()) {
                    LOG.debug("comment for " + stageId + " already up to date in " + submissionUrl);
                }
            }
        }
    }
}
