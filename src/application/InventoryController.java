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

public class InventoryController {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/loris_cafe_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "user123"; 

    @FXML private FlowPane productGrid;
    @FXML private ToggleGroup inventoryTabs;
    @FXML private ToggleButton productTab, rawMaterialTab;
    @FXML private Button addItemBtn;
    
    // Summary Labels from Inventory.fxml 
    @FXML private Label totalProductsLabel;
    @FXML private Label totalValueLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label avgStockLabel;
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo;

    @FXML private TextField nameInput, priceInput, costInput, stockInput, minStockInput;
    @FXML private ComboBox<String> categoryInput; 
    
    @FXML private TextField rawNameInput, rawStockInput;
    @FXML private ComboBox<String> unitCombo;

    private boolean isProductMode = true;

    private final String[] CATEGORIES = {
        "Milktea", "Cream Cheese", "Iced Coffee", 
        "Latte Series", "Hot Drinks", "Refreshers", "Meals"
    };

    @FXML
    public void initialize() {
        setupTabListeners();
        setupCategoryFiltering(); 
        setupSearchListener();
        
        if (categoryInput != null) {
            categoryInput.getItems().addAll(CATEGORIES);
        }

        if (productGrid != null) refreshInventory();
        
        if (unitCombo != null) {
            unitCombo.getItems().addAll("grams", "ml", "pieces", "kg", "liters"); 
        }
    }

    /**
     * Calculates and updates the summary cards based on current database state.
     * Uses cost_price * current_stock for Total Value calculation.
     */
    private void updateSummaryCards() {
        // Query to get all totals in one go for efficiency
        String sql = "SELECT " +
                     "COUNT(*) as total_items, " +
                     "SUM(cost_price * current_stock) as total_value, " +
                     "COUNT(*) FILTER (WHERE current_stock <= min_stock) as low_stock_count, " +
                     "AVG(current_stock) as avg_stock " +
                     "FROM products";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                int totalItems = rs.getInt("total_items");
                double totalValue = rs.getDouble("total_value");
                int lowStock = rs.getInt("low_stock_count");
                double avgStock = rs.getDouble("avg_stock");

                // Update UI Labels 
                if (totalProductsLabel != null) totalProductsLabel.setText(String.valueOf(totalItems));
                if (totalValueLabel != null) totalValueLabel.setText("₱" + String.format("%,.2f", totalValue));
                if (lowStockLabel != null) lowStockLabel.setText(String.valueOf(lowStock));
                if (avgStockLabel != null) avgStockLabel.setText(String.format("%.1f", avgStock));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void refreshInventory() {
        if (productGrid == null) return;
        productGrid.getChildren().clear();
        
        String selectedCategory = categoryCombo.getSelectionModel().getSelectedItem();
        String sql = "SELECT * FROM products WHERE name ILIKE ?";
        boolean isFilterActive = selectedCategory != null && !selectedCategory.equals("All Categories");
        
        if (isFilterActive) sql += " AND category = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, "%" + (searchField != null ? searchField.getText() : "") + "%");
            if (isFilterActive) pstmt.setString(2, selectedCategory);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Product p = new Product(
                    rs.getInt("id"), rs.getString("name"), rs.getString("description"),
                    rs.getString("category"), rs.getDouble("selling_price"),
                    rs.getDouble("cost_price"), rs.getInt("current_stock"), rs.getInt("min_stock")
                ); 
                productGrid.getChildren().add(createProductCard(p));
            }
            
            // Trigger accurate summary update
            updateSummaryCards(); 
            
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    public void saveNewProduct() {
        String sql = "INSERT INTO products (name, category, selling_price, cost_price, current_stock, min_stock) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, nameInput.getText());
            pstmt.setString(2, categoryInput.getValue()); 
            pstmt.setDouble(3, Double.parseDouble(priceInput.getText()));
            pstmt.setDouble(4, Double.parseDouble(costInput.getText()));
            pstmt.setInt(5, Integer.parseInt(stockInput.getText()));
            pstmt.setInt(6, Integer.parseInt(minStockInput.getText()));
            
            pstmt.executeUpdate();
            handleCloseModal();
            refreshInventory(); // This calls updateSummaryCards() internally
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    private VBox createProductCard(Product p) {
        // Main Container with modern spacing
        VBox card = new VBox(12);
        card.getStyleClass().add("product-card");
        
        // Header: Title and Action Icons (Edit/Delete)
        HBox headerRow = new HBox();
        headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        VBox titleBox = new VBox(2);
        Label name = new Label(p.getName());
        name.getStyleClass().add("product-title");
        Label desc = new Label(p.getDescription() != null ? p.getDescription() : p.getName().toLowerCase());
        desc.getStyleClass().add("product-desc");
        titleBox.getChildren().addAll(name, desc);
        
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        HBox actionIcons = new HBox(10);
        Button editIcon = new Button("📝"); // Replace with FontIcon if available
        editIcon.getStyleClass().add("icon-button");
        Button deleteIcon = new Button("🗑");
        deleteIcon.getStyleClass().add("icon-button");
        actionIcons.getChildren().addAll(editIcon, deleteIcon);
        
        headerRow.getChildren().addAll(titleBox, spacer, actionIcons);

        // Tags: Category and Stock Status
        HBox tagsRow = new HBox(8);
        Label catTag = new Label(p.getCategory());
        catTag.getStyleClass().add("tag-category");
        
        boolean isLow = p.getCurrentStock() <= p.getMinStock();
        Label statusTag = new Label(isLow ? "Low Stock" : "In Stock");
        statusTag.getStyleClass().add(isLow ? "tag-lowstock" : "tag-instock");
        tagsRow.getChildren().addAll(catTag, statusTag);

        // Pricing Grid (Matching the reference layout)
        GridPane priceGrid = new GridPane();
        priceGrid.setHgap(40); 
        priceGrid.setVgap(5);
        
        Label sellLabel = new Label("Selling Price");
        sellLabel.getStyleClass().add("price-label");
        Label sellValue = new Label("₱" + String.format("%.0f", p.getSellingPrice()));
        sellValue.getStyleClass().add("price-value-sell");
        
        Label costLabel = new Label("Cost Price");
        costLabel.getStyleClass().add("price-label");
        Label costValue = new Label("₱" + String.format("%.0f", p.getCostPrice()));
        costValue.getStyleClass().add("price-value-cost");
        
        priceGrid.add(sellLabel, 0, 0);
        priceGrid.add(sellValue, 0, 1);
        priceGrid.add(costLabel, 1, 0);
        priceGrid.add(costValue, 1, 1);

        // Stock Info & Progress Bar
        HBox stockInfo = new HBox();
        Label stockQty = new Label("📦 " + p.getCurrentStock() + " units");
        stockQty.getStyleClass().add("stock-text");
        Pane stockSpacer = new Pane();
        HBox.setHgrow(stockSpacer, Priority.ALWAYS);
        Label minStock = new Label("Min: " + p.getMinStock());
        minStock.getStyleClass().add("price-label");
        stockInfo.getChildren().addAll(stockQty, stockSpacer, minStock);

        ProgressBar stockBar = new ProgressBar(p.getStockPercentage());
        stockBar.getStyleClass().add("stock-bar");
        stockBar.setMaxWidth(Double.MAX_VALUE);
        
        HBox stockFooter = new HBox();
        Label levelText = new Label("Stock Level");
        levelText.getStyleClass().add("price-label");
        Pane footerSpacer = new Pane();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        Label percentText = new Label((int)(p.getStockPercentage() * 100) + "%");
        percentText.getStyleClass().add("price-label");
        stockFooter.getChildren().addAll(levelText, footerSpacer, percentText);

        // Action Button
        Button adjustBtn = new Button("📝 Adjust Stock");
        adjustBtn.getStyleClass().add("adjust-btn");
        adjustBtn.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().addAll(headerRow, tagsRow, priceGrid, stockInfo, stockBar, stockFooter, adjustBtn);
        return card;
    }

    // Existing methods for tabs, search, and raw materials...
    private void setupTabListeners() {
        if (inventoryTabs != null) {
            inventoryTabs.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == productTab) {
                    isProductMode = true;
                    addItemBtn.setText("+ Add Product");
                    categoryCombo.setDisable(false);
                    refreshInventory();
                } else if (newVal == rawMaterialTab) {
                    isProductMode = false;
                    addItemBtn.setText("+ Add Raw Material");
                    categoryCombo.setDisable(true);
                    refreshRawMaterials();
                }
            });
        }
    }

    private void setupCategoryFiltering() {
        if (categoryCombo != null) {
            categoryCombo.getItems().clear();
            categoryCombo.getItems().add("All Categories");
            categoryCombo.getItems().addAll(CATEGORIES);
            categoryCombo.getSelectionModel().selectFirst();
            categoryCombo.setOnAction(e -> refreshInventory());
        }
    }

    private void setupSearchListener() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (isProductMode) refreshInventory(); else refreshRawMaterials();
            });
        }
    }

    @FXML
    public void handleAddItem() {
        String fxml = isProductMode ? "AddProductModal.fxml" : "AddRawMaterialModal.fxml";
        openModal(fxml, isProductMode ? "Add New Product" : "Add New Raw Material");
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
            if (isProductMode) refreshInventory(); else refreshRawMaterials();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML 
    public void handleCloseModal() { 
        Stage stage = (Stage) (nameInput != null ? nameInput.getScene().getWindow() : rawNameInput.getScene().getWindow());
        stage.close(); 
    }

    public void refreshRawMaterials() {
        if (productGrid == null) return;
        productGrid.getChildren().clear();
        
        String query = "SELECT * FROM raw_materials WHERE name ILIKE ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, "%" + (searchField != null ? searchField.getText() : "") + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                // Ensure you have a RawMaterial class with this constructor
                RawMaterial rm = new RawMaterial(
                    rs.getInt("id"), 
                    rs.getString("name"), 
                    "", 
                    rs.getString("unit"), 
                    rs.getDouble("current_stock"), 
                    rs.getDouble("min_stock")
                );
                productGrid.getChildren().add(createRawMaterialCard(rm));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

 // Add this at the top of your class with other variables
    private static int editingRawMaterialId = -1;

    /**
     * Opens the Raw Material modal and pre-fills it with existing data[cite: 5, 23].
     */
    private void openEditRawMaterialModal(RawMaterial rm) {
        try {
            // Load the FXML for the raw material modal 
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AddRawMaterialModal.fxml"));
            Parent root = loader.load();
            
            // Get the controller and inject the material data
            InventoryController modalController = loader.getController();
            modalController.prepareRawMaterialEditMode(rm);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Edit Raw Material: " + rm.getName());
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            refreshRawMaterials(); // Refresh the grid after closing [cite: 2]
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    /**
     * Pre-fills the FXML fields with the selected material's data.
     */
    public void prepareRawMaterialEditMode(RawMaterial rm) {
        editingRawMaterialId = rm.getId(); // Store the ID for the SQL UPDATE
        
        if (rawNameInput != null) {
            rawNameInput.setText(rm.getName());
            unitCombo.setValue(rm.getUnit());
            rawStockInput.setText(String.valueOf(rm.getCurrentStock()));
            
            // If your modal has a minStock field, set it here [cite: 7]
            if (minStockInput != null) {
                minStockInput.setText(String.valueOf(rm.getMinStock()));
            }
        }
    }

    /**
     * Modified save logic to handle both INSERT and UPDATE for raw materials.
     */
    @FXML
    public void saveRawMaterial() {
        String sql;
        if (editingRawMaterialId == -1) {
            sql = "INSERT INTO raw_materials (name, unit, current_stock, min_stock, status) VALUES (?, ?, ?, ?, ?)";
        } else {
            sql = "UPDATE raw_materials SET name=?, unit=?, current_stock=?, min_stock=?, status=? WHERE id=?";
        }
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            double current = Double.parseDouble(rawStockInput.getText());
            double min = (minStockInput != null && !minStockInput.getText().isEmpty()) 
                          ? Double.parseDouble(minStockInput.getText()) : 0;
            
            // Determine status based on stock levels [cite: 26]
            String status = (current <= min) ? "Low Stock" : "In Stock";
            if (current <= 0) status = "Out of Stock";

            pstmt.setString(1, rawNameInput.getText());
            pstmt.setString(2, unitCombo.getValue());
            pstmt.setDouble(3, current);
            pstmt.setDouble(4, min);
            pstmt.setString(5, status);
            
            if (editingRawMaterialId != -1) {
                pstmt.setInt(6, editingRawMaterialId);
            }
            
            pstmt.executeUpdate();
            handleCloseModal();
            refreshRawMaterials();
            
            // Reset the ID after saving
            editingRawMaterialId = -1;
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    private VBox createRawMaterialCard(RawMaterial rm) {
        VBox card = new VBox(12);
        card.getStyleClass().add("product-card"); // Shared modern card style
        card.setPrefWidth(330);

        // Header: Name and Action Icons
        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label name = new Label(rm.getName());
        name.getStyleClass().add("product-title");
        
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        HBox actions = new HBox(10);
        Button editBtn = new Button("📝");
        editBtn.getStyleClass().add("icon-button");
        editBtn.setOnAction(e -> openEditRawMaterialModal(rm)); 
        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("icon-button");
        actions.getChildren().addAll(editBtn, deleteBtn);
        header.getChildren().addAll(name, spacer, actions);

        // Description/Unit text
        Label desc = new Label(rm.getName().toLowerCase() + " (per " + rm.getUnit() + ")");
        desc.getStyleClass().add("product-desc");

        // Stock Indicator (Icon + Quantity + Min Label)
        HBox stockRow = new HBox();
        stockRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label stockVal = new Label("📦 " + rm.getCurrentStock() + " " + rm.getUnit());
        stockVal.getStyleClass().add("stock-text");
        
        Pane stockSpacer = new Pane();
        HBox.setHgrow(stockSpacer, Priority.ALWAYS);
        Label minLabel = new Label("Min: " + rm.getMinStock());
        minLabel.getStyleClass().add("price-label");
        stockRow.getChildren().addAll(stockVal, stockSpacer, minLabel);

        // Progress Bar
        ProgressBar stockBar = new ProgressBar(rm.getStockPercentage());
        stockBar.getStyleClass().add("stock-bar");
        stockBar.setMaxWidth(Double.MAX_VALUE);

        // Footer labels
        HBox footer = new HBox();
        Label levelTxt = new Label("Stock Level");
        levelTxt.getStyleClass().add("price-label");
        Pane footerSpacer = new Pane();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        Label percentTxt = new Label((int)(rm.getStockPercentage() * 100) + "%");
        percentTxt.getStyleClass().add("price-label");
        footer.getChildren().addAll(levelTxt, footerSpacer, percentTxt);

        card.getChildren().addAll(header, desc, stockRow, stockBar, footer);
        return card;
    }
    
}