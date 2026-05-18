package application;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.*;

public class ReceiptModalController {

    @FXML private VBox rootContainer; // Injected top container for mouse drag listeners
    @FXML private Label lblOrderId;
    @FXML private Label lblDate;
    @FXML private Label lblCashier;
    @FXML private Label lblStatus;
    
    @FXML private VBox itemsContainer;
    
    @FXML private Label lblTotal;
    @FXML private Label lblPaymentMethod;
    @FXML private Label lblPaymentAmount;

    private static final String DB_URL = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0";
    private static final String DB_USER = "postgres.gwjmqejllljupondbzbs";
    private static final String DB_PASS = "Loritastecafe2026";

    // Track mouse coordinates for dragging window offsets
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        // Create drag listeners to make the custom modal window movable
        rootContainer.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        rootContainer.setOnMouseDragged(event -> {
            Stage stage = (Stage) rootContainer.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    @FXML
    public void handleClose() {
        // Find the active window stage scene context and close it safely
        Stage stage = (Stage) rootContainer.getScene().getWindow();
        stage.close();
    }

    public void initData(Order order) {
        lblOrderId.setText(String.valueOf(order.getOrderId()));
        lblDate.setText(order.getDate());
        lblCashier.setText(order.getCashierName());
        lblStatus.setText(order.getStatus());
        
        String formattedTotal = "₱" + String.format("%,.2f", order.getTotalAmount());
        lblTotal.setText(formattedTotal);
        lblPaymentMethod.setText("PAYMENT (" + order.getPaymentMethod() + ")");
        lblPaymentAmount.setText(formattedTotal);

        loadReceiptItems(order.getOrderId());
    }

    private void loadReceiptItems(int orderId) {
        itemsContainer.getChildren().clear();

        String sql = "SELECT ti.quantity, p.name, ti.subtotal " +
                     "FROM transaction_items ti " +
                     "JOIN products p ON ti.product_id = p.id " +
                     "WHERE ti.transaction_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int qty = rs.getInt("quantity");
                String name = rs.getString("name");
                double subtotal = rs.getDouble("subtotal");

                HBox itemLine = new HBox();
                
                Label lblQty = new Label(String.valueOf(qty));
                lblQty.setPrefWidth(40.0);
                lblQty.getStyleClass().add("receipt-text");

                Label lblName = new Label(name);
                lblName.setPrefWidth(200.0);
                lblName.setWrapText(true); 
                lblName.getStyleClass().add("receipt-text");

                Pane spacer = new Pane();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label lblAmount = new Label(String.format("%,.2f", subtotal));
                lblAmount.setAlignment(Pos.CENTER_RIGHT);
                lblAmount.setPrefWidth(100.0);
                lblAmount.getStyleClass().add("receipt-text");

                itemLine.getChildren().addAll(lblQty, lblName, spacer, lblAmount);
                itemsContainer.getChildren().add(itemLine);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Label errorLabel = new Label("Error loading items.");
            errorLabel.getStyleClass().add("receipt-text");
            itemsContainer.getChildren().add(errorLabel);
        }
    }
}