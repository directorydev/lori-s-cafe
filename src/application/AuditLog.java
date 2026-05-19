package application;

import javafx.beans.property.SimpleStringProperty;

public class AuditLog {
    private final SimpleStringProperty traceId; // NEW
    private final SimpleStringProperty date;
    private final SimpleStringProperty module;
    private final SimpleStringProperty action;
    private final SimpleStringProperty details;
    private final SimpleStringProperty user;
    private final SimpleStringProperty referenceId;

    public AuditLog(String traceId, String date, String module, String action, String details, String user, String referenceId) {
        this.traceId = new SimpleStringProperty(traceId);
        this.date = new SimpleStringProperty(date);
        this.module = new SimpleStringProperty(module);
        this.action = new SimpleStringProperty(action);
        this.details = new SimpleStringProperty(details);
        this.user = new SimpleStringProperty(user != null ? user : "System");
        this.referenceId = new SimpleStringProperty(referenceId != null && !referenceId.equals("0") && !referenceId.equals("N/A") ? "#" + referenceId : "N/A");
    }

    public String getTraceId() { return traceId.get(); }
    public String getDate() { return date.get(); }
    public String getModule() { return module.get(); }
    public String getAction() { return action.get(); }
    public String getDetails() { return details.get(); }
    public String getUser() { return user.get(); }
    public String getReferenceId() { return referenceId.get(); }
}