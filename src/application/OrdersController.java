package application;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.Callback;

import java.io.File;
import java.io.PrintWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class OrdersController {

    @FXML private Label totalOrdersLabel, totalRevenueLabel, avgOrderValueLabel, completedOrdersLabel;
    @FXML private ComboBox<String> sortCombo, paymentCombo, timeCombo;
    @FXML private Button refreshBtn, exportBtn;
    
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Integer> colOrderId;
    @FXML private TableColumn<Order, String> colDate, colCustomer, colPayment;
    @FXML private TableColumn<Order, Double> colTotal;
    @FXML private TableColumn<Order, Void> colAction;

    private static final String DB_URL = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require";
    private static final String DB_USER = "postgres.gwjmqejllljupondbzbs";
    private static final String DB_PASS = "Loritastecafe2026";

    private ObservableList<Order> masterOrderList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupFilters();
        setupTableColumns();
        
        ordersTable.setItems(masterOrderList);
        executeDatabaseRefresh(); 
    }

    private void setupFilters() {
        sortCombo.getItems().addAll(
            "Order ID (Lowest First)", 
            "Order ID (Highest First)", 
            "Date & Time (Newest First)", 
            "Date & Time (Oldest First)"
        );
        sortCombo.getSelectionModel().selectFirst();
        sortCombo.setOnAction(e -> refreshData());

        paymentCombo.getItems().addAll("All Payments", "Cash", "E-Wallets");
        paymentCombo.getSelectionModel().selectFirst();
        paymentCombo.setOnAction(e -> refreshData());

        timeCombo.getItems().addAll("All Time", "Today", "This Week", "This Month");
        timeCombo.getSelectionModel().selectFirst();
        timeCombo.setOnAction(e -> refreshData());
    }

    private void setupTableColumns() {
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerDetails"));
        
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colTotal.setCellFactory(tc -> new TableCell<Order, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) { setText(null); } 
                else { setText("₱" + String.format("%,.2f", price)); }
            }
        });

        colPayment.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));

        setupActionColumn();
    }

    private void setupActionColumn() {
        Callback<TableColumn<Order, Void>, TableCell<Order, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Order, Void> call(final TableColumn<Order, Void> param) {
                return new TableCell<>() {
                    private final Button viewBtn = new Button("View");
                    private final Button voidBtn = new Button("Void");
                    private final HBox actionBox = new HBox(10, viewBtn, voidBtn);

                    {
                        viewBtn.setStyle("-fx-background-color: #f3f4f6; -fx-border-color: #d1d5db; -fx-border-radius: 4; -fx-cursor: hand; -fx-text-fill: #374151; -fx-font-weight: bold;");
                        voidBtn.setStyle("-fx-background-color: #fee2e2; -fx-border-color: #fca5a5; -fx-border-radius: 4; -fx-cursor: hand; -fx-text-fill: #dc2626; -fx-font-weight: bold;");

                        viewBtn.setOnAction(event -> {
                            Order order = getTableView().getItems().get(getIndex());
                            viewTransactionItems(order.getOrderId());
                        });

                        voidBtn.setOnAction(event -> {
                            Order order = getTableView().getItems().get(getIndex());
                            voidTransaction(order.getOrderId());
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) { setGraphic(null); } 
                        else { setGraphic(actionBox); }
                    }
                };
            }
        };
        colAction.setCellFactory(cellFactory);
    }

    @FXML
    public void refreshData() {
        if (refreshBtn != null) {
            refreshBtn.setText("⏳ Loading...");
            refreshBtn.setDisable(true);
        }

        PauseTransition pause = new PauseTransition(Duration.millis(800));
        pause.setOnFinished(event -> {
            executeDatabaseRefresh();
            
            if (refreshBtn != null) {
                refreshBtn.setText("🔄 Refresh");
                refreshBtn.setDisable(false);
            }
        });
        pause.play();
    }

    private void executeDatabaseRefresh() {
        masterOrderList.clear();

        double totalRevenue = 0;
        int totalOrders = 0;

        String sql = "SELECT id, order_date, total_amount, payment_method, cashier_name, order_type FROM transactions WHERE 1=1";
        
        String payFilter = paymentCombo.getValue();
        if ("Cash".equals(payFilter)) {
            sql += " AND payment_method ILIKE 'Cash'";
        } else if ("E-Wallets".equals(payFilter)) {
            sql += " AND payment_method NOT ILIKE 'Cash'";
        }

        String timeFilter = timeCombo.getValue();
        if ("Today".equals(timeFilter)) {
            sql += " AND DATE(order_date) = CURRENT_DATE";
        } else if ("This Week".equals(timeFilter)) {
            sql += " AND order_date >= date_trunc('week', CURRENT_DATE)";
        } else if ("This Month".equals(timeFilter)) {
            sql += " AND order_date >= date_trunc('month', CURRENT_DATE)";
        }

        String sortFilter = sortCombo.getValue();
        if ("Order ID (Highest First)".equals(sortFilter)) {
            sql += " ORDER BY id DESC";
        } else if ("Date & Time (Newest First)".equals(sortFilter)) {
            sql += " ORDER BY order_date DESC, id DESC";
        } else if ("Date & Time (Oldest First)".equals(sortFilter)) {
            sql += " ORDER BY order_date ASC, id ASC";
        } else {
            sql += " ORDER BY id ASC"; 
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                Timestamp date = rs.getTimestamp("order_date");
                double amount = rs.getDouble("total_amount");
                String payment = rs.getString("payment_method");
                String details = rs.getString("order_type") + " (" + rs.getString("cashier_name") + ")";
                
                String formattedDate = sdf.format(date);
                masterOrderList.add(new Order(id, formattedDate, details, amount, payment));
                
                totalRevenue += amount;
                totalOrders++;
            }

            totalOrdersLabel.setText(String.valueOf(totalOrders));
            completedOrdersLabel.setText(String.valueOf(totalOrders));
            totalRevenueLabel.setText("₱" + String.format("%,.2f", totalRevenue));
            avgOrderValueLabel.setText("₱" + String.format("%,.2f", totalOrders > 0 ? (totalRevenue / totalOrders) : 0));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- NEW: EXPORT DATA LOGIC ---
    @FXML
    public void handleExportData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Orders Report");
        
        // Auto-generate a clean file name with today's date
        String currentDate = new SimpleDateFormat("yyyyMMdd").format(new Date());
        fileChooser.setInitialFileName("Loris_Taste_Cafe_Orders_" + currentDate + ".csv");
        
        // Ensure it saves as a CSV so Excel can open it
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        Window window = ordersTable.getScene().getWindow();
        File file = fileChooser.showSaveDialog(window);

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                // 1. Write the Headers
                writer.println("Order ID,Date & Time,Customer,Total Amount,Payment Method");

                // 2. Loop through the current table data and write it line by line
                for (Order order : masterOrderList) {
                    writer.printf("%d,\"%s\",\"%s\",%.2f,\"%s\"\n",
                            order.getOrderId(),
                            order.getDate(),
                            order.getCustomerDetails(),
                            order.getTotalAmount(),
                            order.getPaymentMethod()
                    );
                }

                // 3. Show Success Alert
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText("Excel-Ready Report Generated");
                alert.setContentText("Your data has been successfully exported to:\n" + file.getAbsolutePath());
                alert.showAndWait();

            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Error");
                alert.setHeaderText("Failed to generate report");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void viewTransactionItems(int transactionId) {
        StringBuilder itemsText = new StringBuilder();
        itemsText.append("Transaction ID: ").append(transactionId).append("\n\n");
        itemsText.append(String.format("%-25s %-10s %-15s\n", "Item", "Qty", "Subtotal"));
        itemsText.append("--------------------------------------------------\n");

        String sql = "SELECT p.name, ti.quantity, ti.subtotal FROM transaction_items ti " +
                     "JOIN products p ON ti.product_id = p.id WHERE ti.transaction_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, transactionId);
            ResultSet rs = pstmt.executeQuery();

            double grandTotal = 0;
            while (rs.next()) {
                String name = rs.getString("name");
                int qty = rs.getInt("quantity");
                double subtotal = rs.getDouble("subtotal");
                grandTotal += subtotal;

                if(name.length() > 22) name = name.substring(0, 19) + "...";
                itemsText.append(String.format("%-25s x%-9d ₱%,.2f\n", name, qty, subtotal));
            }
            itemsText.append("--------------------------------------------------\n");
            itemsText.append(String.format("%-35s ₱%,.2f\n", "TOTAL:", grandTotal));

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Order Receipt Details");
            alert.setHeaderText("Items for Order #" + transactionId);
            
            TextArea area = new TextArea(itemsText.toString());
            area.setEditable(false);
            area.setWrapText(false);
            area.setStyle("-fx-font-family: 'Consolas', monospace;"); 
            
            alert.getDialogPane().setContent(area);
            alert.showAndWait();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void voidTransaction(int transactionId) {
        Alert alert = new Alert(Alert.AlertType.WARNING, 
            "Are you sure you want to void Order #" + transactionId + "?\n\nThis will permanently delete the transaction and its items from the system. This action cannot be undone.",
            ButtonType.YES, ButtonType.NO);
        alert.setTitle("Void Transaction Warning");
        alert.setHeaderText("Permanent Deletion Notice");

        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            String deleteItemsSql = "DELETE FROM transaction_items WHERE transaction_id = ?";
            String deleteTransactionSql = "DELETE FROM transactions WHERE id = ?";

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                conn.setAutoCommit(false); 
                
                try (PreparedStatement deleteItemsStmt = conn.prepareStatement(deleteItemsSql);
                     PreparedStatement deleteTransactionStmt = conn.prepareStatement(deleteTransactionSql)) {
                    
                    deleteItemsStmt.setInt(1, transactionId);
                    deleteItemsStmt.executeUpdate();
                    
                    deleteTransactionStmt.setInt(1, transactionId);
                    deleteTransactionStmt.executeUpdate();
                    
                    conn.commit(); 
                    refreshData(); 
                    
                } catch (SQLException ex) {
                    conn.rollback(); 
                    ex.printStackTrace();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}