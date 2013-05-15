package com.cloud.vm;

import org.apache.log4j.Logger;

import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobConstants;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;

import com.cloud.async.AsyncJobExecutionContext;
import com.cloud.utils.component.AdapterBase;

public class VmWorkTestWorkJobDispatcher extends AdapterBase implements AsyncJobDispatcher {
    public static final Logger s_logger = Logger.getLogger(VmWorkTestWorkJobDispatcher.class);

	@Override
    public void runJob(AsyncJob job) {
		s_logger.info("Begin work job execution. job-" + job.getId());
		try {
			Thread.sleep(120000);
		} catch (InterruptedException e) {
		}
		
		AsyncJobExecutionContext.getCurrentExecutionContext().completeJobAndJoin(AsyncJobConstants.STATUS_SUCCEEDED, null);
		s_logger.info("End work job execution. job-" + job.getId());
	}
}
