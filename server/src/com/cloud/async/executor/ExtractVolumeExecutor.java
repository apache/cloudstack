package com.cloud.async.executor;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.google.gson.Gson;

public class ExtractVolumeExecutor extends BaseAsyncJobExecutor {

	public static final Logger s_logger = Logger.getLogger(ExtractVolumeExecutor.class.getName());
	
	@Override
	public boolean execute() {
		Gson gson = GsonHelper.getBuilder().create();
    	AsyncJobManager asyncMgr = getAsyncJobMgr();
    	AsyncJobVO job = getJob();

    	ExtractTemplateParam param = gson.fromJson(job.getCmdInfo(), ExtractTemplateParam.class);   	
    	ManagementServer managementServer = asyncMgr.getExecutorContext().getManagementServer();
 
    	/*
		try {
				managementServer.extractVolume(param.getUrl(), param.getTemplateId(), param.getZoneId(), param.getEventId(), getJob().getId());					
		
		} catch (Exception e) {
			s_logger.warn("Unable to extract volume: " + e.getMessage(), e);
    		asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, e.getMessage());
		}
		*/
		
    	return true;
	}

}
