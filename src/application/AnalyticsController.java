package application;

import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import java.sql.*;

public class AnalyticsController {

    @FXML private Label revenueLabel, ordersLabel, avgOrderLabel, topProductLabel;
    @FXML private ComboBox<String> timeFilterCombo;
    
    // Tab buttons
    @FXML private ToggleButton btnSalesTrends, btnProducts, btnCategories, btnPayments, btnInsights;
    
    // Chart Views
    @FXML private HBox viewSalesTrends, viewCategories;
    
    // Charts
    @FXML private LineChart<String, Number> dailySalesChart;
    @FXML private BarChart<String, Number> hourlyBarChart;
    @FXML private PieChart categoryPieChart;

 // SUPABASE CLOUD CONNECTION
 		// 1. Add 'jdbc:' to the start and use port 6543 for the pooler
 		private static final String DB_URL = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require";
 		private static final String DB_USER = "postgres.gwjmqejllljupondbzbs";
 		private static final String DB_PASS = "Loritastecafe2026";

    @FXML
    public void initialize() {
        timeFilterCombo.getItems().addAll("Today", "Yesterday", "Last 7 Days", "Monthly");
        timeFilterCombo.getSelectionModel().selectFirst();
        
        loadStatistics();
        loadCharts();
    }

    @FXML
    public void switchChartCategory() {
        // Hide all views first
        viewSalesTrends.setVisible(false);
        viewSalesTrends.setManaged(false);
        viewCategories.setVisible(false);
        viewCategories.setManaged(false);

        // Show the selected view
        if (btnSalesTrends.isSelected()) {
            viewSalesTrends.setVisible(true);
            viewSalesTrends.setManaged(true);
        } else if (btnCategories.isSelected()) {
            viewCategories.setVisible(true);
            viewCategories.setManaged(true);
        } else {
            // Placeholder: If they click Products, Payments, or Insights, 
            // you can create more HBox views and show them here.
            // For now, default back to sales trends.
            viewSalesTrends.setVisible(true);
            viewSalesTrends.setManaged(true);
        }
    }

    private void loadStatistics() {
        String sql = "SELECT SUM(total_price) as revenue, COUNT(id) as orders FROM sales WHERE sale_date = CURRENT_DATE";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                double rev = rs.getDouble("revenue");
                int ord = rs.getInt("orders");
                revenueLabel.setText("₱" + String.format("%,.2f", rev));
                ordersLabel.setText(String.valueOf(ord));
                avgOrderLabel.setText("₱" + String.format("%,.2f", ord > 0 ? rev / ord : 0));
                topProductLabel.setText("Caramel Macchiato"); // Replace with actual query
            }
        } catch (SQLException e) { 
            // Failsafe for UI preview if DB is not ready
            revenueLabel.setText("₱4,520.00");
            ordersLabel.setText("32");
            avgOrderLabel.setText("₱141.25");
            topProductLabel.setText("Espresso");
        }
    }

    private void loadCharts() {
        // 1. Line Chart Data (Smooth green line)
        XYChart.Series<String, Number> lineSeries = new XYChart.Series<>();
        lineSeries.setName("Revenue");
        lineSeries.getData().add(new XYChart.Data<>("May 7", 2500));
        lineSeries.getData().add(new XYChart.Data<>("May 8", 3100));
        lineSeries.getData().add(new XYChart.Data<>("May 9", 2800));
        lineSeries.getData().add(new XYChart.Data<>("May 10", 4200));
        lineSeries.getData().add(new XYChart.Data<>("May 11", 4520));
        dailySalesChart.getData().add(lineSeries);

        // 2. Bar Chart Data (Blue bars)
        XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
        barSeries.setName("Orders");
        barSeries.getData().add(new XYChart.Data<>("8:00 AM", 5));
        barSeries.getData().add(new XYChart.Data<>("10:00 AM", 12));
        barSeries.getData().add(new XYChart.Data<>("12:00 PM", 25));
        barSeries.getData().add(new XYChart.Data<>("3:00 PM", 18));
        barSeries.getData().add(new XYChart.Data<>("6:00 PM", 8));
        hourlyBarChart.getData().add(barSeries);

        // 3. Pie Chart Data
        categoryPieChart.getData().add(new PieChart.Data("Coffee", 45));
        categoryPieChart.getData().add(new PieChart.Data("Non-Coffee", 25));
        categoryPieChart.getData().add(new PieChart.Data("Pastries", 20)); 	
    }
}