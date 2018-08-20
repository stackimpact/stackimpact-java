
#ifndef _AGENT_H_
#define _AGENT_H_

#include <string>
#include <jvmti.h>

using namespace std;


class CPUProfiler;
class AllocationProfiler;
class LockProfiler;

class Agent {
	public:
		bool debug_mode;
		bool is_attached;
		CPUProfiler* cpu_profiler;
		AllocationProfiler* allocation_profiler;
		LockProfiler* lock_profiler;

		Agent() {
			debug_mode = false;
			is_attached = false;
		}

		static Agent instance;

		JavaVM* jvm;
		JNIEnv* jni;
		jvmtiEnv* jvmti;

		void Init(JavaVM* jvm);
		bool Attach();
		bool CheckJVMTIError(jvmtiEnv* jvmti, jvmtiError error, string msg);
		void LogMessage(string level, string msg);
		void LogInfo(string msg);
		void LogError(string msg);
};


#endif // _AGENT_H_

