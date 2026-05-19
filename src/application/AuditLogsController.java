package application;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.File;
import java.io.PrintWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AuditLogsController {

    @FXML private Label totalLogsLabel, totalSalesLabel, totalInventoryLabel;
    @FXML private ComboBox<String> moduleCombo, actionCombo, timeCombo;
    @FXML private Button refreshBtn, exportBtn;
    @FXML private TextField searchField;
    
    @FXML private TableView<AuditLog> logsTable;
    @FXML private TableColumn<AuditLog, String> colTraceId, colDate, colAction, colDetails, colUser, colRef;
    @FXML private TableColumn<AuditLog, String> colModule; 

    private static final String DB_URL = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require";
    private static final String DB_USER = "postgres.gwjmqejllljupondbzbs";
    private static final String DB_PASS = "Loritastecafe2026";

    private ObservableList<AuditLog> masterLogList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupFilters();
        setupTableColumns();
        
        // NEW: Completely disable row selection so the custom colors never disappear!
        logsTable.setSelectionModel(null);
        
        executeDatabaseRefresh(); 
    }

    private void setupFilters() {
        moduleCombo.getItems().addAll("All Modules", "Sales & Orders", "Inventory Movement");
        moduleCombo.getSelectionModel().selectFirst();
        
        updateActionCombo();

        moduleCombo.setOnAction(e -> {
            updateActionCombo();
            refreshData();
        });
        
        actionCombo.setOnAction(e -> refreshData());

        timeCombo.getItems().addAll("All Time", "Today", "This Week", "This Month");
        timeCombo.getSelectionModel().selectFirst();
        timeCombo.setOnAction(e -> refreshData());
    }

    // --- UPGRADED ACTION CATEGORIZER ---
    private void updateActionCombo() {
        actionCombo.getItems().clear();
        String selectedModule = moduleCombo.getValue();
        
        if ("Sales & Orders".equals(selectedModule)) {
            actionCombo.getItems().addAll("All Actions", "Completed", "Pending", "Voided");
        } else if ("Inventory Movement".equals(selectedModule)) {
            // Added Restocks/Additions so the admin can track manual inventory additions
            actionCombo.getItems().addAll("All Actions", "Deductions", "Returns / Voids", "Restocks / Additions");
        } else {
            actionCombo.getItems().addAll("All Actions");
        }
        
        actionCombo.getSelectionModel().selectFirst();
    }

    private void setupTableColumns() {
        colTraceId.setCellValueFactory(new PropertyValueFactory<>("traceId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("user"));
        colRef.setCellValueFactory(new PropertyValueFactory<>("referenceId"));
        
        colTraceId.setStyle("-fx-font-family: monospace; -fx-text-fill: #6b7280;"); 
        
        // Module Color Coding
        colModule.setCellValueFactory(new PropertyValueFactory<>("module"));
        colModule.setCellFactory(column -> new TableCell<AuditLog, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("Sales")) {
                        setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;"); 
                    } else if (item.equals("Inventory")) {
                        setStyle("-fx-text-fill: #d97706; -fx-font-weight: bold;"); 
                    } else {
                        setStyle("-fx-text-fill: #1f2937; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // --- UPGRADED: EVENT COLOR CODING LOGIC ---
        colAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colAction.setCellFactory(column -> new TableCell<AuditLog, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    String lowerItem = item.toLowerCase();
                    
                    if (lowerItem.contains("void") || lowerItem.contains("return")) {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;"); // Red (Bad/Reversals)
                    } else if (lowerItem.contains("completed") || lowerItem.contains("deduction")) {
                        setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;"); // Green (Success/Normal flow)
                    } else if (lowerItem.contains("pending")) {
                        setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;"); // Orange (Waiting)
                    } else if (lowerItem.contains("restock") || lowerItem.contains("add") || lowerItem.contains("adjust")) {
                        setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;"); // Blue (Manual Inventory Additions)
                    } else {
                        setStyle("-fx-text-fill: #374151; -fx-font-weight: normal;"); // Default Dark Grey
                    }
                }
            }
        });
    }

    @FXML
    public void refreshData() {
        if (refreshBtn != null) {
            refreshBtn.setText("⏳ Loading...");
            refreshBtn.setDisable(true);
        }

        PauseTransition pause = new PauseTransition(Duration.millis(500));
        pause.setOnFinished(event -> {
            executeDatabaseRefresh();
            if (refreshBtn != null) {
                refreshBtn.setText("↻ Refresh");
                refreshBtn.setDisable(false);
            }
        });
        pause.play();
    }

    private void executeDatabaseRefresh() {
        masterLogList.clear();

        int totalLogs = 0;
        int totalSales = 0;
        int totalInventory = 0;

        String timeFilter = timeCombo.getValue();
        String timeCond = "1=1";
        
        if ("Today".equals(timeFilter)) {
            timeCond = "DATE(log_date) = CURRENT_DATE";
        } else if ("This Week".equals(timeFilter)) {
            timeCond = "log_date >= date_trunc('week', CURRENT_DATE)";
        } else if ("This Month".equals(timeFilter)) {
            timeCond = "log_date >= date_trunc('month', CURRENT_DATE)";
        }

        String sql = "SELECT * FROM (" +
                     "SELECT 'INV-' || il.log_id AS trace_id, il.created_at AS log_date, 'Inventory' AS module, il.type AS action, " +
                     "CONCAT(rm.name, ' (', CASE WHEN il.change_amount > 0 THEN '+' ELSE '' END, il.change_amount, ' ', rm.unit, ')') AS details, " +
                     "COALESCE(u.first_name || ' ' || u.last_name, u.username, tr.cashier_name, 'System API') AS user_name, " +
                     "CASE WHEN (il.type ILIKE '%Sale%' OR il.type ILIKE '%Void%') AND il.reference_id IS NOT NULL THEN 'TXN-' || il.reference_id ELSE CAST(il.reference_id AS VARCHAR) END AS ref_id " +
                     "FROM inventory_logs il " +
                     "JOIN raw_materials rm ON il.raw_material_id = rm.id " +
                     "LEFT JOIN users u ON il.user_id = u.id " +
                     "LEFT JOIN transactions tr ON il.reference_id = tr.id " +
                     "UNION ALL " +
                     "SELECT 'TXN-' || t.id AS trace_id, t.order_date AS log_date, 'Sales' AS module, 'Order ' || t.status AS action, " +
                     "CONCAT('Total: ₱', t.total_amount, ' via ', COALESCE(t.payment_method, 'N/A')) AS details, " +
                     "COALESCE(t.cashier_name, 'Unknown') AS user_name, " +
                     "CAST(t.id AS VARCHAR) AS ref_id " +
                     "FROM transactions t" +
                     ") AS unified_logs WHERE " + timeCond;

        String moduleFilter = moduleCombo.getValue();
        String actionFilter = actionCombo.getValue();
        
        if ("Sales & Orders".equals(moduleFilter)) {
            sql += " AND module = 'Sales'";
            if ("Completed".equals(actionFilter)) sql += " AND action = 'Order Completed'";
            if ("Pending".equals(actionFilter)) sql += " AND action = 'Order Pending'";
            if ("Voided".equals(actionFilter)) sql += " AND action = 'Order Voided'";
        } else if ("Inventory Movement".equals(moduleFilter)) {
            sql += " AND module = 'Inventory'";
            if ("Deductions".equals(actionFilter)) sql += " AND action ILIKE '%Deduction%'";
            if ("Returns / Voids".equals(actionFilter)) sql += " AND (action ILIKE '%Return%' OR action ILIKE '%Void%')";
            if ("Restocks / Additions".equals(actionFilter)) sql += " AND (action ILIKE '%Restock%' OR action ILIKE '%Add%' OR action ILIKE '%Adjust%')";
        }

        sql += " ORDER BY log_date DESC";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String trace = rs.getString("trace_id");
                Timestamp date = rs.getTimestamp("log_date"); 
                String module = rs.getString("module");
                String action = rs.getString("action");
                String details = rs.getString("details");
                String user = rs.getString("user_name");
                String refId = rs.getString("ref_id");
                
                String formattedDate = date != null ? sdf.format(date) : "N/A";
                masterLogList.add(new AuditLog(trace, formattedDate, module, action, details, user, refId));
                
                totalLogs++;
                if ("Sales".equals(module)) totalSales++;
                if ("Inventory".equals(module)) totalInventory++;
            }

            totalLogsLabel.setText(String.valueOf(totalLogs));
            totalSalesLabel.setText(String.valueOf(totalSales));
            totalInventoryLabel.setText(String.valueOf(totalInventory));
            
            setupSearchFilter();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupSearchFilter() {
        FilteredList<AuditLog> filteredData = new FilteredList<>(masterLogList, b -> true);
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(log -> {
                if (newValue == null || newValue.isEmpty()) return true;
                
                String lowerCaseFilter = newValue.toLowerCase();
                
                if (log.getTraceId().toLowerCase().contains(lowerCaseFilter)) return true;
                if (log.getDetails().toLowerCase().contains(lowerCaseFilter)) return true;
                if (log.getAction().toLowerCase().contains(lowerCaseFilter)) return true;
                if (log.getUser().toLowerCase().contains(lowerCaseFilter)) return true;
                if (log.getReferenceId().toLowerCase().contains(lowerCaseFilter)) return true;
                
                return false; 
            });
        });
        
        SortedList<AuditLog> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(logsTable.comparatorProperty());
        logsTable.setItems(sortedData);
    }

    @FXML
    public void handleGenerateReport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Master Audit Report");
        
        String currentDate = new SimpleDateFormat("yyyyMMdd").format(new Date());
        fileChooser.setInitialFileName("Loris_Taste_Cafe_Master_Audit_" + currentDate + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        Window window = logsTable.getScene().getWindow();
        File file = fileChooser.showSaveDialog(window);

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("Trace ID,Date & Time,Module,Action / Event,Details,User / Auth,Reference ID");

                for (AuditLog log : masterLogList) {
                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                            log.getTraceId(), log.getDate(), log.getModule(), log.getAction(), 
                            log.getDetails(), log.getUser(), log.getReferenceId()
                    );
                }

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText("Master Audit Report Generated");
                alert.setContentText("Your global system logs have been successfully exported to CSV.");
                alert.showAndWait();

                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(file);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}