package application;

import javafx.beans.property.*;

public class Order {
    private final SimpleIntegerProperty orderId;
    private final SimpleStringProperty date;
    private final SimpleStringProperty customerDetails;
    private final SimpleDoubleProperty totalAmount;
    private final SimpleStringProperty paymentMethod;

    public Order(int orderId, String date, String customerDetails, double totalAmount, String paymentMethod) {
        this.orderId = new SimpleIntegerProperty(orderId);
        this.date = new SimpleStringProperty(date);
        this.customerDetails = new SimpleStringProperty(customerDetails);
        this.totalAmount = new SimpleDoubleProperty(totalAmount);
        this.paymentMethod = new SimpleStringProperty(paymentMethod);
    }

    public int getOrderId() { return orderId.get(); }
    public String getDate() { return date.get(); }
    public String getCustomerDetails() { return customerDetails.get(); }
    public double getTotalAmount() { return totalAmount.get(); }
    public String getPaymentMethod() { return paymentMethod.get(); }
}