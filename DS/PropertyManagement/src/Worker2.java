import java.io.IOException;
import java.text.ParseException;

public class Worker2 {
	public static void main(String[] args) throws IOException, ParseException, ClassNotFoundException{
        Worker worker = new Worker(2);
        worker.start();
	}
}
