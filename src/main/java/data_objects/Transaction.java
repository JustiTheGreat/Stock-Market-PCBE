package data_objects;

import java.io.Serializable;

public class Transaction implements Serializable {
    private String offer_action_name;
    private String bid_action_name;
    private int offer_action_number;
    private int offer_price_action;
    private int bid_action_number;
    private int bid_price_action;
    private int offer_client_id;
    private int bid_client_id;


    public Transaction(String offer_action_name, int offer_action_number, int offer_price_action, String bid_action_name, int bid_action_number, int bid_price_action, int offer_client_id, int bid_client_id) {
        this.offer_action_name = offer_action_name;
        this.bid_action_name = bid_action_name;
        this.offer_action_number = offer_action_number;
        this.offer_price_action = offer_price_action;
        this.bid_action_number = bid_action_number;
        this.bid_price_action = bid_price_action;
        this.offer_client_id = offer_client_id;
        this.bid_client_id = bid_client_id;
    }




    public boolean oneOfTransactionMembersIs(int clientId) {
        return clientId == offer_client_id
                || clientId == bid_client_id;
    }

    @Override
    public String toString() {
        return "Transaction{ " +
                "offer =" + offer_action_name + "," + offer_action_number + "," + offer_price_action  +
                ", bid =" + bid_action_name + "," + bid_action_number + "," + bid_price_action +
                " }\n";
    }
}