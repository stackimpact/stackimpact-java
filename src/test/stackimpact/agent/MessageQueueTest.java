package test.stackimpact.agent;

import com.stackimpact.agent.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class MessageQueueTest {
    @Test
    public void flushSuccess() throws Exception {
        TestAPI.start(8090, "upload");

        Agent agent = Agent.getInstance();
        agent.setDashboardAddress("http://localhost:8090");
        agent.setAgentKey("key");
        agent.setDebugMode(true);
        agent.start("key1", "app1");
        agent.getMessageQueue().reset();

        HashMap m = new HashMap();
        m.put("m0", 1);
        agent.getMessageQueue().add("t0", m);
        agent.getMessageQueue().getQueue().get(0).addedAt = AgentUtils.millis() - 20 * 60 * 1000;

        m = new HashMap();
        m.put("m1", 1);
        agent.getMessageQueue().add("t1", m);

        m = new HashMap();
        m.put("m2", 2);
        agent.getMessageQueue().add("t1", m);

        agent.getMessageQueue().flush();

        TestAPI.stop();

        HashMap requestData = (HashMap)APIRequest.fromJSON(TestAPI.getRequestPayload());

        HashMap payload = (HashMap)requestData.get("payload");
        ArrayList messages = (ArrayList)payload.get("messages");

        assertEquals(1.0, ((HashMap)(((HashMap)messages.get(0)).get("content"))).get("m1"));
        assertEquals(2.0, ((HashMap)(((HashMap)messages.get(1)).get("content"))).get("m2"));
        assertTrue(agent.getMessageQueue().getQueue().isEmpty());
    }


    @Test
    public void flushFail() throws Exception {
        Agent agent = Agent.getInstance();
        agent.setDashboardAddress("http://localhost:8091");
        agent.setAgentKey("key");
        agent.setDebugMode(true);
        agent.start("key1", "app1");
        agent.getMessageQueue().reset();

        HashMap m = new HashMap();
        m.put("m1", 1);
        agent.getMessageQueue().add("t1", m);

        m = new HashMap();
        m.put("m2", 2);
        agent.getMessageQueue().add("t1", m);

        agent.getMessageQueue().flush();

        assertEquals(1, ((HashMap)agent.getMessageQueue().getQueue().get(0).content).get("m1"));
        assertEquals(2, ((HashMap)agent.getMessageQueue().getQueue().get(1).content).get("m2"));
        assertTrue(agent.getMessageQueue().getBackoffSeconds() > 0);
    }
}
