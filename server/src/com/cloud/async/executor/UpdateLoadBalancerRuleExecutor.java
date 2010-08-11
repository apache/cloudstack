package com.cloud.async.executor;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.LoadBalancerVO;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;
import com.google.gson.Gson;

public class UpdateLoadBalancerRuleExecutor extends BaseAsyncJobExecutor {
    public static final Logger s_logger = Logger.getLogger(UpdateLoadBalancerRuleExecutor.class.getName());

    @Override
    public boolean execute() {
        if (getSyncSource() == null) {
            Gson gson = GsonHelper.getBuilder().create();
            AsyncJobManager asyncMgr = getAsyncJobMgr();
            AsyncJobVO job = getJob();

            UpdateLoadBalancerParam param = gson.fromJson(job.getCmdInfo(), UpdateLoadBalancerParam.class);
            asyncMgr.syncAsyncJobExecution(job.getId(), "LoadBalancer", param.getLoadBalancerId()); // in reality I need to synchronize on both the load balancer and domR

            // always true if it does not have sync-source
            return true;
        } else {
            Gson gson = GsonHelper.getBuilder().create();
            AsyncJobManager asyncMgr = getAsyncJobMgr();
            AsyncJobVO job = getJob();

            UpdateLoadBalancerParam param = gson.fromJson(job.getCmdInfo(), UpdateLoadBalancerParam.class);
            ManagementServer ms = asyncMgr.getExecutorContext().getManagementServer();
            LoadBalancerVO loadBalancer = ms.findLoadBalancerById(param.getLoadBalancerId());

            try {
                loadBalancer = ms.updateLoadBalancerRule(loadBalancer, param.getName(), param.getDescription());
                loadBalancer = ms.updateLoadBalancerRule(param.getUserId(), loadBalancer, param.getPrivatePort(), param.getAlgorithm());

                getAsyncJobMgr().completeAsyncJob(job.getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, composeResultObject(ms, loadBalancer));
            } catch (InvalidParameterValueException ex) {
                getAsyncJobMgr().completeAsyncJob(job.getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.PARAM_ERROR, ex.getMessage());
            } catch (Exception ex) {
                s_logger.error("Unhandled exception updating load balancer rule", ex);
                getAsyncJobMgr().completeAsyncJob(job.getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, "Internal error updating load balancer rule " + loadBalancer.getName());
            }
            return true;
        }
    }

    private UpdateLoadBalancerRuleResultObject composeResultObject(ManagementServer ms, LoadBalancerVO loadBalancer) {
        UpdateLoadBalancerRuleResultObject resultObject = new UpdateLoadBalancerRuleResultObject();

        resultObject.setId(loadBalancer.getId());
        resultObject.setName(loadBalancer.getName());
        resultObject.setDescription(loadBalancer.getDescription());
        resultObject.setPublicIp(loadBalancer.getIpAddress());
        resultObject.setPublicPort(loadBalancer.getPublicPort());
        resultObject.setPrivatePort(loadBalancer.getPrivatePort());
        resultObject.setAlgorithm(loadBalancer.getAlgorithm());

        Account accountTemp = ms.findAccountById(loadBalancer.getAccountId());
        if (accountTemp != null) {
            resultObject.setAccountName(accountTemp.getAccountName());
            resultObject.setDomainId(accountTemp.getDomainId());
            resultObject.setDomainName(ms.findDomainIdById(accountTemp.getDomainId()).getName());
        }

        return resultObject;
    }
}
