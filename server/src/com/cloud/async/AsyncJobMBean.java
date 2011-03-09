package com.cloud.async;

public interface AsyncJobMBean {
	public long getAccountId();
	public long getUserId();
	public String getCmd();
	public String getCmdInfo();
	public String getStatus();
	public int getProcessStatus();
	public int getResultCode();
	public String getResult();
	public String getInstanceType();
	public String getInstanceId();
	public String getInitMsid();
	public String getCreateTime();
	public String getLastUpdateTime();
	public String getLastPollTime();
	public String getSyncQueueId();
	public String getSyncQueueContentType();
	public String getSyncQueueContentId();
}
