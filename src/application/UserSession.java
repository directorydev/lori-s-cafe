package application;

public class UserSession {
    private static UserSession instance;
    private final String username;
    private final String role;
    private final String displayName;

    // Private constructor updated to accept role and display name
    private UserSession(String username, String role, String displayName) {
        this.username = username;
        this.role = role;
        this.displayName = displayName != null ? displayName : username;
    }

    // Call this after successful database authentication with all user claims
    public static void startSession(String username, String role, String displayName) {
        instance = new UserSession(username, role, displayName);
    }

    // Call this during handleLogout
    public static void cleanSession() {
        instance = null;
    }

    // Use this in DashboardController to prevent unauthorized access
    public static boolean isActive() {
        return instance != null;
    }

    public static String getLoggedUser() {
        return instance != null ? instance.username : null;
    }

    // New: Use this in headers to show "Welcome, Jairus!" or "Welcome, admin"
    public static String getDisplayName() {
        return instance != null ? instance.displayName : null;
    }

    // New: Use this to get the raw role string ("Admin" or "InventoryStaff")
    public static String getRole() {
        return instance != null ? instance.role : null;
    }

    // New: Role helper verification checks for clean dashboard authorization
    public static boolean isAdmin() {
        return instance != null && "Admin".equalsIgnoreCase(instance.role);
    }

    public static boolean isInventoryStaff() {
        return instance != null && "InventoryStaff".equalsIgnoreCase(instance.role);
    }
}