
import java.util.Random;


public class BasicApp {
    public static void main(String args[]) throws Exception {
		while(true) {
			long startTS = System.currentTimeMillis();

			Random rand = new Random();
			for (int i = 0; i < 20 * 1000000; i++) {
				rand.nextInt(100000);
			}

			long timeout = 1000 - (System.currentTimeMillis() - startTS);
			if (timeout > 0) {
				Thread.sleep(timeout);
			}
		}
    }
}