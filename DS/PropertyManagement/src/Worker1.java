import java.io.IOException;
import java.text.ParseException;

public class Worker1 {

	public static void main(String[] args) throws IOException, ParseException, ClassNotFoundException{
        Worker worker = new Worker(1);
        worker.start();
	}
	
}
