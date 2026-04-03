package application;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

public class MenuItem {
    private final SimpleStringProperty name;
    private final SimpleStringProperty category;
    private final SimpleDoubleProperty price;
    private final String imagePath;

    public MenuItem(String name, String category, double price, String imagePath) {
        this.name = new SimpleStringProperty(name);
        this.category = new SimpleStringProperty(category);
        this.price = new SimpleDoubleProperty(price);
        this.imagePath = imagePath;
    }

    // Getters (Required for TableView to find the data)
    public String getName() { return name.get(); }
    public String getCategory() { return category.get(); }
    public double getPrice() { return price.get(); }
    
    // Properties (Optional, but good for advanced binding)
    public SimpleStringProperty nameProperty() { return name; }
    public SimpleStringProperty categoryProperty() { return category; }
    public SimpleDoubleProperty priceProperty() { return price; }
}