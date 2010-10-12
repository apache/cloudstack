/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.AccountResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.server.Criteria;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.UserContext;
import com.cloud.user.UserStatisticsVO;
import com.cloud.uservm.UserVm;
import com.cloud.vm.State;

@Implementation(method="searchForAccounts", description="Lists accounts and provides detailed account information for listed accounts")
public class ListAccountsCmd extends BaseListCmd {
	public static final Logger s_logger = Logger.getLogger(ListAccountsCmd.class.getName());
    private static final String s_name = "listaccountsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING, description="list account for a specified account. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name="accounttype", type=CommandType.LONG, description="list accounts by account type. Valid account types are 1 (admin), 2 (domain-admin), and 0 (user).")
    private Long accountType;

    @Parameter(name="domainid", type=CommandType.LONG, description="list all accounts in specified domain. If used with the account parameter, retrieves account information for specified account in specified domain.")
    private Long domainId;

    @Parameter(name="id", type=CommandType.LONG, description="list account by account ID")
    private Long id;

    @Parameter(name="iscleanuprequired", type=CommandType.BOOLEAN, description="list accounts by cleanuprequred attribute (values are true or false)")
    private Boolean cleanupRequired;

    @Parameter(name="name", type=CommandType.STRING, description="list account by account name")
    private String searchName;

    @Parameter(name="state", type=CommandType.STRING, description="list accounts by state. Valid states are enabled, disabled, and locked.")
    private String state;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getAccountType() {
        return accountType;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getId() {
        return id;
    }

    public Boolean isCleanupRequired() {
        return cleanupRequired;
    }

    public String getSearchName() {
        return searchName;
    }

    public String getState() {
        return state;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public ListResponse<AccountResponse> getResponse() {
        List<AccountVO> accounts = (List<AccountVO>)getResponseObject();

        ListResponse<AccountResponse> response = new ListResponse<AccountResponse>();

        List<AccountResponse> accountResponses = new ArrayList<AccountResponse>();
        for (AccountVO account : accounts) {
            boolean accountIsAdmin = (account.getType() == Account.ACCOUNT_TYPE_ADMIN);

            AccountResponse acctResponse = new AccountResponse();
            acctResponse.setId(account.getId());
            acctResponse.setName(account.getAccountName());
            acctResponse.setAccountType(account.getType());
            acctResponse.setDomainId(account.getDomainId());
            acctResponse.setDomainName(ApiDBUtils.findDomainById(account.getDomainId()).getName());

            //get network stat
            List<UserStatisticsVO> stats = ApiDBUtils.listUserStatsBy(account.getId());
            if (stats == null) {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Internal error searching for user stats");
            }

            long bytesSent = 0;
            long bytesReceived = 0;
            for (UserStatisticsVO stat : stats) {
                long rx = stat.getNetBytesReceived() + stat.getCurrentBytesReceived();
                long tx = stat.getNetBytesSent() + stat.getCurrentBytesSent();
                bytesReceived = bytesReceived + Long.valueOf(rx);
                bytesSent = bytesSent + Long.valueOf(tx);
            }
            acctResponse.setBytesReceived(bytesReceived);
            acctResponse.setBytesSent(bytesSent);

            // Get resource limits and counts
            
            long vmLimit = ApiDBUtils.findCorrectResourceLimit(ResourceType.user_vm, account.getId());
            String vmLimitDisplay = (accountIsAdmin || vmLimit == -1) ? "Unlimited" : String.valueOf(vmLimit);
            long vmTotal = ApiDBUtils.getResourceCount(ResourceType.user_vm, account.getId());
            String vmAvail = (accountIsAdmin || vmLimit == -1) ? "Unlimited" : String.valueOf(vmLimit - vmTotal);
            acctResponse.setVmLimit(vmLimitDisplay);
            acctResponse.setVmTotal(vmTotal);
            acctResponse.setVmAvailable(vmAvail);
            
            long ipLimit = ApiDBUtils.findCorrectResourceLimit(ResourceType.public_ip, account.getId());
            String ipLimitDisplay = (accountIsAdmin || ipLimit == -1) ? "Unlimited" : String.valueOf(ipLimit);
            long ipTotal = ApiDBUtils.getResourceCount(ResourceType.public_ip, account.getId());
            String ipAvail = (accountIsAdmin || ipLimit == -1) ? "Unlimited" : String.valueOf(ipLimit - ipTotal);
            acctResponse.setIpLimit(ipLimitDisplay);
            acctResponse.setIpTotal(ipTotal);
            acctResponse.setIpAvailable(ipAvail);
            
            long volumeLimit = ApiDBUtils.findCorrectResourceLimit(ResourceType.volume, account.getId());
            String volumeLimitDisplay = (accountIsAdmin || volumeLimit == -1) ? "Unlimited" : String.valueOf(volumeLimit);
            long volumeTotal = ApiDBUtils.getResourceCount(ResourceType.volume, account.getId());
            String volumeAvail = (accountIsAdmin || volumeLimit == -1) ? "Unlimited" : String.valueOf(volumeLimit - volumeTotal);
            acctResponse.setVolumeLimit(volumeLimitDisplay);
            acctResponse.setVolumeTotal(volumeTotal);
            acctResponse.setVolumeAvailable(volumeAvail);
            
            long snapshotLimit = ApiDBUtils.findCorrectResourceLimit(ResourceType.snapshot, account.getId());
            String snapshotLimitDisplay = (accountIsAdmin || snapshotLimit == -1) ? "Unlimited" : String.valueOf(snapshotLimit);
            long snapshotTotal = ApiDBUtils.getResourceCount(ResourceType.snapshot, account.getId());
            String snapshotAvail = (accountIsAdmin || snapshotLimit == -1) ? "Unlimited" : String.valueOf(snapshotLimit - snapshotTotal);
            acctResponse.setSnapshotLimit(snapshotLimitDisplay);
            acctResponse.setSnapshotTotal(snapshotTotal);
            acctResponse.setSnapshotAvailable(snapshotAvail);
            
            long templateLimit = ApiDBUtils.findCorrectResourceLimit(ResourceType.template, account.getId());
            String templateLimitDisplay = (accountIsAdmin || templateLimit == -1) ? "Unlimited" : String.valueOf(templateLimit);
            long templateTotal = ApiDBUtils.getResourceCount(ResourceType.template, account.getId());
            String templateAvail = (accountIsAdmin || templateLimit == -1) ? "Unlimited" : String.valueOf(templateLimit - templateTotal);
            acctResponse.setTemplateLimit(templateLimitDisplay);
            acctResponse.setTemplateTotal(templateTotal);
            acctResponse.setTemplateAvailable(templateAvail);
            
            // Get stopped and running VMs
            int vmStopped = 0;
            int vmRunning = 0;

            Long[] accountIds = new Long[1];
            accountIds[0] = account.getId();

            Criteria c1 = new Criteria();
            c1.addCriteria(Criteria.ACCOUNTID, accountIds);
            List<? extends UserVm> virtualMachines = ApiDBUtils.searchForUserVMs(c1);

            //get Running/Stopped VMs
            for (Iterator<? extends UserVm> iter = virtualMachines.iterator(); iter.hasNext();) {
                // count how many stopped/running vms we have
                UserVm vm = iter.next();

                if (vm.getState() == State.Stopped) {
                    vmStopped++;
                } else if (vm.getState() == State.Running) {
                    vmRunning++;
                }
            }

            acctResponse.setVmStopped(vmStopped);
            acctResponse.setVmRunning(vmRunning);

            //show this info to admins only
            Account ctxAccount = (Account)UserContext.current().getAccountObject();
            if ((ctxAccount == null) || isAdmin(ctxAccount.getType())) {
                acctResponse.setState(account.getState());
                acctResponse.setCleanupRequired(account.getNeedsCleanup());
            }

            acctResponse.setResponseName("account");
            accountResponses.add(acctResponse);
        }

        response.setResponses(accountResponses);
        response.setResponseName(getName());
        return response;
    }
}
