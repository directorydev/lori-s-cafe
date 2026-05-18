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

public class InventoryStaffDashboardController {

    @FXML private BorderPane mainContainer; 
    @FXML private Label dateTimeLabel;
    @FXML private Label roleIndicatorLabel; // Label tracking the user role in the top header
    
    // Restricted Navbar Toggles 
    @FXML private ToggleButton dashboardToggle;
    @FXML private ToggleButton inventoryToggle;
    @FXML private ToggleButton logsToggle; // ADDED: Links to your new Inventory Logs button

    // Allowed Non-financial Staff KPI Labels
    @FXML private Label totalInventoryLabel;

    private static final String DB_URL = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0";
    private static final String DB_USER = "postgres.gwjmqejllljupondbzbs";
    private static final String DB_PASS = "Loritastecafe2026";   

    @FXML
    public void initialize() {
        startClock();
        refreshStaffDashboardStats();
        setupRoleHeader();
        
        // Auto-refresh inventory stats every 30 seconds
        Timeline refreshTimer = new Timeline(new KeyFrame(Duration.seconds(30), e -> refreshStaffDashboardStats()));
        refreshTimer.setCycleCount(Animation.INDEFINITE);
        refreshTimer.play();
        
        // Default view behavior on launch
        showInventory();
    }

    private void startClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            dateTimeLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy hh:mm:ss a")));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void setupRoleHeader() {
        if (roleIndicatorLabel != null && UserSession.isActive()) {
            roleIndicatorLabel.setText("Staff Console: " + UserSession.getDisplayName() + " (" + UserSession.getRole() + ")");
        }
    }

    /**
     * Synchronizes non-sensitive staff KPI counters with Supabase.
     * Completely avoids downloading transaction totals or revenue matrices.
     */
    private void refreshStaffDashboardStats() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String invSql = "SELECT SUM(current_stock) FROM raw_materials";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(invSql)) {
                if (rs.next() && totalInventoryLabel != null) {
                    totalInventoryLabel.setText(String.format("%.0f", rs.getDouble(1)));
                }
            }
        } catch (SQLException e) {
            System.err.println("Staff Metrics Sync Error: " + e.getMessage());
        }
    }

    // --- CARD HANDLERS ---
    @FXML
    private void handleCardInventory() {
        showInventory();
    }

    // --- TOP NAV BAR ACTIONS (RESTRICTED SET) ---
    @FXML 
    public void showDashboard() { 
        if (dashboardToggle != null) dashboardToggle.setSelected(true);
        try {
            Parent dashboardRoot = FXMLLoader.load(getClass().getResource("InventoryStaffDashboard.fxml"));
            if (mainContainer.getScene() != null) {
                mainContainer.getScene().setRoot(dashboardRoot);
            }
        } catch (Exception e) { 
            System.err.println("Error reloading staff dashboard: " + e.getMessage());
        }
    }
    
    @FXML 
    public void showInventoryLogs() { 
        if (logsToggle != null) logsToggle.setSelected(true);
        loadCenterPage("Settings.fxml"); 
    }
    
    @FXML 
    public void showInventory() { 
        if (inventoryToggle != null) inventoryToggle.setSelected(true);
        loadCenterPage("Inventory.fxml"); 
    }
    
    @FXML 
    public void handleLogout() { 
        UserSession.cleanSession(); // Safely clear authentication context
        try {
            Parent loginView = FXMLLoader.load(getClass().getResource("Login.fxml"));
            if (mainContainer.getScene() != null) {
                mainContainer.getScene().setRoot(loginView);
            }
        } catch (Exception e) { 
            System.exit(0); 
        }
    }

    // --- NAVIGATION HELPER MODULE ---
    private void loadCenterPage(String fxml) {
        // Enforce a security firewall: If a staff instance attempts to load admin panels, block it
        if (fxml.equalsIgnoreCase("Analytics.fxml") || fxml.equalsIgnoreCase("Orders.fxml")) {
            System.err.println("SECURITY WARNING: Unauthorized view access blocked for Inventory Staff.");
            return;
        }

        try {
            URL url = getClass().getResource(fxml);
            if (url == null) {
                url = getClass().getResource("/application/" + fxml);
            }
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            mainContainer.setCenter(root);
        } catch (Exception e) {
            System.err.println("Navigation Error loading " + fxml + ": " + e.getMessage());
        }
    }
}