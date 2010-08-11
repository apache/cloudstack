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

public abstract class DownloadActiveState extends DownloadState {

	public DownloadActiveState(DownloadListener dl) {
		super(dl);
	}


	@Override
	public String handleAnswer(DownloadAnswer answer) {
		if (s_logger.isTraceEnabled()) {
			s_logger.trace("handleAnswer, answer status=" + answer.getDownloadStatus() + ", curr state=" + getName());
		}
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
	public  void onEntry(String prevState, DownloadEvent event, Object evtObj) {
		if (s_logger.isTraceEnabled()) {
			getDownloadListener().log("onEntry, prev state= " + prevState + ", curr state=" + getName() + ", event=" + event, Level.TRACE);
		}
		
		if (event==DownloadEvent.DOWNLOAD_ANSWER) {
			getDownloadListener().updateDatabase((DownloadAnswer)evtObj);
			getDownloadListener().setLastUpdated();
		}
		
	}
	
	@Override
	public  void onExit() {
	}
	
	@Override
	public  String handleTimeout(long updateMs) {
		if (s_logger.isTraceEnabled()) {
			getDownloadListener().log("handleTimeout, updateMs=" + updateMs + ", curr state= " + getName(), Level.TRACE);
		}
		String newState = this.getName();
		if (updateMs > 5*DownloadListener.STATUS_POLL_INTERVAL){
			newState=Status.DOWNLOAD_ERROR.toString();
			getDownloadListener().log("timeout: transitioning to download error state, currstate=" + getName(), Level.DEBUG );
		} else if (updateMs > 3*DownloadListener.STATUS_POLL_INTERVAL) {
			getDownloadListener().cancelStatusTask();
			getDownloadListener().scheduleImmediateStatusCheck(RequestType.GET_STATUS);
			getDownloadListener().scheduleTimeoutTask(3*DownloadListener.STATUS_POLL_INTERVAL);
			getDownloadListener().log(getName() + " first timeout: checking again ", Level.DEBUG );
		} else {
			getDownloadListener().scheduleTimeoutTask(3*DownloadListener.STATUS_POLL_INTERVAL);
		}
		return newState;
	}
	
	@Override
	public  String handleAbort(){
		return Status.ABANDONED.toString();
	}
	
	@Override
	public  String handleDisconnect(){

		return Status.DOWNLOAD_ERROR.toString();
	}

}
