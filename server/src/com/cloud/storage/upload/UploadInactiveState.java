package com.cloud.storage.upload;

import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.storage.UploadAnswer;

public abstract class UploadInactiveState extends UploadState {

	public UploadInactiveState(UploadListener ul) {
		super(ul);
	}

	@Override
	public String handleAnswer(UploadAnswer answer) {
		// ignore and stay put
		return getName();
	}

	@Override
	public String handleAbort() {
		// ignore and stay put
		return getName();
	}

	@Override
	public String handleDisconnect() {
		//ignore and stay put
		return getName();
	}

	@Override
	public String handleTimeout(long updateMs) {
		// ignore and stay put
		return getName();
	}
}
