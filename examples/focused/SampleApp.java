
import java.util.Random;

import com.stackimpact.agent.StackImpact;
import com.stackimpact.agent.ProfileSpan;


public class SampleApp {
    public static void main(String args[]) throws Exception {
    	StackImpact.start("agent key here", "SampleJavaApp");

		while(true) {
			someRequestHandler();
		}
    }


    public static void someRequestHandler() {
    	ProfileSpan span = StackImpact.profile();

    	try {
			Random rand = new Random();
			for (int i = 0; i < 20 * 1000000; i++) {
				rand.nextInt(100000);
			}
			Thread.sleep(rand.nextInt(200));
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}

		span.stop();
    }
}