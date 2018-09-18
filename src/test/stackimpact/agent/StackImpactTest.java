package test.stackimpact.agent;

import com.stackimpact.agent.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class StackImpactTest {
    @Test
    public void start() throws Exception {
        StackImpact.setDebugMode(true);
        StackImpact.start("key1", "app1");

        assertTrue(Agent.getInstance().isAgentStarted());
    }    
}
