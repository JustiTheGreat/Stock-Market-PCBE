import java.io.Serializable;

public class Transaction implements Serializable {
    private Stock offer;
    private Stock bid;

    public Transaction(Stock offer, Stock bid) {
        this.offer = offer;
        this.bid = bid;
    }


    public boolean oneOfTransactionMembersIs(int clientId) {
        return clientId == offer.getClientId()
                || clientId == bid.getClientId();
    }

    @Override
    public String toString() {
        return "Transaction{" +
                ", offer=" + offer +
                ", bid=" + bid +
                '}';
    }
}