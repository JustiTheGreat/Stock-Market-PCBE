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

    private CopyOnWriteArrayList<Stock> offers = new CopyOnWriteArrayList();
    private CopyOnWriteArrayList<Stock> bids = new CopyOnWriteArrayList();
    private CopyOnWriteArrayList<Transaction> transactions = new CopyOnWriteArrayList();
    private CopyOnWriteArrayList<Stock> allStocks = new CopyOnWriteArrayList();
    private java.sql.Connection con = DatabaseConnetionQuery();

    public Server(String name) {
        super(name);
    }

    public void closeThread() {
        isRunning = false;
    }

    public synchronized void addStock(Stock stock) {
        new Thread(() -> {
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

            String getStocks = "select * from stock";
            ResultSet rs = null;
            ArrayList<Stock> allStocks = new ArrayList<>();
            try {
                pst = con.prepareStatement(getStocks);
                rs = pst.executeQuery();

                while (rs.next()) {
                    allStocks.add(new Stock(rs.getInt(6), rs.getInt(2), rs.getString(3), rs.getInt(4), rs.getInt(5)));
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }

            Message message = new Message(REFRESH_STOCKS,null,new ArrayList<>(allStocks),null);
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

    public void refreshData() {
        try{
            String getStocks = "select * from stock";
            ArrayList<Stock> allStocks = new ArrayList<>();
            PreparedStatement pst = con.prepareStatement(getStocks);
            ResultSet rs = pst.executeQuery();
            ResultSet rs2, rs3 = null;

            while (rs.next()) {
                System.out.println(rs.getInt(6));
                System.out.println(rs.getInt(2));
                System.out.println(rs.getString(3));
                System.out.println(rs.getInt(4));
                allStocks.add(new Stock(rs.getInt(6), rs.getInt(2), rs.getString(3), rs.getInt(4), rs.getInt(5)));
            }

            String getTransactions = "select * from transaction";
            String getStock = "select * from stock where id = ?";
            ArrayList<Transaction> transactions = new ArrayList<>();
            pst = con.prepareStatement(getTransactions);
            rs = pst.executeQuery();
            Stock stock1 = null;
            Stock stock2 = null;

            while (rs.next()) {
                int offer_id = rs.getInt(2);
                int bid_id = rs.getInt(3);

                pst = con.prepareStatement(getStock);
                pst.setInt(1, offer_id);
                rs2 = pst.executeQuery();
                pst.setInt(1, bid_id);
                rs3 = pst.executeQuery();

                while(rs2.next() && rs3.next()) {
                    stock1 = new Stock(rs2.getInt(6), rs2.getInt(2), rs2.getString(3), rs2.getInt(4), rs2.getInt(5));
                    stock2 = new Stock(rs3.getInt(6), rs3.getInt(2), rs3.getString(3), rs3.getInt(4), rs3.getInt(5));
                }

                transactions.add(new Transaction(stock1, stock2));
            }


            Message messageBack;
            messageBack = new Message(REFRESH_STOCKS, null, new ArrayList<>(allStocks), null);
            publish(messageBack, exchangeNameForServerToClients);
            messageBack = new Message(REFRESH_TRANSACTIONS, null, null, new ArrayList<>(transactions));
            publish(messageBack, exchangeNameForServerToClients);
        } catch (TimeoutException | IOException | SQLException e) {
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