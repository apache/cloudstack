package com.cloud.storage.upload;

import com.cloud.agent.api.storage.UploadProgressCommand.RequestType;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;

public class UploadAbandonedState extends UploadInactiveState {

	public UploadAbandonedState(UploadListener dl) {
		super(dl);
	}

	@Override
	public String getName() {
		return Status.ABANDONED.toString();
	}

	@Override
	public void onEntry(String prevState, UploadEvent event, Object evtObj) {
		super.onEntry(prevState, event, evtObj);
		if (!prevState.equalsIgnoreCase(getName())){
			getUploadListener().updateDatabase(Status.ABANDONED, "Upload canceled");
			getUploadListener().cancelStatusTask();
			getUploadListener().cancelTimeoutTask();
			getUploadListener().sendCommand(RequestType.ABORT);
		}
	}
}
