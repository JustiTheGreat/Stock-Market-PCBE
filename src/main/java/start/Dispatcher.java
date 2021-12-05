package start;

import communication_members.Client;
import communication_members.Server;
import connections.DatabaseConnection;

import java.util.ArrayList;
import java.util.Scanner;

import static connections.DatabaseConnection.USER_ID_NOT_FOUND;

public class Dispatcher extends Thread {
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

        int option = scanner.nextInt();
        while (true) {
            switch (option) {
                case 0:
                    closeAllThreads();
                    break;
                case 1:
                    dispatchClient(scanner);
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

    public static void dispatchClient(Scanner scanner) {
        scanner.nextLine();
        String username = scanner.nextLine();
        int userId;
        if (!username.isEmpty()) {
            userId = DatabaseConnection.getInstance().getUserIdByName(username);
            if (userId == USER_ID_NOT_FOUND) {
                DatabaseConnection.getInstance().insertUser(username);
                userId = DatabaseConnection.getInstance().getUserIdByName(username);
            }
            Client client = new Client(username, userId);
            clients.add(client);
            client.setThread(new Thread(client));
            client.getThread().start();
        }
    }
}