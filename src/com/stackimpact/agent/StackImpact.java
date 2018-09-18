package com.stackimpact.agent;

import java.util.Map;
import java.lang.instrument.Instrumentation;

public class StackImpact {

    public static String getDashboardAddress() {
        Agent agent = Agent.getInstance();
        return agent.getDashboardAddress();
    }


    public static void setDashboardAddress(String dashboardAddress) {
        Agent agent = Agent.getInstance();
        agent.setDashboardAddress(dashboardAddress);
    }


    public static String getAgentKey() {
        Agent agent = Agent.getInstance();
        return agent.getAgentKey();
    }


    public static void setAgentKey(String agentKey) {
        Agent agent = Agent.getInstance();
        agent.setAgentKey(agentKey);
    }


    public static String getAppName() {
        Agent agent = Agent.getInstance();
        return agent.getAppName();
    }


    public static void setAppName(String appName) {
        Agent agent = Agent.getInstance();
        agent.setAppName(appName);
    }


    public static String getAppVersion() {
        Agent agent = Agent.getInstance();
        return agent.getAppVersion();
    }


    public static void setAppVersion(String appVersion) {
        Agent agent = Agent.getInstance();
        agent.setAppVersion(appVersion);
    }


    public static String getAppEnvironment() {
        Agent agent = Agent.getInstance();
        return agent.getAppEnvironment();
    }


    public static void setAppEnvironment(String appEnvironment) {
        Agent agent = Agent.getInstance();
        agent.setAppEnvironment(appEnvironment);
    }


    public static String getHostName() {
        Agent agent = Agent.getInstance();
        return agent.getHostName();
    }


    public static void setHostName(String hostName) {
        Agent agent = Agent.getInstance();
        agent.setHostName(hostName);
    }


    public static boolean isAutoProfilingMode() {
        Agent agent = Agent.getInstance();
        return agent.isAutoProfilingMode();
    }


    public static void setAutoProfilingMode(boolean isAutoProfilingMode) {
        Agent agent = Agent.getInstance();
        agent.setAutoProfilingMode(isAutoProfilingMode);
    }


    public static boolean isCPUProfilerDisabled() {
        Agent agent = Agent.getInstance();
        return agent.isCPUProfilerDisabled();
    }


    public static void setCPUProfilerDisabled(boolean isCPUProfilerDisabled) {
        Agent agent = Agent.getInstance();
        agent.setCPUProfilerDisabled(isCPUProfilerDisabled);
    }


    public static boolean isAllocationProfilerDisabled() {
        Agent agent = Agent.getInstance();
        return agent.isAllocationProfilerDisabled();
    }


    public static void setAllocationProfilerDisabled(boolean isAllocationProfilerDisabled) {
        Agent agent = Agent.getInstance();
        agent.setAllocationProfilerDisabled(isAllocationProfilerDisabled);
    }


    public static boolean isLockProfilerDisabled() {
        Agent agent = Agent.getInstance();
        return agent.isLockProfilerDisabled();
    }


    public static void setLockProfilerDisabled(boolean isLockProfilerDisabled) {
        Agent agent = Agent.getInstance();
        agent.setLockProfilerDisabled(isLockProfilerDisabled);
    }


    public static boolean isDebugMode() {
        Agent agent = Agent.getInstance();
        return agent.isDebugMode();
    }


    public static void setDebugMode(boolean isDebugMode) {
        Agent agent = Agent.getInstance();
        agent.setDebugMode(isDebugMode);
    }

    public static boolean isAgentFramesEnabled() {
        Agent agent = Agent.getInstance();
        return agent.isAgentFramesEnabled();
    }


    public static void setAgentFramesEnabled(boolean isAgentFramesEnabled) {
        Agent agent = Agent.getInstance();
        agent.setAgentFramesEnabled(isAgentFramesEnabled);
    }


    public static void start(String agentKey, String appName) {
        Agent agent = Agent.getInstance();
        agent.start(agentKey, appName);
    }


    public static void destroy() {
        Agent agent = Agent.getInstance();
        agent.destroy();
    }


    public static ProfileSpan profile() {
        Agent agent = Agent.getInstance();
        return agent.profile();
    }


    private static String getJVMOption(Map<String, String> env, String key) {
        key = "si." + key;
        String val = System.getProperty(key);
        if (val != null) {
            return val;
        }
        else {
            return env.get(key.toUpperCase().replace('.', '_'));
        }
    }


    // only used if the agent is loaded via javaagent command line option
    public static void premain(String args, Instrumentation instrumentation) {
        Agent agent = Agent.getInstance();

        Map<String, String> env = System.getenv();

        String dashboardAddress = getJVMOption(env, "dashboard.address");
        if (dashboardAddress != null) {
            agent.setDashboardAddress(dashboardAddress);
        }

        String appVersion = getJVMOption(env, "app.version");
        if (appVersion != null) {
            agent.setAppVersion(appVersion);
        }

        String appEnvironment = getJVMOption(env, "app.environmnet");
        if (appEnvironment != null) {
            agent.setAppEnvironment(appEnvironment);
        }

        String hostName = getJVMOption(env, "host.name");
        if (hostName != null) {
            agent.setHostName(hostName);
        }

        String debugMode = getJVMOption(env, "debug.mode");
        if (debugMode != null) {
            agent.setDebugMode("true".equalsIgnoreCase(debugMode));
        }

        String isCPUProfilerDisabled = getJVMOption(env, "cpu.profiler.disabled");
        if (isCPUProfilerDisabled != null) {
            agent.setCPUProfilerDisabled("true".equalsIgnoreCase(isCPUProfilerDisabled));
        }

        String isLockProfilerDisabled = getJVMOption(env, "lock.profiler.disabled");
        if (isLockProfilerDisabled != null) {
            agent.setLockProfilerDisabled("true".equalsIgnoreCase(isLockProfilerDisabled));
        }

        String isAgentFramesEnabled = getJVMOption(env, "agent.frames.enabled");
        if (isAgentFramesEnabled != null) {
            agent.setAgentFramesEnabled("true".equalsIgnoreCase(isAgentFramesEnabled));
        }

        agent.start(getJVMOption(env, "agent.key"), getJVMOption(env, "app.name"));
    }
}
