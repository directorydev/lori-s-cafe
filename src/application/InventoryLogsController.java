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

public class InventoryLogsController {

    @FXML private Label totalLogsLabel, totalDeductionsLabel, totalReturnsLabel;
    @FXML private ComboBox<String> typeCombo, timeCombo;
    @FXML private Button refreshBtn, exportBtn;
    @FXML private TextField searchField; // NEW Live Search
    
    @FXML private TableView<InventoryLog> logsTable;
    @FXML private TableColumn<InventoryLog, Integer> colLogId;
    @FXML private TableColumn<InventoryLog, String> colDate, colUser, colMaterial, colType, colRef, colRemarks;
    @FXML private TableColumn<InventoryLog, InventoryLog> colAmount; // Bound to the whole object to access Unit

    private static final String DB_URL = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require";
    private static final String DB_USER = "postgres.gwjmqejllljupondbzbs";
    private static final String DB_PASS = "Loritastecafe2026";

    private ObservableList<InventoryLog> masterLogList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupFilters();
        setupTableColumns();
        executeDatabaseRefresh(); 
    }

    private void setupFilters() {
        typeCombo.getItems().addAll("All Log Types", "Sale Deduction", "Void / Return", "Manual Restock", "Adjustment");
        typeCombo.getSelectionModel().selectFirst();
        typeCombo.setOnAction(e -> refreshData());

        timeCombo.getItems().addAll("All Time", "Today", "This Week", "This Month");
        timeCombo.getSelectionModel().selectFirst();
        timeCombo.setOnAction(e -> refreshData());
    }

    private void setupTableColumns() {
        colLogId.setCellValueFactory(new PropertyValueFactory<>("logId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("user"));
        colMaterial.setCellValueFactory(new PropertyValueFactory<>("materialName"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colRef.setCellValueFactory(new PropertyValueFactory<>("referenceId"));
        colRemarks.setCellValueFactory(new PropertyValueFactory<>("remarks"));
        
        // Smart Cell Factory to combine Amount and Unit with Colors
        colAmount.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));
        colAmount.setCellFactory(tc -> new TableCell<InventoryLog, InventoryLog>() {
            @Override
            protected void updateItem(InventoryLog log, boolean empty) {
                super.updateItem(log, empty);
                if (empty || log == null) { 
                    setText(null); 
                    setStyle("");
                } else { 
                    double amt = log.getChangeAmount();
                    setText(String.format("%+.2f %s", amt, log.getUnit())); 
                    
                    if (amt < 0) {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;"); // Red
                    } else if (amt > 0) {
                        setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;"); // Green
                    } else {
                        setStyle("-fx-text-fill: #9ca3af; -fx-font-weight: normal;"); // Gray
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

        PauseTransition pause = new PauseTransition(Duration.millis(800));
        pause.setOnFinished(event -> {
            executeDatabaseRefresh();
            if (refreshBtn != null) {
                refreshBtn.setText("🔄 Refresh");
                refreshBtn.setDisable(false);
            }
        });
        pause.play();
    }

    private void executeDatabaseRefresh() {
        masterLogList.clear();

        int totalLogs = 0;
        int totalDeductions = 0;
        int totalReturns = 0;

        // ENTERPRISE SQL: Joins raw_materials for units, and users for accountability
        String sql = "SELECT il.log_id, il.created_at, rm.name AS material_name, rm.unit, " +
                     "il.change_amount, il.type, il.reference_id, il.remarks, u.first_name, u.last_name " +
                     "FROM inventory_logs il " +
                     "JOIN raw_materials rm ON il.raw_material_id = rm.id " +
                     "LEFT JOIN users u ON il.user_id = u.id WHERE 1=1";
        
        String typeFilter = typeCombo.getValue();
        if (!"All Log Types".equals(typeFilter)) {
            sql += " AND il.type ILIKE '%" + typeFilter + "%'";
        }

        String timeFilter = timeCombo.getValue();
        if ("Today".equals(timeFilter)) {
            sql += " AND DATE(il.created_at) = CURRENT_DATE";
        } else if ("This Week".equals(timeFilter)) {
            sql += " AND il.created_at >= date_trunc('week', CURRENT_DATE)";
        } else if ("This Month".equals(timeFilter)) {
            sql += " AND il.created_at >= date_trunc('month', CURRENT_DATE)";
        }

        sql += " ORDER BY il.created_at DESC";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("log_id");
                Timestamp date = rs.getTimestamp("created_at"); 
                String material = rs.getString("material_name");
                String unit = rs.getString("unit");
                double amount = rs.getDouble("change_amount");
                String type = rs.getString("type");
                String refId = rs.getString("reference_id");
                String remarks = rs.getString("remarks");
                
                String fName = rs.getString("first_name");
                String lName = rs.getString("last_name");
                String user = (fName != null && lName != null) ? fName + " " + lName : "System";
                
                String formattedDate = date != null ? sdf.format(date) : "N/A";
                masterLogList.add(new InventoryLog(id, formattedDate, material, amount, unit, type, user, refId, remarks));
                
                totalLogs++;
                if (amount < 0) totalDeductions++;
                if (amount > 0 && (type.toLowerCase().contains("return") || type.toLowerCase().contains("void"))) totalReturns++;
            }

            totalLogsLabel.setText(String.valueOf(totalLogs));
            totalDeductionsLabel.setText(String.valueOf(totalDeductions));
            totalReturnsLabel.setText(String.valueOf(totalReturns));
            
            // Attach the search logic after data is loaded
            setupSearchFilter();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupSearchFilter() {
        FilteredList<InventoryLog> filteredData = new FilteredList<>(masterLogList, b -> true);
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(log -> {
                if (newValue == null || newValue.isEmpty()) return true;
                
                String lowerCaseFilter = newValue.toLowerCase();
                
                if (log.getMaterialName().toLowerCase().contains(lowerCaseFilter)) return true;
                if (log.getUser().toLowerCase().contains(lowerCaseFilter)) return true;
                if (log.getReferenceId().toLowerCase().contains(lowerCaseFilter)) return true;
                if (log.getRemarks().toLowerCase().contains(lowerCaseFilter)) return true;
                
                return false; 
            });
        });
        
        SortedList<InventoryLog> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(logsTable.comparatorProperty());
        logsTable.setItems(sortedData);
    }

    @FXML
    public void handleExportData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Inventory Audit Report");
        
        String currentDate = new SimpleDateFormat("yyyyMMdd").format(new Date());
        fileChooser.setInitialFileName("Loris_Taste_Cafe_Audit_Logs_" + currentDate + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        Window window = logsTable.getScene().getWindow();
        File file = fileChooser.showSaveDialog(window);

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                // Enterprise CSV Header
                writer.println("Log ID,Date & Time,User,Raw Material,Amount Changed,Unit,Movement Type,Reference ID,Remarks");

                for (InventoryLog log : masterLogList) {
                    writer.printf("%d,\"%s\",\"%s\",\"%s\",%.2f,\"%s\",\"%s\",\"%s\",\"%s\"\n",
                            log.getLogId(), log.getDate(), log.getUser(), log.getMaterialName(), 
                            log.getChangeAmount(), log.getUnit(), log.getType(), 
                            log.getReferenceId(), log.getRemarks()
                    );
                }

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText("Audit Report Generated");
                alert.setContentText("Your inventory audit logs have been successfully exported to CSV.");
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