package application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

    private static final String DB_URL = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0";
    private static final String DB_USER = "postgres.gwjmqejllljupondbzbs";
    private static final String DB_PASS = "Loritastecafe2026";

    @FXML
    public void initialize() {
        // Map Table Columns to the Order model properties
        colId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colCashier.setCellValueFactory(new PropertyValueFactory<>("customerDetails")); // Repurposed for Cashier Name
        colTotal.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty("₱" + String.format("%,.2f", cellData.getValue().getTotalAmount()))
        );
        colPayment.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        
        // Add a visual Status column
        colStatus.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty( ((OrderWithStatus) cellData.getValue()).getStatus() ));

        statusFilterCombo.getItems().addAll("All Orders", "Completed", "Pending", "For Void", "Voided");
        statusFilterCombo.getSelectionModel().select("All Orders");
        statusFilterCombo.setOnAction(e -> loadOrders());

        loadOrders();
    }

    @FXML
    public void loadOrders() {
        ObservableList<Order> orderList = FXCollections.observableArrayList();
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

                // We are reusing the customerDetails property in your Order.java model to hold the cashier/status info for now
                Order order = new Order(
                    rs.getInt("id"),
                    formattedDate,
                    cashier, 
                    rs.getDouble("total_amount"),
                    rs.getString("payment_method")
                );
                // Temporarily inject status into the Order class dynamically
                orderList.add(new OrderWithStatus(order, status));
            }
            ordersTable.setItems(orderList);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleViewDetails() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select an order from the list first.");
            return;
        }
        System.out.println("Opening receipt details for Order ID: " + selected.getOrderId());
        // TODO: Launch a modal showing transaction_items for this order ID
    }

    @FXML
    public void handleApproveVoid() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select an order to void.");
            return;
        }

        if (selected instanceof OrderWithStatus && "Voided".equals(((OrderWithStatus) selected).getStatus())) {
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
                loadOrders(); // Refresh table
                
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

    // Temporary helper class to extend your existing Order model without breaking other code
    public static class OrderWithStatus extends Order {
        private final String status;
        public OrderWithStatus(Order base, String status) {
            super(base.getOrderId(), base.getDate(), base.getCustomerDetails(), base.getTotalAmount(), base.getPaymentMethod());
            this.status = status;
        }
        public String getStatus() { return status; }
    }
}