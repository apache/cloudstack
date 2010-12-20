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
 * aLong with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
import com.cloud.api.response.CapacityResponse;
import com.cloud.api.response.ClusterResponse;
import com.cloud.api.response.ConfigurationResponse;
import com.cloud.api.response.CreateCmdResponse;
import com.cloud.api.response.DiskOfferingResponse;
import com.cloud.api.response.DomainResponse;
import com.cloud.api.response.DomainRouterResponse;
import com.cloud.api.response.EventResponse;
import com.cloud.api.response.ExtractResponse;
import com.cloud.api.response.FirewallRuleResponse;
import com.cloud.api.response.HostResponse;
import com.cloud.api.response.IPAddressResponse;
import com.cloud.api.response.IngressRuleResponse;
import com.cloud.api.response.InstanceGroupResponse;
import com.cloud.api.response.IpForwardingRuleResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.LoadBalancerResponse;
import com.cloud.api.response.NetworkOfferingResponse;
import com.cloud.api.response.NetworkResponse;
import com.cloud.api.response.NicResponse;
import com.cloud.api.response.PodResponse;
import com.cloud.api.response.PreallocatedLunResponse;
import com.cloud.api.response.RemoteAccessVpnResponse;
import com.cloud.api.response.ResourceLimitResponse;
import com.cloud.api.response.SecurityGroupResponse;
import com.cloud.api.response.ServiceOfferingResponse;
import com.cloud.api.response.SnapshotPolicyResponse;
import com.cloud.api.response.SnapshotResponse;
import com.cloud.api.response.StoragePoolResponse;
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
import com.cloud.async.executor.IngressRuleResultObject;
import com.cloud.async.executor.SecurityGroupResultObject;
import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityVO;
import com.cloud.configuration.Configuration;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.ResourceLimit;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
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
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.VpnUser;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.security.IngressRule;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupRules;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.GuestIpType;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.server.Criteria;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOS;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Snapshot.Type;
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
import com.cloud.storage.preallocatedlun.PreallocatedLunVO;
import com.cloud.storage.snapshot.SnapshotPolicy;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.test.PodZoneConfig;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.UserContext;
import com.cloud.user.UserStatisticsVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.InstanceGroup;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.Nic;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.State;
import com.cloud.vm.SystemVm;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmStats;

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
        userResponse.setState(user.getState());
        userResponse.setTimezone(user.getTimezone());
        userResponse.setUsername(user.getUsername());
        userResponse.setApiKey(user.getApiKey());
        userResponse.setSecretKey(user.getSecretKey());
        userResponse.setObjectName("user");

        return userResponse;
    }
    //this method is used for response generation via createAccount (which creates an account + user)
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
        accountResponse.setState(account.getState());

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

        return accountResponse;
    }

    @Override
    public DomainResponse createDomainResponse(Domain domain) {
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
        diskOfferingResponse.setDiskSize(offering.getDiskSize());
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
    public ServiceOfferingResponse createServiceOfferingResponse(ServiceOffering offering) {
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
        offeringResponse.setUseVirtualNetwork(offering.getGuestIpType().equals(GuestIpType.Virtual));
        offeringResponse.setTags(offering.getTags());
        if(offering.getDomainId() != null){
        	offeringResponse.setDomain(ApiDBUtils.findDomainById(offering.getDomainId()).getName());
            offeringResponse.setDomainId(offering.getDomainId());
        }
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
        String snapshotTypeStr = Type.values()[snapshot.getSnapshotType()].name();
        snapshotResponse.setSnapshotType(snapshotTypeStr);
        snapshotResponse.setVolumeId(snapshot.getVolumeId());
        if( volume != null ) {
            snapshotResponse.setVolumeName(volume.getName());
            snapshotResponse.setVolumeType(volume.getVolumeType().name());
        }
        snapshotResponse.setCreated(snapshot.getCreated());
        snapshotResponse.setName(snapshot.getName());
        snapshotResponse.setIntervalType(ApiDBUtils.getSnapshotIntervalTypes(snapshot.getId()));
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
            hostResponse.setAverageLoad(Double.doubleToLongBits(hostStats.getAverageLoad()));
            hostResponse.setNetworkKbsRead(Double.doubleToLongBits(hostStats.getNetworkReadKBs()));
            hostResponse.setNetworkKbsWrite(Double.doubleToLongBits(hostStats.getNetworkWriteKBs()));
        }

        if (host.getType() == Host.Type.Routing) {
            hostResponse.setMemoryTotal(host.getTotalMemory());

            // calculate memory allocated by systemVM and userVm
            Long mem = ApiDBUtils.getMemoryUsagebyHost(host.getId());
            hostResponse.setMemoryAllocated(mem);
            hostResponse.setMemoryUsed(mem);
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
            vlanResponse.setPodName(pod.getName());
        }

        vlanResponse.setGateway(vlan.getVlanGateway());
        vlanResponse.setNetmask(vlan.getVlanNetmask());

        //get start ip and end ip of corresponding vlan
        String ipRange = vlan.getIpRange();
        String[] range = ipRange.split("-"); 
        vlanResponse.setStartIp(range[0]);
        vlanResponse.setEndIp(range[1]);
        
        Long networkId = vlan.getNetworkId();
        if (networkId != null) {
            vlanResponse.setNetworkId(vlan.getNetworkId());
            Network network = ApiDBUtils.findNetworkById(networkId);
            if (network != null) {
              Long accountId = network.getAccountId();
              //Set account information
                if (accountId != null) {
                    Account account = ApiDBUtils.findAccountById(accountId);
                    vlanResponse.setAccountName(account.getAccountName());
                    vlanResponse.setDomainId(account.getDomainId());
                    vlanResponse.setDomainName(ApiDBUtils.findDomainById(account.getDomainId()).getName());
                }
            }
        }
        
        vlanResponse.setObjectName("vlan");
        return vlanResponse;
    }

    @Override
    public IPAddressResponse createIPAddressResponse(IpAddress ipAddress) {
        VlanVO vlan = ApiDBUtils.findVlanById(ipAddress.getVlanId());
        boolean forVirtualNetworks = vlan.getVlanType().equals(VlanType.VirtualNetwork);

        IPAddressResponse ipResponse = new IPAddressResponse();
        ipResponse.setIpAddress(ipAddress.getAddress());
        if (ipAddress.getAllocatedTime() != null) {
            ipResponse.setAllocated(ipAddress.getAllocatedTime());
        }
        ipResponse.setZoneId(ipAddress.getDataCenterId());
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
        
        ipResponse.setAssociatedNetworkId(ipAddress.getAssociatedNetworkId());

        // show this info to admin only
        Account account = UserContext.current().getAccount();
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
        lbResponse.setPublicIp(loadBalancer.getSourceIpAddress().toString());
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
        podResponse.setObjectName("pod");
        return podResponse;
    }

    @Override
    public ZoneResponse createZoneResponse(DataCenter dataCenter) {
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
        zoneResponse.setType(dataCenter.getNetworkType().toString());
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
        if (instanceId != null) {
            VMInstanceVO vm = ApiDBUtils.findVMInstanceById(instanceId);
            volResponse.setVirtualMachineId(vm.getId());
            volResponse.setVirtualMachineName(vm.getName());
            volResponse.setVirtualMachineDisplayName(vm.getName());
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
            if (volume.getPoolId() == null) {
                if (volume.getState() == Volume.State.Allocated) {
                    /* set it as shared, so the UI can attach it to VM */
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
        if (volume.getVolumeType().equals(Volume.VolumeType.ROOT)) {
            volResponse.setServiceOfferingId(volume.getDiskOfferingId());
        } else {
            volResponse.setDiskOfferingId(volume.getDiskOfferingId());
        }

        DiskOfferingVO diskOffering = ApiDBUtils.findDiskOfferingById(volume.getDiskOfferingId());
        if (volume.getVolumeType().equals(Volume.VolumeType.ROOT)) {
            volResponse.setServiceOfferingName(diskOffering.getName());
            volResponse.setServiceOfferingDisplayText(diskOffering.getDisplayText());
        } else {
            volResponse.setDiskOfferingName(diskOffering.getName());
            volResponse.setDiskOfferingDisplayText(diskOffering.getDisplayText());
        }

        Long poolId = volume.getPoolId();
        String poolName = (poolId == null) ? "none" : ApiDBUtils.findStoragePoolById(poolId).getName();
        volResponse.setStoragePoolName(poolName);
        volResponse.setSourceId(volume.getSourceId());
        if (volume.getSourceType() != null) {
            volResponse.setSourceType(volume.getSourceType().toString());
        }
        volResponse.setHypervisor(ApiDBUtils.getVolumeHyperType(volume.getId()).toString());
        volResponse.setAttached(volume.getAttached());
        volResponse.setDestroyed(volume.getDestroyed());
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
    public PreallocatedLunResponse createPreallocatedLunResponse(Object result) {
        PreallocatedLunVO preallocatedLun = (PreallocatedLunVO)result;
        PreallocatedLunResponse preallocLunResponse = new PreallocatedLunResponse();
        preallocLunResponse.setId(preallocatedLun.getId());
        preallocLunResponse.setVolumeId(preallocatedLun.getVolumeId());
        preallocLunResponse.setZoneId(preallocatedLun.getDataCenterId());
        preallocLunResponse.setLun(preallocatedLun.getLun());
        preallocLunResponse.setPortal(preallocatedLun.getPortal());
        preallocLunResponse.setSize(preallocatedLun.getSize());
        preallocLunResponse.setTaken(preallocatedLun.getTaken());
        preallocLunResponse.setTargetIqn(preallocatedLun.getTargetIqn());
        preallocLunResponse.setObjectName("preallocatedlun");
        return preallocLunResponse;
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
            poolResponse.setPodName(ApiDBUtils.findPodById(pool.getPodId()).getName());
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
        HostPodVO pod = ApiDBUtils.findPodById(cluster.getPodId());
        clusterResponse.setPodName(pod.getName());
        DataCenterVO zone = ApiDBUtils.findZoneById(cluster.getDataCenterId());
        clusterResponse.setZoneName(zone.getName());
        clusterResponse.setObjectName("cluster");
        return clusterResponse;
    }

    @Override
    public FirewallRuleResponse createFirewallRuleResponse(PortForwardingRule fwRule) {
        FirewallRuleResponse response = new FirewallRuleResponse();
        response.setId(fwRule.getId());
        response.setPrivatePort(Integer.toString(fwRule.getDestinationPortStart()));
        response.setProtocol(fwRule.getProtocol());
        response.setPublicPort(Integer.toString(fwRule.getSourcePortStart()));
        response.setPublicIpAddress(fwRule.getSourceIpAddress().toString());
        if (fwRule.getSourceIpAddress() != null && fwRule.getDestinationIpAddress() != null) {
            UserVm vm = ApiDBUtils.findUserVmById(fwRule.getVirtualMachineId());
            if(vm != null){
            	response.setVirtualMachineId(vm.getId());
            	response.setVirtualMachineName(vm.getName());
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
    public IpForwardingRuleResponse createIpForwardingRuleResponse(PortForwardingRule fwRule) {
        IpForwardingRuleResponse response = new IpForwardingRuleResponse();
        response.setId(fwRule.getId());
        response.setProtocol(fwRule.getProtocol());
        response.setPublicIpAddress(fwRule.getSourceIpAddress().addr());
        if (fwRule.getSourceIpAddress() != null && fwRule.getDestinationIpAddress() != null) {
            UserVm vm = ApiDBUtils.findUserVmById(fwRule.getVirtualMachineId());
            if(vm != null){//vm might be destroyed
            	response.setVirtualMachineId(vm.getId());
            	response.setVirtualMachineName(vm.getName());
            	response.setVirtualMachineDisplayName(vm.getDisplayName());
            }
        }
        FirewallRule.State state = fwRule.getState();
        String stateToSet = state.toString();
        if (state.equals(FirewallRule.State.Revoke)) {
            stateToSet = "Deleting";
        }
        response.setState(stateToSet);
        response.setObjectName("ipforwardingrule");
        return response;
    }

    @Override
    public UserVmResponse createUserVmResponse(UserVm userVm) {
        UserVmResponse userVmResponse = new UserVmResponse();
        Account acct = ApiDBUtils.findAccountById(Long.valueOf(userVm.getAccountId()));
        // FIXME - this check should be done in searchForUserVm method in
        // ManagementServerImpl;
        // otherwise the number of vms returned is not going to match pageSize
        // request parameter
        if ((acct != null) && (acct.getRemoved() == null)) {
            userVmResponse.setAccountName(acct.getAccountName());
            userVmResponse.setDomainId(acct.getDomainId());
            userVmResponse.setDomainName(ApiDBUtils.findDomainById(acct.getDomainId()).getName());
        } else {
            return null; // the account has been deleted, skip this VM in the
                         // response
        }

        userVmResponse.setId(userVm.getId());
        userVmResponse.setName(userVm.getName());
        userVmResponse.setCreated(userVm.getCreated());

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

        Account account = UserContext.current().getAccount();
        // if user is an admin, display host id
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

        if (userVm.getPassword() != null) {
            userVmResponse.setPassword(userVm.getPassword());
        }

        // ISO Info
        if (userVm.getIsoId() != null) {
            VMTemplateVO iso = ApiDBUtils.findTemplateById(userVm.getIsoId());
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
            String rootDeviceType = "Not created";
            if (rootVolume.getPoolId() != null) {
                StoragePoolVO storagePool = ApiDBUtils.findStoragePoolById(rootVolume.getPoolId());
                rootDeviceType = storagePool.getPoolType().toString();
            }
            userVmResponse.setRootDeviceType(rootDeviceType);
        }

        // stats calculation
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        String cpuUsed = null;
        VmStats vmStats = ApiDBUtils.getVmStatistics(userVm.getId());
        if (vmStats != null) {
            float cpuUtil = (float) vmStats.getCPUUtilization();
            cpuUsed = decimalFormat.format(cpuUtil) + "%";
            userVmResponse.setCpuUsed(cpuUsed);

            Long networkKbRead = Double.doubleToLongBits(vmStats.getNetworkReadKBs());
            userVmResponse.setNetworkKbsRead(networkKbRead);

            Long networkKbWrite = Double.doubleToLongBits(vmStats.getNetworkWriteKBs());
            userVmResponse.setNetworkKbsWrite(networkKbWrite);
        }

        userVmResponse.setGuestOsId(userVm.getGuestOSId());
        // network groups
        userVmResponse.setSecurityGroupList(ApiDBUtils.getNetworkGroupsNamesForVm(userVm.getId()));

        List<? extends Nic> nics = ApiDBUtils.getNics(userVm);
        List<NicResponse> nicResponses = new ArrayList<NicResponse>();
        for (Nic singleNic : nics) {
            NicResponse nicResponse = new NicResponse();
            nicResponse.setId(singleNic.getId());
            nicResponse.setIpaddress(singleNic.getIp4Address());
            nicResponse.setGateway(singleNic.getGateway());
            nicResponse.setNetmask(singleNic.getNetmask());
            nicResponse.setNetworkid(singleNic.getNetworkId());
            if (acct.getType() == Account.ACCOUNT_TYPE_ADMIN) {
                if (singleNic.getBroadcastUri() != null) {
                    nicResponse.setBroadcastUri(singleNic.getBroadcastUri().toString());
                }
                if (singleNic.getIsolationUri() != null) {
                    nicResponse.setIsolationUri(singleNic.getIsolationUri().toString());
                }
            }  
            //Set traffic type
            Network network = ApiDBUtils.findNetworkById(singleNic.getNetworkId());
            nicResponse.setTrafficType(network.getTrafficType().toString());
            
            //Set type
            NetworkOffering networkOffering = ApiDBUtils.findNetworkOfferingById(network.getNetworkOfferingId());
            if (networkOffering.getGuestIpType() != null) {
                nicResponse.setType(networkOffering.getGuestIpType().toString());
            }
            
            nicResponse.setObjectName("nic");
            
            nicResponses.add(nicResponse);
        }
        userVmResponse.setNics(nicResponses);
        userVmResponse.setObjectName("virtualmachine");
        return userVmResponse;
    }

    @Override
    public DomainRouterResponse createDomainRouterResponse(VirtualRouter router) {
        DomainRouterResponse routerResponse = new DomainRouterResponse();
        routerResponse.setId(router.getId());
        routerResponse.setZoneId(router.getDataCenterId());
        routerResponse.setName(router.getName());
        routerResponse.setPodId(router.getPodId());
        routerResponse.setTemplateId(router.getTemplateId());
        routerResponse.setCreated(router.getCreated());
        routerResponse.setState(router.getState());
        routerResponse.setNetworkDomain(router.getDomain());

        if (router.getHostId() != null) {
            routerResponse.setHostId(router.getHostId());
            routerResponse.setHostName(ApiDBUtils.findHostById(router.getHostId()).getName());
        }

        Account accountTemp = ApiDBUtils.findAccountById(router.getAccountId());
        if (accountTemp != null) {
            routerResponse.setAccountName(accountTemp.getAccountName());
            routerResponse.setDomainId(accountTemp.getDomainId());
            routerResponse.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
        }

        List<? extends Nic> nics = ApiDBUtils.getNics(router);
        for (Nic singleNic : nics) {
           Network network = ApiDBUtils.findNetworkById(singleNic.getNetworkId());
           if (network != null) {
               if (network.getTrafficType() == TrafficType.Public) {
                   routerResponse.setPublicIp(singleNic.getIp4Address());
                   routerResponse.setPublicMacAddress(singleNic.getMacAddress());
                   routerResponse.setPublicNetmask(singleNic.getNetmask());
               } else if (network.getTrafficType() == TrafficType.Control) {
                   routerResponse.setPrivateIp(singleNic.getIp4Address());
                   routerResponse.setPrivateMacAddress(singleNic.getMacAddress());
                   routerResponse.setPrivateNetmask(singleNic.getNetmask());
               } else if (network.getTrafficType() == TrafficType.Guest) {
                   routerResponse.setGuestIpAddress(singleNic.getIp4Address());
                   routerResponse.setGuestMacAddress(singleNic.getMacAddress());
                   routerResponse.setGuestNetmask(singleNic.getNetmask());
               }
           }
        }
        DataCenter zone = ApiDBUtils.findZoneById(router.getDataCenterId());
        if (zone != null) {
            routerResponse.setZoneName(zone.getName());
            routerResponse.setDns1(zone.getDns1());
            routerResponse.setDns2(zone.getDns2());
        }

        routerResponse.setObjectName("domainrouter");
        return routerResponse;
    }

    @Override
    public SystemVmResponse createSystemVmResponse(VirtualMachine systemVM) {
        SystemVmResponse vmResponse = new SystemVmResponse();
        if (systemVM instanceof SystemVm) {
            SystemVm vm = (SystemVm) systemVM;

            vmResponse.setId(vm.getId());
            vmResponse.setSystemVmType(vm.getType().toString().toLowerCase());
            vmResponse.setZoneId(vm.getDataCenterId());

            vmResponse.setNetworkDomain(vm.getDomain());
            vmResponse.setName(vm.getName());
            vmResponse.setPodId(vm.getPodId());
            vmResponse.setTemplateId(vm.getTemplateId());
            vmResponse.setCreated(vm.getCreated());

            if (vm.getHostId() != null) {
                vmResponse.setHostId(vm.getHostId());
                vmResponse.setHostName(ApiDBUtils.findHostById(vm.getHostId()).getName());
            }

            if (vm.getState() != null) {
                vmResponse.setState(vm.getState().toString());
            }

            String instanceType = "console_proxy";
            if (systemVM instanceof SecondaryStorageVmVO) {
                instanceType = "sec_storage_vm"; // FIXME: this should be a
                                                 // constant so that the async
                                                 // jobs get updated with the
                                                 // correct instance type, they
                                                 // are using
                                                 // different instance types at
                                                 // the moment
            }

            // for console proxies, add the active sessions
            if (systemVM instanceof ConsoleProxyVO) {
                ConsoleProxyVO proxy = (ConsoleProxyVO) systemVM;
                vmResponse.setActiveViewerSessions(proxy.getActiveSession());
            }

            DataCenter zone = ApiDBUtils.findZoneById(vm.getDataCenterId());
            if (zone != null) {
                vmResponse.setZoneName(zone.getName());
                vmResponse.setDns1(zone.getDns1());
                vmResponse.setDns2(zone.getDns2());
            }

            List<? extends Nic> nics = ApiDBUtils.getNics(systemVM);
            for (Nic singleNic : nics) {
               Network network = ApiDBUtils.findNetworkById(singleNic.getNetworkId());
               if (network != null) {
                   if (network.getTrafficType() == TrafficType.Public) {
                       vmResponse.setPublicIp(singleNic.getIp4Address());
                       vmResponse.setPublicMacAddress(singleNic.getMacAddress());
                       vmResponse.setPublicNetmask(singleNic.getNetmask());
                   } else if (network.getTrafficType() == TrafficType.Control) {
                       vmResponse.setPrivateIp(singleNic.getIp4Address());
                       vmResponse.setPrivateMacAddress(singleNic.getMacAddress());
                       vmResponse.setPrivateNetmask(singleNic.getNetmask());
                   }
               }
            }
        }
        vmResponse.setObjectName("systemvm");
        return vmResponse;
    }
    
    @Override
    public void synchronizeCommand(Object job, String syncObjType, Long syncObjId) {
        ApiDBUtils.synchronizeCommand(job, syncObjType, syncObjId);
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
        vpnResponse.setAccountName(vpnUser.getAccountName());
        
        Account accountTemp = ApiDBUtils.findAccountById(vpnUser.getAccountId());
        if (accountTemp != null) {
            vpnResponse.setDomainId(accountTemp.getDomainId());
            vpnResponse.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
        }

        vpnResponse.setObjectName("vpnuser");
        return vpnResponse;
    }
    
    @Override
    public RemoteAccessVpnResponse createRemoteAccessVpnResponse(RemoteAccessVpn vpn) {
        RemoteAccessVpnResponse vpnResponse = new RemoteAccessVpnResponse();
        vpnResponse.setId(vpn.getId());
        vpnResponse.setPublicIp(vpn.getVpnServerAddress());
        vpnResponse.setIpRange(vpn.getIpRange());
        vpnResponse.setPresharedKey(vpn.getIpsecPresharedKey());
        vpnResponse.setAccountName(vpn.getAccountName());
        
        Account accountTemp = ApiDBUtils.findAccountById(vpn.getAccountId());
        if (accountTemp != null) {
            vpnResponse.setDomainId(accountTemp.getDomainId());
            vpnResponse.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
        }

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
        
        if(result.getFormat() == ImageFormat.ISO){ // Templates are always bootable
        	response.setBootable(result.isBootable());
        }else{
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
    public void createTemplateResponse(List<TemplateResponse> responses, Pair<Long,Long> templateZonePair, boolean isAdmin, Account account) {
        List<VMTemplateHostVO> templateHostRefsForTemplate = ApiDBUtils.listTemplateHostBy(templateZonePair.first(), templateZonePair.second());
        VMTemplateVO template = ApiDBUtils.findTemplateById(templateZonePair.first());
        
        for (VMTemplateHostVO templateHostRef : templateHostRefsForTemplate) {

            TemplateResponse templateResponse = new TemplateResponse();
            templateResponse.setId(template.getId());
            templateResponse.setName(template.getName());
            templateResponse.setDisplayText(template.getDisplayText());
            templateResponse.setPublic(template.isPublicTemplate());
            templateResponse.setCreated(templateHostRef.getCreated());

            templateResponse.setReady(templateHostRef.getDownloadState()==Status.DOWNLOADED);
            templateResponse.setFeatured(template.isFeatured());
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
            
            HostVO host = ApiDBUtils.findHostById(templateHostRef.getHostId());
            DataCenterVO datacenter = ApiDBUtils.findZoneById(host.getDataCenterId());
            
            // Add the zone ID
            templateResponse.setZoneId(host.getDataCenterId());
            templateResponse.setZoneName(datacenter.getName());
            
            // If the user is an admin, add the template download status
            if (isAdmin || account.getId() == template.getAccountId()) {
                // add download status
                if (templateHostRef.getDownloadState()!=Status.DOWNLOADED) {
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
            
            templateResponse.setObjectName("template");
            responses.add(templateResponse);
        }
    }
    
    @Override
    public ListResponse<TemplateResponse> createTemplateResponse2(VirtualMachineTemplate template, Long zoneId) {
        ListResponse<TemplateResponse> response = new ListResponse<TemplateResponse>();
        List<TemplateResponse> responses = new ArrayList<TemplateResponse>();
        List<DataCenterVO> zones = null;

        if ((zoneId != null) && (zoneId != -1)) {
            zones = new ArrayList<DataCenterVO>();
            zones.add(ApiDBUtils.findZoneById(zoneId));
        } else {
            zones = ApiDBUtils.listZones();
        }

        for (DataCenterVO zone : zones) {
            TemplateResponse templateResponse = new TemplateResponse();
            templateResponse.setId(template.getId());
            templateResponse.setName(template.getName());
            templateResponse.setDisplayText(template.getDisplayText());
            templateResponse.setPublic(template.isPublicTemplate());
            templateResponse.setExtractable(template.isExtractable());
            templateResponse.setCrossZones(template.isCrossZones());

            VMTemplateHostVO isoHostRef = ApiDBUtils.findTemplateHostRef(template.getId(), zone.getId());
            if (isoHostRef != null) {
                templateResponse.setCreated(isoHostRef.getCreated());
                templateResponse.setReady(isoHostRef.getDownloadState() == Status.DOWNLOADED);
            }

            templateResponse.setFeatured(template.isFeatured());
            templateResponse.setPasswordEnabled(template.getEnablePassword());
            templateResponse.setFormat(template.getFormat());
            templateResponse.setStatus("Processing");

            GuestOS os = ApiDBUtils.findGuestOSById(template.getGuestOSId());
            if (os != null) {
                templateResponse.setOsTypeId(os.getId());
                templateResponse.setOsTypeName(os.getDisplayName());
            } else {
                templateResponse.setOsTypeId(-1L);
                templateResponse.setOsTypeName("");
            }
              
            Account owner = ApiDBUtils.findAccountById(template.getAccountId());
            if (owner != null) {
                templateResponse.setAccountId(owner.getId());
                templateResponse.setAccount(owner.getAccountName());
                templateResponse.setDomainId(owner.getDomainId());
                templateResponse.setDomainName(ApiDBUtils.findDomainById(owner.getDomainId()).getName());
            }

            templateResponse.setZoneId(zone.getId());
            templateResponse.setZoneName(zone.getName());
            templateResponse.setHypervisor(template.getHypervisorType().toString());
            templateResponse.setObjectName("template");

            responses.add(templateResponse);
        }
        response.setResponses(responses);
        return response;
    }
    
    @Override
    public ListResponse<TemplateResponse> createIsoResponses(VirtualMachineTemplate template, Long zoneId) {
        ListResponse<TemplateResponse> response = new ListResponse<TemplateResponse>();
        List<TemplateResponse> responses = new ArrayList<TemplateResponse>();
        List<DataCenterVO> zones = null;

        if ((zoneId != null) && (zoneId != -1)) {
            zones = new ArrayList<DataCenterVO>();
            zones.add(ApiDBUtils.findZoneById(zoneId));
        } else {
            zones = ApiDBUtils.listZones();   
        }

        for (DataCenterVO zone : zones) {
            TemplateResponse templateResponse = new TemplateResponse();
            templateResponse.setId(template.getId());
            templateResponse.setName(template.getName());
            templateResponse.setDisplayText(template.getDisplayText());
            templateResponse.setPublic(template.isPublicTemplate());

            VMTemplateHostVO isoHostRef = ApiDBUtils.findTemplateHostRef(template.getId(), zone.getId());
            if (isoHostRef != null) {
                templateResponse.setCreated(isoHostRef.getCreated());
                templateResponse.setReady(isoHostRef.getDownloadState() == Status.DOWNLOADED);
            }

            templateResponse.setFeatured(template.isFeatured());
            templateResponse.setBootable(template.isBootable());
            templateResponse.setOsTypeId(template.getGuestOSId());
            templateResponse.setOsTypeName(ApiDBUtils.findGuestOSById(template.getGuestOSId()).getDisplayName());
              
            Account owner = ApiDBUtils.findAccountById(template.getAccountId());
            if (owner != null) {
                templateResponse.setAccountId(owner.getId());
                templateResponse.setAccount(owner.getAccountName());
                templateResponse.setDomainId(owner.getDomainId());
                templateResponse.setDomainName(ApiDBUtils.findDomainById(owner.getDomainId()).getName());
            }

            templateResponse.setZoneId(zone.getId());
            templateResponse.setZoneName(zone.getName());
            templateResponse.setObjectName("iso");

            responses.add(templateResponse);
        }
        response.setResponses(responses);
        return response;
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
        response.setAccountName(group.getAccountName());
        response.setDescription(group.getDescription());
        response.setDomainId(group.getDomainId());
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
        //FIX ME - Need to set the url once the gson jar is upgraded since it is throwing an error right now.
        //response.setUrl(uploadInfo.getUploadUrl());         
        response.setUrl(uploadInfo.getUploadUrl().replaceAll("/", "%2F"));
        return response;
        
    }

    @Override
    public TemplateResponse createTemplateResponse(VirtualMachineTemplate template, Long destZoneId) {
        TemplateResponse templateResponse = new TemplateResponse();
        if (template != null) {
            templateResponse.setId(template.getId());
            templateResponse.setName(template.getName());
            templateResponse.setDisplayText(template.getDisplayText());
            templateResponse.setPublic(template.isPublicTemplate());
            templateResponse.setBootable(template.isBootable());
            templateResponse.setFeatured(template.isFeatured());
            templateResponse.setCrossZones(template.isCrossZones());
            templateResponse.setCreated(template.getCreated());
            templateResponse.setFormat(template.getFormat());
            templateResponse.setPasswordEnabled(template.getEnablePassword());
            templateResponse.setZoneId(destZoneId);
            templateResponse.setZoneName(ApiDBUtils.findZoneById(destZoneId).getName());
             
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
            
            //set status 
            Account account = UserContext.current().getAccount();
            boolean isAdmin = false;
            if ((account == null) || (account.getType() == Account.ACCOUNT_TYPE_ADMIN) || (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN)) {
                isAdmin = true;
            }
            
            //Return download status for admin users
            VMTemplateHostVO templateHostRef = ApiDBUtils.findTemplateHostRef(template.getId(), destZoneId);
            
            if (isAdmin || template.getAccountId() == account.getId()) {
                if (templateHostRef.getDownloadState()!=Status.DOWNLOADED) {
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
            
            templateResponse.setReady(templateHostRef != null && templateHostRef.getDownloadState() == VMTemplateHostVO.Status.DOWNLOADED);
            
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to copy template");
        }
        
        templateResponse.setObjectName("template");
        return templateResponse;
    }
    
    @Override
    public TemplateResponse createIsoResponse3(VirtualMachineTemplate iso, Long destZoneId) {
        TemplateResponse isoResponse = new TemplateResponse();
        if (iso != null) {
            isoResponse.setId(iso.getId());
            isoResponse.setName(iso.getName());
            isoResponse.setDisplayText(iso.getDisplayText());
            isoResponse.setPublic(iso.isPublicTemplate());
            isoResponse.setBootable(iso.isBootable());
            isoResponse.setFeatured(iso.isFeatured());
            isoResponse.setCrossZones(iso.isCrossZones());
            isoResponse.setCreated(iso.getCreated());
            isoResponse.setZoneId(destZoneId);
            isoResponse.setZoneName(ApiDBUtils.findZoneById(destZoneId).getName());
             
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
                isoResponse.setDomainName(ApiDBUtils.findDomainById(owner.getDomainId()).getName());
            }
            
            //set status 
            Account account = UserContext.current().getAccount();
            boolean isAdmin = false;
            if ((account == null) || (account.getType() == Account.ACCOUNT_TYPE_ADMIN) || (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN)) {
                isAdmin = true;
            }
            
            //Return download status for admin users
            VMTemplateHostVO templateHostRef = ApiDBUtils.findTemplateHostRef(iso.getId(), destZoneId);
            
            if (isAdmin || iso.getAccountId() == account.getId()) {
                if (templateHostRef.getDownloadState()!=Status.DOWNLOADED) {
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
                    isoResponse.setStatus(templateStatus);
                } else if (templateHostRef.getDownloadState() == VMTemplateHostVO.Status.DOWNLOADED) {
                    isoResponse.setStatus("Download Complete");
                } else {
                    isoResponse.setStatus("Successfully Installed");
                }
            }
            
            isoResponse.setReady(templateHostRef.getDownloadState() == VMTemplateHostVO.Status.DOWNLOADED);
            
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to copy iso");
        }

        isoResponse.setObjectName("iso");
        return isoResponse;
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
        jobResponse.setJobInstanceId(job.getInstanceId());
        jobResponse.setJobInstanceType(job.getInstanceType().toString());
        jobResponse.setJobProcStatus(job.getProcessStatus());
        jobResponse.setJobResult((ResponseObject)ApiSerializerHelper.fromSerializedString(job.getResult()));
        jobResponse.setJobResultCode(job.getResultCode());
        jobResponse.setJobStatus(job.getStatus());
        jobResponse.setUserId(job.getUserId());

        jobResponse.setObjectName("asyncjobs");
        return jobResponse;
    }
    
    @Override
    public TemplateResponse createTemplateResponse(VirtualMachineTemplate template, Long snapshotId, Long volumeId) {
        TemplateResponse response = new TemplateResponse();
        response.setId(template.getId());
        response.setName(template.getName());
        response.setDisplayText(template.getDisplayText());
        response.setPublic(template.isPublicTemplate());
        response.setPasswordEnabled(template.getEnablePassword());
        response.setCrossZones(template.isCrossZones());

        VolumeVO volume = null;
        if (snapshotId != null) {
            Snapshot snapshot = ApiDBUtils.findSnapshotById(snapshotId);
            volume = findVolumeById(snapshot.getVolumeId());
        } else {
            volume = findVolumeById(volumeId);
        }

        VMTemplateHostVO templateHostRef = ApiDBUtils.findTemplateHostRef(template.getId(), volume.getDataCenterId());
        response.setCreated(templateHostRef.getCreated());
        response.setReady(templateHostRef != null && templateHostRef.getDownloadState() == Status.DOWNLOADED);

        GuestOS os = ApiDBUtils.findGuestOSById(template.getGuestOSId());
        if (os != null) {
            response.setOsTypeId(os.getId());
            response.setOsTypeName(os.getDisplayName());
        } else {
            response.setOsTypeId(-1L);
            response.setOsTypeName("");
        }

        Account owner = ApiDBUtils.findAccountById(template.getAccountId());
        if (owner != null) {
            response.setAccount(owner.getAccountName());
            response.setDomainId(owner.getDomainId());
            response.setDomainName(ApiDBUtils.findDomainById(owner.getDomainId()).getName());
        }

        DataCenter zone = ApiDBUtils.findZoneById(volume.getDataCenterId());
        if (zone != null) {
            response.setZoneId(zone.getId());
            response.setZoneName(zone.getName());
        }

        response.setObjectName("template");
        return response;

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
    
    @Override
    public ListResponse<TemplateResponse> createIsoResponse(Set<Pair<Long,Long>> isoZonePairSet, boolean isAdmin, Account account)  {        

        ListResponse<TemplateResponse> response = new ListResponse<TemplateResponse>();
        List<TemplateResponse> isoResponses = new ArrayList<TemplateResponse>();
        
        for (Pair<Long,Long> isoZonePair : isoZonePairSet) {
        	VMTemplateVO iso = ApiDBUtils.findTemplateById(isoZonePair.first());
            if ( iso.getTemplateType() == TemplateType.PERHOST ) {
                TemplateResponse isoResponse = new TemplateResponse();
                isoResponse.setId(iso.getId());
                isoResponse.setName(iso.getName());
                isoResponse.setDisplayText(iso.getDisplayText());
                isoResponse.setPublic(iso.isPublicTemplate());
                isoResponse.setReady(true);
                isoResponse.setBootable(iso.isBootable());
                isoResponse.setFeatured(iso.isFeatured());
                isoResponse.setCrossZones(iso.isCrossZones());
                isoResponse.setPublic(iso.isPublicTemplate());
                isoResponse.setObjectName("iso");
                isoResponses.add(isoResponse);
                response.setResponses(isoResponses);
                continue;
            }
           
            List<VMTemplateHostVO> isoHosts = ApiDBUtils.listTemplateHostBy(iso.getId(), isoZonePair.second());
            for (VMTemplateHostVO isoHost : isoHosts) {                
                TemplateResponse isoResponse = new TemplateResponse();
                isoResponse.setId(iso.getId());
                isoResponse.setName(iso.getName());
                isoResponse.setDisplayText(iso.getDisplayText());
                isoResponse.setPublic(iso.isPublicTemplate());
                isoResponse.setCreated(isoHost.getCreated());
                isoResponse.setReady(isoHost.getDownloadState() == Status.DOWNLOADED);
                isoResponse.setBootable(iso.isBootable());
                isoResponse.setFeatured(iso.isFeatured());
                isoResponse.setCrossZones(iso.isCrossZones());
                isoResponse.setPublic(iso.isPublicTemplate());

                // TODO:  implement
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
                    // TODO:  implement
                    isoResponse.setDomainName(ApiDBUtils.findDomainById(owner.getDomainId()).getName());
                }
                
                // Add the zone ID
                // TODO:  implement
                HostVO host = ApiDBUtils.findHostById(isoHost.getHostId());
                DataCenterVO datacenter = ApiDBUtils.findZoneById(host.getDataCenterId());
                isoResponse.setZoneId(host.getDataCenterId());
                isoResponse.setZoneName(datacenter.getName());

                // If the user is an admin, add the template download status
                if (isAdmin || account.getId() == iso.getAccountId()) {
                    // add download status
                    if (isoHost.getDownloadState()!=Status.DOWNLOADED) {
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
            }
        }
        
        response.setResponses(isoResponses);

        return response;
    }
    
    private List<CapacityVO> sumCapacities(List<? extends Capacity> hostCapacities) {           
        Map<String, Long> totalCapacityMap = new HashMap<String, Long>();
        Map<String, Long> usedCapacityMap = new HashMap<String, Long>();
        
        Set<Long> poolIdsToIgnore = new HashSet<Long>();
        Criteria c = new Criteria();
        // TODO:  implement
        List<? extends StoragePoolVO> allStoragePools = ApiDBUtils.searchForStoragePools(c);
        for (StoragePoolVO pool : allStoragePools) {
            StoragePoolType poolType = pool.getPoolType();
            if (!(poolType.equals(StoragePoolType.NetworkFilesystem) || poolType.equals(StoragePoolType.IscsiLUN))) {
                poolIdsToIgnore.add(pool.getId());
            }
        }
        
        // collect all the capacity types, sum allocated/used and sum total...get one capacity number for each
        for (Capacity capacity : hostCapacities) {
            if (poolIdsToIgnore.contains(capacity.getHostOrPoolId())) {
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

            if (totalCapacity == null) {
                totalCapacity = new Long(capacity.getTotalCapacity());
            } else {
                totalCapacity = new Long(capacity.getTotalCapacity() + totalCapacity);
            }

            if (usedCapacity == null) {
                usedCapacity = new Long(capacity.getUsedCapacity());
            } else {
                usedCapacity = new Long(capacity.getUsedCapacity() + usedCapacity);
            }

            totalCapacityMap.put(key, totalCapacity);
            usedCapacityMap.put(key, usedCapacity);
            
            if (sumPodCapacity) {
                totalCapacity = totalCapacityMap.get(keyForPodTotal);
                usedCapacity = usedCapacityMap.get(keyForPodTotal);

                if (totalCapacity == null) {
                    totalCapacity = new Long(capacity.getTotalCapacity());
                } else {
                    totalCapacity = new Long(capacity.getTotalCapacity() + totalCapacity);
                }

                if (usedCapacity == null) {
                    usedCapacity = new Long(capacity.getUsedCapacity());
                } else {
                    usedCapacity = new Long(capacity.getUsedCapacity() + usedCapacity);
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
                    capacityResponse.setPodName(ApiDBUtils.findPodById(summedCapacity.getPodId()).getName());
                } else {
                    capacityResponse.setPodName("All");
                }
            }
            capacityResponse.setZoneId(summedCapacity.getDataCenterId());
            capacityResponse.setZoneName(ApiDBUtils.findZoneById(summedCapacity.getDataCenterId()).getName());
            if (summedCapacity.getTotalCapacity() != 0) {
                //float computed = ((float)summedCapacity.getUsedCapacity() / (float)summedCapacity.getTotalCapacity() * 100f);
                capacityResponse.setPercentUsed(format.format((float)summedCapacity.getUsedCapacity() / (float)summedCapacity.getTotalCapacity() * 100f));
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
            // FIXME:  we have just template id and need to get template owner from that
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
    public AsyncJobResponse queryJobResult(QueryAsyncJobResultCmd cmd) throws InvalidParameterValueException{
        AsyncJobResult result = ApiDBUtils._asyncMgr.queryAsyncJobResult(cmd);
        AsyncJobResponse response = new AsyncJobResponse();
        response.setId(result.getJobId());
        response.setJobStatus(result.getJobStatus());
        response.setJobProcStatus(result.getProcessStatus());
        response.setJobResultCode(result.getResultCode());
        response.setJobResult((ResponseObject)ApiSerializerHelper.fromSerializedString(result.getResult()));

        Object resultObject = result.getResultObject();
        if (resultObject != null) {
            Class<?> clz = resultObject.getClass();
            if(clz.isPrimitive() || clz.getSuperclass() == Number.class || clz == String.class || clz == Date.class) {
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

        if ((ingressRules != null) && !ingressRules.isEmpty()) {
            SecurityGroup securityGroup  = ApiDBUtils.findNetworkGroupById(ingressRules.get(0).getSecurityGroupId());
            response.setId(securityGroup.getId());
            response.setName(securityGroup.getName());
            response.setDescription(securityGroup.getDescription());
            response.setAccountName(securityGroup.getAccountName());
            response.setDomainId(securityGroup.getDomainId());
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

                if (ingressRule.getAllowedSecurityGroup() != null) {
                    ingressData.setSecurityGroupName(ingressRule.getAllowedSecurityGroup());
                    ingressData.setAccountName(ingressRule.getAllowedSecGrpAcct());
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
        if (offering.getGuestIpType() != null) {
            response.setType(offering.getGuestIpType().toString());
        }
        response.setMaxconnections(offering.getConcurrentConnections());
        response.setIsDefault(offering.isDefault());
        response.setSpecifyVlan(offering.getSpecifyVlan());
        response.setAvailability(offering.getAvailability().toString());
        response.setObjectName("networkoffering");
        return response;
    }
    
    @Override
    public NetworkResponse createNetworkResponse(Network network) {
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
        
        //get start ip and end ip of corresponding vlan
        List<? extends Vlan> vlan= ApiDBUtils.listVlanByNetworkId(network.getId());
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
        
        //populate network offering information
        NetworkOffering networkOffering = ApiDBUtils.findNetworkOfferingById(network.getNetworkOfferingId());
        if (networkOffering != null) {
            response.setNetworkOfferingId(networkOffering.getId());
            response.setNetworkOfferingName(networkOffering.getName());
            response.setNetworkOfferingDisplayText(networkOffering.getDisplayText());
            response.setIsSystem(networkOffering.isSystemOnly());
            response.setNetworkOfferingAvailability(networkOffering.getAvailability().toString());
        }
        
        response.setIsShared(network.isShared());
        response.setState(network.getState().toString());
        response.setRelated(network.getRelated());
        response.setDns1(network.getDns1());
        response.setDns2(network.getDns2());

        Account account = ApiDBUtils.findAccountById(network.getAccountId());
        if (account != null) {
            response.setAccountName(account.getAccountName());
            Domain domain = ApiDBUtils.findDomainById(account.getDomainId());
            response.setDomainId(domain.getId());
            response.setDomain(domain.getName());
        }
        response.setObjectName("network");
        return response;
    }
}
