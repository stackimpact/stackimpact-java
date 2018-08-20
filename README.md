# StackImpact Java Profiler

## Overview

StackImpact is a production-grade performance profiler built for both production and development environments. It gives developers continuous and historical code-level view of application performance that is essential for locating CPU, memory allocation and I/O hot spots as well as latency bottlenecks. Included runtime metrics and error monitoring complement profiles for extensive performance analysis. Learn more at [stackimpact.com](https://stackimpact.com/).

![dashboard](https://stackimpact.com/img/readme/hotspots-cpu-1.6-java.png)


#### Features

* Continuous hot spot profiling of CPU usage and locks.
* Health monitoring including CPU, memory, garbage collection and other runtime metrics.
* Alerts on profile anomalies.
* Team access.

Learn more on the [features](https://stackimpact.com/features/) page (with screenshots).


#### How it works

The StackImpact profiler agent is imported into a program and used as a normal package. When the program runs, various sampling profilers are started and stopped automatically by the agent. The agent periodically reports recorded profiles and metrics to the StackImpact Dashboard.


#### Documentation

See full [documentation](https://stackimpact.com/docs/) for reference.



## Supported environment

* Linux and macOS. Java 1.7 or higher.


## Getting started


#### Create StackImpact account

Sign up for a free trial account at [stackimpact.com](https://stackimpact.com) (also with GitHub login).


#### Installing and configuring the agent - OPTION 1

Download the [stackimpact.jar](https://github.com/stackimpact/stackimpact-java/raw/master/dist/stackimpact.jar) file.

Add `-javaagent:/path/to/stackimpact.jar` Java option.

Configure the agent using environment variables:

* `SI_AGENT_KEY` (Required) The API key for communication with the StackImpact servers.
* `SI_APP_NAME` (Required) A name to identify and group application data. Typically, a single codebase, deployable unit or executable module corresponds to one application.
* `SI_APP_VERSION` (Optional) Sets application version, which can be used to associate profiling information with the source code release.
* `SI_APP_ENVIRONMENT` (Optional) Used to differentiate applications in different environments.
* `SI_HOST_NAME` (Optional) By default, host name will be the OS hostname.
* `SI_DEBUG_MODE` (Optional) Enables debug logging.
* `SI_CPU_PROFILER_DISABLED`, `SI_LOCK_PROFILER_DISABLED` (Optional) Disables respective profiler when `true`.

Alternatively, the agent can be configured using Java system properties, e.g. `si.agent.key`, `si.app.name`, etc.


#### Installing and configuring the agent - OPTION 2

Download the [stackimpact.jar](https://github.com/stackimpact/stackimpact-java/raw/master/dist/stackimpact.jar) file.

Add the jar file to the classpath.

Import the agent in your application:

```java
import com.stackimpact.agent.StackImpact;
```

Start the agent when the application starts:

```java
StackImpact.start(String agentKey, String appName);
```

The agent can be configured by setting initialization options using the following methods prior to calling the `start()` method:

* `StackImpact.setAppVersion(String appVersion)` (Optional) Sets application version, which can be used to associate profiling information with the source code release.
* `StackImpact.setAppEnvironment(String appEnvironment)` (Optional) Used to differentiate applications in different environments.
* `StackImpact.setHostName(String hostName)` (Optional) By default, host name will be the OS hostname.
* `StackImpact.setDebugMode(boolean isDebugMode)` (Optional) Enables debug logging.
* `StackImpact.setCPUProfilerDisabled(boolean isDisabled)`, `setLockProfilerDisabled(boolean isDisabled)` (Optional) Disables respective profiler when `true`.


#### Shutting down the agent
*Optional*

Use `StackImpact.destroy()` to stop the agent if necessary. This method is automatically called on JVM shutdown.


#### Analyzing performance data in the Dashboard

Once your application is restarted, you can start observing continuous CPU, memory, I/O, and other hot spot profiles, execution bottlenecks as well as process metrics in the [Dashboard](https://dashboard.stackimpact.com/).


#### Troubleshooting

To enable debug logging, use `StackImpact.setDebugMode(true)` method. If the debug log doesn't give you any hints on how to fix a problem, please report it to our support team in your account's Support section.


## Overhead

The agent overhead is measured to be less than 1% for applications under high load.
