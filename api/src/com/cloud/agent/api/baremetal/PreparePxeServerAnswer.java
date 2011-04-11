package com.cloud.agent.api.baremetal;

import com.cloud.agent.api.Answer;

public class PreparePxeServerAnswer extends Answer {
	public PreparePxeServerAnswer(PreparePxeServerCommand cmd) {
		super(cmd, true, "SUCCESS");
	}
	
	public PreparePxeServerAnswer(PreparePxeServerCommand cmd, String details) {
		super(cmd, false, details);
	}	
}
