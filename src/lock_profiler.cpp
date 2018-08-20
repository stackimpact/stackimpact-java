
#include <signal.h>
#include <sys/time.h>
#include <string>
#include <sstream>
#include <iostream>
#include "lock_profiler.h"

using namespace std;



bool LockProfiler::SetupProfiler() {
    return true;
}


void LockProfiler::DestroyProfiler() {
}


void LockProfiler::StartProfiler() {
    Agent* agent = &Agent::instance;

    profile_recorder->Reset();
    num_samples = 0;

    agent->jvmti->GetTime(&start_ts);

    agent->jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTER, NULL);
    agent->jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTERED, NULL);
}


void LockProfiler::StopProfiler() {
    Agent* agent = &Agent::instance;

    agent->jvmti->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTER, NULL);
    agent->jvmti->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTERED, NULL);
}


void LockProfiler::RecordLock(long enter_ts, long wait_time) {
    // only sample waits initiated during current profiling cycle
    if (enter_ts < start_ts) {
        return;
    }

    // ensure sample limit
    if (num_samples > max_samples) {
        return;
    }
    num_samples++;

    // only sample lock waits above sampling rate or close to it
    if (wait_time < sampling_rate * 1000 && wait_time > fastrand() % sampling_rate) {
        return;
    }

    profile_recorder->RecordSample((double)wait_time / 1000000, NULL);
}


void JNICALL LockProfiler::MonitorContendedEnter(jvmtiEnv* jvmti, JNIEnv* env, jthread thread, jobject object) {
    jlong enter_ts;    
    jvmti->GetTime(&enter_ts);
    jvmti->SetTag(object, enter_ts);
}


void JNICALL LockProfiler::MonitorContendedEntered(jvmtiEnv* jvmti, JNIEnv* env, jthread thread, jobject object) {
    jlong entered_ts;
    jvmti->GetTime(&entered_ts);

    jlong enter_ts;
    jvmti->GetTag(object, &enter_ts);

    if (enter_ts > 0) {
        Agent* agent = &Agent::instance;
        agent->lock_profiler->RecordLock(enter_ts, entered_ts - enter_ts);
    }
}


extern "C" JNIEXPORT jboolean JNICALL Java_com_stackimpact_agent_profilers_LockProfiler_setupLockProfiler(JNIEnv* env, jobject unused, jlong sampling_rate, jlong max_samples) {
    Agent* agent = &Agent::instance;

    agent->lock_profiler->sampling_rate = (long)sampling_rate;
    agent->lock_profiler->max_samples = (long)max_samples;

    return agent->lock_profiler->SetupProfiler();
}


extern "C" JNIEXPORT void JNICALL Java_com_stackimpact_agent_profilers_LockProfiler_destroyLockProfiler(JNIEnv* env, jobject unused) {
    Agent* agent = &Agent::instance;

    agent->lock_profiler->DestroyProfiler();
}


extern "C" JNIEXPORT void JNICALL Java_com_stackimpact_agent_profilers_LockProfiler_startLockProfiler(JNIEnv* env, jobject unused) {
    Agent* agent = &Agent::instance;

    agent->lock_profiler->StartProfiler();
}


extern "C" JNIEXPORT jobjectArray JNICALL Java_com_stackimpact_agent_profilers_LockProfiler_stopLockProfiler(JNIEnv* env, jobject unused) {
    Agent* agent = &Agent::instance;

    agent->lock_profiler->StopProfiler();
    return agent->lock_profiler->profile_recorder->ExportProfile(env);
}
