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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.cloud.api.commands.QueryAsyncJobResultCmd;
import com.cloud.api.response.AccountResponse;
import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.api.response.AsyncJobResponse;
import com.cloud.api.response.CapabilityResponse;
import com.cloud.api.response.CapacityResponse;
import com.cloud.api.response.ClusterResponse;
import com.cloud.api.response.ConfigurationResponse;
import com.cloud.api.response.CreateCmdResponse;
import com.cloud.api.response.DiskOfferingResponse;
import com.cloud.api.response.DomainResponse;
import com.cloud.api.response.DomainRouterResponse;
import com.cloud.api.response.EventResponse;
import com.cloud.api.response.ExtractResponse;
import com.cloud.api.response.FirewallResponse;
import com.cloud.api.response.FirewallRuleResponse;
import com.cloud.api.response.HostResponse;
import com.cloud.api.response.IPAddressResponse;
import com.cloud.api.response.IngressRuleResponse;
import com.cloud.api.response.IngressRuleResultObject;
import com.cloud.api.response.InstanceGroupResponse;
import com.cloud.api.response.IpForwardingRuleResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.LoadBalancerResponse;
import com.cloud.api.response.NetworkOfferingResponse;
import com.cloud.api.response.NetworkResponse;
import com.cloud.api.response.NicResponse;
import com.cloud.api.response.PodResponse;
import com.cloud.api.response.RemoteAccessVpnResponse;
import com.cloud.api.response.ResourceCountResponse;
import com.cloud.api.response.ResourceLimitResponse;
import com.cloud.api.response.SecurityGroupResponse;
import com.cloud.api.response.SecurityGroupResultObject;
import com.cloud.api.response.ServiceOfferingResponse;
import com.cloud.api.response.ServiceResponse;
import com.cloud.api.response.SnapshotPolicyResponse;
import com.cloud.api.response.SnapshotResponse;
import com.cloud.api.response.StoragePoolResponse;
import com.cloud.api.response.SystemVmInstanceResponse;
import com.cloud.api.response.SystemVmResponse;
import com.cloud.api.response.TemplatePermissionsResponse;
import com.cloud.api.response.TemplateResponse;
import com.cloud.api.response.UserResponse;
import com.cloud.api.response.UserVmResponse;
import com.cloud.api.response.VlanIpRangeResponse;
import com.cloud.api.response.VolumeResponse;
import com.cloud.api.response.VpnUsersResponse;
import com.cloud.api.response.ZoneResponse;
import com.cloud.async.AsyncJob;
import com.cloud.async.AsyncJobResult;
import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityVO;
import com.cloud.configuration.Configuration;
import com.cloud.configuration.ResourceCount;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.ResourceLimit;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.Vlan;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.domain.Domain;
import com.cloud.event.Event;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.network.IPAddressVO;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.VpnUser;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.StaticNatRule;
import com.cloud.network.security.IngressRule;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupRules;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.server.Criteria;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOS;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.StorageStats;
import com.cloud.storage.UploadVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.snapshot.SnapshotPolicy;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.test.PodZoneConfig;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.UserContext;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.UserVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.StringUtils;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.InstanceGroup;
import com.cloud.vm.NicProfile;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.VmStats;
import com.cloud.vm.dao.UserVmData;
import com.cloud.vm.dao.UserVmData.NicData;
import com.cloud.vm.dao.UserVmData.SecurityGroupData;

public class ApiResponseHelper implements ResponseGenerator {

    public final Logger s_logger = Logger.getLogger(ApiResponseHelper.class);

    @Override
    public UserResponse createUserResponse(UserAccount user) {
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
        userResponse.setObjectName("user");

        return userResponse;
    }

    @Override
    public UserResponse createUserResponse(User user) {
        UserResponse userResponse = new UserResponse();
        Account account = ApiDBUtils.findAccountById(user.getAccountId());
        userResponse.setAccountName(account.getAccountName());
        userResponse.setAccountType(account.getType());
        userResponse.setCreated(user.getCreated());
        userResponse.setDomainId(account.getDomainId());
        userResponse.setDomainName(ApiDBUtils.findDomainById(account.getDomainId()).getName());
        userResponse.setEmail(user.getEmail());
        userResponse.setFirstname(user.getFirstname());
        userResponse.setId(user.getId());
        userResponse.setLastname(user.getLastname());
        userResponse.setState(user.getState().toString());
        userResponse.setTimezone(user.getTimezone());
        userResponse.setUsername(user.getUsername());
        userResponse.setApiKey(user.getApiKey());
        userResponse.setSecretKey(user.getSecretKey());
        userResponse.setObjectName("user");

        return userResponse;
    }

    // this method is used for response generation via createAccount (which creates an account + user)
    @Override
    public AccountResponse createUserAccountResponse(UserAccount user) {
        return createAccountResponse(ApiDBUtils.findAccountById(user.getAccountId()));
    }

    @Override
    public AccountResponse createAccountResponse(Account account) {
        boolean accountIsAdmin = (account.getType() == Account.ACCOUNT_TYPE_ADMIN);
        AccountResponse accountResponse = new AccountResponse();
        accountResponse.setId(account.getId());
        accountResponse.setName(account.getAccountName());
        accountResponse.setAccountType(account.getType());
        accountResponse.setDomainId(account.getDomainId());
        accountResponse.setDomainName(ApiDBUtils.findDomainById(account.getDomainId()).getName());
        accountResponse.setState(account.getState().toString());
        accountResponse.setNetworkDomain(account.getNetworkDomain());

        // get network stat
        List<UserStatisticsVO> stats = ApiDBUtils.listUserStatsBy(account.getId());
        if (stats == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Internal error searching for user stats");
        }

        Long bytesSent = 0L;
        Long bytesReceived = 0L;
        for (UserStatisticsVO stat : stats) {
            Long rx = stat.getNetBytesReceived() + stat.getCurrentBytesReceived();
            Long tx = stat.getNetBytesSent() + stat.getCurrentBytesSent();
            bytesReceived = bytesReceived + Long.valueOf(rx);
            bytesSent = bytesSent + Long.valueOf(tx);
        }
        accountResponse.setBytesReceived(bytesReceived);
        accountResponse.setBytesSent(bytesSent);

        // Get resource limits and counts

        Long vmLimit = ApiDBUtils.findCorrectResourceLimit(ResourceType.user_vm, account.getId());
        String vmLimitDisplay = (accountIsAdmin || vmLimit == -1) ? "Unlimited" : String.valueOf(vmLimit);
        Long vmTotal = ApiDBUtils.getResourceCount(ResourceType.user_vm, account.getId());
        String vmAvail = (accountIsAdmin || vmLimit == -1) ? "Unlimited" : String.valueOf(vmLimit - vmTotal);
        accountResponse.setVmLimit(vmLimitDisplay);
        accountResponse.setVmTotal(vmTotal);
        accountResponse.setVmAvailable(vmAvail);

        Long ipLimit = ApiDBUtils.findCorrectResourceLimit(ResourceType.public_ip, account.getId());
        String ipLimitDisplay = (accountIsAdmin || ipLimit == -1) ? "Unlimited" : String.valueOf(ipLimit);
        Long ipTotal = ApiDBUtils.getResourceCount(ResourceType.public_ip, account.getId());
        String ipAvail = (accountIsAdmin || ipLimit == -1) ? "Unlimited" : String.valueOf(ipLimit - ipTotal);
        accountResponse.setIpLimit(ipLimitDisplay);
        accountResponse.setIpTotal(ipTotal);
        accountResponse.setIpAvailable(ipAvail);

        Long volumeLimit = ApiDBUtils.findCorrectResourceLimit(ResourceType.volume, account.getId());
        String volumeLimitDisplay = (accountIsAdmin || volumeLimit == -1) ? "Unlimited" : String.valueOf(volumeLimit);
        Long volumeTotal = ApiDBUtils.getResourceCount(ResourceType.volume, account.getId());
        String volumeAvail = (accountIsAdmin || volumeLimit == -1) ? "Unlimited" : String.valueOf(volumeLimit - volumeTotal);
        accountResponse.setVolumeLimit(volumeLimitDisplay);
        accountResponse.setVolumeTotal(volumeTotal);
        accountResponse.setVolumeAvailable(volumeAvail);

        Long snapshotLimit = ApiDBUtils.findCorrectResourceLimit(ResourceType.snapshot, account.getId());
        String snapshotLimitDisplay = (accountIsAdmin || snapshotLimit == -1) ? "Unlimited" : String.valueOf(snapshotLimit);
        Long snapshotTotal = ApiDBUtils.getResourceCount(ResourceType.snapshot, account.getId());
        String snapshotAvail = (accountIsAdmin || snapshotLimit == -1) ? "Unlimited" : String.valueOf(snapshotLimit - snapshotTotal);
        accountResponse.setSnapshotLimit(snapshotLimitDisplay);
        accountResponse.setSnapshotTotal(snapshotTotal);
        accountResponse.setSnapshotAvailable(snapshotAvail);

        Long templateLimit = ApiDBUtils.findCorrectResourceLimit(ResourceType.template, account.getId());
        String templateLimitDisplay = (accountIsAdmin || templateLimit == -1) ? "Unlimited" : String.valueOf(templateLimit);
        Long templateTotal = ApiDBUtils.getResourceCount(ResourceType.template, account.getId());
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

        // get Running/Stopped VMs
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
        accountResponse.setObjectName("account");

        // adding all the users for an account as part of the response obj
        List<UserVO> usersForAccount = ApiDBUtils.listUsersByAccount(account.getAccountId());
        List<UserResponse> userResponseList = new ArrayList<UserResponse>();
        for (UserVO user : usersForAccount) {
            UserResponse userResponse = new UserResponse();
            userResponse.setAccountName(account.getAccountName());
            userResponse.setAccountType(account.getType());
            userResponse.setApiKey(user.getApiKey());
            userResponse.setCreated(user.getCreated());
            userResponse.setDomainId(account.getDomainId());
            userResponse.setDomainName(ApiDBUtils.findDomainById(account.getDomainId()).getName());
            userResponse.setEmail(user.getEmail());
            userResponse.setFirstname(user.getFirstname());
            userResponse.setId(user.getId());
            userResponse.setSecretKey(user.getSecretKey());
            userResponse.setLastname(user.getLastname());
            userResponse.setState(user.getState().toString());
            userResponse.setTimezone(user.getTimezone());
            userResponse.setUsername(user.getUsername());

            userResponseList.add(userResponse);
        }

        accountResponse.setUsers(userResponseList);
        return accountResponse;
    }

    @Override
    public DomainResponse createDomainResponse(Domain domain) {
        DomainResponse domainResponse = new DomainResponse();
        domainResponse.setDomainName(domain.getName());
        domainResponse.setId(domain.getId());
        domainResponse.setLevel(domain.getLevel());
        domainResponse.setNetworkDomain(domain.getNetworkDomain());
        domainResponse.setParentDomainId(domain.getParent());
        if (domain.getParent() != null) {
            domainResponse.setParentDomainName(ApiDBUtils.findDomainById(domain.getParent()).getName());
        }
        if (domain.getChildCount() > 0) {
            domainResponse.setHasChild(true);
        }
        domainResponse.setObjectName("domain");
        return domainResponse;
    }

    @Override
    public DiskOfferingResponse createDiskOfferingResponse(DiskOffering offering) {
        DiskOfferingResponse diskOfferingResponse = new DiskOfferingResponse();
        diskOfferingResponse.setId(offering.getId());
        diskOfferingResponse.setName(offering.getName());
        diskOfferingResponse.setDisplayText(offering.getDisplayText());
        diskOfferingResponse.setCreated(offering.getCreated());
        diskOfferingResponse.setDiskSize(offering.getDiskSize() / (1024 * 1024 * 1024));
        if (offering.getDomainId() != null) {
            diskOfferingResponse.setDomain(ApiDBUtils.findDomainById(offering.getDomainId()).getName());
            diskOfferingResponse.setDomainId(offering.getDomainId());
        }
        diskOfferingResponse.setTags(offering.getTags());
        diskOfferingResponse.setCustomized(offering.isCustomized());
        diskOfferingResponse.setObjectName("diskoffering");
        return diskOfferingResponse;
    }

    @Override
    public ResourceLimitResponse createResourceLimitResponse(ResourceLimit limit) {
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
        resourceLimitResponse.setObjectName("resourcelimit");

        return resourceLimitResponse;
    }

    @Override
    public ResourceCountResponse createResourceCountResponse(ResourceCount resourceCount) {
        ResourceCountResponse resourceCountResponse = new ResourceCountResponse();

        if (resourceCount.getAccountId() != null) {
            Account accountTemp = ApiDBUtils.findAccountById(resourceCount.getAccountId());
            if (accountTemp != null) {
                resourceCountResponse.setAccountName(accountTemp.getAccountName());
                resourceCountResponse.setDomainId(accountTemp.getDomainId());
                resourceCountResponse.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
            }
        } else if (resourceCount.getDomainId() != null) {
            resourceCountResponse.setDomainId(resourceCount.getDomainId());
            resourceCountResponse.setDomainName(ApiDBUtils.findDomainById(resourceCount.getDomainId()).getName());
        }

        resourceCountResponse.setResourceType(Integer.valueOf(resourceCount.getType().ordinal()).toString());
        resourceCountResponse.setResourceCount(resourceCount.getCount());
        resourceCountResponse.setObjectName("resourcecount");
        return resourceCountResponse;
    }

    @Override
    public ServiceOfferingResponse createServiceOfferingResponse(ServiceOffering offering) {
        ServiceOfferingResponse offeringResponse = new ServiceOfferingResponse();
        offeringResponse.setId(offering.getId());
        offeringResponse.setName(offering.getName());
        offeringResponse.setIsSystemOffering(offering.getSystemUse());
        offeringResponse.setDefaultUse(offering.getDefaultUse());
        offeringResponse.setSystemVmType(offering.getSystemVmType());
        offeringResponse.setDisplayText(offering.getDisplayText());
        offeringResponse.setCpuNumber(offering.getCpu());
        offeringResponse.setCpuSpeed(offering.getSpeed());
        offeringResponse.setMemory(offering.getRamSize());
        offeringResponse.setCreated(offering.getCreated());
        offeringResponse.setStorageType(offering.getUseLocalStorage() ? ServiceOffering.StorageType.local.toString() : ServiceOffering.StorageType.shared.toString());
        offeringResponse.setOfferHa(offering.getOfferHA());
        offeringResponse.setLimitCpuUse(offering.getLimitCpuUse());
        offeringResponse.setTags(offering.getTags());
        if (offering.getDomainId() != null) {
            offeringResponse.setDomain(ApiDBUtils.findDomainById(offering.getDomainId()).getName());
            offeringResponse.setDomainId(offering.getDomainId());
        }
        offeringResponse.setNetworkRate(offering.getRateMbps());
        offeringResponse.setHostTag(offering.getHostTag());
        offeringResponse.setObjectName("serviceoffering");

        return offeringResponse;
    }

    @Override
    public ConfigurationResponse createConfigurationResponse(Configuration cfg) {
        ConfigurationResponse cfgResponse = new ConfigurationResponse();
        cfgResponse.setCategory(cfg.getCategory());
        cfgResponse.setDescription(cfg.getDescription());
        cfgResponse.setName(cfg.getName());
        cfgResponse.setValue(cfg.getValue());
        cfgResponse.setObjectName("configuration");

        return cfgResponse;
    }

    @Override
    public SnapshotResponse createSnapshotResponse(Snapshot snapshot) {
        SnapshotResponse snapshotResponse = new SnapshotResponse();
        snapshotResponse.setId(snapshot.getId());

        Account acct = ApiDBUtils.findAccountById(Long.valueOf(snapshot.getAccountId()));
        if (acct != null) {
            snapshotResponse.setAccountName(acct.getAccountName());
            snapshotResponse.setDomainId(acct.getDomainId());
            snapshotResponse.setDomainName(ApiDBUtils.findDomainById(acct.getDomainId()).getName());
        }

        VolumeVO volume = findVolumeById(snapshot.getVolumeId());
        String snapshotTypeStr = snapshot.getType().name();
        snapshotResponse.setSnapshotType(snapshotTypeStr);
        snapshotResponse.setVolumeId(snapshot.getVolumeId());
        if (volume != null) {
            snapshotResponse.setVolumeName(volume.getName());
            snapshotResponse.setVolumeType(volume.getVolumeType().name());
        }
        snapshotResponse.setCreated(snapshot.getCreated());
        snapshotResponse.setName(snapshot.getName());
        snapshotResponse.setIntervalType(ApiDBUtils.getSnapshotIntervalTypes(snapshot.getId()));
        snapshotResponse.setState(snapshot.getStatus());
        snapshotResponse.setObjectName("snapshot");
        return snapshotResponse;
    }

    @Override
    public SnapshotPolicyResponse createSnapshotPolicyResponse(SnapshotPolicy policy) {
        SnapshotPolicyResponse policyResponse = new SnapshotPolicyResponse();
        policyResponse.setId(policy.getId());
        policyResponse.setVolumeId(policy.getVolumeId());
        policyResponse.setSchedule(policy.getSchedule());
        policyResponse.setIntervalType(policy.getInterval());
        policyResponse.setMaxSnaps(policy.getMaxSnaps());
        policyResponse.setTimezone(policy.getTimezone());
        policyResponse.setObjectName("snapshotpolicy");

        return policyResponse;
    }

    @Override
    public HostResponse createHostResponse(Host host) {
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
            HostPodVO pod = ApiDBUtils.findPodById(host.getPodId());
            if (pod != null) {
                hostResponse.setPodName(pod.getName());
            }
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

            String cpuWithOverprovisioning = new Float(host.getCpus() * host.getSpeed() * ApiDBUtils.getCpuOverprovisioningFactor()).toString();
            hostResponse.setCpuWithOverprovisioning(cpuWithOverprovisioning);
        }

        // calculate cpu utilized
        String cpuUsed = null;
        HostStats hostStats = ApiDBUtils.getHostStatistics(host.getId());
        if (hostStats != null) {
            float cpuUtil = (float) hostStats.getCpuUtilization();
            cpuUsed = decimalFormat.format(cpuUtil) + "%";
            hostResponse.setCpuUsed(cpuUsed);
            hostResponse.setNetworkKbsRead((new Double(hostStats.getNetworkReadKBs())).longValue());
            hostResponse.setNetworkKbsWrite((new Double(hostStats.getNetworkWriteKBs())).longValue());
        }

        if (host.getType() == Host.Type.Routing) {
            hostResponse.setMemoryTotal(host.getTotalMemory());

            // calculate memory allocated by systemVM and userVm
            Long mem = ApiDBUtils.getMemoryUsagebyHost(host.getId());
            hostResponse.setMemoryAllocated(mem);
            hostResponse.setMemoryUsed(mem);
            hostResponse.setHostTags(ApiDBUtils.getHostTags(host.getId()));
        } else if (host.getType().toString().equals("Storage")) {
            hostResponse.setDiskSizeTotal(host.getTotalSize());
            hostResponse.setDiskSizeAllocated(0L);
        }

        if (host.getClusterId() != null) {
            ClusterVO cluster = ApiDBUtils.findClusterById(host.getClusterId());
            hostResponse.setClusterName(cluster.getName());
            hostResponse.setClusterType(cluster.getClusterType().toString());
        }

        hostResponse.setLocalStorageActive(ApiDBUtils.isLocalStorageActiveOnHost(host));

        Set<com.cloud.host.Status.Event> possibleEvents = host.getStatus().getPossibleEvents();
        if ((possibleEvents != null) && !possibleEvents.isEmpty()) {
            String events = "";
            Iterator<com.cloud.host.Status.Event> iter = possibleEvents.iterator();
            while (iter.hasNext()) {
                com.cloud.host.Status.Event event = iter.next();
                events += event.toString();
                if (iter.hasNext()) {
                    events += "; ";
                }
            }
            hostResponse.setEvents(events);
        }

        hostResponse.setAllocationState(host.getHostAllocationState().toString());

        hostResponse.setObjectName("host");

        return hostResponse;
    }

    @Override
    public VlanIpRangeResponse createVlanIpRangeResponse(Vlan vlan) {
        Long podId = ApiDBUtils.getPodIdForVlan(vlan.getId());

        VlanIpRangeResponse vlanResponse = new VlanIpRangeResponse();
        vlanResponse.setId(vlan.getId());
        vlanResponse.setForVirtualNetwork(vlan.getVlanType().equals(VlanType.VirtualNetwork));
        vlanResponse.setVlan(vlan.getVlanTag());
        vlanResponse.setZoneId(vlan.getDataCenterId());

        if (podId != null) {
            HostPodVO pod = ApiDBUtils.findPodById(podId);
            vlanResponse.setPodId(podId);
            if (pod != null) {
                vlanResponse.setPodName(pod.getName());
            }
        }

        vlanResponse.setGateway(vlan.getVlanGateway());
        vlanResponse.setNetmask(vlan.getVlanNetmask());

        // get start ip and end ip of corresponding vlan
        String ipRange = vlan.getIpRange();
        String[] range = ipRange.split("-");
        vlanResponse.setStartIp(range[0]);
        vlanResponse.setEndIp(range[1]);

        vlanResponse.setNetworkId(vlan.getNetworkId());
        Account owner = ApiDBUtils.getVlanAccount(vlan.getId());
        if (owner != null) {
            vlanResponse.setAccountName(owner.getAccountName());
            vlanResponse.setDomainId(owner.getDomainId());
            vlanResponse.setDomainName(ApiDBUtils.findDomainById(owner.getDomainId()).getName());
        }

        vlanResponse.setObjectName("vlan");
        return vlanResponse;
    }

    @Override
    public IPAddressResponse createIPAddressResponse(IpAddress ipAddress) {
        VlanVO vlan = ApiDBUtils.findVlanById(ipAddress.getVlanId());
        boolean forVirtualNetworks = vlan.getVlanType().equals(VlanType.VirtualNetwork);
        long zoneId = ipAddress.getDataCenterId();

        IPAddressResponse ipResponse = new IPAddressResponse();
        ipResponse.setId(ipAddress.getId());
        ipResponse.setIpAddress(ipAddress.getAddress().toString());
        if (ipAddress.getAllocatedTime() != null) {
            ipResponse.setAllocated(ipAddress.getAllocatedTime());
        }
        ipResponse.setZoneId(zoneId);
        ipResponse.setZoneName(ApiDBUtils.findZoneById(ipAddress.getDataCenterId()).getName());
        ipResponse.setSourceNat(ipAddress.isSourceNat());

        // get account information
        Account accountTemp = ApiDBUtils.findAccountById(ipAddress.getAllocatedToAccountId());
        if (accountTemp != null) {
            ipResponse.setAccountName(accountTemp.getAccountName());
            ipResponse.setDomainId(accountTemp.getDomainId());
            ipResponse.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
        }

        ipResponse.setForVirtualNetwork(forVirtualNetworks);
        ipResponse.setStaticNat(ipAddress.isOneToOneNat());

        if (ipAddress.getAssociatedWithVmId() != null) {
            UserVm vm = ApiDBUtils.findUserVmById(ipAddress.getAssociatedWithVmId());
            ipResponse.setVirtualMachineId(vm.getId());
            ipResponse.setVirtualMachineName(vm.getHostName());
            ipResponse.setVirtualMachineDisplayName(vm.getDisplayName());
        }

        ipResponse.setAssociatedNetworkId(ipAddress.getAssociatedWithNetworkId());

        // Network id the ip is associated withif associated networkId is null, try to get this information from vlan
        Long associatedNetworkId = ipAddress.getAssociatedWithNetworkId();
        Long vlanNetworkId = ApiDBUtils.getVlanNetworkId(ipAddress.getVlanId());
        if (associatedNetworkId == null) {
            associatedNetworkId = vlanNetworkId;
        }

        ipResponse.setAssociatedNetworkId(associatedNetworkId);

        // Network id the ip belongs to
        Long networkId;
        if (vlanNetworkId != null) {
            networkId = vlanNetworkId;
        } else {
            networkId = ApiDBUtils.getPublicNetworkIdByZone(zoneId);
        }

        ipResponse.setNetworkId(networkId);
        ipResponse.setState(ipAddress.getState().toString());

        // show this info to admin only
        Account account = UserContext.current().getCaller();
        if ((account == null) || account.getType() == Account.ACCOUNT_TYPE_ADMIN) {
            ipResponse.setVlanId(ipAddress.getVlanId());
            ipResponse.setVlanName(ApiDBUtils.findVlanById(ipAddress.getVlanId()).getVlanTag());
        }
        ipResponse.setObjectName("ipaddress");
        return ipResponse;
    }

    @Override
    public LoadBalancerResponse createLoadBalancerResponse(LoadBalancer loadBalancer) {
        LoadBalancerResponse lbResponse = new LoadBalancerResponse();
        lbResponse.setId(loadBalancer.getId());
        lbResponse.setName(loadBalancer.getName());
        lbResponse.setDescription(loadBalancer.getDescription());
        List<String> cidrs = ApiDBUtils.findFirewallSourceCidrs(loadBalancer.getId());
        lbResponse.setCidrList(StringUtils.join(cidrs, ","));

        IPAddressVO publicIp = ApiDBUtils.findIpAddressById(loadBalancer.getSourceIpAddressId());
        lbResponse.setPublicIpId(publicIp.getId());
        lbResponse.setPublicIp(publicIp.getAddress().addr());
        lbResponse.setPublicPort(Integer.toString(loadBalancer.getSourcePortStart()));
        lbResponse.setPrivatePort(Integer.toString(loadBalancer.getDefaultPortStart()));
        lbResponse.setAlgorithm(loadBalancer.getAlgorithm());
        FirewallRule.State state = loadBalancer.getState();
        String stateToSet = state.toString();
        if (state.equals(FirewallRule.State.Revoke)) {
            stateToSet = "Deleting";
        }
        lbResponse.setState(stateToSet);

        Account accountTemp = ApiDBUtils.findAccountById(loadBalancer.getAccountId());
        if (accountTemp != null) {
            lbResponse.setAccountName(accountTemp.getAccountName());
            lbResponse.setDomainId(accountTemp.getDomainId());
            lbResponse.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
        }

        lbResponse.setZoneId(publicIp.getDataCenterId());

        lbResponse.setObjectName("loadbalancer");
        return lbResponse;
    }

    @Override
    public PodResponse createPodResponse(Pod pod) {
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
        podResponse.setNetmask(NetUtils.getCidrNetmask(pod.getCidrSize()));
        podResponse.setStartIp(ipRange[0]);
        podResponse.setEndIp(((ipRange.length > 1) && (ipRange[1] != null)) ? ipRange[1] : "");
        podResponse.setGateway(pod.getGateway());
        podResponse.setAllocationState(pod.getAllocationState().toString());
        podResponse.setObjectName("pod");
        return podResponse;
    }

    @Override
    public ZoneResponse createZoneResponse(DataCenter dataCenter) {
        Account account = UserContext.current().getCaller();
        ZoneResponse zoneResponse = new ZoneResponse();
        zoneResponse.setId(dataCenter.getId());
        zoneResponse.setName(dataCenter.getName());
        zoneResponse.setSecurityGroupsEnabled(ApiDBUtils.isSecurityGroupEnabledInZone(dataCenter.getId()));

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
        zoneResponse.setType(dataCenter.getNetworkType().toString());
        zoneResponse.setAllocationState(dataCenter.getAllocationState().toString());
        zoneResponse.setZoneToken(dataCenter.getZoneToken());
        zoneResponse.setDhcpProvider(dataCenter.getDhcpProvider());
        zoneResponse.setObjectName("zone");
        return zoneResponse;
    }

    @Override
    public VolumeResponse createVolumeResponse(Volume volume) {
        VolumeResponse volResponse = new VolumeResponse();
        volResponse.setId(volume.getId());

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
        if (instanceId != null && volume.getState() != Volume.State.Destroy) {
            VMInstanceVO vm = ApiDBUtils.findVMInstanceById(instanceId);
            if(vm != null){
            	volResponse.setVirtualMachineId(vm.getId());
            	volResponse.setVirtualMachineName(vm.getHostName());
            	UserVm userVm = ApiDBUtils.findUserVmById(vm.getId());
            	if (userVm != null) {
            		volResponse.setVirtualMachineDisplayName(userVm.getDisplayName());
            		volResponse.setVirtualMachineState(vm.getState().toString());
            	} else {
            		s_logger.error("User Vm with Id: "+instanceId+" does not exist for volume "+volume.getId());	
            	}
            } else {
            	s_logger.error("Vm with Id: "+instanceId+" does not exist for volume "+volume.getId());	
            }
        }

        // Show the virtual size of the volume
        volResponse.setSize(volume.getSize());

        volResponse.setCreated(volume.getCreated());
        volResponse.setState(volume.getState().toString());

        Account accountTemp = ApiDBUtils.findAccountById(volume.getAccountId());
        if (accountTemp != null) {
            volResponse.setAccountName(accountTemp.getAccountName());
            volResponse.setDomainId(accountTemp.getDomainId());
            volResponse.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
        }

        String storageType;
        try {
            if (volume.getPoolId() == null) {
                if (volume.getState() == Volume.State.Allocated) {
                    /* set it as shared, so the UI can attach it to VM */
                    storageType = "shared";
                } else {
                    storageType = "unknown";
                }
            } else {
                storageType = ApiDBUtils.volumeIsOnSharedStorage(volume.getId()) ? ServiceOffering.StorageType.shared.toString() : ServiceOffering.StorageType.local.toString();
            }
        } catch (InvalidParameterValueException e) {
            s_logger.error(e.getMessage(), e);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Volume " + volume.getName() + " does not have a valid ID");
        }

        volResponse.setStorageType(storageType);
        if (volume.getVolumeType().equals(Volume.Type.ROOT)) {
            volResponse.setServiceOfferingId(volume.getDiskOfferingId());
        } else {
            volResponse.setDiskOfferingId(volume.getDiskOfferingId());
        }

        DiskOfferingVO diskOffering = ApiDBUtils.findDiskOfferingById(volume.getDiskOfferingId());
        if (volume.getVolumeType().equals(Volume.Type.ROOT)) {
            volResponse.setServiceOfferingName(diskOffering.getName());
            volResponse.setServiceOfferingDisplayText(diskOffering.getDisplayText());
        } else {
            volResponse.setDiskOfferingName(diskOffering.getName());
            volResponse.setDiskOfferingDisplayText(diskOffering.getDisplayText());
        }

        Long poolId = volume.getPoolId();
        String poolName = (poolId == null) ? "none" : ApiDBUtils.findStoragePoolById(poolId).getName();
        volResponse.setStoragePoolName(poolName);
        // volResponse.setSourceId(volume.getSourceId());
        // if (volume.getSourceType() != null) {
        // volResponse.setSourceType(volume.getSourceType().toString());
        // }
        volResponse.setHypervisor(ApiDBUtils.getVolumeHyperType(volume.getId()).toString());
        volResponse.setAttached(volume.getAttached());
        volResponse.setDestroyed(volume.getState() == Volume.State.Destroy);
        VMTemplateVO template = ApiDBUtils.findTemplateById(volume.getTemplateId());
        boolean isExtractable = template != null && template.isExtractable() && !(template.getTemplateType() == TemplateType.SYSTEM);
        volResponse.setExtractable(isExtractable);
        volResponse.setObjectName("volume");
        return volResponse;
    }

    @Override
    public InstanceGroupResponse createInstanceGroupResponse(InstanceGroup group) {
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
        groupResponse.setObjectName("instancegroup");
        return groupResponse;
    }

    @Override
    public StoragePoolResponse createStoragePoolResponse(StoragePool pool) {
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
            HostPodVO pod = ApiDBUtils.findPodById(pool.getPodId());
            if (pod != null) {
                poolResponse.setPodName(pod.getName());
            }
        }
        if (pool.getCreated() != null) {
            poolResponse.setCreated(pool.getCreated());
        }

        StorageStats stats = ApiDBUtils.getStoragePoolStatistics(pool.getId());
        Long capacity = pool.getCapacityBytes();
        Long available = pool.getAvailableBytes();
        Long used = capacity - available;

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
        poolResponse.setObjectName("storagepool");
        return poolResponse;
    }

    @Override
    public ClusterResponse createClusterResponse(Cluster cluster) {
        ClusterResponse clusterResponse = new ClusterResponse();
        clusterResponse.setId(cluster.getId());
        clusterResponse.setName(cluster.getName());
        clusterResponse.setPodId(cluster.getPodId());
        clusterResponse.setZoneId(cluster.getDataCenterId());
        clusterResponse.setHypervisorType(cluster.getHypervisorType().toString());
        clusterResponse.setClusterType(cluster.getClusterType().toString());
        clusterResponse.setAllocationState(cluster.getAllocationState().toString());
        clusterResponse.setManagedState(cluster.getManagedState().toString());
        HostPodVO pod = ApiDBUtils.findPodById(cluster.getPodId());
        if (pod != null) {
            clusterResponse.setPodName(pod.getName());
        }
        DataCenterVO zone = ApiDBUtils.findZoneById(cluster.getDataCenterId());
        clusterResponse.setZoneName(zone.getName());
        clusterResponse.setObjectName("cluster");
        return clusterResponse;
    }

    @Override
    public FirewallRuleResponse createPortForwardingRuleResponse(PortForwardingRule fwRule) {
        FirewallRuleResponse response = new FirewallRuleResponse();
        response.setId(fwRule.getId());
        response.setPrivateStartPort(Integer.toString(fwRule.getDestinationPortStart()));
        response.setPrivateEndPort(Integer.toString(fwRule.getDestinationPortEnd()));
        response.setProtocol(fwRule.getProtocol());
        response.setPublicStartPort(Integer.toString(fwRule.getSourcePortStart()));
        response.setPublicEndPort(Integer.toString(fwRule.getSourcePortEnd()));
        List<String> cidrs = ApiDBUtils.findFirewallSourceCidrs(fwRule.getId());
        response.setCidrList(StringUtils.join(cidrs, ","));

        IpAddress ip = ApiDBUtils.findIpAddressById(fwRule.getSourceIpAddressId());
        response.setPublicIpAddressId(ip.getId());
        response.setPublicIpAddress(ip.getAddress().addr());

        if (ip != null && fwRule.getDestinationIpAddress() != null) {
            UserVm vm = ApiDBUtils.findUserVmById(fwRule.getVirtualMachineId());
            if (vm != null) {
                response.setVirtualMachineId(vm.getId());
                response.setVirtualMachineName(vm.getHostName());
                response.setVirtualMachineDisplayName(vm.getDisplayName());
            }
        }
        FirewallRule.State state = fwRule.getState();
        String stateToSet = state.toString();
        if (state.equals(FirewallRule.State.Revoke)) {
            stateToSet = "Deleting";
        }
        response.setState(stateToSet);
        response.setObjectName("portforwardingrule");
        return response;
    }

    @Override
    public IpForwardingRuleResponse createIpForwardingRuleResponse(StaticNatRule fwRule) {
        IpForwardingRuleResponse response = new IpForwardingRuleResponse();
        response.setId(fwRule.getId());
        response.setProtocol(fwRule.getProtocol());

        IpAddress ip = ApiDBUtils.findIpAddressById(fwRule.getSourceIpAddressId());
        response.setPublicIpAddressId(ip.getId());
        response.setPublicIpAddress(ip.getAddress().addr());

        if (ip != null && fwRule.getDestIpAddress() != null) {
            UserVm vm = ApiDBUtils.findUserVmById(ip.getAssociatedWithVmId());
            if (vm != null) {// vm might be destroyed
                response.setVirtualMachineId(vm.getId());
                response.setVirtualMachineName(vm.getHostName());
                response.setVirtualMachineDisplayName(vm.getDisplayName());
            }
        }
        FirewallRule.State state = fwRule.getState();
        String stateToSet = state.toString();
        if (state.equals(FirewallRule.State.Revoke)) {
            stateToSet = "Deleting";
        }

        response.setStartPort(fwRule.getSourcePortStart());
        response.setEndPort(fwRule.getSourcePortEnd());
        response.setProtocol(fwRule.getProtocol());
        response.setState(stateToSet);
        response.setObjectName("ipforwardingrule");
        return response;
    }

    @Override
    public List<UserVmResponse> createUserVmResponse(String objectName, UserVm... userVms) {
        Account caller = UserContext.current().getCaller();
        boolean caller_is_admin = ((caller == null) || (caller.getType() == Account.ACCOUNT_TYPE_ADMIN));
        
        Hashtable<Long, UserVmData> vmDataList = new Hashtable<Long, UserVmData>();
        // Initialise the vmdatalist with the input data
        for (UserVm userVm : userVms) {
            UserVmData userVmData = newUserVmData(userVm);
            vmDataList.put(userVm.getId(), userVmData);
        }
        
        vmDataList = ApiDBUtils.listVmDetails(vmDataList);
        
        //initialize vmresponse from vmdatalist
        List<UserVmResponse> vmResponses = new ArrayList<UserVmResponse>();
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        for (UserVmData uvd: vmDataList.values()){
            UserVmResponse userVmResponse = newUserVmResponse(uvd, caller_is_admin);
            
            // stats calculation
            String cpuUsed = null;
            VmStats vmStats = ApiDBUtils.getVmStatistics(userVmResponse.getId());
            if (vmStats != null) {
                float cpuUtil = (float) vmStats.getCPUUtilization();
                cpuUsed = decimalFormat.format(cpuUtil) + "%";
                userVmResponse.setCpuUsed(cpuUsed);

                Double networkKbRead = Double.valueOf(vmStats.getNetworkReadKBs());
                userVmResponse.setNetworkKbsRead(networkKbRead.longValue());

                Double networkKbWrite = Double.valueOf(vmStats.getNetworkWriteKBs());
                userVmResponse.setNetworkKbsWrite(networkKbWrite.longValue());
            }
            userVmResponse.setObjectName(objectName);
            
            vmResponses.add(userVmResponse);
        }
        return vmResponses;
    }


    @Override
    public DomainRouterResponse createDomainRouterResponse(VirtualRouter router) {
        Map<Long, ServiceOffering> serviceOfferings = new HashMap<Long, ServiceOffering>();

        DomainRouterResponse routerResponse = new DomainRouterResponse();
        routerResponse.setId(router.getId());
        routerResponse.setZoneId(router.getDataCenterIdToDeployIn());
        routerResponse.setName(router.getHostName());
        routerResponse.setPodId(router.getPodIdToDeployIn());
        routerResponse.setTemplateId(router.getTemplateId());
        routerResponse.setCreated(router.getCreated());
        routerResponse.setState(router.getState());
        routerResponse.setIsRedundantRouter(router.getIsRedundantRouter());
        routerResponse.setRedundantState(router.getRedundantState().toString());

        if (router.getHostId() != null) {
            routerResponse.setHostId(router.getHostId());
            routerResponse.setHostName(ApiDBUtils.findHostById(router.getHostId()).getName());
        }

        // Service Offering Info
        ServiceOffering offering = serviceOfferings.get(router.getServiceOfferingId());

        if (offering == null) {
            offering = ApiDBUtils.findServiceOfferingById(router.getServiceOfferingId());
            serviceOfferings.put(offering.getId(), offering);
        }
        routerResponse.setServiceOfferingId(offering.getId());
        routerResponse.setServiceOfferingName(offering.getName());

        Account accountTemp = ApiDBUtils.findAccountById(router.getAccountId());
        if (accountTemp != null) {
            routerResponse.setAccountName(accountTemp.getAccountName());
            routerResponse.setDomainId(accountTemp.getDomainId());
            routerResponse.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
        }

        List<NicProfile> nicProfiles = ApiDBUtils.getNics(router);
        for (NicProfile singleNicProfile : nicProfiles) {
            Network network = ApiDBUtils.findNetworkById(singleNicProfile.getNetworkId());
            if (network != null) {
                if (network.getTrafficType() == TrafficType.Public) {
                    routerResponse.setPublicIp(singleNicProfile.getIp4Address());
                    routerResponse.setPublicMacAddress(singleNicProfile.getMacAddress());
                    routerResponse.setPublicNetmask(singleNicProfile.getNetmask());
                    routerResponse.setGateway(singleNicProfile.getGateway());
                    routerResponse.setPublicNetworkId(singleNicProfile.getNetworkId());
                } else if (network.getTrafficType() == TrafficType.Control) {
                    routerResponse.setLinkLocalIp(singleNicProfile.getIp4Address());
                    routerResponse.setLinkLocalMacAddress(singleNicProfile.getMacAddress());
                    routerResponse.setLinkLocalMacAddress(singleNicProfile.getNetmask());
                    routerResponse.setLinkLocalNetworkId(singleNicProfile.getNetworkId());
                } else if (network.getTrafficType() == TrafficType.Guest) {
                    routerResponse.setGuestIpAddress(singleNicProfile.getIp4Address());
                    routerResponse.setGuestMacAddress(singleNicProfile.getMacAddress());
                    routerResponse.setGuestNetmask(singleNicProfile.getNetmask());
                    routerResponse.setGuestNetworkId(singleNicProfile.getNetworkId());
                    routerResponse.setNetworkDomain(network.getNetworkDomain());
                }
            }
        }
        DataCenter zone = ApiDBUtils.findZoneById(router.getDataCenterIdToDeployIn());
        if (zone != null) {
            routerResponse.setZoneName(zone.getName());
            routerResponse.setDns1(zone.getDns1());
            routerResponse.setDns2(zone.getDns2());
        }

        routerResponse.setObjectName("domainrouter");
        return routerResponse;
    }

    @Override
    public SystemVmResponse createSystemVmResponse(VirtualMachine vm) {
        SystemVmResponse vmResponse = new SystemVmResponse();
        if (vm.getType() == Type.SecondaryStorageVm || vm.getType() == Type.ConsoleProxy) {
            // SystemVm vm = (SystemVm) systemVM;

            vmResponse.setId(vm.getId());
            vmResponse.setSystemVmType(vm.getType().toString().toLowerCase());
            vmResponse.setZoneId(vm.getDataCenterIdToDeployIn());

            vmResponse.setName(vm.getHostName());
            vmResponse.setPodId(vm.getPodIdToDeployIn());
            vmResponse.setTemplateId(vm.getTemplateId());
            vmResponse.setCreated(vm.getCreated());

            if (vm.getHostId() != null) {
                vmResponse.setHostId(vm.getHostId());
                vmResponse.setHostName(ApiDBUtils.findHostById(vm.getHostId()).getName());
            }

            if (vm.getState() != null) {
                vmResponse.setState(vm.getState().toString());
            }

            // for console proxies, add the active sessions
            if (vm.getType() == Type.ConsoleProxy) {
                ConsoleProxyVO proxy = ApiDBUtils.findConsoleProxy(vm.getId());
                //proxy can be already destroyed
                if (proxy != null) {
                    vmResponse.setActiveViewerSessions(proxy.getActiveSession());
                } 
            }

            DataCenter zone = ApiDBUtils.findZoneById(vm.getDataCenterIdToDeployIn());
            if (zone != null) {
                vmResponse.setZoneName(zone.getName());
                vmResponse.setDns1(zone.getDns1());
                vmResponse.setDns2(zone.getDns2());
            }

            List<NicProfile> nicProfiles = ApiDBUtils.getNics(vm);
            for (NicProfile singleNicProfile : nicProfiles) {
                Network network = ApiDBUtils.findNetworkById(singleNicProfile.getNetworkId());
                if (network != null) {
                    TrafficType trafficType = TrafficType.Public;
                    if (zone.getNetworkType() == NetworkType.Basic || zone.isSecurityGroupEnabled()) {
                        trafficType = TrafficType.Guest;
                    }
                    if (network.getTrafficType() == trafficType) {
                        vmResponse.setPublicIp(singleNicProfile.getIp4Address());
                        vmResponse.setPublicMacAddress(singleNicProfile.getMacAddress());
                        vmResponse.setPublicNetmask(singleNicProfile.getNetmask());
                        vmResponse.setGateway(singleNicProfile.getGateway());
                    } else if (network.getTrafficType() == TrafficType.Management) {
                        vmResponse.setPrivateIp(singleNicProfile.getIp4Address());
                        vmResponse.setPrivateMacAddress(singleNicProfile.getMacAddress());
                        vmResponse.setPrivateNetmask(singleNicProfile.getNetmask());
                    } else if (network.getTrafficType() == TrafficType.Control) {
                        vmResponse.setLinkLocalIp(singleNicProfile.getIp4Address());
                        vmResponse.setLinkLocalMacAddress(singleNicProfile.getMacAddress());
                        vmResponse.setLinkLocalNetmask(singleNicProfile.getNetmask());
                    }
                }
            }
        }
        vmResponse.setObjectName("systemvm");
        return vmResponse;
    }

    @Override
    public Host findHostById(Long hostId) {
        return ApiDBUtils.findHostById(hostId);
    }

    @Override
    public User findUserById(Long userId) {
        return ApiDBUtils.findUserById(userId);
    }

    @Override
    public UserVm findUserVmById(Long vmId) {
        return ApiDBUtils.findUserVmById(vmId);

    }

    @Override
    public VolumeVO findVolumeById(Long volumeId) {
        return ApiDBUtils.findVolumeById(volumeId);
    }

    @Override
    public Account findAccountByNameDomain(String accountName, Long domainId) {
        return ApiDBUtils.findAccountByNameDomain(accountName, domainId);
    }

    @Override
    public VirtualMachineTemplate findTemplateById(Long templateId) {
        return ApiDBUtils.findTemplateById(templateId);
    }

    @Override
    public VpnUsersResponse createVpnUserResponse(VpnUser vpnUser) {
        VpnUsersResponse vpnResponse = new VpnUsersResponse();
        vpnResponse.setId(vpnUser.getId());
        vpnResponse.setUserName(vpnUser.getUsername());

        Account accountTemp = ApiDBUtils.findAccountById(vpnUser.getAccountId());
        if (accountTemp != null) {
            vpnResponse.setAccountName(accountTemp.getAccountName());
            vpnResponse.setDomainId(accountTemp.getDomainId());
            vpnResponse.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
        }

        vpnResponse.setObjectName("vpnuser");
        return vpnResponse;
    }

    @Override
    public RemoteAccessVpnResponse createRemoteAccessVpnResponse(RemoteAccessVpn vpn) {
        RemoteAccessVpnResponse vpnResponse = new RemoteAccessVpnResponse();
        vpnResponse.setPublicIpId(vpn.getServerAddressId());
        vpnResponse.setPublicIp(ApiDBUtils.findIpAddressById(vpn.getServerAddressId()).getAddress().addr());
        vpnResponse.setIpRange(vpn.getIpRange());
        vpnResponse.setPresharedKey(vpn.getIpsecPresharedKey());
        vpnResponse.setDomainId(vpn.getDomainId());

        Account accountTemp = ApiDBUtils.findAccountById(vpn.getAccountId());
        if (accountTemp != null) {
            vpnResponse.setAccountName(accountTemp.getAccountName());
            vpnResponse.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
        }
        vpnResponse.setState(vpn.getState().toString());
        vpnResponse.setObjectName("remoteaccessvpn");

        return vpnResponse;
    }

    @Override
    public TemplateResponse createIsoResponse(VirtualMachineTemplate result) {
        TemplateResponse response = new TemplateResponse();
        response.setId(result.getId());
        response.setName(result.getName());
        response.setDisplayText(result.getDisplayText());
        response.setPublic(result.isPublicTemplate());
        response.setCreated(result.getCreated());
        response.setFormat(result.getFormat());
        response.setOsTypeId(result.getGuestOSId());
        response.setOsTypeName(ApiDBUtils.findGuestOSById(result.getGuestOSId()).getDisplayName());

        if (result.getFormat() == ImageFormat.ISO) { // Templates are always bootable
            response.setBootable(result.isBootable());
        } else {
            response.setHypervisor(result.getHypervisorType().toString());// hypervisors are associated with templates
        }

        // add account ID and name
        Account owner = ApiDBUtils.findAccountById(result.getAccountId());
        if (owner != null) {
            response.setAccount(owner.getAccountName());
            response.setDomainId(owner.getDomainId());
            response.setDomainName(ApiDBUtils.findDomainById(owner.getDomainId()).getName());
        }
        response.setObjectName("iso");
        return response;
    }

    @Override
    public List<TemplateResponse> createTemplateResponses(long templateId, Long zoneId, boolean readyOnly) {
        List<DataCenterVO> dcs = new ArrayList<DataCenterVO>();
        if (zoneId == null || zoneId == -1) {
            dcs.addAll(ApiDBUtils.listZones());
            List<TemplateResponse> response = new ArrayList<TemplateResponse>();
            for (DataCenterVO dc : dcs ) {
                response.addAll(createTemplateResponses(templateId, dc.getId(), readyOnly));
            }
            return response;
        } else {
            return createTemplateResponses(templateId, zoneId.longValue(), readyOnly);
        }
    }

    @Override
    public List<TemplateResponse> createTemplateResponses(long templateId, long zoneId, boolean readyOnly) {
        VirtualMachineTemplate template = findTemplateById(templateId);
        List<TemplateResponse> responses = new ArrayList<TemplateResponse>();
        VMTemplateHostVO templateHostRef = ApiDBUtils.findTemplateHostRef(templateId, zoneId, readyOnly);
        if (templateHostRef == null) {
            return responses;
        }

        HostVO host = ApiDBUtils.findHostById(templateHostRef.getHostId());
        if (host.getType() == Host.Type.LocalSecondaryStorage && host.getStatus() != com.cloud.host.Status.Up) {
            return responses;
        }

        TemplateResponse templateResponse = new TemplateResponse();
        templateResponse.setId(template.getId());
        templateResponse.setName(template.getName());
        templateResponse.setDisplayText(template.getDisplayText());
        templateResponse.setPublic(template.isPublicTemplate());
        templateResponse.setCreated(templateHostRef.getCreated());

        templateResponse.setReady(templateHostRef.getDownloadState() == Status.DOWNLOADED);
        templateResponse.setFeatured(template.isFeatured());
        templateResponse.setExtractable(template.isExtractable() && !(template.getTemplateType() == TemplateType.SYSTEM));
        templateResponse.setPasswordEnabled(template.getEnablePassword());
        templateResponse.setCrossZones(template.isCrossZones());
        templateResponse.setFormat(template.getFormat());
        if (template.getTemplateType() != null) {
            templateResponse.setTemplateType(template.getTemplateType().toString());
        }
        templateResponse.setHypervisor(template.getHypervisorType().toString());

        GuestOS os = ApiDBUtils.findGuestOSById(template.getGuestOSId());
        if (os != null) {
            templateResponse.setOsTypeId(os.getId());
            templateResponse.setOsTypeName(os.getDisplayName());
        } else {
            templateResponse.setOsTypeId(-1L);
            templateResponse.setOsTypeName("");
        }

        // add account ID and name
        Account owner = ApiDBUtils.findAccountById(template.getAccountId());
        if (owner != null) {
            templateResponse.setAccount(owner.getAccountName());
            templateResponse.setDomainId(owner.getDomainId());
            templateResponse.setDomainName(ApiDBUtils.findDomainById(owner.getDomainId()).getName());
        }



        DataCenterVO datacenter = ApiDBUtils.findZoneById(zoneId);

        // Add the zone ID
        templateResponse.setZoneId(zoneId);
        templateResponse.setZoneName(datacenter.getName());

        Account account = UserContext.current().getCaller();
        boolean isAdmin = false;
        if ((account == null) || BaseCmd.isAdmin(account.getType())) {
            isAdmin = true;
        }

        // If the user is an Admin, add the template download status
        if (isAdmin || account.getId() == template.getAccountId()) {
            // add download status
            if (templateHostRef.getDownloadState() != Status.DOWNLOADED) {
                String templateStatus = "Processing";
                if (templateHostRef.getDownloadState() == VMTemplateHostVO.Status.DOWNLOAD_IN_PROGRESS) {
                    if (templateHostRef.getDownloadPercent() == 100) {
                        templateStatus = "Installing Template";
                    } else {
                        templateStatus = templateHostRef.getDownloadPercent() + "% Downloaded";
                    }
                } else {
                    templateStatus = templateHostRef.getErrorString();
                }
                templateResponse.setStatus(templateStatus);
            } else if (templateHostRef.getDownloadState() == VMTemplateHostVO.Status.DOWNLOADED) {
                templateResponse.setStatus("Download Complete");
            } else {
                templateResponse.setStatus("Successfully Installed");
            }
        }

        Long templateSize = templateHostRef.getSize();
        if (templateSize > 0) {
            templateResponse.setSize(templateSize);
        }

        templateResponse.setChecksum(template.getChecksum());
        templateResponse.setSourceTemplateId(template.getSourceTemplateId());

        templateResponse.setChecksum(template.getChecksum());

        templateResponse.setTemplateTag(template.getTemplateTag());

        templateResponse.setObjectName("template");
        responses.add(templateResponse);
        return responses;
    }
    @Override
    public List<TemplateResponse> createIsoResponses(long isoId, Long zoneId, boolean readyOnly) {
        List<TemplateResponse> isoResponses = new ArrayList<TemplateResponse>();
        VirtualMachineTemplate iso = findTemplateById(isoId);
        if ( iso.getTemplateType() == TemplateType.PERHOST) {
            TemplateResponse isoResponse = new TemplateResponse();
            isoResponse.setId(iso.getId());
            isoResponse.setName(iso.getName());
            isoResponse.setDisplayText(iso.getDisplayText());
            isoResponse.setPublic(iso.isPublicTemplate());
            isoResponse.setExtractable(iso.isExtractable() && !(iso.getTemplateType() == TemplateType.PERHOST));
            isoResponse.setReady(true);
            isoResponse.setBootable(iso.isBootable());
            isoResponse.setFeatured(iso.isFeatured());
            isoResponse.setCrossZones(iso.isCrossZones());
            isoResponse.setPublic(iso.isPublicTemplate());
            isoResponse.setCreated(iso.getCreated());
            isoResponse.setPasswordEnabled(false);
            Account owner = ApiDBUtils.findAccountById(iso.getAccountId());
            if (owner != null) {
                isoResponse.setAccount(owner.getAccountName());
                isoResponse.setDomainId(owner.getDomainId());
                isoResponse.setDomainName(ApiDBUtils.findDomainById(owner.getDomainId()).getName());
            }
            isoResponse.setObjectName("iso");
            isoResponses.add(isoResponse);
            return isoResponses;
        } else {
            List<DataCenterVO> dcs = new ArrayList<DataCenterVO>();       
            if (zoneId == null || zoneId == -1 ) {
                dcs.addAll(ApiDBUtils.listZones());
                for (DataCenterVO dc : dcs ) {
                    isoResponses.addAll(createIsoResponses(iso, dc.getId(), readyOnly));
                }
                return isoResponses;
            } else {
                return createIsoResponses(iso, zoneId, readyOnly);
            }
        }
    }

    @Override
    public List<TemplateResponse> createIsoResponses(VirtualMachineTemplate iso, long zoneId, boolean readyOnly) {
        long isoId = iso.getId();
        List<TemplateResponse> isoResponses = new ArrayList<TemplateResponse>();
        VMTemplateHostVO isoHost = ApiDBUtils.findTemplateHostRef(isoId, zoneId, readyOnly);
        if( isoHost == null ) {
            return isoResponses;
        }
        TemplateResponse isoResponse = new TemplateResponse();
        isoResponse.setId(iso.getId());
        isoResponse.setName(iso.getName());
        isoResponse.setDisplayText(iso.getDisplayText());
        isoResponse.setPublic(iso.isPublicTemplate());
        isoResponse.setExtractable(iso.isExtractable() && !(iso.getTemplateType() == TemplateType.PERHOST));
        isoResponse.setCreated(isoHost.getCreated());
        isoResponse.setReady(isoHost.getDownloadState() == Status.DOWNLOADED);
        isoResponse.setBootable(iso.isBootable());
        isoResponse.setFeatured(iso.isFeatured());
        isoResponse.setCrossZones(iso.isCrossZones());
        isoResponse.setPublic(iso.isPublicTemplate());

        // TODO: implement
        GuestOS os = ApiDBUtils.findGuestOSById(iso.getGuestOSId());
        if (os != null) {
            isoResponse.setOsTypeId(os.getId());
            isoResponse.setOsTypeName(os.getDisplayName());
        } else {
            isoResponse.setOsTypeId(-1L);
            isoResponse.setOsTypeName("");
        }

        // add account ID and name
        Account owner = ApiDBUtils.findAccountById(iso.getAccountId());
        if (owner != null) {
            isoResponse.setAccount(owner.getAccountName());
            isoResponse.setDomainId(owner.getDomainId());
            // TODO: implement
            isoResponse.setDomainName(ApiDBUtils.findDomainById(owner.getDomainId()).getName());
        }

        Account account = UserContext.current().getCaller();
        boolean isAdmin = false;
        if ((account == null) || BaseCmd.isAdmin(account.getType())) {
            isAdmin = true;
        }
        // Add the zone ID
        DataCenterVO datacenter = ApiDBUtils.findZoneById(zoneId);
        isoResponse.setZoneId(zoneId);
        isoResponse.setZoneName(datacenter.getName());

        // If the user is an admin, add the template download status
        if (isAdmin || account.getId() == iso.getAccountId()) {
            // add download status
            if (isoHost.getDownloadState() != Status.DOWNLOADED) {
                String isoStatus = "Processing";
                if (isoHost.getDownloadState() == VMTemplateHostVO.Status.DOWNLOADED) {
                    isoStatus = "Download Complete";
                } else if (isoHost.getDownloadState() == VMTemplateHostVO.Status.DOWNLOAD_IN_PROGRESS) {
                    if (isoHost.getDownloadPercent() == 100) {
                        isoStatus = "Installing ISO";
                    } else {
                        isoStatus = isoHost.getDownloadPercent() + "% Downloaded";
                    }
                } else {
                    isoStatus = isoHost.getErrorString();
                }
                isoResponse.setStatus(isoStatus);
            } else {
                isoResponse.setStatus("Successfully Installed");
            }
        }

        Long isoSize = isoHost.getSize();
        if (isoSize > 0) {
            isoResponse.setSize(isoSize);
        }

        isoResponse.setObjectName("iso");
        isoResponses.add(isoResponse);
        return isoResponses;
    }

    @Override
    public ListResponse<SecurityGroupResponse> createSecurityGroupResponses(List<? extends SecurityGroupRules> networkGroups) {
        List<SecurityGroupResultObject> groupResultObjs = SecurityGroupResultObject.transposeNetworkGroups(networkGroups);

        ListResponse<SecurityGroupResponse> response = new ListResponse<SecurityGroupResponse>();
        List<SecurityGroupResponse> netGrpResponses = new ArrayList<SecurityGroupResponse>();
        for (SecurityGroupResultObject networkGroup : groupResultObjs) {
            SecurityGroupResponse netGrpResponse = new SecurityGroupResponse();
            netGrpResponse.setId(networkGroup.getId());
            netGrpResponse.setName(networkGroup.getName());
            netGrpResponse.setDescription(networkGroup.getDescription());
            netGrpResponse.setAccountName(networkGroup.getAccountName());
            netGrpResponse.setDomainId(networkGroup.getDomainId());
            netGrpResponse.setDomainName(ApiDBUtils.findDomainById(networkGroup.getDomainId()).getName());

            List<IngressRuleResultObject> ingressRules = networkGroup.getIngressRules();
            if ((ingressRules != null) && !ingressRules.isEmpty()) {
                List<IngressRuleResponse> ingressRulesResponse = new ArrayList<IngressRuleResponse>();

                for (IngressRuleResultObject ingressRule : ingressRules) {
                    IngressRuleResponse ingressData = new IngressRuleResponse();

                    ingressData.setRuleId(ingressRule.getId());
                    ingressData.setProtocol(ingressRule.getProtocol());
                    if ("icmp".equalsIgnoreCase(ingressRule.getProtocol())) {
                        ingressData.setIcmpType(ingressRule.getStartPort());
                        ingressData.setIcmpCode(ingressRule.getEndPort());
                    } else {
                        ingressData.setStartPort(ingressRule.getStartPort());
                        ingressData.setEndPort(ingressRule.getEndPort());
                    }

                    if (ingressRule.getAllowedSecurityGroup() != null) {
                        ingressData.setSecurityGroupName(ingressRule.getAllowedSecurityGroup());
                        ingressData.setAccountName(ingressRule.getAllowedSecGroupAcct());
                    } else {
                        ingressData.setCidr(ingressRule.getAllowedSourceIpCidr());
                    }

                    ingressData.setObjectName("ingressrule");
                    ingressRulesResponse.add(ingressData);
                }
                netGrpResponse.setIngressRules(ingressRulesResponse);
            }
            netGrpResponse.setObjectName("securitygroup");
            netGrpResponses.add(netGrpResponse);
        }

        response.setResponses(netGrpResponses);
        return response;
    }

    @Override
    public SecurityGroupResponse createSecurityGroupResponse(SecurityGroup group) {
        SecurityGroupResponse response = new SecurityGroupResponse();
        Account account = ApiDBUtils.findAccountById(group.getAccountId());

        response.setAccountName(account.getAccountName());
        response.setDomainId(group.getDomainId());
        response.setDescription(group.getDescription());
        response.setDomainName(ApiDBUtils.findDomainById(group.getDomainId()).getName());
        response.setId(group.getId());
        response.setName(group.getName());

        response.setObjectName("securitygroup");
        return response;

    }

    @Override
    public ExtractResponse createExtractResponse(Long uploadId, Long id, Long zoneId, Long accountId, String mode) {
        UploadVO uploadInfo = ApiDBUtils.findUploadById(uploadId);
        ExtractResponse response = new ExtractResponse();
        response.setObjectName("template");
        response.setId(id);
        response.setName(ApiDBUtils.findTemplateById(id).getName());
        response.setZoneId(zoneId);
        response.setZoneName(ApiDBUtils.findZoneById(zoneId).getName());
        response.setMode(mode);
        response.setUploadId(uploadId);
        response.setState(uploadInfo.getUploadState().toString());
        response.setAccountId(accountId);
        // FIX ME - Need to set the url once the gson jar is upgraded since it is throwing an error right now.
        // response.setUrl(uploadInfo.getUploadUrl());
        response.setUrl(uploadInfo.getUploadUrl().replaceAll("/", "%2F"));
        return response;

    }

    @Override
    public String toSerializedString(CreateCmdResponse response, String responseType) {
        return ApiResponseSerializer.toSerializedString(response, responseType);
    }

    @Override
    public AsyncJobResponse createAsyncJobResponse(AsyncJob job) {
        AsyncJobResponse jobResponse = new AsyncJobResponse();
        jobResponse.setAccountId(job.getAccountId());
        jobResponse.setCmd(job.getCmd());
        jobResponse.setCreated(job.getCreated());
        jobResponse.setId(job.getId());

        if (job.getInstanceType() != null && job.getInstanceId() != null) {
            jobResponse.setJobInstanceId(job.getInstanceId());
            jobResponse.setJobInstanceType(job.getInstanceType().toString());
        }
        jobResponse.setJobProcStatus(job.getProcessStatus());
        jobResponse.setJobResult((ResponseObject) ApiSerializerHelper.fromSerializedString(job.getResult()));
        jobResponse.setJobResultCode(job.getResultCode());
        jobResponse.setJobStatus(job.getStatus());
        jobResponse.setUserId(job.getUserId());

        jobResponse.setObjectName("asyncjobs");
        return jobResponse;
    }

    @Override
    public List<TemplateResponse> createTemplateResponses(long templateId, Long snapshotId, Long volumeId, boolean readyOnly) {
        VolumeVO volume = null;
        if (snapshotId != null) {
            Snapshot snapshot = ApiDBUtils.findSnapshotById(snapshotId);
            volume = findVolumeById(snapshot.getVolumeId());
        } else {
            volume = findVolumeById(volumeId);
        }
        return createTemplateResponses(templateId, volume.getDataCenterId(), readyOnly);
    }

    @Override
    public List<TemplateResponse> createTemplateResponses(long templateId, Long vmId) {
        UserVm vm = findUserVmById(vmId);
        Long hostId = (vm.getHostId() == null ? vm.getLastHostId() : vm.getHostId());
        Host host = findHostById(hostId);
        return createTemplateResponses(templateId, host.getDataCenterId(), true);
    }

    @Override
    public EventResponse createEventResponse(Event event) {
        EventResponse responseEvent = new EventResponse();
        responseEvent.setAccountName(event.getAccountName());
        responseEvent.setCreated(event.getCreateDate());
        responseEvent.setDescription(event.getDescription());
        responseEvent.setDomainId(event.getDomainId());
        responseEvent.setEventType(event.getType());
        responseEvent.setId(event.getId());
        responseEvent.setLevel(event.getLevel());
        responseEvent.setParentId(event.getStartId());
        responseEvent.setState(event.getState());
        responseEvent.setDomainName(ApiDBUtils.findDomainById(event.getDomainId()).getName());
        User user = ApiDBUtils.findUserById(event.getUserId());
        if (user != null) {
            responseEvent.setUsername(user.getUsername());
        }

        responseEvent.setObjectName("event");
        return responseEvent;
    }


    private List<CapacityVO> sumCapacities(List<? extends Capacity> hostCapacities) {
        Map<String, Long> totalCapacityMap = new HashMap<String, Long>();
        Map<String, Long> usedCapacityMap = new HashMap<String, Long>();

        Set<Long> poolIdsToIgnore = new HashSet<Long>();
        Criteria c = new Criteria();
        // TODO: implement
        List<? extends StoragePoolVO> allStoragePools = ApiDBUtils.searchForStoragePools(c);
        for (StoragePoolVO pool : allStoragePools) {
            StoragePoolType poolType = pool.getPoolType();
            if (!(poolType.isShared())) {// All the non shared storages shouldn't show up in the capacity calculation
                poolIdsToIgnore.add(pool.getId());
            }
        }

        float cpuOverprovisioningFactor = ApiDBUtils.getCpuOverprovisioningFactor();

        // collect all the capacity types, sum allocated/used and sum total...get one capacity number for each
        for (Capacity capacity : hostCapacities) {
            
            //check if zone exist
            DataCenter zone = ApiDBUtils.findZoneById(capacity.getDataCenterId());
            if (zone == null) {
                continue;
            }
            
            short capacityType = capacity.getCapacityType();

            //If local storage then ignore
            if ( (capacityType == Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED || capacityType == Capacity.CAPACITY_TYPE_STORAGE) 
                    && poolIdsToIgnore.contains(capacity.getHostOrPoolId())) {
                continue;
            }

            String key = capacity.getCapacityType() + "_" + capacity.getDataCenterId();
            String keyForPodTotal = key + "_-1";

            boolean sumPodCapacity = false;
            if (capacity.getPodId() != null) {
                key += "_" + capacity.getPodId();
                sumPodCapacity = true;
            }

            Long totalCapacity = totalCapacityMap.get(key);
            Long usedCapacity = usedCapacityMap.get(key);

            // reset overprovisioning factor to 1
            float overprovisioningFactor = 1;
            if (capacityType == Capacity.CAPACITY_TYPE_CPU) {
                overprovisioningFactor = cpuOverprovisioningFactor;
            }

            if (totalCapacity == null) {
                totalCapacity = new Long((long) (capacity.getTotalCapacity() * overprovisioningFactor));
            } else {
                totalCapacity = new Long((long) (capacity.getTotalCapacity() * overprovisioningFactor)) + totalCapacity;
            }

            if (usedCapacity == null) {
                usedCapacity = new Long(capacity.getUsedCapacity());
            } else {
                usedCapacity = new Long(capacity.getUsedCapacity() + usedCapacity);
            }

            if (capacityType == Capacity.CAPACITY_TYPE_CPU || capacityType == Capacity.CAPACITY_TYPE_MEMORY) { // Reserved
                                                                                                               // Capacity
                                                                                                               // accounts for
                                                                                                               // stopped vms
                                                                                                               // that have been
                                                                                                               // stopped within
                                                                                                               // an interval
                usedCapacity += capacity.getReservedCapacity();
            }

            totalCapacityMap.put(key, totalCapacity);
            usedCapacityMap.put(key, usedCapacity);

            if (sumPodCapacity) {
                totalCapacity = totalCapacityMap.get(keyForPodTotal);
                usedCapacity = usedCapacityMap.get(keyForPodTotal);

                overprovisioningFactor = 1;
                if (capacityType == Capacity.CAPACITY_TYPE_CPU) {
                    overprovisioningFactor = cpuOverprovisioningFactor;
                }

                if (totalCapacity == null) {
                    totalCapacity = new Long((long) (capacity.getTotalCapacity() * overprovisioningFactor));
                } else {
                    totalCapacity = new Long((long) (capacity.getTotalCapacity() * overprovisioningFactor)) + totalCapacity;
                }

                if (usedCapacity == null) {
                    usedCapacity = new Long(capacity.getUsedCapacity());
                } else {
                    usedCapacity = new Long(capacity.getUsedCapacity() + usedCapacity);
                }

                if (capacityType == Capacity.CAPACITY_TYPE_CPU || capacityType == Capacity.CAPACITY_TYPE_MEMORY) { // Reserved
                                                                                                                   // Capacity
                                                                                                                   // accounts
                                                                                                                   // for
                                                                                                                   // stopped
                                                                                                                   // vms that
                                                                                                                   // have been
                                                                                                                   // stopped
                                                                                                                   // within an
                                                                                                                   // interval
                    usedCapacity += capacity.getReservedCapacity();
                }

                totalCapacityMap.put(keyForPodTotal, totalCapacity);
                usedCapacityMap.put(keyForPodTotal, usedCapacity);
            }
        }

        List<CapacityVO> summedCapacities = new ArrayList<CapacityVO>();
        for (String key : totalCapacityMap.keySet()) {
            CapacityVO summedCapacity = new CapacityVO();

            StringTokenizer st = new StringTokenizer(key, "_");
            summedCapacity.setCapacityType(Short.parseShort(st.nextToken()));
            summedCapacity.setDataCenterId(Long.parseLong(st.nextToken()));
            if (st.hasMoreTokens()) {
                summedCapacity.setPodId(Long.parseLong(st.nextToken()));
            }

            summedCapacity.setTotalCapacity(totalCapacityMap.get(key));
            summedCapacity.setUsedCapacity(usedCapacityMap.get(key));

            summedCapacities.add(summedCapacity);
        }
        return summedCapacities;
    }

    @Override
    public List<CapacityResponse> createCapacityResponse(List<? extends Capacity> result, DecimalFormat format) {
        List<CapacityResponse> capacityResponses = new ArrayList<CapacityResponse>();
        List<CapacityVO> summedCapacities = sumCapacities(result);
        for (CapacityVO summedCapacity : summedCapacities) { 
            CapacityResponse capacityResponse = new CapacityResponse();
            capacityResponse.setCapacityTotal(summedCapacity.getTotalCapacity());
            capacityResponse.setCapacityType(summedCapacity.getCapacityType());
            capacityResponse.setCapacityUsed(summedCapacity.getUsedCapacity());
            if (summedCapacity.getPodId() != null) {
                capacityResponse.setPodId(summedCapacity.getPodId());
                if (summedCapacity.getPodId() > 0) {
                    HostPodVO pod = ApiDBUtils.findPodById(summedCapacity.getPodId());
                    if (pod != null) {
                        capacityResponse.setPodName(pod.getName());
                    }
                } else {
                    capacityResponse.setPodName("All");
                }
            }
            capacityResponse.setZoneId(summedCapacity.getDataCenterId());
            capacityResponse.setZoneName(ApiDBUtils.findZoneById(summedCapacity.getDataCenterId()).getName());
            if (summedCapacity.getTotalCapacity() != 0) {
                capacityResponse.setPercentUsed(format.format((float) summedCapacity.getUsedCapacity() / (float) summedCapacity.getTotalCapacity() * 100f));
            } else {
                capacityResponse.setPercentUsed(format.format(0L));
            }

            capacityResponse.setObjectName("capacity");
            capacityResponses.add(capacityResponse);
        }

        return capacityResponses;
    }

    @Override
    public TemplatePermissionsResponse createTemplatePermissionsResponse(List<String> accountNames, Long id, boolean isAdmin) {
        Long templateOwnerDomain = null;
        VirtualMachineTemplate template = ApiDBUtils.findTemplateById(id);
        if (isAdmin) {
            // FIXME: we have just template id and need to get template owner from that
            Account templateOwner = ApiDBUtils.findAccountById(template.getAccountId());
            if (templateOwner != null) {
                templateOwnerDomain = templateOwner.getDomainId();
            }
        }

        TemplatePermissionsResponse response = new TemplatePermissionsResponse();
        response.setId(template.getId());
        response.setPublicTemplate(template.isPublicTemplate());
        if (isAdmin && (templateOwnerDomain != null)) {
            response.setDomainId(templateOwnerDomain);
        }

        response.setAccountNames(accountNames);
        response.setObjectName("templatepermission");
        return response;
    }

    @Override
    public AsyncJobResponse queryJobResult(QueryAsyncJobResultCmd cmd) {
        AsyncJobResult result = ApiDBUtils._asyncMgr.queryAsyncJobResult(cmd);
        AsyncJobResponse response = new AsyncJobResponse();
        response.setId(result.getJobId());
        response.setJobStatus(result.getJobStatus());
        response.setJobProcStatus(result.getProcessStatus());
        response.setJobResultCode(result.getResultCode());
        response.setJobResult((ResponseObject) ApiSerializerHelper.fromSerializedString(result.getResult()));

        Object resultObject = result.getResultObject();
        if (resultObject != null) {
            Class<?> clz = resultObject.getClass();
            if (clz.isPrimitive() || clz.getSuperclass() == Number.class || clz == String.class || clz == Date.class) {
                response.setJobResultType("text");
            } else {
                response.setJobResultType("object");
            }
        }

        return response;
    }

    @Override
    public SecurityGroupResponse createSecurityGroupResponseFromIngressRule(List<? extends IngressRule> ingressRules) {
        SecurityGroupResponse response = new SecurityGroupResponse();
        Map<Long, Account> securiytGroupAccounts = new HashMap<Long, Account>();
        Map<Long, SecurityGroup> allowedSecurityGroups = new HashMap<Long, SecurityGroup>();
        Map<Long, Account> allowedSecuriytGroupAccounts = new HashMap<Long, Account>();

        if ((ingressRules != null) && !ingressRules.isEmpty()) {
            SecurityGroup securityGroup = ApiDBUtils.findSecurityGroupById(ingressRules.get(0).getSecurityGroupId());
            response.setId(securityGroup.getId());
            response.setName(securityGroup.getName());
            response.setDescription(securityGroup.getDescription());

            Account account = securiytGroupAccounts.get(securityGroup.getAccountId());

            if (account == null) {
                account = ApiDBUtils.findAccountById(securityGroup.getAccountId());
                securiytGroupAccounts.put(securityGroup.getAccountId(), account);
            }

            response.setAccountName(account.getAccountName());
            response.setDomainId(account.getDomainId());
            response.setDomainName(ApiDBUtils.findDomainById(securityGroup.getDomainId()).getName());

            List<IngressRuleResponse> responses = new ArrayList<IngressRuleResponse>();
            for (IngressRule ingressRule : ingressRules) {
                IngressRuleResponse ingressData = new IngressRuleResponse();

                ingressData.setRuleId(ingressRule.getId());
                ingressData.setProtocol(ingressRule.getProtocol());
                if ("icmp".equalsIgnoreCase(ingressRule.getProtocol())) {
                    ingressData.setIcmpType(ingressRule.getStartPort());
                    ingressData.setIcmpCode(ingressRule.getEndPort());
                } else {
                    ingressData.setStartPort(ingressRule.getStartPort());
                    ingressData.setEndPort(ingressRule.getEndPort());
                }

                Long allowedSecurityGroupId = ingressRule.getAllowedNetworkId();
                if (allowedSecurityGroupId != null) {
                    SecurityGroup allowedSecurityGroup = allowedSecurityGroups.get(allowedSecurityGroupId);
                    if (allowedSecurityGroup == null) {
                        allowedSecurityGroup = ApiDBUtils.findSecurityGroupById(allowedSecurityGroupId);
                        allowedSecurityGroups.put(allowedSecurityGroupId, allowedSecurityGroup);
                    }

                    ingressData.setSecurityGroupName(allowedSecurityGroup.getName());

                    Account allowedAccount = allowedSecuriytGroupAccounts.get(allowedSecurityGroup.getAccountId());
                    if (allowedAccount == null) {
                        allowedAccount = ApiDBUtils.findAccountById(allowedSecurityGroup.getAccountId());
                        allowedSecuriytGroupAccounts.put(allowedAccount.getId(), allowedAccount);
                    }

                    ingressData.setAccountName(allowedAccount.getAccountName());
                } else {
                    ingressData.setCidr(ingressRule.getAllowedSourceIpCidr());
                }

                ingressData.setObjectName("ingressrule");
                responses.add(ingressData);
            }
            response.setIngressRules(responses);
            response.setObjectName("securitygroup");

        }
        return response;
    }

    @Override
    public NetworkOfferingResponse createNetworkOfferingResponse(NetworkOffering offering) {
        NetworkOfferingResponse response = new NetworkOfferingResponse();
        response.setId(offering.getId());
        response.setName(offering.getName());
        response.setDisplayText(offering.getDisplayText());
        response.setTags(offering.getTags());
        response.setTrafficType(offering.getTrafficType().toString());
        response.setMaxconnections(offering.getConcurrentConnections());
        response.setIsDefault(offering.isDefault());
        response.setSpecifyVlan(offering.getSpecifyVlan());
        response.setAvailability(offering.getAvailability().toString());
        response.setNetworkRate(ApiDBUtils.getNetworkRate(offering.getId()));

        if (offering.getGuestType() != null) {
            response.setGuestIpType(offering.getGuestType().toString());
        }

        response.setObjectName("networkoffering");
        return response;
    }

    @Override
    public NetworkResponse createNetworkResponse(Network network) {
        // need to get network profile in order to retrieve dns information from there
        NetworkProfile profile = ApiDBUtils.getNetworkProfile(network.getId());
        NetworkResponse response = new NetworkResponse();
        response.setId(network.getId());
        response.setName(network.getName());
        response.setDisplaytext(network.getDisplayText());
        if (network.getBroadcastDomainType() != null) {
            response.setBroadcastDomainType(network.getBroadcastDomainType().toString());
        }
        if (network.getBroadcastUri() != null) {
            response.setBroadcastUri(network.getBroadcastUri().toString());
        }

        if (network.getTrafficType() != null) {
            response.setTrafficType(network.getTrafficType().name());
        }

        if (network.getGuestType() != null) {
            response.setType(network.getGuestType().name());
        }

        // get start ip and end ip of corresponding vlan
        List<? extends Vlan> vlan = ApiDBUtils.listVlanByNetworkId(network.getId());
        if (vlan != null && !vlan.isEmpty()) {
            Vlan singleVlan = vlan.get(0);
            String ipRange = singleVlan.getIpRange();
            String[] range = ipRange.split("-");
            response.setStartIp(range[0]);
            response.setEndIp(range[1]);
            response.setGateway(singleVlan.getVlanGateway());
            response.setNetmask(singleVlan.getVlanNetmask());
            response.setVlan(singleVlan.getVlanTag());
        }

        response.setZoneId(network.getDataCenterId());

        // populate network offering information
        NetworkOffering networkOffering = ApiDBUtils.findNetworkOfferingById(network.getNetworkOfferingId());
        if (networkOffering != null) {
            response.setNetworkOfferingId(networkOffering.getId());
            response.setNetworkOfferingName(networkOffering.getName());
            response.setNetworkOfferingDisplayText(networkOffering.getDisplayText());
            response.setIsSystem(networkOffering.isSystemOnly());
            response.setNetworkOfferingAvailability(networkOffering.getAvailability().toString());
        }

        response.setIsShared(network.getIsShared());
        response.setIsDefault(network.isDefault());
        response.setState(network.getState().toString());
        response.setRelated(network.getRelated());
        response.setNetworkDomain(network.getNetworkDomain());

        response.setDns1(profile.getDns1());
        response.setDns2(profile.getDns2());

        response.setIsSecurityGroupEnabled(network.isSecurityGroupEnabled());
        response.setTags(network.getTags());

        // populate capability
        Map<Service, Map<Capability, String>> serviceCapabilitiesMap = ApiDBUtils.getNetworkCapabilities(networkOffering.getId(), network.getDataCenterId());
        List<ServiceResponse> serviceResponses = new ArrayList<ServiceResponse>();
        if (serviceCapabilitiesMap != null) {
            for (Service service : serviceCapabilitiesMap.keySet()) {
                ServiceResponse serviceResponse = new ServiceResponse();
                serviceResponse.setName(service.getName());

                // set list of capabilities for the service
                List<CapabilityResponse> capabilityResponses = new ArrayList<CapabilityResponse>();
                Map<Capability, String> serviceCapabilities = serviceCapabilitiesMap.get(service);
                if (serviceCapabilities != null) {
                    for (Capability capability : serviceCapabilities.keySet()) {
                        CapabilityResponse capabilityResponse = new CapabilityResponse();
                        String capabilityValue = serviceCapabilities.get(capability);
                        capabilityResponse.setName(capability.getName());
                        capabilityResponse.setValue(capabilityValue);
                        capabilityResponse.setObjectName("capability");
                        capabilityResponses.add(capabilityResponse);
                    }
                    serviceResponse.setCapabilities(capabilityResponses);
                }

                serviceResponse.setObjectName("service");
                serviceResponses.add(serviceResponse);
            }
        }
        response.setServices(serviceResponses);

        Account account = ApiDBUtils.findAccountById(network.getAccountId());
        if (account != null && !network.getIsShared()) {
            response.setAccountName(account.getAccountName());
            Domain domain = ApiDBUtils.findDomainById(account.getDomainId());
            response.setDomainId(domain.getId());
            response.setDomain(domain.getName());
        }

        Long dedicatedDomainId = ApiDBUtils.getDedicatedNetworkDomain(network.getId());
        if (dedicatedDomainId != null) {
            Domain domain = ApiDBUtils.findDomainById(dedicatedDomainId);
            response.setDomainId(dedicatedDomainId);
            response.setDomain(domain.getName());
        }

        response.setObjectName("network");
        return response;
    }

    @Override
    public Long getSecurityGroupId (String groupName, long accountId) {
        SecurityGroup sg = ApiDBUtils.getSecurityGroup(groupName, accountId);
        if (sg == null) {
            return null;
        } else {
            return sg.getId();
        }
    }

    @Override
    public FirewallResponse createFirewallResponse(FirewallRule fwRule) {
        FirewallResponse response = new FirewallResponse();

        response.setId(fwRule.getId());
        response.setProtocol(fwRule.getProtocol());
        if (fwRule.getSourcePortStart() != null) {
            response.setStartPort(Integer.toString(fwRule.getSourcePortStart()));
        }

        if (fwRule.getSourcePortEnd() != null) {
            response.setEndPort(Integer.toString(fwRule.getSourcePortEnd()));
        }

        List<String> cidrs = ApiDBUtils.findFirewallSourceCidrs(fwRule.getId());
        response.setCidrList(StringUtils.join(cidrs, ","));

        IpAddress ip = ApiDBUtils.findIpAddressById(fwRule.getSourceIpAddressId());
        response.setPublicIpAddressId(ip.getId());
        response.setPublicIpAddress(ip.getAddress().addr());

        FirewallRule.State state = fwRule.getState();
        String stateToSet = state.toString();
        if (state.equals(FirewallRule.State.Revoke)) {
            stateToSet = "Deleting";
        }

        response.setIcmpCode(fwRule.getIcmpCode());
        response.setIcmpType(fwRule.getIcmpType());

        response.setState(stateToSet);
        response.setObjectName("firewallrule");
        return response;
    }
    

    
    public UserVmData newUserVmData(UserVm userVm){
        UserVmData userVmData = new UserVmData();
        userVmData.setId(userVm.getId());
        userVmData.setName(userVm.getHostName());
        userVmData.setCreated(userVm.getCreated());
        userVmData.setGuestOsId(userVm.getGuestOSId());
        userVmData.setHaEnable(userVm.isHaEnabled());
        if (userVm.getState() != null) {
            userVmData.setState(userVm.getState().toString());
        }
        if (userVm.getDisplayName() != null) {
            userVmData.setDisplayName(userVm.getDisplayName());
        } else {
            userVmData.setDisplayName(userVm.getHostName());
        } 
        userVmData.setDomainId(userVm.getDomainId());

        if (userVm.getHypervisorType() != null) {
            userVmData.setHypervisor(userVm.getHypervisorType().toString());
        }

        if (userVm.getPassword() != null) {
            userVmData.setPassword(userVm.getPassword());
        }
        return userVmData;
    }
    
    public UserVmResponse newUserVmResponse(UserVmData userVmData, boolean caller_is_admin){
        UserVmResponse userVmResponse = new UserVmResponse();
        userVmResponse.setHypervisor(userVmData.getHypervisor());
        userVmResponse.setId(userVmData.getId());
        userVmResponse.setName(userVmData.getName());
        userVmResponse.setDisplayName(userVmData.getDisplayName());
        userVmResponse.setIpAddress(userVmData.getIpAddress());
        userVmResponse.setAccountName(userVmData.getAccountName());
        userVmResponse.setDomainId(userVmData.getDomainId());
        userVmResponse.setDomainName(userVmData.getDomainName());
        userVmResponse.setCreated(userVmData.getCreated());
        userVmResponse.setState(userVmData.getState());
        userVmResponse.setHaEnable(userVmData.getHaEnable());
        userVmResponse.setGroupId(userVmData.getGroupId());
        userVmResponse.setGroup(userVmData.getGroup());
        userVmResponse.setZoneId(userVmData.getZoneId());
        userVmResponse.setZoneName(userVmData.getZoneName());
        if (caller_is_admin){
            userVmResponse.setHostId(userVmData.getHostId());
            userVmResponse.setHostName(userVmData.getHostName());
        }
        userVmResponse.setTemplateId(userVmData.getTemplateId());
        userVmResponse.setTemplateName(userVmData.getTemplateName());
        userVmResponse.setTemplateDisplayText(userVmData.getTemplateDisplayText());
        userVmResponse.setPasswordEnabled(userVmData.getPasswordEnabled());
        userVmResponse.setIsoId(userVmData.getIsoId());
        userVmResponse.setIsoName(userVmData.getIsoName());
        userVmResponse.setIsoDisplayText(userVmData.getIsoDisplayText());
        userVmResponse.setServiceOfferingId(userVmData.getServiceOfferingId());
        userVmResponse.setServiceOfferingName(userVmData.getServiceOfferingName());
        userVmResponse.setCpuNumber(userVmData.getCpuNumber());
        userVmResponse.setCpuSpeed(userVmData.getCpuSpeed());
        userVmResponse.setMemory(userVmData.getMemory());
        userVmResponse.setCpuUsed(userVmData.getCpuUsed());
        userVmResponse.setNetworkKbsRead(userVmData.getNetworkKbsRead());
        userVmResponse.setNetworkKbsWrite(userVmData.getNetworkKbsWrite());
        userVmResponse.setGuestOsId(userVmData.getGuestOsId());
        userVmResponse.setRootDeviceId(userVmData.getRootDeviceId());
        userVmResponse.setRootDeviceType(userVmData.getRootDeviceType());
        userVmResponse.setPassword(userVmData.getPassword());
        userVmResponse.setJobId(userVmData.getJobId());
        userVmResponse.setJobStatus(userVmData.getJobStatus());
        userVmResponse.setForVirtualNetwork(userVmData.getForVirtualNetwork());

        Set<SecurityGroupResponse> securityGroupResponse = new HashSet<SecurityGroupResponse>();
        for (SecurityGroupData sgd: userVmData.getSecurityGroupList()){
            SecurityGroupResponse sgr = new SecurityGroupResponse();
            sgr.setId(sgd.getId());
            sgr.setName(sgd.getName());
            sgr.setDescription(sgd.getDescription());
            sgr.setAccountName(sgd.getAccountName());
            sgr.setDomainId(sgd.getDomainId());
            sgr.setDomainName(sgd.getDomainName());
            sgr.setObjectName(sgd.getObjectName());
            securityGroupResponse.add(sgr);
        }
        userVmResponse.setSecurityGroupList(new ArrayList<SecurityGroupResponse>(securityGroupResponse));

        Set<NicResponse> nicResponses = new HashSet<NicResponse>();
        for (NicData nd: userVmData.getNics()){
            NicResponse nr = new NicResponse();
            nr.setId(nd.getId());
            nr.setNetworkid(nd.getNetworkid());
            nr.setNetmask(nd.getNetmask());
            nr.setGateway(nd.getGateway());
            nr.setIpaddress(nd.getIpaddress());
            nr.setIsolationUri(nd.getIsolationUri());
            nr.setBroadcastUri(nd.getBroadcastUri());
            nr.setTrafficType(nd.getTrafficType());
            nr.setType(nd.getType());
            nr.setIsDefault(nd.getIsDefault());
            nr.setMacAddress(nd.getMacAddress());
            nr.setObjectName(nd.getObjectName());
            nicResponses.add(nr);
        }
        userVmResponse.setNics(new ArrayList<NicResponse>(nicResponses));

        return userVmResponse;
    }

    @Override
    public SystemVmInstanceResponse createSystemVmInstanceResponse(VirtualMachine vm){
        SystemVmInstanceResponse vmResponse = new SystemVmInstanceResponse();
        vmResponse.setId(vm.getId());
        vmResponse.setSystemVmType(vm.getType().toString().toLowerCase());
        vmResponse.setName(vm.getHostName());
        if (vm.getHostId() != null) {
            vmResponse.setHostId(vm.getHostId());
        }
        if (vm.getState() != null) {
            vmResponse.setState(vm.getState().toString());
        }
        if (vm.getType() == Type.DomainRouter) {
            VirtualRouter router = (VirtualRouter)vm;
            if(router.getRole() != null){
                vmResponse.setRole(router.getRole().toString());
            }
        }
        vmResponse.setObjectName("systemvminstance");
        return vmResponse;
    }

}
