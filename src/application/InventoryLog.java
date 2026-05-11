package application;

import javafx.beans.property.*;

public class InventoryLog {
    private final SimpleIntegerProperty logId;
    private final SimpleStringProperty date;
    private final SimpleStringProperty materialName;
    private final SimpleDoubleProperty changeAmount;
    private final SimpleStringProperty type;

    public InventoryLog(int logId, String date, String materialName, double changeAmount, String type) {
        this.logId = new SimpleIntegerProperty(logId);
        this.date = new SimpleStringProperty(date);
        this.materialName = new SimpleStringProperty(materialName);
        this.changeAmount = new SimpleDoubleProperty(changeAmount);
        this.type = new SimpleStringProperty(type);
    }

    public int getLogId() { return logId.get(); }
    public String getDate() { return date.get(); }
    public String getMaterialName() { return materialName.get(); }
    public double getChangeAmount() { return changeAmount.get(); }
    public String getType() { return type.get(); }
}