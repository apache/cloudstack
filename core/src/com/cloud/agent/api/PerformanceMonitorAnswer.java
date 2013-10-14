package com.cloud.agent.api;

public class PerformanceMonitorAnswer extends Answer {
	public PerformanceMonitorAnswer() {
	}

	public PerformanceMonitorAnswer(PerformanceMonitorCommand cmd,
			boolean result, String details) {
		super(cmd, result, details);
	}
}
