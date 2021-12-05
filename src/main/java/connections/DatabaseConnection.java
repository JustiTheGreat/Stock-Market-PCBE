package connections;

import constants.EventsAndConstants;
import data_objects.Stock;
import data_objects.Transaction;

import java.sql.*;
import java.util.ArrayList;

public class DatabaseConnection {
    private static final DatabaseConnection instance = new DatabaseConnection();
    private static volatile Connection DATABASE_CONNECTION;
    public static final int USER_ID_NOT_FOUND = -1;

    private final Object getUserLock = new Object();
    private final Object insertUserLock = new Object();

    private final Object alterStockLock = new Object();
    private final Object readStocksLock = new Object();
    private final Object readStockLock = new Object();

    private final Object insertTransactionLock = new Object();
    private final Object readTransactionsLock = new Object();

    private DatabaseConnection() {
        try {
            DATABASE_CONNECTION = DriverManager.getConnection("jdbc:postgresql://localhost:5432/stockmarket", "postgres", "pcbe");
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static DatabaseConnection getInstance() {
        return instance;
    }

    public int getUserIdByName(String username) {
        try {
            String selectUserByName = "select * from users where name = ?";
            while (Thread.holdsLock(insertUserLock)) ;
            synchronized (getUserLock) {
                PreparedStatement pst = DATABASE_CONNECTION.prepareStatement(selectUserByName);
                pst.setString(1, username);
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return USER_ID_NOT_FOUND;
    }

    public void insertUser(String username) {
        try {
            String insertUser = "insert into users (name) values (?)";

            while (Thread.holdsLock(getUserLock)) ;
            synchronized (insertUserLock) {
                PreparedStatement pst = DATABASE_CONNECTION.prepareStatement(insertUser);
                pst.setString(1, username);
                pst.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public ArrayList<Stock> getAllActiveStocks() {
        try {
            String selectAllStocks = "select * from stock";
            PreparedStatement pst = DATABASE_CONNECTION.prepareStatement(selectAllStocks);
            while (Thread.holdsLock(alterStockLock)) ;
            synchronized (readStocksLock) {
                ResultSet rs = pst.executeQuery();
                ArrayList<Stock> allStocks = new ArrayList<>();
                while (rs.next()) {
                    if (rs.getInt(2) != EventsAndConstants.INACTIVE) {
                        allStocks.add(new Stock(rs.getInt(1), rs.getInt(6), rs.getInt(2), rs.getString(3), rs.getInt(4), rs.getInt(5)));
                    }
                }
                return allStocks;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return null;
    }

    public Stock getStockById(int id) {
        try {
            String selectStockById = "select * from stock where id = ?";
            while (Thread.holdsLock(alterStockLock)) ;
            synchronized (readStockLock) {
                PreparedStatement pst = DATABASE_CONNECTION.prepareStatement(selectStockById);
                pst.setInt(1, id);
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    return new Stock(rs.getInt(1), rs.getInt(6), rs.getInt(2), rs.getString(3), rs.getInt(4), rs.getInt(5));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return null;
    }

    public ArrayList<Transaction> getAllTransactions() {
        ArrayList<Transaction> transactions = new ArrayList<>();
        try {
            String selectAllTransactions = "select * from transaction";

            while (Thread.holdsLock(insertTransactionLock)) ;
            synchronized (readTransactionsLock) {
                PreparedStatement pst = DATABASE_CONNECTION.prepareStatement(selectAllTransactions);
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    transactions.add(new Transaction(getStockById(rs.getInt(2)), getStockById(rs.getInt(3))));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return transactions;
    }

    public void insertStock(Stock stock) {
        try {
            String insertStock = "insert into stock (type, action_name, action_number, price_per_action, client_id) values (?,?,?,?,?)";

            while (Thread.holdsLock(readStocksLock)) ;
            synchronized (alterStockLock) {
                PreparedStatement pst = DATABASE_CONNECTION.prepareStatement(insertStock);
                pst.setInt(1, stock.getType());
                pst.setString(2, stock.getActionName());
                pst.setInt(3, stock.getActionNumber());
                pst.setInt(4, stock.getPricePerAction());
                pst.setInt(5, stock.getClientId());
                pst.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void insertTransaction(Stock offer, Stock bid) {
        try {
            String insertTransaction = "insert into transaction (offer_id, bid_id) values (?,?)";

            while (Thread.holdsLock((readTransactionsLock))) ;
            synchronized (insertTransactionLock) {
                PreparedStatement pst = DATABASE_CONNECTION.prepareStatement(insertTransaction);
                pst.setInt(1, offer.getStockId());
                pst.setInt(2, bid.getStockId());
                pst.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void updateStock(Stock stock) {
        try {
            String updateStock = "update stock set type = ?, action_name = ?, action_number = ?, price_per_action = ?, client_id = ? where id = ?";

            while (Thread.holdsLock(readStocksLock)) ;
            synchronized (alterStockLock) {
                PreparedStatement pst = DATABASE_CONNECTION.prepareStatement(updateStock);
                pst.setInt(1, stock.getType());
                pst.setString(2, stock.getActionName());
                pst.setInt(3, stock.getActionNumber());
                pst.setInt(4, stock.getPricePerAction());
                pst.setInt(5, stock.getClientId());
                pst.setInt(6, stock.getStockId());
                pst.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void deleteStockById(Stock stock) {
        try {
            String deleteStockById = "delete from stock where id = ?";

            while (Thread.holdsLock(readStocksLock)) ;
            synchronized (alterStockLock) {
                PreparedStatement pst = DATABASE_CONNECTION.prepareStatement(deleteStockById);
                pst.setInt(1, stock.getStockId());
                pst.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
