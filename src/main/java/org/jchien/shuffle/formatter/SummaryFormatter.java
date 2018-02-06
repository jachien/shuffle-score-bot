package org.jchien.shuffle.formatter;

import org.jchien.shuffle.model.BotComment;
import org.jchien.shuffle.model.Stage;
import org.jchien.shuffle.model.StageType;
import org.jchien.shuffle.model.TablePartId;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static org.jchien.shuffle.formatter.FormatterUtils.appendCapitalizedWords;

/**
 * @author jchien
 */
public class SummaryFormatter {

    public final static String SUMMARY_HEADER = "###Run Round-up\n\n";

    private static final Comparator<TablePartId> PART_NUM_COMPARATOR = comparingInt(TablePartId::getPart);

    // todo move this into separate class
    public String formatSummary(String submissionUrl, Map<TablePartId, BotComment> tableMap) {
        List<TablePartId> compParts = getCompetitionParts(tableMap);
        Map<Stage, List<TablePartId>> ebParts = getEscalationBattleParts(tableMap);
        Map<Stage, List<TablePartId>> normalParts = getNormalStageParts(tableMap);

        StringBuilder sb = new StringBuilder(SUMMARY_HEADER);
        appendCompetitionLinks(sb, compParts, tableMap, submissionUrl);
        appendEscalationBattleLinks(sb, ebParts, tableMap, submissionUrl);
        appendNormalStageLinks(sb, normalParts, tableMap, submissionUrl);

        return sb.toString();
    }

    private List<TablePartId> getCompetitionParts(Map<TablePartId, BotComment> tableMap) {
        List<TablePartId> compParts = tableMap.keySet()
                .stream()
                .filter(id -> StageType.COMPETITION == id.getStage().getStageType())
                .collect(Collectors.toList());
        compParts.sort(PART_NUM_COMPARATOR);
        return compParts;
    }

    private Map<Stage, List<TablePartId>> getEscalationBattleParts(Map<TablePartId, BotComment> tableMap) {
        Comparator<Stage> ebComparator =
                comparingInt(stage -> Integer.parseInt(stage.getStageId()));

        Map<Stage, List<TablePartId>> ebParts = tableMap.keySet()
                .stream()
                .filter(id -> StageType.ESCALATION_BATTLE == id.getStage().getStageType())
                .collect(Collectors.groupingBy(TablePartId::getStage,
                                               () -> new TreeMap<>(ebComparator),
                                               Collectors.toList()));
        ebParts.values()
                .stream()
                .forEach(list -> list.sort(PART_NUM_COMPARATOR));

        return ebParts;
    }

    private Map<Stage, List<TablePartId>> getNormalStageParts(Map<TablePartId, BotComment> tableMap) {
        Comparator<Stage> normalComparator = comparing(Stage::getStageId);

        Map<Stage, List<TablePartId>> normalParts = tableMap.keySet()
                .stream()
                .filter(id -> StageType.NORMAL == id.getStage().getStageType())
                .collect(Collectors.groupingBy(TablePartId::getStage,
                                               () -> new TreeMap<>(normalComparator),
                                               Collectors.toList()));
        normalParts.values()
                .stream()
                .forEach(list -> list.sort(PART_NUM_COMPARATOR));

        return normalParts;
    }

    private void appendCompetitionLinks(StringBuilder sb,
                                        List<TablePartId> compParts,
                                        Map<TablePartId, BotComment> tableMap,
                                        String submissionUrl) {
        if (compParts.size() == 1) {
            String tableUrl = getTableUrl(submissionUrl, tableMap, compParts.get(0));
            sb.append("* **[Competition](").append(tableUrl).append(")**\n");
        } else if (!compParts.isEmpty()) {
            sb.append("* **Competition**\n");
            for (TablePartId part : compParts) {
                String tableUrl = getTableUrl(submissionUrl, tableMap, part);
                int displayPart = part.getPart() + 1;
                sb.append(" * [Competition")
                        .append(", part ")
                        .append(displayPart)
                        .append("](")
                        .append(tableUrl)
                        .append(")\n");
            }
        }
    }

    private void appendEscalationBattleLinks(StringBuilder sb,
                                             Map<Stage, List<TablePartId>> ebParts,
                                             Map<TablePartId, BotComment> tableMap,
                                             String submissionUrl) {
        if (ebParts.isEmpty()) {
            return;
        }
        sb.append("* **Escalation Battles**\n");
        ebParts.forEach((stage, parts) -> {
            if (parts.size() == 1) {
                TablePartId part = parts.get(0);
                String tableUrl = getTableUrl(submissionUrl, tableMap, part);
                sb.append(" * [Stage ")
                        .append(stage.getStageId())
                        .append("](")
                        .append(tableUrl)
                        .append(")\n");
            } else {
                sb.append(" * Stage ").append(stage.getStageId()).append("\n");
                for (TablePartId part : parts) {
                    int displayNum = part.getPart() + 1;
                    String tableUrl = getTableUrl(submissionUrl, tableMap, part);
                    sb.append("      * [Part ")
                            .append(displayNum)
                            .append("](")
                            .append(tableUrl)
                            .append(")\n");
                }
            }
        });
    }

    private void appendNormalStageLinks(StringBuilder sb,
                                        Map<Stage, List<TablePartId>> normalParts,
                                        Map<TablePartId, BotComment> tableMap,
                                        String submissionUrl) {
        if (normalParts.isEmpty()) {
            return;
        }

        sb.append("* **Main / Special / Expert Stages**\n");
        normalParts.forEach((stage, parts) -> {
            if (parts.size() == 1) {
                TablePartId part = parts.get(0);
                String tableUrl = getTableUrl(submissionUrl, tableMap, part);
                sb.append(" * [");
                appendCapitalizedWords(sb, stage.getStageId());
                sb.append("](")
                        .append(tableUrl)
                        .append(")\n");
            } else {
                sb.append(" * ");
                appendCapitalizedWords(sb, stage.getStageId());
                sb.append("\n");
                for (TablePartId part : parts) {
                    int displayNum = part.getPart() + 1;
                    String tableUrl = getTableUrl(submissionUrl, tableMap, part);
                    sb.append("      * [Part ")
                            .append(displayNum)
                            .append("](")
                            .append(tableUrl)
                            .append(")\n");
                }
            }
        });
    }

    private String getTableUrl(String submissionUrl,
                               Map<TablePartId, BotComment> aggregateTableMap,
                               TablePartId partId) {
        BotComment comment = aggregateTableMap.get(partId);
        return FormatterUtils.getCommentPermalink(submissionUrl, comment.getCommentId());
    }
}
