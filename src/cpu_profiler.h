
#ifndef _CPU_PROFILER_H_
#define _CPU_PROFILER_H_

#include <string>
#include <vector>
#include <jvmti.h>
#include "agent.h"
#include "profile_recorder.h"

using namespace std;

class CPUProfiler {
    public:
        ProfileRecorder* profile_recorder;
        long sampling_rate;

        CPUProfiler() {
            profile_recorder = new ProfileRecorder();
        }

        bool SetupProfiler();
        void DestroyProfiler();
        void StartProfiler();
        void StopProfiler();
        void SetSamplingRate(long);
        void HandleSignal(void*);
};


#endif // _CPU_PROFILER_H_

