import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;

public class Server extends Thread implements EventsAndConstants , MyConnection, DatabaseConnection{
    private volatile boolean isRunning = true;

    private java.sql.Connection con = DatabaseConnetionQuery();

    public Server(String name) {
        super(name);
    }

    public void closeThread() {
        isRunning = false;
    }

    public synchronized void addStockDB(Stock stock) {
        String createUser = "insert into stock (type, action_name, action_number, price_per_action, client_id) values (?,?,?,?,?)";
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement(createUser);
            pst.setInt(1, stock.getType());
            pst.setString(2, stock.getActionName());
            pst.setInt(3, stock.getActionNumber());
            pst.setInt(4, stock.getPricePerAction());
            pst.setInt(5, stock.getClientId());
            pst.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public synchronized void addTrasactionDB(int offer_id, int bid_id) {
        String createUser = "insert into transaction (offer_id, bid_id) values (?,?)";
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement(createUser);
            pst.setInt(1, offer_id);
            pst.setInt(2, bid_id);
            pst.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public synchronized void addStock(Stock stock) {
        new Thread(() -> {
            addStockDB(stock);
            Message message = new Message(REFRESH_STOCKS,null,getStocks(),null);
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
            ArrayList<Stock> allStocks = getStocks();
            ArrayList<Stock> bids = new ArrayList<>();
            ArrayList<Stock> offers = new ArrayList<>();

            for(Stock stock : allStocks) {
                if(stock.getType() == BID){
                    bids.add(stock);
                } else {
                    offers.add(stock);
                }
            }

            boolean foundTransaction = false;
            for (int i = 0; i < offers.size() && !foundTransaction; i++) {
                for (int j = 0; j < bids.size() && !foundTransaction; j++) {
                    if (offers.get(i).matchesPriceWith(bids.get(j)) && !offers.get(i).matchesClientWith(bids.get(j))) {
                        if(offers.get(i).getActionNumber() == bids.get(j).getActionNumber()) {
                            addTrasactionDB(offers.get(i).getId(), bids.get(j).getId());
                            updateStock(-1, offers.get(i).getId(), offers.get(i).getActionName(), offers.get(i).getActionNumber(), offers.get(i).getPricePerAction(), offers.get(i).getClientId());
                            updateStock(-1, bids.get(j).getId(), bids.get(j).getActionName(), bids.get(j).getActionNumber(), bids.get(j).getPricePerAction(), bids.get(j).getClientId());
                        } else if (offers.get(i).getActionNumber() >= bids.get(j).getActionNumber()) {
                            int newActionNumber = offers.get(i).getActionNumber() - bids.get(j).getActionNumber();
                            addTrasactionDB(offers.get(i).getId(), bids.get(j).getId());
                            updateStock(offers.get(i).getType(), offers.get(i).getId(), offers.get(i).getActionName(), newActionNumber, offers.get(i).getPricePerAction(), offers.get(i).getClientId());
                            updateStock(-1, bids.get(j).getId(), bids.get(j).getActionName(), bids.get(j).getActionNumber(), bids.get(j).getPricePerAction(), bids.get(j).getClientId());
                        } else {
                            int newActionNumber = bids.get(j).getActionNumber() - offers.get(i).getActionNumber();
                            addTrasactionDB(offers.get(i).getId(), bids.get(j).getId());
                            updateStock(-1, offers.get(i).getId(), offers.get(i).getActionName(), offers.get(i).getActionNumber(), offers.get(i).getPricePerAction(), offers.get(i).getClientId());
                            updateStock(bids.get(j).getType(), bids.get(j).getId(), bids.get(j).getActionName(), newActionNumber, bids.get(j).getPricePerAction(), bids.get(j).getClientId());
                        }
                        foundTransaction = true;
                    }
                }
            }
            if (foundTransaction) {
                Message message1 = new Message(REFRESH_STOCKS, null, getStocks(), null);
                Message message2 = new Message(REFRESH_TRANSACTIONS, null, null, getTransactions());
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
                case EDIT:
                    break;
                case DELETE:
                    break;
                case REFRESH:
                    refreshData();
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

    public synchronized void updateStock(int type, int stockId, String actionName, int actionNumber, int pricePerAction, int clientId) {
        try {
            String updateStock = "update stock set type = ?, action_name = ?, action_number = ?, price_per_action = ?, client_id = ? where id = ?";
            PreparedStatement pst = con.prepareStatement(updateStock);
            pst.setInt(1, type);
            pst.setString(2, actionName);
            pst.setInt(3, actionNumber);
            pst.setInt(4, pricePerAction);
            pst.setInt(5, clientId);
            pst.setInt(6, stockId);
            pst.executeUpdate();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public synchronized ArrayList<Stock> getStocks() {
        ArrayList<Stock> allStocks = new ArrayList<>();
        try {
        String getStocks = "select * from stock";
        PreparedStatement pst = con.prepareStatement(getStocks);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            if(rs.getInt(2) >= 0) {
                allStocks.add(new Stock(rs.getInt(1), rs.getInt(6), rs.getInt(2), rs.getString(3), rs.getInt(4), rs.getInt(5)));
            }
        }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return allStocks;
    }

    public synchronized ArrayList<Transaction> getTransactions() {
        ArrayList<Transaction> transactions = new ArrayList<>();
        try {
            String getTransactions = "select * from transaction";
            String getStock = "select * from stock where id = ?";
            PreparedStatement pst = con.prepareStatement(getTransactions);
            ResultSet rs = pst.executeQuery();
            Stock stock1 = null;
            Stock stock2 = null;

            while (rs.next()) {
                int offer_id = rs.getInt(2);
                int bid_id = rs.getInt(3);

                pst = con.prepareStatement(getStock);
                pst.setInt(1, offer_id);
                ResultSet rs2 = pst.executeQuery();

                while (rs2.next()) {
                    stock1 = new Stock(rs2.getInt(1), rs2.getInt(6), rs2.getInt(2), rs2.getString(3), rs2.getInt(4), rs2.getInt(5));
                }

                pst = con.prepareStatement(getStock);
                pst.setInt(1, bid_id);
                ResultSet rs3 = pst.executeQuery();

                while (rs3.next()) {
                    stock2 = new Stock(rs3.getInt(1), rs3.getInt(6), rs3.getInt(2), rs3.getString(3), rs3.getInt(4), rs3.getInt(5));
                }

                transactions.add(new Transaction(stock1, stock2));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return transactions;
    }

        public void refreshData() {
        try{

            Message messageBack;
            messageBack = new Message(REFRESH_STOCKS, null, getStocks(), null);
            publish(messageBack, exchangeNameForServerToClients);
            messageBack = new Message(REFRESH_TRANSACTIONS, null, null, getTransactions());
            publish(messageBack, exchangeNameForServerToClients);
        } catch (TimeoutException | IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
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