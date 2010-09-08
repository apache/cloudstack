package com.cloud.storage.upload;

import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.cloud.agent.api.storage.UploadAnswer;
import com.cloud.storage.upload.UploadState.UploadEvent;

public abstract class UploadState {

	public static enum UploadEvent {UPLOAD_ANSWER, ABANDON_UPLOAD, TIMEOUT_CHECK, DISCONNECT};
	protected static final Logger s_logger = Logger.getLogger(UploadListener.class.getName());

	private UploadListener ul;
	
	public UploadState(UploadListener ul) {
		this.ul = ul;
	}
	
	protected UploadListener getUploadListener() {
		return ul;
	}
	
	public String handleEvent(UploadEvent event, Object eventObj){
		if (s_logger.isTraceEnabled()) {
			getUploadListener().log("handleEvent, event type=" + event + ", curr state=" + getName(), Level.TRACE);
		}
		switch (event) {
		case UPLOAD_ANSWER:
			UploadAnswer answer=(UploadAnswer)eventObj;
			return handleAnswer(answer);
		case ABANDON_UPLOAD:
			return handleAbort();
		case TIMEOUT_CHECK:
			Date now = new Date();
			long update = now.getTime() - ul.getLastUpdated().getTime();
			return handleTimeout(update);
		case DISCONNECT:
			return handleDisconnect();
		}
		return null;
	}
	
	public   void onEntry(String prevState, UploadEvent event, Object evtObj){
		if (s_logger.isTraceEnabled()) {
			getUploadListener().log("onEntry, event type=" + event + ", curr state=" + getName(), Level.TRACE);
		}
		if (event == UploadEvent.UPLOAD_ANSWER) {
			getUploadListener().updateDatabase((UploadAnswer)evtObj);
		}
	}
	
	public  void onExit() {
		
	}
	
	public abstract String handleTimeout(long updateMs) ;
	
	public abstract String handleAbort();
	
	public abstract  String handleDisconnect();

	public abstract String handleAnswer(UploadAnswer answer) ;
	
	public abstract String getName();


}
