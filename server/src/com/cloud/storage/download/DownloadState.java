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

import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.cloud.agent.api.storage.DownloadAnswer;

/**
 * @author chiradeep
 *
 */
public abstract class DownloadState {
	public static enum DownloadEvent {DOWNLOAD_ANSWER, ABANDON_DOWNLOAD, TIMEOUT_CHECK, DISCONNECT};
	protected static final Logger s_logger = Logger.getLogger(DownloadListener.class.getName());

	private DownloadListener dl;
	
	public DownloadState(DownloadListener dl) {
		this.dl = dl;
	}
	
	protected DownloadListener getDownloadListener() {
		return dl;
	}
	
	public String handleEvent(DownloadEvent event, Object eventObj){
		if (s_logger.isTraceEnabled()) {
			getDownloadListener().log("handleEvent, event type=" + event + ", curr state=" + getName(), Level.TRACE);
		}
		switch (event) {
		case DOWNLOAD_ANSWER:
			DownloadAnswer answer=(DownloadAnswer)eventObj;
			return handleAnswer(answer);
		case ABANDON_DOWNLOAD:
			return handleAbort();
		case TIMEOUT_CHECK:
			Date now = new Date();
			long update = now.getTime() - dl.getLastUpdated().getTime();
			return handleTimeout(update);
		case DISCONNECT:
			return handleDisconnect();
		}
		return null;
	}
	
	public   void onEntry(String prevState, DownloadEvent event, Object evtObj){
		if (s_logger.isTraceEnabled()) {
			getDownloadListener().log("onEntry, event type=" + event + ", curr state=" + getName(), Level.TRACE);
		}
		if (event==DownloadEvent.DOWNLOAD_ANSWER) {
			getDownloadListener().updateDatabase((DownloadAnswer)evtObj);
		}
	}
	
	public  void onExit() {
		
	}
	
	public abstract String handleTimeout(long updateMs) ;
	
	public abstract String handleAbort();
	
	public abstract  String handleDisconnect();

	public abstract String handleAnswer(DownloadAnswer answer) ;
	
	public abstract String getName();


}
