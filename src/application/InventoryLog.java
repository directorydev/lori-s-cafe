package application;

import javafx.beans.property.*;

public class InventoryLog {
    private final SimpleIntegerProperty logId;
    private final SimpleStringProperty date;
    private final SimpleStringProperty materialName;
    private final SimpleDoubleProperty changeAmount;
    private final SimpleStringProperty unit; // Added
    private final SimpleStringProperty type;
    private final SimpleStringProperty user; // Added
    private final SimpleStringProperty referenceId; // Added
    private final SimpleStringProperty remarks; // Added

    public InventoryLog(int logId, String date, String materialName, double changeAmount, String unit, String type, String user, String referenceId, String remarks) {
        this.logId = new SimpleIntegerProperty(logId);
        this.date = new SimpleStringProperty(date);
        this.materialName = new SimpleStringProperty(materialName);
        this.changeAmount = new SimpleDoubleProperty(changeAmount);
        this.unit = new SimpleStringProperty(unit != null ? unit : "");
        this.type = new SimpleStringProperty(type);
        this.user = new SimpleStringProperty(user != null ? user : "System");
        this.referenceId = new SimpleStringProperty(referenceId != null && !referenceId.equals("0") ? "#" + referenceId : "N/A");
        this.remarks = new SimpleStringProperty(remarks != null ? remarks : "");
    }

    public int getLogId() { return logId.get(); }
    public String getDate() { return date.get(); }
    public String getMaterialName() { return materialName.get(); }
    public double getChangeAmount() { return changeAmount.get(); }
    public String getUnit() { return unit.get(); }
    public String getType() { return type.get(); }
    public String getUser() { return user.get(); }
    public String getReferenceId() { return referenceId.get(); }
    public String getRemarks() { return remarks.get(); }
}