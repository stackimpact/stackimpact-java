package com.stackimpact.agent;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import com.stackimpact.agent.profilers.*;
import com.stackimpact.agent.reporters.*;


public class Agent {
    public final static String VERSION = "1.0.2";
    public final static String SAAS_DASHBOARD_ADDRESS = "https://agent-api.stackimpact.com";

    private static Agent instance;

    private long runTS;
    private String runID;

    private APIRequest apiRequest;
    private ConfigLoader configLoader;
    private MessageQueue messageQueue;
    private ProfileReporter cpuReporter;
    private ProfileReporter lockReporter;
    private ProcessReporter processReporter;

    private boolean isAgentStarted = false;
    private boolean isAgentDestroyed = false;
    private boolean isAgentEnabled = false;
    private boolean isAutoProfilingMode = true;
    private boolean isProfilingDisabled = false;
    private AtomicBoolean profilerLock = new AtomicBoolean(false);
    private AtomicBoolean spanLock = new AtomicBoolean(false);

    private String dashboardAddress;
    private String agentKey;
    private String appName;
    private String appVersion;
    private String appEnvironment;
    private String hostName;
    private boolean isCPUProfilerDisabled = false;
    private boolean isAllocationProfilerDisabled = false;
    private boolean isLockProfilerDisabled = false;
    private boolean isDebugMode = false;
    private boolean isAgentFramesEnabled = false;

    private Thread shutdownHook;


    private Agent() {
    }


    public static Agent getInstance() {
        if (instance != null) {
            return instance;
        }

        instance = new Agent();
        return instance;
    }


    public long getRunTS() {
        return runTS;
    }


    public String getRunID() {
        return runID;
    }


    public APIRequest getAPIRequest() {
        return apiRequest;
    }


    public ConfigLoader getConfigLoader() {
        return configLoader;
    }


    public MessageQueue getMessageQueue() {
        return messageQueue;
    }


    public ProcessReporter getProcessReporter() {
        return processReporter;
    }


    public ProfileReporter getCPUReporter() {
        return cpuReporter;
    }

    public ProfileReporter getLockReporter() {
        return lockReporter;
    }


    public boolean isAgentStarted() {
        return isAgentStarted;
    }


    public boolean isAgentDestroyed() {
        return isAgentStarted;
    }


    public boolean isAgentEnabled() {
        return isAgentEnabled;
    }


    public void setAgentEnabled(boolean isAgentEnabled) {
        this.isAgentEnabled = isAgentEnabled;
    }


    public boolean isAutoProfilingMode() {
        return isAutoProfilingMode;
    }


    public void setAutoProfilingMode(boolean isAutoProfilingMode) {
        this.isAutoProfilingMode = isAutoProfilingMode;
    }


    public boolean isProfilingDisabled() {
        return isProfilingDisabled;
    }


    public void setProfilingDisabled(boolean isProfilingDisabled) {
        this.isProfilingDisabled = isProfilingDisabled;
    }


    public AtomicBoolean getProfilerLock() {
        return profilerLock;
    }


    public AtomicBoolean getSpanLock() {
        return spanLock;
    }


    public String getDashboardAddress() {
        return dashboardAddress;
    }


    public void setDashboardAddress(String dashboardAddress) {
        this.dashboardAddress = dashboardAddress;
    }


    public String getAgentKey() {
        return agentKey;
    }


    public void setAgentKey(String agentKey) {
        this.agentKey = agentKey;
    }


    public String getAppName() {
        return appName;
    }


    public void setAppName(String appName) {
        this.appName = appName;
    }


    public String getAppVersion() {
        return appVersion;
    }


    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }


    public String getAppEnvironment() {
        return appEnvironment;
    }


    public void setAppEnvironment(String appEnvironment) {
        this.appEnvironment = appEnvironment;
    }    


    public String getHostName() {
        return hostName;
    }


    public void setHostName(String hostName) {
        this.hostName = hostName;
    }


    public boolean isCPUProfilerDisabled() {
        return this.isCPUProfilerDisabled;
    }


    public void setCPUProfilerDisabled(boolean isCPUProfilerDisabled) {
        this.isCPUProfilerDisabled = isCPUProfilerDisabled;
    }


    public boolean isAllocationProfilerDisabled() {
        return this.isAllocationProfilerDisabled;
    }


    public void setAllocationProfilerDisabled(boolean isAllocationProfilerDisabled) {
        this.isAllocationProfilerDisabled = isAllocationProfilerDisabled;
    }


    public boolean isLockProfilerDisabled() {
        return this.isLockProfilerDisabled;
    }


    public void setLockProfilerDisabled(boolean isLockProfilerDisabled) {
        this.isLockProfilerDisabled = isLockProfilerDisabled;
    }


    public boolean isDebugMode() {
        return isDebugMode;
    }


    public void setDebugMode(boolean isDebugMode) {
        this.isDebugMode = isDebugMode;
    }

    
    public boolean isAgentFramesEnabled() {
        return this.isAgentFramesEnabled;
    }


    public void setAgentFramesEnabled(boolean isAgentFramesEnabled) {
        this.isAgentFramesEnabled = isAgentFramesEnabled;
    }


    public synchronized void start(String agentKey, String appName) {
        if (isAgentStarted) {
            logInfo("The agent has already been started.");
            return;
        }

        if (AgentUtils.getJavaVersion() < 1.7) {
            logError("Java versions older than 1.7 are not supported.");
            return;
        }

        try {
            runTS = AgentUtils.timestamp();
            runID = AgentUtils.generateUUID();
        }
        catch(Exception ex) {
            logException(ex);
            return;
        }

        if (agentKey == null || appName == null) {
            logError("Invalid initialization parameters");
            return;
        }

        if(dashboardAddress == null) {
            dashboardAddress = SAAS_DASHBOARD_ADDRESS;
        }

        this.agentKey = agentKey;
        this.appName = appName;

        if (hostName == null) {
            try {
                hostName = InetAddress.getLocalHost().getHostName();
            }
            catch(Exception ex) {
                hostName = "unknown";
                logException(ex);
            }
        }

        try {
            loadAgentLib();

            if (!attach(isDebugMode)) {
                logError("Attaching JVMTI agent failed.");
                return;
            }
        }
        catch (Exception ex) {
            logException(ex);
            return;
        }

        apiRequest = new APIRequest(this);
        configLoader = new ConfigLoader(this);
        messageQueue = new MessageQueue(this);
        processReporter = new ProcessReporter(this);

        ProfilerConfig cpuProfilerConfig = new ProfilerConfig();
        cpuProfilerConfig.logPrefix = "CPU profiler";
        cpuProfilerConfig.maxProfileDuration = 10 * 1000;
        cpuProfilerConfig.maxSpanDuration = 2 * 1000;
        cpuProfilerConfig.maxSpanCount = 30;
        cpuProfilerConfig.spanInterval = 16 * 1000;
        cpuProfilerConfig.reportInterval = 120 * 1000;
        cpuReporter = new ProfileReporter(this, new CPUProfiler(this), cpuProfilerConfig);
        cpuReporter.setup();

        ProfilerConfig lockProfilerConfig = new ProfilerConfig();
        lockProfilerConfig.logPrefix = "Lock profiler";
        lockProfilerConfig.maxProfileDuration = 10 * 1000;
        lockProfilerConfig.maxSpanDuration = 2 * 1000;
        lockProfilerConfig.maxSpanCount = 30;
        lockProfilerConfig.spanInterval = 16 * 1000;
        lockProfilerConfig.reportInterval = 120 * 1000;
        lockReporter = new ProfileReporter(this, new LockProfiler(this), lockProfilerConfig);
        lockReporter.setup();

        configLoader.start();
        messageQueue.start();

        shutdownHook = new Thread() {
            public void run() {
                try {
                    Agent.this.destroy();
                }
                catch (Exception ex) {
                    Agent.this.logException(ex);
                }
            }
        };

        Runtime.getRuntime().addShutdownHook(shutdownHook);

        isAgentStarted = true;
        logInfo("Agent started.");        
    }


    private native boolean attach(boolean isDebugMode);


    public void destroy() {
        if (!isAgentStarted) {
            logInfo("The agent has not been started.");
            return;
        }

        if (isAgentDestroyed) {
            logInfo("The agent has already been destroyed.");
            return;
        }

        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }
        catch(Exception ex) {            
        }

        cpuReporter.stop();
        lockReporter.stop();
        processReporter.stop();
        configLoader.stop();
        messageQueue.stop();

        cpuReporter.destroy();
        lockReporter.destroy();

        isAgentStarted = false;
        logInfo("Agent destroyed.");
    }


    private String extractResourceFromJar(String name) throws Exception {
        String filePath = AgentUtils.getSystemTempDir() + File.separator + name;
        logInfo("Extracting JVMTI agent to " + filePath + ".");

        File file = new File(filePath);
        if (file.exists()) {
            logInfo("Found extracted JVMTI agent.");
            return filePath;
        }
        file.setExecutable(true, true);

        InputStream in = getClass().getResourceAsStream(File.separator + name);
        if (in == null) {
            throw new Exception("Resource not found in JAR: " + name);
        }
        OutputStream out = new FileOutputStream(file);
        byte[] buf = new byte[1024];
        int len;
        while((len = in.read(buf)) > 0){
            out.write(buf, 0, len);
        }
        out.close();
        in.close();

        if(!System.getProperty("os.name").startsWith("Windows")) {
            Runtime.getRuntime().exec("chmod u+x " + filePath);
        }

        return filePath;
    }


    private void loadAgentLib() throws Exception {
        String agentTempDir = null;

        String os = AgentUtils.getOSTag();
        if (os == null) {
            throw new Exception("Cannot identify the OS.");
        }
        String agentLibName = 
            "libstackimpact-" + VERSION + "-" + os + "-x64" +
            (os != "win" ? ".so" : ".dll");
        String agentLibPath = extractResourceFromJar(agentLibName);

        logInfo("Loading JVM TI agent: " + agentLibPath);
        System.load(agentLibPath);
    }


    public ProfileSpan profile() {
        ProfileSpan span = new ProfileSpan(this);
        span.start();

        return span;
    }


    public void logMessage(String level, String message) {
        if (!isDebugMode) {
            return;
        }

        StringBuffer buf = new StringBuffer();

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.S");

        buf.append("[");
        buf.append(sdf.format(cal.getTime()));
        buf.append("] ");
        buf.append("StackImpact ");
        buf.append(VERSION);
        buf.append(": ");
        buf.append(level);
        buf.append(": ");
        buf.append(message);

        System.out.println(buf.toString());
    }


    public void logInfo(String message) {
        logMessage("INFO", message);
    }


    public void logError(String message) {
        logMessage("ERROR", message);
    }


    public void logException(Throwable t) {
        logMessage("EXCEPTION", t.getMessage());
        if (isDebugMode) {
            t.printStackTrace();
        }
    }
}
