package test.stackimpact.agent;

import com.stackimpact.agent.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class APIRequestTest {
    @Test
    public void post() throws Exception {
        TestAPI.start(8090, "test");
        TestAPI.setResponsePayload("{\"a\": 1, \"b\": 2}");

        Agent agent = Agent.getInstance();
        agent.setDashboardAddress("http://localhost:8090");
        agent.setAgentKey("key");
        agent.setDebugMode(true);

        APIRequest apiRequest = new APIRequest(agent);

        HashMap data = new HashMap();
        data.put("a", "1");
        HashMap responseData = (HashMap)apiRequest.post("test", data);

        TestAPI.stop();

        String requestPayload = TestAPI.getRequestPayload();

        HashMap requestData = (HashMap)APIRequest.fromJSON(TestAPI.getRequestPayload());

        assertEquals("java", requestData.get("runtime_type"));
        assertEquals(System.getProperty("java.version"), requestData.get("runtime_version"));
        assertEquals(Agent.VERSION, requestData.get("agent_version"));
        assertEquals(agent.getAppName(), requestData.get("app_name"));
        assertEquals(agent.getAppVersion(), requestData.get("app_version"));
        assertEquals(agent.getAppEnvironment(), requestData.get("app_environment"));
        assertEquals(agent.getHostName(), requestData.get("host_name"));
        assertEquals(AgentUtils.getPID(), requestData.get("process_id"));
        assertEquals(agent.getRunID(), requestData.get("run_id"));
        //assertEquals(agent.getRunTS() + "", requestData.get("run_ts"));
        assertEquals("1", ((HashMap)requestData.get("payload")).get("a"));

        assertEquals(1.0, responseData.get("a"));
        assertEquals(2.0, responseData.get("b"));
    }


    @Test
    public void toJSON() throws Exception {
        HashMap obj = new LinkedHashMap();
        obj.put("key1", "value1\nvalue1");
        obj.put("key2", 2);
        obj.put("key3", 3.3);
        HashMap value4 = new LinkedHashMap();
        obj.put("key4_1", null);
        obj.put("key4", value4);
        ArrayList value5 = new ArrayList();
        value5.add("value5_5");
        value5.add(5.5);
        obj.put("key5", value5);

        APIRequest apiRequest = new APIRequest(Agent.getInstance());
        String json = APIRequest.toJSON(obj);
        assertEquals("{\"key1\":\"value1\\nvalue1\",\"key2\":2,\"key3\":3.3,\"key4_1\":null,\"key4\":{},\"key5\":[\"value5_5\",5.5]}", json);
    }


    @Test
    public void fromJSON() throws Exception {
        String json = "{\"key1\":\"value1\\nvalue1\",\"key2\":2,\"key5\":[\"value5_5\",5.5],\"key3\":3.3,\"key4\":{},\"key4_1\":null}";

        APIRequest apiRequest = new APIRequest(Agent.getInstance());
        Object obj = APIRequest.fromJSON(json);

        HashMap expectedObj = new HashMap();
        expectedObj.put("key1", "value1\nvalue1");
        expectedObj.put("key2", 2.0);
        expectedObj.put("key3", 3.3);
        HashMap value4 = new HashMap();
        expectedObj.put("key4_1", null);
        expectedObj.put("key4", value4);
        ArrayList value5 = new ArrayList();
        value5.add("value5_5");
        value5.add(5.5);
        expectedObj.put("key5", value5);

        assertTrue(expectedObj.equals(obj));
    }


    @Test
    public void toBase64() throws Exception {
        APIRequest apiRequest = new APIRequest(Agent.getInstance());
        String base64 = APIRequest.toBase64("42195b1f5273f4f1bbd57503cec51cbb65797d4d".getBytes());
        assertEquals("NDIxOTViMWY1MjczZjRmMWJiZDU3NTAzY2VjNTFjYmI2NTc5N2Q0ZA==", base64);
    }


}
