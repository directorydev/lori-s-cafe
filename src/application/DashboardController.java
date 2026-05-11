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

public class DashboardController {

    @FXML private BorderPane mainContainer; 
    @FXML private Label dateTimeLabel;
    
    @FXML private Label totalInventoryLabel;
    @FXML private Label todaySalesLabel;
    @FXML private Label todayOrdersLabel;

    private static final String DB_URL = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require";
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
                if (rs.next()) {
                    totalInventoryLabel.setText(String.format("%.0f", rs.getDouble(1)));
                }
            }

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
            e.printStackTrace();
        }
    }

    // --- NAVIGATION LOGIC ---

    @FXML private void handleCardInventory() { loadCenterPage("Inventory.fxml"); }
    @FXML private void handleCardOrders() { loadCenterPage("Orders.fxml"); }
    @FXML private void handleCardLogs() { loadCenterPage("Settings.fxml"); }
    
    @FXML
    private void handleStartSale() {
        // Placeholder for POS
        System.out.println("Opening Cashier POS...");
    }

    private void loadCenterPage(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            mainContainer.setCenter(root);
        } catch (Exception e) {
            System.err.println("Error loading " + fxml + ": " + e.getMessage());
        }
    }

    @FXML private void showDashboard() { 
        // Logic to reload the dashboard content if necessary
    }
    
    @FXML private void showInventory() { loadCenterPage("Inventory.fxml"); }
    @FXML private void showOrders() { loadCenterPage("Orders.fxml"); }
    @FXML private void handleAnalytics() { System.out.println("Analytics under construction"); }
    @FXML private void handleSettings() { loadCenterPage("Settings.fxml"); }
    @FXML private void handleLogout() { System.exit(0); }
}