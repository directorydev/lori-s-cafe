package application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;

public class MenuController {
    @FXML private TableView<MenuItem> menuTable;
    @FXML private TableColumn<MenuItem, String> colName;
    @FXML private TableColumn<MenuItem, String> colCategory;
    @FXML private TableColumn<MenuItem, Double> colPrice;
    
    @FXML private TextField nameInput, priceInput;
    
    // CHANGED: TextField to ComboBox
    @FXML private ComboBox<String> categoryComboBox;
    
    @FXML private ImageView itemImagePreview;
    @FXML private Label placeholderLabel;
    
    private String selectedImagePath = null;
    private ObservableList<MenuItem> menuData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 1. Setup Table Columns
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        // 2. Setup Category Dropdown (Data Integrity for Quirap & Listangco)
        ObservableList<String> categories = FXCollections.observableArrayList(
            "Coffee", "Non-Coffee", "Pastry", "Snacks", "Meals"
        );
        categoryComboBox.setItems(categories);

        // 3. Sample Data
        menuData.add(new MenuItem("Espresso", "Coffee", 120.00, null));
        menuData.add(new MenuItem("Caramel Latte", "Coffee", 165.00, null));
        
        menuTable.setItems(menuData);
    }

    @FXML
    public void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        // Get window from any existing node
        File file = fileChooser.showOpenDialog(nameInput.getScene().getWindow());

        if (file != null) {
            selectedImagePath = file.toURI().toString();
            Image image = new Image(selectedImagePath);
            itemImagePreview.setImage(image);
            placeholderLabel.setVisible(false); // Hide "No Image Selected" text
        }
    }

    @FXML
    public void addItem() {
        String name = nameInput.getText();
        String category = categoryComboBox.getValue(); // Get from ComboBox
        String priceRaw = priceInput.getText();

        // Simple validation: Ensure no empty fields
        if(name.isEmpty() || category == null || priceRaw.isEmpty()) {
            System.out.println("Error: All fields are required!");
            return;
        }

        try {
            double price = Double.parseDouble(priceRaw);
            
            menuData.add(new MenuItem(
                name,
                category,
                price,
                selectedImagePath
            ));
            
            clearInputs();
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid price format. Please enter a number.");
        }
    }

    private void clearInputs() {
        nameInput.clear();
        categoryComboBox.getSelectionModel().clearSelection();
        priceInput.clear();
        itemImagePreview.setImage(null);
        placeholderLabel.setVisible(true);
        selectedImagePath = null;
    }

    @FXML
    public void deleteItem() {
        MenuItem selected = menuTable.getSelectionModel().getSelectedItem();
        if(selected != null) {
            menuData.remove(selected);
        }
    }
}