public class Client extends Thread{
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


    public void run() {
        System.out.println("Client with name " + this.getName() + " started succesfully!");
        while (isRunning) {}
        System.out.println("Client with name " + this.getName() + " stopped succesfully!");

    }



}
