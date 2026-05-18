package application;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

public class SettingsController {

    @FXML private Label totalLogsLabel, totalDeductionsLabel, totalReturnsLabel;
    @FXML private ComboBox<String> typeCombo, timeCombo;
    @FXML private Button refreshBtn, exportBtn;
    
    @FXML private TableView<InventoryLog> logsTable;
    @FXML private TableColumn<InventoryLog, Integer> colLogId;
    @FXML private TableColumn<InventoryLog, String> colDate, colMaterial, colType;
    @FXML private TableColumn<InventoryLog, Double> colAmount;

    private static final String DB_URL = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require";
    private static final String DB_USER = "postgres.gwjmqejllljupondbzbs";
    private static final String DB_PASS = "Loritastecafe2026";

    private ObservableList<InventoryLog> masterLogList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupFilters();
        setupTableColumns();
        
        logsTable.setItems(masterLogList);
        executeDatabaseRefresh(); 
    }

    private void setupFilters() {
        typeCombo.getItems().addAll("All Log Types", "Sale Deduction", "Void / Return");
        typeCombo.getSelectionModel().selectFirst();
        typeCombo.setOnAction(e -> refreshData());

        timeCombo.getItems().addAll("All Time", "Today", "This Week", "This Month");
        timeCombo.getSelectionModel().selectFirst();
        timeCombo.setOnAction(e -> refreshData());
    }

    private void setupTableColumns() {
        colLogId.setCellValueFactory(new PropertyValueFactory<>("logId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colMaterial.setCellValueFactory(new PropertyValueFactory<>("materialName"));
        
        colAmount.setCellValueFactory(new PropertyValueFactory<>("changeAmount"));
        colAmount.setCellFactory(tc -> new TableCell<InventoryLog, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) { 
                    setText(null); 
                    setStyle("");
                } else { 
                    setText(String.format("%+.2f", amount)); 
                    
                    if (amount < 0) {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;"); // Red for negative
                    } else if (amount > 0) {
                        setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;"); // Green for positive
                    } else {
                        setStyle("-fx-text-fill: #9ca3af; -fx-font-weight: normal;"); // Gray for zero values
                    }
                }
            }
        });

        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
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

        String sql = "SELECT il.log_id, il.created_at, rm.name AS material_name, il.change_amount, il.type " +
                     "FROM inventory_logs il " +
                     "JOIN raw_materials rm ON il.raw_material_id = rm.id WHERE 1=1";
        
        String typeFilter = typeCombo.getValue();
        if (!"All Log Types".equals(typeFilter)) {
            sql += " AND il.type ILIKE '" + typeFilter + "'";
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
                double amount = rs.getDouble("change_amount");
                String type = rs.getString("type");
                
                String formattedDate = date != null ? sdf.format(date) : "N/A";
                
                // FIXED: Added "System" as the 6th parameter to match the updated InventoryLog model
                masterLogList.add(new InventoryLog(id, formattedDate, material, amount, type, "System"));
                
                totalLogs++;
                if (amount < 0) totalDeductions++;
                if (amount > 0 && (type.toLowerCase().contains("return") || type.toLowerCase().contains("void"))) totalReturns++;
            }

            totalLogsLabel.setText(String.valueOf(totalLogs));
            totalDeductionsLabel.setText(String.valueOf(totalDeductions));
            totalReturnsLabel.setText(String.valueOf(totalReturns));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleExportData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Inventory Logs Report");
        
        String currentDate = new SimpleDateFormat("yyyyMMdd").format(new Date());
        fileChooser.setInitialFileName("Loris_Taste_Cafe_Inventory_Logs_" + currentDate + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        Window window = logsTable.getScene().getWindow();
        File file = fileChooser.showSaveDialog(window);

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("Log ID,Date & Time,Raw Material,Amount Changed,Movement Type");

                for (InventoryLog log : masterLogList) {
                    writer.printf("%d,\"%s\",\"%s\",%.2f,\"%s\"\n",
                            log.getLogId(), log.getDate(), log.getMaterialName(), log.getChangeAmount(), log.getType()
                    );
                }

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText("Excel Report Generated");
                alert.setContentText("Your inventory logs have been exported.");
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