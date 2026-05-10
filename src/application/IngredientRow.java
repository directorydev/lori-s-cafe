package application;

import javafx.beans.property.*;

public class IngredientRow {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty unit = new SimpleStringProperty();
    private final DoubleProperty availableStock = new SimpleDoubleProperty();
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final DoubleProperty usage = new SimpleDoubleProperty(0.0);

    public IngredientRow(int id, String name, String unit, double stock) {
        this.id.set(id);
        this.name.set(name);
        this.unit.set(unit);
        this.availableStock.set(stock);
    }

    // Property getters for TableView binding
    public IntegerProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public StringProperty unitProperty() { return unit; }
    public DoubleProperty availableStockProperty() { return availableStock; }
    public BooleanProperty selectedProperty() { return selected; }
    public DoubleProperty usageProperty() { return usage; }

    // Standard Getters and Setters for the Controller logic
    public int getId() { return id.get(); }
    public boolean isSelected() { return selected.get(); }
    public void setSelected(boolean val) { this.selected.set(val); }
    public double getUsage() { return usage.get(); }
    public void setUsage(double val) { this.usage.set(val); }
}