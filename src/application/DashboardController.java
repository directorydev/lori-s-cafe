package application;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
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

    @FXML private BorderPane mainContainer;
    @FXML private VBox dashboardContentArea; 
    
    @FXML private ToggleButton dashboardToggle, posToggle, inventoryToggle, ordersToggle;
    @FXML private ToggleButton analyticsToggle, promotionsToggle, settingsToggle;
    @FXML private ToggleGroup mainNavToggleGroup;

    @FXML private Label dateTimeLabel;

    private List<Node> initialDashboardContent;

    @FXML
    public void initialize() {
        // --- SECURITY: Verify Session ---
        if (!UserSession.isActive()) {
            Platform.runLater(this::redirectToLogin);
            return;
        }

        startRealTimeClock();

        // Cache the original dashboard content (cards/banner)
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

    // --- FIX: Added missing showDashboard method to resolve LoadException ---
    @FXML
    public void showDashboard() {
        if (!UserSession.isActive()) {
            redirectToLogin();
            return;
        }

        if (dashboardContentArea != null && initialDashboardContent != null) {
            dashboardContentArea.getChildren().setAll(initialDashboardContent);
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), dashboardContentArea);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        }
    }

    private void redirectToLogin() {
        try {
            Parent loginView = FXMLLoader.load(getClass().getResource("Login.fxml"));
            Stage stage = (Stage) mainContainer.getScene().getWindow();
            
            // Maintain Full Window State during logout
            stage.getScene().setRoot(loginView);
            stage.setMaximized(true);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        UserSession.cleanSession();
        redirectToLogin();
    }

    // --- NAVIGATION LOGIC ---

    private void loadView(String fxmlFile) {
        if (!UserSession.isActive()) {
            redirectToLogin();
            return;
        }

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
            loadPlaceholder(fxmlFile.replace(".fxml", ""));
        }
    }

    private void loadPlaceholder(String pageName) {
        Label label = new Label(pageName + " is under construction.");
        label.setStyle("-fx-font-size: 24px; -fx-text-fill: #9ca3af; -fx-font-weight: bold;");
        VBox placeholder = new VBox(label);
        placeholder.setAlignment(javafx.geometry.Pos.CENTER);
        placeholder.setPrefHeight(400);
        placeholder.setOpacity(0);
        dashboardContentArea.getChildren().setAll(placeholder);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), placeholder);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    @FXML public void showInventory() { loadView("Inventory.fxml"); }
    @FXML public void showOrders() { loadView("Orders.fxml"); }
    @FXML public void handlePOS() { loadView("POS.fxml"); }
    @FXML public void handleAnalytics() { loadView("Analytics.fxml"); }
    @FXML public void handlePromotions() { loadView("Promotions.fxml"); }
    @FXML public void handleSettings() { loadView("Settings.fxml"); }
}