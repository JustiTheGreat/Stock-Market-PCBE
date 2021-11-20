import java.io.Serializable;

public class Transaction implements Serializable {
    private long ID;
    private Stock offer;
    private Stock bid;

    public Transaction(Stock offer, Stock bid) {
        this.offer = offer;
        this.bid = bid;
        ID = System.currentTimeMillis();
    }

    public long getID() {
        return ID;
    }

    public boolean oneOfTransactionMembersIs(int clientId) {
        return clientId == offer.getClientId()
                || clientId == bid.getClientId();
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "ID=" + ID +
                ", offer=" + offer +
                ", bid=" + bid +
                '}';
    }
}
