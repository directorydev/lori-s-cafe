package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {

	// SUPABASE CLOUD CONNECTION
	private static final String DB_URL = "jdbc:postgresql://db.gwjmqejlljupondbzbs.supabase.co:5432/postgres?sslmode=require";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "Loritastecafe2026"; // Delete this text and type your actual Supabase password; 

    // --- FXML UI COMPONENTS ---
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private VBox contentArea; 
    @FXML private RadioButton staffRadio;
    @FXML private RadioButton adminRadio;
    @FXML private ToggleGroup roleGroup;

    // --- DEMO UI COMPONENTS ---
    @FXML private VBox demoAccountsBox;
    @FXML private Label demoUserLabel;
    @FXML private Label demoPassLabel;
    @FXML private Button useDemoAccountButton;

    // --- REGISTER UI COMPONENTS ---
    @FXML private TextField regUsernameField; 
    @FXML private PasswordField regPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;

    // Temporary variables to hold the demo credentials
    private String currentDemoUser;
    private String currentDemoPass;

    @FXML
    public void initialize() {
        if (demoAccountsBox != null) {
            demoAccountsBox.setOpacity(0); 
        }

        if (roleGroup != null) {
            roleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
                if (staffRadio.isSelected()) {
                    demoUserLabel.setText("Cashier: staff1");
                    demoPassLabel.setText("Password: staff123");
                    currentDemoUser = "staff1";
                    currentDemoPass = "staff123";
                } else if (adminRadio.isSelected()) {
                    // Reverted back to admin123 to match your probable database state
                    demoUserLabel.setText("Admin: admin");
                    demoPassLabel.setText("Password: Admin235");
                    currentDemoUser = "admin";
                    currentDemoPass = "Admin235";
                }
                
                if (demoAccountsBox != null) {
                    javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), demoAccountsBox);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();
                }
            });
        }
        
        if (staffRadio != null) {
            staffRadio.setSelected(true); 
        }
    }

    private Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver not found!");
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void handleLogin() {
        // .trim() removes any accidental spaces copied into the fields
        String user = usernameField.getText().trim();
        String pass = passwordField.getText().trim(); 
        String selectedRole = adminRadio.isSelected() ? "ADMIN" : "STAFF";

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Info", "Please fill in all fields.");
            return;
        }

        if (authenticate(user, pass, selectedRole)) {
            if (selectedRole.equals("ADMIN")) {
                loadDashboard("AdminDashboard.fxml", "Admin Control - Lori's Taste Cafe");
            } else {
                loadDashboard("StaffDashboard.fxml", "POS Terminal - Lori's Taste Cafe");
            }
        } else {
            // The console will tell you exactly why this failed!
            showAlert(Alert.AlertType.ERROR, "Login Failed", "Incorrect username, password, or role. Check Eclipse Console for details.");
        }
    }

    /**
     * UPGRADED LEGIT AUTHENTICATION LOGIC
     * This checks the database and prints exact error reasons to your Eclipse console.
     */
    private boolean authenticate(String user, String pass, String role) {
        String query = "SELECT password, role FROM users WHERE username = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, user);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String dbPassword = rs.getString("password");
                    String dbRole = rs.getString("role");
                    
                    // 1. Check if password matches exactly
                    if (!dbPassword.equals(pass)) {
                        System.out.println("❌ LOGIN FAILED: Incorrect password.");
                        System.out.println("   -> Database expects password: '" + dbPassword + "'");
                        System.out.println("   -> You entered: '" + pass + "'");
                        return false;
                    }
                    
                    // 2. Check if the role matches
                    if (!dbRole.equalsIgnoreCase(role)) {
                        System.out.println("❌ LOGIN FAILED: Incorrect role selected.");
                        System.out.println("   -> Database says this user is: '" + dbRole + "'");
                        System.out.println("   -> You selected the radio button: '" + role + "'");
                        return false;
                    }
                    
                    System.out.println("✅ LOGIN SUCCESSFUL for user: " + user);
                    return true;
                    
                } else {
                    System.out.println("❌ LOGIN FAILED: Username '" + user + "' does not exist in the database.");
                    return false;
                }
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not connect to PostgreSQL.");
            e.printStackTrace();
            return false;
        }
    }

    @FXML
    public void handleUseDemoAccount() {
        usernameField.setText(currentDemoUser);
        passwordField.setText(currentDemoPass);
    }

    private void loadDashboard(String fxmlFile, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            
            stage.setMaximized(false);
            stage.setMaximized(true);
            
            stage.show();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Load Error", "The module " + fxmlFile + " could not be found.");
            e.printStackTrace();
        }
    }

    @FXML
    public void handleRegisterSubmit() {
        String fName = firstNameField.getText().trim();
        String user = regUsernameField.getText().trim();
        String pass = regPasswordField.getText().trim();
        String confirm = confirmPasswordField.getText().trim();

        if (user.isEmpty() || pass.isEmpty() || fName.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Registration Error", "All fields are required.");
            return;
        }

        if (!pass.equals(confirm)) {
            showAlert(Alert.AlertType.ERROR, "Match Error", "Passwords do not match!");
            return;
        }

        String insertQuery = "INSERT INTO users (username, password, role, first_name, last_name) VALUES (?, ?, 'STAFF', ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
            pstmt.setString(1, user);
            pstmt.setString(2, pass);
            pstmt.setString(3, fName);
            pstmt.setString(4, lastNameField.getText().trim());
            pstmt.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Account Created", "Staff account registered successfully.");
            handleCancel(new ActionEvent()); 
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Username might already exist.");
        }
    }

    @FXML 
    public void handleRegister() { 
        try { 
            fadeSwap(FXMLLoader.load(getClass().getResource("Register.fxml"))); 
        } catch (Exception e) { 
            e.printStackTrace(); 
        } 
    }
    
    @FXML 
    public void handleForgotPassword() { 
        showAlert(Alert.AlertType.INFORMATION, "Support", "Contact Management to reset your password."); 
    }
    
    @FXML 
    public void handleCancel(ActionEvent event) { 
        try { 
            Parent loginView = FXMLLoader.load(getClass().getResource("Login.fxml")); 
            Stage stage = (Stage) contentArea.getScene().getWindow(); 
            
            stage.setScene(new Scene(loginView));
            
            stage.setMaximized(false);
            stage.setMaximized(true);
            
        } catch (Exception e) { 
            e.printStackTrace(); 
        } 
    }

    private void fadeSwap(Parent newUI) {
        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), contentArea);
        fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            contentArea.getChildren().setAll(newUI);
            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), contentArea);
            fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0); fadeIn.play();
        });
        fadeOut.play();
    }
}