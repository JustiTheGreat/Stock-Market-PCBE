import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {
    public static void main(String[] args) throws IOException, TimeoutException {
        Thread dispatcher = new Dispatcher();
        dispatcher.start();
//        Stock stock = new Stock("asdas",1,"asdasdas",1,1);
//        Subscriber subscriber = new Subscriber("name");
//        Publisher publisher = new Publisher("name");
//        System.out.println(stock.toString());
//        subscriber.subscribe();
//        publisher.publish(stock);
    }
}
