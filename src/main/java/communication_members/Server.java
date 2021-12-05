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

    public synchronized void matchOffersAndBids() {
        ArrayList<Stock> allStocks = DatabaseConnection.getInstance().getAllActiveStocks();
        ArrayList<Stock> bids = new ArrayList<>();
        ArrayList<Stock> offers = new ArrayList<>();

        for (Stock stock : allStocks) {
            if (stock.isBid()) bids.add(stock);
            else offers.add(stock);
        }

        boolean foundTransaction = false;
        for (int i = 0; i < offers.size() && !foundTransaction; i++) {
            for (int j = 0; j < bids.size() && !foundTransaction; j++) {
                if (offers.get(i).matchesPriceWith(bids.get(j)) && !offers.get(i).matchesClientWith(bids.get(j))) {
                    if (offers.get(i).getActionNumber() == bids.get(j).getActionNumber()) {
                        DatabaseConnection.getInstance().insertTransaction(offers.get(i), bids.get(j));
                        DatabaseConnection.getInstance().updateStock(new Stock(offers.get(i).getStockId(), offers.get(i).getClientId(), INACTIVE, offers.get(i).getActionName(), offers.get(i).getActionNumber(), offers.get(i).getPricePerAction()));
                        DatabaseConnection.getInstance().updateStock(new Stock(bids.get(j).getStockId(), bids.get(j).getClientId(), INACTIVE, bids.get(j).getActionName(), bids.get(j).getActionNumber(), bids.get(j).getPricePerAction()));
                    } else if (offers.get(i).getActionNumber() >= bids.get(j).getActionNumber()) {
                        int newActionNumber = offers.get(i).getActionNumber() - bids.get(j).getActionNumber();
                        DatabaseConnection.getInstance().insertTransaction(offers.get(i), bids.get(j));
                        DatabaseConnection.getInstance().updateStock(new Stock(offers.get(i).getStockId(), offers.get(i).getClientId(), offers.get(i).getType(), offers.get(i).getActionName(), newActionNumber, offers.get(i).getPricePerAction()));
                        DatabaseConnection.getInstance().updateStock(new Stock(bids.get(j).getStockId(), bids.get(j).getClientId(), INACTIVE, bids.get(j).getActionName(), bids.get(j).getActionNumber(), bids.get(j).getPricePerAction()));
                    } else {
                        int newActionNumber = bids.get(j).getActionNumber() - offers.get(i).getActionNumber();
                        DatabaseConnection.getInstance().insertTransaction(offers.get(i), bids.get(j));
                        DatabaseConnection.getInstance().updateStock(new Stock(offers.get(i).getStockId(), offers.get(i).getClientId(), INACTIVE, offers.get(i).getActionName(), offers.get(i).getActionNumber(), offers.get(i).getPricePerAction()));
                        DatabaseConnection.getInstance().updateStock(new Stock(bids.get(j).getStockId(), bids.get(j).getClientId(), bids.get(j).getType(), bids.get(j).getActionName(), newActionNumber, bids.get(j).getPricePerAction()));
                    }
                    foundTransaction = true;
                }
            }
        }
        if (foundTransaction) {
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