package com.stackimpact.agent.model;


import java.util.Map;
import java.util.HashMap;


public class Measurement {

    public String id;
    public String trigger;
    public double value;
    public long duration;
    public Breakdown breakdown;
    public long timestamp;


    public Measurement(String id, String trigger, double value, long duration, Breakdown breakdown, long timestamp) {
        this.id = id;
        this.trigger = trigger;
        this.value = value;
        this.duration = duration;
        this.breakdown = breakdown;
        this.timestamp = timestamp;
    }


    public Map toMap() {
        Map breakdownMap = null;
        if (breakdown != null) {
            breakdownMap = breakdown.toMap();
        }

        Map measurementMap = new HashMap();
        measurementMap.put("id", id);
        measurementMap.put("trigger", trigger);
        measurementMap.put("value", value);
        measurementMap.put("duration", duration);
        measurementMap.put("breakdown", breakdownMap);
        measurementMap.put("timestamp", timestamp);

        return measurementMap;
    }
}
