import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;

public class Client extends JFrame implements Runnable, EventsAndConstants, MyConnection {
    public volatile boolean isRunning = true;
    private Thread thread;
    //GUI elements
    //main window
    private final BorderLayout mainBorderLayout = new BorderLayout();
    private final BorderLayout rightPanelBorderLayout = new BorderLayout();
    private final JPanel leftPanel = new JPanel();
    private final JPanel rightPanel = new JPanel();
    //the input from the left panel
    private final JLabel jLabel1 = new JLabel();
    private final JLabel jLabel2 = new JLabel();
    private final JLabel jLabel3 = new JLabel();
    private final JLabel jLabel4 = new JLabel();
    private final JComboBox jComboBox1 = new JComboBox(ACTIONS);
    private final JComboBox jComboBox2 = new JComboBox(ACTION_NAMES);
    private final JSpinner jSpinner1 = new JSpinner();
    private final JSpinner jSpinner2 = new JSpinner();
    private final JButton jButton = new JButton();
    //the tabs from the right panel
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final JPanel tab1Panel = new JPanel();
    private final JPanel tab2Panel = new JPanel();
    private final JPanel tab3Panel = new JPanel();
    private final JPanel tab4Panel = new JPanel();
    private final JTextArea tab1TextArea = new JTextArea();
    private final JTextArea tab2TextArea = new JTextArea();
    private final JTextArea tab3TextArea = new JTextArea();
    private final JTextArea tab4TextArea = new JTextArea();
    private final JScrollPane tab1ScrollPane = new JScrollPane();
    private final JScrollPane tab2ScrollPane = new JScrollPane();
    private final JScrollPane tab3ScrollPane = new JScrollPane();
    private final JScrollPane tab4ScrollPane = new JScrollPane();

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

    //initiate the GUI
    private void inititiateGUI() {
        this.setVisible(true);
        this.getContentPane().setLayout(mainBorderLayout);
        this.setSize(1000, 400);
        this.setTitle("Client: " + this.getName());

        //left pannel inputs
        leftPanel.setLayout(null);
        leftPanel.setPreferredSize(new Dimension(300, 400));
        this.getContentPane().add(leftPanel, BorderLayout.WEST);

        jLabel1.setText("Select action:");
        jLabel1.setBounds(20, 20, 100, 20);
        leftPanel.add(jLabel1);

        jLabel2.setText("Action name:");
        jLabel2.setBounds(20, 70, 100, 20);
        leftPanel.add(jLabel2);

        jLabel3.setText("Action number:");
        jLabel3.setBounds(20, 120, 100, 20);
        leftPanel.add(jLabel3);

        jLabel4.setText("Action price (€):");
        jLabel4.setBounds(20, 170, 100, 20);
        leftPanel.add(jLabel4);

        jComboBox1.setBounds(150, 20, 100, 20);
        leftPanel.add(jComboBox1);

        jComboBox2.setBounds(150, 70, 100, 20);
        leftPanel.add(jComboBox2);

        jSpinner1.setModel(new SpinnerNumberModel(1, 1, 100, 1));
        jSpinner1.setBounds(150, 120, 100, 20);
        leftPanel.add(jSpinner1);

        jSpinner2.setModel(new SpinnerNumberModel(1, 1, 100, 1));
        jSpinner2.setBounds(150, 170, 100, 20);
        leftPanel.add(jSpinner2);

        jButton.setText("Execute");
        jButton.setBounds(100, 220, 100, 20);
        leftPanel.add(jButton);

        //right panel tabs
        rightPanel.setLayout(rightPanelBorderLayout);
        rightPanel.setPreferredSize(new Dimension(680, 400));
        this.getContentPane().add(rightPanel, BorderLayout.EAST);

        tab1Panel.setLayout(new BorderLayout());
        tab1Panel.add(tab1ScrollPane);
        tabbedPane.addTab("All Bids&Offers", tab1Panel);

        tab2Panel.setLayout(new BorderLayout());
        tab2Panel.add(tab2ScrollPane);
        tabbedPane.addTab("My Bids&Offers", tab2Panel);

        tab3Panel.setLayout(new BorderLayout());
        tab3Panel.add(tab3ScrollPane);
        tabbedPane.addTab("All Transactions History", tab3Panel);

        tab4Panel.setLayout(new BorderLayout());
        tab4Panel.add(tab4ScrollPane);
        tabbedPane.addTab("My Transactions History", tab4Panel);

        rightPanel.add(tabbedPane);

        //text areas
        tab1TextArea.setBackground(Color.gray);
        tab1TextArea.setSelectedTextColor(Color.blue);
        tab1TextArea.setEnabled(false);
        tab1ScrollPane.getViewport().add(tab1TextArea);

        tab2TextArea.setBackground(Color.gray);
        tab2TextArea.setSelectedTextColor(Color.blue);
        tab2TextArea.setEnabled(false);
        tab2ScrollPane.getViewport().add(tab2TextArea);

        tab3TextArea.setBackground(Color.gray);
        tab3TextArea.setSelectedTextColor(Color.blue);
        tab3TextArea.setEnabled(false);
        tab3ScrollPane.getViewport().add(tab3TextArea);

        tab4TextArea.setBackground(Color.gray);
        tab4TextArea.setSelectedTextColor(Color.blue);
        tab4TextArea.setEnabled(false);
        tab4ScrollPane.getViewport().add(tab4TextArea);
    }

    //server-client communication
    public synchronized void refreshStockTabs(ArrayList<Stock> allStocks) {
        tab1TextArea.setText("");
        tab2TextArea.setText("");
        allStocks.forEach(stock -> {
            tab1TextArea.append(INDENT + stock.toString() + "\n");
            if (stock.getClientId() == Integer.parseInt(this.getName().split(";")[1]))
                tab2TextArea.append(INDENT + stock.toString() + "\n");
        });
    }

    public synchronized void refreshTransactionTabs(ArrayList<Transaction> allTransactions) {
        tab3TextArea.setText("");
        tab4TextArea.setText("");
        allTransactions.forEach(transaction -> {
            tab3TextArea.append(INDENT + transaction.toString() + "\n");
            if (transaction.oneOfTransactionMembersIs(Integer.parseInt(this.getName().split(";")[1])))
                tab4TextArea.append(INDENT + transaction.toString() + "\n");
        });
    }

    public void subscribeToServer() throws IOException, TimeoutException {
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(exchangeNameForServerToClients, "fanout");
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, exchangeNameForServerToClients, "");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            byte[] bytes = delivery.getBody();
            Message message = null;
            try {
                message = (Message) MyConnection.ByteArrayToObject(bytes);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                System.exit(-1);
            }
            switch (message.getSubject()) {
                case REFRESH_STOCKS:
                    refreshStockTabs(message.getStocks());
                    break;
                case REFRESH_TRANSACTIONS:
                    refreshTransactionTabs(message.getTransactions());
                    break;
                default:
                    System.out.println(this.getName() + " received wrong message type!");
                    System.exit(-1);
            }
            System.err.println(this.getName() + " received: " + message.getSubject());
        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
        });
    }

    @Override
    public void run() {
        inititiateGUI();
        try {
            publish(new Message(REFRESH, null, null, null), exchangeNameForClientsToServer);
            subscribeToServer();
            System.out.println("Client with name " + this.getName() + " started succesfully!");
            while (isRunning) {
                if (jButton.getModel().isPressed()) {
                    while (jButton.getModel().isPressed()) {
                        System.out.print("");
                    }
                    Stock stock;
                    Message message;
                    switch (jComboBox1.getSelectedIndex()) {
                        case PUBLISH:
                            stock = new Stock(Integer.parseInt(this.getName().split(";")[1]), OFFER, (String) jComboBox2.getSelectedItem(), (int) jSpinner1.getValue(), (int) jSpinner2.getValue());
                            message = new Message(PUBLISH, stock, null, null);
                            publish(message, exchangeNameForClientsToServer);
                            break;
                        case SUBSCRIBE:
                            stock = new Stock(Integer.parseInt(this.getName().split(";")[1]), BID, (String) jComboBox2.getSelectedItem(), (int) jSpinner1.getValue(), (int) jSpinner2.getValue());
                            message = new Message(SUBSCRIBE, stock, null, null);
                            publish(message, exchangeNameForClientsToServer);
                            break;
                        case EDIT:
                            break;
                        case DELETE:
                            break;
                        default:
                    }
                }
            }
            System.out.println("Client with name " + this.getName() + " stopped succesfully!");
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}