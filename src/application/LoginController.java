package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

public class LoginController {

    // --- Login Pane Components ---
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private VBox contentArea; // Connect this to your right-side VBox in Login.fxml

    // --- Register Pane Components ---
    @FXML private TextField regUsernameField; 
    @FXML private PasswordField regPasswordField;
    @FXML private PasswordField confirmPasswordField;

    // --- LOGIN LOGIC ---
    @FXML
    public void handleLogin() {
        System.out.println("Login check initiated...");
        String user = usernameField.getText();
        String pass = passwordField.getText();

        if (user.equals("admin") && pass.equals("1234")) {
            System.out.println("Login Success! Opening Dashboard...");
        } else {
            System.out.println("Invalid Username or Password.");
        }
    }

    // --- SWITCHING TO REGISTER PANE ---
    @FXML
    public void handleRegister() {
        try {
            // Load the Register UI
            Parent registerUI = FXMLLoader.load(getClass().getResource("Register.fxml"));
            
            // Remove everything currently in the right-side VBox
            contentArea.getChildren().clear();
            
            // Add the Register fields into that same VBox
            contentArea.getChildren().add(registerUI);
            
        } catch (Exception e) {
            System.out.println("Error: Could not load Register.fxml inside the pane.");
            e.printStackTrace();
        }
    }

    // --- REGISTER LOGIC ---
    @FXML
    public void handleRegisterSubmit() {
        String user = regUsernameField.getText();
        String pass = regPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            System.out.println("Error: Fields cannot be empty!");
        } else if (!pass.equals(confirm)) {
            System.out.println("Error: Passwords do not match!");
        } else {
            System.out.println("Registration Successful for: " + user);
        }
    }

    // --- CANCEL / GO BACK ---
    @FXML
    public void handleCancel(ActionEvent event) {
        try {
            // 1. Load the original Login.fxml again
            Parent loginView = FXMLLoader.load(getClass().getResource("Login.fxml"));
            
            // 2. Get the current Stage (Window) from the button click
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            
            // 3. Swap the scene back to the Login view
            // This will bring back the original "Sign In" fields on the right
            stage.getScene().setRoot(loginView);
            
            System.out.println("Returned to Login screen successfully!");
            
        } catch (Exception e) {
            System.out.println("Error returning to Login screen.");
            e.printStackTrace();
        }
    }
}