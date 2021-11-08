import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.concurrent.TimeoutException;

public class Client {
    public volatile boolean isRunning = true;
    private Thread thread;

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    public Thread getThread() {
        return thread;
    }

    public void closeThread() {
        isRunning = false;
    }

    @Override
    public void run() {
        System.out.println("Client with name " + this.getName() + " started succesfully!");
        while (isRunning) {};
        System.out.println("Client with name " + this.getName() + " stopped succesfully!");

    }
}
