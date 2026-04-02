package application;
	
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;

public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
			// This line is the secret! It loads your Scene Builder design.
			Parent root = FXMLLoader.load(getClass().getResource("Login.fxml"));
			
			// We remove the (400, 400) size so it uses the size you set in Scene Builder
			Scene scene = new Scene(root);
			
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			
			primaryStage.setTitle("Lori's Taste Cafe - Login");
			primaryStage.setScene(scene);
			primaryStage.setResizable(false); // Keeps your nice layout looking perfect
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}