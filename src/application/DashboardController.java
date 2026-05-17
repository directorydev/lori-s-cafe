package application;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DashboardController {

    @FXML private BorderPane mainContainer; 
    @FXML private Label dateTimeLabel;
    
    // Navbar Toggles (To sync bold state)
    @FXML private ToggleButton dashboardToggle;
    @FXML private ToggleButton inventoryToggle;
    @FXML private ToggleButton ordersToggle;
    @FXML private ToggleButton analyticsToggle;
    @FXML private ToggleButton settingsToggle;

    // KPI Labels
    @FXML private Label totalInventoryLabel;
    @FXML private Label todaySalesLabel;
    @FXML private Label todayOrdersLabel;

    // FIXED: Added &prepareThreshold=0
    private static final String DB_URL = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0";
    private static final String DB_USER = "postgres.gwjmqejllljupondbzbs";
    private static final String DB_PASS = "Loritastecafe2026";	

    @FXML
    public void initialize() {
        startClock();
        refreshDashboardStats();
    }

    private void startClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            dateTimeLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy hh:mm:ss a")));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void refreshDashboardStats() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String invSql = "SELECT SUM(current_stock) FROM raw_materials";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(invSql)) {
                if (rs.next()) totalInventoryLabel.setText(String.format("%.0f", rs.getDouble(1)));
            }

            String salesSql = "SELECT SUM(total_amount), COUNT(id) FROM transactions WHERE DATE(order_date) = CURRENT_DATE";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(salesSql)) {
                if (rs.next()) {
                    todaySalesLabel.setText("₱" + String.format("%,.2f", rs.getDouble(1)));
                    todayOrdersLabel.setText(String.valueOf(rs.getInt(2)));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- CARD CLICK HANDLERS (Now with Navbar Sync) ---

    @FXML
    private void handleCardInventory() {
        if (inventoryToggle != null) inventoryToggle.setSelected(true); 
        loadCenterPage("Inventory.fxml");
    }

    @FXML
    private void handleCardOrders() {
        if (ordersToggle != null) ordersToggle.setSelected(true); 
        loadCenterPage("Orders.fxml");
    }

    @FXML
    private void handleCardLogs() {
        if (settingsToggle != null) settingsToggle.setSelected(true); 
        loadCenterPage("Settings.fxml"); // This is your Inventory Logs
    }

    @FXML
    private void handleStartSale() {
        System.out.println("Opening Cashier POS...");
    }

    // --- RESTORED TOP NAV BAR ACTIONS ---

    @FXML 
    public void showDashboard() { 
        if (dashboardToggle != null) dashboardToggle.setSelected(true);
        // Add logic to reload the main dashboard VBox if needed
    }

    @FXML 
    public void showInventory() { 
        if (inventoryToggle != null) inventoryToggle.setSelected(true);
        loadCenterPage("Inventory.fxml"); 
    }
    
    @FXML 
    public void showOrders() { 
        if (ordersToggle != null) ordersToggle.setSelected(true);
        loadCenterPage("Orders.fxml"); 
    }
    
    @FXML 
    public void handleAnalytics() { 
        if (analyticsToggle != null) analyticsToggle.setSelected(true);
        loadCenterPage("Analytics.fxml"); 
    }
    
    @FXML 
    public void handleSettings() { 
        if (settingsToggle != null) settingsToggle.setSelected(true);
        loadCenterPage("Settings.fxml"); 
    }
    
    @FXML 
    public void handleLogout() { 
        System.exit(0); 
    }

    // --- HELPER METHOD ---
    private void loadCenterPage(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            mainContainer.setCenter(root);
        } catch (Exception e) {
            System.err.println("Error loading " + fxml + ": " + e.getMessage());
            e.printStackTrace(); 
        }
    }
}