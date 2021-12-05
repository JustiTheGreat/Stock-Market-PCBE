package data_objects;

import constants.EventsAndConstants;

import java.io.Serializable;

public class Stock implements Serializable, EventsAndConstants {
    private final int stockId;
    private final int clientId;
    private final int type;
    private String actionName;
    private int actionNumber;
    private int pricePerAction;

    public Stock(int stockId, int clientId, int type, String actionName, int actionNumber, int pricePerAction) {
        this.stockId = stockId;
        this.clientId = clientId;
        this.type = type;
        this.actionName = actionName;
        this.actionNumber = actionNumber;
        this.pricePerAction = pricePerAction;
    }

    public int getStockId() {
        return this.stockId;
    }

    public int getClientId() {
        return clientId;
    }

    public int getType() {
        return type;
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

    public boolean isActive() {
        return type != INACTIVE;
    }

    public boolean matchesPriceWith(Stock stock) {
        return pricePerAction == stock.getPricePerAction();
    }

    public boolean matchesClientWith(Stock stock) {
        return clientId == stock.getClientId();
    }

    @Override
    public String toString() {
        return "Bursa{ "
                + "type=" + type + ", "
                + "actionName=" + actionName + ", "
                + "actionNumber=" + actionNumber + ", "
                + "pricePerAction=" + pricePerAction + ", "
                + "clientId=" + clientId + " }";
    }
}