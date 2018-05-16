package org.jchien.shuffle.bot;

import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkAdapter;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.OAuthHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author jchien
 */
@Component
public class ScoreBotConfig {
    private static final UserAgent USER_AGENT = new UserAgent(
            "server",
            "org.jchien.shuffle",
            "0.1",
            "jcrixus");

    private ScoreBotPropsConfig props;

    @Autowired
    public ScoreBotConfig(ScoreBotPropsConfig props) {
        this.props = props;
    }

    @Bean
    public RedditClient getRedditClient() {
        Credentials credentials = Credentials.script(
                props.getUsername(),
                props.getPassword(),
                props.getClientId(),
                props.getClientSecret());

        NetworkAdapter adapter = new OkHttpNetworkAdapter(USER_AGENT);

        RedditClient redditClient = OAuthHelper.automatic(adapter, credentials);

        return redditClient;
    }
}
