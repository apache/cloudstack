package org.apache.cloudstack.storage;

import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CopyCmd;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.resource.ServerResource;
import com.cloud.storage.resource.LocalNfsSecondaryStorageResource;
import com.cloud.utils.component.ComponentContext;

public class LocalHostEndpoint implements EndPoint {
	
	ServerResource resource;
	public LocalHostEndpoint() {
		resource = ComponentContext.inject(LocalNfsSecondaryStorageResource.class);
	}
	@Override
	public long getId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Answer sendMessage(Command cmd) {
		if (cmd instanceof CopyCmd) {
			return resource.executeRequest(cmd);
		}
		// TODO Auto-generated method stub
		return new Answer(cmd, false, "unsupported command:" + cmd.toString());
	}

	@Override
	public void sendMessageAsync(Command cmd,
			AsyncCompletionCallback<Answer> callback) {
		// TODO Auto-generated method stub

	}

}
