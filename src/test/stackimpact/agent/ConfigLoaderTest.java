package test.stackimpact.agent;

import com.stackimpact.agent.*;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class ConfigLoaderTest {
    @Test
    public void load() throws Exception {
        TestAPI.start(8090, "config");
        TestAPI.setResponsePayload("{\"agent_enabled\": \"yes\", \"profiling_disabled\": \"yes\"}");

        Agent agent = Agent.getInstance();
        agent.setDashboardAddress("http://localhost:8090");
        agent.setAgentKey("key");
        agent.setDebugMode(true);
        agent.start("key1", "app1");

        agent.getConfigLoader().load();

        TestAPI.stop();

        assertTrue(agent.isProfilingDisabled());
    }

}
