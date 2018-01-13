package org.jchien.shuffle.bot;

import com.google.common.annotations.VisibleForTesting;
import org.jchien.shuffle.model.FormatException;
import org.jchien.shuffle.model.Item;
import org.jchien.shuffle.model.MoveType;
import org.jchien.shuffle.model.Pokemon;
import org.jchien.shuffle.model.RunDetails;
import org.jchien.shuffle.model.StageType;
import org.jchien.shuffle.parser.RawPokemon;
import org.jchien.shuffle.parser.RawRunDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Further parses and validates output coming from RunParser, but I didn't want to call it a parser to avoid confusion.
 *
 * @author jchien
 */
public class Canonicalizer {
    public RunDetails canonicalize(RawRunDetails raw) {
        try {
            // this should logically be a set, but I don't want to deal with validating that
            List<Pokemon> team = getTeam(raw.getTeam());

            List<Item> items = getItems(raw.getItems());

            String stage = getStage(raw.getStage());

            Integer score = getScore(raw.getScore());

            Integer movesLeft = getMovesLeft(raw.getMovesLeft());

            Integer timeLeft = getTimeLeft(raw.getTimeLeft());

            StageType stageType = raw.getStageType();

            MoveType moveType = raw.getMoveType();

            return new RunDetails(
                    team,
                    items,
                    stage,
                    score,
                    movesLeft,
                    timeLeft,
                    stageType,
                    moveType
            );
        } catch (FormatException e) {
            return new RunDetails(e);
        }
    }

    private List<Pokemon> getTeam(List<RawPokemon> raw) throws FormatException {
        List<Pokemon> team = new ArrayList<>();
        for (RawPokemon rawPokemon : raw) {
            team.add(getPokemon(rawPokemon));
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


    private static final Pattern LEVEL_PATTERN = Pattern.compile("(?:lv\\s*)?(\\d+)", Pattern.CASE_INSENSITIVE);

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

    private static final Pattern SKILL_LEVEL_PATTERN = Pattern.compile("\\bsl\\s*([0-9]+)\\b", Pattern.CASE_INSENSITIVE);

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
        int end = m.end();

        if (m.find(end)) {
            throw new FormatException("Multiple skill levels defined: \"" + raw + "\".");
        }

        int skillLevel = Integer.parseInt(levelStr, 10);
        if (skillLevel < 1 || skillLevel > 5) {
            throw new FormatException("Invalid skill level " + skillLevel + ": \n" + raw + "\"");
        }
        return skillLevel;
    }

    private static final Pattern SKILL_NAME_PATTERN = Pattern.compile("(?<start>.*)(?:\\bsl\\s*[0-9]+\\b)?(?<end>.*)",
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

        if (sb.length() == 0) {
            return null;
        }

        // todo validate skill name
        return sb.toString();
    }

    private static final Pattern MSU_PATTERN = Pattern.compile("([0-9]+)\\s*/\\s*([0-9]+)");
    private Integer getMsuCount(String raw) throws FormatException {
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

    private Integer getMaxMsus(String raw) throws FormatException {
        if (raw == null) {
            return null;
        }

        Matcher m = MSU_PATTERN.matcher(raw);

        if (!m.matches()) {
            throw new FormatException("Unable to parse msu count: \n" + raw +"\n");
        }

        // todo validate
        return Integer.parseInt(m.group(2));
    }

    public List<Item> getItems(List<String> raw) throws FormatException {
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

            if ("all".equalsIgnoreCase(rawItem)
                    || "full".equalsIgnoreCase(rawItem)
                    || "full items".equalsIgnoreCase(rawItem)
                    || "full item run".equalsIgnoreCase(rawItem)) {
                // people don't generally include a jewel when they say full item run
                return new ArrayList<>(EnumSet.complementOf(EnumSet.of(Item.JEWEL)));
            }
        }

        List<Item> ret = new ArrayList<>();
        for (String rawItem : raw) {
            try {
                ret.add(Item.get(rawItem));
            } catch (NoSuchElementException e) {
                // not going to worry about a nicer error message for silliness like "items: none, all"
                throw new FormatException("No such item: \"" + rawItem + "\"");
            }
        }

        Collections.sort(ret, Comparator.comparing(Item::ordinal));

        return ret;
    }

    private Integer getScore(String raw) throws FormatException {
        if (raw == null) {
            return null;
        }

        // the parser stripped commas and decimals so we don't have to worry about differing thousands separators
        try {
            return Integer.parseInt(raw, 10);
        } catch (NumberFormatException e) {
            throw new FormatException("Unable to parse score: \"" + raw + "\"");
        }
    }

    private String getStage(String raw) {
        // todo validate that this is an EB stage or pokemon name
        return raw;
    }

    private Integer getMovesLeft(String raw) throws FormatException {
        if (raw == null) {
            return null;
        }

        try {
            // todo should this verify there is exactly one number and ignore everything else?
            // then we can handle strings like "3 moves" or "3 moves left"
            return Integer.parseInt(raw, 10);
        } catch (NumberFormatException e) {
            throw new FormatException("Unable to parse moves left: \"" + raw + "\"");
        }
    }

    private Integer getTimeLeft(String raw) throws FormatException {
        if (raw == null) {
            return null;
        }

        try {
            // todo should this verify there is exactly one number and ignore everything else?
            // then we can handle strings like "3s" or "3 secs"
            return Integer.parseInt(raw, 10);
        } catch (NumberFormatException e) {
            throw new FormatException("Unable to parse time left: \"" + raw + "\"");
        }
    }
}
