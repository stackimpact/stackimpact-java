package com.stackimpact.agent;


import com.stackimpact.agent.reporters.ProfileReporter;


public class ProfileSpan {

    private Agent agent;

    private boolean isStarted;
    private boolean isProfiling;
    private ProfileReporter reporter;


    public ProfileSpan(Agent agent) {
        this.agent = agent;
    }


    public void start() {
        isStarted = agent.getSpanLock().compareAndSet(false, true);
        if (!isStarted) {
          return;
        }

        if (Math.random() > 0.5) {
            reporter = agent.getCPUReporter();
        }
        else {
            reporter = agent.getLockReporter();
        }

        try {
            isProfiling = reporter.startProfiling();
        }
        catch(Exception ex) {
            agent.logException(ex);
        }
    }


    public void stop() {
        if (isStarted) {
            if (isProfiling) {
                try {
                    reporter.stopProfiling();
                }
                catch(Exception ex) {
                    agent.logException(ex);
                }
            }

            if (!agent.isAutoProfilingMode()) {
                try {
                    agent.getConfigLoader().load();

                    agent.getCPUReporter().report(true);
                    agent.getLockReporter().report(true);

                    agent.getMessageQueue().flush();
                }
                catch(Exception ex) {
                    agent.logException(ex);
                }
            }

            agent.getSpanLock().set(false);
        }
    }
}
