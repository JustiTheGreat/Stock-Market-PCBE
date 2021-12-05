package connections;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import data_objects.Message;

import java.io.*;
import java.util.concurrent.TimeoutException;

public class RabbitMQConnection {
    private static final RabbitMQConnection instance = new RabbitMQConnection();
    public static ConnectionFactory FACTORY = new ConnectionFactory() {{
        setHost("localhost");
    }};
    public static String exchangeNameForClientsToServer = "CTS";
    public static String exchangeNameForServerToClients = "STC";

    public static RabbitMQConnection getInstance(){
        return instance;
    }

    private synchronized byte[] objectToByteArray(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    public synchronized Object ByteArrayToObject(byte[] bytes) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream is = new ObjectInputStream(in);
            return is.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return null;
    }

    public synchronized void publish(Object object, String exchangeName) {
        try {
            Connection connection = FACTORY.newConnection();
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(exchangeName, "fanout");
            byte[] bytes = objectToByteArray(object);
            channel.basicPublish(exchangeName, "", null, bytes);
            System.err.println("Message " + ((Message) object).getSubject() + " sent to " + exchangeName);
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
