package com.cloud.agent.api;

import java.util.HashMap;
import java.util.Map;

public class PerformanceMonitorCommand extends Command {

	Map<String, String> params = new HashMap<String, String>();

	public PerformanceMonitorCommand() {
	}

	public PerformanceMonitorCommand(Map<String, String> params, int wait) {
		setWait(wait);
		this.params = params;
	}

	@Override
	public boolean executeInSequence() {
		return false;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}
}
