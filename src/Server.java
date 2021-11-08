public class Server extends Thread{
    private volatile boolean isRunning = true;

    public Server(String name) {
        super(name);
    }

    @Override
    public void run() {
        System.out.println("Server started successfully!");
        while (isRunning);
        System.out.println("Server closed successfully!");
    }
}
