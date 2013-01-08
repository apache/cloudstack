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

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.ViewResponseHelper;
import com.cloud.api.query.vo.AccountJoinVO;
import com.cloud.api.query.vo.UserAccountJoinVO;
import com.cloud.configuration.Resource.ResourceType;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.UserResponse;
import com.cloud.user.Account;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;


@Local(value={AccountJoinDao.class})
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
        boolean accountIsAdmin = (account.getType() == Account.ACCOUNT_TYPE_ADMIN);
        AccountResponse accountResponse = new AccountResponse();
        accountResponse.setId(account.getUuid());
        accountResponse.setName(account.getAccountName());
        accountResponse.setAccountType(account.getType());
        accountResponse.setDomainId(account.getDomainUuid());
        accountResponse.setDomainName(account.getDomainName());
        accountResponse.setState(account.getState().toString());
        accountResponse.setNetworkDomain(account.getNetworkDomain());
        accountResponse.setDefaultZone(account.getDataCenterUuid());

        // get network stat
        accountResponse.setBytesReceived(account.getBytesReceived());
        accountResponse.setBytesSent(account.getBytesSent());

        // Get resource limits and counts

        long vmLimit = ApiDBUtils.findCorrectResourceLimit(account.getVmLimit(), account.getType(), ResourceType.user_vm);
        String vmLimitDisplay = (accountIsAdmin || vmLimit == -1) ? "Unlimited" : String.valueOf(vmLimit);
        long vmTotal = (account.getVmTotal() == null) ? 0 : account.getVmTotal();
        String vmAvail = (accountIsAdmin || vmLimit == -1) ? "Unlimited" : String.valueOf(vmLimit - vmTotal);
        accountResponse.setVmLimit(vmLimitDisplay);
        accountResponse.setVmTotal(vmTotal);
        accountResponse.setVmAvailable(vmAvail);

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

        accountResponse.setIpLimit(ipLimitDisplay);
        accountResponse.setIpTotal(ipTotal);
        accountResponse.setIpAvailable(ipAvail);

        long volumeLimit = ApiDBUtils.findCorrectResourceLimit(account.getVolumeLimit(), account.getType(), ResourceType.volume);
        String volumeLimitDisplay = (accountIsAdmin || volumeLimit == -1) ? "Unlimited" : String.valueOf(volumeLimit);
        long volumeTotal = (account.getVolumeTotal() == 0) ? 0 : account.getVolumeTotal();
        String volumeAvail = (accountIsAdmin || volumeLimit == -1) ? "Unlimited" : String.valueOf(volumeLimit - volumeTotal);
        accountResponse.setVolumeLimit(volumeLimitDisplay);
        accountResponse.setVolumeTotal(volumeTotal);
        accountResponse.setVolumeAvailable(volumeAvail);

        long snapshotLimit = ApiDBUtils.findCorrectResourceLimit(account.getSnapshotLimit(), account.getType(), ResourceType.snapshot);
        String snapshotLimitDisplay = (accountIsAdmin || snapshotLimit == -1) ? "Unlimited" : String.valueOf(snapshotLimit);
        long snapshotTotal = (account.getSnapshotTotal() == null) ? 0 : account.getSnapshotTotal();
        String snapshotAvail = (accountIsAdmin || snapshotLimit == -1) ? "Unlimited" : String.valueOf(snapshotLimit - snapshotTotal);
        accountResponse.setSnapshotLimit(snapshotLimitDisplay);
        accountResponse.setSnapshotTotal(snapshotTotal);
        accountResponse.setSnapshotAvailable(snapshotAvail);

        Long templateLimit = ApiDBUtils.findCorrectResourceLimit(account.getTemplateLimit(), account.getType(), ResourceType.template);
        String templateLimitDisplay = (accountIsAdmin || templateLimit == -1) ? "Unlimited" : String.valueOf(templateLimit);
        Long templateTotal = (account.getTemplateTotal() == null) ? 0 : account.getTemplateTotal();
        String templateAvail = (accountIsAdmin || templateLimit == -1) ? "Unlimited" : String.valueOf(templateLimit - templateTotal);
        accountResponse.setTemplateLimit(templateLimitDisplay);
        accountResponse.setTemplateTotal(templateTotal);
        accountResponse.setTemplateAvailable(templateAvail);

        // Get stopped and running VMs
        accountResponse.setVmStopped(account.getVmStopped());
        accountResponse.setVmRunning(account.getVmRunning());


        //get resource limits for projects
        long projectLimit = ApiDBUtils.findCorrectResourceLimit(account.getProjectLimit(), account.getType(), ResourceType.project);
        String projectLimitDisplay = (accountIsAdmin || projectLimit == -1) ? "Unlimited" : String.valueOf(projectLimit);
        long projectTotal = (account.getProjectTotal() == null) ? 0 : account.getProjectTotal();
        String projectAvail = (accountIsAdmin || projectLimit == -1) ? "Unlimited" : String.valueOf(projectLimit - projectTotal);
        accountResponse.setProjectLimit(projectLimitDisplay);
        accountResponse.setProjectTotal(projectTotal);
        accountResponse.setProjectAvailable(projectAvail);

        //get resource limits for networks
        long networkLimit = ApiDBUtils.findCorrectResourceLimit(account.getNetworkLimit(), account.getType(), ResourceType.network);
        String networkLimitDisplay = (accountIsAdmin || networkLimit == -1) ? "Unlimited" : String.valueOf(networkLimit);
        long networkTotal = (account.getNetworkTotal() == null) ? 0 : account.getNetworkTotal();
        String networkAvail = (accountIsAdmin || networkLimit == -1) ? "Unlimited" : String.valueOf(networkLimit - networkTotal);
        accountResponse.setNetworkLimit(networkLimitDisplay);
        accountResponse.setNetworkTotal(networkTotal);
        accountResponse.setNetworkAvailable(networkAvail);

        //get resource limits for vpcs
        long vpcLimit = ApiDBUtils.findCorrectResourceLimit(account.getVpcLimit(), account.getType(), ResourceType.vpc);
        String vpcLimitDisplay = (accountIsAdmin || vpcLimit == -1) ? "Unlimited" : String.valueOf(vpcLimit);
        long vpcTotal = (account.getVpcTotal() == null) ? 0 : account.getVpcTotal();
        String vpcAvail = (accountIsAdmin || vpcLimit == -1) ? "Unlimited" : String.valueOf(vpcLimit - vpcTotal);
        accountResponse.setNetworkLimit(vpcLimitDisplay);
        accountResponse.setNetworkTotal(vpcTotal);
        accountResponse.setNetworkAvailable(vpcAvail);

        // adding all the users for an account as part of the response obj
        List<UserAccountJoinVO> usersForAccount = ApiDBUtils.findUserViewByAccountId(account.getId());
        List<UserResponse> userResponses = ViewResponseHelper.createUserResponse(usersForAccount.toArray(new UserAccountJoinVO[usersForAccount.size()]));
        accountResponse.setUsers(userResponses);

        // set details
        accountResponse.setDetails(ApiDBUtils.getAccountDetails(account.getId()));
        accountResponse.setObjectName("account");

        // set async job
        accountResponse.setJobId(account.getJobUuid());
        accountResponse.setJobStatus(account.getJobStatus());
        return accountResponse;
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
