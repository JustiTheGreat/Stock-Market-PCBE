import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public interface DatabaseConnection {
    default Connection DatabaseConnetionQuery() {
        Connection con = null;
        try {
            con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/stockmarket", "postgres", "postgresadmin");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return con;
    }
}