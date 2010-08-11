package com.cloud.async.executor;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.network.security.IngressRuleVO;
import com.cloud.network.security.NetworkGroupRulesVO;
import com.cloud.network.security.NetworkGroupVO;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.cloud.user.AccountVO;
import com.google.gson.Gson;

public class AuthorizeNetworkGroupIngressExecutor extends BaseAsyncJobExecutor {
    public static final Logger s_logger = Logger.getLogger(AuthorizeNetworkGroupIngressExecutor.class.getName());

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
                List<IngressRuleVO> addedRules = managementServer.authorizeNetworkGroupIngress(account, param.getGroupName(), param.getProtocol(), param.getStartPort(), param.getEndPort(), param.getCidrList(), param.getAuthorizedGroups());
                if ((addedRules != null) && !addedRules.isEmpty()) {
                    asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, composeResultObject(managementServer, account.getId(), param.getGroupName(), addedRules));
                } else {
                    if (addedRules == null) {
                        asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR,
                                "Failed to authorize network group ingress for group: " + param.getGroupName() + " for account: " + account.getAccountName() + " (id: " + account.getId() + ")");
                    } else {
                        asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.PARAM_ERROR,
                                "Failed to authorize network group ingress for group: " + param.getGroupName() + ", ingress rules already exist.");
                    }
                }
            } catch(Exception e) {
                s_logger.warn("Failed to authorize network group ingress from group: " + param.getGroupName() + " for account: " + account.getAccountName() + " (id: " + account.getId() + ")", e);
                asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
                        "Failed to authorize network group ingress from group: " + param.getGroupName() + " for account: " + account.getAccountName() + " (id: " + account.getId() + ")");
            }
        }

        return true;
    }

    private NetworkGroupResultObject composeResultObject(ManagementServer ms, Long accountId, String groupName, List<IngressRuleVO> addedRules) {
        NetworkGroupVO networkGroup = ms.findNetworkGroupByName(accountId, groupName);
        List<NetworkGroupRulesVO> groupRules = new ArrayList<NetworkGroupRulesVO>();
        for (IngressRuleVO ingressRule : addedRules) {
            NetworkGroupRulesVO groupRule = new NetworkGroupRulesVO(networkGroup.getId(), networkGroup.getName(), networkGroup.getDescription(), networkGroup.getDomainId(),
                                                                    networkGroup.getAccountId(), networkGroup.getAccountName(), ingressRule.getId(), ingressRule.getStartPort(), ingressRule.getEndPort(),
                                                                    ingressRule.getProtocol(), ingressRule.getAllowedNetworkId(), ingressRule.getAllowedNetworkGroup(), ingressRule.getAllowedNetGrpAcct(),
                                                                    ingressRule.getAllowedSourceIpCidr());
            groupRules.add(groupRule);
        }
        List<NetworkGroupResultObject> results = NetworkGroupResultObject.transposeNetworkGroups(groupRules);
        if ((results != null) && !results.isEmpty()) {
            return results.get(0);
        }
        return null;
    }
}
