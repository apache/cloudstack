package com.cloud.api;

import com.cloud.serializer.SerializerHelper;
import com.cloud.user.Account;

/**
 * A base command for supporting asynchronous API calls.  When an API command is received, the command will be
 * serialized to the queue (currently the async_job table) and a response will be immediately returned with the
 * id of the queue object.  The id can be used to query the status/progress of the command using the
 * queryAsyncJobResult API command.
 *
 * TODO:  Create commands are often async and yet they need to return the id of object in question, e.g. deployVirtualMachine
 * should return a virtual machine id, createPortForwardingServiceRule should return a rule id, and createVolume should return
 * a volume id.
 */
public abstract class BaseAsyncCmd extends BaseCmd {
    public String getResponse(long jobId) {
        return SerializerHelper.toSerializedString(Long.valueOf(jobId));
    }
}
