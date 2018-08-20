
#include <string>
#include <sstream>
#include <iostream>
#include <dlfcn.h>
#include "agent.h"
#include "cpu_profiler.h"
#include "lock_profiler.h"
#include <chrono>
#include <thread>

using namespace std;

Agent Agent::instance;

void Agent::LogMessage(string level, string msg) {
    if (debug_mode) {
        cout << "StackImpact Agent: " << level << ": " << msg << endl;
    }
}


void Agent::LogInfo(string msg) {
    LogMessage("INFO", msg);
}


void Agent::LogError(string msg) {
    LogMessage("ERROR", msg);
}


bool Agent::CheckJVMTIError(jvmtiEnv *jvmti, jvmtiError error, string msg) {
    if (error != JVMTI_ERROR_NONE) {
        if (debug_mode) {
            char *error_str;
            error_str = NULL;
            jvmti->GetErrorName(error, &error_str);
            ostringstream error_message;
            error_message << "JVMTI: "
                    << (error_str == NULL ? "Unknown" : error_str)
                    << " (" << error << "): "
                    << msg;

            LogError(error_message.str());

            return false;
        }
    }

    return true;
}


void Agent::Init(JavaVM* vm) {
    Agent* agent = &Agent::instance;

    jvm = vm;

    int rc = jvm->GetEnv((void**)&agent->jni, JNI_VERSION_1_6);
    if (rc != JNI_OK && rc != JNI_EDETACHED) {
        agent->LogError("GetEnv, JNI_VERSION_1_6");
        return;
    }

    rc = jvm->GetEnv((void **)&agent->jvmti, JVMTI_VERSION_1_2);
    if (rc != JNI_OK) {
        agent->LogError("GetEnv, JVMTI_VERSION_1_2");
        return;
    }
}


void LoadMethodIDs(jvmtiEnv* jvmti, jclass klass) {
    jint num_methods;
    jmethodID* methods;
    if (jvmti->GetClassMethods(klass, &num_methods, &methods) == 0) {
        jvmti->Deallocate((unsigned char*)methods);
    }
}


void LoadAllMethodIDs(jvmtiEnv* jvmti) {
    jint num_classes;
    jclass* classes;
    if (jvmti->GetLoadedClasses(&num_classes, &classes) == 0) {
        for (int i = 0; i < num_classes; i++) {
            LoadMethodIDs(jvmti, classes[i]);
        }
        jvmti->Deallocate((unsigned char*)classes);
    }
}


void JNICALL ClassLoad(jvmtiEnv *jvmti_env, JNIEnv *jni_env, jthread thread,
        jclass klass) {
}


void JNICALL ClassPrepare(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
        jclass klass) {
    LoadMethodIDs(jvmti_env, klass);
}


void JNICALL CompiledMethodLoad(jvmtiEnv* jvmti, jmethodID method,
        jint code_size, const void* code_addr,
        jint map_length, const jvmtiAddrLocationMap* map,
        const void* compile_info) {
}


bool Agent::Attach() {
    if (is_attached) {
        return true;
    }

    cpu_profiler = new CPUProfiler();
    lock_profiler = new LockProfiler();

    if (dlsym(RTLD_DEFAULT, "AsyncGetCallTrace") == NULL) {
        LogError("AsyncGetCallTrace not available.");
        return false;
    }

    jvmtiError error;

    jvmtiCapabilities potentialCapabilities = {0};
    jvmti->GetPotentialCapabilities(&potentialCapabilities);

    jvmtiCapabilities capabilities = {0};
    jvmtiEventCallbacks callbacks = {0};

    callbacks.ClassLoad = ClassLoad;
    callbacks.ClassPrepare = ClassPrepare;

    if (potentialCapabilities.can_generate_compiled_method_load_events) {
        capabilities.can_generate_compiled_method_load_events = 1;
        callbacks.CompiledMethodLoad = CompiledMethodLoad;
    }
    else {
        LogInfo("Missing capability: can_generate_compiled_method_load_events");
    }

    if (potentialCapabilities.can_get_source_file_name) {
        capabilities.can_get_source_file_name = 1;
    }
    else {
        LogInfo("Missing capability: can_get_source_file_name");
    }

    if (potentialCapabilities.can_get_line_numbers) {
        capabilities.can_get_line_numbers = 1;
    }
    else {
        LogInfo("Missing capability: can_get_line_numbers");
    }

    if (potentialCapabilities.can_generate_monitor_events) {
        capabilities.can_generate_monitor_events = 1;
        callbacks.MonitorContendedEnter = LockProfiler::MonitorContendedEnter;
        callbacks.MonitorContendedEntered = LockProfiler::MonitorContendedEntered;
    }
    else {
        LogInfo("Missing capability: can_generate_monitor_events");
    }

    if (potentialCapabilities.can_tag_objects) {
        capabilities.can_tag_objects = 1;
    }
    else {
        LogInfo("Missing capability: can_tag_objects");
    }
    
    error = jvmti->AddCapabilities(&capabilities);
    CheckJVMTIError(jvmti, error, "AddCapabilities");

    error = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    CheckJVMTIError(jvmti, error, "SetEventCallbacks");

    jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_LOAD, NULL);
    jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_PREPARE, NULL);
    jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_COMPILED_METHOD_LOAD, NULL);
    //error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_EXCEPTION, NULL);
    CheckJVMTIError(jvmti, error, "SetEventNotificationMode");

    // Initialize methodIDs by calling GetClassMethods, needed for AsyncGetCallTrace to function.
    LoadAllMethodIDs(jvmti);
    jvmti->GenerateEvents(JVMTI_EVENT_DYNAMIC_CODE_GENERATED);
    jvmti->GenerateEvents(JVMTI_EVENT_COMPILED_METHOD_LOAD);

    is_attached = true;
    LogInfo("The JVMTI agent has been attached successfuly.");

    return true;
}


extern "C" JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
    // the agent is not loaded via agentpath, it's loaded from java agent
    return JNI_ERR;
}


extern "C" JNIEXPORT jint JNICALL Agent_OnAttach(JavaVM *jvm, char *options, void *reserved) {
    return JNI_ERR;
}


extern "C" JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm) {
    Agent::instance.LogInfo("Agent_OnUnload");
}


extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* jvm, void* reserved) {
    Agent* agent = &Agent::instance;
    
    agent->Init(jvm);

    return JNI_VERSION_1_6;
}


extern "C" JNIEXPORT jboolean JNICALL Java_com_stackimpact_agent_Agent_attach(JNIEnv* env, jobject unused, jboolean debug_mode) {
    Agent* agent = &Agent::instance;
    
    agent->jni = env;
    agent->debug_mode = (bool)debug_mode;

    return agent->Attach();
}
