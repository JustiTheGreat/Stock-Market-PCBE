import java.util.ArrayList;
import java.util.Scanner;

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

    public static void dispatchClient() {
        Client client = new Client();
        clients.add(client);
        client.setThread(new Thread(client));
        client.getThread().start();
    }
}

