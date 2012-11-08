package org.apache.cloudstack.engine.subsystem.api.storage;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

public interface EndPoint {
	public Answer sendMessage(Command cmd);
}
