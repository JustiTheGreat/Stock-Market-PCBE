import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Publisher {
    private final String exchangeName;

    public Publisher(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public void publish(Object object) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(exchangeName, "fanout");

        byte[] bytes = MyConnection.objectToByteArray(object);
        channel.basicPublish(exchangeName,"", null, bytes);
        System.out.println(" [x] Sent '" + bytes + "'");
    }
}
