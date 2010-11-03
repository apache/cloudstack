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

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;

import com.cloud.api.response.AccountResponse;
import com.cloud.api.response.ConfigurationResponse;
import com.cloud.api.response.DiskOfferingResponse;
import com.cloud.api.response.DomainResponse;
import com.cloud.api.response.ResourceLimitResponse;
import com.cloud.api.response.ServiceOfferingResponse;
import com.cloud.api.response.SnapshotPolicyResponse;
import com.cloud.api.response.SnapshotResponse;
import com.cloud.api.response.UserResponse;
import com.cloud.api.response.UserVmResponse;
import com.cloud.async.AsyncJobVO;
import com.cloud.configuration.ConfigurationVO;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.domain.DomainVO;
import com.cloud.offering.NetworkOffering.GuestIpType;
import com.cloud.offering.ServiceOffering;
import com.cloud.server.Criteria;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Snapshot.SnapshotType;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.user.Account;
import com.cloud.user.UserAccount;
import com.cloud.user.UserContext;
import com.cloud.user.UserStatisticsVO;
import com.cloud.uservm.UserVm;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.State;
import com.cloud.vm.VmStats;

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
   
   public static ResourceLimitResponse createResourceLimitResponse (ResourceLimitVO limit) {
       ResourceLimitResponse resourceLimitResponse = new ResourceLimitResponse();
       if (limit.getDomainId() != null) {
           resourceLimitResponse.setDomainId(limit.getDomainId());
           resourceLimitResponse.setDomainName(ApiDBUtils.findDomainById(limit.getDomainId()).getName());
       }
           
       if (limit.getAccountId() != null) {
           Account accountTemp = ApiDBUtils.findAccountById(limit.getAccountId());
           if (accountTemp != null) {
               resourceLimitResponse.setAccountName(accountTemp.getAccountName());
               resourceLimitResponse.setDomainId(accountTemp.getDomainId());
               resourceLimitResponse.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
           }
       }
       resourceLimitResponse.setResourceType(Integer.valueOf(limit.getType().ordinal()).toString());
       resourceLimitResponse.setMax(limit.getMax());
       
       return resourceLimitResponse;
   }
   
   
   public static ServiceOfferingResponse createServiceOfferingResponse (ServiceOfferingVO offering) {
       ServiceOfferingResponse offeringResponse = new ServiceOfferingResponse();
       offeringResponse.setId(offering.getId());
       offeringResponse.setName(offering.getName());
       offeringResponse.setDisplayText(offering.getDisplayText());
       offeringResponse.setCpuNumber(offering.getCpu());
       offeringResponse.setCpuSpeed(offering.getSpeed());
       offeringResponse.setMemory(offering.getRamSize());
       offeringResponse.setCreated(offering.getCreated());
       offeringResponse.setStorageType(offering.getUseLocalStorage() ? "local" : "shared");
       offeringResponse.setOfferHa(offering.getOfferHA());
       offeringResponse.setUseVirtualNetwork(offering.getGuestIpType().equals(GuestIpType.Virtualized));
       offeringResponse.setTags(offering.getTags());
       
       return offeringResponse;
   }
   
   public static ConfigurationResponse createConfigurationResponse (ConfigurationVO cfg) {
       ConfigurationResponse cfgResponse = new ConfigurationResponse();
       cfgResponse.setCategory(cfg.getCategory());
       cfgResponse.setDescription(cfg.getDescription());
       cfgResponse.setName(cfg.getName());
       cfgResponse.setValue(cfg.getValue());
       
       return cfgResponse;
   }
   
   public static SnapshotResponse createSnapshotResponse (Snapshot snapshot) {
       SnapshotResponse snapshotResponse = new SnapshotResponse();
       snapshotResponse.setId(snapshot.getId());

       Account acct = ApiDBUtils.findAccountById(Long.valueOf(snapshot.getAccountId()));
       if (acct != null) {
           snapshotResponse.setAccountName(acct.getAccountName());
           snapshotResponse.setDomainId(acct.getDomainId());
           snapshotResponse.setDomainName(ApiDBUtils.findDomainById(acct.getDomainId()).getName());
       }

       VolumeVO volume = ApiDBUtils.findVolumeById(snapshot.getVolumeId());
       String snapshotTypeStr = SnapshotType.values()[snapshot.getSnapshotType()].name();
       snapshotResponse.setSnapshotType(snapshotTypeStr);
       snapshotResponse.setVolumeId(snapshot.getVolumeId());
       snapshotResponse.setVolumeName(volume.getName());
       snapshotResponse.setVolumeType(volume.getVolumeType().name());
       snapshotResponse.setCreated(snapshot.getCreated());
       snapshotResponse.setName(snapshot.getName());
       snapshotResponse.setIntervalType(ApiDBUtils.getSnapshotIntervalTypes(snapshot.getId()));
       AsyncJobVO asyncJob = ApiDBUtils.findInstancePendingAsyncJob("snapshot", snapshot.getId());
       if (asyncJob != null) {
           snapshotResponse.setJobId(asyncJob.getId());
           snapshotResponse.setJobStatus(asyncJob.getStatus());
       }
       return snapshotResponse;
   }
   

   public static SnapshotPolicyResponse createSnapshotPolicyResponse (SnapshotPolicyVO policy) {
       SnapshotPolicyResponse policyResponse = new SnapshotPolicyResponse();
       policyResponse.setId(policy.getId());
       policyResponse.setVolumeId(policy.getVolumeId());
       policyResponse.setSchedule(policy.getSchedule());
       policyResponse.setIntervalType(policy.getInterval());
       policyResponse.setMaxSnaps(policy.getMaxSnaps());
       policyResponse.setTimezone(policy.getTimezone());
       
       return policyResponse;
   }
   
   public static UserVmResponse createUserVmResponse (UserVm userVm) {
       UserVmResponse userVmResponse = new UserVmResponse();
       
       Account acct = ApiDBUtils.findAccountById(Long.valueOf(userVm.getAccountId()));
       //FIXME - this check should be done in searchForUserVm method in ManagementServerImpl; 
       //otherwise the number of vms returned is not going to match pageSize request parameter
       if ((acct != null) && (acct.getRemoved() == null)) {
           userVmResponse.setAccountName(acct.getAccountName());
           userVmResponse.setDomainId(acct.getDomainId());
           userVmResponse.setDomainName(ApiDBUtils.findDomainById(acct.getDomainId()).getName());
       } else {
           return null; // the account has been deleted, skip this VM in the response
       }

       userVmResponse.setId(userVm.getId());
       AsyncJobVO asyncJob = ApiDBUtils.findInstancePendingAsyncJob("vm_instance", userVm.getId());
       if (asyncJob != null) {
           userVmResponse.setJobId(asyncJob.getId());
           userVmResponse.setJobStatus(asyncJob.getStatus());
       } 

       userVmResponse.setName(userVm.getName());
       userVmResponse.setCreated(userVm.getCreated());
       userVmResponse.setIpAddress(userVm.getPrivateIpAddress());
       if (userVm.getState() != null) {
           userVmResponse.setState(userVm.getState().toString());
       }


       userVmResponse.setHaEnable(userVm.isHaEnabled());
       
       if (userVm.getDisplayName() != null) {
           userVmResponse.setDisplayName(userVm.getDisplayName());
       } else {
           userVmResponse.setDisplayName(userVm.getName());
       }

       InstanceGroupVO group = ApiDBUtils.findInstanceGroupForVM(userVm.getId());
       if (group != null) {
           userVmResponse.setGroup(group.getName());
           userVmResponse.setGroupId(group.getId());
       }

       // Data Center Info
       userVmResponse.setZoneId(userVm.getDataCenterId());
       userVmResponse.setZoneName(ApiDBUtils.findZoneById(userVm.getDataCenterId()).getName());

       Account account = (Account)UserContext.current().getAccount();
       //if user is an admin, display host id
       if (((account == null) || (account.getType() == Account.ACCOUNT_TYPE_ADMIN)) && (userVm.getHostId() != null)) {
           userVmResponse.setHostId(userVm.getHostId());
           userVmResponse.setHostName(ApiDBUtils.findHostById(userVm.getHostId()).getName());
       }
       
       // Template Info
       VMTemplateVO template = ApiDBUtils.findTemplateById(userVm.getTemplateId());
       if (template != null) {
           userVmResponse.setTemplateId(userVm.getTemplateId());
           userVmResponse.setTemplateName(template.getName());
           userVmResponse.setTemplateDisplayText(template.getDisplayText());
           userVmResponse.setPasswordEnabled(template.getEnablePassword());
       } else {
           userVmResponse.setTemplateId(-1L);
           userVmResponse.setTemplateName("ISO Boot");
           userVmResponse.setTemplateDisplayText("ISO Boot");
           userVmResponse.setPasswordEnabled(false);
       }

       // ISO Info
       if (userVm.getIsoId() != null) {
           VMTemplateVO iso = ApiDBUtils.findTemplateById(userVm.getIsoId().longValue());
           if (iso != null) {
               userVmResponse.setIsoId(userVm.getIsoId());
               userVmResponse.setIsoName(iso.getName());
           }
       }

       // Service Offering Info
       ServiceOffering offering = ApiDBUtils.findServiceOfferingById(userVm.getServiceOfferingId());
       userVmResponse.setServiceOfferingId(userVm.getServiceOfferingId());
       userVmResponse.setServiceOfferingName(offering.getName());
       userVmResponse.setCpuNumber(offering.getCpu());
       userVmResponse.setCpuSpeed(offering.getSpeed());
       userVmResponse.setMemory(offering.getRamSize());

       VolumeVO rootVolume = ApiDBUtils.findRootVolume(userVm.getId());
       if (rootVolume != null) {
           userVmResponse.setRootDeviceId(rootVolume.getDeviceId());
           StoragePoolVO storagePool = ApiDBUtils.findStoragePoolById(rootVolume.getPoolId());
           userVmResponse.setRootDeviceType(storagePool.getPoolType().toString());
       }

       //stats calculation
       DecimalFormat decimalFormat = new DecimalFormat("#.##");
       String cpuUsed = null;
       VmStats vmStats = ApiDBUtils.getVmStatistics(userVm.getId());
       if (vmStats != null) {
           float cpuUtil = (float) vmStats.getCPUUtilization();
           cpuUsed = decimalFormat.format(cpuUtil) + "%";
           userVmResponse.setCpuUsed(cpuUsed);

           long networkKbRead = (long)vmStats.getNetworkReadKBs();
           userVmResponse.setNetworkKbsRead(networkKbRead);
           
           long networkKbWrite = (long)vmStats.getNetworkWriteKBs();
           userVmResponse.setNetworkKbsWrite(networkKbWrite);
       }
       
       userVmResponse.setGuestOsId(userVm.getGuestOSId());
       //network groups
       userVmResponse.setNetworkGroupList(ApiDBUtils.getNetworkGroupsNamesForVm(userVm.getId()));
       
       return userVmResponse;

   }
}
