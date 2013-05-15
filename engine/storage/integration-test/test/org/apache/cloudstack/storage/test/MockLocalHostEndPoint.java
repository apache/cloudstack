package org.apache.cloudstack.storage.test;

import org.apache.cloudstack.storage.LocalHostEndpoint;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.DownloadCommand;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.DeleteSnapshotBackupCommand;

public class MockLocalHostEndPoint extends LocalHostEndpoint {
	@Override
	public Answer sendMessage(Command cmd) {
		if ((cmd instanceof CopyCommand) || (cmd instanceof DownloadCommand) || (cmd instanceof DeleteSnapshotBackupCommand)) {
			return resource.executeRequest(cmd);
		}
		// TODO Auto-generated method stub
		return new Answer(cmd, false, "unsupported command:" + cmd.toString());
	}
}
