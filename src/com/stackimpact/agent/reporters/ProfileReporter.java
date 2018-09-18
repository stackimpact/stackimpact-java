package com.stackimpact.agent.reporters;


import java.util.Timer;
import java.util.TimerTask;
import java.util.Random;

import com.stackimpact.agent.*;
import com.stackimpact.agent.model.*;
import com.stackimpact.agent.profilers.*;


public class ProfileReporter {

    private Agent agent;
    private Profiler profiler;
    private ProfilerConfig config;
    private Timer spanTimer;
    private Timer reportTimer;
    private boolean isSetup = false;
    private boolean isStarted = false;
    private long profileStartTS;
    private long profileDuration;
    private boolean isSpanActive;
    private int spanCount;
    private long spanStart;
    private Timer spanTimeoutTimer;


    public ProfileReporter(Agent agent, Profiler profiler, ProfilerConfig config) {
        this.agent = agent;
        this.profiler = profiler;
        this.config = config;
    }


    public void setup() {
        if (isSetup) {
            return;
        }

        if (!profiler.setupProfiler()) {
            agent.logInfo(config.logPrefix + ": profiler disabled.");
            return;
        }

        isSetup = true;
    }


    public void destroy() {
        if (!isSetup) {
            return;
        }

        profiler.destroyProfiler();
        agent.logInfo(config.logPrefix + ": profiler destroyed.");
    }


    public void start() {
        if (!isSetup) {
            return;
        }

        if (isStarted) {
            return;
        }
        isStarted = true;

        reset();

        if (agent.isAutoProfilingMode()) {
            final Random rand = new Random(AgentUtils.millis());

            spanTimer = new Timer();
            spanTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(rand.nextInt(config.spanInterval - config.maxSpanDuration));
                        startProfiling();
                    }
                    catch(Exception ex) {
                        agent.logException(ex);
                    }
                }
            }, config.spanInterval, config.spanInterval);

            reportTimer = new Timer();
            reportTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        report(false);
                    }
                    catch(Exception ex) {
                        agent.logException(ex);
                    }
                }
            }, config.reportInterval, config.reportInterval);
        }
    }


    public void stop() {
        if (!isSetup) {
            return;
        }

        if (!isStarted) {
            return;
        }
        isStarted = false;

        if (spanTimer != null) {
            spanTimer.cancel();
        }

        if (reportTimer != null) {
            reportTimer.cancel();
        }
    }


    public void reset() {
        profiler.resetProfiler();
        profileStartTS = AgentUtils.millis();
        profileDuration = 0;
        spanCount = 0;
    }


    public synchronized boolean startProfiling() throws Exception {
        if (!isStarted) {
          return false;
        }

        if (spanCount > config.maxSpanCount) {
          agent.logInfo(config.logPrefix + ": max recording count reached.");
          return false;
        }

        if (profileDuration > config.maxProfileDuration * 1e6) {
          agent.logInfo(config.logPrefix + ": max profiling duration reached.");
          return false;
        }

        if (!agent.getProfilerLock().compareAndSet(false, true)) {
          agent.logInfo(config.logPrefix + ": profiler lock exists.");
          return false;
        }

        agent.logInfo(config.logPrefix + ": started.");

        try {
            profiler.startProfiler();
            spanStart = AgentUtils.nanos();
            spanCount++;
            isSpanActive = true;

            spanTimeoutTimer = new Timer();
            spanTimeoutTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        stopProfiling();
                    }
                    catch(Exception ex) {
                        agent.logException(ex);
                    }
                }
            }, config.maxSpanDuration);
        }
        catch(Exception ex) {
          agent.logException(ex);
        }

        return true;
    }


    public synchronized boolean stopProfiling() throws Exception {
        if (!isSpanActive) {
            return false;
        }
        isSpanActive = false;

        spanTimeoutTimer.cancel();
        profileDuration += AgentUtils.nanos() - spanStart;

        try {
            profiler.stopProfiler();

            agent.logInfo(config.logPrefix + ": stopped.");
        }
        catch(Exception ex) {
          agent.logException(ex);
        }

        agent.getProfilerLock().set(false);

        return true;
    }


    public synchronized void report(boolean withInterval) throws Exception {
        if (!isStarted) {
          return;
        }

        if (withInterval) {
            if (profileStartTS > AgentUtils.millis() - config.reportInterval) {
                return;
            } 
            else if (profileStartTS < AgentUtils.millis() - 2 * config.reportInterval) {
              reset();
              return;
            }
        }

        if (profileDuration == 0) {
            return;
        }

        agent.logInfo(config.logPrefix + ": reporting profile.");

        ProfileData profileData = profiler.buildProfile(profileDuration);

        Metric metric = new Metric(Metric.TYPE_PROFILE, profileData.category, profileData.name, profileData.unit);
        metric.createMetricID(agent);
        metric.createMeasurement(Metric.TRIGGER_TIMER, profileData.profile.measurement, profileData.unitInterval, profileData.profile);
        agent.getMessageQueue().add("metric", metric.toMap());

        reset();
    }

}