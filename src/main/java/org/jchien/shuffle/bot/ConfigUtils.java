package org.jchien.shuffle.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * @author jchien
 */
@Component
public class ConfigUtils {
    private static final String DT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private final DateTimeFormatter dtFormat = DateTimeFormatter.ofPattern(DT_PATTERN);

    private ScoreBotPropsConfig config;

    @Autowired
    public ConfigUtils(ScoreBotPropsConfig config) throws ConfigException {
        this.config = config;

        validateConfiguration();
    }

    public LocalDateTime getEndTime() {
        int pollDays = config.getPollDays();
        LocalDateTime pollEnd = LocalDateTime.now().minusDays(pollDays);

        LocalDateTime stopDate = getStopDate();
        if (stopDate != null) {
            if (stopDate.isAfter(pollEnd)) {
                return stopDate;
            }
        }

        return pollEnd;
    }

    private LocalDateTime getStopDate() {
        String stopDate = config.getStopDate();
        if (stopDate == null || stopDate.isEmpty()) {
            return null;
        }

        return LocalDateTime.parse(stopDate, dtFormat);
    }

    private void validateConfiguration() throws ConfigException {
        try {
            getStopDate();
        } catch (DateTimeParseException e) {
            throw new ConfigException("invalid stop date: \"" + config.getStopDate() + "\"," +
                                              " needs to match " + DT_PATTERN, e);
        }
    }
}
