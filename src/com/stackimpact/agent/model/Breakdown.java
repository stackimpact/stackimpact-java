package com.stackimpact.agent.model;


import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class Breakdown {

	public final static String TYPE_CALLGRAPH = "callgraph";
	public final static String TYPE_CALLSITE = "callsite";
	public final static String TYPE_ERROR = "error";

    public String type;
    public String name;
    public Map<String,String> metadata = new HashMap<String,String>();
    public double measurement = 0;
    public long numSamples = 0;
    public Map<String,Breakdown> children = new HashMap<String,Breakdown>();


  	public Breakdown(String type, String name) {
	    this.type = type;
	    this.name = name;
	}


	public Map toMap() {
		List childrenList = new ArrayList();

		for (Breakdown child : children.values()) {
			childrenList.add(child.toMap());
		}

    	Map breakdownMap = new HashMap();
    	breakdownMap.put("type", type);
    	breakdownMap.put("name", name);
    	breakdownMap.put("metadata", metadata);
    	breakdownMap.put("measurement", measurement);
    	breakdownMap.put("num_samples", numSamples);
    	breakdownMap.put("children", childrenList);

    	return breakdownMap;
	}


	public void addMetadata(String key, String value) {
		metadata.put(key, value);
	}


	public String getMetadata(String key) {
		return metadata.get(key);
	}


	public Breakdown findChild(String name) {
		return children.get(name);
	}


	public Breakdown findOrAddChild(String type, String name) {
		Breakdown child = findChild(name);
		if (child == null) {
			child = new Breakdown(type, name);
			addChild(child);
		}

		return child;
	}


    public void addChild(Breakdown child) {
    	children.put(child.name, child);
    }


    public void removeChild(String name) {
    	children.remove(name);
    }


    public void filter(int fromLevel, double min, double max) {
    	filterLevel(1, fromLevel, min, max);
    }


    public void filterLevel(int currentLevel, int fromLevel, double min, double max) {
		Iterator<Map.Entry<String,Breakdown>> iter = children.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String,Breakdown> entry = iter.next();
			Breakdown child = entry.getValue();
	      	if (currentLevel >= fromLevel && 
	      			(child.measurement < min || child.measurement > max)) {
	        	iter.remove();
	      	}
	      	else {
	        	child.filterLevel(currentLevel + 1, fromLevel, min, max);
	      	}
	    }
    }


    public int depth() {
	    int max = 0;

		for (Breakdown child : children.values()) {
	      int d = child.depth();
	      if (d > max) {
	        max = d;
	      }
		}

	    return max + 1;
    }


    public void increment(double value, long count) {
    	measurement += value;
    	numSamples += count;
    }


    public void propagate() {
		for (Breakdown child : children.values()) {
	      child.propagate();
	      
	      measurement += child.measurement;
	      numSamples += child.numSamples;
	    }
    }


    public void normalize(double factor) {
	    measurement = measurement / factor;
	    numSamples = Math.round(Math.ceil(numSamples / factor));

		for (Breakdown child : children.values()) {
	      child.normalize(factor);
	    }
    }


    public void round() {
	    measurement = Math.rint(measurement);

		for (Breakdown child : children.values()) {
	      child.floor();
	    }
	}


    public void floor() {
	    measurement = Math.floor(measurement);

		for (Breakdown child : children.values()) {
	      child.floor();
	    }
    }


    public void evaluatePercent(long totalSamples) {
	    if (totalSamples > 0) {
	      measurement = ((double)numSamples / totalSamples) * 100;
	    }

		for (Breakdown child : children.values()) {
	      child.evaluatePercent(totalSamples);
	    }
    }


    public String dump() {
    	return dumpLevel(0);
    }


    public String dumpLevel(int level) {
	    StringBuilder out = new StringBuilder();

	    for (int i = 0; i < level; i++) {
	      out.append(" ");
	    }

	    out.append(name);
	    out.append(" - ");
	    out.append(measurement);
	    out.append(" (");
	    out.append(numSamples);
	    out.append(")\n");

		for (Breakdown child : children.values()) {
	      out.append(child.dumpLevel(level + 1));
	    }

	    return out.toString();
    }

}
