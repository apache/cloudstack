package com.cloud.agent.api.baremetal;

import com.cloud.agent.api.Answer;

public class PrepareLinMinPxeServerAnswer extends Answer {
	public PrepareLinMinPxeServerAnswer(PrepareLinMinPxeServerCommand cmd) {
		super(cmd, true, "SUCCESS");
	}
	
	public PrepareLinMinPxeServerAnswer(PrepareLinMinPxeServerCommand cmd, String details) {
		super(cmd, false, details);
	}	
}
