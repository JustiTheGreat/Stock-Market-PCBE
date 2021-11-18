public interface EventsAndConstants {
    //stock type
    int OFFER = 0;
    int BID = 1;
    //message type
    int PUBLISH = 0;
    int SUBSCRIBE = 1;
    int EDIT = 2;
    int DELETE = 3;
    int REFRESH_STOCKS = 4;
    int REFRESH_TRANSACTIONS = 5;
    int REFRESH = 6;
    //strings
    String INDENT = "     ";
    String[] ACTIONS = {"OFFER", "BID", "EDIT", "DELETE"};
    String[] ACTION_NAMES = {"petrol", "masini", "turism"};

    default int getIndexOfActionName(String s) {
        for (int i = 0; i < ACTION_NAMES.length; i++)
            if (s.equals(ACTION_NAMES[i]))
                return i;
        return -1;
    }
}
