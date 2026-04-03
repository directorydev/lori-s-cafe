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
    @FXML private TextField emailResetField;

    @FXML
    public void handleResetSubmit() {
        String email = emailResetField.getText();
        if (email.isEmpty()) {
            System.out.println("Error: Please enter your email.");
        } else {
            System.out.println("Success: Reset link sent to " + email);
            // You could also call handleCancel(null) here to go back to login automatically
        }
    }
    @FXML
    public void handleForgotPassword() {
        try {
            Parent forgotUI = FXMLLoader.load(getClass().getResource("ForgotPassword.fxml"));
            fadeSwap(forgotUI); // Smooth transition!
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- LOGIN LOGIC ---
    @FXML
    public void handleLogin() {
        String user = usernameField.getText();
        String pass = passwordField.getText();

        if (user.equals("admin") && pass.equals("1234")) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("AdminDashboard.fxml"));
                Parent dashboard = loader.load();
                
                // 1. Get the current Stage
                Stage stage = (Stage) usernameField.getScene().getWindow();
                
                // 2. Set opacity to 0 before showing
                dashboard.setOpacity(0);
                
                Scene scene = new Scene(dashboard);
                stage.setScene(scene);
                
                // 3. Smoothly Maximize
                stage.setMaximized(true);
                stage.show();
                
                // 4. Create the Fade-In Animation
                javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(500), dashboard);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
                
                System.out.println("Smooth transition to Dashboard complete.");
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // --- SWITCHING TO REGISTER PANE ---
    @FXML
    public void handleRegister() {
        try {
            Parent registerUI = FXMLLoader.load(getClass().getResource("Register.fxml"));
            fadeSwap(registerUI); // Smooth transition!
        } catch (Exception e) {
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
    private void fadeSwap(Parent newUI) {
        // 1. Fade Out current content
        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), contentArea);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        
        fadeOut.setOnFinished(e -> {
            // 2. Swap the content once it's invisible
            contentArea.getChildren().clear();
            contentArea.getChildren().add(newUI);
            
            // Ensure the new UI fills the area (The "Squish" Fix)
            if (newUI instanceof javafx.scene.layout.Region) {
                ((javafx.scene.layout.Region) newUI).prefWidthProperty().bind(contentArea.widthProperty());
                ((javafx.scene.layout.Region) newUI).prefHeightProperty().bind(contentArea.heightProperty());
            }

            // 3. Fade In the new content
            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), contentArea);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        
        fadeOut.play();
    }
}