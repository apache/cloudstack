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
package com.cloud.api;

import com.cloud.api.query.ViewResponseHelper;
import com.cloud.api.query.vo.*;
import com.cloud.api.response.ApiResponseSerializer;

import org.apache.cloudstack.api.response.AsyncJobResponse;
import org.apache.cloudstack.api.response.AutoScalePolicyResponse;
import org.apache.cloudstack.api.response.AutoScaleVmGroupResponse;
import org.apache.cloudstack.api.response.AutoScaleVmProfileResponse;
import org.apache.cloudstack.api.response.CapabilityResponse;
import org.apache.cloudstack.api.response.CapacityResponse;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.ConditionResponse;
import org.apache.cloudstack.api.response.ConfigurationResponse;
import org.apache.cloudstack.api.response.ControlledEntityResponse;
import org.apache.cloudstack.api.response.CounterResponse;
import org.apache.cloudstack.api.response.CreateCmdResponse;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.api.response.EventResponse;
import org.apache.cloudstack.api.response.ExtractResponse;
import org.apache.cloudstack.api.response.FirewallResponse;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.GuestOSResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.HypervisorCapabilitiesResponse;
import org.apache.cloudstack.api.response.ControlledViewEntityResponse;
import org.apache.cloudstack.api.response.IPAddressResponse;
import org.apache.cloudstack.api.response.InstanceGroupResponse;
import org.apache.cloudstack.api.response.IpForwardingRuleResponse;
import org.apache.cloudstack.api.response.LBHealthCheckPolicyResponse;
import org.apache.cloudstack.api.response.LBHealthCheckResponse;
import org.apache.cloudstack.api.response.LBStickinessPolicyResponse;
import org.apache.cloudstack.api.response.LBStickinessResponse;
import org.apache.cloudstack.api.response.LDAPConfigResponse;
import org.apache.cloudstack.api.response.LoadBalancerResponse;
import org.apache.cloudstack.api.response.NetworkACLResponse;
import org.apache.cloudstack.api.response.NetworkOfferingResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.PrivateGatewayResponse;
import org.apache.cloudstack.api.response.ProjectAccountResponse;
import org.apache.cloudstack.api.response.ProjectInvitationResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ProviderResponse;
import org.apache.cloudstack.api.response.RemoteAccessVpnResponse;
import org.apache.cloudstack.api.response.ResourceCountResponse;
import org.apache.cloudstack.api.response.ResourceLimitResponse;
import org.apache.cloudstack.api.response.ResourceTagResponse;
import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.SecurityGroupRuleResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.ServiceResponse;
import org.apache.cloudstack.api.response.Site2SiteCustomerGatewayResponse;
import org.apache.cloudstack.api.response.Site2SiteVpnConnectionResponse;
import org.apache.cloudstack.api.response.Site2SiteVpnGatewayResponse;
import org.apache.cloudstack.api.response.SnapshotPolicyResponse;
import org.apache.cloudstack.api.response.SnapshotResponse;
import org.apache.cloudstack.api.response.SnapshotScheduleResponse;
import org.apache.cloudstack.api.response.StaticRouteResponse;
import org.apache.cloudstack.api.response.StorageNetworkIpRangeResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.SwiftResponse;
import org.apache.cloudstack.api.response.SystemVmInstanceResponse;
import org.apache.cloudstack.api.response.SystemVmResponse;
import org.apache.cloudstack.api.response.TemplatePermissionsResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.TrafficMonitorResponse;
import org.apache.cloudstack.api.response.TrafficTypeResponse;
import org.apache.cloudstack.api.response.UsageRecordResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VirtualRouterProviderResponse;
import org.apache.cloudstack.api.response.VlanIpRangeResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.api.response.VpcOfferingResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.cloudstack.api.response.VpnUsersResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.api.response.S3Response;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.springframework.stereotype.Component;

import com.cloud.async.AsyncJob;
import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDaoImpl.SummedCapacity;
import com.cloud.configuration.Configuration;
import com.cloud.configuration.Resource.ResourceOwnerType;
import com.cloud.configuration.ResourceCount;
import com.cloud.configuration.ResourceLimit;
import com.cloud.dao.EntityManager;
import com.cloud.dc.*;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.domain.Domain;
import com.cloud.event.Event;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.HypervisorCapabilities;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PhysicalNetworkTrafficType;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.Site2SiteCustomerGateway;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.VpnUser;
import com.cloud.network.as.AutoScalePolicy;
import com.cloud.network.as.AutoScaleVmGroup;
import com.cloud.network.as.AutoScaleVmProfile;
import com.cloud.network.as.AutoScaleVmProfileVO;
import com.cloud.network.as.Condition;
import com.cloud.network.as.ConditionVO;
import com.cloud.network.as.Counter;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.*;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.HealthCheckPolicy;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.StaticNatRule;
import com.cloud.network.rules.StickinessPolicy;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupVO;
import com.cloud.network.security.SecurityRule;
import com.cloud.network.security.SecurityRule.SecurityRuleType;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.StaticRoute;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.org.Cluster;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectAccount;
import com.cloud.projects.ProjectInvitation;
import com.cloud.server.Criteria;
import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.*;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;

import com.cloud.storage.snapshot.SnapshotPolicy;
import com.cloud.storage.snapshot.SnapshotSchedule;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.UserContext;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.InstanceGroup;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.NicSecondaryIp;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.dao.NicSecondaryIpVO;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.api.ApiConstants.HostDetails;
import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.command.user.job.QueryAsyncJobResultCmd;
import org.apache.cloudstack.api.response.*;
import org.apache.cloudstack.region.Region;
import org.apache.cloudstack.usage.Usage;
import org.apache.cloudstack.usage.UsageService;
import org.apache.cloudstack.usage.UsageTypes;
import com.cloud.vm.VmStats;                                                                                                        
import com.cloud.vm.dao.UserVmData;                                                                                                 
import com.cloud.vm.dao.UserVmData.NicData;                                                                                         
import com.cloud.vm.dao.UserVmData.SecurityGroupData;                                                                               
import com.cloud.vm.snapshot.VMSnapshot;                                                                                            
import org.apache.cloudstack.api.ResponseGenerator;                                                                                 
import org.apache.cloudstack.api.response.VMSnapshotResponse;
import org.apache.log4j.Logger;

import java.text.DecimalFormat;
import java.util.*;

import javax.inject.Inject;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Component
public class ApiResponseHelper implements ResponseGenerator {

    public final Logger s_logger = Logger.getLogger(ApiResponseHelper.class);
    private static final DecimalFormat s_percentFormat = new DecimalFormat("##.##");
    @Inject private EntityManager _entityMgr = null;
    @Inject private UsageService _usageSvc = null;

    @Override
    public UserResponse createUserResponse(User user) {
        UserAccountJoinVO vUser = ApiDBUtils.newUserView(user);
        return ApiDBUtils.newUserResponse(vUser);
    }



    // this method is used for response generation via createAccount (which creates an account + user)
    @Override
    public AccountResponse createUserAccountResponse(UserAccount user) {
        return ApiDBUtils.newAccountResponse(ApiDBUtils.findAccountViewById(user.getAccountId()));
    }

    @Override
    public AccountResponse createAccountResponse(Account account) {
        AccountJoinVO vUser = ApiDBUtils.newAccountView(account);
        return ApiDBUtils.newAccountResponse(vUser);
    }


    @Override
    public UserResponse createUserResponse(UserAccount user) {
        UserAccountJoinVO vUser = ApiDBUtils.newUserView(user);
        return ApiDBUtils.newUserResponse(vUser);
    }

    @Override
    public DomainResponse createDomainResponse(Domain domain) {
        DomainResponse domainResponse = new DomainResponse();
        domainResponse.setDomainName(domain.getName());
        domainResponse.setId(domain.getUuid());
        domainResponse.setLevel(domain.getLevel());
        domainResponse.setNetworkDomain(domain.getNetworkDomain());
        Domain parentDomain = ApiDBUtils.findDomainById(domain.getParent());
        if (parentDomain != null) {
            domainResponse.setParentDomainId(parentDomain.getUuid());
        }
        StringBuilder domainPath = new StringBuilder("ROOT");
        (domainPath.append(domain.getPath())).deleteCharAt(domainPath.length() - 1);
        domainResponse.setPath(domainPath.toString());
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
        DiskOfferingJoinVO vOffering = ApiDBUtils.newDiskOfferingView(offering);
        return ApiDBUtils.newDiskOfferingResponse(vOffering);
    }

    @Override
    public ResourceLimitResponse createResourceLimitResponse(ResourceLimit limit) {
        ResourceLimitResponse resourceLimitResponse = new ResourceLimitResponse();
        if (limit.getResourceOwnerType() == ResourceOwnerType.Domain) {
            populateDomain(resourceLimitResponse, limit.getOwnerId());
        } else if (limit.getResourceOwnerType() == ResourceOwnerType.Account) {
            Account accountTemp = ApiDBUtils.findAccountById(limit.getOwnerId());
            populateAccount(resourceLimitResponse, limit.getOwnerId());
            populateDomain(resourceLimitResponse, accountTemp.getDomainId());
        }
        resourceLimitResponse.setResourceType(Integer.valueOf(limit.getType().getOrdinal()).toString());
        resourceLimitResponse.setMax(limit.getMax());
        resourceLimitResponse.setObjectName("resourcelimit");

        return resourceLimitResponse;
    }

    @Override
    public ResourceCountResponse createResourceCountResponse(ResourceCount resourceCount) {
        ResourceCountResponse resourceCountResponse = new ResourceCountResponse();

        if (resourceCount.getResourceOwnerType() == ResourceOwnerType.Account) {
            Account accountTemp = ApiDBUtils.findAccountById(resourceCount.getOwnerId());
            if (accountTemp != null) {
                populateAccount(resourceCountResponse, accountTemp.getId());
                populateDomain(resourceCountResponse, accountTemp.getDomainId());
            }
        } else if (resourceCount.getResourceOwnerType() == ResourceOwnerType.Domain) {
            populateDomain(resourceCountResponse, resourceCount.getOwnerId());
        }

        resourceCountResponse.setResourceType(Integer.valueOf(resourceCount.getType().getOrdinal()).toString());
        resourceCountResponse.setResourceCount(resourceCount.getCount());
        resourceCountResponse.setObjectName("resourcecount");
        return resourceCountResponse;
    }

    @Override
    public ServiceOfferingResponse createServiceOfferingResponse(ServiceOffering offering) {
        ServiceOfferingJoinVO vOffering = ApiDBUtils.newServiceOfferingView(offering);
        return ApiDBUtils.newServiceOfferingResponse(vOffering);
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
        snapshotResponse.setId(snapshot.getUuid());

        populateOwner(snapshotResponse, snapshot);

        VolumeVO volume = findVolumeById(snapshot.getVolumeId());
        String snapshotTypeStr = snapshot.getRecurringType().name();
        snapshotResponse.setSnapshotType(snapshotTypeStr);
        if (volume != null) {
            snapshotResponse.setVolumeId(volume.getUuid());
            snapshotResponse.setVolumeName(volume.getName());
            snapshotResponse.setVolumeType(volume.getVolumeType().name());
        }
        snapshotResponse.setCreated(snapshot.getCreated());
        snapshotResponse.setName(snapshot.getName());
        snapshotResponse.setIntervalType(ApiDBUtils.getSnapshotIntervalTypes(snapshot.getId()));
        snapshotResponse.setState(snapshot.getState());

        //set tag information
        List<? extends ResourceTag> tags = ApiDBUtils.listByResourceTypeAndId(TaggedResourceType.Snapshot, snapshot.getId());
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            tagResponses.add(tagResponse);
        }
        snapshotResponse.setTags(tagResponses);

        snapshotResponse.setObjectName("snapshot");
        return snapshotResponse;
    }

    @Override
    public VMSnapshotResponse createVMSnapshotResponse(VMSnapshot vmSnapshot) {
        VMSnapshotResponse vmSnapshotResponse = new VMSnapshotResponse();
        vmSnapshotResponse.setId(vmSnapshot.getUuid());
        vmSnapshotResponse.setName(vmSnapshot.getName());
        vmSnapshotResponse.setState(vmSnapshot.getState());
        vmSnapshotResponse.setCreated(vmSnapshot.getCreated());
        vmSnapshotResponse.setDescription(vmSnapshot.getDescription());
        vmSnapshotResponse.setDisplayName(vmSnapshot.getDisplayName());
        UserVm vm = ApiDBUtils.findUserVmById(vmSnapshot.getVmId());        
        if(vm!=null)
            vmSnapshotResponse.setVirtualMachineid(vm.getUuid());
        if(vmSnapshot.getParent() != null)
            vmSnapshotResponse.setParentName(ApiDBUtils.getVMSnapshotById(vmSnapshot.getParent()).getDisplayName());
        vmSnapshotResponse.setCurrent(vmSnapshot.getCurrent());
        vmSnapshotResponse.setType(vmSnapshot.getType().toString());
        return vmSnapshotResponse;
    }
    
    @Override
    public SnapshotPolicyResponse createSnapshotPolicyResponse(SnapshotPolicy policy) {
        SnapshotPolicyResponse policyResponse = new SnapshotPolicyResponse();
        policyResponse.setId(policy.getUuid());
        Volume vol = ApiDBUtils.findVolumeById(policy.getVolumeId());
        if (vol != null) {
            policyResponse.setVolumeId(vol.getUuid());
        }
        policyResponse.setSchedule(policy.getSchedule());
        policyResponse.setIntervalType(policy.getInterval());
        policyResponse.setMaxSnaps(policy.getMaxSnaps());
        policyResponse.setTimezone(policy.getTimezone());
        policyResponse.setObjectName("snapshotpolicy");

        return policyResponse;
    }

    @Override
    public HostResponse createHostResponse(Host host) {
        return createHostResponse(host, EnumSet.of(HostDetails.all));
    }

    @Override
    public HostResponse createHostResponse(Host host, EnumSet<HostDetails> details) {
        List<HostJoinVO> viewHosts = ApiDBUtils.newHostView(host);
        List<HostResponse> listHosts = ViewResponseHelper.createHostResponse(details, viewHosts.toArray(new HostJoinVO[viewHosts.size()]));
        assert listHosts != null && listHosts.size() == 1 : "There should be one host returned";
        return listHosts.get(0);
    }

    @Override
    public SwiftResponse createSwiftResponse(Swift swift) {
        SwiftResponse swiftResponse = new SwiftResponse();
        swiftResponse.setId(swift.getUuid());
        swiftResponse.setUrl(swift.getUrl());
        swiftResponse.setAccount(swift.getAccount());
        swiftResponse.setUsername(swift.getUserName());
        swiftResponse.setObjectName("swift");
        return swiftResponse;
    }

    @Override
    public S3Response createS3Response(final S3 result) {

        final S3Response response = new S3Response();

        response.setAccessKey(result.getAccessKey());
        response.setConnectionTimeout(result.getConnectionTimeout());
        response.setEndPoint(result.getEndPoint());
        response.setHttpsFlag(result.getHttpsFlag());
        response.setMaxErrorRetry(result.getMaxErrorRetry());
        response.setObjectId(result.getUuid());
        response.setSecretKey(result.getSecretKey());
        response.setSocketTimeout(result.getSocketTimeout());
        response.setTemplateBucketName(result.getBucketName());

        return response;

    }

    @Override
    public VlanIpRangeResponse createVlanIpRangeResponse(Vlan vlan) {
        Long podId = ApiDBUtils.getPodIdForVlan(vlan.getId());

        VlanIpRangeResponse vlanResponse = new VlanIpRangeResponse();
        vlanResponse.setId(vlan.getUuid());
        if (vlan.getVlanType() != null) {
            vlanResponse.setForVirtualNetwork(vlan.getVlanType().equals(VlanType.VirtualNetwork));
        }
        vlanResponse.setVlan(vlan.getVlanTag());
        DataCenter zone = ApiDBUtils.findZoneById(vlan.getDataCenterId());
        if (zone != null) {
            vlanResponse.setZoneId(zone.getUuid());
        }

        if (podId != null) {
            HostPodVO pod = ApiDBUtils.findPodById(podId);
            if (pod != null) {
                vlanResponse.setPodId(pod.getUuid());
                vlanResponse.setPodName(pod.getName());
            }
        }

        vlanResponse.setGateway(vlan.getVlanGateway());
        vlanResponse.setNetmask(vlan.getVlanNetmask());

        // get start ip and end ip of corresponding vlan
        String ipRange = vlan.getIpRange();
        if (ipRange != null) {
        	String[] range = ipRange.split("-");
        	vlanResponse.setStartIp(range[0]);
        	vlanResponse.setEndIp(range[1]);
        }

        vlanResponse.setIp6Gateway(vlan.getIp6Gateway());
        vlanResponse.setIp6Cidr(vlan.getIp6Cidr());
        
        String ip6Range = vlan.getIp6Range();
        if (ip6Range != null) {
        	String[] range = ip6Range.split("-");
        	vlanResponse.setStartIpv6(range[0]);
        	vlanResponse.setEndIpv6(range[1]);
        }

        if (vlan.getNetworkId() != null) {
            Network nw = ApiDBUtils.findNetworkById(vlan.getNetworkId());
            if (nw != null) {
                vlanResponse.setNetworkId(nw.getUuid());
            }
        }
        Account owner = ApiDBUtils.getVlanAccount(vlan.getId());
        if (owner != null) {
            populateAccount(vlanResponse, owner.getId());
            populateDomain(vlanResponse, owner.getDomainId());
        }

        if (vlan.getPhysicalNetworkId() != null) {
            PhysicalNetwork pnw = ApiDBUtils.findPhysicalNetworkById(vlan.getPhysicalNetworkId());
            if (pnw != null) {
                vlanResponse.setPhysicalNetworkId(pnw.getUuid());
            }
        }
        vlanResponse.setObjectName("vlan");
        return vlanResponse;
    }

    @Override
    public IPAddressResponse createIPAddressResponse(IpAddress ipAddr) {
        VlanVO vlan = ApiDBUtils.findVlanById(ipAddr.getVlanId());
        boolean forVirtualNetworks = vlan.getVlanType().equals(VlanType.VirtualNetwork);
        long zoneId = ipAddr.getDataCenterId();

        IPAddressResponse ipResponse = new IPAddressResponse();
        ipResponse.setId(ipAddr.getUuid());
        ipResponse.setIpAddress(ipAddr.getAddress().toString());
        if (ipAddr.getAllocatedTime() != null) {
            ipResponse.setAllocated(ipAddr.getAllocatedTime());
        }
        DataCenter zone = ApiDBUtils.findZoneById(ipAddr.getDataCenterId());
        if (zone != null) {
            ipResponse.setZoneId(zone.getUuid());
            ipResponse.setZoneName(zone.getName());
        }
        ipResponse.setSourceNat(ipAddr.isSourceNat());
        ipResponse.setIsSystem(ipAddr.getSystem());

        // get account information
        if (ipAddr.getAllocatedToAccountId() != null) {
            populateOwner(ipResponse, ipAddr);
        }

        ipResponse.setForVirtualNetwork(forVirtualNetworks);
        ipResponse.setStaticNat(ipAddr.isOneToOneNat());

        if (ipAddr.getAssociatedWithVmId() != null) {
            UserVm vm = ApiDBUtils.findUserVmById(ipAddr.getAssociatedWithVmId());
            if (vm != null) {
                ipResponse.setVirtualMachineId(vm.getUuid());
                ipResponse.setVirtualMachineName(vm.getHostName());
                if (vm.getDisplayName() != null) {
                    ipResponse.setVirtualMachineDisplayName(vm.getDisplayName());
                } else {
                    ipResponse.setVirtualMachineDisplayName(vm.getHostName());
                }
            }
        }
        if (ipAddr.getVmIp() != null) {
            ipResponse.setVirtualMachineIp(ipAddr.getVmIp());
        }

        if (ipAddr.getAssociatedWithNetworkId() != null) {
            Network ntwk = ApiDBUtils.findNetworkById(ipAddr.getAssociatedWithNetworkId());
            if (ntwk != null) {
                ipResponse.setAssociatedNetworkId(ntwk.getUuid());
                ipResponse.setAssociatedNetworkName(ntwk.getName());
            }
        }

        if (ipAddr.getVpcId() != null) {
            Vpc vpc = ApiDBUtils.findVpcById(ipAddr.getVpcId());
            if (vpc != null) {
                ipResponse.setVpcId(vpc.getUuid());
            }
        }

        // Network id the ip is associated with (if associated networkId is
        // null, try to get this information from vlan)
        Long vlanNetworkId = ApiDBUtils.getVlanNetworkId(ipAddr.getVlanId());

        // Network id the ip belongs to
        Long networkId;
        if (vlanNetworkId != null) {
            networkId = vlanNetworkId;
        } else {
            networkId = ApiDBUtils.getPublicNetworkIdByZone(zoneId);
        }

        if (networkId != null) {
            NetworkVO nw = ApiDBUtils.findNetworkById(networkId);
            if (nw != null) {
                ipResponse.setNetworkId(nw.getUuid());
            }
        }
        ipResponse.setState(ipAddr.getState().toString());

        if (ipAddr.getPhysicalNetworkId() != null) {
            PhysicalNetworkVO pnw = ApiDBUtils.findPhysicalNetworkById(ipAddr.getPhysicalNetworkId());
            if (pnw != null) {
                ipResponse.setPhysicalNetworkId(pnw.getUuid());
            }
        }

        // show this info to admin only
        Account account = UserContext.current().getCaller();
        if (account.getType() == Account.ACCOUNT_TYPE_ADMIN) {
            VlanVO vl = ApiDBUtils.findVlanById(ipAddr.getVlanId());
            if (vl != null) {
                ipResponse.setVlanId(vl.getUuid());
                ipResponse.setVlanName(vl.getVlanTag());
            }
        }

        if (ipAddr.getSystem()) {
            if (ipAddr.isOneToOneNat()) {
                ipResponse.setPurpose(IpAddress.Purpose.StaticNat.toString());
            } else {
                ipResponse.setPurpose(IpAddress.Purpose.Lb.toString());
            }
        }

        //set tag information
        List<? extends ResourceTag> tags = ApiDBUtils.listByResourceTypeAndId(TaggedResourceType.PublicIpAddress, ipAddr.getId());
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            tagResponses.add(tagResponse);
        }
        ipResponse.setTags(tagResponses);

        ipResponse.setObjectName("ipaddress");
        return ipResponse;
    }

    @Override
    public LoadBalancerResponse createLoadBalancerResponse(LoadBalancer loadBalancer) {
        LoadBalancerResponse lbResponse = new LoadBalancerResponse();
        lbResponse.setId(loadBalancer.getUuid());
        lbResponse.setName(loadBalancer.getName());
        lbResponse.setDescription(loadBalancer.getDescription());
        List<String> cidrs = ApiDBUtils.findFirewallSourceCidrs(loadBalancer.getId());
        lbResponse.setCidrList(StringUtils.join(cidrs, ","));

        IPAddressVO publicIp = ApiDBUtils.findIpAddressById(loadBalancer.getSourceIpAddressId());
        lbResponse.setPublicIpId(publicIp.getUuid());
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
        populateOwner(lbResponse, loadBalancer);
        DataCenter zone = ApiDBUtils.findZoneById(publicIp.getDataCenterId());
        if (zone != null) {
            lbResponse.setZoneId(zone.getUuid());
        }

        //set tag information
        List<? extends ResourceTag> tags = ApiDBUtils.listByResourceTypeAndId(TaggedResourceType.UserVm, loadBalancer.getId());
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            tagResponses.add(tagResponse);
        }
        lbResponse.setTags(tagResponses);

        lbResponse.setObjectName("loadbalancer");
        return lbResponse;
    }

    @Override
    public PodResponse createPodResponse(Pod pod, Boolean showCapacities) {
        String[] ipRange = new String[2];
        if (pod.getDescription() != null && pod.getDescription().length() > 0) {
            ipRange = pod.getDescription().split("-");
        } else {
            ipRange[0] = pod.getDescription();
        }

        PodResponse podResponse = new PodResponse();
        podResponse.setId(pod.getUuid());
        podResponse.setName(pod.getName());
        DataCenter zone = ApiDBUtils.findZoneById(pod.getDataCenterId());
        if (zone != null) {
            podResponse.setZoneId(zone.getUuid());
            podResponse.setZoneName(zone.getName());
        }
        podResponse.setNetmask(NetUtils.getCidrNetmask(pod.getCidrSize()));
        podResponse.setStartIp(ipRange[0]);
        podResponse.setEndIp(((ipRange.length > 1) && (ipRange[1] != null)) ? ipRange[1] : "");
        podResponse.setGateway(pod.getGateway());
        podResponse.setAllocationState(pod.getAllocationState().toString());
        if (showCapacities != null && showCapacities) {
            List<SummedCapacity> capacities = ApiDBUtils.getCapacityByClusterPodZone(null, pod.getId(), null);
            Set<CapacityResponse> capacityResponses = new HashSet<CapacityResponse>();
            for (SummedCapacity capacity : capacities) {
                CapacityResponse capacityResponse = new CapacityResponse();
                capacityResponse.setCapacityType(capacity.getCapacityType());
                capacityResponse.setCapacityUsed(capacity.getUsedCapacity());
                if (capacity.getCapacityType() == Capacity.CAPACITY_TYPE_CPU) {
                    capacityResponse.setCapacityTotal(new Long((long) (capacity.getTotalCapacity())));
                } else if (capacity.getCapacityType() == Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED) {
                    List<SummedCapacity> c = ApiDBUtils.findNonSharedStorageForClusterPodZone(null, pod.getId(), null);
                    capacityResponse.setCapacityTotal(capacity.getTotalCapacity() - c.get(0).getTotalCapacity());
                    capacityResponse.setCapacityUsed(capacity.getUsedCapacity() - c.get(0).getUsedCapacity());
                } else {
                    capacityResponse.setCapacityTotal(capacity.getTotalCapacity());
                }
                if (capacityResponse.getCapacityTotal() != 0) {
                    capacityResponse.setPercentUsed(s_percentFormat.format((float) capacityResponse.getCapacityUsed() / (float) capacityResponse.getCapacityTotal() * 100f));
                } else {
                    capacityResponse.setPercentUsed(s_percentFormat.format(0L));
                }
                capacityResponses.add(capacityResponse);
            }
            // Do it for stats as well.
            capacityResponses.addAll(getStatsCapacityresponse(null, null, pod.getId(), pod.getDataCenterId()));
            podResponse.setCapacitites(new ArrayList<CapacityResponse>(capacityResponses));
        }
        podResponse.setObjectName("pod");
        return podResponse;
    }

    @Override
    public ZoneResponse createZoneResponse(DataCenter dataCenter, Boolean showCapacities) {
        DataCenterJoinVO vOffering = ApiDBUtils.newDataCenterView(dataCenter);
        return ApiDBUtils.newDataCenterResponse(vOffering, showCapacities);
    }

    public static List<CapacityResponse> getDataCenterCapacityResponse(Long zoneId){
        List<SummedCapacity> capacities = ApiDBUtils.getCapacityByClusterPodZone(zoneId, null, null);
        Set<CapacityResponse> capacityResponses = new HashSet<CapacityResponse>();
        float cpuOverprovisioningFactor = ApiDBUtils.getCpuOverprovisioningFactor();

        for (SummedCapacity capacity : capacities) {
            CapacityResponse capacityResponse = new CapacityResponse();
            capacityResponse.setCapacityType(capacity.getCapacityType());
            capacityResponse.setCapacityUsed(capacity.getUsedCapacity());
            if (capacity.getCapacityType() == Capacity.CAPACITY_TYPE_CPU) {
                capacityResponse.setCapacityTotal(new Long((long) (capacity.getTotalCapacity() * cpuOverprovisioningFactor)));
            } else if (capacity.getCapacityType() == Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED) {
                List<SummedCapacity> c = ApiDBUtils.findNonSharedStorageForClusterPodZone(zoneId, null, null);
                capacityResponse.setCapacityTotal(capacity.getTotalCapacity() - c.get(0).getTotalCapacity());
                capacityResponse.setCapacityUsed(capacity.getUsedCapacity() - c.get(0).getUsedCapacity());
            } else {
                capacityResponse.setCapacityTotal(capacity.getTotalCapacity());
            }
            if (capacityResponse.getCapacityTotal() != 0) {
                capacityResponse.setPercentUsed(s_percentFormat.format((float) capacityResponse.getCapacityUsed() / (float) capacityResponse.getCapacityTotal() * 100f));
            } else {
                capacityResponse.setPercentUsed(s_percentFormat.format(0L));
            }
            capacityResponses.add(capacityResponse);
        }
        // Do it for stats as well.
        capacityResponses.addAll(getStatsCapacityresponse(null, null, null, zoneId));

        return new ArrayList<CapacityResponse>(capacityResponses);
    }

    private static List<CapacityResponse> getStatsCapacityresponse(Long poolId, Long clusterId, Long podId, Long zoneId) {
        List<CapacityVO> capacities = new ArrayList<CapacityVO>();
        capacities.add(ApiDBUtils.getStoragePoolUsedStats(poolId, clusterId, podId, zoneId));
        if (clusterId == null && podId == null) {
            capacities.add(ApiDBUtils.getSecondaryStorageUsedStats(poolId, zoneId));
        }

        List<CapacityResponse> capacityResponses = new ArrayList<CapacityResponse>();
        for (CapacityVO capacity : capacities) {
            CapacityResponse capacityResponse = new CapacityResponse();
            capacityResponse.setCapacityType(capacity.getCapacityType());
            capacityResponse.setCapacityUsed(capacity.getUsedCapacity());
            capacityResponse.setCapacityTotal(capacity.getTotalCapacity());
            if (capacityResponse.getCapacityTotal() != 0) {
                capacityResponse.setPercentUsed(s_percentFormat.format((float) capacityResponse.getCapacityUsed() / (float) capacityResponse.getCapacityTotal() * 100f));
            } else {
                capacityResponse.setPercentUsed(s_percentFormat.format(0L));
            }
            capacityResponses.add(capacityResponse);
        }

        return capacityResponses;
    }

    @Override
    public VolumeResponse createVolumeResponse(Volume volume) {
        List<VolumeJoinVO> viewVrs = ApiDBUtils.newVolumeView(volume);
        List<VolumeResponse> listVrs = ViewResponseHelper.createVolumeResponse(viewVrs.toArray(new VolumeJoinVO[viewVrs.size()]));
        assert listVrs != null && listVrs.size() == 1 : "There should be one volume returned";
        return listVrs.get(0);
    }

    @Override
    public InstanceGroupResponse createInstanceGroupResponse(InstanceGroup group) {
        InstanceGroupJoinVO vgroup = ApiDBUtils.newInstanceGroupView(group);
        return ApiDBUtils.newInstanceGroupResponse(vgroup);

    }



    @Override
    public StoragePoolResponse createStoragePoolResponse(StoragePool pool) {
        List<StoragePoolJoinVO> viewPools = ApiDBUtils.newStoragePoolView(pool);
        List<StoragePoolResponse> listPools = ViewResponseHelper.createStoragePoolResponse(viewPools.toArray(new StoragePoolJoinVO[viewPools.size()]));
        assert listPools != null && listPools.size() == 1 : "There should be one storage pool returned";
        return listPools.get(0);


    }

    @Override
    public ClusterResponse createClusterResponse(Cluster cluster, Boolean showCapacities) {
        ClusterResponse clusterResponse = new ClusterResponse();
        clusterResponse.setId(cluster.getUuid());
        clusterResponse.setName(cluster.getName());
        HostPodVO pod = ApiDBUtils.findPodById(cluster.getPodId());
        if (pod != null) {
            clusterResponse.setPodId(pod.getUuid());
            clusterResponse.setPodName(pod.getName());
        }
        DataCenter dc = ApiDBUtils.findZoneById(cluster.getDataCenterId());
        if (dc != null) {
            clusterResponse.setZoneId(dc.getUuid());
            clusterResponse.setZoneName(dc.getName());
        }
        clusterResponse.setHypervisorType(cluster.getHypervisorType().toString());
        clusterResponse.setClusterType(cluster.getClusterType().toString());
        clusterResponse.setAllocationState(cluster.getAllocationState().toString());
        clusterResponse.setManagedState(cluster.getManagedState().toString());
        String cpuOvercommitRatio=ApiDBUtils.findClusterDetails(cluster.getId(),"cpuOvercommitRatio").getValue();
        String memoryOvercommitRatio=ApiDBUtils.findClusterDetails(cluster.getId(),"memoryOvercommitRatio").getValue();
        clusterResponse.setCpuovercommitratio(cpuOvercommitRatio);
        clusterResponse.setRamovercommitratio(memoryOvercommitRatio);


        if (showCapacities != null && showCapacities) {
            List<SummedCapacity> capacities = ApiDBUtils.getCapacityByClusterPodZone(null, null, cluster.getId());
            Set<CapacityResponse> capacityResponses = new HashSet<CapacityResponse>();

            for (SummedCapacity capacity : capacities) {
                CapacityResponse capacityResponse = new CapacityResponse();
                capacityResponse.setCapacityType(capacity.getCapacityType());
                capacityResponse.setCapacityUsed(capacity.getUsedCapacity());

                if (capacity.getCapacityType() == Capacity.CAPACITY_TYPE_CPU) {
                    capacityResponse.setCapacityTotal(new Long((long) (capacity.getTotalCapacity() * Float.parseFloat(cpuOvercommitRatio))));
                }else if (capacity.getCapacityType() == Capacity.CAPACITY_TYPE_MEMORY){
                    capacityResponse.setCapacityTotal(new Long((long) (capacity.getTotalCapacity() * Float.parseFloat(memoryOvercommitRatio))));
                }
                else if (capacity.getCapacityType() == Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED) {
                    List<SummedCapacity> c = ApiDBUtils.findNonSharedStorageForClusterPodZone(null, null, cluster.getId());
                    capacityResponse.setCapacityTotal(capacity.getTotalCapacity() - c.get(0).getTotalCapacity());
                    capacityResponse.setCapacityUsed(capacity.getUsedCapacity() - c.get(0).getUsedCapacity());
                } else {
                    capacityResponse.setCapacityTotal(capacity.getTotalCapacity());
                }
                if (capacityResponse.getCapacityTotal() != 0) {
                    capacityResponse.setPercentUsed(s_percentFormat.format((float) capacityResponse.getCapacityUsed() / (float) capacityResponse.getCapacityTotal() * 100f));
                } else {
                    capacityResponse.setPercentUsed(s_percentFormat.format(0L));
                }
                capacityResponses.add(capacityResponse);
            }
            // Do it for stats as well.
            capacityResponses.addAll(getStatsCapacityresponse(null, cluster.getId(), pod.getId(), pod.getDataCenterId()));
            clusterResponse.setCapacitites(new ArrayList<CapacityResponse>(capacityResponses));
        }
        clusterResponse.setObjectName("cluster");
        return clusterResponse;
    }

    @Override
    public FirewallRuleResponse createPortForwardingRuleResponse(PortForwardingRule fwRule) {
        FirewallRuleResponse response = new FirewallRuleResponse();
        response.setId(fwRule.getUuid());
        response.setPrivateStartPort(Integer.toString(fwRule.getDestinationPortStart()));
        response.setPrivateEndPort(Integer.toString(fwRule.getDestinationPortEnd()));
        response.setProtocol(fwRule.getProtocol());
        response.setPublicStartPort(Integer.toString(fwRule.getSourcePortStart()));
        response.setPublicEndPort(Integer.toString(fwRule.getSourcePortEnd()));
        List<String> cidrs = ApiDBUtils.findFirewallSourceCidrs(fwRule.getId());
        response.setCidrList(StringUtils.join(cidrs, ","));

        IpAddress ip = ApiDBUtils.findIpAddressById(fwRule.getSourceIpAddressId());
        response.setPublicIpAddressId(ip.getUuid());
        response.setPublicIpAddress(ip.getAddress().addr());

        if (ip != null && fwRule.getDestinationIpAddress() != null) {
            UserVm vm = ApiDBUtils.findUserVmById(fwRule.getVirtualMachineId());
            if (vm != null) {
                response.setVirtualMachineId(vm.getUuid());
                response.setVirtualMachineName(vm.getHostName());

                if (vm.getDisplayName() != null) {
                    response.setVirtualMachineDisplayName(vm.getDisplayName());
                } else {
                    response.setVirtualMachineDisplayName(vm.getHostName());
                }
            }
        }
        FirewallRule.State state = fwRule.getState();
        String stateToSet = state.toString();
        if (state.equals(FirewallRule.State.Revoke)) {
            stateToSet = "Deleting";
        }

        //set tag information
        List<? extends ResourceTag> tags = ApiDBUtils.listByResourceTypeAndId(TaggedResourceType.PortForwardingRule, fwRule.getId());
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            tagResponses.add(tagResponse);
        }
        response.setTags(tagResponses);

        response.setState(stateToSet);
        response.setObjectName("portforwardingrule");
        return response;
    }

    @Override
    public IpForwardingRuleResponse createIpForwardingRuleResponse(StaticNatRule fwRule) {
        IpForwardingRuleResponse response = new IpForwardingRuleResponse();
        response.setId(fwRule.getUuid());
        response.setProtocol(fwRule.getProtocol());

        IpAddress ip = ApiDBUtils.findIpAddressById(fwRule.getSourceIpAddressId());
        response.setPublicIpAddressId(ip.getId());
        response.setPublicIpAddress(ip.getAddress().addr());

        if (ip != null && fwRule.getDestIpAddress() != null) {
            UserVm vm = ApiDBUtils.findUserVmById(ip.getAssociatedWithVmId());
            if (vm != null) {// vm might be destroyed
                response.setVirtualMachineId(vm.getUuid());
                response.setVirtualMachineName(vm.getHostName());
                if (vm.getDisplayName() != null) {
                    response.setVirtualMachineDisplayName(vm.getDisplayName());
                } else {
                    response.setVirtualMachineDisplayName(vm.getHostName());
                }
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
    public List<UserVmResponse> createUserVmResponse(String objectName, EnumSet<VMDetails> details, UserVm... userVms) {
        List<UserVmJoinVO> viewVms = ApiDBUtils.newUserVmView(userVms);
        return ViewResponseHelper.createUserVmResponse(objectName, details, viewVms.toArray(new UserVmJoinVO[viewVms.size()]));

    }

    @Override
    public List<UserVmResponse> createUserVmResponse(String objectName, UserVm... userVms) {
        List<UserVmJoinVO> viewVms = ApiDBUtils.newUserVmView(userVms);
        return ViewResponseHelper.createUserVmResponse(objectName, viewVms.toArray(new UserVmJoinVO[viewVms.size()]));
    }



    @Override
    public DomainRouterResponse createDomainRouterResponse(VirtualRouter router) {
        List<DomainRouterJoinVO> viewVrs = ApiDBUtils.newDomainRouterView(router);
        List<DomainRouterResponse> listVrs = ViewResponseHelper.createDomainRouterResponse(viewVrs.toArray(new DomainRouterJoinVO[viewVrs.size()]));
        assert listVrs != null && listVrs.size() == 1 : "There should be one virtual router returned";
        return listVrs.get(0);
    }


    @Override
    public SystemVmResponse createSystemVmResponse(VirtualMachine vm) {
        SystemVmResponse vmResponse = new SystemVmResponse();
        if (vm.getType() == Type.SecondaryStorageVm || vm.getType() == Type.ConsoleProxy) {
            // SystemVm vm = (SystemVm) systemVM;
            vmResponse.setId(vm.getUuid());
            //vmResponse.setObjectId(vm.getId());
            vmResponse.setSystemVmType(vm.getType().toString().toLowerCase());

            vmResponse.setName(vm.getHostName());
            if ( vm.getPodIdToDeployIn() != null ){
                HostPodVO pod = ApiDBUtils.findPodById(vm.getPodIdToDeployIn());
                if ( pod != null ){
            vmResponse.setPodId(pod.getUuid());
                }
            }
            VMTemplateVO template = ApiDBUtils.findTemplateById(vm.getTemplateId());
            if (template != null){
            vmResponse.setTemplateId(template.getUuid());
            }
            vmResponse.setCreated(vm.getCreated());

            if (vm.getHostId() != null) {
                Host host = ApiDBUtils.findHostById(vm.getHostId());
                if (host != null) {
                    vmResponse.setHostId(host.getUuid());
                    vmResponse.setHostName(host.getName());
                }
            }

            if (vm.getState() != null) {
                vmResponse.setState(vm.getState().toString());
            }

            // for console proxies, add the active sessions
            if (vm.getType() == Type.ConsoleProxy) {
                ConsoleProxyVO proxy = ApiDBUtils.findConsoleProxy(vm.getId());
                // proxy can be already destroyed
                if (proxy != null) {
                    vmResponse.setActiveViewerSessions(proxy.getActiveSession());
                }
            }

            DataCenter zone = ApiDBUtils.findZoneById(vm.getDataCenterId());
            if (zone != null) {
                vmResponse.setZoneId(zone.getUuid());
                vmResponse.setZoneName(zone.getName());
                vmResponse.setDns1(zone.getDns1());
                vmResponse.setDns2(zone.getDns2());
            }

            List<NicProfile> nicProfiles = ApiDBUtils.getNics(vm);
            for (NicProfile singleNicProfile : nicProfiles) {
                Network network = ApiDBUtils.findNetworkById(singleNicProfile.getNetworkId());
                if (network != null) {
                    if (network.getTrafficType() == TrafficType.Management) {
                        vmResponse.setPrivateIp(singleNicProfile.getIp4Address());
                        vmResponse.setPrivateMacAddress(singleNicProfile.getMacAddress());
                        vmResponse.setPrivateNetmask(singleNicProfile.getNetmask());
                    } else if (network.getTrafficType() == TrafficType.Control) {
                        vmResponse.setLinkLocalIp(singleNicProfile.getIp4Address());
                        vmResponse.setLinkLocalMacAddress(singleNicProfile.getMacAddress());
                        vmResponse.setLinkLocalNetmask(singleNicProfile.getNetmask());
                    } else if (network.getTrafficType() == TrafficType.Public || network.getTrafficType() == TrafficType.Guest) {
                        /*In basic zone, public ip has TrafficType.Guest*/
                        vmResponse.setPublicIp(singleNicProfile.getIp4Address());
                        vmResponse.setPublicMacAddress(singleNicProfile.getMacAddress());
                        vmResponse.setPublicNetmask(singleNicProfile.getNetmask());
                        vmResponse.setGateway(singleNicProfile.getGateway());
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
        vpnResponse.setId(vpnUser.getUuid());
        vpnResponse.setUserName(vpnUser.getUsername());

        populateOwner(vpnResponse, vpnUser);

        vpnResponse.setObjectName("vpnuser");
        return vpnResponse;
    }

    @Override
    public RemoteAccessVpnResponse createRemoteAccessVpnResponse(RemoteAccessVpn vpn) {
        RemoteAccessVpnResponse vpnResponse = new RemoteAccessVpnResponse();
        IpAddress ip = ApiDBUtils.findIpAddressById(vpn.getServerAddressId());
        if (ip != null) {
            vpnResponse.setPublicIpId(ip.getUuid());
            vpnResponse.setPublicIp(ip.getAddress().addr());
        }
        vpnResponse.setIpRange(vpn.getIpRange());
        vpnResponse.setPresharedKey(vpn.getIpsecPresharedKey());

        populateOwner(vpnResponse, vpn);

        vpnResponse.setState(vpn.getState().toString());
        vpnResponse.setObjectName("remoteaccessvpn");

        return vpnResponse;
    }

    @Override
    public TemplateResponse createIsoResponse(VirtualMachineTemplate result) {
        TemplateResponse response = new TemplateResponse();
        response.setId(result.getUuid());
        response.setName(result.getName());
        response.setDisplayText(result.getDisplayText());
        response.setPublic(result.isPublicTemplate());
        response.setCreated(result.getCreated());
        response.setFormat(result.getFormat());
        GuestOS os = ApiDBUtils.findGuestOSById(result.getGuestOSId());
        if (os != null) {
            response.setOsTypeId(os.getUuid());
            response.setOsTypeName(os.getDisplayName());
        }
        response.setDetails(result.getDetails());
        Account caller = UserContext.current().getCaller();

        if (result.getFormat() == ImageFormat.ISO) { // Templates are always bootable
            response.setBootable(result.isBootable());
        } else {
            response.setHypervisor(result.getHypervisorType().toString());// hypervisors are associated with templates
        }

        // add account ID and name
        Account owner = ApiDBUtils.findAccountById(result.getAccountId());
        populateAccount(response, owner.getId());
        populateDomain(response, owner.getDomainId());

        //set tag information
        List<? extends ResourceTag> tags = null;
        if (result.getFormat() == ImageFormat.ISO) {
            tags = ApiDBUtils.listByResourceTypeAndId(TaggedResourceType.ISO, result.getId());
        } else {
            tags = ApiDBUtils.listByResourceTypeAndId(TaggedResourceType.Template, result.getId());
        }

        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            tagResponses.add(tagResponse);
        }
        response.setTags(tagResponses);

        response.setObjectName("iso");
        return response;
    }

    @Override
    public List<TemplateResponse> createTemplateResponses(long templateId, Long zoneId, boolean readyOnly) {
        if (zoneId == null || zoneId == -1) {
            List<TemplateResponse> responses = new ArrayList<TemplateResponse>();
            List<DataCenterVO> dcs = new ArrayList<DataCenterVO>();
            responses = createSwiftTemplateResponses(templateId);
            if (!responses.isEmpty()) {
                return responses;
            }
            dcs.addAll(ApiDBUtils.listZones());
            for (DataCenterVO dc : dcs) {
                responses.addAll(createTemplateResponses(templateId, dc.getId(), readyOnly));
            }
            return responses;
        } else {
            return createTemplateResponses(templateId, zoneId.longValue(), readyOnly);
        }
    }

    private List<TemplateResponse> createSwiftTemplateResponses(long templateId) {
        VirtualMachineTemplate template = findTemplateById(templateId);
        List<TemplateResponse> responses = new ArrayList<TemplateResponse>();
        VMTemplateSwiftVO templateSwiftRef = ApiDBUtils.findTemplateSwiftRef(templateId);
        if (templateSwiftRef == null) {
            return responses;
        }

        TemplateResponse templateResponse = new TemplateResponse();
        templateResponse.setId(template.getUuid());
        templateResponse.setName(template.getName());
        templateResponse.setDisplayText(template.getDisplayText());
        templateResponse.setPublic(template.isPublicTemplate());
        templateResponse.setCreated(templateSwiftRef.getCreated());

        templateResponse.setReady(true);
        templateResponse.setFeatured(template.isFeatured());
        templateResponse.setExtractable(template.isExtractable() && !(template.getTemplateType() == TemplateType.SYSTEM));
        templateResponse.setPasswordEnabled(template.getEnablePassword());
        templateResponse.setSshKeyEnabled(template.getEnableSshKey());
        templateResponse.setCrossZones(template.isCrossZones());
        templateResponse.setFormat(template.getFormat());
        templateResponse.setDetails(template.getDetails());
        if (template.getTemplateType() != null) {
            templateResponse.setTemplateType(template.getTemplateType().toString());
        }

        templateResponse.setHypervisor(template.getHypervisorType().toString());

        GuestOS os = ApiDBUtils.findGuestOSById(template.getGuestOSId());
        if (os != null) {
            templateResponse.setOsTypeId(os.getUuid());
            templateResponse.setOsTypeName(os.getDisplayName());
        } else {
            templateResponse.setOsTypeId("-1");
            templateResponse.setOsTypeName("");
        }

        Account account = ApiDBUtils.findAccountByIdIncludingRemoved(template.getAccountId());
        populateAccount(templateResponse, account.getId());
        populateDomain(templateResponse, account.getDomainId());

        Account caller = UserContext.current().getCaller();
        boolean isAdmin = false;
        if (BaseCmd.isAdmin(caller.getType())) {
            isAdmin = true;
        }

        // If the user is an Admin, add the template download status
        if (isAdmin || caller.getId() == template.getAccountId()) {
            // add download status
            templateResponse.setStatus("Successfully Installed");
        }

        Long templateSize = templateSwiftRef.getSize();
        if (templateSize > 0) {
            templateResponse.setSize(templateSize);
        }

        templateResponse.setChecksum(template.getChecksum());
        if (template.getSourceTemplateId() != null) {
            VirtualMachineTemplate tmpl = ApiDBUtils.findTemplateById(template.getSourceTemplateId());
            if (tmpl != null) {
                templateResponse.setSourceTemplateId(tmpl.getUuid());
            }
        }

        templateResponse.setChecksum(template.getChecksum());

        templateResponse.setTemplateTag(template.getTemplateTag());

        templateResponse.setObjectName("template");
        responses.add(templateResponse);
        return responses;
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
        templateResponse.setId(template.getUuid());
        templateResponse.setName(template.getName());
        templateResponse.setDisplayText(template.getDisplayText());
        templateResponse.setPublic(template.isPublicTemplate());
        templateResponse.setCreated(templateHostRef.getCreated());

        templateResponse.setReady(templateHostRef.getDownloadState() == Status.DOWNLOADED);
        templateResponse.setFeatured(template.isFeatured());
        templateResponse.setExtractable(template.isExtractable() && !(template.getTemplateType() == TemplateType.SYSTEM));
        templateResponse.setPasswordEnabled(template.getEnablePassword());
        templateResponse.setSshKeyEnabled(template.getEnableSshKey());
        templateResponse.setCrossZones(template.isCrossZones());
        templateResponse.setFormat(template.getFormat());
        if (template.getTemplateType() != null) {
            templateResponse.setTemplateType(template.getTemplateType().toString());
        }

        templateResponse.setHypervisor(template.getHypervisorType().toString());
        templateResponse.setDetails(template.getDetails());

        GuestOS os = ApiDBUtils.findGuestOSById(template.getGuestOSId());
        if (os != null) {
            templateResponse.setOsTypeId(os.getUuid());
            templateResponse.setOsTypeName(os.getDisplayName());
        } else {
            templateResponse.setOsTypeId("-1");
            templateResponse.setOsTypeName("");
        }

        Account account = ApiDBUtils.findAccountByIdIncludingRemoved(template.getAccountId());
        populateAccount(templateResponse, account.getId());
        populateDomain(templateResponse, account.getDomainId());

        DataCenter datacenter = ApiDBUtils.findZoneById(zoneId);

        if (datacenter != null) {
            // Add the zone ID
            templateResponse.setZoneId(datacenter.getUuid());
            templateResponse.setZoneName(datacenter.getName());
        }

        boolean isAdmin = false;
        Account caller = UserContext.current().getCaller();
        if ((caller == null) || BaseCmd.isAdmin(caller.getType())) {
            isAdmin = true;
        }

        // If the user is an Admin, add the template download status
        if (isAdmin || caller.getId() == template.getAccountId()) {
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
        if (template.getSourceTemplateId() != null) {
            VirtualMachineTemplate tmpl = ApiDBUtils.findTemplateById(template.getSourceTemplateId());
            if (tmpl != null) {
                templateResponse.setSourceTemplateId(tmpl.getUuid());
            }
        }

        templateResponse.setChecksum(template.getChecksum());

        templateResponse.setTemplateTag(template.getTemplateTag());

        //set tag information
        List<? extends ResourceTag> tags = null;
        if (template.getFormat() == ImageFormat.ISO) {
            tags = ApiDBUtils.listByResourceTypeAndId(TaggedResourceType.ISO, template.getId());
        } else {
            tags = ApiDBUtils.listByResourceTypeAndId(TaggedResourceType.Template, template.getId());
        }
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            tagResponses.add(tagResponse);
        }
        templateResponse.setTags(tagResponses);

        templateResponse.setObjectName("template");
        responses.add(templateResponse);
        return responses;
    }

    @Override
    public List<TemplateResponse> createIsoResponses(long isoId, Long zoneId, boolean readyOnly) {

        final List<TemplateResponse> isoResponses = new ArrayList<TemplateResponse>();
        VirtualMachineTemplate iso = findTemplateById(isoId);
        if (iso.getTemplateType() == TemplateType.PERHOST) {
            TemplateResponse isoResponse = new TemplateResponse();
            isoResponse.setId(iso.getUuid());
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
            isoResponse.setChecksum(iso.getChecksum());
            isoResponse.setPasswordEnabled(false);
            isoResponse.setDetails(iso.getDetails());

            // add account ID and name
            Account owner = ApiDBUtils.findAccountById(iso.getAccountId());
            populateAccount(isoResponse, owner.getId());
            populateDomain(isoResponse, owner.getDomainId());

            //set tag information
            List<? extends ResourceTag> tags =  ApiDBUtils.listByResourceTypeAndId(TaggedResourceType.ISO, iso.getId());
            List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
            for (ResourceTag tag : tags) {
                ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
                tagResponses.add(tagResponse);
            }
            isoResponse.setTags(tagResponses);

            isoResponse.setObjectName("iso");
            isoResponses.add(isoResponse);
            return isoResponses;
        } else {
            if (zoneId == null || zoneId == -1) {
                isoResponses.addAll(createSwiftIsoResponses(iso));
                if (!isoResponses.isEmpty()) {
                    return isoResponses;
                }

                isoResponses.addAll(createS3IsoResponses(iso));
                if (!isoResponses.isEmpty()) {
                    return isoResponses;
                }

                final List<DataCenterVO> dcs = new ArrayList<DataCenterVO>();
                dcs.addAll(ApiDBUtils.listZones());
                for (DataCenterVO dc : dcs) {
                    isoResponses.addAll(createIsoResponses(iso, dc.getId(), readyOnly));
                }
                return isoResponses;
            } else {
                return createIsoResponses(iso, zoneId, readyOnly);
            }
        }
    }

    private List<? extends TemplateResponse> createS3IsoResponses(final VirtualMachineTemplate iso) {

        final VMTemplateS3VO s3Iso = ApiDBUtils.findTemplateS3Ref(iso.getId());

        if (s3Iso == null) {
            return emptyList();
        }

        final TemplateResponse templateResponse = new TemplateResponse();

        templateResponse.setId(iso.getUuid());
        templateResponse.setName(iso.getName());
        templateResponse.setDisplayText(iso.getDisplayText());
        templateResponse.setPublic(iso.isPublicTemplate());
        templateResponse.setExtractable(iso.isExtractable());
        templateResponse.setCreated(s3Iso.getCreated());
        templateResponse.setReady(true);
        templateResponse.setBootable(iso.isBootable());
        templateResponse.setFeatured(iso.isFeatured());
        templateResponse.setCrossZones(iso.isCrossZones());
        templateResponse.setChecksum(iso.getChecksum());
        templateResponse.setDetails(iso.getDetails());

        final GuestOS os = ApiDBUtils.findGuestOSById(iso.getGuestOSId());

        if (os != null) {
            templateResponse.setOsTypeId(os.getUuid());
            templateResponse.setOsTypeName(os.getDisplayName());
        } else {
            templateResponse.setOsTypeId("");
            templateResponse.setOsTypeName("");
        }

        final Account account = ApiDBUtils.findAccountByIdIncludingRemoved(iso.getAccountId());
        populateAccount(templateResponse, account.getId());
        populateDomain(templateResponse, account.getDomainId());

        boolean isAdmin = false;
        if ((account == null) || BaseCmd.isAdmin(account.getType())) {
            isAdmin = true;
        }

        // If the user is an admin, add the template download status
        if (isAdmin || account.getId() == iso.getAccountId()) {
            // add download status
            templateResponse.setStatus("Successfully Installed");
        }

        final Long isoSize = s3Iso.getSize();
        if (isoSize > 0) {
            templateResponse.setSize(isoSize);
        }

        templateResponse.setObjectName("iso");

        return singletonList(templateResponse);

    }

    private List<TemplateResponse> createSwiftIsoResponses(VirtualMachineTemplate iso) {
        long isoId = iso.getId();
        List<TemplateResponse> isoResponses = new ArrayList<TemplateResponse>();
        VMTemplateSwiftVO isoSwift = ApiDBUtils.findTemplateSwiftRef(isoId);
        if (isoSwift == null) {
            return isoResponses;
        }
        TemplateResponse isoResponse = new TemplateResponse();
        isoResponse.setId(iso.getUuid());
        isoResponse.setName(iso.getName());
        isoResponse.setDisplayText(iso.getDisplayText());
        isoResponse.setPublic(iso.isPublicTemplate());
        isoResponse.setExtractable(iso.isExtractable() && !(iso.getTemplateType() == TemplateType.PERHOST));
        isoResponse.setCreated(isoSwift.getCreated());
        isoResponse.setReady(true);
        isoResponse.setBootable(iso.isBootable());
        isoResponse.setFeatured(iso.isFeatured());
        isoResponse.setCrossZones(iso.isCrossZones());
        isoResponse.setPublic(iso.isPublicTemplate());
        isoResponse.setChecksum(iso.getChecksum());
        isoResponse.setDetails(iso.getDetails());

        // TODO: implement
        GuestOS os = ApiDBUtils.findGuestOSById(iso.getGuestOSId());
        if (os != null) {
            isoResponse.setOsTypeId(os.getUuid());
            isoResponse.setOsTypeName(os.getDisplayName());
        } else {
            isoResponse.setOsTypeId("-1");
            isoResponse.setOsTypeName("");
        }
        Account account = ApiDBUtils.findAccountByIdIncludingRemoved(iso.getAccountId());
        populateAccount(isoResponse, account.getId());
        populateDomain(isoResponse, account.getDomainId());
        boolean isAdmin = false;
        if ((account == null) || BaseCmd.isAdmin(account.getType())) {
            isAdmin = true;
        }

        // If the user is an admin, add the template download status
        if (isAdmin || account.getId() == iso.getAccountId()) {
            // add download status
            isoResponse.setStatus("Successfully Installed");
        }
        Long isoSize = isoSwift.getSize();
        if (isoSize > 0) {
            isoResponse.setSize(isoSize);
        }
        isoResponse.setObjectName("iso");
        isoResponses.add(isoResponse);
        return isoResponses;
    }

    @Override
    public List<TemplateResponse> createIsoResponses(VirtualMachineTemplate iso, long zoneId, boolean readyOnly) {
        long isoId = iso.getId();
        List<TemplateResponse> isoResponses = new ArrayList<TemplateResponse>();
        VMTemplateHostVO isoHost = ApiDBUtils.findTemplateHostRef(isoId, zoneId, readyOnly);
        if (isoHost == null) {
            return isoResponses;
        }
        TemplateResponse isoResponse = new TemplateResponse();
        isoResponse.setId(iso.getUuid());
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
        isoResponse.setChecksum(iso.getChecksum());
        isoResponse.setDetails(iso.getDetails());

        // TODO: implement
        GuestOS os = ApiDBUtils.findGuestOSById(iso.getGuestOSId());
        if (os != null) {
            isoResponse.setOsTypeId(os.getUuid());
            isoResponse.setOsTypeName(os.getDisplayName());
        } else {
            isoResponse.setOsTypeId("-1");
            isoResponse.setOsTypeName("");
        }

        Account account = ApiDBUtils.findAccountByIdIncludingRemoved(iso.getAccountId());
        populateAccount(isoResponse, account.getId());
        populateDomain(isoResponse, account.getDomainId());

        Account caller = UserContext.current().getCaller();
        boolean isAdmin = false;
        if ((caller == null) || BaseCmd.isAdmin(caller.getType())) {
            isAdmin = true;
        }
        // Add the zone ID
        DataCenter datacenter = ApiDBUtils.findZoneById(zoneId);
        if (datacenter != null) {
            isoResponse.setZoneId(datacenter.getUuid());
            isoResponse.setZoneName(datacenter.getName());
        }

        // If the user is an admin, add the template download status
        if (isAdmin || caller.getId() == iso.getAccountId()) {
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

        //set tag information
        List<? extends ResourceTag> tags =  ApiDBUtils.listByResourceTypeAndId(TaggedResourceType.ISO, iso.getId());

        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            tagResponses.add(tagResponse);
        }
        isoResponse.setTags(tagResponses);

        isoResponse.setObjectName("iso");
        isoResponses.add(isoResponse);
        return isoResponses;
    }



    @Override
    public SecurityGroupResponse createSecurityGroupResponse(SecurityGroup group) {
        List<SecurityGroupJoinVO> viewSgs = ApiDBUtils.newSecurityGroupView(group);
        List<SecurityGroupResponse> listSgs = ViewResponseHelper.createSecurityGroupResponses(viewSgs);
        assert listSgs != null && listSgs.size() == 1 : "There should be one security group returned";
        return listSgs.get(0);
    }

    @Override
    public ExtractResponse createExtractResponse(Long uploadId, Long id, Long zoneId, Long accountId, String mode) {
        UploadVO uploadInfo = ApiDBUtils.findUploadById(uploadId);
        ExtractResponse response = new ExtractResponse();
        response.setObjectName("template");
        VMTemplateVO template = ApiDBUtils.findTemplateById(id);
        response.setId(template.getUuid());
        response.setName(template.getName());
        if (zoneId != null) {
            DataCenter zone = ApiDBUtils.findZoneById(zoneId);
            response.setZoneId(zone.getUuid());
            response.setZoneName(zone.getName());
        }
        response.setMode(mode);
        response.setUploadId(uploadInfo.getUuid());
        response.setState(uploadInfo.getUploadState().toString());
        Account account = ApiDBUtils.findAccountById(accountId);
        response.setAccountId(account.getUuid());
        response.setUrl(uploadInfo.getUploadUrl());
        return response;

    }

    @Override
    public String toSerializedString(CreateCmdResponse response, String responseType) {
        return ApiResponseSerializer.toSerializedString(response, responseType);
    }

    @Override
    public AsyncJobResponse createAsyncJobResponse(AsyncJob job) {
        AsyncJobJoinVO vJob = ApiDBUtils.newAsyncJobView(job);
        return ApiDBUtils.newAsyncJobResponse(vJob);
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
        EventJoinVO vEvent = ApiDBUtils.newEventView(event);
        return ApiDBUtils.newEventResponse(vEvent);
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

            // check if zone exist
            DataCenter zone = ApiDBUtils.findZoneById(capacity.getDataCenterId());
            if (zone == null) {
                continue;
            }

            short capacityType = capacity.getCapacityType();

            // If local storage then ignore
            if ((capacityType == Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED || capacityType == Capacity.CAPACITY_TYPE_STORAGE)
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
                // accounts
                // for
                // stopped
                // vms
                // that
                // have been
                // stopped
                // within
                // an
                // interval
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
                    // vms
                    // that
                    // have
                    // been
                    // stopped
                    // within
                    // an
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

        for (Capacity summedCapacity : result) {
            CapacityResponse capacityResponse = new CapacityResponse();
            capacityResponse.setCapacityTotal(summedCapacity.getTotalCapacity());
            capacityResponse.setCapacityType(summedCapacity.getCapacityType());
            capacityResponse.setCapacityUsed(summedCapacity.getUsedCapacity());
            if (summedCapacity.getPodId() != null) {
                capacityResponse.setPodId(ApiDBUtils.findPodById(summedCapacity.getPodId()).getUuid());
                HostPodVO pod = ApiDBUtils.findPodById(summedCapacity.getPodId());
                if (pod != null) {
                    capacityResponse.setPodId(pod.getUuid());
                    capacityResponse.setPodName(pod.getName());
                }
            }
            if (summedCapacity.getClusterId() != null) {
                ClusterVO cluster = ApiDBUtils.findClusterById(summedCapacity.getClusterId());
                if (cluster != null) {
                    capacityResponse.setClusterId(cluster.getUuid());
                    capacityResponse.setClusterName(cluster.getName());
                    if (summedCapacity.getPodId() == null) {
                        HostPodVO pod = ApiDBUtils.findPodById(cluster.getPodId());
                        capacityResponse.setPodId(pod.getUuid());
                        capacityResponse.setPodName(pod.getName());
                    }
                }
            }
            DataCenter zone = ApiDBUtils.findZoneById(summedCapacity.getDataCenterId());
            if (zone != null) {
                capacityResponse.setZoneId(zone.getUuid());
                capacityResponse.setZoneName(zone.getName());
            }
            if (summedCapacity.getUsedPercentage() != null){
                capacityResponse.setPercentUsed(format.format(summedCapacity.getUsedPercentage() * 100f));
            } else if (summedCapacity.getTotalCapacity() != 0) {
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
        Account templateOwner = ApiDBUtils.findAccountById(template.getAccountId());
        if (isAdmin) {
            // FIXME: we have just template id and need to get template owner from that
            if (templateOwner != null) {
                templateOwnerDomain = templateOwner.getDomainId();
            }
        }

        TemplatePermissionsResponse response = new TemplatePermissionsResponse();
        response.setId(template.getUuid());
        response.setPublicTemplate(template.isPublicTemplate());
        if (isAdmin && (templateOwnerDomain != null)) {
            Domain domain = ApiDBUtils.findDomainById(templateOwnerDomain);
            if (domain != null) {
                response.setDomainId(domain.getUuid());
            }
        }

        // Set accounts
        List<String> projectIds = new ArrayList<String>();
        List<String> regularAccounts = new ArrayList<String>();
        for (String accountName : accountNames) {
            Account account = ApiDBUtils.findAccountByNameDomain(accountName, templateOwner.getDomainId());
            if (account.getType() != Account.ACCOUNT_TYPE_PROJECT) {
                regularAccounts.add(accountName);
            } else {
                // convert account to projectIds
                Project project = ApiDBUtils.findProjectByProjectAccountIdIncludingRemoved(account.getId());

                if (project.getUuid() != null && !project.getUuid().isEmpty())
                    projectIds.add(project.getUuid());
                else
                    projectIds.add(String.valueOf(project.getId()));
            }
        }

        if (!projectIds.isEmpty()) {
            response.setProjectIds(projectIds);
        }

        if (!regularAccounts.isEmpty()) {
            response.setAccountNames(regularAccounts);
        }

        response.setObjectName("templatepermission");
        return response;
    }

    @Override
    public AsyncJobResponse queryJobResult(QueryAsyncJobResultCmd cmd) {
        AsyncJob result = ApiDBUtils._asyncMgr.queryAsyncJobResult(cmd);
        return createAsyncJobResponse(result);
    }

    @Override
    public SecurityGroupResponse createSecurityGroupResponseFromSecurityGroupRule(List<? extends SecurityRule> securityRules) {
        SecurityGroupResponse response = new SecurityGroupResponse();
        Map<Long, Account> securiytGroupAccounts = new HashMap<Long, Account>();
        Map<Long, SecurityGroup> allowedSecurityGroups = new HashMap<Long, SecurityGroup>();
        Map<Long, Account> allowedSecuriytGroupAccounts = new HashMap<Long, Account>();

        if ((securityRules != null) && !securityRules.isEmpty()) {
            SecurityGroupJoinVO securityGroup = ApiDBUtils.findSecurityGroupViewById(securityRules.get(0).getSecurityGroupId()).get(0);
            response.setId(securityGroup.getUuid());
            response.setName(securityGroup.getName());
            response.setDescription(securityGroup.getDescription());

            Account account = securiytGroupAccounts.get(securityGroup.getAccountId());

            if (securityGroup.getAccountType() == Account.ACCOUNT_TYPE_PROJECT) {
                response.setProjectId(securityGroup.getProjectUuid());
                response.setProjectName(securityGroup.getProjectName());
            } else {
                response.setAccountName(securityGroup.getAccountName());
            }

            response.setDomainId(securityGroup.getDomainUuid());
            response.setDomainName(securityGroup.getDomainName());

            for (SecurityRule securityRule : securityRules) {
                SecurityGroupRuleResponse securityGroupData = new SecurityGroupRuleResponse();

                securityGroupData.setRuleId(securityRule.getUuid());
                securityGroupData.setProtocol(securityRule.getProtocol());
                if ("icmp".equalsIgnoreCase(securityRule.getProtocol())) {
                    securityGroupData.setIcmpType(securityRule.getStartPort());
                    securityGroupData.setIcmpCode(securityRule.getEndPort());
                } else {
                    securityGroupData.setStartPort(securityRule.getStartPort());
                    securityGroupData.setEndPort(securityRule.getEndPort());
                }

                Long allowedSecurityGroupId = securityRule.getAllowedNetworkId();
                if (allowedSecurityGroupId != null) {
                    List<SecurityGroupJoinVO> sgs = ApiDBUtils.findSecurityGroupViewById(allowedSecurityGroupId);
                    if (sgs != null && sgs.size() > 0) {
                        SecurityGroupJoinVO sg = sgs.get(0);
                        securityGroupData.setSecurityGroupName(sg.getName());
                        securityGroupData.setAccountName(sg.getAccountName());
                    }
                } else {
                    securityGroupData.setCidr(securityRule.getAllowedSourceIpCidr());
                }
                if (securityRule.getRuleType() == SecurityRuleType.IngressRule) {
                    securityGroupData.setObjectName("ingressrule");
                    response.addSecurityGroupIngressRule(securityGroupData);
                } else {
                    securityGroupData.setObjectName("egressrule");
                    response.addSecurityGroupEgressRule(securityGroupData);
                }

            }
            response.setObjectName("securitygroup");

        }
        return response;
    }

    @Override
    public NetworkOfferingResponse createNetworkOfferingResponse(NetworkOffering offering) {
        NetworkOfferingResponse response = new NetworkOfferingResponse();
        response.setId(offering.getUuid());
        response.setName(offering.getName());
        response.setDisplayText(offering.getDisplayText());
        response.setTags(offering.getTags());
        response.setTrafficType(offering.getTrafficType().toString());
        response.setIsDefault(offering.isDefault());
        response.setSpecifyVlan(offering.getSpecifyVlan());
        response.setConserveMode(offering.isConserveMode());
        response.setSpecifyIpRanges(offering.getSpecifyIpRanges());
        response.setAvailability(offering.getAvailability().toString());
        response.setIsPersistent(offering.getIsPersistent());
        response.setNetworkRate(ApiDBUtils.getNetworkRate(offering.getId()));
        Long so = null;
        if (offering.getServiceOfferingId() != null) {
            so = offering.getServiceOfferingId();
        } else {
            so = ApiDBUtils.findDefaultRouterServiceOffering();
        }
        if (so != null) {
            ServiceOffering soffering = ApiDBUtils.findServiceOfferingById(so);
            if (soffering != null)
                response.setServiceOfferingId(soffering.getUuid());
        }

        if (offering.getGuestType() != null) {
            response.setGuestIpType(offering.getGuestType().toString());
        }

        response.setState(offering.getState().name());

        Map<Service, Set<Provider>> serviceProviderMap = ApiDBUtils.listNetworkOfferingServices(offering.getId());
        List<ServiceResponse> serviceResponses = new ArrayList<ServiceResponse>();
        for (Service service : serviceProviderMap.keySet()) {
            ServiceResponse svcRsp = new ServiceResponse();
            // skip gateway service
            if (service == Service.Gateway) {
                continue;
            }
            svcRsp.setName(service.getName());
            List<ProviderResponse> providers = new ArrayList<ProviderResponse>();
            for (Provider provider : serviceProviderMap.get(service)) {
                if (provider != null) {
                    ProviderResponse providerRsp = new ProviderResponse();
                    providerRsp.setName(provider.getName());
                    providers.add(providerRsp);
                }
            }
            svcRsp.setProviders(providers);

            if (Service.Lb == service) {
                List<CapabilityResponse> lbCapResponse = new ArrayList<CapabilityResponse>();

                CapabilityResponse lbIsoaltion = new CapabilityResponse();
                lbIsoaltion.setName(Capability.SupportedLBIsolation.getName());
                lbIsoaltion.setValue(offering.getDedicatedLB() ? "dedicated" : "shared");
                lbCapResponse.add(lbIsoaltion);

                CapabilityResponse eLb = new CapabilityResponse();
                eLb.setName(Capability.ElasticLb.getName());
                eLb.setValue(offering.getElasticLb() ? "true" : "false");
                lbCapResponse.add(eLb);

		CapabilityResponse inline = new CapabilityResponse();
		inline.setName(Capability.InlineMode.getName());
		inline.setValue(offering.isInline() ? "true" : "false");
		lbCapResponse.add(inline);

                svcRsp.setCapabilities(lbCapResponse);
            } else if (Service.SourceNat == service) {
                List<CapabilityResponse> capabilities = new ArrayList<CapabilityResponse>();
                CapabilityResponse sharedSourceNat = new CapabilityResponse();
                sharedSourceNat.setName(Capability.SupportedSourceNatTypes.getName());
                sharedSourceNat.setValue(offering.getSharedSourceNat() ? "perzone" : "peraccount");
                capabilities.add(sharedSourceNat);

                CapabilityResponse redundantRouter = new CapabilityResponse();
                redundantRouter.setName(Capability.RedundantRouter.getName());
                redundantRouter.setValue(offering.getRedundantRouter() ? "true" : "false");
                capabilities.add(redundantRouter);

                svcRsp.setCapabilities(capabilities);
            } else if (service == Service.StaticNat) {
                List<CapabilityResponse> staticNatCapResponse = new ArrayList<CapabilityResponse>();

                CapabilityResponse eIp = new CapabilityResponse();
                eIp.setName(Capability.ElasticIp.getName());
                eIp.setValue(offering.getElasticLb() ? "true" : "false");
                staticNatCapResponse.add(eIp);

                svcRsp.setCapabilities(staticNatCapResponse);
            }

            serviceResponses.add(svcRsp);
        }
        response.setForVpc(ApiDBUtils.isOfferingForVpc(offering));

        response.setServices(serviceResponses);
        response.setObjectName("networkoffering");
        return response;
    }

    @Override
    public NetworkResponse createNetworkResponse(Network network) {
        // need to get network profile in order to retrieve dns information from there
        NetworkProfile profile = ApiDBUtils.getNetworkProfile(network.getId());
        NetworkResponse response = new NetworkResponse();
        response.setId(network.getUuid());
        response.setName(network.getName());
        response.setDisplaytext(network.getDisplayText());
        if (network.getBroadcastDomainType() != null) {
            response.setBroadcastDomainType(network.getBroadcastDomainType().toString());
        }

        if (network.getTrafficType() != null) {
            response.setTrafficType(network.getTrafficType().name());
        }

        if (network.getGuestType() != null) {
            response.setType(network.getGuestType().toString());
        }

        response.setGateway(network.getGateway());

        // FIXME - either set netmask or cidr
        response.setCidr(network.getCidr());
        response.setNetworkCidr((network.getNetworkCidr()));
        // If network has reservation its entire network cidr is defined by getNetworkCidr()
        // if no reservation is present then getCidr() will define the entire network cidr
        if (network.getNetworkCidr() != null) {
            response.setNetmask(NetUtils.cidr2Netmask(network.getNetworkCidr()));
        }
        if (((network.getCidr()) != null) && (network.getNetworkCidr() == null)) {
            response.setNetmask(NetUtils.cidr2Netmask(network.getCidr()));
        }
        
        response.setIp6Gateway(network.getIp6Gateway());
        response.setIp6Cidr(network.getIp6Cidr());

        // create response for reserved IP ranges that can be used for non-cloudstack purposes
        String  reservation = null;
        if ((network.getCidr() != null) && (NetUtils.isNetworkAWithinNetworkB(network.getCidr(), network.getNetworkCidr()))) {
                String[] guestVmCidrPair = network.getCidr().split("\\/");
                String[] guestCidrPair = network.getNetworkCidr().split("\\/");

                Long guestVmCidrSize = Long.valueOf(guestVmCidrPair[1]);
                Long guestCidrSize = Long.valueOf(guestCidrPair[1]);

                String[] guestVmIpRange = NetUtils.getIpRangeFromCidr(guestVmCidrPair[0], guestVmCidrSize);
                String[] guestIpRange = NetUtils.getIpRangeFromCidr(guestCidrPair[0], guestCidrSize);
                long startGuestIp = NetUtils.ip2Long(guestIpRange[0]);
                long endGuestIp = NetUtils.ip2Long(guestIpRange[1]);
                long startVmIp = NetUtils.ip2Long(guestVmIpRange[0]);
                long endVmIp = NetUtils.ip2Long(guestVmIpRange[1]);

                if (startVmIp == startGuestIp && endVmIp < endGuestIp -1) {
                    reservation = (NetUtils.long2Ip(endVmIp + 1) + "-" + NetUtils.long2Ip(endGuestIp));
                }
                if (endVmIp == endGuestIp && startVmIp > startGuestIp + 1) {
                    reservation = (NetUtils.long2Ip(startGuestIp) + "-" + NetUtils.long2Ip(startVmIp-1));
                }
                if(startVmIp > startGuestIp + 1 && endVmIp < endGuestIp - 1) {
                reservation = (NetUtils.long2Ip(startGuestIp) + "-" +  NetUtils.long2Ip(startVmIp-1) + " ,  " +
                        NetUtils.long2Ip(endVmIp + 1) + "-"+  NetUtils.long2Ip(endGuestIp));
            }
        }
        response.setReservedIpRange(reservation);

        //return vlan information only to Root admin
        if (network.getBroadcastUri() != null && UserContext.current().getCaller().getType() == Account.ACCOUNT_TYPE_ADMIN) {
            String broadcastUri = network.getBroadcastUri().toString();
            response.setBroadcastUri(broadcastUri);
            String vlan="N/A";
            if (broadcastUri.startsWith("vlan")) {
                vlan = broadcastUri.substring("vlan://".length(), broadcastUri.length());
            }
            //return vlan information only to Root admin
            response.setVlan(vlan);

        }

        DataCenter zone = ApiDBUtils.findZoneById(network.getDataCenterId());
        if (zone != null) {
            response.setZoneId(zone.getUuid());
            response.setZoneName(zone.getName());
        }
        if (network.getPhysicalNetworkId() != null) {
            PhysicalNetworkVO pnet = ApiDBUtils.findPhysicalNetworkById(network.getPhysicalNetworkId());
            response.setPhysicalNetworkId(pnet.getUuid());
        }

        // populate network offering information
        NetworkOffering networkOffering = (NetworkOffering) ApiDBUtils.findNetworkOfferingById(network.getNetworkOfferingId());
        if (networkOffering != null) {
            response.setNetworkOfferingId(networkOffering.getUuid());
            response.setNetworkOfferingName(networkOffering.getName());
            response.setNetworkOfferingDisplayText(networkOffering.getDisplayText());
            response.setIsSystem(networkOffering.isSystemOnly());
            response.setNetworkOfferingAvailability(networkOffering.getAvailability().toString());
            response.setIsPersistent(networkOffering.getIsPersistent());
        }

        if (network.getAclType() != null) {
            response.setAclType(network.getAclType().toString());
        }
        response.setState(network.getState().toString());
        response.setRestartRequired(network.isRestartRequired());
        NetworkVO nw = ApiDBUtils.findNetworkById(network.getRelated());
        if (nw != null) {
            response.setRelated(nw.getUuid());
        }
        response.setNetworkDomain(network.getNetworkDomain());

        response.setDns1(profile.getDns1());
        response.setDns2(profile.getDns2());
        // populate capability
        Map<Service, Map<Capability, String>> serviceCapabilitiesMap = ApiDBUtils.getNetworkCapabilities(network.getId(), network.getDataCenterId());
        List<ServiceResponse> serviceResponses = new ArrayList<ServiceResponse>();
        if (serviceCapabilitiesMap != null) {
            for (Service service : serviceCapabilitiesMap.keySet()) {
                ServiceResponse serviceResponse = new ServiceResponse();
                // skip gateway service
                if (service == Service.Gateway) {
                    continue;
                }
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

        if (network.getAclType() == null || network.getAclType() == ACLType.Account) {
            populateOwner(response, network);
        } else {
            // get domain from network_domain table
            Pair<Long, Boolean> domainNetworkDetails = ApiDBUtils.getDomainNetworkDetails(network.getId());
            if (domainNetworkDetails.first() != null) {
                Domain domain = ApiDBUtils.findDomainById(domainNetworkDetails.first());
                if (domain != null) {
                    response.setDomainId(domain.getUuid());
                }
            }
            response.setSubdomainAccess(domainNetworkDetails.second());
        }

        Long dedicatedDomainId = ApiDBUtils.getDedicatedNetworkDomain(network.getId());
        if (dedicatedDomainId != null) {
            Domain domain = ApiDBUtils.findDomainById(dedicatedDomainId);
            if (domain != null) {
                response.setDomainId(domain.getUuid());
            }
            response.setDomainName(domain.getName());
        }

        response.setSpecifyIpRanges(network.getSpecifyIpRanges());
        if (network.getVpcId() != null) {
            Vpc vpc = ApiDBUtils.findVpcById(network.getVpcId());
            if (vpc != null) {
                response.setVpcId(vpc.getUuid());
            }
        }
        response.setCanUseForDeploy(ApiDBUtils.canUseForDeploy(network));

        //set tag information
        List<? extends ResourceTag> tags = ApiDBUtils.listByResourceTypeAndId(TaggedResourceType.Network, network.getId());
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            tagResponses.add(tagResponse);
        }
        response.setTags(tagResponses);

        response.setObjectName("network");
        return response;
    }

    @Override
    public Long getSecurityGroupId(String groupName, long accountId) {
        SecurityGroup sg = ApiDBUtils.getSecurityGroup(groupName, accountId);
        if (sg == null) {
            return null;
        } else {
            return sg.getId();
        }
    }

    @Override
    public ProjectResponse createProjectResponse(Project project) {
        List<ProjectJoinVO> viewPrjs = ApiDBUtils.newProjectView(project);
        List<ProjectResponse> listPrjs = ViewResponseHelper.createProjectResponse(viewPrjs.toArray(new ProjectJoinVO[viewPrjs.size()]));
        assert listPrjs != null && listPrjs.size() == 1 : "There should be one project  returned";
        return listPrjs.get(0);
    }




    @Override
    public FirewallResponse createFirewallResponse(FirewallRule fwRule) {
        FirewallResponse response = new FirewallResponse();

        response.setId(fwRule.getUuid());
        response.setProtocol(fwRule.getProtocol());
        if (fwRule.getSourcePortStart() != null) {
            response.setStartPort(Integer.toString(fwRule.getSourcePortStart()));
        }

        if (fwRule.getSourcePortEnd() != null) {
            response.setEndPort(Integer.toString(fwRule.getSourcePortEnd()));
        }

        List<String> cidrs = ApiDBUtils.findFirewallSourceCidrs(fwRule.getId());
        response.setCidrList(StringUtils.join(cidrs, ","));
        
        if (fwRule.getTrafficType() == FirewallRule.TrafficType.Ingress) {
            IpAddress ip = ApiDBUtils.findIpAddressById(fwRule.getSourceIpAddressId());
            response.setPublicIpAddressId(ip.getId());
            response.setPublicIpAddress(ip.getAddress().addr());
        } else if (fwRule.getTrafficType() == FirewallRule.TrafficType.Egress) {
            response.setPublicIpAddress(null);
            response.setNetworkId(fwRule.getNetworkId());
        }

        FirewallRule.State state = fwRule.getState();
        String stateToSet = state.toString();
        if (state.equals(FirewallRule.State.Revoke)) {
            stateToSet = "Deleting";
        }

        response.setIcmpCode(fwRule.getIcmpCode());
        response.setIcmpType(fwRule.getIcmpType());

        //set tag information
        List<? extends ResourceTag> tags = ApiDBUtils.listByResourceTypeAndId(TaggedResourceType.FirewallRule, fwRule.getId());
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            tagResponses.add(tagResponse);
        }
        response.setTags(tagResponses);

        response.setState(stateToSet);
        response.setObjectName("firewallrule");
        return response;
    }

    @Override
    public NetworkACLResponse createNetworkACLResponse(FirewallRule networkACL) {
        NetworkACLResponse response = new NetworkACLResponse();

        response.setId(networkACL.getUuid());
        response.setProtocol(networkACL.getProtocol());
        if (networkACL.getSourcePortStart() != null) {
            response.setStartPort(Integer.toString(networkACL.getSourcePortStart()));
        }

        if (networkACL.getSourcePortEnd() != null) {
            response.setEndPort(Integer.toString(networkACL.getSourcePortEnd()));
        }

        List<String> cidrs = ApiDBUtils.findFirewallSourceCidrs(networkACL.getId());
        response.setCidrList(StringUtils.join(cidrs, ","));

        response.setTrafficType(networkACL.getTrafficType().toString());

        FirewallRule.State state = networkACL.getState();
        String stateToSet = state.toString();
        if (state.equals(FirewallRule.State.Revoke)) {
            stateToSet = "Deleting";
        }

        response.setIcmpCode(networkACL.getIcmpCode());
        response.setIcmpType(networkACL.getIcmpType());

        response.setState(stateToSet);

        //set tag information
        List<? extends ResourceTag> tags = ApiDBUtils.listByResourceTypeAndId(TaggedResourceType.NetworkACL, networkACL.getId());
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            tagResponses.add(tagResponse);
        }
        response.setTags(tagResponses);

        response.setObjectName("networkacl");
        return response;
    }

    @Override
    public HypervisorCapabilitiesResponse createHypervisorCapabilitiesResponse(HypervisorCapabilities hpvCapabilities) {
        HypervisorCapabilitiesResponse hpvCapabilitiesResponse = new HypervisorCapabilitiesResponse();
        hpvCapabilitiesResponse.setId(hpvCapabilities.getUuid());
        hpvCapabilitiesResponse.setHypervisor(hpvCapabilities.getHypervisorType());
        hpvCapabilitiesResponse.setHypervisorVersion(hpvCapabilities.getHypervisorVersion());
        hpvCapabilitiesResponse.setIsSecurityGroupEnabled(hpvCapabilities.isSecurityGroupEnabled());
        hpvCapabilitiesResponse.setMaxGuestsLimit(hpvCapabilities.getMaxGuestsLimit());
        return hpvCapabilitiesResponse;
    }

    // TODO: we may need to refactor once ControlledEntityResponse and ControlledEntity id to uuid conversion are all done.
    // currently code is scattered in
    private void populateOwner(ControlledEntityResponse response, ControlledEntity object) {
        Account account = ApiDBUtils.findAccountByIdIncludingRemoved(object.getAccountId());

        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            // find the project
            Project project = ApiDBUtils.findProjectByProjectAccountIdIncludingRemoved(account.getId());
            response.setProjectId(project.getUuid());
            response.setProjectName(project.getName());
        } else {
            response.setAccountName(account.getAccountName());
        }

        Domain domain = ApiDBUtils.findDomainById(object.getDomainId());
        response.setDomainId(domain.getUuid());
        response.setDomainName(domain.getName());
    }

    public static void populateOwner(ControlledViewEntityResponse response, ControlledViewEntity object) {

        if (object.getAccountType() == Account.ACCOUNT_TYPE_PROJECT) {
            response.setProjectId(object.getProjectUuid());
            response.setProjectName(object.getProjectName());
        } else {
            response.setAccountName(object.getAccountName());
        }

        response.setDomainId(object.getDomainUuid());
        response.setDomainName(object.getDomainName());
    }

    private void populateAccount(ControlledEntityResponse response, long accountId) {
        Account account = ApiDBUtils.findAccountByIdIncludingRemoved(accountId);
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            // find the project
            Project project = ApiDBUtils.findProjectByProjectAccountIdIncludingRemoved(account.getId());
            response.setProjectId(project.getUuid());
            response.setProjectName(project.getName());
        } else {
            response.setAccountName(account.getAccountName());
        }
    }

    private void populateDomain(ControlledEntityResponse response, long domainId) {
        Domain domain = ApiDBUtils.findDomainById(domainId);

        response.setDomainId(domain.getUuid());
        response.setDomainName(domain.getName());
    }

    @Override
    public ProjectAccountResponse createProjectAccountResponse(ProjectAccount projectAccount) {
        ProjectAccountJoinVO vProj = ApiDBUtils.newProjectAccountView(projectAccount);
        List<ProjectAccountResponse> listProjs = ViewResponseHelper.createProjectAccountResponse(vProj);
        assert listProjs != null && listProjs.size() == 1 : "There should be one project account returned";
        return listProjs.get(0);
    }




    @Override
    public ProjectInvitationResponse createProjectInvitationResponse(ProjectInvitation invite) {
        ProjectInvitationJoinVO vInvite = ApiDBUtils.newProjectInvitationView(invite);
        return ApiDBUtils.newProjectInvitationResponse(vInvite);
    }


    @Override
    public SystemVmInstanceResponse createSystemVmInstanceResponse(VirtualMachine vm) {
        SystemVmInstanceResponse vmResponse = new SystemVmInstanceResponse();
        vmResponse.setId(vm.getUuid());
        vmResponse.setSystemVmType(vm.getType().toString().toLowerCase());
        vmResponse.setName(vm.getHostName());
        if (vm.getHostId() != null) {
            Host host = ApiDBUtils.findHostById(vm.getHostId());
            if (host != null) {
                vmResponse.setHostId(host.getUuid());
            }
        }
        if (vm.getState() != null) {
            vmResponse.setState(vm.getState().toString());
        }
        if (vm.getType() == Type.DomainRouter) {
            VirtualRouter router = (VirtualRouter) vm;
            if (router.getRole() != null) {
                vmResponse.setRole(router.getRole().toString());
            }
        }
        vmResponse.setObjectName("systemvminstance");
        return vmResponse;
    }

    @Override
    public PhysicalNetworkResponse createPhysicalNetworkResponse(PhysicalNetwork result) {
        PhysicalNetworkResponse response = new PhysicalNetworkResponse();

        DataCenter zone = ApiDBUtils.findZoneById(result.getDataCenterId());
        if (zone != null) {
            response.setZoneId(zone.getUuid());
        }
        response.setNetworkSpeed(result.getSpeed());
        response.setVlan(result.getVnet());
        if (result.getDomainId() != null) {
            Domain domain = ApiDBUtils.findDomainById(result.getDomainId());
            if (domain != null) {
                response.setDomainId(domain.getUuid());
            }
        }
        response.setId(result.getUuid());
        if (result.getBroadcastDomainRange() != null) {
            response.setBroadcastDomainRange(result.getBroadcastDomainRange().toString());
        }
        response.setIsolationMethods(result.getIsolationMethods());
        response.setTags(result.getTags());
        if (result.getState() != null) {
            response.setState(result.getState().toString());
        }

        response.setName(result.getName());

        response.setObjectName("physicalnetwork");
        return response;
    }

    @Override
    public ServiceResponse createNetworkServiceResponse(Service service) {
        ServiceResponse response = new ServiceResponse();
        response.setName(service.getName());

        // set list of capabilities required for the service
        List<CapabilityResponse> capabilityResponses = new ArrayList<CapabilityResponse>();
        Capability[] capabilities = service.getCapabilities();
        for (Capability cap : capabilities) {
            CapabilityResponse capabilityResponse = new CapabilityResponse();
            capabilityResponse.setName(cap.getName());
            capabilityResponse.setObjectName("capability");
            if (cap.getName().equals(Capability.SupportedLBIsolation.getName()) ||
                    cap.getName().equals(Capability.SupportedSourceNatTypes.getName()) ||
                    cap.getName().equals(Capability.RedundantRouter.getName())) {
                capabilityResponse.setCanChoose(true);
            } else {
                capabilityResponse.setCanChoose(false);
            }
            capabilityResponses.add(capabilityResponse);
        }
        response.setCapabilities(capabilityResponses);

        // set list of providers providing this service
        List<? extends Network.Provider> serviceProviders = ApiDBUtils.getProvidersForService(service);
        List<ProviderResponse> serviceProvidersResponses = new ArrayList<ProviderResponse>();
        for (Network.Provider serviceProvider : serviceProviders) {
            // return only Virtual Router/JuniperSRX as a provider for the firewall
            if (service == Service.Firewall && !(serviceProvider == Provider.VirtualRouter || serviceProvider == Provider.JuniperSRX)) {
                continue;
            }

            ProviderResponse serviceProviderResponse = createServiceProviderResponse(serviceProvider);
            serviceProvidersResponses.add(serviceProviderResponse);
        }
        response.setProviders(serviceProvidersResponses);

        response.setObjectName("networkservice");
        return response;

    }

    private ProviderResponse createServiceProviderResponse(Provider serviceProvider) {
        ProviderResponse response = new ProviderResponse();
        response.setName(serviceProvider.getName());
        boolean canEnableIndividualServices = ApiDBUtils.canElementEnableIndividualServices(serviceProvider);
        response.setCanEnableIndividualServices(canEnableIndividualServices);
        return response;
    }

    @Override
    public ProviderResponse createNetworkServiceProviderResponse(PhysicalNetworkServiceProvider result) {
        ProviderResponse response = new ProviderResponse();
        response.setId(result.getUuid());
        response.setName(result.getProviderName());
        PhysicalNetwork pnw = ApiDBUtils.findPhysicalNetworkById(result.getPhysicalNetworkId());
        if (pnw != null) {
            response.setPhysicalNetworkId(pnw.getUuid());
        }
        PhysicalNetwork dnw = ApiDBUtils.findPhysicalNetworkById(result.getDestinationPhysicalNetworkId());
        if (dnw != null) {
            response.setDestinationPhysicalNetworkId(dnw.getUuid());
        }
        response.setState(result.getState().toString());

        // set enabled services
        List<String> services = new ArrayList<String>();
        for (Service service : result.getEnabledServices()) {
            services.add(service.getName());
        }
        response.setServices(services);

        response.setObjectName("networkserviceprovider");
        return response;
    }

    @Override
    public TrafficTypeResponse createTrafficTypeResponse(PhysicalNetworkTrafficType result) {
        TrafficTypeResponse response = new TrafficTypeResponse();
        response.setId(result.getUuid());
        PhysicalNetwork pnet = ApiDBUtils.findPhysicalNetworkById(result.getPhysicalNetworkId());
        if (pnet != null) {
            response.setPhysicalNetworkId(pnet.getUuid());
        }
        if (result.getTrafficType() != null) {
            response.setTrafficType(result.getTrafficType().toString());
        }

        response.setXenLabel(result.getXenNetworkLabel());
        response.setKvmLabel(result.getKvmNetworkLabel());
        response.setVmwareLabel(result.getVmwareNetworkLabel());

        response.setObjectName("traffictype");
        return response;
    }

    @Override
    public VirtualRouterProviderResponse createVirtualRouterProviderResponse(VirtualRouterProvider result) {
        VirtualRouterProviderResponse response = new VirtualRouterProviderResponse();
        response.setId(result.getUuid());
        PhysicalNetworkServiceProvider nsp = ApiDBUtils.findPhysicalNetworkServiceProviderById(result.getNspId());
        if (nsp != null) {
            response.setNspId(nsp.getUuid());
        }
        response.setEnabled(result.isEnabled());

        response.setObjectName("virtualrouterelement");
        return response;
    }

    @Override
    public LBStickinessResponse createLBStickinessPolicyResponse(
            StickinessPolicy stickinessPolicy, LoadBalancer lb) {
        LBStickinessResponse spResponse = new LBStickinessResponse();

        spResponse.setlbRuleId(lb.getUuid());
        Account accountTemp = ApiDBUtils.findAccountById(lb.getAccountId());
        if (accountTemp != null) {
            spResponse.setAccountName(accountTemp.getAccountName());
            Domain domain = ApiDBUtils.findDomainById(accountTemp.getDomainId());
            if (domain != null) {
                spResponse.setDomainId(domain.getUuid());
                spResponse.setDomainName(domain.getName());
            }
        }

        List<LBStickinessPolicyResponse> responses = new ArrayList<LBStickinessPolicyResponse>();
        LBStickinessPolicyResponse ruleResponse = new LBStickinessPolicyResponse(
                stickinessPolicy);
        responses.add(ruleResponse);

        spResponse.setRules(responses);

        spResponse.setObjectName("stickinesspolicies");
        return spResponse;
    }

    @Override
    public LBStickinessResponse createLBStickinessPolicyResponse(
            List<? extends StickinessPolicy> stickinessPolicies, LoadBalancer lb) {
        LBStickinessResponse spResponse = new LBStickinessResponse();

        if (lb == null)
            return spResponse;
        spResponse.setlbRuleId(lb.getUuid());
        Account account = ApiDBUtils.findAccountById(lb.getAccountId());
        if (account != null) {
            spResponse.setAccountName(account.getAccountName());
            Domain domain = ApiDBUtils.findDomainById(account.getDomainId());
            if (domain != null) {
                spResponse.setDomainId(domain.getUuid());
                spResponse.setDomainName(domain.getName());
            }
        }

        List<LBStickinessPolicyResponse> responses = new ArrayList<LBStickinessPolicyResponse>();
        for (StickinessPolicy stickinessPolicy : stickinessPolicies) {
            LBStickinessPolicyResponse ruleResponse = new LBStickinessPolicyResponse(stickinessPolicy);
            responses.add(ruleResponse);
        }
        spResponse.setRules(responses);

        spResponse.setObjectName("stickinesspolicies");
        return spResponse;
    }

    @Override
    public LBHealthCheckResponse createLBHealthCheckPolicyResponse(
            List<? extends HealthCheckPolicy> healthcheckPolicies, LoadBalancer lb) {
        LBHealthCheckResponse hcResponse = new LBHealthCheckResponse();

        if (lb == null)
            return hcResponse;
        hcResponse.setlbRuleId(lb.getUuid());
        Account account = ApiDBUtils.findAccountById(lb.getAccountId());
        if (account != null) {
            hcResponse.setAccountName(account.getAccountName());
            Domain domain = ApiDBUtils.findDomainById(account.getDomainId());
            if (domain != null) {
                hcResponse.setDomainId(domain.getUuid());
                hcResponse.setDomainName(domain.getName());
            }
        }

        List<LBHealthCheckPolicyResponse> responses = new ArrayList<LBHealthCheckPolicyResponse>();
        for (HealthCheckPolicy healthcheckPolicy : healthcheckPolicies) {
            LBHealthCheckPolicyResponse ruleResponse = new LBHealthCheckPolicyResponse(healthcheckPolicy);
            responses.add(ruleResponse);
        }
        hcResponse.setRules(responses);

        hcResponse.setObjectName("healthcheckpolicies");
        return hcResponse;
    }

    @Override
    public LBHealthCheckResponse createLBHealthCheckPolicyResponse(HealthCheckPolicy healthcheckPolicy, LoadBalancer lb) {
        LBHealthCheckResponse hcResponse = new LBHealthCheckResponse();

        hcResponse.setlbRuleId(lb.getUuid());
        Account accountTemp = ApiDBUtils.findAccountById(lb.getAccountId());
        if (accountTemp != null) {
            hcResponse.setAccountName(accountTemp.getAccountName());
            Domain domain = ApiDBUtils.findDomainById(accountTemp.getDomainId());
            if (domain != null) {
                hcResponse.setDomainId(domain.getUuid());
                hcResponse.setDomainName(domain.getName());
            }
        }

        List<LBHealthCheckPolicyResponse> responses = new ArrayList<LBHealthCheckPolicyResponse>();
        LBHealthCheckPolicyResponse ruleResponse = new LBHealthCheckPolicyResponse(healthcheckPolicy);
        responses.add(ruleResponse);
        hcResponse.setRules(responses);
        hcResponse.setObjectName("healthcheckpolicies");
        return hcResponse;
    }

    @Override
    public LDAPConfigResponse createLDAPConfigResponse(String hostname,
            Integer port, Boolean useSSL, String queryFilter,
            String searchBase, String bindDN) {
        LDAPConfigResponse lr = new LDAPConfigResponse();
        lr.setHostname(hostname);
        lr.setPort(port.toString());
        lr.setUseSSL(useSSL.toString());
        lr.setQueryFilter(queryFilter);
        lr.setBindDN(bindDN);
        lr.setSearchBase(searchBase);
        lr.setObjectName("ldapconfig");
        return lr;
    }

    @Override
    public StorageNetworkIpRangeResponse createStorageNetworkIpRangeResponse(StorageNetworkIpRange result) {
        StorageNetworkIpRangeResponse response = new StorageNetworkIpRangeResponse();
        response.setUuid(result.getUuid());
        response.setVlan(result.getVlan());
        response.setEndIp(result.getEndIp());
        response.setStartIp(result.getStartIp());
        response.setPodUuid(result.getPodUuid());
        response.setZoneUuid(result.getZoneUuid());
        response.setNetworkUuid(result.getNetworkUuid());
        response.setNetmask(result.getNetmask());
        response.setGateway(result.getGateway());
        response.setObjectName("storagenetworkiprange");
        return response;
    }

	@Override
	public RegionResponse createRegionResponse(Region region) {
		RegionResponse response = new RegionResponse();
		response.setId(region.getId());
		response.setName(region.getName());
		response.setEndPoint(region.getEndPoint());
		response.setObjectName("region");
		return response;
	}

    @Override
    public ResourceTagResponse createResourceTagResponse(ResourceTag resourceTag, boolean keyValueOnly) {
        ResourceTagJoinVO rto = ApiDBUtils.newResourceTagView(resourceTag);
        return ApiDBUtils.newResourceTagResponse(rto, keyValueOnly);
    }



    @Override
    public VpcOfferingResponse createVpcOfferingResponse(VpcOffering offering) {
        VpcOfferingResponse response = new VpcOfferingResponse();
        response.setId(offering.getUuid());
        response.setName(offering.getName());
        response.setDisplayText(offering.getDisplayText());
        response.setIsDefault(offering.isDefault());
        response.setState(offering.getState().name());

        Map<Service, Set<Provider>> serviceProviderMap = ApiDBUtils.listVpcOffServices(offering.getId());
        List<ServiceResponse> serviceResponses = new ArrayList<ServiceResponse>();
        for (Service service : serviceProviderMap.keySet()) {
            ServiceResponse svcRsp = new ServiceResponse();
            // skip gateway service
            if (service == Service.Gateway) {
                continue;
            }
            svcRsp.setName(service.getName());
            List<ProviderResponse> providers = new ArrayList<ProviderResponse>();
            for (Provider provider : serviceProviderMap.get(service)) {
                if (provider != null) {
                    ProviderResponse providerRsp = new ProviderResponse();
                    providerRsp.setName(provider.getName());
                    providers.add(providerRsp);
                }
            }
            svcRsp.setProviders(providers);

            serviceResponses.add(svcRsp);
        }
        response.setServices(serviceResponses);
        response.setObjectName("vpcoffering");
        return response;
    }


    @Override
    public VpcResponse createVpcResponse(Vpc vpc) {
        VpcResponse response = new VpcResponse();
        response.setId(vpc.getUuid());
        response.setName(vpc.getName());
        response.setDisplayText(vpc.getDisplayText());
        response.setState(vpc.getState().name());
        VpcOffering voff = ApiDBUtils.findVpcOfferingById(vpc.getVpcOfferingId());
        if (voff != null) {
            response.setVpcOfferingId(voff.getUuid());
        }
        response.setCidr(vpc.getCidr());
        response.setRestartRequired(vpc.isRestartRequired());
        response.setNetworkDomain(vpc.getNetworkDomain());

        Map<Service, Set<Provider>> serviceProviderMap = ApiDBUtils.listVpcOffServices(vpc.getVpcOfferingId());
        List<ServiceResponse> serviceResponses = new ArrayList<ServiceResponse>();
        for (Service service : serviceProviderMap.keySet()) {
            ServiceResponse svcRsp = new ServiceResponse();
            // skip gateway service
            if (service == Service.Gateway) {
                continue;
            }
            svcRsp.setName(service.getName());
            List<ProviderResponse> providers = new ArrayList<ProviderResponse>();
            for (Provider provider : serviceProviderMap.get(service)) {
                if (provider != null) {
                    ProviderResponse providerRsp = new ProviderResponse();
                    providerRsp.setName(provider.getName());
                    providers.add(providerRsp);
                }
            }
            svcRsp.setProviders(providers);

            serviceResponses.add(svcRsp);
        }

        List<NetworkResponse> networkResponses = new ArrayList<NetworkResponse>();
        List<? extends Network> networks = ApiDBUtils.listVpcNetworks(vpc.getId());
        for (Network network : networks) {
            NetworkResponse ntwkRsp = createNetworkResponse(network);
            networkResponses.add(ntwkRsp);
        }

        DataCenter zone = ApiDBUtils.findZoneById(vpc.getZoneId());
        if (zone != null) {
            response.setZoneId(zone.getUuid());
            response.setZoneName(zone.getName());
        }

        response.setNetworks(networkResponses);
        response.setServices(serviceResponses);
        populateOwner(response, vpc);

        //set tag information
        List<? extends ResourceTag> tags = ApiDBUtils.listByResourceTypeAndId(TaggedResourceType.Vpc, vpc.getId());
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            tagResponses.add(tagResponse);
        }
        response.setTags(tagResponses);
        response.setObjectName("vpc");
        return response;
    }

    @Override
    public PrivateGatewayResponse createPrivateGatewayResponse(PrivateGateway result) {
        PrivateGatewayResponse response = new PrivateGatewayResponse();
        response.setId(result.getUuid());
        response.setVlan(result.getVlanTag());
        response.setGateway(result.getGateway());
        response.setNetmask(result.getNetmask());
        if (result.getVpcId() != null) {
            Vpc vpc = ApiDBUtils.findVpcById(result.getVpcId());
            response.setVpcId(vpc.getUuid());
        }

        DataCenter zone = ApiDBUtils.findZoneById(result.getZoneId());
        if (zone != null) {
            response.setZoneId(zone.getUuid());
            response.setZoneName(zone.getName());
        }
        response.setAddress(result.getIp4Address());
        PhysicalNetwork pnet = ApiDBUtils.findPhysicalNetworkById(result.getPhysicalNetworkId());
        if (pnet != null) {
            response.setPhysicalNetworkId(pnet.getUuid());
        }

        populateAccount(response, result.getAccountId());
        populateDomain(response, result.getDomainId());
        response.setState(result.getState().toString());

        response.setObjectName("privategateway");


        return response;
    }

    @Override
    public CounterResponse createCounterResponse(Counter counter) {
        CounterResponse response = new CounterResponse();
        response.setId(counter.getUuid());
        response.setSource(counter.getSource().toString());
        response.setName(counter.getName());
        response.setValue(counter.getValue());
        response.setObjectName("counter");
        return response;
    }

    @Override
    public ConditionResponse createConditionResponse(Condition condition) {
        ConditionResponse response = new ConditionResponse();
        response.setId(condition.getUuid());
        List<CounterResponse> counterResponseList = new ArrayList<CounterResponse>();
        counterResponseList.add(createCounterResponse(ApiDBUtils.getCounter(condition.getCounterid())));
        response.setCounterResponse(counterResponseList);
        response.setRelationalOperator(condition.getRelationalOperator().toString());
        response.setThreshold(condition.getThreshold());
        response.setObjectName("condition");
        populateOwner(response, condition);
        return response;
    }

    @Override
    public AutoScaleVmProfileResponse createAutoScaleVmProfileResponse(AutoScaleVmProfile profile) {
        AutoScaleVmProfileResponse response = new AutoScaleVmProfileResponse();
        response.setId(profile.getUuid());
        if (profile.getZoneId() != null) {
            DataCenter zone = ApiDBUtils.findZoneById(profile.getZoneId());
            if (zone != null) {
                response.setZoneId(zone.getUuid());
            }
        }
        if (profile.getServiceOfferingId() != null) {
            ServiceOffering so = ApiDBUtils.findServiceOfferingById(profile.getServiceOfferingId());
            if (so != null) {
                response.setServiceOfferingId(so.getUuid());
            }
        }
        if (profile.getTemplateId() != null) {
            VMTemplateVO template = ApiDBUtils.findTemplateById(profile.getTemplateId());
            if (template != null) {
                response.setTemplateId(template.getUuid());
            }
        }
        response.setOtherDeployParams(profile.getOtherDeployParams());
        response.setCounterParams(profile.getCounterParams());
        response.setDestroyVmGraceperiod(profile.getDestroyVmGraceperiod());
        User user = ApiDBUtils.findUserById(profile.getAutoScaleUserId());
        if (user != null) {
            response.setAutoscaleUserId(user.getUuid());
        }
        response.setObjectName("autoscalevmprofile");

        // Populates the account information in the response
        populateOwner(response, profile);
        return response;
    }

    @Override
    public AutoScalePolicyResponse createAutoScalePolicyResponse(AutoScalePolicy policy) {
        AutoScalePolicyResponse response = new AutoScalePolicyResponse();
        response.setId(policy.getUuid());
        response.setDuration(policy.getDuration());
        response.setQuietTime(policy.getQuietTime());
        response.setAction(policy.getAction());
        List<ConditionVO> vos = ApiDBUtils.getAutoScalePolicyConditions(policy.getId());
        ArrayList<ConditionResponse> conditions = new ArrayList<ConditionResponse>(vos.size());
        for (ConditionVO vo : vos) {
            conditions.add(createConditionResponse(vo));
        }
        response.setConditions(conditions);
        response.setObjectName("autoscalepolicy");

        // Populates the account information in the response
        populateOwner(response, policy);

        return response;
    }

    @Override
    public AutoScaleVmGroupResponse createAutoScaleVmGroupResponse(AutoScaleVmGroup vmGroup) {
        AutoScaleVmGroupResponse response = new AutoScaleVmGroupResponse();
        response.setId(vmGroup.getUuid());
        response.setMinMembers(vmGroup.getMinMembers());
        response.setMaxMembers(vmGroup.getMaxMembers());
        response.setState(vmGroup.getState());
        response.setInterval(vmGroup.getInterval());
        AutoScaleVmProfileVO profile = ApiDBUtils.findAutoScaleVmProfileById(vmGroup.getProfileId());
        if (profile != null) {
            response.setProfileId(profile.getUuid());
        }
        FirewallRuleVO fw = ApiDBUtils.findFirewallRuleById(vmGroup.getProfileId());
        if (fw != null) {
            response.setLoadBalancerId(fw.getUuid());
        }

        List<AutoScalePolicyResponse> scaleUpPoliciesResponse = new ArrayList<AutoScalePolicyResponse>();
        List<AutoScalePolicyResponse> scaleDownPoliciesResponse = new ArrayList<AutoScalePolicyResponse>();
        response.setScaleUpPolicies(scaleUpPoliciesResponse);
        response.setScaleDownPolicies(scaleDownPoliciesResponse);
        response.setObjectName("autoscalevmgroup");

        // Fetch policies for vmgroup
        List<AutoScalePolicy> scaleUpPolicies = new ArrayList<AutoScalePolicy>();
        List<AutoScalePolicy> scaleDownPolicies = new ArrayList<AutoScalePolicy>();
        ApiDBUtils.getAutoScaleVmGroupPolicies(vmGroup.getId(), scaleUpPolicies, scaleDownPolicies);
        // populate policies
        for (AutoScalePolicy autoScalePolicy : scaleUpPolicies) {
            scaleUpPoliciesResponse.add(createAutoScalePolicyResponse(autoScalePolicy));
        }
        for (AutoScalePolicy autoScalePolicy : scaleDownPolicies) {
            scaleDownPoliciesResponse.add(createAutoScalePolicyResponse(autoScalePolicy));
        }

        return response;
    }

    @Override
    public StaticRouteResponse createStaticRouteResponse(StaticRoute result) {
        StaticRouteResponse response = new StaticRouteResponse();
        response.setId(result.getUuid());
        if (result.getVpcId() != null) {
            Vpc vpc = ApiDBUtils.findVpcById(result.getVpcId());
            if (vpc != null) {
                response.setVpcId(vpc.getUuid());
            }
        }
        response.setCidr(result.getCidr());

        StaticRoute.State state = result.getState();
        String stateToSet = state.toString();
        if (state.equals(FirewallRule.State.Revoke)) {
            stateToSet = "Deleting";
        }
        response.setState(stateToSet);
        populateAccount(response, result.getAccountId());
        populateDomain(response, result.getDomainId());

        //set tag information
        List<? extends ResourceTag> tags = ApiDBUtils.listByResourceTypeAndId(TaggedResourceType.StaticRoute, result.getId());
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            tagResponses.add(tagResponse);
        }
        response.setTags(tagResponses);
        response.setObjectName("staticroute");

        return response;
    }

    @Override
    public Site2SiteVpnGatewayResponse createSite2SiteVpnGatewayResponse(Site2SiteVpnGateway result) {
        Site2SiteVpnGatewayResponse response = new Site2SiteVpnGatewayResponse();
        response.setId(result.getUuid());
        response.setIp(ApiDBUtils.findIpAddressById(result.getAddrId()).getAddress().toString());
        Vpc vpc = ApiDBUtils.findVpcById(result.getVpcId());
        if (vpc != null) {
            response.setVpcId(result.getUuid());
        }
        response.setRemoved(result.getRemoved());
        response.setObjectName("vpngateway");

        populateAccount(response, result.getAccountId());
        populateDomain(response, result.getDomainId());
        return response;
    }

    @Override
    public Site2SiteCustomerGatewayResponse createSite2SiteCustomerGatewayResponse(Site2SiteCustomerGateway result) {
        Site2SiteCustomerGatewayResponse response = new Site2SiteCustomerGatewayResponse();
        response.setId(result.getUuid());
        response.setName(result.getName());
        response.setGatewayIp(result.getGatewayIp());
        response.setGuestCidrList(result.getGuestCidrList());
        response.setIpsecPsk(result.getIpsecPsk());
        response.setIkePolicy(result.getIkePolicy());
        response.setEspPolicy(result.getEspPolicy());
        response.setIkeLifetime(result.getIkeLifetime());
        response.setEspLifetime(result.getEspLifetime());
        response.setDpd(result.getDpd());

        response.setRemoved(result.getRemoved());
        response.setObjectName("vpncustomergateway");

        populateAccount(response, result.getAccountId());
        populateDomain(response, result.getDomainId());

        return response;
    }

    @Override
    public Site2SiteVpnConnectionResponse createSite2SiteVpnConnectionResponse(Site2SiteVpnConnection result) {
        Site2SiteVpnConnectionResponse response = new Site2SiteVpnConnectionResponse();
        response.setId(result.getUuid());

        Long vpnGatewayId = result.getVpnGatewayId();
        if(vpnGatewayId != null) {
            Site2SiteVpnGateway vpnGateway = ApiDBUtils.findVpnGatewayById(vpnGatewayId);
            if (vpnGateway != null) {
                response.setVpnGatewayId(vpnGateway.getUuid());
                long ipId = vpnGateway.getAddrId();
                IPAddressVO ipObj = ApiDBUtils.findIpAddressById(ipId);
                response.setIp(ipObj.getAddress().addr());
            }
        }

        Long customerGatewayId = result.getCustomerGatewayId();
        if(customerGatewayId != null) {
            Site2SiteCustomerGateway customerGateway = ApiDBUtils.findCustomerGatewayById(customerGatewayId);
            if (customerGateway != null) {
                response.setCustomerGatewayId(customerGateway.getUuid());
                response.setGatewayIp(customerGateway.getGatewayIp());
                response.setGuestCidrList(customerGateway.getGuestCidrList());
                response.setIpsecPsk(customerGateway.getIpsecPsk());
                response.setIkePolicy(customerGateway.getIkePolicy());
                response.setEspPolicy(customerGateway.getEspPolicy());
                response.setIkeLifetime(customerGateway.getIkeLifetime());
                response.setEspLifetime(customerGateway.getEspLifetime());
                response.setDpd(customerGateway.getDpd());
            }
        }

        populateAccount(response, result.getAccountId());
        populateDomain(response, result.getDomainId());

        response.setState(result.getState().toString());
        response.setCreated(result.getCreated());
        response.setRemoved(result.getRemoved());
        response.setObjectName("vpnconnection");
        return response;
    }
    @Override
    public GuestOSResponse createGuestOSResponse(GuestOS guestOS) {
        GuestOSResponse response = new GuestOSResponse();
        response.setDescription(guestOS.getDisplayName());
        response.setId(guestOS.getUuid());
        GuestOSCategoryVO category = ApiDBUtils.findGuestOsCategoryById(guestOS.getCategoryId());
        if ( category != null ){
            response.setOsCategoryId(category.getUuid());
        }

        response.setObjectName("ostype");
        return response;
    }



    @Override
    public SnapshotScheduleResponse createSnapshotScheduleResponse(SnapshotSchedule snapshotSchedule) {
        SnapshotScheduleResponse response = new SnapshotScheduleResponse();
        response.setId(snapshotSchedule.getUuid());
        if (snapshotSchedule.getVolumeId() != null) {
            Volume vol = ApiDBUtils.findVolumeById(snapshotSchedule.getVolumeId());
            if (vol != null) {
                response.setVolumeId(vol.getUuid());
            }
        }
        if (snapshotSchedule.getPolicyId() != null) {
            SnapshotPolicy policy = ApiDBUtils.findSnapshotPolicyById(snapshotSchedule.getPolicyId());
            if (policy != null) {
                response.setSnapshotPolicyId(policy.getUuid());
            }
        }
        response.setScheduled(snapshotSchedule.getScheduledTimestamp());

        response.setObjectName("snapshot");
        return response;
    }


	@Override
	public UsageRecordResponse createUsageResponse(Usage usageRecord) {
		UsageRecordResponse usageRecResponse = new UsageRecordResponse();

		Account account = ApiDBUtils.findAccountByIdIncludingRemoved(usageRecord.getAccountId());
		if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
			//find the project
			Project project = ApiDBUtils.findProjectByProjectAccountIdIncludingRemoved(account.getId());
			usageRecResponse.setProjectId(project.getUuid());
			usageRecResponse.setProjectName(project.getName());
		} else {
			usageRecResponse.setAccountId(account.getUuid());
			usageRecResponse.setAccountName(account.getAccountName());
		}

		Domain domain = ApiDBUtils.findDomainById(usageRecord.getDomainId());
		if (domain != null) {
			usageRecResponse.setDomainId(domain.getUuid());
		}

		if (usageRecord.getZoneId() != null) {
			DataCenter zone = ApiDBUtils.findZoneById(usageRecord.getZoneId());
			if (zone != null) {
				usageRecResponse.setZoneId(zone.getUuid());
			}
		}
		usageRecResponse.setDescription(usageRecord.getDescription());
		usageRecResponse.setUsage(usageRecord.getUsageDisplay());
		usageRecResponse.setUsageType(usageRecord.getUsageType());
		if (usageRecord.getVmInstanceId() != null) {
			VMInstanceVO vm = _entityMgr.findByIdIncludingRemoved(VMInstanceVO.class, usageRecord.getVmInstanceId());
			usageRecResponse.setVirtualMachineId(vm.getUuid());
		}
		usageRecResponse.setVmName(usageRecord.getVmName());
		if (usageRecord.getTemplateId() != null) {
			VMTemplateVO template = ApiDBUtils.findTemplateById(usageRecord.getTemplateId());
			if (template != null) {
				usageRecResponse.setTemplateId(template.getUuid());
			}
		}

		if(usageRecord.getUsageType() == UsageTypes.RUNNING_VM || usageRecord.getUsageType() == UsageTypes.ALLOCATED_VM){
			ServiceOfferingVO svcOffering = _entityMgr.findByIdIncludingRemoved(ServiceOfferingVO.class, usageRecord.getOfferingId().toString());
			//Service Offering Id
			usageRecResponse.setOfferingId(svcOffering.getUuid());
			//VM Instance ID
			VMInstanceVO vm = _entityMgr.findByIdIncludingRemoved(VMInstanceVO.class, usageRecord.getUsageId().toString());
			usageRecResponse.setUsageId(vm.getUuid());
			//Hypervisor Type
			usageRecResponse.setType(usageRecord.getType());

		} else if(usageRecord.getUsageType() == UsageTypes.IP_ADDRESS){
			//isSourceNAT
			usageRecResponse.setSourceNat((usageRecord.getType().equals("SourceNat"))?true:false);
			//isSystem
			usageRecResponse.setSystem((usageRecord.getSize() == 1)?true:false);
			//IP Address ID
			IPAddressVO ip = _entityMgr.findByIdIncludingRemoved(IPAddressVO.class, usageRecord.getUsageId().toString());
			usageRecResponse.setUsageId(ip.getUuid());

		} else if(usageRecord.getUsageType() == UsageTypes.NETWORK_BYTES_SENT || usageRecord.getUsageType() == UsageTypes.NETWORK_BYTES_RECEIVED){
			//Device Type
			usageRecResponse.setType(usageRecord.getType());
			if(usageRecord.getType().equals("DomainRouter")){
				//Domain Router Id
				VMInstanceVO vm = _entityMgr.findByIdIncludingRemoved(VMInstanceVO.class, usageRecord.getUsageId().toString());
				usageRecResponse.setUsageId(vm.getUuid());
			} else {
				//External Device Host Id
				HostVO host = _entityMgr.findByIdIncludingRemoved(HostVO.class, usageRecord.getUsageId().toString());
				usageRecResponse.setUsageId(host.getUuid());
			}
			//Network ID
			NetworkVO network = _entityMgr.findByIdIncludingRemoved(NetworkVO.class, usageRecord.getNetworkId().toString());
			usageRecResponse.setNetworkId(network.getUuid());

		} else if(usageRecord.getUsageType() == UsageTypes.VOLUME){
			//Volume ID
			VolumeVO volume = _entityMgr.findByIdIncludingRemoved(VolumeVO.class, usageRecord.getUsageId().toString());
			usageRecResponse.setUsageId(volume.getUuid());
			//Volume Size
			usageRecResponse.setSize(usageRecord.getSize());
			//Disk Offering Id
			if(usageRecord.getOfferingId() != null){
				DiskOfferingVO diskOff = _entityMgr.findByIdIncludingRemoved(DiskOfferingVO.class, usageRecord.getOfferingId().toString());
				usageRecResponse.setOfferingId(diskOff.getUuid());
			}

		} else if(usageRecord.getUsageType() == UsageTypes.TEMPLATE || usageRecord.getUsageType() == UsageTypes.ISO){
			//Template/ISO ID
			VMTemplateVO tmpl = _entityMgr.findByIdIncludingRemoved(VMTemplateVO.class, usageRecord.getUsageId().toString());
			usageRecResponse.setUsageId(tmpl.getUuid());
			//Template/ISO Size
			usageRecResponse.setSize(usageRecord.getSize());

		} else if(usageRecord.getUsageType() == UsageTypes.SNAPSHOT){
			//Snapshot ID
			SnapshotVO snap = _entityMgr.findByIdIncludingRemoved(SnapshotVO.class, usageRecord.getUsageId().toString());
			usageRecResponse.setUsageId(snap.getUuid());
			//Snapshot Size
			usageRecResponse.setSize(usageRecord.getSize());

		} else if(usageRecord.getUsageType() == UsageTypes.LOAD_BALANCER_POLICY){
			//Load Balancer Policy ID
			usageRecResponse.setUsageId(usageRecord.getUsageId().toString());

		} else if(usageRecord.getUsageType() == UsageTypes.PORT_FORWARDING_RULE){
			//Port Forwarding Rule ID
			usageRecResponse.setUsageId(usageRecord.getUsageId().toString());

		} else if(usageRecord.getUsageType() == UsageTypes.NETWORK_OFFERING){
			//Network Offering Id
			NetworkOfferingVO netOff = _entityMgr.findByIdIncludingRemoved(NetworkOfferingVO.class, usageRecord.getOfferingId().toString());
			usageRecResponse.setOfferingId(netOff.getUuid());
			//is Default
			usageRecResponse.setDefault((usageRecord.getUsageId() == 1)? true:false);

		} else if(usageRecord.getUsageType() == UsageTypes.VPN_USERS){
			//VPN User ID
			usageRecResponse.setUsageId(usageRecord.getUsageId().toString());

		} else if(usageRecord.getUsageType() == UsageTypes.SECURITY_GROUP){
			//Security Group Id
			SecurityGroupVO sg = _entityMgr.findByIdIncludingRemoved(SecurityGroupVO.class, usageRecord.getUsageId().toString());
			usageRecResponse.setUsageId(sg.getUuid());
		}

		if (usageRecord.getRawUsage() != null) {
			DecimalFormat decimalFormat = new DecimalFormat("###########.######");
			usageRecResponse.setRawUsage(decimalFormat.format(usageRecord.getRawUsage()));
		}

		if (usageRecord.getStartDate() != null) {
			usageRecResponse.setStartDate(getDateStringInternal(usageRecord.getStartDate()));
		}
		if (usageRecord.getEndDate() != null) {
			usageRecResponse.setEndDate(getDateStringInternal(usageRecord.getEndDate()));
		}

		return usageRecResponse;
	}

	
    public String getDateStringInternal(Date inputDate) {
        if (inputDate == null) return null;

        TimeZone tz = _usageSvc.getUsageTimezone();
        Calendar cal = Calendar.getInstance(tz);
        cal.setTime(inputDate);

        StringBuffer sb = new StringBuffer();
        sb.append(cal.get(Calendar.YEAR)+"-");

        int month = cal.get(Calendar.MONTH) + 1;
        if (month < 10) {
            sb.append("0" + month + "-");
        } else {
            sb.append(month+"-");
        }

        int day = cal.get(Calendar.DAY_OF_MONTH);
        if (day < 10) {
            sb.append("0" + day);
        } else {
            sb.append(""+day);
        }

        sb.append("'T'");

        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour < 10) {
            sb.append("0" + hour + ":");
        } else {
            sb.append(hour+":");
        }

        int minute = cal.get(Calendar.MINUTE);
        if (minute < 10) {
            sb.append("0" + minute + ":");
        } else {
            sb.append(minute+":");
        }

        int seconds = cal.get(Calendar.SECOND);
        if (seconds < 10) {
            sb.append("0" + seconds);
        } else {
            sb.append(""+seconds);
        }

        double offset = cal.get(Calendar.ZONE_OFFSET);
        if (tz.inDaylightTime(inputDate)) {
            offset += (1.0*tz.getDSTSavings()); // add the timezone's DST value (typically 1 hour expressed in milliseconds)
        }

        offset = offset / (1000d*60d*60d);
        int hourOffset = (int)offset;
        double decimalVal = Math.abs(offset) - Math.abs(hourOffset);
        int minuteOffset = (int)(decimalVal * 60);

        if (hourOffset < 0) {
            if (hourOffset > -10) {
                sb.append("-0"+Math.abs(hourOffset));
            } else {
                sb.append("-"+Math.abs(hourOffset));
            }
        } else {
            if (hourOffset < 10) {
                sb.append("+0" + hourOffset);
            } else {
                sb.append("+" + hourOffset);
            }
        }

        sb.append(":");

        if (minuteOffset == 0) {
            sb.append("00");
        } else if (minuteOffset < 10) {
            sb.append("0" + minuteOffset);
        } else {
            sb.append("" + minuteOffset);
        }

        return sb.toString();
    }
    
    @Override
    public TrafficMonitorResponse createTrafficMonitorResponse(Host trafficMonitor) {
        Map<String, String> tmDetails = ApiDBUtils.findHostDetailsById(trafficMonitor.getId());
        TrafficMonitorResponse response = new TrafficMonitorResponse();
        response.setId(trafficMonitor.getUuid());
        response.setIpAddress(trafficMonitor.getPrivateIpAddress());
        response.setNumRetries(tmDetails.get("numRetries"));
        response.setTimeout(tmDetails.get("timeout"));
        return response;
    }

    public NicSecondaryIpResponse createSecondaryIPToNicResponse(String ipAddr, Long nicId, Long networkId) {
        NicSecondaryIpResponse response = new NicSecondaryIpResponse();
        NicVO nic = _entityMgr.findById(NicVO.class, nicId);
        NetworkVO network = _entityMgr.findById(NetworkVO.class, networkId);
        response.setIpAddr(ipAddr);
        response.setNicId(nic.getUuid());
        response.setNwId(network.getUuid());
        response.setObjectName("nicsecondaryip");
        return response;
    }

    public NicResponse createNicResponse(Nic result) {
        NicResponse response = new NicResponse();
        response.setId(result.getUuid());
        response.setIpaddress(result.getIp4Address());

        if (result.getSecondaryIp()) {
            List<NicSecondaryIpVO> secondaryIps = ApiDBUtils.findNicSecondaryIps(result.getId());
            if (secondaryIps != null) {
                List<NicSecondaryIpResponse> ipList = new ArrayList<NicSecondaryIpResponse>();
                for (NicSecondaryIpVO ip: secondaryIps) {
                    NicSecondaryIpResponse ipRes = new NicSecondaryIpResponse();
                    ipRes.setId(ip.getUuid());
                    ipRes.setIpAddr(ip.getIp4Address());
                    ipList.add(ipRes);
                }
                response.setSecondaryIps(ipList);
            }
        }

        response.setGateway(result.getGateway());
        response.setId(result.getUuid());
        response.setGateway(result.getGateway());
        response.setNetmask(result.getNetmask());
        response.setMacAddress(result.getMacAddress());
        if (result.getBroadcastUri() != null) {
            response.setBroadcastUri(result.getBroadcastUri().toString());
        }
        if (result.getIsolationUri() != null) {
            response.setIsolationUri(result.getIsolationUri().toString());
        }
        if (result.getIp6Address() != null) {
            response.setId(result.getIp6Address());
        }

        response.setIsDefault(result.isDefaultNic());
        return response;
    }
}
