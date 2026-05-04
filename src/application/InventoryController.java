package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventoryController {

    // Database connection details[cite: 2]
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/loris_cafe_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "user123"; 

    // Main UI components from Inventory.fxml[cite: 7]
    @FXML private FlowPane productGrid;
    @FXML private ToggleGroup inventoryTabs;
    @FXML private ToggleButton productTab, rawMaterialTab;
    @FXML private Button addItemBtn;
    
    // Summary Card Labels[cite: 7]
    @FXML private Label totalProductsLabel, totalValueLabel, lowStockLabel, avgStockLabel;
    
    // Filtering and Search[cite: 2, 7]
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo;

    // FXML fields for AddProductModal and AddRawMaterialModal[cite: 5]
    @FXML private TextField nameInput, priceInput, costInput, stockInput, minStockInput;
    @FXML private TextField rawNameInput, rawStockInput;
    @FXML private ComboBox<String> unitCombo;

    private boolean isProductMode = true;

    /**
     * Initializes the controller, sets up tab listeners, and loads initial data.[cite: 2, 7]
     */
    @FXML
    public void initialize() {
        // Handle Tab Switching between Products and Raw Materials views[cite: 7]
        if (inventoryTabs != null) {
            inventoryTabs.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == productTab) {
                    isProductMode = true;
                    addItemBtn.setText("+ Add Product");
                    refreshInventory();
                } else if (newVal == rawMaterialTab) {
                    isProductMode = false;
                    addItemBtn.setText("+ Add Raw Material");
                    refreshRawMaterials();
                } else if (newVal == null) {
                    productTab.setSelected(true); // Prevent having no tab selected
                }
            });
        }

        // Initialize search listener if searchField exists[cite: 2]
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (isProductMode) refreshInventory(); else refreshRawMaterials();
            });
        }

        // Default view on load[cite: 2]
        if (productGrid != null) {
            refreshInventory();
        }
        
        // Populate unit combo box for raw materials if it exists in the current view[cite: 4]
        if (unitCombo != null) {
            unitCombo.getItems().addAll("grams", "ml", "pieces", "kg", "liters");
        }
    }

    // --- DATABASE RETRIEVAL LOGIC ---

    /**
     * Fetches finished products from the 'products' table.[cite: 2, 3]
     */
    public void refreshInventory() {
        productGrid.getChildren().clear();
        String query = "SELECT * FROM products WHERE name ILIKE ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, "%" + (searchField != null ? searchField.getText() : "") + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Product p = new Product(
                    rs.getInt("id"), rs.getString("name"), rs.getString("description"),
                    rs.getString("category"), rs.getDouble("selling_price"),
                    rs.getDouble("cost_price"), rs.getInt("current_stock"), rs.getInt("min_stock")
                );
                productGrid.getChildren().add(createProductCard(p));
            }
            updateSummaryCards(); // Update dashboard totals[cite: 7]
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /**
     * Fetches ingredients from the 'raw_materials' table.[cite: 4, 7]
     */
    public void refreshRawMaterials() {
        productGrid.getChildren().clear();
        String query = "SELECT * FROM raw_materials WHERE name ILIKE ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, "%" + (searchField != null ? searchField.getText() : "") + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                RawMaterial rm = new RawMaterial(
                    rs.getInt("id"), rs.getString("name"), rs.getString("description"),
                    rs.getString("unit"), rs.getDouble("current_stock"), rs.getDouble("min_stock")
                );
                productGrid.getChildren().add(createRawMaterialCard(rm));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- UI CARD CREATION ---

    private VBox createProductCard(Product p) {
        VBox card = new VBox(10);
        card.getStyleClass().add("product-card"); // Styled in application.css[cite: 6]
        
        Label name = new Label(p.getName());
        name.getStyleClass().add("product-title");
        
        Label price = new Label("₱" + String.format("%.2f", p.getSellingPrice()));
        price.getStyleClass().add("price-value-sell");

        ProgressBar stockBar = new ProgressBar(p.getStockPercentage());
        stockBar.getStyleClass().add("stock-bar");
        stockBar.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().addAll(name, price, stockBar);
        return card;
    }

    private VBox createRawMaterialCard(RawMaterial rm) {
        VBox card = new VBox(10);
        card.getStyleClass().add("product-card"); 
        
        Label name = new Label(rm.getName());
        name.getStyleClass().add("product-title");
        
        Label stock = new Label("Stock: " + rm.getCurrentStock() + " " + rm.getUnit());
        stock.getStyleClass().add(rm.getCurrentStock() <= rm.getMinStock() ? "tag-lowstock" : "tag-instock");

        ProgressBar stockBar = new ProgressBar(rm.getStockPercentage());
        stockBar.getStyleClass().add("stock-bar");
        stockBar.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().addAll(name, stock, stockBar);
        return card;
    }

    // --- MODAL AND SAVE LOGIC ---

    /**
     * Opens the appropriate modal based on the current active tab.[cite: 7]
     */
    @FXML
    public void handleAddItem() {
        String fxml = isProductMode ? "AddProductModal.fxml" : "AddRawMaterialModal.fxml";
        String title = isProductMode ? "Add New Product" : "Add New Raw Material";
        openModal(fxml, title);
    }

    private void openModal(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            // Refresh the grid after the modal closes to show new data[cite: 2]
            if (isProductMode) refreshInventory(); else refreshRawMaterials();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void saveNewProduct() {
        String sql = "INSERT INTO products (name, selling_price, cost_price, current_stock, min_stock) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nameInput.getText());
            pstmt.setDouble(2, Double.parseDouble(priceInput.getText()));
            pstmt.setDouble(3, Double.parseDouble(costInput.getText()));
            pstmt.setInt(4, Integer.parseInt(stockInput.getText()));
            pstmt.setInt(5, Integer.parseInt(minStockInput.getText()));
            
            pstmt.executeUpdate();
            handleCloseModal();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void saveRawMaterial() {
        String sql = "INSERT INTO raw_materials (name, unit, current_stock) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, rawNameInput.getText());
            pstmt.setString(2, unitCombo.getValue());
            pstmt.setDouble(3, Double.parseDouble(rawStockInput.getText()));
            
            pstmt.executeUpdate();
            handleCloseModal();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML 
    public void handleCloseModal() { 
        Stage stage = (Stage) (nameInput != null ? nameInput.getScene().getWindow() : rawNameInput.getScene().getWindow());
        stage.close(); 
    }

    private void updateSummaryCards() {
        // Logic to update totalProductsLabel, totalValueLabel, etc. based on products list[cite: 2]
    }
}