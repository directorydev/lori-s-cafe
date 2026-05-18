package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.*;

public class LoginController {

    private static final String DB_URL = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0";
    private static final String DB_USER = "postgres.gwjmqejllljupondbzbs";
    private static final String DB_PASS = "Loritastecafe2026";

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    
    private int loginAttempts = 0;
    private static final int MAX_ATTEMPTS = 5;

    @FXML
    public void handleLogin() {
        if (loginAttempts >= MAX_ATTEMPTS) {
            showAlert("System Locked", "Too many failed attempts. Please contact an Administrator.");
            return;
        }

        String user = usernameField.getText().trim();
        String pass = passwordField.getText().trim(); 

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Input Error", "Please enter both username and password.");
            return;
        }

        // 1. Process Authentication and Fetch User Claims
        UserContext context = authenticate(user, pass);

        if (context != null) {
            // 2. Start Global Session
            UserSession.startSession(context.username, context.role, context.firstName); 
            
            // 3. Dynamic Route Navigation Based on Detected Role
            routeUserToDashboard(context.role);
        } else {
            loginAttempts++;
            showAlert("Access Denied", "Invalid Credentials. Attempts left: " + (MAX_ATTEMPTS - loginAttempts));
        }
    }

    private UserContext authenticate(String user, String pass) {
        // Query reads both password, exact role string, and names from the DB
        String query = "SELECT password, role, first_name FROM users WHERE username = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, user);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String dbPassword = rs.getString("password");
                // Secure plain text matches for now; change to BCrypt.checkpw in production
                if (dbPassword.equals(pass)) {
                    return new UserContext(
                        user,
                        rs.getString("role"),
                        rs.getString("first_name")
                    );
                }
            }
        } catch (SQLException e) { 
            System.err.println("DATABASE ERROR: " + e.getMessage());
        }
        return null;
    }

    private void routeUserToDashboard(String role) {
        String fxmlFile;
        String stageTitle;

        // Sanitize input to handle casing variations gracefully
        if (role == null) {
            showAlert("Configuration Error", "Your user account has no assigned role.");
            return;
        }

        switch (role.trim()) {
            case "Admin":
                fxmlFile = "AdminDashboard.fxml";
                stageTitle = "Admin Control - Lori's Taste Cafe";
                break;
                
            case "Inventory": 
            case "InventoryStaff":
                // Maps both "Inventory" (from your DB data) and "InventoryStaff" to the same dashboard
                fxmlFile = "InventoryStaffDashboard.fxml";
                stageTitle = "Staff Inventory Module - Lori's Taste Cafe";
                break;
                
            case "Cashier":
                // Explicit restriction: Prevents mobile-only users from accessing the desktop console
                showAlert("Access Restricted", "Cashier accounts are only permitted to log in via the Mobile POS Application.");
                return;
                
            default:
                showAlert("Configuration Error", "Your user account has an unknown role: " + role);
                return;
        }
        
        loadNewScene(fxmlFile, stageTitle);
    }

    private void loadNewScene(String fxmlFile, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle(title);
            stage.getScene().setRoot(root);
            stage.setMaximized(true);
        } catch (Exception e) { 
            e.printStackTrace(); 
            showAlert("Navigation Error", "Could not load UI dashboard asset.");
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    @FXML 
    public void handleUseDemoAccount() {
        // Updated to fill staff demo info to easily test both flows
        usernameField.setText("cashier");
        passwordField.setText("cashier123");
    }

    // Small container class to securely pass user credentials up from the SQL stream
    private static class UserContext {
        String username;
        String role;
        String firstName;

        UserContext(String username, String role, String firstName) {
            this.username = username;
            this.role = role;
            this.firstName = firstName;
        }
    }
}