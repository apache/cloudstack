// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.api.query.dao;

import java.util.EnumSet;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.api.ApiConstants.DomainDetails;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.ResourceLimitAndCountResponse;
import org.apache.cloudstack.api.response.UserResponse;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.ViewResponseHelper;
import com.cloud.api.query.vo.AccountJoinVO;
import com.cloud.api.query.vo.UserAccountJoinVO;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class AccountJoinDaoImpl extends GenericDaoBase<AccountJoinVO, Long> implements AccountJoinDao {
    public static final Logger s_logger = Logger.getLogger(AccountJoinDaoImpl.class);

    private final SearchBuilder<AccountJoinVO> acctIdSearch;
    @Inject
    AccountManager _acctMgr;

    protected AccountJoinDaoImpl() {

        acctIdSearch = createSearchBuilder();
        acctIdSearch.and("id", acctIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        acctIdSearch.done();

        _count = "select count(distinct id) from account_view WHERE ";
    }

    @Override
    public AccountResponse newAccountResponse(ResponseView view, EnumSet<DomainDetails> details, AccountJoinVO account) {
        AccountResponse accountResponse = new AccountResponse();
        accountResponse.setId(account.getUuid());
        accountResponse.setName(account.getAccountName());
        accountResponse.setAccountType(account.getType());
        accountResponse.setDomainId(account.getDomainUuid());
        accountResponse.setDomainName(account.getDomainName());
        StringBuilder domainPath = new StringBuilder("ROOT");
        (domainPath.append(account.getDomainPath())).deleteCharAt(domainPath.length() - 1);
        accountResponse.setDomainPath(domainPath.toString());
        accountResponse.setState(account.getState().toString());
        accountResponse.setNetworkDomain(account.getNetworkDomain());
        accountResponse.setDefaultZone(account.getDataCenterUuid());
        accountResponse.setIsDefault(account.isDefault());

        // get network stat
        accountResponse.setBytesReceived(account.getBytesReceived());
        accountResponse.setBytesSent(account.getBytesSent());

        if (details.contains(DomainDetails.all) || details.contains(DomainDetails.resource)) {
            boolean fullView = (view == ResponseView.Full && _acctMgr.isRootAdmin(account.getId()));
            setResourceLimits(account, fullView, accountResponse);

            //get resource limits for projects
            long projectLimit = ApiDBUtils.findCorrectResourceLimit(account.getProjectLimit(), account.getId(), ResourceType.project);
            String projectLimitDisplay = (fullView || projectLimit == -1) ? "Unlimited" : String.valueOf(projectLimit);
            long projectTotal = (account.getProjectTotal() == null) ? 0 : account.getProjectTotal();
            String projectAvail = (fullView || projectLimit == -1) ? "Unlimited" : String.valueOf(projectLimit - projectTotal);
            accountResponse.setProjectLimit(projectLimitDisplay);
            accountResponse.setProjectTotal(projectTotal);
            accountResponse.setProjectAvailable(projectAvail);
        }

        // set async job
        if (account.getJobId() != null) {
            accountResponse.setJobId(account.getJobUuid());
            accountResponse.setJobStatus(account.getJobStatus());
        }

        // adding all the users for an account as part of the response obj
        List<UserAccountJoinVO> usersForAccount = ApiDBUtils.findUserViewByAccountId(account.getId());
        List<UserResponse> userResponses = ViewResponseHelper.createUserResponse(usersForAccount.toArray(new UserAccountJoinVO[usersForAccount.size()]));
        accountResponse.setUsers(userResponses);

        // set details
        accountResponse.setDetails(ApiDBUtils.getAccountDetails(account.getId()));
        accountResponse.setObjectName("account");

        // add all the acl groups for an account
        accountResponse.setGroups(_acctMgr.listAclGroupsByAccount(account.getId()));

        return accountResponse;
    }

    @Override
    public void setResourceLimits(AccountJoinVO account, boolean fullView, ResourceLimitAndCountResponse response) {
        // Get resource limits and counts
        long vmLimit = ApiDBUtils.findCorrectResourceLimit(account.getVmLimit(), account.getId(), ResourceType.user_vm);
        String vmLimitDisplay = (fullView || vmLimit == -1) ? "Unlimited" : String.valueOf(vmLimit);
        long vmTotal = (account.getVmTotal() == null) ? 0 : account.getVmTotal();
        String vmAvail = (fullView || vmLimit == -1) ? "Unlimited" : String.valueOf(vmLimit - vmTotal);
        response.setVmLimit(vmLimitDisplay);
        response.setVmTotal(vmTotal);
        response.setVmAvailable(vmAvail);

        long ipLimit = ApiDBUtils.findCorrectResourceLimit(account.getIpLimit(), account.getId(), ResourceType.public_ip);
        String ipLimitDisplay = (fullView || ipLimit == -1) ? "Unlimited" : String.valueOf(ipLimit);
        long ipTotal = (account.getIpTotal() == null) ? 0 : account.getIpTotal();

        Long ips = ipLimit - ipTotal;
        // check how many free ips are left, and if it's less than max allowed number of ips from account - use this
        // value
        Long ipsLeft = account.getIpFree();
        boolean unlimited = true;
        if (ips.longValue() > ipsLeft.longValue()) {
            ips = ipsLeft;
            unlimited = false;
        }

        String ipAvail = ((fullView || ipLimit == -1) && unlimited) ? "Unlimited" : String.valueOf(ips);

        response.setIpLimit(ipLimitDisplay);
        response.setIpTotal(ipTotal);
        response.setIpAvailable(ipAvail);

        long volumeLimit = ApiDBUtils.findCorrectResourceLimit(account.getVolumeLimit(), account.getId(), ResourceType.volume);
        String volumeLimitDisplay = (fullView || volumeLimit == -1) ? "Unlimited" : String.valueOf(volumeLimit);
        long volumeTotal = (account.getVolumeTotal() == null) ? 0 : account.getVolumeTotal();
        String volumeAvail = (fullView || volumeLimit == -1) ? "Unlimited" : String.valueOf(volumeLimit - volumeTotal);
        response.setVolumeLimit(volumeLimitDisplay);
        response.setVolumeTotal(volumeTotal);
        response.setVolumeAvailable(volumeAvail);

        long snapshotLimit = ApiDBUtils.findCorrectResourceLimit(account.getSnapshotLimit(), account.getId(), ResourceType.snapshot);
        String snapshotLimitDisplay = (fullView || snapshotLimit == -1) ? "Unlimited" : String.valueOf(snapshotLimit);
        long snapshotTotal = (account.getSnapshotTotal() == null) ? 0 : account.getSnapshotTotal();
        String snapshotAvail = (fullView || snapshotLimit == -1) ? "Unlimited" : String.valueOf(snapshotLimit - snapshotTotal);
        response.setSnapshotLimit(snapshotLimitDisplay);
        response.setSnapshotTotal(snapshotTotal);
        response.setSnapshotAvailable(snapshotAvail);

        Long templateLimit = ApiDBUtils.findCorrectResourceLimit(account.getTemplateLimit(), account.getId(), ResourceType.template);
        String templateLimitDisplay = (fullView || templateLimit == -1) ? "Unlimited" : String.valueOf(templateLimit);
        Long templateTotal = (account.getTemplateTotal() == null) ? 0 : account.getTemplateTotal();
        String templateAvail = (fullView || templateLimit == -1) ? "Unlimited" : String.valueOf(templateLimit - templateTotal);
        response.setTemplateLimit(templateLimitDisplay);
        response.setTemplateTotal(templateTotal);
        response.setTemplateAvailable(templateAvail);

        // Get stopped and running VMs
        response.setVmStopped(account.getVmStopped()!=null ? account.getVmStopped() : 0);
        response.setVmRunning(account.getVmRunning()!=null ? account.getVmRunning() : 0);

        //get resource limits for networks
        long networkLimit = ApiDBUtils.findCorrectResourceLimit(account.getNetworkLimit(), account.getId(), ResourceType.network);
        String networkLimitDisplay = (fullView || networkLimit == -1) ? "Unlimited" : String.valueOf(networkLimit);
        long networkTotal = (account.getNetworkTotal() == null) ? 0 : account.getNetworkTotal();
        String networkAvail = (fullView || networkLimit == -1) ? "Unlimited" : String.valueOf(networkLimit - networkTotal);
        response.setNetworkLimit(networkLimitDisplay);
        response.setNetworkTotal(networkTotal);
        response.setNetworkAvailable(networkAvail);

        //get resource limits for vpcs
        long vpcLimit = ApiDBUtils.findCorrectResourceLimit(account.getVpcLimit(), account.getId(), ResourceType.vpc);
        String vpcLimitDisplay = (fullView || vpcLimit == -1) ? "Unlimited" : String.valueOf(vpcLimit);
        long vpcTotal = (account.getVpcTotal() == null) ? 0 : account.getVpcTotal();
        String vpcAvail = (fullView || vpcLimit == -1) ? "Unlimited" : String.valueOf(vpcLimit - vpcTotal);
        response.setVpcLimit(vpcLimitDisplay);
        response.setVpcTotal(vpcTotal);
        response.setVpcAvailable(vpcAvail);

        //get resource limits for cpu cores
        long cpuLimit = ApiDBUtils.findCorrectResourceLimit(account.getCpuLimit(), account.getId(), ResourceType.cpu);
        String cpuLimitDisplay = (fullView || cpuLimit == -1) ? "Unlimited" : String.valueOf(cpuLimit);
        long cpuTotal = (account.getCpuTotal() == null) ? 0 : account.getCpuTotal();
        String cpuAvail = (fullView || cpuLimit == -1) ? "Unlimited" : String.valueOf(cpuLimit - cpuTotal);
        response.setCpuLimit(cpuLimitDisplay);
        response.setCpuTotal(cpuTotal);
        response.setCpuAvailable(cpuAvail);

        //get resource limits for memory
        long memoryLimit = ApiDBUtils.findCorrectResourceLimit(account.getMemoryLimit(), account.getId(), ResourceType.memory);
        String memoryLimitDisplay = (fullView || memoryLimit == -1) ? "Unlimited" : String.valueOf(memoryLimit);
        long memoryTotal = (account.getMemoryTotal() == null) ? 0 : account.getMemoryTotal();
        String memoryAvail = (fullView || memoryLimit == -1) ? "Unlimited" : String.valueOf(memoryLimit - memoryTotal);
        response.setMemoryLimit(memoryLimitDisplay);
        response.setMemoryTotal(memoryTotal);
        response.setMemoryAvailable(memoryAvail);

      //get resource limits for primary storage space and convert it from Bytes to GiB
        long primaryStorageLimit = ApiDBUtils.findCorrectResourceLimit(account.getPrimaryStorageLimit(), account.getId(), ResourceType.primary_storage);
        String primaryStorageLimitDisplay = (fullView || primaryStorageLimit == -1) ? "Unlimited" : String.valueOf(primaryStorageLimit / ResourceType.bytesToGiB);
        long primaryStorageTotal = (account.getPrimaryStorageTotal() == null) ? 0 : (account.getPrimaryStorageTotal() / ResourceType.bytesToGiB);
        String primaryStorageAvail = (fullView || primaryStorageLimit == -1) ? "Unlimited" : String.valueOf((primaryStorageLimit / ResourceType.bytesToGiB) - primaryStorageTotal);

        response.setPrimaryStorageLimit(primaryStorageLimitDisplay);
        response.setPrimaryStorageTotal(primaryStorageTotal);
        response.setPrimaryStorageAvailable(primaryStorageAvail);

        //get resource limits for secondary storage space and convert it from Bytes to GiB
        long secondaryStorageLimit = ApiDBUtils.findCorrectResourceLimit(account.getSecondaryStorageLimit(), account.getId(), ResourceType.secondary_storage);
        String secondaryStorageLimitDisplay = (fullView || secondaryStorageLimit == -1) ? "Unlimited" : String.valueOf(secondaryStorageLimit / ResourceType.bytesToGiB);
        float secondaryStorageTotal = (account.getSecondaryStorageTotal() == null) ? 0 : (account.getSecondaryStorageTotal() / (ResourceType.bytesToGiB * 1f));
        String secondaryStorageAvail = (fullView || secondaryStorageLimit == -1) ? "Unlimited" : String.valueOf((secondaryStorageLimit / ResourceType.bytesToGiB)
                - secondaryStorageTotal);

        response.setSecondaryStorageLimit(secondaryStorageLimitDisplay);
        response.setSecondaryStorageTotal(secondaryStorageTotal);
        response.setSecondaryStorageAvailable(secondaryStorageAvail);
    }

    @Override
    public AccountJoinVO newAccountView(Account acct) {
        SearchCriteria<AccountJoinVO> sc = acctIdSearch.create();
        sc.setParameters("id", acct.getId());
        List<AccountJoinVO> accounts = searchIncludingRemoved(sc, null, null, false);
        assert accounts != null && accounts.size() == 1 : "No account found for account id " + acct.getId();
        return accounts.get(0);

    }

}
