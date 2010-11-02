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
package com.cloud.api;

import java.util.Iterator;
import java.util.List;

import com.cloud.api.response.AccountResponse;
import com.cloud.api.response.DiskOfferingResponse;
import com.cloud.api.response.DomainResponse;
import com.cloud.api.response.UserResponse;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.domain.DomainVO;
import com.cloud.server.Criteria;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.user.Account;
import com.cloud.user.UserAccount;
import com.cloud.user.UserStatisticsVO;
import com.cloud.uservm.UserVm;
import com.cloud.vm.State;

public class ApiResponseHelper {
    
    public static UserResponse createUserResponse (UserAccount user) {
        UserResponse userResponse = new UserResponse();
        userResponse.setAccountName(user.getAccountName());
        userResponse.setAccountType(user.getType());
        userResponse.setCreated(user.getCreated());
        userResponse.setDomainId(user.getDomainId());
        userResponse.setDomainName(ApiDBUtils.findDomainById(user.getDomainId()).getName());
        userResponse.setEmail(user.getEmail());
        userResponse.setFirstname(user.getFirstname());
        userResponse.setId(user.getId());
        userResponse.setLastname(user.getLastname());
        userResponse.setState(user.getState());
        userResponse.setTimezone(user.getTimezone());
        userResponse.setUsername(user.getUsername());
        userResponse.setApiKey(user.getApiKey());
        userResponse.setSecretKey(user.getSecretKey());
        
        return userResponse;
    }
    
   public static AccountResponse createAccountResponse (Account account) {
       boolean accountIsAdmin = (account.getType() == Account.ACCOUNT_TYPE_ADMIN);
       AccountResponse accountResponse = new AccountResponse();
       accountResponse.setId(account.getId());
       accountResponse.setName(account.getAccountName());
       accountResponse.setAccountType(account.getType());
       accountResponse.setDomainId(account.getDomainId());
       accountResponse.setDomainName(ApiDBUtils.findDomainById(account.getDomainId()).getName());

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
       accountResponse.setBytesReceived(bytesReceived);
       accountResponse.setBytesSent(bytesSent);

       // Get resource limits and counts
       
       long vmLimit = ApiDBUtils.findCorrectResourceLimit(ResourceType.user_vm, account.getId());
       String vmLimitDisplay = (accountIsAdmin || vmLimit == -1) ? "Unlimited" : String.valueOf(vmLimit);
       long vmTotal = ApiDBUtils.getResourceCount(ResourceType.user_vm, account.getId());
       String vmAvail = (accountIsAdmin || vmLimit == -1) ? "Unlimited" : String.valueOf(vmLimit - vmTotal);
       accountResponse.setVmLimit(vmLimitDisplay);
       accountResponse.setVmTotal(vmTotal);
       accountResponse.setVmAvailable(vmAvail);
       
       long ipLimit = ApiDBUtils.findCorrectResourceLimit(ResourceType.public_ip, account.getId());
       String ipLimitDisplay = (accountIsAdmin || ipLimit == -1) ? "Unlimited" : String.valueOf(ipLimit);
       long ipTotal = ApiDBUtils.getResourceCount(ResourceType.public_ip, account.getId());
       String ipAvail = (accountIsAdmin || ipLimit == -1) ? "Unlimited" : String.valueOf(ipLimit - ipTotal);
       accountResponse.setIpLimit(ipLimitDisplay);
       accountResponse.setIpTotal(ipTotal);
       accountResponse.setIpAvailable(ipAvail);
       
       long volumeLimit = ApiDBUtils.findCorrectResourceLimit(ResourceType.volume, account.getId());
       String volumeLimitDisplay = (accountIsAdmin || volumeLimit == -1) ? "Unlimited" : String.valueOf(volumeLimit);
       long volumeTotal = ApiDBUtils.getResourceCount(ResourceType.volume, account.getId());
       String volumeAvail = (accountIsAdmin || volumeLimit == -1) ? "Unlimited" : String.valueOf(volumeLimit - volumeTotal);
       accountResponse.setVolumeLimit(volumeLimitDisplay);
       accountResponse.setVolumeTotal(volumeTotal);
       accountResponse.setVolumeAvailable(volumeAvail);
       
       long snapshotLimit = ApiDBUtils.findCorrectResourceLimit(ResourceType.snapshot, account.getId());
       String snapshotLimitDisplay = (accountIsAdmin || snapshotLimit == -1) ? "Unlimited" : String.valueOf(snapshotLimit);
       long snapshotTotal = ApiDBUtils.getResourceCount(ResourceType.snapshot, account.getId());
       String snapshotAvail = (accountIsAdmin || snapshotLimit == -1) ? "Unlimited" : String.valueOf(snapshotLimit - snapshotTotal);
       accountResponse.setSnapshotLimit(snapshotLimitDisplay);
       accountResponse.setSnapshotTotal(snapshotTotal);
       accountResponse.setSnapshotAvailable(snapshotAvail);
       
       long templateLimit = ApiDBUtils.findCorrectResourceLimit(ResourceType.template, account.getId());
       String templateLimitDisplay = (accountIsAdmin || templateLimit == -1) ? "Unlimited" : String.valueOf(templateLimit);
       long templateTotal = ApiDBUtils.getResourceCount(ResourceType.template, account.getId());
       String templateAvail = (accountIsAdmin || templateLimit == -1) ? "Unlimited" : String.valueOf(templateLimit - templateTotal);
       accountResponse.setTemplateLimit(templateLimitDisplay);
       accountResponse.setTemplateTotal(templateTotal);
       accountResponse.setTemplateAvailable(templateAvail);
       
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

       accountResponse.setVmStopped(vmStopped);
       accountResponse.setVmRunning(vmRunning);
        
       return accountResponse;
    }
   
   public static DomainResponse createDomainResponse (DomainVO domain) {
       DomainResponse domainResponse = new DomainResponse();
       domainResponse.setDomainName(domain.getName());
       domainResponse.setId(domain.getId());
       domainResponse.setLevel(domain.getLevel());
       domainResponse.setParentDomainId(domain.getParent());
       if (domain.getParent() != null) {
           domainResponse.setParentDomainName(ApiDBUtils.findDomainById(domain.getParent()).getName());
       }
       if (domain.getChildCount() > 0) {
           domainResponse.setHasChild(true);
       }
       return domainResponse;
   }
   
   public static DiskOfferingResponse createDiskOfferingResponse (DiskOfferingVO offering) {
       DiskOfferingResponse diskOfferingResponse = new DiskOfferingResponse();
       diskOfferingResponse.setId(offering.getId());
       diskOfferingResponse.setName(offering.getName());
       diskOfferingResponse.setDisplayText(offering.getDisplayText());
       diskOfferingResponse.setCreated(offering.getCreated());
       diskOfferingResponse.setDiskSize(offering.getDiskSize());
       if(offering.getDomainId() != null){
           diskOfferingResponse.setDomain(ApiDBUtils.findDomainById(offering.getDomainId()).getName());
           diskOfferingResponse.setDomainId(offering.getDomainId());
       }
       diskOfferingResponse.setTags(offering.getTags());
       return diskOfferingResponse;
   }

}
