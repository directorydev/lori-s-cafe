package application;
	
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("Login.fxml"));
            Scene scene = new Scene(root);
            
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/application/Lorislogo.jpg")));
            primaryStage.setTitle("Lori's Taste Cafe");
            primaryStage.setScene(scene);
            
            // --- UPDATED: Force Full Window State ---
            primaryStage.setMaximized(true);
            primaryStage.show();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }	
    public static void main(String[] args) {
        launch(args);
    }
}