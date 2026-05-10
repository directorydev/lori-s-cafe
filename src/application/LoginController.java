package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.*;

public class LoginController {


	// SUPABASE CLOUD CONNECTION
	private static final String DB_URL = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require";
	private static final String DB_USER = "postgres.gwjmqejllljupondbzbs";
	private static final String DB_PASS = "Loritastecafe2026"; 


    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    
    private int loginAttempts = 0;
    private static final int MAX_ATTEMPTS = 5;

    @FXML
    public void handleLogin() {
        if (loginAttempts >= MAX_ATTEMPTS) {
            showAlert("System Locked", "Too many failed attempts. Restart the application.");
            return;
        }

        String user = usernameField.getText().trim();
        String pass = passwordField.getText().trim(); 

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Input Error", "Please enter both username and password.");
            return;
        }

        if (authenticate(user, pass)) {
            UserSession.startSession(user); 
            loadDashboard("AdminDashboard.fxml", "Admin Control - Lori's Taste Cafe");
        } else {
            loginAttempts++;
            showAlert("Access Denied", "Invalid Admin credentials. Attempts left: " + (MAX_ATTEMPTS - loginAttempts));
        }
    }

    private boolean authenticate(String user, String pass) {
        String query = "SELECT password FROM users WHERE username = ? AND role = 'Admin'";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, user);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("password").equals(pass);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private void loadDashboard(String fxmlFile, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle(title);
            
            // --- UPDATED: Maintain Full Window State during transition ---
            stage.getScene().setRoot(root);
            stage.setMaximized(true);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    @FXML public void handleUseDemoAccount() {
        usernameField.setText("admin");
        passwordField.setText("Admin235");
    }
}