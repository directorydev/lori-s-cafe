package application;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.event.ActionEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DashboardController {

    @FXML
    private BorderPane mainContainer;

    @FXML
    private VBox dashboardContentArea; 
    
    // FXML fields for navigation components
    @FXML private ToggleButton dashboardToggle;
    @FXML private ToggleButton posToggle;
    @FXML private ToggleButton inventoryToggle;
    @FXML private ToggleButton ordersToggle;
    @FXML private ToggleButton analyticsToggle;
    @FXML private ToggleButton promotionsToggle;
    @FXML private ToggleButton settingsToggle;
    @FXML private ToggleGroup mainNavToggleGroup;

    @FXML private Label dateTimeLabel;

    // --- NEW: Cache to store the original Dashboard cards ---
    private List<Node> initialDashboardContent;

    /**
     * Called automatically when the FXML is loaded.
     */
    @FXML
    public void initialize() {
        startRealTimeClock();
        
        // Save the original dashboard content (the cards and banner)
        // so we can bring it back when the user clicks the "Dashboard" button.
        if (dashboardContentArea != null) {
            initialDashboardContent = new ArrayList<>(dashboardContentArea.getChildren());
        }
    }

    private void startRealTimeClock() {
        if (dateTimeLabel != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy  h:mm a");
            dateTimeLabel.setText(LocalDateTime.now().format(formatter));
            
            Timeline clock = new Timeline(
                new KeyFrame(Duration.ZERO, e -> {
                    dateTimeLabel.setText(LocalDateTime.now().format(formatter));
                }),
                new KeyFrame(Duration.seconds(1))
            );
            
            clock.setCycleCount(Animation.INDEFINITE); 
            clock.play();
        }
    }

    // Helper method to load a specific FXML file into the center area
    private void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent view = loader.load();
            
            view.setOpacity(0);
            dashboardContentArea.getChildren().setAll(view);
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), view);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
            
        } catch (Exception e) {
            System.out.println("Warning: FXML file not found - " + fxmlFile);
            // If the file doesn't exist yet, show the placeholder instead of breaking
            loadPlaceholder(fxmlFile.replace(".fxml", ""));
        }
    }

    // --- NEW: Helper method to show a placeholder for unfinished pages ---
    private void loadPlaceholder(String pageName) {
        Label label = new Label(pageName + " is under construction.");
        label.setStyle("-fx-font-size: 24px; -fx-text-fill: #9ca3af; -fx-font-weight: bold;");
        
        VBox placeholder = new VBox(label);
        placeholder.setAlignment(javafx.geometry.Pos.CENTER);
        placeholder.setPrefHeight(400); // Give it some height
        placeholder.setOpacity(0);
        
        dashboardContentArea.getChildren().setAll(placeholder);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), placeholder);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    // --- NAVIGATION ACTIONS ---
    
    @FXML
    public void showDashboard() {
        // Restore the original cards from memory
        if (dashboardContentArea != null && initialDashboardContent != null) {
            dashboardContentArea.getChildren().setAll(initialDashboardContent);
            
            // Add a smooth fade-in effect when returning to the dashboard
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), dashboardContentArea);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        }
    }

    @FXML
    public void showMenuManagement() {
        loadView("MenuManagement.fxml");
    }

    @FXML
    public void showInventory() {
        loadView("Inventory.fxml");
    }

    @FXML
    public void showOrders() {
        loadView("Orders.fxml");
    }
    
    @FXML
    public void handlePOS() {
        loadView("POS.fxml");
    }

    @FXML
    public void handleAnalytics() {
        loadView("Analytics.fxml");
    }

    @FXML
    public void handlePromotions() {
        loadView("Promotions.fxml");
    }

    @FXML
    public void handleSettings() {
        loadView("Settings.fxml");
    }
    
    // --- LOGOUT LOGIC ---
    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            Parent loginView = FXMLLoader.load(getClass().getResource("Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            stage.setScene(new Scene(loginView));
            stage.setTitle("Lori's Taste Cafe");
            
            stage.setMaximized(false); 
            stage.setMaximized(true);
            
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}