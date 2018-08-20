
#include <iostream>
#include <stdint.h>
#include <algorithm>
#include <unordered_map>
#include "profile_recorder.h"


using namespace std;



void ProfileRecorder::Reset() {
    sample_index = 0;
}


string FormatClassSignature(string class_sig) {
    if (class_sig.empty()) {
        return "";
    }

    if (class_sig.front() == 'L' && class_sig.back() == ';') {
        class_sig = class_sig.substr(1, class_sig.size() - 2);
        replace(class_sig.begin(), class_sig.end(), '/', '.');
        return class_sig;
    }

    return "";
}


Frame* CreateFrame(jmethodID method, jint bci) {
    char* method_name = NULL;
    jclass method_class;
    char* class_sig = NULL;
    Frame* frame = new Frame();

    jvmtiEnv* jvmti = Agent::instance.jvmti;
    jvmtiError error;

    error = jvmti->GetMethodName(method, &method_name, NULL, NULL);
    Agent::instance.CheckJVMTIError(jvmti, error, "GetMethodName");
    if (Agent::instance.CheckJVMTIError(jvmti, error, "GetMethodDeclaringClass")) {
        frame->method_name = method_name;
    }

    error = jvmti->GetMethodDeclaringClass(method, &method_class);
    if (Agent::instance.CheckJVMTIError(jvmti, error, "GetMethodDeclaringClass")) {
        error = jvmti->GetClassSignature(method_class, &class_sig, NULL);
        if (Agent::instance.CheckJVMTIError(jvmti, error, "GetClassSignature")) {
            frame->class_name = FormatClassSignature(class_sig);
        }
    }

    jint line_count = 0;
    jvmtiLineNumberEntry* line_table = NULL;
    error = jvmti->GetLineNumberTable(method, &line_count, &line_table);
    if (error == JVMTI_ERROR_NONE) {
        frame->line_number = line_table[0].line_number;
        for (int i = 1 ; i < line_count; i++) {
            if (bci < line_table[i].start_location) {
                break;
            }
            frame->line_number = line_table[i].line_number;
        }
    }

    jvmti->Deallocate((unsigned char*)method_name);
    jvmti->Deallocate((unsigned char*)class_sig);
    jvmti->Deallocate((unsigned char*)line_table);

    return frame;
}


void ProfileRecorder::RecordSample(double measurement, void* context) {
    Sample* sample = &samples[sample_index++ % MAX_SAMPLES];
    sample->measurement = measurement;

    AsyncGetCallTrace(&sample->call_trace, MAX_FRAMES, context);
}


size_t CallTraceHash(int num_frames, ASGCT_CallFrame* frames) {
    size_t h = 0;

    for (int i = 0; i < num_frames; i++) {
        uint64_t v = (uint64_t)frames[i].method_id;
        hash<uint64_t> hasher;
        h ^= hasher(v) + 0x9e3779b9 + (h<<6) + (h>>2);
    }

    return h;
}


jobjectArray ProfileRecorder::ExportProfile(JNIEnv* env) {
    Agent* agent = &Agent::instance;
    
    unordered_map<size_t, Record*> records;

    for (int j = 0; j < sample_index; j++) {
        Sample* sample = &samples[j];

        size_t key = CallTraceHash(sample->call_trace.num_frames, sample->call_trace.frames);
        Record* rec;
        auto iter = records.find(key);
        if (iter == records.end()) {
            rec = new Record();
            records.insert(make_pair(key, rec));

            for (int i = 0; i < sample->call_trace.num_frames; i++) {
                Frame* frame = CreateFrame(sample->call_trace.frames[i].method_id, sample->call_trace.frames[i].bci);
                rec->AddFrame(frame);
            }
        }
        else {
            rec = iter->second;
        }

        rec->num_samples++;
        rec->total += sample->measurement;
    }


    jclass record_class = env->FindClass("com/stackimpact/agent/profilers/Record");
    jmethodID record_constructor_id = env->GetMethodID(record_class, "<init>", "()V");
    jfieldID num_samples_id = env->GetFieldID(record_class, "numSamples", "J");
    jfieldID total_id = env->GetFieldID(record_class, "total", "D");
    jfieldID frames_id = env->GetFieldID(record_class, "frames", "[Lcom/stackimpact/agent/profilers/Frame;");

    jclass frame_class = env->FindClass("com/stackimpact/agent/profilers/Frame");
    jmethodID frame_constructor_id = env->GetMethodID(frame_class, "<init>", "()V");
    jfieldID class_name_id = env->GetFieldID(frame_class, "className", "Ljava/lang/String;");
    jfieldID method_name_id = env->GetFieldID(frame_class, "methodName", "Ljava/lang/String;");
    jfieldID line_number_id = env->GetFieldID(frame_class, "lineNumber", "I");

    jobjectArray records_arr = env->NewObjectArray(records.size(), record_class, NULL);

    int record_index = 0;
    for (auto kv : records) {
        Record* record = kv.second;

        jobject record_obj = env->NewObject(record_class, record_constructor_id);
        env->SetLongField(record_obj, num_samples_id, record->num_samples);
        env->SetDoubleField(record_obj, total_id, record->total);

        jobjectArray frames_arr = env->NewObjectArray(record->stack_trace.size(), frame_class, NULL);

        int frame_index = 0;
        for (auto frame : record->stack_trace) {
            jobject frame_obj = env->NewObject(frame_class, frame_constructor_id);

            env->SetObjectField(frame_obj, class_name_id, env->NewStringUTF(frame->class_name.c_str()));
            env->SetObjectField(frame_obj, method_name_id, env->NewStringUTF(frame->method_name.c_str()));
            env->SetIntField(frame_obj, line_number_id, frame->line_number);

            env->SetObjectArrayElement(frames_arr, frame_index, frame_obj);

            frame_index++;
        }

        env->SetObjectField(record_obj, frames_id, frames_arr);

        env->SetObjectArrayElement(records_arr, record_index, record_obj);

        record_index++;
    }


    for (auto kv : records) {
        Record* record = kv.second;

        for (auto frame : record->stack_trace) {
            delete frame;
        }
        record->stack_trace.clear();

        delete record;
    }
    records.clear();


    return records_arr;
}



void Record::AddFrame(Frame* frame) {
    stack_trace.push_back(frame);
}
