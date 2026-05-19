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
import java.net.URL;

public class DashboardController {

    @FXML private BorderPane mainContainer; 
    @FXML private Label dateTimeLabel;
    
    @FXML private ToggleButton dashboardToggle;
    @FXML private ToggleButton inventoryToggle;
    @FXML private ToggleButton ordersToggle;
    @FXML private ToggleButton analyticsToggle;
    @FXML private ToggleButton settingsToggle;

    @FXML private Label totalInventoryLabel;
    @FXML private Label todaySalesLabel;
    @FXML private Label todayOrdersLabel;

    private static final String DB_URL = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0";
    private static final String DB_USER = "postgres.gwjmqejllljupondbzbs";
    private static final String DB_PASS = "Loritastecafe2026";	

    @FXML
    public void initialize() {
        startClock();
        refreshDashboardStats();
        
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
            System.err.println("Database Sync Error: " + e.getMessage());
        }
    }

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
    private void handleCardAnalytics() {
        if (analyticsToggle != null) analyticsToggle.setSelected(true);
        loadCenterPage("Analytics.fxml");
    }

    @FXML
    private void handleCardLogs() {
        if (settingsToggle != null) settingsToggle.setSelected(true); 
        loadCenterPage("AuditLogs.fxml"); 
    }

    @FXML
    private void handleStartSale() {
        System.out.println("Opening Cashier POS...");
        loadCenterPage("POS.fxml"); 
    }

    @FXML 
    public void showDashboard() { 
        if (dashboardToggle != null) dashboardToggle.setSelected(true);
        try {
            Parent dashboardRoot = FXMLLoader.load(getClass().getResource("AdminDashboard.fxml"));
            if (mainContainer.getScene() != null) {
                mainContainer.getScene().setRoot(dashboardRoot);
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
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
        loadCenterPage("AuditLogs.fxml"); 
    }
    
    @FXML 
    public void handleLogout() { 
        try {
            Parent loginView = FXMLLoader.load(getClass().getResource("Login.fxml"));
            if (mainContainer.getScene() != null) {
                mainContainer.getScene().setRoot(loginView);
            } else {
                System.exit(0);
            }
        } catch (Exception e) { 
            System.exit(0); 
        }
    }

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
            e.printStackTrace();
        }
    }
}