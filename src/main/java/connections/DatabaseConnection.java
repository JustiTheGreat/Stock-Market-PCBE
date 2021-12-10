package connections;

import constants.EventsAndConstants;
import data_objects.Stock;
import data_objects.Transaction;

import java.sql.*;
import java.util.ArrayList;
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
        ThreadLocal<Integer> id = new ThreadLocal<Integer>() {{
            set(null);
        }};
        String selectUserByName = "select * from users where name = ?";
        userReadLock.lock();
        try {
            PreparedStatement pst = DATABASE_CONNECTION.prepareStatement(selectUserByName);
            pst.setString(1, username);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) id.set(rs.getInt(1));
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        } finally {
            userReadLock.unlock();
        }
        if (id.get() == null) return USER_ID_NOT_FOUND;
        return id.get();
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
        ThreadLocal<ArrayList<Stock>> stocks = ThreadLocal.withInitial(ArrayList::new);
        String selectAllStocks = "select * from stock";
        try {
            PreparedStatement pst = DATABASE_CONNECTION.prepareStatement(selectAllStocks);
            stockReadLock.lock();
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                if (rs.getInt(2) != EventsAndConstants.INACTIVE) {
                    stocks.get().add(new Stock(rs.getInt(1), rs.getInt(6), rs.getInt(2), rs.getString(3), rs.getInt(4), rs.getInt(5)));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        } finally {
            stockReadLock.unlock();
        }
        return stocks.get();
    }

    public Stock getStockById(int id) {
        ThreadLocal<Stock> stock = new ThreadLocal<Stock>() {{
            set(null);
        }};
        String selectStockById = "select * from stock where id = ?";
        stockReadLock.lock();
        try {
            PreparedStatement pst = DATABASE_CONNECTION.prepareStatement(selectStockById);
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                stock.set(new Stock(rs.getInt(1), rs.getInt(6), rs.getInt(2), rs.getString(3), rs.getInt(4), rs.getInt(5)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        } finally {
            stockReadLock.unlock();
        }
        return stock.get();
    }

    public ArrayList<Transaction> getAllTransactions() {
        ThreadLocal<ArrayList<Transaction>> transactions = ThreadLocal.withInitial(ArrayList::new);
        String selectAllTransactions = "select * from transaction";
        try {
            PreparedStatement pst = DATABASE_CONNECTION.prepareStatement(selectAllTransactions);
            transactionReadLock.lock();
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                transactions.get().add(new Transaction(getStockById(rs.getInt(2)), getStockById(rs.getInt(3))));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        } finally {
            transactionReadLock.unlock();
        }
        return transactions.get();
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
