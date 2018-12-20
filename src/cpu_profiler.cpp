
#include <signal.h>
#include <sys/time.h>
#include <string>
#include <sstream>
#include <iostream>
#include "cpu_profiler.h"

using namespace std;


void SignalHandler(int signum, siginfo_t *info, void *context) {
    if (info->si_code <= 0) {
        return;
    }

    Agent::instance.cpu_profiler->HandleSignal(context);
}


bool CPUProfiler::SetupProfiler() {
    if (!profile_recorder->sample_index.is_lock_free()) {
        Agent::instance.LogInfo("Lock-free atomics not available.");
        return false;
    }

    struct sigaction sa;
    sigemptyset(&sa.sa_mask);
    sa.sa_handler = NULL;
    sa.sa_sigaction = SignalHandler;
    sa.sa_flags = SA_RESTART | SA_SIGINFO;

    if (sigaction(SIGPROF, &sa, NULL) < 0) {
        Agent::instance.LogInfo("Signal handler setup failed.");
        return false;
    }

    return true;
}


void CPUProfiler::DestroyProfiler() {
    SetSamplingRate(0);

    struct sigaction sa;
    sigemptyset(&sa.sa_mask);
    sa.sa_handler = SIG_DFL;

    sigaction(SIGPROF, &sa, NULL);
}


void CPUProfiler::StartProfiler() {
    profile_recorder->Reset();

    SetSamplingRate(sampling_rate);
}


void CPUProfiler::StopProfiler() {
    SetSamplingRate(0);
}


void CPUProfiler::SetSamplingRate(long rate) {
    static struct itimerval timer;
    timer.it_interval.tv_sec = 0;
    timer.it_interval.tv_usec = rate;
    timer.it_value = timer.it_interval;
    if (setitimer(ITIMER_PROF, &timer, 0) == -1) {
        ostringstream msg;
        msg << "setitimer failed with error " << errno;
        Agent::instance.LogError(msg.str());
        return;
    }
}


void CPUProfiler::HandleSignal(void* context) {
    profile_recorder->RecordSample(0, context);
}


extern "C" JNIEXPORT jboolean JNICALL Java_com_stackimpact_agent_profilers_CPUProfiler_setupCPUProfiler(JNIEnv* env, jobject unused, jlong sampling_rate) {
    Agent* agent = &Agent::instance;

    agent->cpu_profiler->sampling_rate = (long)sampling_rate;

    return agent->cpu_profiler->SetupProfiler();
}


extern "C" JNIEXPORT void JNICALL Java_com_stackimpact_agent_profilers_CPUProfiler_destroyCPUProfiler(JNIEnv* env, jobject unused) {
    Agent* agent = &Agent::instance;

    agent->cpu_profiler->DestroyProfiler();
}


extern "C" JNIEXPORT void JNICALL Java_com_stackimpact_agent_profilers_CPUProfiler_startCPUProfiler(JNIEnv* env, jobject unused) {
    Agent* agent = &Agent::instance;

    agent->cpu_profiler->StartProfiler();
}


extern "C" JNIEXPORT jobjectArray JNICALL Java_com_stackimpact_agent_profilers_CPUProfiler_stopCPUProfiler(JNIEnv* env, jobject unused) {
    Agent* agent = &Agent::instance;

    agent->cpu_profiler->StopProfiler();
    return agent->cpu_profiler->profile_recorder->ExportProfile(env);
}
