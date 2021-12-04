import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

public class Dispatcher extends Thread implements DatabaseConnection {
    private static final Server server = new Server("Server burse");
    private static final ArrayList<Client> clients = new ArrayList<>();

    @Override
    public void run() {
        server.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        System.out.println("Commands:\n0: Exit system\n1: Start new client");
        Scanner scanner = new Scanner(System.in);
        Connection con = DatabaseConnetionQuery();

        int option = scanner.nextInt();
        while (true) {
            switch (option) {
                case 0:
                    closeAllThreads();
                    break;
                case 1:
                    try {
                        dispatchClient(scanner, con);
                    } catch (SQLException throwables) {
                        System.err.println("SQL Exception catched !!!");
                        throwables.printStackTrace();
                    }
                    break;
                default:
                    System.out.println("Wrong input!");
            }
        }
    }

    public static void closeAllThreads() {
        clients.forEach((client) -> {
            if (client.getThread().isAlive()) {
                client.closeThread();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
                if (client.getThread().isAlive()) {
                    System.out.println("Problems closing client: " + client.getName() + "!");
                }
            }
        });
        server.closeThread();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        if (server.isAlive()) System.out.println("Problems closing: " + server.getName() + "!");
        System.exit(0);
    }

    public static void dispatchClient(Scanner scanner, Connection con) throws SQLException {
        scanner.nextLine();
        String username = scanner.nextLine();
        int userId = -1; // wrong case, that means this Id doesn't exist on database

        if (username != "") {
            String query = "select * from users where name = ?";
            PreparedStatement pst = con.prepareStatement(query);
            pst.setString(1, username);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                userId = rs.getInt(1);
            }

            if (userId == -1) {
                String createUser = "insert into users (name) values (?)";
                pst = con.prepareStatement(createUser);
                pst.setString(1, username);
                pst.executeUpdate();

                pst = con.prepareStatement(query);
                pst.setString(1, username);
                rs = pst.executeQuery();
                while (rs.next()) {
                    userId = rs.getInt(1);
                }
            }

            Client client = new Client(username, userId);
            clients.add(client);
            client.setThread(new Thread(client));
            client.getThread().start();
        }
    }
}