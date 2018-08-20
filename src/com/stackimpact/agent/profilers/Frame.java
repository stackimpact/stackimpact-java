package com.stackimpact.agent.profilers;

public class Frame {
    public String className;
    public String methodName;
    public int lineNumber;

    public String toString() {
    	StringBuilder sb = new StringBuilder();

    	if (className != null) {
    		sb.append(className);
    	}

    	if (methodName != null) {
    		if (sb.length() > 0) {
	    		sb.append('.');
    		}
    		sb.append(methodName);
    	}

    	if (lineNumber > 0 && sb.length() > 0) {
	    	sb.append(':');
    		sb.append(lineNumber);
    	}

    	return sb.toString();
    }
}
