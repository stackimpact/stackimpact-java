package test.stackimpact.agent.profilers;

import com.stackimpact.agent.*;
import com.stackimpact.agent.profilers.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class CPUProfilerTest {
    @Test
    public void profile() throws Exception {
        final Agent agent = Agent.getInstance();
        agent.setDashboardAddress("http://localhost:8090");
        agent.setAgentKey("key");
        agent.setDebugMode(true);

        final CPUProfiler cpuProfiler = new CPUProfiler(agent);

        assertTrue(cpuProfiler.setupProfiler());

        final ArrayList found = new ArrayList();

        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    cpuProfiler.startCPUProfiler();

                    Thread.sleep(1000);

                    Record[] records = cpuProfiler.stopCPUProfiler();
                    for (Record record : records) {
                        assertTrue(record.numSamples > 0);

                        for (Frame frame : record.frames) {
                            if (frame.className.indexOf("CPUProfilerTest") != -1 &&
                                    frame.methodName.indexOf("profile") != -1) {
                                found.add(1);
                            }
                        }

                        agent.logInfo("Record");
                        agent.logInfo("num samples: " + record.numSamples);
                        agent.logInfo("total: " + record.total);
                        agent.logInfo("Frames");
                        for (Frame frame : record.frames) {
                            agent.logInfo(frame.toString());
                        }
                    }
                }
                catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        t.start();

        Random rand = new Random();
        for (int i = 0; i < 100000000; i++) {
            rand.nextInt(1000000);
        }

        t.join();

        cpuProfiler.destroyProfiler();

        assertTrue(!found.isEmpty());
    }
}
