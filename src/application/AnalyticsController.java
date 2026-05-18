package application;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AnalyticsController {

    // Main Financial KPIs
    @FXML private Label revenueLabel, cogsLabel, netProfitLabel, ordersLabel, avgOrderLabel, topProductLabel;
    @FXML private Label revTrendLabel, cogsTrendLabel, netTrendLabel, ordersTrendLabel, aovTrendLabel, topProductDescLabel;
    
    @FXML private ComboBox<String> timeFilterCombo;
    
    // Main Charts
    @FXML private BarChart<String, Number> financialBreakdownChart; 
    @FXML private PieChart cogsPieChart; 
    @FXML private AreaChart<String, Number> revenueAreaChart; 
    @FXML private BarChart<String, Number> hourlyBarChart;
    @FXML private BarChart<String, Number> topProductsBarChart;
    @FXML private PieChart categoryRevPieChart;
    @FXML private BarChart<String, Number> cashierBarChart;
    @FXML private PieChart orderTypePieChart;

    // --- NEW: Category Deep Dive Section Elements ---
    @FXML private ComboBox<String> categoryDrillDownCombo;
    @FXML private BarChart<String, Number> categoryProductsChart;
    @FXML private Label categoryDrillDownSummary;
    // The 4 mini-KPI labels
    @FXML private Label catGrossLabel, catCogsLabel, catNetLabel, catUnitsLabel;

    // Chart Summary Labels
    @FXML private Label financialChartSummary, cogsChartSummary;
    @FXML private Label revChartSummary, hourlyChartSummary, topProdChartSummary;
    @FXML private Label catChartSummary, cashierChartSummary, orderTypeChartSummary;

    private static final String DB_URL = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0";
    private static final String DB_USER = "postgres.gwjmqejllljupondbzbs";
    private static final String DB_PASS = "Loritastecafe2026";

    @FXML
    public void initialize() {
        timeFilterCombo.getItems().addAll("Today", "This Week", "This Month", "This Year");
        timeFilterCombo.getSelectionModel().select("This Month");
        
        timeFilterCombo.setOnAction(e -> {
            loadKPIs();
            loadAllCharts();
        });

        loadCategoriesForDrillDown();
        
        loadKPIs();
        loadAllCharts();
    }

    private void loadCategoriesForDrillDown() {
        ObservableList<String> categories = FXCollections.observableArrayList("All Categories");
        String sql = "SELECT DISTINCT category FROM products WHERE category IS NOT NULL ORDER BY category";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        
        categoryDrillDownCombo.setItems(categories);
        categoryDrillDownCombo.getSelectionModel().select("All Categories");
        
        // Trigger the specific chart update when the combo box changes
        categoryDrillDownCombo.setOnAction(e -> loadCategoryDrillDownChart());
    }

    private void loadKPIs() {
        String filter = timeFilterCombo.getValue();
        String currentCond = "DATE(order_date) = CURRENT_DATE";
        String prevCond = "DATE(order_date) = CURRENT_DATE - INTERVAL '1 day'";
        String prevPeriodName = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));

        LocalDate today = LocalDate.now();
        if ("This Week".equals(filter)) {
            currentCond = "order_date >= date_trunc('week', CURRENT_DATE)";
            prevCond = "order_date >= date_trunc('week', CURRENT_DATE - INTERVAL '1 week') AND order_date < date_trunc('week', CURRENT_DATE)";
            prevPeriodName = "Week of " + today.minusWeeks(1).format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        } else if ("This Month".equals(filter)) {
            currentCond = "order_date >= date_trunc('month', CURRENT_DATE)";
            prevCond = "order_date >= date_trunc('month', CURRENT_DATE - INTERVAL '1 month') AND order_date < date_trunc('month', CURRENT_DATE)";
            prevPeriodName = today.minusMonths(1).format(DateTimeFormatter.ofPattern("MMMM yyyy")); 
        } else if ("This Year".equals(filter)) {
            currentCond = "order_date >= date_trunc('year', CURRENT_DATE)";
            prevCond = "order_date >= date_trunc('year', CURRENT_DATE - INTERVAL '1 year') AND order_date < date_trunc('year', CURRENT_DATE)";
            prevPeriodName = today.minusYears(1).format(DateTimeFormatter.ofPattern("yyyy")); 
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            double revCur = 0; int ordCur = 0; double cogsCur = 0;
            String curSql = "SELECT COALESCE(SUM(total_amount), 0) as rev, COUNT(id) as ord FROM transactions WHERE status != 'Voided' AND " + currentCond;
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(curSql)) {
                if(rs.next()) { revCur = rs.getDouble("rev"); ordCur = rs.getInt("ord"); }
            }
            String curCogsSql = "SELECT COALESCE(SUM(ti.quantity * p.cost_price), 0) as cogs FROM transaction_items ti JOIN transactions t ON ti.transaction_id = t.id JOIN products p ON ti.product_id = p.id WHERE t.status != 'Voided' AND " + currentCond.replace("order_date", "t.order_date");
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(curCogsSql)) {
                if(rs.next()) { cogsCur = rs.getDouble("cogs"); }
            }

            double revPrev = 0; int ordPrev = 0; double cogsPrev = 0;
            String prevSql = "SELECT COALESCE(SUM(total_amount), 0) as rev, COUNT(id) as ord FROM transactions WHERE status != 'Voided' AND " + prevCond;
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(prevSql)) {
                if(rs.next()) { revPrev = rs.getDouble("rev"); ordPrev = rs.getInt("ord"); }
            }
            String prevCogsSql = "SELECT COALESCE(SUM(ti.quantity * p.cost_price), 0) as cogs FROM transaction_items ti JOIN transactions t ON ti.transaction_id = t.id JOIN products p ON ti.product_id = p.id WHERE t.status != 'Voided' AND " + prevCond.replace("order_date", "t.order_date");
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(prevCogsSql)) {
                if(rs.next()) { cogsPrev = rs.getDouble("cogs"); }
            }

            String topSql = "SELECT p.name, SUM(ti.quantity) as qty FROM transaction_items ti JOIN products p ON ti.product_id = p.id JOIN transactions t ON ti.transaction_id = t.id WHERE t.status != 'Voided' AND " + currentCond.replace("order_date", "t.order_date") + " GROUP BY p.name ORDER BY qty DESC LIMIT 1";
            String topProd = "No Sales"; int topQty = 0;
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(topSql)) {
                if(rs.next()) { topProd = rs.getString("name"); topQty = rs.getInt("qty"); }
            }

            double netCur = revCur - cogsCur;
            double netPrev = revPrev - cogsPrev;
            double aovCur = ordCur > 0 ? revCur / ordCur : 0;
            double aovPrev = ordPrev > 0 ? revPrev / ordPrev : 0;
            
            revenueLabel.setText("₱" + String.format("%,.2f", revCur));
            cogsLabel.setText("- ₱" + String.format("%,.2f", cogsCur));
            netProfitLabel.setText("₱" + String.format("%,.2f", netCur));
            
            ordersLabel.setText(String.valueOf(ordCur));
            avgOrderLabel.setText("₱" + String.format("%,.2f", aovCur));
            topProductLabel.setText(topProd);
            topProductDescLabel.setText(topQty + " units sold (" + filter.toLowerCase() + ")");

            updateTrendLabel(revTrendLabel, revPrev == 0 ? (revCur>0?100:0) : ((revCur - revPrev) / revPrev) * 100, prevPeriodName, "₱" + String.format("%,.2f", revPrev));
            double cogsGrowth = cogsPrev == 0 ? (cogsCur>0?100:0) : ((cogsCur - cogsPrev) / cogsPrev) * 100;
            cogsTrendLabel.setText(String.format("%s %.1f%% vs %s", cogsGrowth >= 0 ? "↗" : "↘", Math.abs(cogsGrowth), prevPeriodName));
            cogsTrendLabel.setStyle(cogsGrowth > 0 ? "-fx-text-fill: #ef4444;" : "-fx-text-fill: #10b981;"); 
            updateTrendLabel(netTrendLabel, netPrev == 0 ? (netCur>0?100:0) : ((netCur - netPrev) / netPrev) * 100, prevPeriodName, "₱" + String.format("%,.2f", netPrev));
            updateTrendLabel(ordersTrendLabel, ordPrev == 0 ? (ordCur>0?100:0) : ((ordCur - ordPrev) / (double)ordPrev) * 100, prevPeriodName, String.valueOf(ordPrev));
            updateTrendLabel(aovTrendLabel, aovPrev == 0 ? (aovCur>0?100:0) : ((aovCur - aovPrev) / aovPrev) * 100, prevPeriodName, "₱" + String.format("%,.2f", aovPrev));

            applyTooltip(revenueLabel, "Metric: Gross Sales\nSource: 'transactions' table\nFormula: SUM(total_amount)");
            applyTooltip(cogsLabel, "Metric: Cost of Goods Sold\nSource: 'transaction_items' JOIN 'products'\nFormula: SUM(qty * cost_price)");
            applyTooltip(netProfitLabel, "Metric: Net Profit\nSource: Computed locally\nFormula: Gross Sales - COGS");

        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updateTrendLabel(Label lbl, double growth, String periodName, String exactValue) {
        lbl.setText(String.format("%s %.1f%% vs %s (%s)", growth >= 0 ? "↗" : "↘", Math.abs(growth), periodName, exactValue));
        lbl.setStyle(growth >= 0 ? "-fx-text-fill: #10b981; -fx-font-weight: bold;" : "-fx-text-fill: #ef4444; -fx-font-weight: bold;");
    }

    private void applyTooltip(Label targetLabel, String info) {
        Tooltip tooltip = new Tooltip(info);
        tooltip.setStyle("-fx-font-size: 13px; -fx-background-color: #1f2937; -fx-text-fill: white; -fx-padding: 10px;");
        tooltip.setShowDelay(Duration.millis(100)); 
        targetLabel.setTooltip(tooltip);
    }

    private void loadAllCharts() {
        revenueAreaChart.getData().clear(); hourlyBarChart.getData().clear();
        topProductsBarChart.getData().clear(); categoryRevPieChart.getData().clear();
        cashierBarChart.getData().clear(); orderTypePieChart.getData().clear();
        financialBreakdownChart.getData().clear(); cogsPieChart.getData().clear();

        String filter = timeFilterCombo.getValue();
        String periodCond = "DATE(order_date) = CURRENT_DATE";
        String groupSql = "TO_CHAR(order_date, 'HH12:00 AM')"; 
        String orderBy = "EXTRACT(HOUR FROM order_date)";

        if ("This Week".equals(filter) || "This Month".equals(filter)) {
            periodCond = "order_date >= date_trunc('" + (filter.contains("Week") ? "week" : "month") + "', CURRENT_DATE)";
            groupSql = "TO_CHAR(order_date, 'Mon DD, YYYY')"; 
            orderBy = "DATE(order_date)";
        } else if ("This Year".equals(filter)) {
            periodCond = "order_date >= date_trunc('year', CURRENT_DATE)";
            groupSql = "TO_CHAR(order_date, 'Month YYYY')"; 
            orderBy = "EXTRACT(MONTH FROM order_date)"; 
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            
            // Financial Flow Data
            double revCur = 0; double cogsCur = 0;
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COALESCE(SUM(total_amount), 0) as rev FROM transactions WHERE status != 'Voided' AND " + periodCond)) {
                if(rs.next()) { revCur = rs.getDouble("rev"); }
            }
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COALESCE(SUM(ti.quantity * p.cost_price), 0) as cogs FROM transaction_items ti JOIN transactions t ON ti.transaction_id = t.id JOIN products p ON ti.product_id = p.id WHERE t.status != 'Voided' AND " + periodCond.replace("order_date", "t.order_date"))) {
                if(rs.next()) { cogsCur = rs.getDouble("cogs"); }
            }
            double netCur = revCur - cogsCur;

            XYChart.Series<String, Number> finSeries = new XYChart.Series<>();
            finSeries.setName("Financial Flow (₱)");
            finSeries.getData().add(new XYChart.Data<>("1. Gross Sales", revCur));
            finSeries.getData().add(new XYChart.Data<>("2. (-) COGS", cogsCur));
            finSeries.getData().add(new XYChart.Data<>("3. (=) Net Profit", netCur));
            financialBreakdownChart.getData().add(finSeries);
            
            double profitMargin = revCur > 0 ? (netCur / revCur) * 100 : 0;
            financialChartSummary.setText(String.format("📊 Data: Gross Sales (₱%,.2f) - COGS (₱%,.2f) = Net Profit (₱%,.2f).\n💡 Analysis: The Net Profit Margin is %.1f%%, indicating overall financial health.", revCur, cogsCur, netCur, profitMargin));

            // COGS Pie Chart
            ObservableList<PieChart.Data> cogsData = FXCollections.observableArrayList();
            String cogsDistSql = "SELECT p.category, SUM(ti.quantity * p.cost_price) as category_cost FROM transaction_items ti JOIN products p ON ti.product_id = p.id JOIN transactions t ON ti.transaction_id = t.id WHERE t.status != 'Voided' AND " + periodCond.replace("order_date", "t.order_date") + " GROUP BY p.category ORDER BY category_cost DESC";
            String topCostCat = "N/A"; double topCostVal = 0; boolean firstCost = true;
            try(Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(cogsDistSql)) {
                while(rs.next()) {
                    String catName = rs.getString("category") != null ? rs.getString("category") : "Other";
                    double catCost = rs.getDouble("category_cost");
                    if(firstCost) { topCostCat = catName; topCostVal = catCost; firstCost = false; }
                    cogsData.add(new PieChart.Data(catName, catCost));
                }
                cogsPieChart.setData(cogsData);
                cogsChartSummary.setText(String.format("📊 Data: '%s' is the highest cost category at ₱%,.2f.\n💡 Analysis: Shows which menu sector consumes the largest portion of your budget to produce.", topCostCat, topCostVal));
            }

            // Area Chart: Revenue AND Net Profit
            final XYChart.Series<String, Number> revSeries = new XYChart.Series<>(); revSeries.setName("Gross Sales (₱)");
            final XYChart.Series<String, Number> netSeries = new XYChart.Series<>(); netSeries.setName("Net Profit (₱)");
            
            String lineSql = "SELECT " + groupSql + " as time_group, SUM(t.total_amount) as rev, COALESCE(SUM(t.total_amount) - SUM(ti.quantity * p.cost_price), SUM(t.total_amount)) as net FROM transactions t LEFT JOIN transaction_items ti ON t.id = ti.transaction_id LEFT JOIN products p ON ti.product_id = p.id WHERE t.status != 'Voided' AND " + periodCond.replace("order_date", "t.order_date") + " GROUP BY " + groupSql + ", " + orderBy + " ORDER BY " + orderBy + " ASC";
            double maxRev = 0; String maxRevDate = "N/A";
            try(Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(lineSql)) {
                while(rs.next()) {
                    double currentRev = rs.getDouble("rev");
                    double currentNet = rs.getDouble("net");
                    String currentDate = rs.getString("time_group");
                    revSeries.getData().add(new XYChart.Data<>(currentDate, currentRev));
                    netSeries.getData().add(new XYChart.Data<>(currentDate, currentNet));
                    if(currentRev > maxRev) { maxRev = currentRev; maxRevDate = currentDate; }
                }
                revenueAreaChart.getData().addAll(revSeries, netSeries);
                revChartSummary.setText(String.format("📊 Data: Highest earning period was %s (₱%,.2f).\n💡 Analysis: Comparing Sales against Net Profit reveals if margins stay consistent during busy times.", maxRevDate, maxRev));
            }

            // Hourly Bar Chart
            XYChart.Series<String, Number> hourSeries = new XYChart.Series<>(); hourSeries.setName("Orders per Hour");
            String hourSql = "SELECT EXTRACT(HOUR FROM order_date) as hr, COUNT(id) as ord FROM transactions WHERE status != 'Voided' AND " + periodCond + " GROUP BY hr ORDER BY hr ASC";
            int peakOrders = 0; String peakHour = "N/A";
            try(Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(hourSql)) {
                while(rs.next()) {
                    int h = rs.getInt("hr");
                    int ords = rs.getInt("ord");
                    String timeLabel = (h % 12 == 0 ? 12 : h % 12) + (h >= 12 ? " PM" : " AM");
                    hourSeries.getData().add(new XYChart.Data<>(timeLabel, ords));
                    if(ords > peakOrders) { peakOrders = ords; peakHour = timeLabel; }
                }
                hourlyBarChart.getData().add(hourSeries);
                hourlyChartSummary.setText(String.format("📊 Data: Busiest hour is %s with %d orders.\n💡 Analysis: Identifies operational traffic to help optimize staff breaks.", peakHour, peakOrders));
            }

            // Top Products (Top 5)
            final XYChart.Series<String, Number> prodRevSeries = new XYChart.Series<>(); prodRevSeries.setName("Revenue Generated (₱)");
            final XYChart.Series<String, Number> prodCostSeries = new XYChart.Series<>(); prodCostSeries.setName("Cost to Produce (₱)");
            
            String prodSql = "SELECT p.name, SUM(ti.subtotal) as rev, SUM(ti.quantity * p.cost_price) as cogs FROM transaction_items ti JOIN products p ON ti.product_id = p.id JOIN transactions t ON ti.transaction_id = t.id WHERE t.status != 'Voided' AND " + periodCond.replace("order_date", "t.order_date") + " GROUP BY p.name ORDER BY rev DESC LIMIT 5";
            String topItem = "N/A"; double topItemRev = 0; double topItemCogs = 0; boolean first = true;
            try(Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(prodSql)) {
                while(rs.next()) {
                    String name = rs.getString("name");
                    double pRev = rs.getDouble("rev");
                    double pCogs = rs.getDouble("cogs");
                    if(first) { topItem = name; topItemRev = pRev; topItemCogs = pCogs; first = false; }
                    prodRevSeries.getData().add(new XYChart.Data<>(name, pRev));
                    prodCostSeries.getData().add(new XYChart.Data<>(name, pCogs));
                }
                topProductsBarChart.getData().addAll(prodRevSeries, prodCostSeries);
                topProdChartSummary.setText(String.format("📊 Data: '%s' generated ₱%,.2f in revenue.\n💡 Analysis: Look for items with the largest gap between Revenue and Cost bars—those are your most profitable.", topItem, topItemRev));
            }

            // Category Pie
            ObservableList<PieChart.Data> catPieData = FXCollections.observableArrayList();
            String catSql = "SELECT p.category, SUM(ti.subtotal) as rev FROM transaction_items ti JOIN products p ON ti.product_id = p.id JOIN transactions t ON ti.transaction_id = t.id WHERE t.status != 'Voided' AND " + periodCond.replace("order_date", "t.order_date") + " GROUP BY p.category ORDER BY rev DESC";
            String topCat = "N/A"; double topCatRev = 0; first = true;
            try(Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(catSql)) {
                while(rs.next()) {
                    String catName = rs.getString("category") != null ? rs.getString("category") : "Other";
                    double catRev = rs.getDouble("rev");
                    if(first) { topCat = catName; topCatRev = catRev; first = false; }
                    catPieData.add(new PieChart.Data(catName, catRev));
                }
                categoryRevPieChart.setData(catPieData);
                catChartSummary.setText(String.format("📊 Data: '%s' generated highest revenue at ₱%,.2f.\n💡 Analysis: Shows which category drives the most sales.", topCat, topCatRev));
            }

            // Cashier Performance
            final XYChart.Series<String, Number> cashRevSeries = new XYChart.Series<>(); cashRevSeries.setName("Total Revenue (₱)");
            final XYChart.Series<String, Number> cashAovSeries = new XYChart.Series<>(); cashAovSeries.setName("Avg Order Value (₱)");
            
            String cashSql = "SELECT cashier_name, SUM(total_amount) as rev, (SUM(total_amount) / COUNT(id)) as aov FROM transactions WHERE status != 'Voided' AND " + periodCond + " GROUP BY cashier_name ORDER BY rev DESC LIMIT 5";
            String topCashier = "N/A"; double topCashierRev = 0; double topCashierAov = 0; first = true;
            try(Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(cashSql)) {
                while(rs.next()) {
                    String cName = rs.getString("cashier_name") != null ? rs.getString("cashier_name") : "System";
                    double cRev = rs.getDouble("rev");
                    double cAov = rs.getDouble("aov");
                    if(first) { topCashier = cName; topCashierRev = cRev; topCashierAov = cAov; first = false; }
                    cashRevSeries.getData().add(new XYChart.Data<>(cName, cRev));
                    cashAovSeries.getData().add(new XYChart.Data<>(cName, cAov));
                }
                cashierBarChart.getData().addAll(cashRevSeries, cashAovSeries);
                cashierChartSummary.setText(String.format("📊 Data: %s processed ₱%,.2f (AOV: ₱%,.2f).\n💡 Analysis: Compare Total Revenue against AOV to see who successfully upsells.", topCashier, topCashierRev, topCashierAov));
            }

            // Order Types
            ObservableList<PieChart.Data> typePieData = FXCollections.observableArrayList();
            String typeSql = "SELECT order_type, COUNT(id) as ord, (SUM(total_amount) / COUNT(id)) as aov FROM transactions WHERE status != 'Voided' AND " + periodCond + " GROUP BY order_type ORDER BY ord DESC";
            String topType = "N/A"; int topTypeOrders = 0; double topTypeAov = 0; first = true;
            try(Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(typeSql)) {
                while(rs.next()) {
                    String tName = rs.getString("order_type") != null ? rs.getString("order_type") : "Walk-in";
                    int tOrders = rs.getInt("ord");
                    if(first) { topType = tName; topTypeOrders = tOrders; topTypeAov = rs.getDouble("aov"); first = false; }
                    typePieData.add(new PieChart.Data(tName, tOrders));
                }
                orderTypePieChart.setData(typePieData);
                orderTypeChartSummary.setText(String.format("📊 Data: '%s' orders account for %d transactions (AOV ₱%,.2f).\n💡 Analysis: Highlights customer dining preference and actual spend behavior.", topType, topTypeOrders, topTypeAov));
            }

            // FORCE FLAT COLORS 
            Platform.runLater(() -> {
                try {
                    String colorPrimary = "#1f2937";   // Deep Charcoal
                    String colorSecondary = "#d97706"; // Warm Caramel
                    String colorTertiary = "#be123c";  // Crimson

                    if (revSeries.getNode() != null && revSeries.getNode().lookup(".chart-series-line") != null) revSeries.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: " + colorPrimary + "; -fx-stroke-width: 3px;");
                    Node revArea = revSeries.getNode().lookup(".chart-series-area-fill"); if (revArea != null) revArea.setStyle("-fx-fill: rgba(31, 41, 55, 0.15);"); 
                    if (netSeries.getNode() != null && netSeries.getNode().lookup(".chart-series-line") != null) netSeries.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: " + colorSecondary + "; -fx-stroke-width: 3px;");
                    Node netArea = netSeries.getNode().lookup(".chart-series-area-fill"); if (netArea != null) netArea.setStyle("-fx-fill: rgba(217, 119, 6, 0.15);"); 
                    int lc=0; for(Node n:revenueAreaChart.lookupAll(".chart-legend-item-symbol")){ if(lc==0)n.setStyle("-fx-background-color: " + colorPrimary + ", white;"); if(lc==1)n.setStyle("-fx-background-color: " + colorSecondary + ", white;"); lc++; }

                    for(XYChart.Data<String,Number> d:prodRevSeries.getData()) if(d.getNode()!=null)d.getNode().setStyle("-fx-bar-fill:" + colorPrimary + ";");
                    for(XYChart.Data<String,Number> d:prodCostSeries.getData()) if(d.getNode()!=null)d.getNode().setStyle("-fx-bar-fill:" + colorTertiary + ";");
                    int pc=0; for(Node n:topProductsBarChart.lookupAll(".chart-legend-item-symbol")){ if(pc==0)n.setStyle("-fx-background-color:" + colorPrimary + ";"); if(pc==1)n.setStyle("-fx-background-color:" + colorTertiary + ";"); pc++; }
                    
                    for(XYChart.Data<String,Number> d:cashRevSeries.getData()) if(d.getNode()!=null)d.getNode().setStyle("-fx-bar-fill:" + colorPrimary + ";");
                    for(XYChart.Data<String,Number> d:cashAovSeries.getData()) if(d.getNode()!=null)d.getNode().setStyle("-fx-bar-fill:" + colorSecondary + ";");
                    int cc=0; for(Node n:cashierBarChart.lookupAll(".chart-legend-item-symbol")){ if(cc==0)n.setStyle("-fx-background-color:" + colorPrimary + ";"); if(cc==1)n.setStyle("-fx-background-color:" + colorSecondary + ";"); cc++; }
                    
                    for (Node n : hourlyBarChart.lookupAll(".default-color0.chart-bar")) n.setStyle("-fx-bar-fill: " + colorPrimary + ";");
                    for (Node n : hourlyBarChart.lookupAll(".chart-legend-item-symbol")) n.setStyle("-fx-background-color: " + colorPrimary + ";");
                    for (Node n : financialBreakdownChart.lookupAll(".default-color0.chart-bar")) n.setStyle("-fx-bar-fill: " + colorPrimary + ";");
                    for (Node n : financialBreakdownChart.lookupAll(".chart-legend-item-symbol")) n.setStyle("-fx-background-color: " + colorPrimary + ";");
                } catch (Exception ex) {}
            });
            
            // Generate the Deep-Dive Chart + KPI Cards
            loadCategoryDrillDownChart();

        } catch(SQLException e) { e.printStackTrace(); }
    }

    // --- UPDATED DEEP-DIVE LOGIC WITH MINI KPI CARDS ---
    private void loadCategoryDrillDownChart() {
        categoryProductsChart.getData().clear();
        
        String timeFilter = timeFilterCombo.getValue();
        String catFilter = categoryDrillDownCombo.getValue();
        if (catFilter == null) return;

        String periodCond = "DATE(t.order_date) = CURRENT_DATE";
        if ("This Week".equals(timeFilter)) periodCond = "t.order_date >= date_trunc('week', CURRENT_DATE)";
        else if ("This Month".equals(timeFilter)) periodCond = "t.order_date >= date_trunc('month', CURRENT_DATE)";
        else if ("This Year".equals(timeFilter)) periodCond = "t.order_date >= date_trunc('year', CURRENT_DATE)";

        String sql = "SELECT p.name, SUM(ti.subtotal) as rev, SUM(ti.quantity * p.cost_price) as cogs, SUM(ti.quantity) as qty FROM transaction_items ti JOIN products p ON ti.product_id = p.id JOIN transactions t ON ti.transaction_id = t.id WHERE t.status != 'Voided' AND " + periodCond;
        if (!"All Categories".equals(catFilter)) {
            sql += " AND p.category = '" + catFilter.replace("'", "''") + "'";
        }
        sql += " GROUP BY p.name ORDER BY rev DESC";

        final XYChart.Series<String, Number> drillRevSeries = new XYChart.Series<>(); drillRevSeries.setName("Revenue Generated (₱)");
        final XYChart.Series<String, Number> drillCostSeries = new XYChart.Series<>(); drillCostSeries.setName("Cost to Produce (₱)");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            
            double totalCatRev = 0; 
            double totalCatCogs = 0;
            int totalCatQty = 0;
            int productCount = 0;
            
            while (rs.next()) {
                String name = rs.getString("name");
                double rev = rs.getDouble("rev");
                double cogs = rs.getDouble("cogs");
                int qty = rs.getInt("qty");
                
                drillRevSeries.getData().add(new XYChart.Data<>(name, rev));
                drillCostSeries.getData().add(new XYChart.Data<>(name, cogs));
                
                totalCatRev += rev;
                totalCatCogs += cogs;
                totalCatQty += qty;
                productCount++;
            }
            categoryProductsChart.getData().addAll(drillRevSeries, drillCostSeries);
            
            // --- POPULATE THE NEW MINI-KPI CARDS ---
            double totalCatNet = totalCatRev - totalCatCogs;
            
            catGrossLabel.setText("₱" + String.format("%,.2f", totalCatRev));
            catCogsLabel.setText("₱" + String.format("%,.2f", totalCatCogs));
            catNetLabel.setText("₱" + String.format("%,.2f", totalCatNet));
            catUnitsLabel.setText(String.valueOf(totalCatQty));

            categoryDrillDownSummary.setText(String.format("📊 Data: Displaying %d products in '%s'.\n💡 Analysis: The mini-dashboard above isolates the exact financial health of this specific menu category. Compare the charcoal (revenue) and crimson (cost) bars to find your highest margin items.", productCount, catFilter));

            // Force Flat Colors for Drill-Down Chart
            Platform.runLater(() -> {
                try {
                    String colorPrimary = "#1f2937";   // Deep Charcoal 
                    String colorTertiary = "#be123c";  // Muted Crimson
                    
                    for(XYChart.Data<String,Number> d:drillRevSeries.getData()) if(d.getNode()!=null)d.getNode().setStyle("-fx-bar-fill:" + colorPrimary + ";");
                    for(XYChart.Data<String,Number> d:drillCostSeries.getData()) if(d.getNode()!=null)d.getNode().setStyle("-fx-bar-fill:" + colorTertiary + ";");
                    int dc=0; for(Node n:categoryProductsChart.lookupAll(".chart-legend-item-symbol")){ if(dc==0)n.setStyle("-fx-background-color:" + colorPrimary + ";"); if(dc==1)n.setStyle("-fx-background-color:" + colorTertiary + ";"); dc++; }
                } catch (Exception ex) {}
            });

        } catch (SQLException e) { e.printStackTrace(); }
    }
}