public class Stock {
    private int type;
    private long ID;
    private String actionName;
    private int actionNumber;
    private int pricePerAction;
    private Client client;
    public int OFFER = 0;
    public int BID = 1;
    public int EDIT = 2;
    public int DELETE = 3;


    public Stock(int type, String actionName, int actionNumber, int pricePerAction, Client client) {
        this.type = type;
        this.actionName = actionName;
        this.actionNumber = actionNumber;
        this.pricePerAction = pricePerAction;
        this.client = client;
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
        return client.getName();
    }

    public void set(String actionName, int actionNumber, int pricePerAction, Client client) {
        this.actionName = actionName;
        this.actionNumber = actionNumber;
        this.pricePerAction = pricePerAction;
        this.client = client;
    }

    public boolean hasClient(Client client) {
        return client.equals(this.client);
    }

    public boolean isOffer() {
        return type == OFFER;
    }

    public boolean isBid() {
        return type == BID;
    }

    public boolean matchPriceWith(Stock stock) {
        return pricePerAction == stock.getPricePerAction();
    }

    @Override
    public String toString() {
        return "Bursa{" +
                "ID=" + ID +
                ", actionName='" + actionName + '\'' +
                ", actionNumber=" + actionNumber +
                ", pricePerAction=" + pricePerAction +
                ", client=" + client.getName() +
                '}';
    }
}