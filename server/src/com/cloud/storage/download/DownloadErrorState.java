/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.storage.download;

import org.apache.log4j.Level;

import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.storage.DownloadProgressCommand.RequestType;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;

public class DownloadErrorState extends DownloadInactiveState {

	public DownloadErrorState(DownloadListener dl) {
		super(dl);
	}


	@Override
	public String handleAnswer(DownloadAnswer answer) {
		switch (answer.getDownloadStatus()) {
		case DOWNLOAD_IN_PROGRESS:
			getDownloadListener().scheduleStatusCheck(RequestType.GET_STATUS);
			return Status.DOWNLOAD_IN_PROGRESS.toString();
		case DOWNLOADED:
			getDownloadListener().scheduleImmediateStatusCheck(RequestType.PURGE);
			getDownloadListener().cancelTimeoutTask();
			return Status.DOWNLOADED.toString();
		case NOT_DOWNLOADED:
			getDownloadListener().scheduleStatusCheck(RequestType.GET_STATUS);
			return Status.NOT_DOWNLOADED.toString();
		case DOWNLOAD_ERROR:
			getDownloadListener().cancelStatusTask();
			getDownloadListener().cancelTimeoutTask();
			return Status.DOWNLOAD_ERROR.toString();
		case UNKNOWN:
			getDownloadListener().cancelStatusTask();
			getDownloadListener().cancelTimeoutTask();
			return Status.DOWNLOAD_ERROR.toString();
		default:
			return null;
		}
	}



	@Override
	public String handleAbort() {
		return Status.ABANDONED.toString();
	}


	@Override
	public String getName() {
		return Status.DOWNLOAD_ERROR.toString();
	}


	@Override
	public void onEntry(String prevState, DownloadEvent event, Object evtObj) {
		super.onEntry(prevState, event, evtObj);
		if (event==DownloadEvent.DISCONNECT){
			getDownloadListener().logDisconnect();
			getDownloadListener().cancelStatusTask();
			getDownloadListener().cancelTimeoutTask();
			getDownloadListener().updateDatabase(Status.DOWNLOAD_ERROR, "Storage agent or storage VM disconnected");  
			getDownloadListener().log("Entering download error state because the storage host disconnected", Level.WARN);
		} else if (event==DownloadEvent.TIMEOUT_CHECK){
			getDownloadListener().updateDatabase(Status.DOWNLOAD_ERROR, "Timeout waiting for response from storage host");
			getDownloadListener().log("Entering download error state: timeout waiting for response from storage host", Level.WARN);
		}
		getDownloadListener().setDownloadInactive(Status.DOWNLOAD_ERROR);
	}


}
