package org.jchien.shuffle.parser;

import com.google.common.annotations.VisibleForTesting;
import org.jchien.shuffle.parser.exception.FormatException;
import org.jchien.shuffle.model.Item;
import org.jchien.shuffle.parser.exception.ItemException;
import org.jchien.shuffle.model.Pokemon;
import org.jchien.shuffle.model.RunDetails;
import org.jchien.shuffle.model.RunDetailsBuilder;
import org.jchien.shuffle.parser.exception.SkillException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Further parses and validates output coming from RunParser, but I didn't want to call it a parser to avoid confusion.
 *
 * @author jchien
 */
public class Canonicalizer {
    private interface DetailConsumer {
        void consume() throws FormatException;
    }

    public RunDetails canonicalize(RawRunDetails raw, Map<String, Pokemon> roster, Throwable preexistingThrowable) {
        List<Throwable> throwables = new ArrayList<>();
        if (preexistingThrowable != null) {
            throwables.add(preexistingThrowable);
        }

        final RunDetailsBuilder rdb = new RunDetailsBuilder();

        addDetail(throwables, () -> rdb.setTeam(getTeam(raw.getTeam(), roster)));

        addDetail(throwables, () -> rdb.setItems(getItems(raw.getItems())));

        addDetail(throwables, () -> rdb.setStage(getStage(raw.getStage())));

        addDetail(throwables, () -> rdb.setScore(getScore(raw.getScore())));

        addDetail(throwables, () -> rdb.setMovesLeft(getMovesLeft(raw.getMovesLeft())));

        addDetail(throwables, () -> rdb.setTimeLeft(getTimeLeft(raw.getTimeLeft())));

        addDetail(throwables, () -> rdb.setStageType(raw.getStageType()));

        addDetail(throwables, () -> rdb.setMoveType(raw.getMoveType()));

        rdb.setThrowables(throwables);

        return rdb.build();
    }

    private void addDetail(List<Throwable> throwables,
                           DetailConsumer consumer) {
        try {
            consumer.consume();
        } catch(Throwable t) {
            throwables.add(t);
        }
    }

    private List<Pokemon> getTeam(List<RawPokemon> raw, Map<String, Pokemon> roster) throws FormatException {
        List<Pokemon> team = new ArrayList<>();
        for (RawPokemon rawPokemon : raw) {
            String rosterKey = rawPokemon.getName().toLowerCase();
            if (roster != null && roster.containsKey(rosterKey)) {
                team.add(roster.get(rosterKey));
            } else {
                team.add(getPokemon(rawPokemon));
            }
        }
        return team;
    }

    private Pokemon getPokemon(RawPokemon raw) throws FormatException {
        return new Pokemon(
                getName(raw.getName()),
                getLevel(raw.getLevel()),
                getSkillLevel(raw.getSkill()),
                getSkillName(raw.getSkill()),
                getMsuCount(raw.getMsus()),
                getMaxMsus(raw.getMsus()),
                raw.isPerfect());
    }

    private String getName(String raw) {
        // todo reference shuffledex data
        return raw;
    }

    private static final Pattern LEVEL_PATTERN = Pattern.compile("(?:lvl?\\s*)?(\\d+)", Pattern.CASE_INSENSITIVE);

    @VisibleForTesting
    Integer getLevel(String raw) throws FormatException {
        if (raw == null) {
            // no level specified, that's okay
            return null;
        }

        Matcher m = LEVEL_PATTERN.matcher(raw);

        // whole string must match
        if (!m.matches()) {
            throw new FormatException("Unable to parse level: \"" + raw + "\".");
        }

        // todo validate level, this is harder because it's on a per pokemon basis
        return Integer.parseInt(m.group(1), 10);
    }

    private static final Pattern SKILL_LEVEL_PATTERN = Pattern.compile("\\s*\\bsl\\s*(\\d+)\\b\\s*", Pattern.CASE_INSENSITIVE);

    @VisibleForTesting
    Integer getSkillLevel(String raw) throws FormatException {
        if (raw == null) {
            return null;
        }

        Matcher m = SKILL_LEVEL_PATTERN.matcher(raw);

        if (!m.find()) {
            // it might just be the skill name, so we'll just treat it as unspecified
            return null;
        }

        String levelStr = m.group(1);
        int start = m.start();
        int end = m.end();

        if (start != 0 && end != raw.length()) {
            String matchedText = m.group(0).trim();
            throw new SkillException(
                    String.format("Skill level \"%s\" must be at the start or end of the skill section \"%s\".",
                            matchedText,
                            raw));
        }

        if (m.find()) {
            throw new SkillException("Multiple skill levels defined: \"" + raw + "\".");
        }

        int skillLevel = Integer.parseInt(levelStr, 10);
        if (skillLevel < 1 || skillLevel > 5) {
            throw new SkillException("Invalid skill level " + skillLevel + ": \n" + raw + "\".");
        }
        return skillLevel;
    }

    private static final Pattern SKILL_NAME_PATTERN = Pattern.compile("(?:\\bsl\\s*\\d+\\b)?(.*?)(?:\\bsl\\s*\\d+\\b)?",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @VisibleForTesting
    String getSkillName(String raw) {
        if (raw == null) {
            return null;
        }

        Matcher m = SKILL_NAME_PATTERN.matcher(raw);
        if (!m.matches()) {
            // this is literally impossible with our regex, right?
            return null;
        }

        StringBuilder sb = new StringBuilder();

        for (int i=1; i <= m.groupCount(); i++) {
            if (m.group(i).length() == 0) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(m.group(i));
        }

        // todo validate skill name
        String ret = sb.toString()
                .replaceAll("\\s+", " ")
                .trim();

        if (ret.length() == 0) {
            return null;
        }

        return ret;
    }

    private static final Pattern MSU_PATTERN = Pattern.compile("\\s*(\\d+)\\s*/\\s*(\\d+)\\s*");

    @VisibleForTesting
    Integer getMsuCount(String raw) throws FormatException {
        if (raw == null) {
            return null;
        }

        Matcher m = MSU_PATTERN.matcher(raw);

        if (!m.matches()) {
            throw new FormatException("Unable to parse msu count: \n" + raw +"\n");
        }

        // todo validate
        return Integer.parseInt(m.group(1));
    }

    @VisibleForTesting
    Integer getMaxMsus(String raw) throws FormatException {
        if (raw == null) {
            return null;
        }

        Matcher m = MSU_PATTERN.matcher(raw);

        if (!m.matches()) {
            throw new FormatException("Unable to parse max msus: \n" + raw +"\n");
        }

        // todo validate
        return Integer.parseInt(m.group(2));
    }

    @VisibleForTesting
    List<Item> getItems(List<String> raw) throws ItemException {
        if (raw == null) {
            return null;
        }

        if (raw.size() == 1) {
            String rawItem = raw.get(0);

            if ("none".equalsIgnoreCase(rawItem)
                    || "itemless".equalsIgnoreCase(rawItem)
                    || "no items".equalsIgnoreCase(rawItem)) {
                return new ArrayList<>(EnumSet.noneOf(Item.class));
            }
        }

        List<Item> ret = new ArrayList<>();
        for (String rawItem : raw) {
            ret.add(Item.get(rawItem));
        }

        Collections.sort(ret, Comparator.comparing(Item::ordinal));

        return ret;
    }

    private Integer getScore(String raw) throws FormatException {
        // the parser stripped commas and decimals so we don't have to worry about differing thousands separators
        return getNonNegativeInteger(raw, "score");
    }

    private String getStage(String raw) {
        // todo validate that this is an EB stage or pokemon name
        return raw;
    }

    private Integer getMovesLeft(String raw) throws FormatException {
        // todo should this verify there is exactly one number and ignore everything else?
        // then we can handle strings like "3 moves" or "3 moves left"
        return getNonNegativeInteger(raw, "moves left");
    }


    private Integer getTimeLeft(String raw) throws FormatException {
        // todo should this verify there is exactly one number and ignore everything else?
        // then we can handle strings like "3 moves" or "3 moves left"
        return getNonNegativeInteger(raw, "time left");
    }

    @VisibleForTesting
    Integer getNonNegativeInteger(String raw, String section) throws FormatException {
        if (raw == null) {
            return null;
        }

        try {
            int value = Integer.parseInt(raw, 10);
            if (value < 0) {
                throw new FormatException("Negative " + section + " disallowed: \"" + value + "\"");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new FormatException("Unable to parse " + section + ": \"" + raw + "\"");
        }
    }
}
