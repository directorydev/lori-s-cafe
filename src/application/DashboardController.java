package application;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.event.ActionEvent;

public class DashboardController {

    @FXML
    private BorderPane mainContainer; // Ensure this matches the fx:id in Scene Builder

    // Helper method to swap the center view smoothly
    private void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent view = loader.load();
            
            // Start invisible for the fade effect
            view.setOpacity(0);
            mainContainer.setCenter(view);
            
            // Smooth Fade In
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), view);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error loading FXML: " + fxmlFile);
        }
    }

    @FXML
    public void showDashboard() {
        // You can create a DashboardHome.fxml later for stats
        // loadView("DashboardHome.fxml");
        System.out.println("Dashboard Home Loaded");
    }

    @FXML
    public void showMenuManagement() {
        loadView("MenuManagement.fxml");
    }

    @FXML
    public void showOrders() {
        // loadView("Orders.fxml");
        System.out.println("Orders View Loaded");
    }

    @FXML
    public void showInventory() {
        // loadView("Inventory.fxml");
        System.out.println("Inventory View Loaded");
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            Parent loginView = FXMLLoader.load(getClass().getResource("Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            // Reset window size for login
            stage.setMaximized(false);
            stage.setResizable(false);
            stage.setWidth(600);
            stage.setHeight(400);
            
            Scene scene = new Scene(loginView);
            stage.setScene(scene);
            stage.setTitle("Lori's Taste Cafe");
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}