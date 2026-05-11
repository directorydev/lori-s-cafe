package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DoubleStringConverter;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;

public class InventoryController {

    private static final String DB_URL = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0";
    private static final String DB_USER = "postgres.gwjmqejllljupondbzbs";
    private static final String DB_PASS = "Loritastecafe2026"; 

    @FXML private FlowPane productGrid;
    @FXML private ToggleGroup inventoryTabs;
    @FXML private ToggleButton productTab, rawMaterialTab;
    @FXML private Button addItemBtn;
    @FXML private Label totalProductsLabel, totalValueLabel, lowStockLabel, avgStockLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo, stockLevelCombo;

    // Product Modal Fields
    @FXML private TextField nameInput, priceInput, costInput, stockInput, minStockInput;
    @FXML private ComboBox<String> categoryInput; 
    @FXML private Label modalHeaderLabel;
    
    // Ingredient Table for Products
    @FXML private TableView<IngredientRow> ingredientTable;
    @FXML private TableColumn<IngredientRow, Boolean> colSelect;
    @FXML private TableColumn<IngredientRow, String> colRawMaterial, colUnit;
    @FXML private TableColumn<IngredientRow, Double> colStock, colUsage;

    // Raw Material Modal Fields
    @FXML private TextField rawNameInput, rawStockInput;
    @FXML private ComboBox<String> unitCombo;

    private boolean isProductMode = true;
    private static int editingProductId = -1;
    private static int editingRawMaterialId = -1;

    private final String[] CATEGORIES = {
        "Milktea", "Cream Cheese", "Iced Coffee","Special Milktea", "Lemon Blends", 
        "Latte", "Hot Drinks", "Fruity"
    };

    @FXML
    public void initialize() {
        setupTabListeners();
        setupCategoryFiltering(); 
        setupStockLevelFiltering();
        setupSearchListener();
        
        if (categoryInput != null) categoryInput.getItems().addAll(CATEGORIES);
        if (unitCombo != null) unitCombo.getItems().addAll("Grams", "ml", "Pieces", "kg", "Liters"); 
        
        if (ingredientTable != null) setupIngredientTable();
        if (productGrid != null) {
            productGrid.setHgap(15);
            productGrid.setVgap(15);
            if (isProductMode) refreshInventory(); else refreshRawMaterials();
        }
    }

    private void setupTabListeners() {
        if (inventoryTabs != null) {
            inventoryTabs.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == productTab) {
                    isProductMode = true;
                    if (addItemBtn != null) addItemBtn.setText("+ Add Product");
                    if (categoryCombo != null) categoryCombo.setDisable(false);
                    refreshInventory();
                } else if (newVal == rawMaterialTab) {
                    isProductMode = false;
                    if (addItemBtn != null) addItemBtn.setText("+ Add Raw Material");
                    if (categoryCombo != null) categoryCombo.setDisable(true);
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

    private void setupStockLevelFiltering() {
        if (stockLevelCombo != null) {
            stockLevelCombo.getItems().setAll("All Stock Levels", "In Stock", "Low Stock", "Out of Stock");
            stockLevelCombo.getSelectionModel().selectFirst();
            stockLevelCombo.setOnAction(e -> {
                if (isProductMode) refreshInventory(); else refreshRawMaterials();
            });
        }
    }

    private void setupSearchListener() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (isProductMode) refreshInventory(); else refreshRawMaterials();
            });
        }
    }

    private void setupIngredientTable() {
        colSelect.setCellValueFactory(f -> f.getValue().selectedProperty());
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        colRawMaterial.setCellValueFactory(f -> f.getValue().nameProperty());
        colUnit.setCellValueFactory(f -> f.getValue().unitProperty());
        colStock.setCellValueFactory(f -> f.getValue().availableStockProperty().asObject());
        colUsage.setCellValueFactory(f -> f.getValue().usageProperty().asObject());
        colUsage.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        ingredientTable.setEditable(true);
        loadRawMaterialsIntoTable();
    }

    private void loadRawMaterialsIntoTable() {
        if (ingredientTable == null) return;
        ObservableList<IngredientRow> data = FXCollections.observableArrayList();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, unit, current_stock FROM raw_materials ORDER BY name ASC")) {
            while (rs.next()) {
                data.add(new IngredientRow(
                    rs.getInt("id"), rs.getString("name"), rs.getString("unit"), rs.getDouble("current_stock")
                ));
            }
            ingredientTable.setItems(data);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    public void saveNewProduct() {
        String name = nameInput.getText() != null ? nameInput.getText().trim() : "";
        if (name.isEmpty()) {
            showAlert("Validation Error", "Product name cannot be empty.");
            return;
        }
        
        if (isDuplicateName("products", name, editingProductId)) {
            showDuplicateAlert(name);
            return;
        }

        ObservableList<IngredientRow> selectedIngs = ingredientTable.getItems().filtered(IngredientRow::isSelected);
        if (selectedIngs.isEmpty()) {
            showAlert("Validation Error", "Every product MUST have at least one raw material ingredient.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to save this product?", ButtonType.YES, ButtonType.NO);
        confirmAlert.setTitle("Confirm Save");
        confirmAlert.setHeaderText(null);
        
        if (confirmAlert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                conn.setAutoCommit(false); 
                try {
                    int productId = upsertProduct(conn, name);
                    
                    try (PreparedStatement del = conn.prepareStatement("DELETE FROM product_ingredients WHERE product_id = ?")) {
                        del.setInt(1, productId);
                        del.executeUpdate();
                    }
                    
                    String ins = "INSERT INTO product_ingredients (product_id, raw_material_id, quantity_required, usage_condition) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(ins)) {
                        for (IngredientRow row : selectedIngs) {
                            pstmt.setInt(1, productId);
                            pstmt.setInt(2, row.getId());
                            pstmt.setDouble(3, row.getUsage()); 
                            pstmt.setString(4, "Always"); 
                            pstmt.addBatch();
                        }
                        pstmt.executeBatch();
                    }

                    conn.commit(); 
                    handleCloseModal();
                    refreshInventory();
                    editingProductId = -1;

                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Success");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("Product successfully saved to the database!");
                    successAlert.showAndWait();

                } catch (SQLException e) {
                    conn.rollback(); 
                    e.printStackTrace();
                    showAlert("Database Error", "Failed to save product details. Changes rolled back.");
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private int upsertProduct(Connection conn, String name) throws SQLException {
        String sql = (editingProductId == -1) 
            ? "INSERT INTO products (name, category, selling_price, cost_price, current_stock, min_stock) VALUES (?, ?, ?, ?, ?, ?) RETURNING id"
            : "UPDATE products SET name=?, category=?, selling_price=?, cost_price=?, current_stock=?, min_stock=? WHERE id=?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            // Handle null category from dropdown
            pstmt.setString(2, categoryInput.getValue() != null ? categoryInput.getValue() : "Uncategorized");
            
            // Handle empty inputs safely
            pstmt.setDouble(3, parseDoubleSafe(priceInput.getText()));
            pstmt.setDouble(4, parseDoubleSafe(costInput.getText()));
            pstmt.setInt(5, parseIntSafe(stockInput.getText()));
            pstmt.setInt(6, parseIntSafe(minStockInput.getText()));
            
            if (editingProductId != -1) {
                pstmt.setInt(7, editingProductId);
                pstmt.executeUpdate();
                return editingProductId;
            } else {
                ResultSet rs = pstmt.executeQuery();
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }
    
    // Helper methods for safer parsing
    private double parseDoubleSafe(String text) {
        try { return text == null || text.trim().isEmpty() ? 0.0 : Double.parseDouble(text); } 
        catch (NumberFormatException e) { return 0.0; }
    }
    private int parseIntSafe(String text) {
        try { return text == null || text.trim().isEmpty() ? 0 : Integer.parseInt(text); } 
        catch (NumberFormatException e) { return 0; }
    }

    public void refreshInventory() {
        if (productGrid == null) return;
        productGrid.getChildren().clear();
        String selectedCategory = categoryCombo.getSelectionModel().getSelectedItem();
        String selectedStockLevel = (stockLevelCombo != null) ? stockLevelCombo.getSelectionModel().getSelectedItem() : "All Stock Levels";
        
        StringBuilder sql = new StringBuilder("SELECT * FROM products WHERE name ILIKE ?");
        if (selectedCategory != null && !selectedCategory.equals("All Categories")) {
            sql.append(" AND category = '").append(selectedCategory).append("'");
        }
        
        if (selectedStockLevel.equals("In Stock")) sql.append(" AND current_stock > min_stock");
        else if (selectedStockLevel.equals("Low Stock")) sql.append(" AND current_stock <= min_stock AND current_stock > 0");
        else if (selectedStockLevel.equals("Out of Stock")) sql.append(" AND current_stock <= 0");
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            pstmt.setString(1, "%" + (searchField != null ? searchField.getText() : "") + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Product p = new Product(rs.getInt("id"), rs.getString("name"), rs.getString("description"), rs.getString("category"), rs.getDouble("selling_price"), rs.getDouble("cost_price"), rs.getInt("current_stock"), rs.getInt("min_stock")); 
                productGrid.getChildren().add(createProductCard(p));
            }
            updateSummaryCards(); 
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private VBox createProductCard(Product p) {
        VBox card = new VBox(15);
        card.setPrefWidth(320);
        card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-border-color: #e8e8e8; -fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 4);");

        // --- Safe Variable Assignments ---
        String safeName = p.getName() != null ? p.getName() : "Unnamed Product";
        String safeCategory = (p.getCategory() != null && !p.getCategory().trim().isEmpty()) ? p.getCategory() : "Uncategorized";
        String safeDesc = (p.getDescription() != null && !p.getDescription().trim().isEmpty()) ? p.getDescription() : safeName.toLowerCase();

        // --- Top Row: Title, Description, Actions ---
        HBox headerRow = new HBox();
        headerRow.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        VBox titleBox = new VBox(5);
        Label name = new Label(safeName);
        name.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
        Label desc = new Label(safeDesc);
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #737373;");
        titleBox.getChildren().addAll(name, desc);
        
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        HBox actionIcons = new HBox(8);
        Button editBtn = new Button("📝");
        editBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
        editBtn.setOnAction(e -> openEditProductModal(p)); 
        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
        deleteBtn.setOnAction(e -> deleteProduct(p));
        actionIcons.getChildren().addAll(editBtn, deleteBtn);
        headerRow.getChildren().addAll(titleBox, spacer, actionIcons);

        // --- Tags Row ---
        HBox tagsRow = new HBox(8);
        
        Label catTag = new Label(safeCategory.toLowerCase());
        // Dynamic colors, defaulting to brown if unrecognized
        String catColor = safeCategory.equalsIgnoreCase("Milktea") ? "#fce4ec" : "#d7ccc8";
        String catTextCol = safeCategory.equalsIgnoreCase("Milktea") ? "#d81b60" : "#5d4037";
        catTag.setStyle("-fx-background-color: " + catColor + "; -fx-text-fill: " + catTextCol + "; -fx-padding: 4 8 4 8; -fx-background-radius: 5; -fx-font-size: 11px; -fx-font-weight: bold;");
        
        boolean isLow = p.getCurrentStock() <= p.getMinStock();
        Label statusTag = new Label(isLow ? "Low Stock" : "In Stock");
        statusTag.setStyle(isLow ? "-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-padding: 4 8 4 8; -fx-background-radius: 5; -fx-font-size: 11px;" 
                                 : "-fx-background-color: #e8f5e9; -fx-text-fill: #00c853; -fx-padding: 4 8 4 8; -fx-background-radius: 5; -fx-font-size: 11px;");
        tagsRow.getChildren().addAll(catTag, statusTag);

        // --- Pricing Row ---
        HBox pricingRow = new HBox(50);
        VBox sellingBox = new VBox(2);
        Label sellLabel = new Label("Selling Price");
        sellLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #737373;");
        Label sellValue = new Label("₱" + String.format("%.0f", p.getSellingPrice()));
        sellValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #00c853;");
        sellingBox.getChildren().addAll(sellLabel, sellValue);

        VBox costBox = new VBox(2);
        Label costLabel = new Label("Cost Price");
        costLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #737373;");
        Label costValue = new Label("₱" + String.format("%.0f", p.getCostPrice()));
        costValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
        costBox.getChildren().addAll(costLabel, costValue);
        pricingRow.getChildren().addAll(sellingBox, costBox);

        // --- Stock Overview Row ---
        HBox stockRow = new HBox();
        stockRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label unitsLabel = new Label("📦 " + p.getCurrentStock() + " units");
        unitsLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #00c853;");
        Pane stockSpacer = new Pane();
        HBox.setHgrow(stockSpacer, Priority.ALWAYS);
        Label minLabel = new Label("Min: " + p.getMinStock());
        minLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #737373;");
        stockRow.getChildren().addAll(unitsLabel, stockSpacer, minLabel);

        // --- Progress Bar Section ---
        VBox progressSection = new VBox(3);
        ProgressBar stockBar = new ProgressBar(p.getStockPercentage());
        stockBar.setMaxWidth(Double.MAX_VALUE);
        stockBar.setPrefHeight(6);
        stockBar.setStyle("-fx-accent: #00c853; -fx-control-inner-background: #f0f0f0;");
        
        HBox progressLabels = new HBox();
        Label pLabel = new Label("Stock Level");
        pLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #a0a0a0;");
        Pane pSpacer = new Pane();
        HBox.setHgrow(pSpacer, Priority.ALWAYS);
        Label pValue = new Label(String.format("%.0f%%", p.getStockPercentage() * 100));
        pValue.setStyle("-fx-font-size: 10px; -fx-text-fill: #a0a0a0;");
        progressLabels.getChildren().addAll(pLabel, pSpacer, pValue);
        progressSection.getChildren().addAll(stockBar, progressLabels);

        // --- Adjust Stock Button ---
        Button adjustBtn = new Button("📝 Adjust Stock");
        adjustBtn.setMaxWidth(Double.MAX_VALUE);
        adjustBtn.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e8e8e8; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: #333333; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 0;");
        adjustBtn.setOnAction(e -> openEditProductModal(p));

        card.getChildren().addAll(headerRow, tagsRow, pricingRow, stockRow, progressSection, adjustBtn);
        return card;
    }

    public void refreshRawMaterials() {
        if (productGrid == null) return;
        productGrid.getChildren().clear();
        String selectedStockLevel = (stockLevelCombo != null) ? stockLevelCombo.getSelectionModel().getSelectedItem() : "All Stock Levels";
        StringBuilder sql = new StringBuilder("SELECT * FROM raw_materials WHERE name ILIKE ?");
        
        if (selectedStockLevel.equals("Low Stock")) sql.append(" AND current_stock <= min_stock AND current_stock > 0");
        else if (selectedStockLevel.equals("Out of Stock")) sql.append(" AND current_stock <= 0");
        else if (selectedStockLevel.equals("In Stock")) sql.append(" AND current_stock > min_stock");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            pstmt.setString(1, "%" + (searchField != null ? searchField.getText() : "") + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                RawMaterial rm = new RawMaterial(rs.getInt("id"), rs.getString("name"), "", rs.getString("unit"), rs.getDouble("current_stock"), rs.getDouble("min_stock"));
                productGrid.getChildren().add(createRawMaterialCard(rm));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private VBox createRawMaterialCard(RawMaterial rm) {
        VBox card = new VBox(15);
        card.setPrefWidth(320);
        card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-border-color: #e8e8e8; -fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 4);");
        
        String safeName = rm.getName() != null ? rm.getName() : "Unknown Material";
        String safeUnit = rm.getUnit() != null ? rm.getUnit() : "unit";
        boolean isLow = rm.getCurrentStock() <= rm.getMinStock();
        
        // --- Top Row: Title, Description, Actions ---
        HBox headerRow = new HBox();
        headerRow.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        
        VBox titleBox = new VBox(5);
        Label name = new Label(safeName);
        name.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
        Label desc = new Label(safeName.toLowerCase() + " (per " + safeUnit + ")");
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #737373;");
        titleBox.getChildren().addAll(name, desc);
        
        Pane spacer = new Pane(); HBox.setHgrow(spacer, Priority.ALWAYS);
        
        HBox actionIcons = new HBox(8);
        Button editBtn = new Button("📝"); 
        editBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
        editBtn.setOnAction(e -> openEditRawMaterialModal(rm)); 
        Button deleteBtn = new Button("🗑"); 
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
        deleteBtn.setOnAction(e -> deleteRawMaterial(rm));
        actionIcons.getChildren().addAll(editBtn, deleteBtn);
        headerRow.getChildren().addAll(titleBox, spacer, actionIcons);
        
        // --- Stock Overview Row ---
        HBox stockRow = new HBox();
        stockRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // Format stock without trailing zero if whole number
        String stockDisplay = (rm.getCurrentStock() == Math.floor(rm.getCurrentStock())) ? 
            String.format("%.0f", rm.getCurrentStock()) : String.valueOf(rm.getCurrentStock());
            
        Label unitsLabel = new Label("📦 " + stockDisplay + " " + safeUnit);
        String stockColor = isLow ? "#c62828" : "#00c853";
        unitsLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + stockColor + ";");
        
        Pane stockSpacer = new Pane();
        HBox.setHgrow(stockSpacer, Priority.ALWAYS);
        
        String minDisplay = (rm.getMinStock() == Math.floor(rm.getMinStock())) ? 
            String.format("%.0f", rm.getMinStock()) : String.valueOf(rm.getMinStock());
        Label minLabel = new Label("Min: " + minDisplay);
        minLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #737373;");
        stockRow.getChildren().addAll(unitsLabel, stockSpacer, minLabel);

        // --- Progress Bar Section ---
        VBox progressSection = new VBox(3);
        ProgressBar stockBar = new ProgressBar(rm.getStockPercentage());
        stockBar.setMaxWidth(Double.MAX_VALUE);
        stockBar.setPrefHeight(6);
        String barColor = isLow ? "#ef5350" : "#00c853";
        stockBar.setStyle("-fx-accent: " + barColor + "; -fx-control-inner-background: #f0f0f0;");
        
        HBox progressLabels = new HBox();
        Label pLabel = new Label("Stock Level");
        pLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #a0a0a0;");
        Pane pSpacer = new Pane();
        HBox.setHgrow(pSpacer, Priority.ALWAYS);
        Label pValue = new Label(String.format("%.0f%%", rm.getStockPercentage() * 100));
        pValue.setStyle("-fx-font-size: 10px; -fx-text-fill: #a0a0a0;");
        progressLabels.getChildren().addAll(pLabel, pSpacer, pValue);
        
        progressSection.getChildren().addAll(stockBar, progressLabels);
        
        // Add padding to push the stock info down a bit like the image
        VBox.setMargin(stockRow, new javafx.geometry.Insets(10, 0, 0, 0));

        card.getChildren().addAll(headerRow, stockRow, progressSection);
        return card;
    }

    @FXML
    public void saveRawMaterial() {
        String name = rawNameInput.getText() != null ? rawNameInput.getText().trim() : "";
        if (name.isEmpty()) {
            showAlert("Validation Error", "Raw material name cannot be empty.");
            return;
        }
        
        if (isDuplicateName("raw_materials", name, editingRawMaterialId)) {
            showDuplicateAlert(name);
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to save this raw material?", ButtonType.YES, ButtonType.NO);
        confirmAlert.setTitle("Confirm Save");
        confirmAlert.setHeaderText(null);

        if (confirmAlert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            String sql = (editingRawMaterialId == -1)
                ? "INSERT INTO raw_materials (name, unit, current_stock, min_stock, status) VALUES (?, ?, ?, ?, ?)"
                : "UPDATE raw_materials SET name=?, unit=?, current_stock=?, min_stock=?, status=? WHERE id=?";
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                double current = parseDoubleSafe(rawStockInput.getText());
                double min = parseDoubleSafe(minStockInput != null ? minStockInput.getText() : "0");
                String status = (current <= min) ? (current <= 0 ? "Out of Stock" : "Low Stock") : "In Stock";
                
                pstmt.setString(1, name);
                pstmt.setString(2, unitCombo.getValue() != null ? unitCombo.getValue() : "Pieces");
                pstmt.setDouble(3, current);
                pstmt.setDouble(4, min);
                pstmt.setString(5, status);
                
                if (editingRawMaterialId != -1) pstmt.setInt(6, editingRawMaterialId);
                pstmt.executeUpdate();
                
                handleCloseModal();
                refreshRawMaterials();
                editingRawMaterialId = -1;

                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Success");
                successAlert.setHeaderText(null);
                successAlert.setContentText("Raw material successfully saved to the database!");
                successAlert.showAndWait();

            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void updateSummaryCards() {
        String sql = "SELECT COUNT(*) as total_items, SUM(cost_price * current_stock) as total_value, COUNT(*) FILTER (WHERE current_stock <= min_stock) as low_stock_count, AVG(current_stock) as avg_stock FROM products";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                if (totalProductsLabel != null) totalProductsLabel.setText(String.valueOf(rs.getInt("total_items")));
                if (totalValueLabel != null) totalValueLabel.setText("₱" + String.format("%,.2f", rs.getDouble("total_value")));
                if (lowStockLabel != null) lowStockLabel.setText(String.valueOf(rs.getInt("low_stock_count")));
                if (avgStockLabel != null) avgStockLabel.setText(String.format("%.1f", rs.getDouble("avg_stock")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private boolean isDuplicateName(String table, String name, int currentId) {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE name ILIKE ? AND id != ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name.trim());
            pstmt.setInt(2, currentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    @FXML public void handleAddItem() {
        editingProductId = -1; editingRawMaterialId = -1;
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

    @FXML public void handleCloseModal() { 
        Stage stage = null;
        if (nameInput != null && nameInput.getScene() != null) stage = (Stage) nameInput.getScene().getWindow();
        else if (rawNameInput != null && rawNameInput.getScene() != null) stage = (Stage) rawNameInput.getScene().getWindow();
        if (stage != null) stage.close(); 
    }

    private void deleteProduct(Product p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + p.getName() + "	?", ButtonType.OK, ButtonType.CANCEL);
        if (alert.showAndWait().get() == ButtonType.OK) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM products WHERE id = ?")) {
                pstmt.setInt(1, p.getId());
                pstmt.executeUpdate();
                refreshInventory();
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    private void deleteRawMaterial(RawMaterial rm) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + rm.getName() + "?", ButtonType.OK, ButtonType.CANCEL);
        if (alert.showAndWait().get() == ButtonType.OK) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM raw_materials WHERE id = ?")) {
                pstmt.setInt(1, rm.getId());
                pstmt.executeUpdate();
                refreshRawMaterials();
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    private void openEditProductModal(Product p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AddProductModal.fxml"));
            Parent root = loader.load();
            InventoryController modalController = loader.getController();
            modalController.prepareProductEditMode(p);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            refreshInventory();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void prepareProductEditMode(Product p) {
        editingProductId = p.getId();
        if (nameInput != null) {
            nameInput.setText(p.getName());
            categoryInput.setValue(p.getCategory() != null ? p.getCategory() : "");
            priceInput.setText(String.valueOf(p.getSellingPrice()));
            costInput.setText(String.valueOf(p.getCostPrice()));
            stockInput.setText(String.valueOf(p.getCurrentStock()));
            minStockInput.setText(String.valueOf(p.getMinStock()));
            if (modalHeaderLabel != null) modalHeaderLabel.setText("Edit Product");
            loadProductIngredients(p.getId()); 
        }
    }

    private void loadProductIngredients(int productId) {
        if (ingredientTable == null) return;
        for (IngredientRow row : ingredientTable.getItems()) {
            row.setSelected(false);
            row.setUsage(0.0);
        }
        String sql = "SELECT raw_material_id, quantity_required FROM product_ingredients WHERE product_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int rawId = rs.getInt("raw_material_id");
                double usage = rs.getDouble("quantity_required");
                for (IngredientRow row : ingredientTable.getItems()) {
                    if (row.getId() == rawId) {
                        row.setSelected(true);
                        row.setUsage(usage);
                        break;
                    }
                }
            }
            ingredientTable.refresh();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void openEditRawMaterialModal(RawMaterial rm) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AddRawMaterialModal.fxml"));
            Parent root = loader.load();
            InventoryController modalController = loader.getController();
            modalController.prepareRawMaterialEditMode(rm);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            refreshRawMaterials();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void prepareRawMaterialEditMode(RawMaterial rm) {
        editingRawMaterialId = rm.getId();
        if (rawNameInput != null) {
            rawNameInput.setText(rm.getName());
            unitCombo.setValue(rm.getUnit());
            rawStockInput.setText(String.valueOf(rm.getCurrentStock()));
            if (minStockInput != null) minStockInput.setText(String.valueOf(rm.getMinStock()));
        }
    }

    private void showDuplicateAlert(String name) { showAlert("Duplicate Entry", "The item '" + name + "' already exists!"); }
    
    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}