
import com.stackimpact.agent.StackImpact;

import java.util.Random;


public class DemoApp {

	private static Object lock = new Object();

    public static void main(String args[]) throws Exception {
    	StackImpact.start(System.getenv("AGENT_KEY"), "ExampleJavaApp");

		while (true) {
			long startTS = System.currentTimeMillis();

			simulateCPULoad(20);

			Thread t = new Thread(new Runnable() {
	    		public void run() {
	    			try {
	    				synchronized(lock) {
							Thread.sleep(80);
	    				}
					}
					catch(Exception ex) {
						ex.printStackTrace();
					}
		        }
		    });
	    	t.start();

			simulateLockWait();

			t.join();

			long timeout = 1000 - (System.currentTimeMillis() - startTS) - 10;
			if (timeout > 0) {
				Thread.sleep(timeout);
			}
		}
    }


	public static void simulateCPULoad(int usage) throws Exception {
		Random rand = new Random();
		for (int i = 0; i < usage * 1000000; i++) {
			rand.nextInt(100000);
		}
	}


	public static void simulateLockWait() throws Exception {
		Thread.sleep(10);
    	synchronized(lock) {
    		Thread.sleep(10);
    	}
	}

}