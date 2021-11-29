import java.io.Serializable;

public class Stock implements Serializable,EventsAndConstants {
    private final int type;
    private String actionName;
    private int actionNumber;
    private int pricePerAction;
    private int clientId;
    private int stockId;

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }


    public Stock(int stockId, int clientId, int type, String actionName, int actionNumber, int pricePerAction) {
        this.stockId = stockId;
        this.clientId = clientId;
        this.type = type;
        this.actionName = actionName;
        this.actionNumber = actionNumber;
        this.pricePerAction = pricePerAction;
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



    public int getType() {
        return type;
    }



    public void set(String actionName, int actionNumber, int pricePerAction) {
        this.actionName = actionName;
        this.actionNumber = actionNumber;
        this.pricePerAction = pricePerAction;
    }

    public boolean isOffer() {
        return type == OFFER;
    }

    public boolean isBid() {
        return type == BID;
    }

//    public boolean hasClient(Client client) {
//        return client.equals(this.clientName);
//    }

    public boolean matchesPriceWith(Stock stock) {
        return pricePerAction == stock.getPricePerAction();
    }

    public boolean matchesClientWith(Stock stock) {
        return this.getClientId() == stock.getClientId();
    }

    @Override
    public String toString() {
        return "Bursa{" +
                ", type=" + type +
                ", actionName='" + actionName + '\'' +
                ", actionNumber=" + actionNumber +
                ", pricePerAction=" + pricePerAction +
                ", clientId=" + clientId +
                '}';
    }


    public int getStockId() {
        return this.stockId;
    }
}