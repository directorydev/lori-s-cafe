package application;

public class Product {
    private int id; // Added ID for database operations
    private String name;
    private String description;
    private String category;
    private double sellingPrice;
    private double costPrice;
    private int currentStock;
    private int minStock;

    public Product(int id, String name, String description, String category, double sellingPrice, double costPrice, int currentStock, int minStock) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.sellingPrice = sellingPrice;
        this.costPrice = costPrice;
        this.currentStock = currentStock;
        this.minStock = minStock;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public double getSellingPrice() { return sellingPrice; }
    public double getCostPrice() { return costPrice; }
    public int getCurrentStock() { return currentStock; }
    public int getMinStock() { return minStock; }
    
    public double getStockPercentage() {
        if (minStock == 0) return 1.0;
        return Math.min(1.0, (double) currentStock / (minStock * 2.0));
    }
}