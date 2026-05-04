package application;

public class RawMaterial {
    private int id;
    private String name, description, unit;
    private double currentStock, minStock;

    public RawMaterial(int id, String name, String description, String unit, double currentStock, double minStock) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.unit = unit;
        this.currentStock = currentStock;
        this.minStock = minStock;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getUnit() { return unit; }
    public double getCurrentStock() { return currentStock; }
    public double getMinStock() { return minStock; }

    public double getStockPercentage() {
        if (minStock == 0) return 1.0;
        return Math.min(1.0, currentStock / (minStock * 2.0));
    }
}