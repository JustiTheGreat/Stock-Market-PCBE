import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.*;
import java.util.concurrent.TimeoutException;

public interface MyConnection {
    ConnectionFactory factory = new ConnectionFactory() {{
        setHost("localhost");
    }};
    String exchangeNameForClientsToServer = "CTS";
    String exchangeNameForServerToClients = "STC";

    static byte[] objectToByteArray(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    static Object ByteArrayToObject(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    default void publish(Object object, String exchangeName) throws IOException, TimeoutException {
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(exchangeName, "fanout");
        byte[] bytes = MyConnection.objectToByteArray(object);
        channel.basicPublish(exchangeName, "", null, bytes);
        System.err.println("Message " + ((Message) object).getSubject() + " sent to " + exchangeName);
    }
}
