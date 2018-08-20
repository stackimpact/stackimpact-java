package com.stackimpact.agent.profilers;

import com.stackimpact.agent.*;
import com.stackimpact.agent.model.*;

public class CPUProfiler implements Profiler {


    public final static long SAMPLING_RATE = 10000; // microseconds

	private Agent agent;

    private Breakdown profile;


	public CPUProfiler(Agent agent) {
		this.agent = agent;
	}


    public boolean setupProfiler() {
        if (agent.isCPUProfilerDisabled()) {
            return false;
        }

    	return setupCPUProfiler(SAMPLING_RATE);
    }


    public void destroyProfiler() {
    	destroyCPUProfiler();
    }


    public void resetProfiler() {
        profile = new Breakdown(Breakdown.TYPE_CALLGRAPH, "CPU call graph");
    }


    public void startProfiler() {
    	startCPUProfiler();
    }


    public void stopProfiler() {
    	Record[] records = stopCPUProfiler();

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
        long possibleSamples = 
                (duration / (SAMPLING_RATE * 1000)) * 
                Runtime.getRuntime().availableProcessors();

        profile.propagate();
        profile.evaluatePercent(possibleSamples);
        profile.filter(2, 1, 100);

        ProfileData profileData = new ProfileData();

        profileData.category = Metric.CATEGORY_CPU_PROFILE;
        profileData.name = Metric.NAME_CPU_USAGE;
        profileData.unit = Metric.UNIT_PERCENT;
        profileData.unitInterval = 0;
        profileData.profile = profile;

    	return profileData;
    }


    public native boolean setupCPUProfiler(long samplingRate);
    public native void startCPUProfiler();
    public native Record[] stopCPUProfiler();
    public native void destroyCPUProfiler();    
}
