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

    private final ThreadLocal<Boolean> foundTransaction = ThreadLocal.withInitial(() -> false);

    public Server(String name) {
        super(name);
    }

    public void closeThread() {
        isRunning = false;
    }

    public void addStock(Stock stock) {
        DatabaseConnection.getInstance().insertStock(stock);
        new Thread(() -> RabbitMQConnection.getInstance().publish(new Message(REFRESH_STOCKS, null, DatabaseConnection.getInstance().getAllActiveStocks(), null), exchangeNameForServerToClients)).start();
        new Thread(() -> matchOffersAndBids(stock)).start();
    }

    public void matchOffersAndBids(Stock stock) {
        List<Stock> allStocks = Collections.synchronizedList(DatabaseConnection.getInstance().getAllActiveStocks());
        List<Stock> listToSearchIn = Collections.synchronizedList(new ArrayList<>());

        synchronized(allStocks) {
            synchronized(listToSearchIn) {
                for (Stock aux : allStocks) {
                    if (stock.isOffer() && aux.isBid() || stock.isBid() && aux.isOffer()) {
                        listToSearchIn.add(aux);
                    }
                }
                Iterator<Stock> i = listToSearchIn.iterator();
                while (i.hasNext() && !foundTransaction.get()) {
                    Stock complementaryStock = i.next();
                    if (complementaryStock.matchesPriceWith(stock) && !complementaryStock.matchesClientWith(stock)) {
                        if (complementaryStock.getActionNumber() == stock.getActionNumber()) {
                            if(stock.isOffer()) DatabaseConnection.getInstance().insertTransaction(stock, complementaryStock);
                            else DatabaseConnection.getInstance().insertTransaction(complementaryStock, stock);
                            DatabaseConnection.getInstance().updateStock(new Stock(complementaryStock.getStockId(), complementaryStock.getClientId(), INACTIVE, complementaryStock.getActionName(), complementaryStock.getActionNumber(), complementaryStock.getPricePerAction()));
                            DatabaseConnection.getInstance().updateStock(new Stock(stock.getStockId(), stock.getClientId(), INACTIVE, stock.getActionName(), stock.getActionNumber(), stock.getPricePerAction()));
                        } else if (complementaryStock.getActionNumber() >= stock.getActionNumber()) {
                            int newActionNumber = complementaryStock.getActionNumber() - stock.getActionNumber();
                            if(stock.isOffer()) DatabaseConnection.getInstance().insertTransaction(stock, complementaryStock);
                            else DatabaseConnection.getInstance().insertTransaction(complementaryStock, stock);
                            DatabaseConnection.getInstance().updateStock(new Stock(complementaryStock.getStockId(), complementaryStock.getClientId(), complementaryStock.getType(), complementaryStock.getActionName(), newActionNumber, complementaryStock.getPricePerAction()));
                            DatabaseConnection.getInstance().updateStock(new Stock(stock.getStockId(), stock.getClientId(), INACTIVE, stock.getActionName(), stock.getActionNumber(), stock.getPricePerAction()));
                        } else {
                            int newActionNumber = stock.getActionNumber() - complementaryStock.getActionNumber();
                            if(stock.isOffer()) DatabaseConnection.getInstance().insertTransaction(stock, complementaryStock);
                            else DatabaseConnection.getInstance().insertTransaction(complementaryStock, stock);
                            DatabaseConnection.getInstance().updateStock(new Stock(complementaryStock.getStockId(), complementaryStock.getClientId(), INACTIVE, complementaryStock.getActionName(), complementaryStock.getActionNumber(), complementaryStock.getPricePerAction()));
                            DatabaseConnection.getInstance().updateStock(new Stock(stock.getStockId(), stock.getClientId(), stock.getType(), stock.getActionName(), newActionNumber, stock.getPricePerAction()));
                        }
                        foundTransaction.set(true);
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
        while (isRunning);
        System.out.println("Server closed successfully!");
    }
}