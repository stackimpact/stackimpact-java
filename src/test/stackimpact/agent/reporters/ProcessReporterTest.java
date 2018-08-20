package test.stackimpact.agent.reporters;

import com.stackimpact.agent.*;
import com.stackimpact.agent.model.*;
import com.stackimpact.agent.reporters.*;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class ProcessReporterTest {
    @Test
    public void report() throws Exception {
        Agent agent = Agent.getInstance();
        agent.setDashboardAddress("http://localhost:8090");
        agent.setAgentKey("key");
        agent.setDebugMode(true);

        agent.getProcessReporter().reset();

        agent.getProcessReporter().report();
        agent.getProcessReporter().report();

        List<MessageQueue.Message> queue = agent.getMessageQueue().getQueue();
        Map<String, Double> measurements = new HashMap<String, Double>();
        for (MessageQueue.Message msg : queue) {
            Map metric = (Map)msg.content;
            String key = 
                    (String)metric.get("category") + 
                    (String)metric.get("name") + 
                    (String)metric.get("unit");
            measurements.put(key, ((Double)((Map)metric.get("measurement")).get("value")));
        }


        Double value = measurements.get(Metric.CATEGORY_CPU + Metric.NAME_CPU_USAGE + Metric.UNIT_PERCENT);
        assertTrue(value != null && value > 0);

        value = measurements.get(Metric.CATEGORY_MEMORY + Metric.NAME_USED_MEMORY + Metric.UNIT_BYTE);
        assertTrue(value != null && value > 0);

        value = measurements.get(Metric.CATEGORY_GC + Metric.NAME_GC_CYCLES + Metric.UNIT_NONE);
        assertTrue(value != null && value >= 0);

        value = measurements.get(Metric.CATEGORY_GC + Metric.NAME_GC_TIME + Metric.UNIT_MILLISECOND);
        assertTrue(value != null && value >= 0);

        value = measurements.get(Metric.CATEGORY_RUNTIME + Metric.NAME_THREAD_COUNT + Metric.UNIT_NONE);
        assertTrue(value != null && value > 0);

        /*Map memMetric = (Map)queue.get(0).content;
        assertEquals(Metric.CATEGORY_MEMORY, memMetric.get("category"));
        assertEquals(Metric.NAME_USED_MEMORY_SIZE, memMetric.get("name"));
        assertEquals(Metric.UNIT_BYTE, memMetric.get("unit"));
        assertTrue(((Double)((Map)memMetric.get("measurement")).get("value")) > 0);

        Map threadMetric = (Map)queue.get(1).content;
        assertEquals(Metric.CATEGORY_RUNTIME, threadMetric.get("category"));
        assertEquals(Metric.NAME_THREAD_COUNT, threadMetric.get("name"));
        assertEquals(Metric.UNIT_NONE, threadMetric.get("unit"));
        assertTrue(((Double)((Map)threadMetric.get("measurement")).get("value")) > 0);

        Map cpuMetric = (Map)queue.get(2).content;
        assertEquals(Metric.CATEGORY_CPU, cpuMetric.get("category"));
        assertEquals(Metric.NAME_CPU_USAGE, cpuMetric.get("name"));
        assertEquals(Metric.UNIT_PERCENT, cpuMetric.get("unit"));
        assertTrue(((Double)((Map)cpuMetric.get("measurement")).get("value")) > 0);

        System.out.println(queue.size());
        Map gcCyclesMetric = (Map)queue.get(queue.size() - 2).content;
        assertEquals(Metric.CATEGORY_GC, cpuMetric.get("category"));
        assertEquals(Metric.NAME_GC_CYCLES, cpuMetric.get("name"));
        assertEquals(Metric.UNIT_NONE, cpuMetric.get("unit"));
        assertTrue(((Double)((Map)cpuMetric.get("measurement")).get("value")) > 0);

        Map gcTimeMetric = (Map)queue.get(queue.size() - 1).content;
        assertEquals(Metric.CATEGORY_GC, cpuMetric.get("category"));
        assertEquals(Metric.NAME_GC_TIME, cpuMetric.get("name"));
        assertEquals(Metric.UNIT_NONE, cpuMetric.get("unit"));
        assertTrue(((Double)((Map)cpuMetric.get("measurement")).get("value")) > 0);*/
    }
}
