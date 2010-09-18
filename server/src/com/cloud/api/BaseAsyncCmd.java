package com.cloud.api;

import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.api.response.AsyncJobResponse;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobVO;

/**
 * A base command for supporting asynchronous API calls.  When an API command is received, the command will be
 * serialized to the queue (currently the async_job table) and a response will be immediately returned with the
 * id of the queue object.  The id can be used to query the status/progress of the command using the
 * queryAsyncJobResult API command.
 */
public abstract class BaseAsyncCmd extends BaseCmd {
    private AsyncJobManager _asyncJobMgr = null;
    private AsyncJobVO _job = null;
    private Long startEventId;

    public String getResponse(long jobId) {
        // FIXME:  We need a generic response object here, see BaseAsyncCreateCmd
        AsyncJobResponse response = new AsyncJobResponse();
        response.setId(jobId);
        response.setResponseName(getName());
        return ApiResponseSerializer.toSerializedString(response);
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

    public Long getStartEventId() {
        return startEventId;
    }

    public void setStartEventId(Long startEventId) {
        this.startEventId = startEventId;
    }
}
