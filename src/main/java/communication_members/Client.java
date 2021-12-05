package communication_members;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import connections.RabbitMQConnection;
import constants.EventsAndConstants;
import data_objects.Message;
import data_objects.Stock;
import data_objects.Transaction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import static connections.RabbitMQConnection.*;


public class Client extends JFrame implements Runnable, EventsAndConstants {
    public volatile boolean isRunning = true;
    private Thread thread;
    private Stock selectedStock;
    //GUI elements
    //the inputs from the left panel
    private final JComboBox<String> action = new JComboBox<>(ACTIONS);
    private final JComboBox<String> name = new JComboBox<>(ACTION_NAMES);
    private final JSpinner number = new JSpinner();
    private final JSpinner price = new JSpinner();
    private final JButton jButton = new JButton();
    //the lists from the right panel
    private final ArrayList<JList> jLists = new ArrayList<JList>() {{
        add(new JList());
        add(new JList());
        add(new JList());
        add(new JList());
    }};

    public Client(String username, int userId) {
        this.setTitle(username);
        this.setName(username + ";" + userId);
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    public Thread getThread() {
        return thread;
    }

    //for closing the thread externally and internally
    public void closeThread() {
        isRunning = false;
    }

    //when you close the window the thread ends
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING)
            this.closeThread();
    }

    //methods initiating the GUI
    private JLabel newJLabel(String text, Rectangle rectangle) {
        return new JLabel() {{
            setText(text);
            setBounds(rectangle);
        }};
    }

    private void JListSetup(JList jList) {
        jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        jList.setVisibleRowCount(-1);
        jList.setEnabled(false);
        jList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && jLists.get(1).getSelectedIndex() != -1) {
                JList editableJList = jLists.get(1);
                switch (action.getSelectedIndex()) {
                    case EDIT:
                        selectedStock = (Stock) editableJList.getModel().getElementAt(editableJList.getSelectedIndex());
                        name.setSelectedIndex(getIndexOfActionName((selectedStock.getActionName())));
                        number.setValue(selectedStock.getActionNumber());
                        price.setValue(selectedStock.getPricePerAction());
                        name.setEnabled(true);
                        number.setEnabled(true);
                        price.setEnabled(true);
                        jButton.setEnabled(true);
                    case DELETE:
                        selectedStock = (Stock) editableJList.getModel().getElementAt(editableJList.getSelectedIndex());
                        name.setSelectedIndex(getIndexOfActionName((selectedStock.getActionName())));
                        number.setValue(selectedStock.getActionNumber());
                        price.setValue(selectedStock.getPricePerAction());
                        jButton.setEnabled(true);
                        break;
                    default:
                }
                jButton.setEnabled(true);
            }
        });
    }

    private JPanel newJPanel(JScrollPane jScrollPane) {
        return new JPanel() {{
            setLayout(new BorderLayout());
            add(jScrollPane);
        }};
    }

    private void addTab(JTabbedPane jTabbedPane, int index, String title) {
        JScrollPane jScrollPane = new JScrollPane();
        JListSetup(jLists.get(index));
        jScrollPane.getViewport().add(jLists.get(index));
        jTabbedPane.addTab(title, newJPanel(jScrollPane));
    }

    private void initiateGUI() {
        this.setVisible(true);
        this.getContentPane().setLayout(new BorderLayout());
        this.setSize(1000, 400);
        this.setTitle("Client: " + getName());

        //left pannel inputs
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(null);
        leftPanel.setPreferredSize(new Dimension(300, 400));
        this.getContentPane().add(leftPanel, BorderLayout.WEST);

        leftPanel.add(newJLabel("Select action:", new Rectangle(20, 20, 100, 20)));
        leftPanel.add(newJLabel("Action name:", new Rectangle(20, 70, 100, 20)));
        leftPanel.add(newJLabel("Action number:", new Rectangle(20, 120, 100, 20)));
        leftPanel.add(newJLabel("Action price (€):", new Rectangle(20, 170, 100, 20)));

        action.setBounds(150, 20, 100, 20);
        leftPanel.add(action);
        action.addActionListener(e -> {
            switch (action.getSelectedIndex()) {
                case PUBLISH:
                case SUBSCRIBE:
                    jLists.get(1).clearSelection();
                    jLists.get(1).setEnabled(false);
                    name.setEnabled(true);
                    number.setEnabled(true);
                    price.setEnabled(true);
                    jButton.setEnabled(true);
                    break;
                case EDIT:
                case DELETE:
                    jLists.get(1).clearSelection();
                    jLists.get(1).setEnabled(true);
                    name.setEnabled(false);
                    number.setEnabled(false);
                    price.setEnabled(false);
                    jButton.setEnabled(false);
                    break;
                default:
            }
        });

        name.setBounds(150, 70, 100, 20);
        leftPanel.add(name);

        number.setModel(new SpinnerNumberModel(1, 1, 100, 1));
        number.setBounds(150, 120, 100, 20);
        leftPanel.add(number);

        price.setModel(new SpinnerNumberModel(1, 1, 100, 1));
        price.setBounds(150, 170, 100, 20);
        leftPanel.add(price);

        jButton.setText("Execute");
        jButton.setBounds(100, 220, 100, 20);
        leftPanel.add(jButton);

        //right panel tabs
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(680, 400));

        JTabbedPane jTabbedPane = new JTabbedPane();
        addTab(jTabbedPane, 0, "All Bids&Offers");
        addTab(jTabbedPane, 1, "My Bids&Offers");
        addTab(jTabbedPane, 2, "All Transactions History");
        addTab(jTabbedPane, 3, "My Transactions History");
        rightPanel.add(jTabbedPane);

        this.getContentPane().add(rightPanel, BorderLayout.EAST);
    }

    //server-client communication
    public synchronized void refreshStockTabs(ArrayList<Stock> allStocks) {
        DefaultListModel<Stock> all = new DefaultListModel<>();
        DefaultListModel<Stock> client = new DefaultListModel<>();
        allStocks.forEach(stock -> {
            all.addElement(stock);
            if (stock.getClientId() == Integer.parseInt(this.getName().split(";")[1]))
                client.addElement(stock);
        });
        jLists.get(0).setModel(all);
        jLists.get(1).setModel(client);
    }

    public synchronized void refreshTransactionTabs(ArrayList<Transaction> allTransactions) {
        DefaultListModel<Transaction> all = new DefaultListModel<>();
        DefaultListModel<Transaction> client = new DefaultListModel<>();
        allTransactions.forEach(transaction -> {
            all.addElement(transaction);
            if (transaction.oneOfTransactionMembersIs(Integer.parseInt(this.getName().split(";")[1])))
                client.addElement(transaction);
        });
        jLists.get(2).setModel(all);
        jLists.get(3).setModel(client);
    }

    public void subscribeToServer() throws IOException, TimeoutException {
        Connection connection = FACTORY.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(exchangeNameForServerToClients, "fanout");
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, exchangeNameForServerToClients, "");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            byte[] bytes = delivery.getBody();
            Message message = (Message) RabbitMQConnection.getInstance().ByteArrayToObject(bytes);
            switch (message.getSubject()) {
                case REFRESH_STOCKS:
                    new Thread(() -> refreshStockTabs(message.getStocks())).start();
                    break;
                case REFRESH_TRANSACTIONS:
                    new Thread(() -> refreshTransactionTabs(message.getTransactions())).start();
                    break;
                default:
                    System.out.println(this.getName() + " received wrong type of message!");
            }
            System.err.println(this.getName() + " received: " + message.getSubject());
        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
        });
    }

    @Override
    public void run() {
        initiateGUI();
        try {
            RabbitMQConnection.getInstance().publish(new Message(REFRESH, null, null, null), exchangeNameForClientsToServer);
            subscribeToServer();
            System.out.println("Client with name " + this.getName() + " started successfully!");
            while (isRunning) {
                if (jButton.getModel().isPressed()) {
                    while (jButton.getModel().isPressed()) {
                        System.out.print("");
                    }
                    Stock stock;
                    Message message;
                    switch (action.getSelectedIndex()) {
                        case PUBLISH:
                            stock = new Stock(-1, Integer.parseInt(this.getName().split(";")[1]), OFFER, (String) name.getSelectedItem(), (int) number.getValue(), (int) price.getValue());
                            message = new Message(PUBLISH, stock, null, null);
                            RabbitMQConnection.getInstance().publish(message, exchangeNameForClientsToServer);
                            break;
                        case SUBSCRIBE:
                            stock = new Stock(-1, Integer.parseInt(this.getName().split(";")[1]), BID, (String) name.getSelectedItem(), (int) number.getValue(), (int) price.getValue());
                            message = new Message(SUBSCRIBE, stock, null, null);
                            RabbitMQConnection.getInstance().publish(message, exchangeNameForClientsToServer);
                            break;
                        case EDIT:
                            selectedStock.set(Objects.requireNonNull(name.getSelectedItem()).toString(), (int) number.getValue(), (int) price.getValue());
                            message = new Message(EDIT, selectedStock, null, null);
                            RabbitMQConnection.getInstance().publish(message, exchangeNameForClientsToServer);
                            break;
                        case DELETE:
                            message = new Message(DELETE, selectedStock, null, null);
                            RabbitMQConnection.getInstance().publish(message, exchangeNameForClientsToServer);
                            break;
                        default:
                    }
                }
            }
            System.out.println("Client with name " + this.getName() + " stopped successfully!");
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}