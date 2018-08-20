package com.stackimpact.agent;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class ConfigLoader {

    public final static long LOAD_DELAY = 2 * 1000;
    public final static long LOAD_INTERVAL = 120 * 1000;

    private Agent agent;

    private Timer loadTimer;

    public ConfigLoader(Agent agent) {
        this.agent = agent;
    }


    public void start() {
        loadTimer = new Timer();
        loadTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    load();
                }
                catch(Exception ex) {
                    agent.logException(ex);
                }
            }
        }, LOAD_DELAY, LOAD_INTERVAL);
    }


    public void stop() {
        if (loadTimer != null) {
            loadTimer.cancel();
        }
    }


    public void load() throws Exception {
        Object response = agent.getAPIRequest().post("config", new HashMap());
        if (response instanceof HashMap) {
            HashMap config = (HashMap)response;

            agent.setAgentEnabled("yes".equals(config.get("agent_enabled")));
            agent.setProfilingDisabled("yes".equals(config.get("profiling_disabled")));
        }

        if (agent.isAgentEnabled() && !agent.isProfilingDisabled()) {
            agent.getCPUReporter().start();
            agent.getLockReporter().start();
            agent.logInfo("Profiling enabled.");
        }
        else {
            agent.getCPUReporter().stop();
            agent.getLockReporter().stop();
            agent.logInfo("Profiling disabled.");
        }

        if (agent.isAgentEnabled()) {
            agent.getProcessReporter().start();
            agent.logInfo("Agent activated.");
        }
        else {
            agent.getProcessReporter().stop();
            agent.logInfo("Agent deactivated.");
        }
    }
}
