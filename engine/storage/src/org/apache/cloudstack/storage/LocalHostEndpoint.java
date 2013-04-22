package org.apache.cloudstack.storage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CopyCmd;

import com.cloud.agent.Listener;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.resource.ServerResource;
import com.cloud.storage.download.DownloadListener;
import com.cloud.storage.resource.LocalNfsSecondaryStorageResource;
import com.cloud.utils.component.ComponentContext;

public class LocalHostEndpoint implements EndPoint {
	private ScheduledExecutorService executor;
	ServerResource resource;
	public LocalHostEndpoint() {
		resource = ComponentContext.inject(LocalNfsSecondaryStorageResource.class);
		executor = Executors.newScheduledThreadPool(10);
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

	private class CmdRunner implements Runnable {
		final Command cmd;
		final AsyncCompletionCallback<Answer> callback;
		public CmdRunner(Command cmd, AsyncCompletionCallback<Answer> callback) {
			this.cmd = cmd;
			this.callback = callback;
		}
		@Override
		public void run() {
			Answer answer = sendMessage(cmd);
			callback.complete(answer);
		}
	}
	
	private class CmdRunner2 implements Runnable {
		final Command cmd;
		final AsyncCompletionCallback<DownloadAnswer> callback;
		public CmdRunner2(Command cmd, AsyncCompletionCallback<DownloadAnswer> callback) {
			this.cmd = cmd;
			this.callback = callback;
		}
		@Override
		public void run() {
			DownloadAnswer answer = (DownloadAnswer)sendMessage(cmd);
			callback.complete(answer);
		}
	}
	@Override
	public void sendMessageAsync(Command cmd,
			AsyncCompletionCallback<Answer> callback) {
		 executor.schedule(new CmdRunner(cmd, callback), 10, TimeUnit.SECONDS);
	}
	
	@Override
	public void sendMessageAsyncWithListener(Command cmd, Listener listner) {
		if (listner instanceof DownloadListener) {
			DownloadListener listener = (DownloadListener)listner;
			executor.schedule(new CmdRunner2(cmd, listener.getCallback()), 10, TimeUnit.SECONDS);
		}
	}

}
