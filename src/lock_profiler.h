
#ifndef _LOCK_PROFILER_H_
#define _LOCK_PROFILER_H_

#include <string>
#include <vector>
#include <time.h>
#include "agent.h"
#include "profile_recorder.h"

using namespace std;



static unsigned int g_seed = time(NULL);

inline void fast_srand( int seed ) {
    g_seed = seed;
}

inline int fastrand() {
    g_seed = (214013*g_seed+2531011);
    return (g_seed>>16)&0x7FFF;
}


class LockProfiler {
    public:
        ProfileRecorder* profile_recorder;
        long sampling_rate;
        long max_samples;
        atomic<long> num_samples{0};
        long start_ts = 0;

        LockProfiler() {
            profile_recorder = new ProfileRecorder();
        }

        bool SetupProfiler();
        void DestroyProfiler();
        void StartProfiler();
        void StopProfiler();
        void RecordLock(long enter_ts, long wait_time);
        static void JNICALL MonitorContendedEnter(jvmtiEnv* jvmti, JNIEnv* env, jthread thread, jobject object);
        static void JNICALL MonitorContendedEntered(jvmtiEnv* jvmti, JNIEnv* env, jthread thread, jobject object);
};

#endif // _LOCK_PROFILER_H_

