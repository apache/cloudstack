package com.cloud.storage.upload;

import com.cloud.agent.api.storage.UploadProgressCommand.RequestType;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;

public class UploadCompleteState extends UploadInactiveState {

	public UploadCompleteState(UploadListener ul) {
		super(ul);
	}

	@Override
	public String getName() {
		return Status.UPLOADED.toString();

	}


	@Override
	public void onEntry(String prevState, UploadEvent event, Object evtObj) {
		super.onEntry(prevState, event, evtObj);
		if (! prevState.equals(getName())) {
			if (event == UploadEvent.UPLOAD_ANSWER){
				getUploadListener().scheduleImmediateStatusCheck(RequestType.PURGE);
			}
			getUploadListener().setUploadInactive(Status.UPLOADED);
		}
		
	}
}
