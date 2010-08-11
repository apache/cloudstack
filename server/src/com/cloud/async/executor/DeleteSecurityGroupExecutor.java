package com.cloud.async.executor;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.SecurityGroupVO;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.google.gson.Gson;

public class DeleteSecurityGroupExecutor extends BaseAsyncJobExecutor {
    public static final Logger s_logger = Logger.getLogger(DeleteSecurityGroupExecutor.class.getName());

    @Override
    public boolean execute() {
        Gson gson = GsonHelper.getBuilder().create();
        AsyncJobManager asyncMgr = getAsyncJobMgr();
        AsyncJobVO job = getJob();
        ManagementServer managementServer = asyncMgr.getExecutorContext().getManagementServer();
        SecurityGroupParam param = gson.fromJson(job.getCmdInfo(), SecurityGroupParam.class);
        
        if(getSyncSource() == null) {
            SecurityGroupVO securityGroup = managementServer.findSecurityGroupById(param.getSecurityGroupId());
            if(securityGroup == null) {
                asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, 
                    BaseCmd.NET_INVALID_PARAM_ERROR, "Unable to find security group " + param.getSecurityGroupId() + ", failed to delete security group"); 
            } else {
                asyncMgr.syncAsyncJobExecution(job.getId(), "SecurityGroup", securityGroup.getId());
            }
            return true;
        } else {
            try {
                managementServer.deleteSecurityGroup(param.getUserId(), param.getSecurityGroupId(), param.getEventId());
                asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, "success");
            } catch (PermissionDeniedException e) {
                if(s_logger.isDebugEnabled())
                    s_logger.debug("Unable to remove security group: " + e.getMessage());
                asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.PARAM_ERROR, e.getMessage());
            } catch(Exception e) {
                s_logger.warn("Unable to remove security group : " + e.getMessage(), e);
                asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.PARAM_ERROR, e.getMessage());
            }
        }
        return true;
    }
}
