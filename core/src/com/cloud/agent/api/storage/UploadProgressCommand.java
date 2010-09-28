package com.cloud.agent.api.storage;

public class UploadProgressCommand extends UploadCommand {

	public static enum RequestType {GET_STATUS, ABORT, RESTART, PURGE, GET_OR_RESTART}
	private String jobId;
	private RequestType request;

	protected UploadProgressCommand() {
		super();
	}
	
	public UploadProgressCommand(UploadCommand cmd, String jobId, RequestType req) {
	    super(cmd);

		this.jobId = jobId;
		this.setRequest(req);
	}

	public String getJobId() {
		return jobId;
	}

	public void setRequest(RequestType request) {
		this.request = request;
	}

	public RequestType getRequest() {
		return request;
	}
	
}