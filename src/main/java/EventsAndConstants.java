public interface EventsAndConstants {
    //stock type
    int OFFER = 0;
    int BID = 1;
    int OFFER_INACTIVE = -1;
    int BID_INACTIVE = -1;
    int GET_ACTIVE = 0;
    int GET_ALL = -2;
    //message type
    int PUBLISH = 0;
    int SUBSCRIBE = 1;
    int EDIT = 2;
    int DELETE = 3;
    int REFRESH_STOCKS = 4;
    int REFRESH_TRANSACTIONS =5;
    int REFRESH = 6;
    //strings
    String INDENT = "     ";
    String[] ACTIONS = {"OFFER", "BID", "EDIT", "DELETE"};
    String[] ACTION_NAMES = {"petrol", "masini", "turism"};
}
