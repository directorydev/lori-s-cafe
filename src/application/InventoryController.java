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

	// SUPABASE CLOUD CONNECTION
		// 1. Add 'jdbc:' to the start and use port 6543 for the pooler
		private static final String DB_URL = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require";
		private static final String DB_USER = "postgres.gwjmqejllljupondbzbs";
		private static final String DB_PASS = "Loritastecafe2026"; 

    @FXML private FlowPane productGrid;
    @FXML private ToggleGroup inventoryTabs;
    @FXML private ToggleButton productTab, rawMaterialTab;
    @FXML private Button addItemBtn;
    
    @FXML private Label totalProductsLabel, totalValueLabel, lowStockLabel, avgStockLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo;
    
    // --- NEW FEATURE: Stock Level ComboBox ---
    @FXML private ComboBox<String> stockLevelCombo;

    @FXML private TextField nameInput, priceInput, costInput, stockInput, minStockInput;
    @FXML private ComboBox<String> categoryInput; 
    @FXML private Label modalHeaderLabel;
    
    @FXML private TextField rawNameInput, rawStockInput;
    @FXML private ComboBox<String> unitCombo;

    private boolean isProductMode = true;
    private static int editingProductId = -1;
    private static int editingRawMaterialId = -1;

    private final String[] CATEGORIES = {
        "Milktea", "Cream Cheese", "Iced Coffee", 
        "Latte Series", "Hot Drinks", "Refreshers", "Meals", "Coffee"
    };

    @FXML
    public void initialize() {
        setupTabListeners();
        setupCategoryFiltering(); 
        setupStockLevelFiltering(); // Initialize new feature
        setupSearchListener();
        
        if (categoryInput != null) {
            categoryInput.getItems().addAll(CATEGORIES);
        }

        if (productGrid != null) refreshInventory();
        
        if (unitCombo != null) {
            unitCombo.getItems().addAll("Grams", "ml", "Pieces", "kg", "Liters"); 
        }
    }

    // --- NEW FEATURE: Setup Stock Level Filter ---
    private void setupStockLevelFiltering() {
        if (stockLevelCombo != null) {
            stockLevelCombo.getItems().setAll("All Stock Levels", "In Stock", "Low Stock", "Out of Stock");
            stockLevelCombo.getSelectionModel().selectFirst();
            stockLevelCombo.setOnAction(e -> {
                if (isProductMode) refreshInventory(); else refreshRawMaterials();
            });
        }
    }

    // --- DUPLICATE PREVENTION LOGIC (Preserved) ---
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

    private void showDuplicateAlert(String name) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Duplicate Entry");
        alert.setHeaderText(null);
        alert.setContentText("The item '" + name + "' already exists! Please use a unique name.");
        alert.showAndWait();
    }

    // --- REFRESH LOGIC (Modified to include Stock Level Filtering) ---
    public void refreshInventory() {
        if (productGrid == null) return;
        productGrid.getChildren().clear();
        
        String selectedCategory = categoryCombo.getSelectionModel().getSelectedItem();
        String selectedStockLevel = (stockLevelCombo != null) ? stockLevelCombo.getSelectionModel().getSelectedItem() : "All Stock Levels";
        
        StringBuilder sql = new StringBuilder("SELECT * FROM products WHERE name ILIKE ?");
        
        if (selectedCategory != null && !selectedCategory.equals("All Categories")) {
            sql.append(" AND category = '").append(selectedCategory).append("'");
        }
        
        // Add accurate Stock Level logic for Products
        if (selectedStockLevel != null) {
            if (selectedStockLevel.equals("In Stock")) {
                sql.append(" AND current_stock > min_stock");
            } else if (selectedStockLevel.equals("Low Stock")) {
                sql.append(" AND current_stock <= min_stock AND current_stock > 0");
            } else if (selectedStockLevel.equals("Out of Stock")) {
                sql.append(" AND current_stock <= 0");
            }
        }
        
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

    public void refreshRawMaterials() {
        if (productGrid == null) return;
        productGrid.getChildren().clear();
        
        String selectedStockLevel = (stockLevelCombo != null) ? stockLevelCombo.getSelectionModel().getSelectedItem() : "All Stock Levels";
        StringBuilder sql = new StringBuilder("SELECT * FROM raw_materials WHERE name ILIKE ?");
        
        // Accurate Stock Filtering for Raw Materials
        if (selectedStockLevel != null && !selectedStockLevel.equals("All Stock Levels")) {
            sql.append(" AND status = '").append(selectedStockLevel).append("'");
        }

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

    // --- PRODUCT CARD & MODAL LOGIC (Preserved) ---
    private VBox createProductCard(Product p) {
        VBox card = new VBox(12);
        card.getStyleClass().add("product-card");
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
        Button editBtn = new Button("📝");
        editBtn.getStyleClass().add("icon-button");
        editBtn.setOnAction(e -> openEditProductModal(p)); 
        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("icon-button");
        deleteBtn.setOnAction(e -> deleteProduct(p));
        actionIcons.getChildren().addAll(editBtn, deleteBtn);
        headerRow.getChildren().addAll(titleBox, spacer, actionIcons);
        HBox tagsRow = new HBox(8);
        Label catTag = new Label(p.getCategory());
        catTag.getStyleClass().add("tag-category");
        boolean isLow = p.getCurrentStock() <= p.getMinStock();
        Label statusTag = new Label(isLow ? "Low Stock" : "In Stock");
        statusTag.getStyleClass().add(isLow ? "tag-lowstock" : "tag-instock");
        tagsRow.getChildren().addAll(catTag, statusTag);
        GridPane priceGrid = new GridPane();
        priceGrid.setHgap(40); priceGrid.setVgap(5);
        priceGrid.add(new Label("Selling Price"), 0, 0);
        Label sellValue = new Label("₱" + String.format("%.0f", p.getSellingPrice()));
        sellValue.getStyleClass().add("price-value-sell");
        priceGrid.add(sellValue, 0, 1);
        priceGrid.add(new Label("Cost Price"), 1, 0);
        Label costValue = new Label("₱" + String.format("%.0f", p.getCostPrice()));
        costValue.getStyleClass().add("price-value-cost");
        priceGrid.add(costValue, 1, 1);
        ProgressBar stockBar = new ProgressBar(p.getStockPercentage());
        stockBar.getStyleClass().add("stock-bar");
        stockBar.setMaxWidth(Double.MAX_VALUE);
        Button adjustBtn = new Button("📝 Adjust Stock");
        adjustBtn.getStyleClass().add("adjust-btn");
        adjustBtn.setMaxWidth(Double.MAX_VALUE);
        adjustBtn.setOnAction(e -> openEditProductModal(p));
        card.getChildren().addAll(headerRow, tagsRow, priceGrid, stockBar, adjustBtn);
        return card;
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
            categoryInput.setValue(p.getCategory());
            priceInput.setText(String.valueOf(p.getSellingPrice()));
            costInput.setText(String.valueOf(p.getCostPrice()));
            stockInput.setText(String.valueOf(p.getCurrentStock()));
            minStockInput.setText(String.valueOf(p.getMinStock()));
            if (modalHeaderLabel != null) modalHeaderLabel.setText("Edit Product");
        }
    }

    @FXML
    public void saveNewProduct() {
        String name = nameInput.getText().trim();
        if (isDuplicateName("products", name, editingProductId)) {
            showDuplicateAlert(name);
            return;
        }
        String sql = (editingProductId == -1) 
            ? "INSERT INTO products (name, category, selling_price, cost_price, current_stock, min_stock) VALUES (?, ?, ?, ?, ?, ?)"
            : "UPDATE products SET name=?, category=?, selling_price=?, cost_price=?, current_stock=?, min_stock=? WHERE id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, categoryInput.getValue());
            pstmt.setDouble(3, Double.parseDouble(priceInput.getText()));
            pstmt.setDouble(4, Double.parseDouble(costInput.getText()));
            pstmt.setInt(5, Integer.parseInt(stockInput.getText()));
            pstmt.setInt(6, Integer.parseInt(minStockInput.getText()));
            if (editingProductId != -1) pstmt.setInt(7, editingProductId);
            pstmt.executeUpdate();
            handleCloseModal();
            refreshInventory();
            editingProductId = -1;
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void deleteProduct(Product p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + p.getName() + "? This cannot be undone.", ButtonType.OK, ButtonType.CANCEL);
        if (alert.showAndWait().get() == ButtonType.OK) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM products WHERE id = ?")) {
                pstmt.setInt(1, p.getId());
                pstmt.executeUpdate();
                refreshInventory();
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // --- RAW MATERIAL LOGIC (Preserved) ---
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

    @FXML
    public void saveRawMaterial() {
        String name = rawNameInput.getText().trim();
        if (isDuplicateName("raw_materials", name, editingRawMaterialId)) {
            showDuplicateAlert(name);
            return;
        }
        String sql = (editingRawMaterialId == -1)
            ? "INSERT INTO raw_materials (name, unit, current_stock, min_stock, status) VALUES (?, ?, ?, ?, ?)"
            : "UPDATE raw_materials SET name=?, unit=?, current_stock=?, min_stock=?, status=? WHERE id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            double current = Double.parseDouble(rawStockInput.getText());
            double min = (minStockInput != null && !minStockInput.getText().isEmpty()) ? Double.parseDouble(minStockInput.getText()) : 0;
            String status = (current <= min) ? "Low Stock" : "In Stock";
            if (current <= 0) status = "Out of Stock";
            pstmt.setString(1, name);
            pstmt.setString(2, unitCombo.getValue());
            pstmt.setDouble(3, current);
            pstmt.setDouble(4, min);
            pstmt.setString(5, status);
            if (editingRawMaterialId != -1) pstmt.setInt(6, editingRawMaterialId);
            pstmt.executeUpdate();
            handleCloseModal();
            refreshRawMaterials();
            editingRawMaterialId = -1;
        } catch (Exception e) { e.printStackTrace(); }
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

    private VBox createRawMaterialCard(RawMaterial rm) {
        VBox card = new VBox(12);
        card.getStyleClass().add("product-card"); card.setPrefWidth(330);
        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label name = new Label(rm.getName());
        name.getStyleClass().add("product-title");
        Pane spacer = new Pane(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(10);
        Button editBtn = new Button("📝"); editBtn.getStyleClass().add("icon-button");
        editBtn.setOnAction(e -> openEditRawMaterialModal(rm)); 
        Button deleteBtn = new Button("🗑"); deleteBtn.getStyleClass().add("icon-button");
        deleteBtn.setOnAction(e -> deleteRawMaterial(rm));
        actions.getChildren().addAll(editBtn, deleteBtn);
        header.getChildren().addAll(name, spacer, actions);
        Label desc = new Label(rm.getName().toLowerCase() + " (per " + rm.getUnit() + ")");
        desc.getStyleClass().add("product-desc");
        ProgressBar stockBar = new ProgressBar(rm.getStockPercentage());
        stockBar.getStyleClass().add("stock-bar"); stockBar.setMaxWidth(Double.MAX_VALUE);
        card.getChildren().addAll(header, desc, stockBar);
        return card;
    }

    // --- SUMMARY & UTILITIES (Preserved) ---
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
        if (nameInput != null && nameInput.getScene() != null) {
            stage = (Stage) nameInput.getScene().getWindow();
        } else if (rawNameInput != null && rawNameInput.getScene() != null) {
            stage = (Stage) rawNameInput.getScene().getWindow();
        }
        if (stage != null) stage.close(); 
    }

    private void setupTabListeners() {
        if (inventoryTabs != null) {
            inventoryTabs.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == productTab) {
                    isProductMode = true; addItemBtn.setText("+ Add Product");
                    categoryCombo.setDisable(false); refreshInventory();
                } else if (newVal == rawMaterialTab) {
                    isProductMode = false; addItemBtn.setText("+ Add Raw Material");
                    categoryCombo.setDisable(true); refreshRawMaterials();
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
}