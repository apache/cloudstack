package com.cloud.storage.upload;

import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;

public class UploadInProgressState extends UploadActiveState {

	public UploadInProgressState(UploadListener dl) {
		super(dl);
	}

	@Override
	public String getName() {
		return Status.UPLOAD_IN_PROGRESS.toString();
	}

	@Override
	public void onEntry(String prevState, UploadEvent event, Object evtObj) {
		super.onEntry(prevState, event, evtObj);
		if (!prevState.equals(getName()))
			getUploadListener().logUploadStart();
	}

}
