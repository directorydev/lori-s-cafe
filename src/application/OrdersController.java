package application;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.*;

public class OrdersController {

    @FXML private Label totalOrdersLabel, totalRevenueLabel, avgOrderValueLabel, completedOrdersLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusCombo, paymentCombo, timeCombo;
    
    @FXML private TableView<?> ordersTable; // Uses placeholder until you link an Order.java model
    @FXML private TableColumn<?, ?> colOrderId, colDate, colCustomer, colTotal, colPayment, colStatus, colAction;

 // SUPABASE CLOUD CONNECTION
 		// 1. Add 'jdbc:' to the start and use port 6543 for the pooler
 		private static final String DB_URL = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require";
 		private static final String DB_USER = "postgres.gwjmqejllljupondbzbs";
 		private static final String DB_PASS = "Loritastecafe2026";

    @FXML
    public void initialize() {
        setupFilters();
        
        // Setup Table Columns (Uncomment and replace 'Order' with your actual model class when ready)
        /*
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customer"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colPayment.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        */

        refreshData();
    }

    private void setupFilters() {
        statusCombo.getItems().addAll("All Status", "Completed", "Pending", "Cancelled");
        statusCombo.getSelectionModel().selectFirst();

        paymentCombo.getItems().addAll("All Payments", "Cash", "GCash");
        paymentCombo.getSelectionModel().selectFirst();

        timeCombo.getItems().addAll("All Time", "Today", "This Week", "This Month");
        timeCombo.getSelectionModel().selectFirst();
    }

    @FXML
    public void refreshData() {
        // Here you will fetch data from your 'sales' or 'orders' table in PostgreSQL
        // Because the database is likely empty right now, the TableView's <placeholder> 
        // will automatically show the "No orders found" UI!
        
        System.out.println("Refreshing order data...");
    }
}