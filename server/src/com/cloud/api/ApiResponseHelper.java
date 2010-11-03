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
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.api.response.AccountResponse;
import com.cloud.api.response.ClusterResponse;
import com.cloud.api.response.ConfigurationResponse;
import com.cloud.api.response.DiskOfferingResponse;
import com.cloud.api.response.DomainResponse;
import com.cloud.api.response.DomainRouterResponse;
import com.cloud.api.response.FirewallRuleResponse;
import com.cloud.api.response.HostResponse;
import com.cloud.api.response.IPAddressResponse;
import com.cloud.api.response.InstanceGroupResponse;
import com.cloud.api.response.LoadBalancerResponse;
import com.cloud.api.response.PodResponse;
import com.cloud.api.response.PreallocatedLunResponse;
import com.cloud.api.response.ResourceLimitResponse;
import com.cloud.api.response.ServiceOfferingResponse;
import com.cloud.api.response.SnapshotPolicyResponse;
import com.cloud.api.response.SnapshotResponse;
import com.cloud.api.response.StoragePoolResponse;
import com.cloud.api.response.SystemVmResponse;
import com.cloud.api.response.UserResponse;
import com.cloud.api.response.UserVmResponse;
import com.cloud.api.response.VlanIpRangeResponse;
import com.cloud.api.response.VolumeResponse;
import com.cloud.api.response.ZoneResponse;
import com.cloud.async.AsyncJobVO;
import com.cloud.configuration.ConfigurationVO;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.domain.DomainVO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.Status.Event;
import com.cloud.network.FirewallRuleVO;
import com.cloud.network.IPAddressVO;
import com.cloud.network.LoadBalancerVO;
import com.cloud.offering.NetworkOffering.GuestIpType;
import com.cloud.offering.ServiceOffering;
import com.cloud.server.Criteria;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Snapshot.SnapshotType;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.StorageStats;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.preallocatedlun.PreallocatedLunVO;
import com.cloud.test.PodZoneConfig;
import com.cloud.user.Account;
import com.cloud.user.UserAccount;
import com.cloud.user.UserContext;
import com.cloud.user.UserStatisticsVO;
import com.cloud.uservm.UserVm;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DomainRouter;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.State;
import com.cloud.vm.SystemVm;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VmStats;

public class ApiResponseHelper {
    
    public static final Logger s_logger = Logger.getLogger(ApiResponseHelper.class.getName());
    
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
       diskOfferingResponse.setCustomized(offering.isCustomized());
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

       userVmResponse.setName(userVm.getHostName());
       userVmResponse.setCreated(userVm.getCreated());
       userVmResponse.setIpAddress(userVm.getPrivateIpAddress());
       if (userVm.getState() != null) {
           userVmResponse.setState(userVm.getState().toString());
       }


       userVmResponse.setHaEnable(userVm.isHaEnabled());
       
       if (userVm.getDisplayName() != null) {
           userVmResponse.setDisplayName(userVm.getDisplayName());
       } else {
           userVmResponse.setDisplayName(userVm.getHostName());
       }

       InstanceGroupVO group = ApiDBUtils.findInstanceGroupForVM(userVm.getId());
       if (group != null) {
           userVmResponse.setGroup(group.getName());
           userVmResponse.setGroupId(group.getId());
       }

       // Data Center Info
       userVmResponse.setZoneId(userVm.getDataCenterId());
       userVmResponse.setZoneName(ApiDBUtils.findZoneById(userVm.getDataCenterId()).getName());

       Account account = UserContext.current().getAccount();
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
   
   public static SystemVmResponse createSystemVmResponse (VMInstanceVO systemVM) {
       SystemVmResponse vmResponse = new SystemVmResponse();
       if (systemVM instanceof SystemVm) {
           SystemVm vm = (SystemVm)systemVM;

           vmResponse.setId(vm.getId());
           vmResponse.setSystemVmType(vm.getType().toString().toLowerCase());

           String instanceType = "console_proxy";
           if (systemVM instanceof SecondaryStorageVmVO) {
               instanceType = "sec_storage_vm"; // FIXME:  this should be a constant so that the async jobs get updated with the correct instance type, they are using
                                                //         different instance types at the moment
           }

           AsyncJobVO asyncJob = ApiDBUtils.findInstancePendingAsyncJob(instanceType, vm.getId());
           if (asyncJob != null) {
               vmResponse.setJobId(asyncJob.getId());
               vmResponse.setJobStatus(asyncJob.getStatus());
           } 

           vmResponse.setZoneId(vm.getDataCenterId());
           vmResponse.setZoneName(ApiDBUtils.findZoneById(vm.getDataCenterId()).getName());
           vmResponse.setDns1(vm.getDns1());
           vmResponse.setDns2(vm.getDns2());
           vmResponse.setNetworkDomain(vm.getDomain());
           vmResponse.setGateway(vm.getGateway());
           vmResponse.setName(vm.getHostName());
           vmResponse.setPodId(vm.getPodId());
           if (vm.getHostId() != null) {
               vmResponse.setHostId(vm.getHostId());
               vmResponse.setHostName(ApiDBUtils.findHostById(vm.getHostId()).getName());
           }
           vmResponse.setPrivateIp(vm.getPrivateIpAddress());
           vmResponse.setPrivateMacAddress(vm.getPrivateMacAddress());
           vmResponse.setPrivateNetmask(vm.getPrivateNetmask());
           vmResponse.setPublicIp(vm.getPublicIpAddress());
           vmResponse.setPublicMacAddress(vm.getPublicMacAddress());
           vmResponse.setPublicNetmask(vm.getPublicNetmask());
           vmResponse.setTemplateId(vm.getTemplateId());
           vmResponse.setCreated(vm.getCreated());
           if (vm.getState() != null) {
               vmResponse.setState(vm.getState().toString());
           }
       }

       // for console proxies, add the active sessions
       if (systemVM instanceof ConsoleProxyVO) {
           ConsoleProxyVO proxy = (ConsoleProxyVO)systemVM;
           vmResponse.setActiveViewerSessions(proxy.getActiveSession());
       }
       return vmResponse;
   }
   
   
   public static DomainRouterResponse createDomainRouterResponse (DomainRouter router) {
       DomainRouterResponse routerResponse = new DomainRouterResponse();
       routerResponse.setId(router.getId());

       AsyncJobVO asyncJob = ApiDBUtils.findInstancePendingAsyncJob("domain_router", router.getId());
       if (asyncJob != null) {
           routerResponse.setJobId(asyncJob.getId());
           routerResponse.setJobStatus(asyncJob.getStatus());
       } 

       routerResponse.setZoneId(router.getDataCenterId());
       routerResponse.setZoneName(ApiDBUtils.findZoneById(router.getDataCenterId()).getName());
       routerResponse.setDns1(router.getDns1());
       routerResponse.setDns2(router.getDns2());
       routerResponse.setNetworkDomain(router.getDomain());
       routerResponse.setGateway(router.getGateway());
       routerResponse.setName(router.getHostName());
       routerResponse.setPodId(router.getPodId());

       if (router.getHostId() != null) {
           routerResponse.setHostId(router.getHostId());
           routerResponse.setHostName(ApiDBUtils.findHostById(router.getHostId()).getName());
       } 

       routerResponse.setPrivateIp(router.getPrivateIpAddress());
       routerResponse.setPrivateMacAddress(router.getPrivateMacAddress());
       routerResponse.setPrivateNetmask(router.getPrivateNetmask());
       routerResponse.setPublicIp(router.getPublicIpAddress());
       routerResponse.setPublicMacAddress(router.getPublicMacAddress());
       routerResponse.setPublicNetmask(router.getPublicNetmask());
       routerResponse.setGuestIpAddress(router.getGuestIpAddress());
       routerResponse.setGuestMacAddress(router.getGuestMacAddress());
       routerResponse.setGuestNetmask(router.getGuestNetmask());
       routerResponse.setTemplateId(router.getTemplateId());
       routerResponse.setCreated(router.getCreated());
       routerResponse.setState(router.getState());

       Account accountTemp = ApiDBUtils.findAccountById(router.getAccountId());
       if (accountTemp != null) {
           routerResponse.setAccountName(accountTemp.getAccountName());
           routerResponse.setDomainId(accountTemp.getDomainId());
           routerResponse.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
       }
       
       return routerResponse;
   }
   
   public static HostResponse createHostResponse (HostVO host) {
       HostResponse hostResponse = new HostResponse();
       hostResponse.setId(host.getId());
       hostResponse.setCapabilities(host.getCapabilities());
       hostResponse.setClusterId(host.getClusterId());
       hostResponse.setCpuNumber(host.getCpus());
       hostResponse.setZoneId(host.getDataCenterId());
       hostResponse.setDisconnectedOn(host.getDisconnectedOn());
       hostResponse.setHypervisor(host.getHypervisorType());
       hostResponse.setHostType(host.getType());
       hostResponse.setLastPinged(new Date(host.getLastPinged()));
       hostResponse.setManagementServerId(host.getManagementServerId());
       hostResponse.setName(host.getName());
       hostResponse.setPodId(host.getPodId());
       hostResponse.setRemoved(host.getRemoved());
       hostResponse.setCpuSpeed(host.getSpeed());
       hostResponse.setState(host.getStatus());
       hostResponse.setIpAddress(host.getPrivateIpAddress());
       hostResponse.setVersion(host.getVersion());
       hostResponse.setCreated(host.getCreated());

       GuestOSCategoryVO guestOSCategory = ApiDBUtils.getHostGuestOSCategory(host.getId());
       if (guestOSCategory != null) {
           hostResponse.setOsCategoryId(guestOSCategory.getId());
           hostResponse.setOsCategoryName(guestOSCategory.getName());
       }
       hostResponse.setZoneName(ApiDBUtils.findZoneById(host.getDataCenterId()).getName());

       if (host.getPodId() != null) {
           hostResponse.setPodName(ApiDBUtils.findPodById(host.getPodId()).getName());
       }

       DecimalFormat decimalFormat = new DecimalFormat("#.##");

       // calculate cpu allocated by vm
       if ((host.getCpus() != null) && (host.getSpeed() != null)) {
           int cpu = 0;
           String cpuAlloc = null;
           List<UserVmVO> instances = ApiDBUtils.listUserVMsByHostId(host.getId());
           for (UserVmVO vm : instances) {
               ServiceOffering so = ApiDBUtils.findServiceOfferingById(vm.getServiceOfferingId());
               cpu += so.getCpu() * so.getSpeed();
           }
           cpuAlloc = decimalFormat.format(((float) cpu / (float) (host.getCpus() * host.getSpeed())) * 100f) + "%";
           hostResponse.setCpuAllocated(cpuAlloc);
       }

       // calculate cpu utilized
       String cpuUsed = null;
       HostStats hostStats = ApiDBUtils.getHostStatistics(host.getId());
       if (hostStats != null) {
           float cpuUtil = (float) hostStats.getCpuUtilization();
           cpuUsed = decimalFormat.format(cpuUtil) + "%";
           hostResponse.setCpuUsed(cpuUsed);
           hostResponse.setAverageLoad((long)hostStats.getAverageLoad());
           hostResponse.setNetworkKbsRead((long)hostStats.getNetworkReadKBs());
           hostResponse.setNetworkKbsWrite((long)hostStats.getNetworkWriteKBs());
       }

       if (host.getType() == Host.Type.Routing) {
           hostResponse.setMemoryTotal(host.getTotalMemory());
           
           // calculate memory allocated by systemVM and userVm
           long mem = ApiDBUtils.getMemoryUsagebyHost(host.getId());
           hostResponse.setMemoryAllocated(mem);
           hostResponse.setMemoryUsed(mem);
       } else if (host.getType().toString().equals("Storage")) {
           hostResponse.setDiskSizeTotal(host.getTotalSize());
           hostResponse.setDiskSizeAllocated(0L);
       }

       if (host.getClusterId() != null) {
           ClusterVO cluster = ApiDBUtils.findClusterById(host.getClusterId());
           hostResponse.setClusterName(cluster.getName());
       }

       hostResponse.setLocalStorageActive(ApiDBUtils.isLocalStorageActiveOnHost(host));

       Set<Event> possibleEvents = host.getStatus().getPossibleEvents();
       if ((possibleEvents != null) && !possibleEvents.isEmpty()) {
           String events = "";
           Iterator<Event> iter = possibleEvents.iterator();
           while (iter.hasNext()) {
               Event event = iter.next();
               events += event.toString();
               if (iter.hasNext()) {
                   events += "; ";
               }
           }
           hostResponse.setEvents(events);
       }
       
       return hostResponse;
   }
   
   public static VlanIpRangeResponse createVlanIpRangeResponse (VlanVO vlan) {
       Long accountId = ApiDBUtils.getAccountIdForVlan(vlan.getId());
       Long podId = ApiDBUtils.getPodIdForVlan(vlan.getId());

       VlanIpRangeResponse vlanResponse = new VlanIpRangeResponse();
       vlanResponse.setId(vlan.getId());
       vlanResponse.setForVirtualNetwork(vlan.getVlanType().equals(VlanType.VirtualNetwork));
       vlanResponse.setVlan(vlan.getVlanId());
       vlanResponse.setZoneId(vlan.getDataCenterId());
       
       if (accountId != null) {
           Account account = ApiDBUtils.findAccountById(accountId);
           vlanResponse.setAccountName(account.getAccountName());
           vlanResponse.setDomainId(account.getDomainId());
           vlanResponse.setDomainName(ApiDBUtils.findDomainById(account.getDomainId()).getName());
       }

       if (podId != null) {
           HostPodVO pod = ApiDBUtils.findPodById(podId);
           vlanResponse.setPodId(podId);
           vlanResponse.setPodName(pod.getName());
       }

       vlanResponse.setGateway(vlan.getVlanGateway());
       vlanResponse.setNetmask(vlan.getVlanNetmask());
       vlanResponse.setDescription(vlan.getIpRange());
       
       return vlanResponse;
   }
   
   public static IPAddressResponse createIPAddressResponse (IPAddressVO ipAddress) {
       VlanVO vlan  = ApiDBUtils.findVlanById(ipAddress.getVlanDbId());
       boolean forVirtualNetworks = vlan.getVlanType().equals(VlanType.VirtualNetwork);

       IPAddressResponse ipResponse = new IPAddressResponse();
       ipResponse.setIpAddress(ipAddress.getAddress());
       if (ipAddress.getAllocated() != null) {
           ipResponse.setAllocated(ipAddress.getAllocated());
       }
       ipResponse.setZoneId(ipAddress.getDataCenterId());
       ipResponse.setZoneName(ApiDBUtils.findZoneById(ipAddress.getDataCenterId()).getName());
       ipResponse.setSourceNat(ipAddress.isSourceNat());

       //get account information
       Account accountTemp = ApiDBUtils.findAccountById(ipAddress.getAccountId());
       if (accountTemp !=null){
           ipResponse.setAccountName(accountTemp.getAccountName());
           ipResponse.setDomainId(accountTemp.getDomainId());
           ipResponse.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
       } 
       
       ipResponse.setForVirtualNetwork(forVirtualNetworks);

       //show this info to admin only
       Account account = UserContext.current().getAccount();
       if ((account == null)  || account.getType() == Account.ACCOUNT_TYPE_ADMIN) {
           ipResponse.setVlanId(ipAddress.getVlanDbId());
           ipResponse.setVlanName(ApiDBUtils.findVlanById(ipAddress.getVlanDbId()).getVlanId());
       }
       
       return ipResponse;
   }
   
   public static LoadBalancerResponse createLoadBalancerResponse (LoadBalancerVO loadBalancer) {
       LoadBalancerResponse lbResponse = new LoadBalancerResponse();
       lbResponse.setId(loadBalancer.getId());
       lbResponse.setName(loadBalancer.getName());
       lbResponse.setDescription(loadBalancer.getDescription());
       lbResponse.setPublicIp(loadBalancer.getIpAddress());
       lbResponse.setPublicPort(loadBalancer.getPublicPort());
       lbResponse.setPrivatePort(loadBalancer.getPrivatePort());
       lbResponse.setAlgorithm(loadBalancer.getAlgorithm());

       Account accountTemp = ApiDBUtils.findAccountById(loadBalancer.getAccountId());
       if (accountTemp != null) {
           lbResponse.setAccountName(accountTemp.getAccountName());
           lbResponse.setDomainId(accountTemp.getDomainId());
           lbResponse.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
       }
       
       return lbResponse;
   }
   
   public static PodResponse createPodResponse (HostPodVO pod) {
       String[] ipRange = new String[2];
       if (pod.getDescription() != null && pod.getDescription().length() > 0) {
           ipRange = pod.getDescription().split("-");
       } else {
           ipRange[0] = pod.getDescription();
       }

       PodResponse podResponse = new PodResponse();
       podResponse.setId(pod.getId());
       podResponse.setName(pod.getName());
       podResponse.setZoneId(pod.getDataCenterId());
       podResponse.setZoneName(PodZoneConfig.getZoneName(pod.getDataCenterId()));
       podResponse.setCidr(pod.getCidrAddress() +"/" + pod.getCidrSize());
       podResponse.setStartIp(ipRange[0]);
       podResponse.setEndIp(((ipRange.length > 1) && (ipRange[1] != null)) ? ipRange[1] : "");
       podResponse.setGateway(pod.getGateway());
       
       return podResponse;
   }
   
   public static ZoneResponse createZoneResponse (DataCenterVO dataCenter) {
       Account account = UserContext.current().getAccount();
       ZoneResponse zoneResponse = new ZoneResponse();
       zoneResponse.setId(dataCenter.getId());
       zoneResponse.setName(dataCenter.getName());

       if ((dataCenter.getDescription() != null) && !dataCenter.getDescription().equalsIgnoreCase("null")) {
           zoneResponse.setDescription(dataCenter.getDescription());
       }

       if ((account == null) || (account.getType() == Account.ACCOUNT_TYPE_ADMIN)) {
           zoneResponse.setDns1(dataCenter.getDns1());
           zoneResponse.setDns2(dataCenter.getDns2());
           zoneResponse.setInternalDns1(dataCenter.getInternalDns1());
           zoneResponse.setInternalDns2(dataCenter.getInternalDns2());
           zoneResponse.setVlan(dataCenter.getVnet());
           zoneResponse.setGuestCidrAddress(dataCenter.getGuestNetworkCidr());
       }
       
       zoneResponse.setDomain(dataCenter.getDomain());
       zoneResponse.setDomainId(dataCenter.getDomainId());
       
       return zoneResponse;
   }
   
   public static VolumeResponse createVolumeResponse(VolumeVO volume) {
       VolumeResponse volResponse = new VolumeResponse();
       volResponse.setId(volume.getId());

       AsyncJobVO asyncJob = ApiDBUtils.findInstancePendingAsyncJob("volume", volume.getId());
       if (asyncJob != null) {
           volResponse.setJobId(asyncJob.getId());
           volResponse.setJobStatus(asyncJob.getStatus());
       } 

       if (volume.getName() != null) {
           volResponse.setName(volume.getName());
       } else {
           volResponse.setName("");
       }
       
       volResponse.setZoneId(volume.getDataCenterId());
       volResponse.setZoneName(ApiDBUtils.findZoneById(volume.getDataCenterId()).getName());

       volResponse.setVolumeType(volume.getVolumeType().toString());
       volResponse.setDeviceId(volume.getDeviceId());
       
       Long instanceId = volume.getInstanceId();
       if (instanceId != null) {
           VMInstanceVO vm = ApiDBUtils.findVMInstanceById(instanceId);
           volResponse.setVirtualMachineId(vm.getId());
           volResponse.setVirtualMachineName(vm.getHostName());
           volResponse.setVirtualMachineDisplayName(vm.getHostName());
           volResponse.setVirtualMachineState(vm.getState().toString());
       }             

       // Show the virtual size of the volume
       volResponse.setSize(volume.getSize());

       volResponse.setCreated(volume.getCreated());
       volResponse.setState(volume.getStatus().toString());
       
       Account accountTemp = ApiDBUtils.findAccountById(volume.getAccountId());
       if (accountTemp != null) {
           volResponse.setAccountName(accountTemp.getAccountName());
           volResponse.setDomainId(accountTemp.getDomainId());
           volResponse.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
       }

       String storageType;
       try {
           if(volume.getPoolId() == null){
               if (volume.getState() == Volume.State.Allocated) {
                   /*set it as shared, so the UI can attach it to VM*/
                   storageType = "shared";
               } else {
                   storageType = "unknown";
               }
           } else {
               storageType = ApiDBUtils.volumeIsOnSharedStorage(volume.getId()) ? "shared" : "local";
           }
       } catch (InvalidParameterValueException e) {
           s_logger.error(e.getMessage(), e);
           throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Volume " + volume.getName() + " does not have a valid ID");
       }

       volResponse.setStorageType(storageType);            
       volResponse.setDiskOfferingId(volume.getDiskOfferingId());

       DiskOfferingVO diskOffering = ApiDBUtils.findDiskOfferingById(volume.getDiskOfferingId());
       volResponse.setDiskOfferingName(diskOffering.getName());
       volResponse.setDiskOfferingDisplayText(diskOffering.getDisplayText());

       Long poolId = volume.getPoolId();
       String poolName = (poolId == null) ? "none" : ApiDBUtils.findStoragePoolById(poolId).getName();
       volResponse.setStoragePoolName(poolName);
       volResponse.setSourceId(volume.getSourceId());
       if (volume.getSourceType() != null) {
           volResponse.setSourceType(volume.getSourceType().toString());
       }
       volResponse.setHypervisor(ApiDBUtils.getVolumeHyperType(volume.getId()).toString());
       volResponse.setAttached(volume.getAttached());
       
       return volResponse;
   }
   
   
   public static InstanceGroupResponse createInstanceGroupResponse(InstanceGroupVO group) {
       InstanceGroupResponse groupResponse = new InstanceGroupResponse();
       groupResponse.setId(group.getId());
       groupResponse.setName(group.getName());
       groupResponse.setCreated(group.getCreated());

       Account accountTemp = ApiDBUtils.findAccountById(group.getAccountId());
       if (accountTemp != null) {
           groupResponse.setAccountName(accountTemp.getAccountName());
           groupResponse.setDomainId(accountTemp.getDomainId());
           groupResponse.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
       }
       
       return groupResponse;
   }
   
   public static PreallocatedLunResponse createPreallocatedLunResponse(PreallocatedLunVO preallocatedLun) {
       PreallocatedLunResponse preallocLunResponse = new PreallocatedLunResponse();
       preallocLunResponse.setId(preallocatedLun.getId());
       preallocLunResponse.setVolumeId(preallocatedLun.getVolumeId());
       preallocLunResponse.setZoneId(preallocatedLun.getDataCenterId());
       preallocLunResponse.setLun(preallocatedLun.getLun());
       preallocLunResponse.setPortal(preallocatedLun.getPortal());
       preallocLunResponse.setSize(preallocatedLun.getSize());
       preallocLunResponse.setTaken(preallocatedLun.getTaken());
       preallocLunResponse.setTargetIqn(preallocatedLun.getTargetIqn());
       
       return preallocLunResponse;
   }
   
   public static StoragePoolResponse createStoragePoolResponse(StoragePoolVO pool) {
       StoragePoolResponse poolResponse = new StoragePoolResponse();
       poolResponse.setId(pool.getId());
       poolResponse.setName(pool.getName());
       poolResponse.setState(pool.getStatus());
       poolResponse.setPath(pool.getPath());
       poolResponse.setIpAddress(pool.getHostAddress());
       poolResponse.setZoneId(pool.getDataCenterId());
       poolResponse.setZoneName(ApiDBUtils.findZoneById(pool.getDataCenterId()).getName());
       if (pool.getPoolType() != null) {
           poolResponse.setType(pool.getPoolType().toString());
       }
       if (pool.getPodId() != null) {
           poolResponse.setPodId(pool.getPodId());
           poolResponse.setPodName(ApiDBUtils.findPodById(pool.getPodId()).getName());
       }
       if (pool.getCreated() != null) {
           poolResponse.setCreated(pool.getCreated());
       }

       StorageStats stats = ApiDBUtils.getStoragePoolStatistics(pool.getId());
       long capacity = pool.getCapacityBytes();
       long available = pool.getAvailableBytes() ;
       long used = capacity - available;

       if (stats != null) {
           used = stats.getByteUsed();
           available = capacity - used;
       }

       poolResponse.setDiskSizeTotal(pool.getCapacityBytes());
       poolResponse.setDiskSizeAllocated(used);

       if (pool.getClusterId() != null) {
           ClusterVO cluster = ApiDBUtils.findClusterById(pool.getClusterId());
           poolResponse.setClusterId(cluster.getId());
           poolResponse.setClusterName(cluster.getName());
       }           
       poolResponse.setTags(ApiDBUtils.getStoragePoolTags(pool.getId()));
       
       return poolResponse;
   }
   
   
   public static ClusterResponse createClusterResponse(ClusterVO cluster) {
       ClusterResponse clusterResponse = new ClusterResponse();
       clusterResponse.setId(cluster.getId());
       clusterResponse.setName(cluster.getName());
       clusterResponse.setPodId(cluster.getPodId());
       clusterResponse.setZoneId(cluster.getDataCenterId());
       HostPodVO pod = ApiDBUtils.findPodById(cluster.getPodId());
       clusterResponse.setPodName(pod.getName());
       DataCenterVO zone = ApiDBUtils.findZoneById(cluster.getDataCenterId());
       clusterResponse.setZoneName(zone.getName());
       
       return clusterResponse;
   }
   
   public static FirewallRuleResponse createFirewallRuleResponse(FirewallRuleVO fwRule) {
       FirewallRuleResponse response = new FirewallRuleResponse();
       response.setId(fwRule.getId());
       response.setPrivatePort(fwRule.getPrivatePort());
       response.setProtocol(fwRule.getProtocol());
       response.setPublicPort(fwRule.getPublicPort());
       if (fwRule.getPublicIpAddress() != null && fwRule.getPrivateIpAddress() != null) {
           UserVm vm = ApiDBUtils.findUserVmByPublicIpAndGuestIp(fwRule.getPublicIpAddress(), fwRule.getPrivateIpAddress());
           response.setVirtualMachineId(vm.getId());
           response.setVirtualMachineName(vm.getHostName());
       }

       return response;
   }
   
}
