package test.stackimpact.agent.profilers;

import com.stackimpact.agent.*;
import com.stackimpact.agent.profilers.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class LockProfilerTest {
    @Test
    public void profile() throws Exception {
        Agent agent = Agent.getInstance();
        agent.setDashboardAddress("http://localhost:8090");
        agent.setAgentKey("key");
        agent.setDebugMode(true);

        final LockProfiler lockProfiler = new LockProfiler(agent);

        assertTrue(lockProfiler.setupProfiler());

        final Object lock = new Object();

		lockProfiler.startLockProfiler();

		Thread t = new Thread(new Runnable() {
    		public void run() {
    			try {
    				synchronized(lock) {
						Thread.sleep(1000);
    				}
				}
				catch(Exception ex) {
					ex.printStackTrace();
				}
	        }
	    });
    	t.start();

		Thread.sleep(500);

    	synchronized(lock) {
    		Thread.sleep(10);
    	}

		t.join();

        ArrayList found = new ArrayList();
		Record[] records = lockProfiler.stopLockProfiler();
		for (Record record : records) {
			assertTrue(record.numSamples > 0);

			for (Frame frame : record.frames) {
				if (frame.className.indexOf("LockProfilerTest") != -1 &&
						frame.methodName.indexOf("profile") != -1) {
					found.add(1);
				}
			}

			/*agent.logInfo("Record");
			agent.logInfo("num samples: " + record.numSamples);
			agent.logInfo("total: " + record.total);
			agent.logInfo("Frames");
			for (Frame frame : record.frames) {
				agent.logInfo("class name: " + frame.className);
				agent.logInfo("method name:" + frame.methodName);				
			}*/
		}

		lockProfiler.destroyProfiler();

		assertTrue(!found.isEmpty());
    }
}
