package com.stackimpact.agent.profilers;

import com.stackimpact.agent.*;
import com.stackimpact.agent.model.*;

public class LockProfiler implements Profiler {


    public final static long SAMPLING_RATE = 1000; // microseconds
    public final static long MAX_SAMPLES = 1000;

    private Agent agent;

    private Breakdown profile;


    public LockProfiler(Agent agent) {
        this.agent = agent;
    }


    public boolean setupProfiler() {
        if (agent.isLockProfilerDisabled()) {
            return false;
        }

        return setupLockProfiler(SAMPLING_RATE, MAX_SAMPLES);
    }


    public void destroyProfiler() {
        destroyLockProfiler();
    }


    public void resetProfiler() {
        profile = new Breakdown(Breakdown.TYPE_CALLGRAPH, "Lock call graph");
    }


    public void startProfiler() {
        startLockProfiler();
    }


    public void stopProfiler() {
        Record[] records = stopLockProfiler();

        for (Record record : records) {
            /*agent.logInfo("Record");
            agent.logInfo("num samples: " + record.numSamples);
            agent.logInfo("total: " + record.total);
            agent.logInfo("Frames");
            for (Frame frame : record.frames) {
                agent.logInfo("class name: " + frame.className);
                agent.logInfo("method name:" + frame.methodName);               
            }*/

            Breakdown currentNode = profile;

            for (int i = record.frames.length - 1; i >= 0; i--) {
                Frame frame = record.frames[i];

                if (agent.isAgentFramesEnabled() || !AgentUtils.isAgentFrame(frame.className)) {
                    currentNode = currentNode.findOrAddChild(Breakdown.TYPE_CALLSITE, frame.toString());
                }
            }

            currentNode.measurement += record.total;
            currentNode.numSamples += record.numSamples;
        }
    }


    public ProfileData buildProfile(long duration) {
        profile.propagate();
        profile.normalize(duration / 1e9);
        profile.filter(2, 1, Long.MAX_VALUE);

        ProfileData profileData = new ProfileData();

        profileData.category = Metric.CATEGORY_LOCK_PROFILE;
        profileData.name = Metric.NAME_LOCK_WAIT;
        profileData.unit = Metric.UNIT_MILLISECOND;
        profileData.unitInterval = 1;
        profileData.profile = profile;

        return profileData;
    }


    public native boolean setupLockProfiler(long samplingRate, long maxSamples);
    public native void startLockProfiler();
    public native Record[] stopLockProfiler();
    public native void destroyLockProfiler();    
}
