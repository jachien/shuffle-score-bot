package org.jchien.shuffle.formatter;

import org.jchien.shuffle.model.BotComment;
import org.jchien.shuffle.model.Stage;
import org.jchien.shuffle.model.StageType;
import org.jchien.shuffle.model.TablePartId;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;

/**
 * @author jchien
 */
public class SummaryFormatterTest {
    @Test
    public void testParseEscalationBattleStage() {
        assertEquals(25, SummaryFormatter.parseEscalationBattleStage("25"));
        assertEquals(Integer.MAX_VALUE, SummaryFormatter.parseEscalationBattleStage("25x200b"));
    }

    @Test
    public void testGetEscalationBattleParts_SinglePartPerStage() {
        // this test is mainly checking that we don't crash if a non-integer stageId shows up

        SummaryFormatter f = new SummaryFormatter();

        BotComment comment = new BotComment("1", "contents don't matter");
        Map<TablePartId, BotComment> map = new HashMap<>();
        map.put(new TablePartId(new Stage(StageType.ESCALATION_BATTLE, "125"), 1), comment);
        map.put(new TablePartId(new Stage(StageType.ESCALATION_BATTLE, "25"), 1), comment);
        map.put(new TablePartId(new Stage(StageType.ESCALATION_BATTLE, "25x200b"), 1), comment);

        Map<Stage, List<TablePartId>> ebParts = f.getEscalationBattleParts(map);

        assertEquals(3, ebParts.size());

        String[] expectedStageOrder = new String[] { "25", "125", "25x200b" };
        int idx = 0;
        for (Map.Entry<Stage, List<TablePartId>> entry : ebParts.entrySet()) {
            assertEquals(expectedStageOrder[idx++], entry.getKey().getStageId());
        }
    }
}
