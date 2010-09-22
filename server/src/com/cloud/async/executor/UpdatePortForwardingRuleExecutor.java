
package com.cloud.async.executor;

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.network.FirewallRuleVO;
import com.cloud.network.IPAddressVO;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.Criteria;
import com.cloud.server.ManagementServer;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.UserVmVO;
import com.google.gson.Gson;

public class UpdatePortForwardingRuleExecutor extends BaseAsyncJobExecutor {
    public static final Logger s_logger = Logger.getLogger(UpdatePortForwardingRuleExecutor.class.getName());

    @Override
    public boolean execute() {
        if (getSyncSource() == null) {
            Gson gson = GsonHelper.getBuilder().create();
            AsyncJobManager asyncMgr = getAsyncJobMgr();
            AsyncJobVO job = getJob();

            CreateOrUpdateRuleParam param = gson.fromJson(job.getCmdInfo(), CreateOrUpdateRuleParam.class);
            ManagementServer ms = asyncMgr.getExecutorContext().getManagementServer();
            IPAddressVO ipAddr = ms.findIPAddressById(param.getAddress());
            DomainRouterVO router = ms.findDomainRouterBy(ipAddr.getAccountId(), ipAddr.getDataCenterId());
            asyncMgr.syncAsyncJobExecution(job.getId(), "Router", router.getId()); // synchronize on the router

            // always true if it does not have sync-source
            return true;
        } else {
            Gson gson = GsonHelper.getBuilder().create();
            AsyncJobManager asyncMgr = getAsyncJobMgr();
            AsyncJobVO job = getJob();

            CreateOrUpdateRuleParam param = gson.fromJson(job.getCmdInfo(), CreateOrUpdateRuleParam.class);
            ManagementServer ms = asyncMgr.getExecutorContext().getManagementServer();

            try {
                FirewallRuleVO fwRule = ms.updatePortForwardingRule(param.getUserId(), param.getAddress(), param.getPrivateIpAddress(), param.getPort(), param.getPrivatePort(), param.getProtocol());

                if (fwRule != null) {
                    getAsyncJobMgr().completeAsyncJob(job.getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, composeResultObject(ms, fwRule));
                } else {
                    getAsyncJobMgr().completeAsyncJob(job.getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, "Internal error updating forwarding rule for address " + param.getAddress());
                }
            } catch (Exception ex) {
                s_logger.error("Unhandled exception updating port forwarding rule", ex);
                getAsyncJobMgr().completeAsyncJob(job.getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, "Internal error updating forwarding rule for address " + param.getAddress());
            }
            return true;
        }
    }

    private UpdatePortForwardingRuleResultObject composeResultObject(ManagementServer ms, FirewallRuleVO firewallRule) {
        UpdatePortForwardingRuleResultObject resultObject = new UpdatePortForwardingRuleResultObject();

        IPAddressVO ipAddressVO = ms.findIPAddressById(firewallRule.getPublicIpAddress());
        Criteria c = new Criteria();
        c.addCriteria(Criteria.ACCOUNTID, new Object[] {ipAddressVO.getAccountId()});
        c.addCriteria(Criteria.DATACENTERID, ipAddressVO.getDataCenterId());
        c.addCriteria(Criteria.IPADDRESS, firewallRule.getPrivateIpAddress());
        List<UserVmVO> userVMs = ms.searchForUserVMs(c);

        if ((userVMs != null) && (userVMs.size() > 0)) {
            UserVmVO userVM = userVMs.get(0);
            resultObject.setVirtualMachineId(userVM.getId());
            resultObject.setVirtualMachineName(userVM.getName());
            resultObject.setVirtualMachineDisplayName(userVM.getDisplayName());
        }

        resultObject.setId(firewallRule.getId());
        resultObject.setPublicIp(firewallRule.getPublicIpAddress());
        resultObject.setPrivateIp(firewallRule.getPrivateIpAddress());
        resultObject.setPublicPort(firewallRule.getPublicPort());
        resultObject.setPrivatePort(firewallRule.getPrivatePort());
        resultObject.setProtocol(firewallRule.getProtocol());

        return resultObject;
    }
}
