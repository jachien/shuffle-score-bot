package org.jchien.shuffle.bot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author jchien
 */
@Configuration
@ConfigurationProperties(prefix="shufflescorebot")
public class ScoreBotConfig {
    private String username;

    private String password;

    private String clientId;

    private String clientSecret;

    private long pollDelayMillis;

    private List<String> subreddits;

    private int pollDays;

    // process submissions and their comments only after max(time(now-pollDays), stopDate)
    // this feature helps avoid massive resubmission of comments or PMs when formatting changes
    private String stopDate = null;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public long getPollDelayMillis() {
        return pollDelayMillis;
    }

    public void setPollDelayMillis(long pollDelayMillis) {
        this.pollDelayMillis = pollDelayMillis;
    }

    public List<String> getSubreddits() {
        return subreddits;
    }

    public void setSubreddits(List<String> subreddits) {
        this.subreddits = subreddits;
    }

    public int getPollDays() {
        return pollDays;
    }

    public void setPollDays(int pollDays) {
        this.pollDays = pollDays;
    }

    public String getStopDate() {
        return stopDate;
    }

    public void setStopDate(String stopDate) {
        this.stopDate = stopDate;
    }
}
