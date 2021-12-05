package data_objects;

import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable {
    private final int subject;
    private final Stock stock;
    private final ArrayList<Stock> stocks;
    private final ArrayList<Transaction> transactions;

    public Message(int subject, Stock stock, ArrayList<Stock> stocks, ArrayList<Transaction> transactions) {
        this.subject = subject;
        this.stock = stock;
        this.stocks = stocks;
        this.transactions = transactions;
    }

    public int getSubject() {
        return subject;
    }

    public Stock getStock() {
        return stock;
    }

    public ArrayList<Stock> getStocks() {
        return stocks;
    }

    public ArrayList<Transaction> getTransactions() {
        return transactions;
    }
}
