package com.stackimpact.agent.profilers;

import com.stackimpact.agent.*;

public interface Profiler {
    public boolean setupProfiler();
    public void destroyProfiler();
    public void resetProfiler();
    public void startProfiler();
    public void stopProfiler();
    public ProfileData buildProfile(long duration);
}
