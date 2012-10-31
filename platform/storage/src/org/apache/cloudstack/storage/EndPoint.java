package org.apache.cloudstack.storage;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

public interface EndPoint {
	public Answer sendMessage(Command cmd);
}
