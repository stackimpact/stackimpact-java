package com.stackimpact.agent.model;

import java.util.Map;
import java.util.HashMap;

import com.stackimpact.agent.*;

public class Metric {

    public final static String TYPE_STATE = "state";
    public final static String TYPE_COUNTER = "counter";
    public final static String TYPE_PROFILE = "profile";
    public final static String TYPE_TRACE = "trace";
    public final static String CATEGORY_CPU = "cpu";
    public final static String CATEGORY_MEMORY = "memory";
    public final static String CATEGORY_GC = "gc";
    public final static String CATEGORY_RUNTIME = "runtime";
    public final static String CATEGORY_SPAN = "span";
    public final static String CATEGORY_CPU_PROFILE = "cpu-profile";
    public final static String CATEGORY_MEMORY_PROFILE = "memory-profile";
    public final static String CATEGORY_LOCK_PROFILE = "lock-profile";
    public final static String CATEGORY_ERROR_PROFILE = "error-profile";
    public final static String NAME_CPU_USAGE = "CPU usage";
    public final static String NAME_USED_MEMORY = "Used memory";
    public final static String NAME_GC_CYCLES = "GC cycles";
    public final static String NAME_GC_TIME = "GC time";
    public final static String NAME_THREAD_COUNT = "Thread count";
    public final static String NAME_MEMORY_ALLOCATION_RATE = "Memory allocation rate";
    public final static String NAME_LOCK_WAIT = "Lock wait";
    public final static String NAME_EXCEPTIONS = "Exceptions";
    public final static String UNIT_NONE = "";
    public final static String UNIT_MILLISECOND = "millisecond";
    public final static String UNIT_MICROSECOND = "microsecond";
    public final static String UNIT_NANOSECOND = "nanosecond";
    public final static String UNIT_BYTE = "byte";
    public final static String UNIT_KILOBYTE = "kilobyte";
    public final static String UNIT_PERCENT = "percent";
    public final static String TRIGGER_TIMER = "timer";
    public final static String TRIGGER_API = "api";


    public String id;
    public String type;
    public String category;
    public String name;
    public String unit;
    public Measurement measurement = null;
    public boolean hasLastValue = false;
    public double lastValue = 0;


    public Metric(String type, String category, String name, String unit) {
        this.type = type;
        this.category = category;
        this.name = name;
        this.unit = unit;
    }


    public void createMetricID(Agent agent) throws Exception {
        StringBuilder idBuilder = new StringBuilder();
        idBuilder.append(agent.getAppName());
        idBuilder.append(agent.getAppEnvironment());
        idBuilder.append(agent.getHostName());
        idBuilder.append(type);
        idBuilder.append(name);
        idBuilder.append(category);
        idBuilder.append(unit);

        this.id = AgentUtils.generateSHA1(idBuilder.toString());
    }


    public boolean hasMeasurement() {
        return (measurement != null);
    }


    public void createMeasurement(String trigger, double value, long duration) throws Exception {
        createMeasurement(trigger, value, duration, null);
    }


    public void createMeasurement(String trigger, double value, long duration, Breakdown breakdown) throws Exception {
        boolean ready = true;

        if (type == TYPE_COUNTER) {
            if (!hasLastValue) {
                ready = false;
                hasLastValue = true;
                lastValue = value;
            }
            else {
                double tmpValue = value;
                value = value - lastValue;
                lastValue = tmpValue;
            }
        }

        if (ready) {
            measurement = new Measurement(
                AgentUtils.generateUUID(),
                trigger,
                value,
                duration,
                breakdown,
                AgentUtils.timestamp());
        }
    }


    public Map toMap() {
        Map measurementMap = null;
        if (measurement != null) {
            measurementMap = measurement.toMap();
        }

        Map metricMap = new HashMap();
        metricMap.put("id", id);
        metricMap.put("type", type);
        metricMap.put("category", category);
        metricMap.put("name", name);
        metricMap.put("unit", unit);
        metricMap.put("measurement", measurementMap);

        return metricMap;
    } 
}
