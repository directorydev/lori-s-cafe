package application;

import javafx.beans.property.*;

public class Order {
    private final SimpleIntegerProperty orderId;
    private final SimpleStringProperty date;
    private final SimpleStringProperty cashierName;
    private final SimpleDoubleProperty totalAmount;
    private final SimpleStringProperty paymentMethod;
    private final SimpleStringProperty status; // NEW

    public Order(int orderId, String date, String cashierName, double totalAmount, String paymentMethod, String status) {
        this.orderId = new SimpleIntegerProperty(orderId);
        this.date = new SimpleStringProperty(date);
        this.cashierName = new SimpleStringProperty(cashierName);
        this.totalAmount = new SimpleDoubleProperty(totalAmount);
        this.paymentMethod = new SimpleStringProperty(paymentMethod);
        this.status = new SimpleStringProperty(status);
    }

    public int getOrderId() { return orderId.get(); }
    public String getDate() { return date.get(); }
    public String getCashierName() { return cashierName.get(); }
    public double getTotalAmount() { return totalAmount.get(); }
    public String getPaymentMethod() { return paymentMethod.get(); }
    public String getStatus() { return status.get(); }
}