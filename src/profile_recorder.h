
#ifndef _PROFILE_RECORDER_H_
#define _PROFILE_RECORDER_H_

#include <string>
#include <atomic>
#include <vector>
#include "agent.h"

using namespace std;

#define MAX_SAMPLES 1000
#define MAX_FRAMES 100


typedef struct {
    jint bci;
    jmethodID method_id;
} ASGCT_CallFrame;

typedef struct {
    JNIEnv* env;
    jint num_frames;
    ASGCT_CallFrame* frames;
} ASGCT_CallTrace;

extern "C" void AsyncGetCallTrace(ASGCT_CallTrace *trace, jint depth, void* ucontext);

/*
// possible num_frames values
enum {
  ticks_no_Java_frame         =  0,
  ticks_no_class_load         = -1,
  ticks_GC_active             = -2,
  ticks_unknown_not_Java      = -3,
  ticks_not_walkable_not_Java = -4,
  ticks_unknown_Java          = -5,
  ticks_not_walkable_Java     = -6,
  ticks_unknown_state         = -7,
  ticks_thread_exit           = -8,
  ticks_deopt                 = -9,
  ticks_safepoint             = -10
};
*/


class Sample {
    public:
        ASGCT_CallFrame frames[MAX_FRAMES];
        ASGCT_CallTrace call_trace;
        double measurement;

        Sample() {
            call_trace = {Agent::instance.jni, MAX_FRAMES, frames};
        }
};


class Frame {
    public:
        string class_name;
        string method_name;
        int line_number;

        Frame() {
            line_number = 0;
        }
};


class Record {
    public:
        vector<Frame*> stack_trace;
        long num_samples;
        double total;

        Record() {
            num_samples = 0;
            total = 0;
        }

        void AddFrame(Frame* frame);
        long GenerateID();
};


class ProfileRecorder {
    public:
        Sample samples[MAX_SAMPLES];
        atomic<int> sample_index{0};

        void Reset();
        void RecordSample(double measurement, void* context);
        //vector<Record*>* BuildProfile();
        jobjectArray ExportProfile(JNIEnv* env);
};


#endif // _PROFILE_RECORDER_H_

