package application;

public class UserSession {
    private static UserSession instance;
    private String username;

    private UserSession(String username) {
        this.username = username;
    }

    // Call this after successful database authentication
    public static void startSession(String username) {
        instance = new UserSession(username);
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
}