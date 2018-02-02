package org.jchien.shuffle.model;

import java.util.Objects;

/**
 * @author jchien
 */
public class TablePartId {
    private final Stage stage;
    private final int part;

    public TablePartId(Stage stage, int part) {
        this.stage = stage;
        this.part = part;
    }

    public Stage getStage() {
        return stage;
    }

    public int getPart() {
        return part;
    }

    @Override
    public String toString() {
        return "TablePartId{" +
                "stage=" + stage +
                ", part=" + part +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TablePartId tablePartId = (TablePartId) o;
        return part == tablePartId.part &&
                Objects.equals(stage, tablePartId.stage);
    }

    @Override
    public int hashCode() {

        return Objects.hash(stage, part);
    }
}
