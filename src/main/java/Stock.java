import java.io.Serializable;

public class Stock implements Serializable,EventsAndConstants {
    private String clientName;
    private final long ID;
    private final int type;
    private String actionName;
    private int actionNumber;
    private int pricePerAction;

    public Stock(String clientName, int type, String actionName, int actionNumber, int pricePerAction) {
        this.type = type;
        this.actionName = actionName;
        this.actionNumber = actionNumber;
        this.pricePerAction = pricePerAction;
        this.clientName = clientName;
        ID = System.currentTimeMillis();
    }

    public long getID() {
        return ID;
    }

    public String getActionName() {
        return actionName;
    }

    public int getActionNumber() {
        return actionNumber;
    }

    public int getPricePerAction() {
        return pricePerAction;
    }

    public String getClientName() {
        return clientName;
    }

    public void set(String actionName, int actionNumber, int pricePerAction, String clientName) {
        this.actionName = actionName;
        this.actionNumber = actionNumber;
        this.pricePerAction = pricePerAction;
        this.clientName = clientName;
    }

    public boolean isOffer() {
        return type == OFFER;
    }

    public boolean isBid() {
        return type == BID;
    }

    public boolean hasClient(Client client) {
        return client.equals(this.clientName);
    }

    public boolean matchesPriceWith(Stock stock) {
        return pricePerAction == stock.getPricePerAction();
    }

    public boolean matchesClientWith(Stock stock) {
        return this.getClientName().equals(stock.getClientName());
    }

    @Override
    public String toString() {
        return "Bursa{"
                + "ID=" + ID
                + ", type=" + type
                + ", actionName=" + actionName
                + ", actionNumber=" + actionNumber
                + ", pricePerAction=" + pricePerAction
                + ", client=" + clientName
                + '}';
    }
}
