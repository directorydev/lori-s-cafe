package application;

import javafx.beans.property.*;

public class InventoryLog {
    private final SimpleIntegerProperty logId;
    private final SimpleStringProperty date;
    private final SimpleStringProperty materialName;
    private final SimpleDoubleProperty changeAmount;
    private final SimpleStringProperty type;
    
    // NEW: Tracks the staff member or admin who made the change
    private final SimpleStringProperty changedBy; 

    public InventoryLog(int logId, String date, String materialName, double changeAmount, String type, String changedBy) {
        this.logId = new SimpleIntegerProperty(logId);
        this.date = new SimpleStringProperty(date);
        this.materialName = new SimpleStringProperty(materialName);
        this.changeAmount = new SimpleDoubleProperty(changeAmount);
        this.type = new SimpleStringProperty(type);
        this.changedBy = new SimpleStringProperty(changedBy);
    }

    // Standard Getters
    public int getLogId() { return logId.get(); }
    public String getDate() { return date.get(); }
    public String getMaterialName() { return materialName.get(); }
    public double getChangeAmount() { return changeAmount.get(); }
    public String getType() { return type.get(); }
    public String getChangedBy() { return changedBy.get(); }
    
    // Property Getters (Required by JavaFX TableView for dynamic data binding)
    public SimpleIntegerProperty logIdProperty() { return logId; }
    public SimpleStringProperty dateProperty() { return date; }
    public SimpleStringProperty materialNameProperty() { return materialName; }
    public SimpleDoubleProperty changeAmountProperty() { return changeAmount; }
    public SimpleStringProperty typeProperty() { return type; }
    public SimpleStringProperty changedByProperty() { return changedBy; }
}