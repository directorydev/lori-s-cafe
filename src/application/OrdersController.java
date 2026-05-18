package application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.*;
import java.time.format.DateTimeFormatter;

public class OrdersController {

    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Integer> colId;
    @FXML private TableColumn<Order, String> colDate;
    @FXML private TableColumn<Order, String> colCashier;
    @FXML private TableColumn<Order, String> colTotal;
    @FXML private TableColumn<Order, String> colPayment;
    @FXML private TableColumn<Order, String> colStatus;
    
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TextField searchField; // NEW: Search Bar

    private ObservableList<Order> masterOrderList = FXCollections.observableArrayList();

    private static final String DB_URL = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0";
    private static final String DB_USER = "postgres.gwjmqejllljupondbzbs";
    private static final String DB_PASS = "Loritastecafe2026";

    @FXML
    public void initialize() {
        // Map Table Columns to the cleaned-up Order model properties
        colId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colCashier.setCellValueFactory(new PropertyValueFactory<>("cashierName"));
        colTotal.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty("₱" + String.format("%,.2f", cellData.getValue().getTotalAmount()))
        );
        colPayment.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Add colors to the Status column!
        colStatus.setCellFactory(column -> new TableCell<Order, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("Completed")) {
                        setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;"); // Green
                    } else if (item.equals("Voided")) {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;"); // Red
                    } else {
                        setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;"); // Orange
                    }
                }
            }
        });

        statusFilterCombo.getItems().addAll("All Orders", "Completed", "Pending", "For Void", "Voided");
        statusFilterCombo.getSelectionModel().select("All Orders");
        statusFilterCombo.setOnAction(e -> loadOrders());

        loadOrders();
    }

    @FXML
    public void loadOrders() {
        masterOrderList.clear();
        String filter = statusFilterCombo.getValue();
        
        String sql = "SELECT id, order_date, cashier_name, total_amount, payment_method, status FROM transactions";
        if (!"All Orders".equals(filter)) {
            sql += " WHERE status = ?";
        }
        sql += " ORDER BY order_date DESC";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            if (!"All Orders".equals(filter)) {
                pstmt.setString(1, filter);
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String formattedDate = rs.getTimestamp("order_date").toLocalDateTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a"));
                String cashier = rs.getString("cashier_name") != null ? rs.getString("cashier_name") : "Unknown";
                String status = rs.getString("status") != null ? rs.getString("status") : "Completed";

                // Using the clean, updated Order model
                masterOrderList.add(new Order(
                    rs.getInt("id"),
                    formattedDate,
                    cashier, 
                    rs.getDouble("total_amount"),
                    rs.getString("payment_method"),
                    status
                ));
            }
            
            // Apply the Live Search Logic
            setupSearchFilter();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupSearchFilter() {
        FilteredList<Order> filteredData = new FilteredList<>(masterOrderList, b -> true);
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(order -> {
                if (newValue == null || newValue.isEmpty()) return true;
                
                String lowerCaseFilter = newValue.toLowerCase();
                
                if (String.valueOf(order.getOrderId()).contains(lowerCaseFilter)) return true;
                if (order.getCashierName().toLowerCase().contains(lowerCaseFilter)) return true;
                
                return false; // Does not match
            });
        });
        
        SortedList<Order> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(ordersTable.comparatorProperty());
        ordersTable.setItems(sortedData);
    }

    @FXML
    public void handleViewDetails() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select an order from the list first.");
            return;
        }

        try {
            // Using absolute classpath mapping to prevent Location is not set errors
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/application/ReceiptModal.fxml"));
            javafx.scene.Parent root = loader.load();
            
            // Pass the selected order data to the receipt controller
            ReceiptModalController controller = loader.getController();
            controller.initData(selected);

            // Create a pop-up window (Stage)
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL); // Blocks clicking the main window
            stage.setTitle("Digital Receipt - Order #" + selected.getOrderId());
            
            // Ensure the window background is transparent so the drop shadow looks like real paper
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            
            stage.setScene(scene);
            stage.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not load the receipt modal.");
        }
    }

    @FXML
    public void handleApproveVoid() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select an order to void.");
            return;
        }

        if ("Voided".equals(selected.getStatus())) {
            showAlert("Already Voided", "This order has already been voided.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to VOID Order #" + selected.getOrderId() + "? This will return the inventory.", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            processVoidTransaction(selected.getOrderId());
        }
    }

    private void processVoidTransaction(int orderId) {
        String updateStatusSql = "UPDATE transactions SET status = 'Voided' WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            conn.setAutoCommit(false); // Start transaction
            
            try (PreparedStatement pstmt = conn.prepareStatement(updateStatusSql)) {
                pstmt.setInt(1, orderId);
                pstmt.executeUpdate();
                
                // TODO: Here is where you will write the SQL to loop through transaction_items 
                // and UPDATE raw_materials SET current_stock = current_stock + (returned amounts)
                
                conn.commit();
                loadOrders(); // Refresh table immediately
                
                Alert success = new Alert(Alert.AlertType.INFORMATION, "Order #" + orderId + " successfully voided.");
                success.showAndWait();
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                showAlert("Database Error", "Failed to void transaction.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}