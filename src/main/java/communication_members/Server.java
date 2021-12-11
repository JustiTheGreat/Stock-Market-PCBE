package communication_members;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import connections.DatabaseConnection;
import connections.RabbitMQConnection;
import constants.EventsAndConstants;
import data_objects.Message;
import data_objects.Stock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static connections.RabbitMQConnection.*;

public class Server extends Thread implements EventsAndConstants {
    private volatile boolean isRunning = true;

    public Server(String name) {
        super(name);
    }

    public void closeThread() {
        isRunning = false;
    }

    public void addStock(Stock stock) {
        DatabaseConnection.getInstance().insertStock(stock);
        new Thread(() -> RabbitMQConnection.getInstance().publish(new Message(REFRESH_STOCKS, null, DatabaseConnection.getInstance().getAllActiveStocks(), null), exchangeNameForServerToClients)).start();
        new Thread(this::matchOffersAndBids).start();
    }

    public void matchOffersAndBids() {

        List<Stock> allStocks = Collections.synchronizedList(DatabaseConnection.getInstance().getAllActiveStocks());
        List<Stock> bids = Collections.synchronizedList(new ArrayList<>());
        List<Stock> offers = Collections.synchronizedList(new ArrayList<>());

        ThreadLocal<Boolean> foundTransaction = new ThreadLocal<>();
        foundTransaction.set(false);

        synchronized(allStocks) {
            synchronized(bids){
                synchronized(offers){
                    Iterator<Stock> k = allStocks.iterator();
                    while (k.hasNext()){
                        Stock aux = k.next();
                        if (aux.isBid()) bids.add(aux);
                        else if (aux.isOffer()) offers.add(aux);
                    }
                    Iterator<Stock> i = offers.iterator();
                    while(i.hasNext() && !foundTransaction.get()){
                        Iterator<Stock> j = bids.iterator();
                        while(j.hasNext() && !foundTransaction.get()){
                            Stock offer = i.next();
                            Stock bid = j.next();
                            if (offer.matchesPriceWith(bid) && !offer.matchesClientWith(bid)) {
                                if (offer.getActionNumber() == bid.getActionNumber()) {
                                    DatabaseConnection.getInstance().insertTransaction(offer, bid);
                                    DatabaseConnection.getInstance().updateStock(new Stock(offer.getStockId(), offer.getClientId(), INACTIVE, offer.getActionName(), offer.getActionNumber(), offer.getPricePerAction()));
                                    DatabaseConnection.getInstance().updateStock(new Stock(bid.getStockId(), bid.getClientId(), INACTIVE, bid.getActionName(), bid.getActionNumber(), bid.getPricePerAction()));
                                } else if (offer.getActionNumber() >= bid.getActionNumber()) {
                                    int newActionNumber = offer.getActionNumber() - bid.getActionNumber();
                                    DatabaseConnection.getInstance().insertTransaction(offer, bid);
                                    DatabaseConnection.getInstance().updateStock(new Stock(offer.getStockId(), offer.getClientId(), offer.getType(), offer.getActionName(), newActionNumber, offer.getPricePerAction()));
                                    DatabaseConnection.getInstance().updateStock(new Stock(bid.getStockId(), bid.getClientId(), INACTIVE, bid.getActionName(), bid.getActionNumber(), bid.getPricePerAction()));
                                } else {
                                    int newActionNumber = bid.getActionNumber() - offer.getActionNumber();
                                    DatabaseConnection.getInstance().insertTransaction(offer, bid);
                                    DatabaseConnection.getInstance().updateStock(new Stock(offer.getStockId(), offer.getClientId(), INACTIVE, offer.getActionName(), offer.getActionNumber(), offer.getPricePerAction()));
                                    DatabaseConnection.getInstance().updateStock(new Stock(bid.getStockId(), bid.getClientId(), bid.getType(), bid.getActionName(), newActionNumber, bid.getPricePerAction()));
                                }
                                foundTransaction.set(true);
                            }
                        }
                    }
                }
            }
        }
        
        if (foundTransaction.get()) {
            refreshData();
        }
    }

    private void editStock(Stock stock) {
        DatabaseConnection.getInstance().updateStock(stock);
        refreshData();
    }

    private void deleteStock(Stock stock) {
        DatabaseConnection.getInstance().deleteStockById(stock);
        refreshData();
    }

    public void refreshData() {
        new Thread(() -> RabbitMQConnection.getInstance().publish(new Message(REFRESH_STOCKS, null, DatabaseConnection.getInstance().getAllActiveStocks(), null), exchangeNameForServerToClients)).start();
        new Thread(() -> RabbitMQConnection.getInstance().publish(new Message(REFRESH_TRANSACTIONS, null, null, DatabaseConnection.getInstance().getAllTransactions()), exchangeNameForServerToClients)).start();
    }

    public void startWaitingForClients() throws IOException, TimeoutException {
        Connection connection = FACTORY.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(exchangeNameForClientsToServer, "fanout");
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, exchangeNameForClientsToServer, "");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            byte[] bytes = delivery.getBody();
            Message message = (Message) RabbitMQConnection.getInstance().ByteArrayToObject(bytes);
            switch (message.getSubject()) {
                case PUBLISH:
                case SUBSCRIBE:
                    new Thread(() -> addStock(message.getStock())).start();
                    break;
                case EDIT:
                    new Thread(() -> editStock(message.getStock())).start();
                    break;
                case DELETE:
                    new Thread(() -> deleteStock(message.getStock())).start();
                    break;
                case REFRESH:
                    new Thread(this::refreshData).start();
                    break;
                default:
                    System.out.println("Server received wrong message type!");
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
        while (isRunning) ;
        System.out.println("Server closed successfully!");
    }
}