package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.Node;

public class DashboardController {

	@FXML
	public void handleLogout(ActionEvent event) {
	    try {
	        Parent loginView = FXMLLoader.load(getClass().getResource("Login.fxml"));
	        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

	        // 1. Reset the Window
	        stage.setMaximized(false);
	        stage.setResizable(false);
	        
	        // 2. Set the Scene (Matches your FXML prefWidth/Height)
	        Scene scene = new Scene(loginView, 600, 400);
	        stage.setScene(scene);

	        // 3. FORCE the SplitPane divider to stay at 30% (0.3)
	        // This stops the logo from being compressed
	        javafx.scene.control.SplitPane sp = (javafx.scene.control.SplitPane) loginView.lookup("SplitPane");
	        if (sp != null) {
	            sp.setDividerPositions(0.3);
	        }

	        stage.centerOnScreen();
	        stage.show();
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
}