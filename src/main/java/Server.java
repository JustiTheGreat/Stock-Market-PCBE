import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;


public class Server extends Thread implements EventsAndConstants , MyConnection{
    private volatile boolean isRunning = true;

    private CopyOnWriteArrayList<Stock> offers = new CopyOnWriteArrayList();
    private CopyOnWriteArrayList<Stock> bids = new CopyOnWriteArrayList();
    private CopyOnWriteArrayList<Stock> allStocks = new CopyOnWriteArrayList();
    private CopyOnWriteArrayList<Transaction> transactions = new CopyOnWriteArrayList();

    public Server(String name) {
        super(name);
    }

    public void closeThread() {
        isRunning = false;
    }

    public void startWaitingForClients() throws IOException, TimeoutException {
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(exchangeNameForClientsToServer, "fanout");
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, exchangeNameForClientsToServer, "");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            byte[] bytes = delivery.getBody();
            Message message = null;
            try {
                message = (Message) MyConnection.ByteArrayToObject(bytes);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                System.exit(-1);
            }
            switch (message.getSubject()) {
                case PUBLISH:
                case SUBSCRIBE:

                case EDIT:

                case DELETE:

                case REFRESH:

                default:
                    System.out.println("Server received wrong message type!");
                    System.exit(-1);
            }
            System.err.println("Server received: " + message.getSubject());
        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
        });
        System.err.println("Server started waiting for client messages!");
    }


    @Override
    public void run() {
        try {
            startWaitingForClients();
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        System.out.println("Server started successfully!");
        while (isRunning);
        System.out.println("Server closed successfully!");
    }
}
