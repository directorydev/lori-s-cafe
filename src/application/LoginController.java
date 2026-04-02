package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    public void handleLogin() {
    	System.out.println("Hello! The button is working!");
        String user = usernameField.getText();
        String pass = passwordField.getText();

        // Temporary check for testing
        if (user.equals("admin") && pass.equals("1234")) {
            System.out.println("Login Success! Opening Dashboard...");
            // Later we will add code here to switch to the Main Cafe Screen
        } else {
            System.out.println("Invalid Username or Password.");
        }
    }

    @FXML
    public void handleRegister() {
        try {
            // Load the Register.fxml file
            Parent root = FXMLLoader.load(getClass().getResource("Register.fxml"));
            Stage stage = new Stage();
            
            // This makes the popup "Modal" (you can't click the login until it's closed)
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Lori's Taste Cafe - Create Account");
            stage.setScene(new Scene(root));
            stage.show();
            
        } catch (Exception e) {
            System.out.println("Error: Could not find Register.fxml. Create the file first!");
            e.printStackTrace();
        }
    }
}