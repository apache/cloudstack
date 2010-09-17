package com.cloud.api;

import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobVO;
import com.cloud.serializer.SerializerHelper;

/**
 * A base command for supporting asynchronous API calls.  When an API command is received, the command will be
 * serialized to the queue (currently the async_job table) and a response will be immediately returned with the
 * id of the queue object.  The id can be used to query the status/progress of the command using the
 * queryAsyncJobResult API command.
 */
public abstract class BaseAsyncCmd extends BaseCmd {
    private AsyncJobManager _asyncJobMgr = null;
    private AsyncJobVO _job = null;

    public String getResponse(long jobId) {
        return SerializerHelper.toSerializedString(Long.valueOf(jobId));
    }

    public void setAsyncJobManager(AsyncJobManager mgr) {
        _asyncJobMgr = mgr;
    }

    public void synchronizeCommand(String syncObjType, long syncObjId) {
        _asyncJobMgr.syncAsyncJobExecution(_job, syncObjType, syncObjId);
    }

    public void setJob(AsyncJobVO job) {
        _job = job;
    }
}
