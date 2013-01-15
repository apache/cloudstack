package org.apache.cloudstack.engine.subsystem.api.storage;

import org.apache.cloudstack.framework.async.AsyncCompletionCallback;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

public interface EndPoint {
	public Answer sendMessage(Command cmd);
	public void sendMessageAsync(Command cmd, AsyncCompletionCallback<Answer> callback);
}
