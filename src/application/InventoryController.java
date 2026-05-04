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

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/loris_cafe_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "user123"; 

    @FXML private FlowPane productGrid;
    @FXML private Label totalProductsLabel, totalValueLabel, lowStockLabel, avgStockLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo, stockLevelCombo;
    
    // Modal Inputs
    @FXML private TextField nameInput, priceInput, costInput, stockInput, minStockInput;
    @FXML private ComboBox<String> categoryInput;
    @FXML private TextArea descInput;
    @FXML private Label modalHeaderLabel;

    private List<Product> inventoryList = new ArrayList<>();
    private final String[] CATEGORIES = {"Milktea", "Cream Cheese", "Iced Coffee", "Latte Series", "Special Milktea", "Hot Drinks", "Meals"};
    private Product selectedProductForEdit;

    @FXML
    public void initialize() {
        if (categoryCombo != null) {
            setupFilters();
            searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshInventory());
            categoryCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshInventory());
            stockLevelCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshInventory());
            refreshInventory();
        }
        if (categoryInput != null) { categoryInput.getItems().addAll(CATEGORIES); }
    }

    private void setupFilters() {
        categoryCombo.getItems().setAll("All Categories");
        categoryCombo.getItems().addAll(CATEGORIES);
        categoryCombo.getSelectionModel().selectFirst();
        stockLevelCombo.getItems().setAll("All Stock Levels", "In Stock", "Low Stock", "Out of Stock");
        stockLevelCombo.getSelectionModel().selectFirst();
    }

    public void refreshInventory() {
        inventoryList.clear();
        StringBuilder query = new StringBuilder("SELECT * FROM products WHERE (name ILIKE ? OR description ILIKE ?)");
        
        String selectedCat = categoryCombo.getValue();
        if (selectedCat != null && !selectedCat.equals("All Categories")) {
            query.append(" AND category = '").append(selectedCat).append("'");
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(query.toString())) {
            pstmt.setString(1, "%" + searchField.getText() + "%");
            pstmt.setString(2, "%" + searchField.getText() + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                inventoryList.add(new Product(
                    rs.getInt("id"), rs.getString("name"), rs.getString("description"),
                    rs.getString("category"), rs.getDouble("selling_price"),
                    rs.getDouble("cost_price"), rs.getInt("current_stock"), rs.getInt("min_stock")
                ));
            }
            updateSummaryCards();
            renderProductGrid();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updateSummaryCards() {
        int totalProducts = inventoryList.size();
        double totalValue = 0;
        int lowStockCount = 0;
        int totalStock = 0;

        for (Product p : inventoryList) {
            totalValue += (p.getSellingPrice() * p.getCurrentStock());
            totalStock += p.getCurrentStock();
            if (p.getCurrentStock() <= p.getMinStock()) lowStockCount++;
        }

        totalProductsLabel.setText(String.valueOf(totalProducts));
        totalValueLabel.setText("₱" + String.format("%.0f", totalValue));
        lowStockLabel.setText(String.valueOf(lowStockCount));
        avgStockLabel.setText(String.valueOf(totalProducts == 0 ? 0 : totalStock / totalProducts));
    }

    private void renderProductGrid() {
        productGrid.getChildren().clear();
        for (Product p : inventoryList) {
            productGrid.getChildren().add(createProductCard(p));
        }
    }

    private VBox createProductCard(Product p) {
        VBox card = new VBox(10);
        card.getStyleClass().add("product-card");

        // Header with Icons
        HBox header = new HBox();
        VBox titleBox = new VBox(2);
        Label name = new Label(p.getName());
        name.getStyleClass().add("product-title");
        Label desc = new Label(p.getDescription() == null ? "" : p.getDescription());
        desc.getStyleClass().add("product-desc");
        titleBox.getChildren().addAll(name, desc);
        
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button editBtn = new Button("📝"); editBtn.getStyleClass().add("icon-button");
        editBtn.setOnAction(e -> handleEditProduct(p));
        Button delBtn = new Button("🗑"); delBtn.getStyleClass().add("icon-button");
        delBtn.setOnAction(e -> deleteProduct(p.getId()));
        header.getChildren().addAll(titleBox, spacer, editBtn, delBtn);

        // Tags
        HBox tags = new HBox(5);
        Label catTag = new Label(p.getCategory()); catTag.getStyleClass().add("tag-category");
        Label stockTag = new Label(p.getCurrentStock() <= p.getMinStock() ? "Low Stock" : "In Stock");
        stockTag.getStyleClass().add(p.getCurrentStock() <= p.getMinStock() ? "tag-lowstock" : "tag-instock");
        tags.getChildren().addAll(catTag, stockTag);

        // Pricing Info
        HBox pricing = new HBox(30);
        pricing.getChildren().addAll(
            new VBox(new Label("Selling Price"), new Label("₱" + p.getSellingPrice())),
            new VBox(new Label("Cost Price"), new Label("₱" + p.getCostPrice()))
        );

        ProgressBar bar = new ProgressBar(p.getStockPercentage());
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add("stock-bar");

        card.getChildren().addAll(header, tags, pricing, bar);
        return card;
    }

    private void handleEditProduct(Product p) {
        this.selectedProductForEdit = p;
        openProductModal("Edit Product");
    }

    private void deleteProduct(int id) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM products WHERE id = ?")) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            refreshInventory();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML 
    public void handleAddProduct() {
        this.selectedProductForEdit = null;
        openProductModal("Add New Product");
    }

    private void openProductModal(String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AddProductModal.fxml"));
            Parent root = loader.load();
            InventoryController ctrl = loader.getController();
            
            if (selectedProductForEdit != null) {
                ctrl.populateFields(selectedProductForEdit);
            }
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            refreshInventory();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void populateFields(Product p) {
        this.selectedProductForEdit = p;
        modalHeaderLabel.setText("Edit Product");
        nameInput.setText(p.getName());
        categoryInput.setValue(p.getCategory());
        priceInput.setText(String.valueOf(p.getSellingPrice()));
        costInput.setText(String.valueOf(p.getCostPrice()));
        stockInput.setText(String.valueOf(p.getCurrentStock()));
        minStockInput.setText(String.valueOf(p.getMinStock()));
        descInput.setText(p.getDescription());
    }

    @FXML
    public void saveNewProduct() {
        String sql;
        boolean isEdit = (selectedProductForEdit != null);
        if (isEdit) {
            sql = "UPDATE products SET name=?, category=?, selling_price=?, cost_price=?, current_stock=?, min_stock=?, description=? WHERE id=?";
        } else {
            sql = "INSERT INTO products (name, category, selling_price, cost_price, current_stock, min_stock, description) VALUES (?, ?, ?, ?, ?, ?, ?)";
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nameInput.getText());
            pstmt.setString(2, categoryInput.getValue());
            pstmt.setDouble(3, Double.parseDouble(priceInput.getText()));
            pstmt.setDouble(4, Double.parseDouble(costInput.getText()));
            pstmt.setInt(5, Integer.parseInt(stockInput.getText()));
            pstmt.setInt(6, Integer.parseInt(minStockInput.getText()));
            pstmt.setString(7, descInput.getText());
            if (isEdit) pstmt.setInt(8, selectedProductForEdit.getId());
            
            pstmt.executeUpdate();
            handleCloseModal();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void handleCloseModal() { ((Stage) nameInput.getScene().getWindow()).close(); }
}