package org.apache.cloudstack.storage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.DownloadCommand;

import com.cloud.agent.Listener;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.resource.ServerResource;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.download.DownloadListener;
import com.cloud.storage.resource.LocalNfsSecondaryStorageResource;
import com.cloud.utils.component.ComponentContext;

public class LocalHostEndpoint implements EndPoint {
	private ScheduledExecutorService executor;
	ServerResource resource;
	public LocalHostEndpoint() {
		resource = new LocalNfsSecondaryStorageResource();
		executor = Executors.newScheduledThreadPool(10);
	}
	@Override
	public long getId() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
    public String getHostAddr() {
        return "127.0.0.0";
    }

    @Override
	public Answer sendMessage(Command cmd) {
		if ((cmd instanceof CopyCommand) || (cmd instanceof DownloadCommand)) {
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
		final Listener listener;
		public CmdRunner2(Command cmd, Listener listener) {
			this.cmd = cmd;
			this.listener = listener;
		}
		@Override
		public void run() {
            try {
                DownloadAnswer answer = (DownloadAnswer) sendMessage(cmd);
                Answer[] answers = new Answer[1];
                answers[0] = answer;
                listener.processAnswers(getId(), 0, answers);
                if (listener instanceof DownloadListener) {
                    DownloadListener dwldListener = (DownloadListener)listener;
                    dwldListener.getCallback().complete(answer);
                }
            } catch (Exception ex) {
                DownloadAnswer fail = new DownloadAnswer("Error in handling DownloadCommand : " + ex.getMessage(), VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR);
                Answer[] answers = new Answer[1];
                answers[0] = fail;
                listener.processAnswers(getId(), 0, answers);
                if (listener instanceof DownloadListener) {
                    DownloadListener dwldListener = (DownloadListener)listener;
                    dwldListener.getCallback().complete(fail);
                }
            }
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
			executor.schedule(new CmdRunner2(cmd, listener), 10, TimeUnit.SECONDS);
		}
	}
    public ServerResource getResource() {
        return resource;
    }
    public void setResource(ServerResource resource) {
        this.resource = resource;
    }



}
