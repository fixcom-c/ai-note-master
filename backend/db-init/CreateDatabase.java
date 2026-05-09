import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class CreateDatabase {
    public static void main(String[] args) throws Exception {
        String host = "192.168.0.76";
        String user = "root";
        String password = "Fix2019";

        String url = "jdbc:mysql://" + host + ":3306?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=GMT%2B8";

        System.out.println("Connecting to MySQL...");
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            System.out.println("Creating database 'backend' if not exists...");
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS backend CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            System.out.println("Database 'backend' created or already exists.");
        }
    }
}