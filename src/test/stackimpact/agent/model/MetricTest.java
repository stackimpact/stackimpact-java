package test.stackimpact.agent.model;

import org.junit.Test;
import com.stackimpact.agent.*;
import com.stackimpact.agent.model.*;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class MetricTest {
    @Test
    public void createMeasurement() throws Exception {
        Agent agent = Agent.getInstance();
        agent.setDashboardAddress("http://localhost:8090");
        agent.setAgentKey("key");
        agent.setDebugMode(true);
        agent.start("key1", "app1");

        Metric m = new Metric(Metric.TYPE_COUNTER, Metric.CATEGORY_CPU, Metric.NAME_CPU_USAGE, Metric.UNIT_NONE);
        m.createMetricID(agent);

        m.createMeasurement(Metric.TRIGGER_TIMER, 100, 0);
        assertTrue(!m.hasMeasurement());

        m.createMeasurement(Metric.TRIGGER_TIMER, 110, 0);
        assertEquals(10.0, m.measurement.value, 0.0);

        m.createMeasurement(Metric.TRIGGER_TIMER, 115, 0);
        assertEquals(5.0, m.measurement.value, 0.0);
    }

}
