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

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.ResourceLimitAndCountResponse;
import org.apache.cloudstack.api.response.UserResponse;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.ViewResponseHelper;
import com.cloud.api.query.vo.AccountJoinVO;
import com.cloud.api.query.vo.UserAccountJoinVO;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.user.Account;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value = {AccountJoinDao.class})
public class AccountJoinDaoImpl extends GenericDaoBase<AccountJoinVO, Long> implements AccountJoinDao {
    public static final Logger s_logger = Logger.getLogger(AccountJoinDaoImpl.class);

    private SearchBuilder<AccountJoinVO> acctIdSearch;

    protected AccountJoinDaoImpl() {

        acctIdSearch = createSearchBuilder();
        acctIdSearch.and("id", acctIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        acctIdSearch.done();

        this._count = "select count(distinct id) from account_view WHERE ";
    }

    @Override
    public AccountResponse newAccountResponse(AccountJoinVO account) {
        AccountResponse accountResponse = new AccountResponse();
        accountResponse.setId(account.getUuid());
        accountResponse.setName(account.getAccountName());
        accountResponse.setAccountType(account.getType());
        accountResponse.setDomainId(account.getDomainUuid());
        accountResponse.setDomainName(account.getDomainName());
        accountResponse.setState(account.getState().toString());
        accountResponse.setNetworkDomain(account.getNetworkDomain());
        accountResponse.setDefaultZone(account.getDataCenterUuid());
        accountResponse.setIsDefault(account.isDefault());

        // get network stat
        accountResponse.setBytesReceived(account.getBytesReceived());
        accountResponse.setBytesSent(account.getBytesSent());

        boolean accountIsAdmin = (account.getType() == Account.ACCOUNT_TYPE_ADMIN);
        setResourceLimits(account, accountIsAdmin, accountResponse);

        //get resource limits for projects
        long projectLimit = ApiDBUtils.findCorrectResourceLimit(account.getProjectLimit(), account.getType(), ResourceType.project);
        String projectLimitDisplay = (accountIsAdmin || projectLimit == -1) ? "Unlimited" : String.valueOf(projectLimit);
        long projectTotal = (account.getProjectTotal() == null) ? 0 : account.getProjectTotal();
        String projectAvail = (accountIsAdmin || projectLimit == -1) ? "Unlimited" : String.valueOf(projectLimit - projectTotal);
        accountResponse.setProjectLimit(projectLimitDisplay);
        accountResponse.setProjectTotal(projectTotal);
        accountResponse.setProjectAvailable(projectAvail);

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

        return accountResponse;
    }

    @Override
    public void setResourceLimits(AccountJoinVO account, boolean accountIsAdmin, ResourceLimitAndCountResponse response) {
        // Get resource limits and counts
        long vmLimit = ApiDBUtils.findCorrectResourceLimit(account.getVmLimit(), account.getType(), ResourceType.user_vm);
        String vmLimitDisplay = (accountIsAdmin || vmLimit == -1) ? "Unlimited" : String.valueOf(vmLimit);
        long vmTotal = (account.getVmTotal() == null) ? 0 : account.getVmTotal();
        String vmAvail = (accountIsAdmin || vmLimit == -1) ? "Unlimited" : String.valueOf(vmLimit - vmTotal);
        response.setVmLimit(vmLimitDisplay);
        response.setVmTotal(vmTotal);
        response.setVmAvailable(vmAvail);

        long ipLimit = ApiDBUtils.findCorrectResourceLimit(account.getIpLimit(), account.getType(), ResourceType.public_ip);
        String ipLimitDisplay = (accountIsAdmin || ipLimit == -1) ? "Unlimited" : String.valueOf(ipLimit);
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

        String ipAvail = ((accountIsAdmin || ipLimit == -1) && unlimited) ? "Unlimited" : String.valueOf(ips);

        response.setIpLimit(ipLimitDisplay);
        response.setIpTotal(ipTotal);
        response.setIpAvailable(ipAvail);

        long volumeLimit = ApiDBUtils.findCorrectResourceLimit(account.getVolumeLimit(), account.getType(), ResourceType.volume);
        String volumeLimitDisplay = (accountIsAdmin || volumeLimit == -1) ? "Unlimited" : String.valueOf(volumeLimit);
        long volumeTotal = (account.getVolumeTotal() == 0) ? 0 : account.getVolumeTotal();
        String volumeAvail = (accountIsAdmin || volumeLimit == -1) ? "Unlimited" : String.valueOf(volumeLimit - volumeTotal);
        response.setVolumeLimit(volumeLimitDisplay);
        response.setVolumeTotal(volumeTotal);
        response.setVolumeAvailable(volumeAvail);

        long snapshotLimit = ApiDBUtils.findCorrectResourceLimit(account.getSnapshotLimit(), account.getType(), ResourceType.snapshot);
        String snapshotLimitDisplay = (accountIsAdmin || snapshotLimit == -1) ? "Unlimited" : String.valueOf(snapshotLimit);
        long snapshotTotal = (account.getSnapshotTotal() == null) ? 0 : account.getSnapshotTotal();
        String snapshotAvail = (accountIsAdmin || snapshotLimit == -1) ? "Unlimited" : String.valueOf(snapshotLimit - snapshotTotal);
        response.setSnapshotLimit(snapshotLimitDisplay);
        response.setSnapshotTotal(snapshotTotal);
        response.setSnapshotAvailable(snapshotAvail);

        Long templateLimit = ApiDBUtils.findCorrectResourceLimit(account.getTemplateLimit(), account.getType(), ResourceType.template);
        String templateLimitDisplay = (accountIsAdmin || templateLimit == -1) ? "Unlimited" : String.valueOf(templateLimit);
        Long templateTotal = (account.getTemplateTotal() == null) ? 0 : account.getTemplateTotal();
        String templateAvail = (accountIsAdmin || templateLimit == -1) ? "Unlimited" : String.valueOf(templateLimit - templateTotal);
        response.setTemplateLimit(templateLimitDisplay);
        response.setTemplateTotal(templateTotal);
        response.setTemplateAvailable(templateAvail);

        // Get stopped and running VMs
        response.setVmStopped(account.getVmStopped());
        response.setVmRunning(account.getVmRunning());

        //get resource limits for networks
        long networkLimit = ApiDBUtils.findCorrectResourceLimit(account.getNetworkLimit(), account.getType(), ResourceType.network);
        String networkLimitDisplay = (accountIsAdmin || networkLimit == -1) ? "Unlimited" : String.valueOf(networkLimit);
        long networkTotal = (account.getNetworkTotal() == null) ? 0 : account.getNetworkTotal();
        String networkAvail = (accountIsAdmin || networkLimit == -1) ? "Unlimited" : String.valueOf(networkLimit - networkTotal);
        response.setNetworkLimit(networkLimitDisplay);
        response.setNetworkTotal(networkTotal);
        response.setNetworkAvailable(networkAvail);

        //get resource limits for vpcs
        long vpcLimit = ApiDBUtils.findCorrectResourceLimit(account.getVpcLimit(), account.getType(), ResourceType.vpc);
        String vpcLimitDisplay = (accountIsAdmin || vpcLimit == -1) ? "Unlimited" : String.valueOf(vpcLimit);
        long vpcTotal = (account.getVpcTotal() == null) ? 0 : account.getVpcTotal();
        String vpcAvail = (accountIsAdmin || vpcLimit == -1) ? "Unlimited" : String.valueOf(vpcLimit - vpcTotal);
        response.setVpcLimit(vpcLimitDisplay);
        response.setVpcTotal(vpcTotal);
        response.setVpcAvailable(vpcAvail);

        //get resource limits for cpu cores
        long cpuLimit = ApiDBUtils.findCorrectResourceLimit(account.getCpuLimit(), account.getType(), ResourceType.cpu);
        String cpuLimitDisplay = (accountIsAdmin || cpuLimit == -1) ? "Unlimited" : String.valueOf(cpuLimit);
        long cpuTotal = (account.getCpuTotal() == null) ? 0 : account.getCpuTotal();
        String cpuAvail = (accountIsAdmin || cpuLimit == -1) ? "Unlimited" : String.valueOf(cpuLimit - cpuTotal);
        response.setCpuLimit(cpuLimitDisplay);
        response.setCpuTotal(cpuTotal);
        response.setCpuAvailable(cpuAvail);

        //get resource limits for memory
        long memoryLimit = ApiDBUtils.findCorrectResourceLimit(account.getMemoryLimit(), account.getType(), ResourceType.memory);
        String memoryLimitDisplay = (accountIsAdmin || memoryLimit == -1) ? "Unlimited" : String.valueOf(memoryLimit);
        long memoryTotal = (account.getMemoryTotal() == null) ? 0 : account.getMemoryTotal();
        String memoryAvail = (accountIsAdmin || memoryLimit == -1) ? "Unlimited" : String.valueOf(memoryLimit - memoryTotal);
        response.setMemoryLimit(memoryLimitDisplay);
        response.setMemoryTotal(memoryTotal);
        response.setMemoryAvailable(memoryAvail);

        //get resource limits for primary storage space and convert it from Bytes to GiB
        long primaryStorageLimit = ApiDBUtils.findCorrectResourceLimit(account.getPrimaryStorageLimit(), account.getType(), ResourceType.primary_storage);
        String primaryStorageLimitDisplay = (accountIsAdmin || primaryStorageLimit == -1) ? "Unlimited" : String.valueOf(primaryStorageLimit / ResourceType.bytesToGiB);
        long primaryStorageTotal = (account.getPrimaryStorageTotal() == null) ? 0 : (account.getPrimaryStorageTotal() / ResourceType.bytesToGiB);
        String primaryStorageAvail =
            (accountIsAdmin || primaryStorageLimit == -1) ? "Unlimited" : String.valueOf((primaryStorageLimit / ResourceType.bytesToGiB) - primaryStorageTotal);
        response.setPrimaryStorageLimit(primaryStorageLimitDisplay);
        response.setPrimaryStorageTotal(primaryStorageTotal);
        response.setPrimaryStorageAvailable(primaryStorageAvail);

        //get resource limits for secondary storage space and convert it from Bytes to GiB
        long secondaryStorageLimit = ApiDBUtils.findCorrectResourceLimit(account.getSecondaryStorageLimit(), account.getType(), ResourceType.secondary_storage);
        String secondaryStorageLimitDisplay =
            (accountIsAdmin || secondaryStorageLimit == -1) ? "Unlimited" : String.valueOf(secondaryStorageLimit / ResourceType.bytesToGiB);
        long secondaryStorageTotal = (account.getSecondaryStorageTotal() == null) ? 0 : (account.getSecondaryStorageTotal() / ResourceType.bytesToGiB);
        String secondaryStorageAvail =
            (accountIsAdmin || secondaryStorageLimit == -1) ? "Unlimited" : String.valueOf((secondaryStorageLimit / ResourceType.bytesToGiB) - secondaryStorageTotal);
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
