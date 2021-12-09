package connections;

import constants.EventsAndConstants;
import data_objects.Stock;
import data_objects.Transaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DatabaseConnection {
    private static final DatabaseConnection instance = new DatabaseConnection();
    private static volatile Connection DATABASE_CONNECTION;
    public static final int USER_ID_NOT_FOUND = -1;

    private final ReentrantReadWriteLock userRWLock = new ReentrantReadWriteLock();
    private final Lock userReadLock = userRWLock.readLock();
    private final Lock userWriteLock = userRWLock.writeLock();

    private final ReentrantReadWriteLock stockRWLock = new ReentrantReadWriteLock();
    private final Lock stockReadLock = stockRWLock.readLock();
    private final Lock stockWriteLock = stockRWLock.writeLock();

    private final ReentrantReadWriteLock transactionRWLock = new ReentrantReadWriteLock();
    private final Lock transactionReadLock = transactionRWLock.readLock();
    private final Lock transactionWriteLock = transactionRWLock.writeLock();

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
            userReadLock.lock();
            PreparedStatement pst = DATABASE_CONNECTION.prepareStatement(selectUserByName);
            pst.setString(1, username);
            ThreadLocal<ResultSet> rs = new ThreadLocal<ResultSet>() {{
                set(pst.executeQuery());
            }};
            userReadLock.unlock();
            if (rs.get().next()) return rs.get().getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return USER_ID_NOT_FOUND;
    }

    public void insertUser(String username) {
        try {
            String insertUser = "insert into users (name) values (?)";
            userWriteLock.lock();
            PreparedStatement pst = DATABASE_CONNECTION.prepareStatement(insertUser);
            pst.setString(1, username);
            pst.executeUpdate();
            userWriteLock.unlock();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public ArrayList<Stock> getAllActiveStocks() {
        try {
            String selectAllStocks = "select * from stock";
            PreparedStatement pst = DATABASE_CONNECTION.prepareStatement(selectAllStocks);
            stockReadLock.lock();
            ThreadLocal<ResultSet> rs = new ThreadLocal<ResultSet>() {{
                set(pst.executeQuery());
            }};
            stockReadLock.unlock();
            Collection<Stock> allStocks = Collections.synchronizedCollection(new ArrayList<>());
            while (rs.get().next()) {
                if (rs.get().getInt(2) != EventsAndConstants.INACTIVE) {
                    allStocks.add(new Stock(rs.get().getInt(1), rs.get().getInt(6), rs.get().getInt(2), rs.get().getString(3), rs.get().getInt(4), rs.get().getInt(5)));
                }
            }
            return (ArrayList<Stock>) allStocks;
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return null;
    }

    public Stock getStockById(int id) {
        try {
            String selectStockById = "select * from stock where id = ?";
            stockReadLock.lock();
            PreparedStatement pst = DATABASE_CONNECTION.prepareStatement(selectStockById);
            pst.setInt(1, id);
            ThreadLocal<ResultSet> rs = new ThreadLocal<ResultSet>() {{
                set(pst.executeQuery());
            }};
            stockReadLock.unlock();
            if (rs.get().next()) {
                return new Stock(rs.get().getInt(1), rs.get().getInt(6), rs.get().getInt(2), rs.get().getString(3), rs.get().getInt(4), rs.get().getInt(5));
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
            PreparedStatement pst = DATABASE_CONNECTION.prepareStatement(selectAllTransactions);
            transactionReadLock.lock();
            ThreadLocal<ResultSet> rs = new ThreadLocal<ResultSet>() {{
                set(pst.executeQuery());
            }};
            transactionReadLock.unlock();
            while (rs.get().next()) {
                transactions.add(new Transaction(getStockById(rs.get().getInt(2)), getStockById(rs.get().getInt(3))));
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
            stockWriteLock.lock();
            PreparedStatement pst = DATABASE_CONNECTION.prepareStatement(insertStock);
            pst.setInt(1, stock.getType());
            pst.setString(2, stock.getActionName());
            pst.setInt(3, stock.getActionNumber());
            pst.setInt(4, stock.getPricePerAction());
            pst.setInt(5, stock.getClientId());
            pst.executeUpdate();
            stockWriteLock.unlock();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void insertTransaction(Stock offer, Stock bid) {
        try {
            String insertTransaction = "insert into transaction (offer_id, bid_id) values (?,?)";
            transactionWriteLock.lock();
            PreparedStatement pst = DATABASE_CONNECTION.prepareStatement(insertTransaction);
            pst.setInt(1, offer.getStockId());
            pst.setInt(2, bid.getStockId());
            pst.executeUpdate();
            transactionWriteLock.unlock();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void updateStock(Stock stock) {
        try {
            String updateStock = "update stock set type = ?, action_name = ?, action_number = ?, price_per_action = ?, client_id = ? where id = ?";
            stockWriteLock.lock();
            PreparedStatement pst = DATABASE_CONNECTION.prepareStatement(updateStock);
            pst.setInt(1, stock.getType());
            pst.setString(2, stock.getActionName());
            pst.setInt(3, stock.getActionNumber());
            pst.setInt(4, stock.getPricePerAction());
            pst.setInt(5, stock.getClientId());
            pst.setInt(6, stock.getStockId());
            pst.executeUpdate();
            stockWriteLock.unlock();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void deleteStockById(Stock stock) {
        try {
            String deleteStockById = "delete from stock where id = ?";
            stockWriteLock.lock();
            PreparedStatement pst = DATABASE_CONNECTION.prepareStatement(deleteStockById);
            pst.setInt(1, stock.getStockId());
            pst.executeUpdate();
            stockWriteLock.unlock();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
