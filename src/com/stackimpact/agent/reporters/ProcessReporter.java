package com.stackimpact.agent.reporters;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.lang.management.*;
import com.stackimpact.agent.*;
import com.stackimpact.agent.model.*;


public class ProcessReporter {

	public final static long REPORT_INTERVAL = 60 * 1000;


	private Agent agent;
	private Timer reportTimer;
	private boolean isStarted;
	private Map<String, Metric> metrics;
	private long lastCPUTime = 0;


	public ProcessReporter(Agent agent) {
		this.agent = agent;
	}


	public void reset() {
		metrics = new HashMap<String, Metric>();
    	lastCPUTime = 0;
	}


	public void start() {
		if (isStarted) {
			return;
		}
		isStarted = true;

		reset();

		reportTimer = new Timer();
		reportTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
		  	public void run() {
		  		try {
			  		report();
			  	}
			  	catch(Exception ex) {
			  		agent.logException(ex);
			  	}
			}
		}, REPORT_INTERVAL, REPORT_INTERVAL);
	}


	public void stop() {
		if (!isStarted) {
			return;
		}
		isStarted = false;

		if (reportTimer != null) {
			reportTimer.cancel();
		}
	}


	public void report() throws Exception {
		ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

	    if (threadBean.isThreadCpuTimeSupported()) {
		    long cpuTime = 0;

			long[] ids = threadBean.getAllThreadIds();
			for (int i = 0; i < ids.length; i++) {
	        	long threadCPUTime = threadBean.getThreadCpuTime(ids[i]);
		        if (threadCPUTime != -1) {
		            cpuTime += threadCPUTime;
		        }
	        }

	        if (cpuTime > 0) {
		    	if (lastCPUTime > 0) {
	        		double cpuUsage = ((cpuTime - lastCPUTime) / (60 * 1e9)) * 100;
	        		cpuUsage = cpuUsage /  Runtime.getRuntime().availableProcessors();
			        reportMetric(Metric.TYPE_STATE, Metric.CATEGORY_CPU, Metric.NAME_CPU_USAGE, Metric.UNIT_PERCENT, cpuUsage, 0);
		    	}

		   		lastCPUTime = cpuTime;
		   	}
		}

   		long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		reportMetric(Metric.TYPE_STATE, Metric.CATEGORY_MEMORY, Metric.NAME_USED_MEMORY, Metric.UNIT_BYTE, usedMemory, 0);


		boolean isCollectionCountSupported = true;
		boolean isCollectionTimeSupported = true;
		long totalCollectionCount = 0;
		long totalCollectionTime = 0;
		Collection<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
		for (GarbageCollectorMXBean gcBean : gcBeans) {
			long collectionCount = gcBean.getCollectionCount();
			if (collectionCount > -1) {
				String metricName = Metric.NAME_GC_CYCLES + ": " + gcBean.getName();
				reportMetric(Metric.TYPE_COUNTER, Metric.CATEGORY_GC, metricName, Metric.UNIT_NONE, collectionCount, 0);
				totalCollectionCount += collectionCount;
				isCollectionCountSupported = true;
			}

			long collectionTime = gcBean.getCollectionTime();
			if (collectionTime > -1) {
				String metricName = Metric.NAME_GC_TIME + ": " + gcBean.getName();
				reportMetric(Metric.TYPE_COUNTER, Metric.CATEGORY_GC, metricName, Metric.UNIT_MILLISECOND, collectionTime, 0);
				totalCollectionTime += collectionTime;
				isCollectionTimeSupported = true;
			}
		}

		if (isCollectionCountSupported) {
			reportMetric(Metric.TYPE_COUNTER, Metric.CATEGORY_GC, Metric.NAME_GC_CYCLES, Metric.UNIT_NONE, totalCollectionCount, 0);
		}

		if (isCollectionTimeSupported) {
			reportMetric(Metric.TYPE_COUNTER, Metric.CATEGORY_GC, Metric.NAME_GC_TIME, Metric.UNIT_MILLISECOND, totalCollectionTime, 0);
		}

   		long numThreads = threadBean.getThreadCount();
		reportMetric(Metric.TYPE_STATE, Metric.CATEGORY_RUNTIME, Metric.NAME_THREAD_COUNT, Metric.UNIT_NONE, numThreads, 0);
	}


  	public void reportMetric(String type, String category, String name, String unit, double value, long duration) throws Exception {
	    String key = type + category + name;

	    Metric metric = metrics.get(key);
	    if (metric == null) {
	      metric = new Metric(type, category, name, unit);
	      metric.createMetricID(agent);

	      metrics.put(key, metric);
	    }
	    metric.createMeasurement(Metric.TRIGGER_TIMER, value, duration);

	    if (metric.hasMeasurement()) {
	      agent.getMessageQueue().add("metric", metric.toMap());
	    }
  	}
}