package com.cloud.async.executor;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.network.security.NetworkGroupVO;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.cloud.user.AccountVO;
import com.google.gson.Gson;

public class RevokeNetworkGroupIngressExecutor extends BaseAsyncJobExecutor {
	public static final Logger s_logger = Logger.getLogger(RevokeNetworkGroupIngressExecutor.class.getName());

	@Override
	public boolean execute() {
		Gson gson = GsonHelper.getBuilder().create();
		AsyncJobManager asyncMgr = getAsyncJobMgr();
		AsyncJobVO job = getJob();
		ManagementServer managementServer = asyncMgr.getExecutorContext().getManagementServer();
		NetworkGroupIngressParam param = gson.fromJson(job.getCmdInfo(), NetworkGroupIngressParam.class);
		AccountVO account = param.getAccount();

		if (getSyncSource() == null) {
			NetworkGroupVO networkGroup = managementServer.findNetworkGroupByName(param.getAccount().getId(), param.getGroupName());
	        if(networkGroup == null) {
				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, 
					BaseCmd.PARAM_ERROR, "Unable to find network group " + param.getGroupName() + " for account " + account.getAccountName() + " (id: " + account.getId() + ")"); 
	        } else {
		    	asyncMgr.syncAsyncJobExecution(job.getId(), "NetworkGroup", networkGroup.getId());
	        }
			return true;
		} else {
			try {
				boolean success = managementServer.revokeNetworkGroupIngress(account, param.getGroupName(), param.getProtocol(), param.getStartPort(), param.getEndPort(), param.getCidrList(), param.getAuthorizedGroups());
				if (success) {
					asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, "success");
				} else {
					asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR,
							"Failed to revoke network group ingress from group: " + param.getGroupName() + " for account: " + account.getAccountName() + " (id: " + account.getId() + ")");
				}
			} catch(Exception e) {
				s_logger.warn("Failed to revoke network group ingress from group: " + param.getGroupName() + " for account: " + account.getAccountName() + " (id: " + account.getId() + ")", e);
				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
						"Failed to revoke network group ingress from group: " + param.getGroupName() + " for account: " + account.getAccountName() + " (id: " + account.getId() + ")");
			}
		}

		return true;
	}
}
