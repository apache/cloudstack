package com.cloud.async.executor;

public class OperationResponse {

	public static final int STATUS_IN_PROGRESS = 0;
	public static final int STATUS_SUCCEEDED = 1;
	public static final int STATUS_FAILED = 2;
	
	private int resultCode;
	private String resultDescription;
	
	public OperationResponse(int resultCode, String resultDescription) {		
		this.resultCode = resultCode;
		this.resultDescription = resultDescription;
	}

	public int getResultCode() {
		return resultCode;
	}

	public void setResultCode(int resultCode) {
		this.resultCode = resultCode;
	}

	public String getResultDescription() {
		return resultDescription;
	}

	public void setResultDescription(String resultDescription) {
		this.resultDescription = resultDescription;
	}
	
}
