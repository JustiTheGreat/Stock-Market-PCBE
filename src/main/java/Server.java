import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;


public class Server extends Thread implements EventsAndConstants , MyConnection {
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

    public synchronized void addStock(Stock stock) {
        new Thread(() -> {
            if (stock.isOffer())
                offers.add(stock);
            else if (stock.isBid())
                bids.add(stock);
            allStocks.add(stock);
            Message message = new Message(REFRESH_STOCKS, null, new ArrayList<>(allStocks), null);
            try {
                publish(message, exchangeNameForServerToClients);
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
                System.exit(-1);
            }
            matchOffersAndBids();
        }).start();
    }

    public synchronized void matchOffersAndBids() {
        new Thread(() -> {
            boolean foundTransaction = false;
            for (int i = 0; i < offers.size() && !foundTransaction; i++) {
                for (int j = 0; j < bids.size() && !foundTransaction; j++) {
                    if (offers.get(i).matchesPriceWith(bids.get(j))
                            && !offers.get(i).matchesClientWith(bids.get(j))) {
                        System.out.println(i + "," + j);
                        transactions.add(new Transaction(offers.get(i), bids.get(j)));
                        allStocks.remove(offers.get(i));
                        allStocks.remove(bids.get(j));
                        offers.remove(i);
                        bids.remove(j);
                        foundTransaction = true;
                    }
                }
            }
            if (foundTransaction) {
                Message message1 = new Message(REFRESH_STOCKS, null, new ArrayList<>(allStocks), null);
                Message message2 = new Message(REFRESH_TRANSACTIONS, null, null, new ArrayList<>(transactions));
                try {
                    publish(message1, exchangeNameForServerToClients);
                    publish(message2, exchangeNameForServerToClients);
                } catch (IOException | TimeoutException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        }).start();
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
                    addStock(message.getStock());
                    break;
                case EDIT:
                    editStock(message.getStock());
                    break;
                case DELETE:
                    break;
                case REFRESH:
                    try {
                        ArrayList<Stock> als = new ArrayList<>(allStocks);
                        Message messageBack;
                        messageBack = new Message(REFRESH_STOCKS, null, new ArrayList<>(allStocks), null);
                        publish(messageBack, exchangeNameForServerToClients);
                        messageBack = new Message(REFRESH_TRANSACTIONS, null, null, new ArrayList<>(transactions));
                        publish(messageBack, exchangeNameForServerToClients);
                    } catch (TimeoutException e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }
                    break;
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

    private synchronized void editStock(Stock stock) {
        new Thread(() -> {
            for (
                    Stock currentStock : allStocks) {
                if (currentStock.getID() == stock.getID()) {
                    allStocks.set(allStocks.indexOf(currentStock), stock);
                }
            }
            if (stock.isBid()) {
                for (Stock currentStock : bids) {
                    if (currentStock.getID() == stock.getID()) {
                        bids.set(bids.indexOf(currentStock), stock);
                    }
                }
            }
            if (stock.isOffer()) {
                for (Stock currentStock : offers) {
                    if (currentStock.getID() == stock.getID()) {
                        offers.set(offers.indexOf(currentStock), stock);
                    }
                }
            }

            Message message = new Message(REFRESH_STOCKS, null, new ArrayList<>(allStocks), null);
            try {
                publish(message, exchangeNameForServerToClients);
            } catch (IOException |
                    TimeoutException e) {
                e.printStackTrace();
                System.exit(-1);
            }

            matchOffersAndBids();
        }).start();

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
        while (isRunning) ;
        System.out.println("Server closed successfully!");
    }
}
