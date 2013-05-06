package org.apache.cloudstack.engine.subsystem.api.storage;


import org.apache.cloudstack.storage.command.CommandResult;

import com.cloud.agent.api.Answer;

public class SnapshotResult extends CommandResult {
	private SnapshotInfo snashot;
	private Answer answer;
	public SnapshotResult(SnapshotInfo snapshot, Answer answer) {
		this.setSnashot(snapshot);
		this.setAnswer(answer);
	}
	public SnapshotInfo getSnashot() {
		return snashot;
	}
	public void setSnashot(SnapshotInfo snashot) {
		this.snashot = snashot;
	}
	public Answer getAnswer() {
		return answer;
	}
	public void setAnswer(Answer answer) {
		this.answer = answer;
	}
}
