package application;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.net.URL;

public class DashboardController {

    @FXML private BorderPane mainContainer; 
    @FXML private Label dateTimeLabel;
    
    @FXML private Label totalInventoryLabel;
    @FXML private Label todaySalesLabel;
    @FXML private Label todayOrdersLabel;

    // Use prepareThreshold=0 to bypass Supabase PgBouncer "Prepared Statement already exists" errors
    private static final String DB_URL = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0";
    private static final String DB_USER = "postgres.gwjmqejllljupondbzbs";
    private static final String DB_PASS = "Loritastecafe2026";

    @FXML
    public void initialize() {
        startClock();
        refreshDashboardStats();
        
        // Auto-refresh stats every 30 seconds to sync with Mobile POS sales in real-time
        Timeline refreshTimer = new Timeline(new KeyFrame(Duration.seconds(30), e -> refreshDashboardStats()));
        refreshTimer.setCycleCount(Animation.INDEFINITE);
        refreshTimer.play();
    }

    private void startClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            dateTimeLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy hh:mm:ss a")));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    /**
     * Synchronizes Dashboard KPI cards with Supabase in real-time.
     */
    private void refreshDashboardStats() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            
            // 1. Fetch Total Raw Material Inventory
            String invSql = "SELECT SUM(current_stock) FROM raw_materials";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(invSql)) {
                if (rs.next()) {
                    totalInventoryLabel.setText(String.format("%.0f", rs.getDouble(1)));
                }
            }

            // 2. Fetch Today's Revenue and Order Count
            String salesSql = "SELECT SUM(total_amount), COUNT(id) FROM transactions WHERE DATE(order_date) = CURRENT_DATE";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(salesSql)) {
                if (rs.next()) {
                    double sales = rs.getDouble(1);
                    int count = rs.getInt(2);
                    todaySalesLabel.setText("₱" + String.format("%,.2f", sales));
                    todayOrdersLabel.setText(String.valueOf(count));
                }
            }
        } catch (SQLException e) {
            System.err.println("Database Sync Error: " + e.getMessage());
        }
    }

    // --- NAVIGATION LOGIC ---

    /**
     * FIX: Resolves LoadException by providing the method referenced in AdminDashboard.fxml
     */
    @FXML
    private void handleStartSale() {
        System.out.println("Opening Cashier POS...");
        loadCenterPage("POS.fxml"); 
    }

    @FXML private void handleCardInventory() { loadCenterPage("Inventory.fxml"); }
    @FXML private void handleCardOrders() { loadCenterPage("Orders.fxml"); }
    @FXML private void handleCardAnalytics() { loadCenterPage("Analytics.fxml"); }
    @FXML private void handleCardLogs() { loadCenterPage("Settings.fxml"); }

    /**
     * Robust resource loader to prevent "FXML file not found" errors in Eclipse.
     */
    private void loadCenterPage(String fxml) {
        try {
            URL url = getClass().getResource(fxml);
            if (url == null) {
                url = getClass().getResource("/application/" + fxml);
            }
            
            if (url == null) {
                throw new java.io.IOException("Cannot find resource: " + fxml);
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            mainContainer.setCenter(root);
        } catch (Exception e) {
            System.err.println("Critical Navigation Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML 
    private void showDashboard() { 
        try {
            Parent dashboardRoot = FXMLLoader.load(getClass().getResource("AdminDashboard.fxml"));
            mainContainer.getScene().setRoot(dashboardRoot);
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    @FXML private void showInventory() { loadCenterPage("Inventory.fxml"); }
    @FXML private void showOrders() { loadCenterPage("Orders.fxml"); }
    @FXML private void handleAnalytics() { loadCenterPage("Analytics.fxml"); }
    @FXML private void handleSettings() { loadCenterPage("Settings.fxml"); }
    
    @FXML 
    private void handleLogout() { 
        try {
            Parent loginView = FXMLLoader.load(getClass().getResource("Login.fxml"));
            mainContainer.getScene().setRoot(loginView);
        } catch (Exception e) { System.exit(0); }
    }
}