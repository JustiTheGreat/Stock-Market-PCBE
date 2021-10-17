import java.util.ArrayList;
import java.util.Scanner;

public class Dispatcher extends Thread {
    private static final Server server = new Server();
    private static final ArrayList<Client> clients = new ArrayList<>();

    @Override
    public void run() {
        server.start();
        System.out.println("Commands:\n0: Exit system\n1: Start new client");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            switch (scanner.nextInt()) {
                case 0:
                    closeAllThreads();
                    break;
                case 1:
                    dispatchClient();
                    break;
                default:
                    System.out.println("Wrong input!");
            }
        }
    }

    public static void closeAllThreads() {
        System.exit(0);
    }

    public static void dispatchClient() {
        Client client = new Client();
        clients.add(client);
        client.start();
    }
}

