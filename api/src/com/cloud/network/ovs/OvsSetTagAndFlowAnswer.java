package com.cloud.network.ovs;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

public class OvsSetTagAndFlowAnswer extends Answer {
	Long vmId;
	Long seqno;
	
	public OvsSetTagAndFlowAnswer(Command cmd, boolean success, String details) {
		super(cmd, success, details);
		OvsSetTagAndFlowCommand c = (OvsSetTagAndFlowCommand)cmd;
		this.vmId = c.getVmId();
		this.seqno = Long.parseLong(c.getSeqNo());
	}
	
	public Long getVmId() {
		return vmId;
	}
	
	public Long getSeqNo() {
		return seqno;
	}
}
