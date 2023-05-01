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

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.affinity.AffinityGroup;
import org.apache.cloudstack.affinity.AffinityGroupResponse;
import org.apache.cloudstack.annotation.AnnotationService;
import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.ApiConstants.DomainDetails;
import org.apache.cloudstack.api.ApiConstants.HostDetails;
import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.BaseResponseWithAssociatedNetwork;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.user.job.QueryAsyncJobResultCmd;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.ApplicationLoadBalancerInstanceResponse;
import org.apache.cloudstack.api.response.ApplicationLoadBalancerResponse;
import org.apache.cloudstack.api.response.ApplicationLoadBalancerRuleResponse;
import org.apache.cloudstack.api.response.AsyncJobResponse;
import org.apache.cloudstack.api.response.AutoScalePolicyResponse;
import org.apache.cloudstack.api.response.AutoScaleVmGroupResponse;
import org.apache.cloudstack.api.response.AutoScaleVmProfileResponse;
import org.apache.cloudstack.api.response.BackupOfferingResponse;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.api.response.BackupScheduleResponse;
import org.apache.cloudstack.api.response.CapabilityResponse;
import org.apache.cloudstack.api.response.CapacityResponse;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.ConditionResponse;
import org.apache.cloudstack.api.response.ConfigurationGroupResponse;
import org.apache.cloudstack.api.response.ConfigurationResponse;
import org.apache.cloudstack.api.response.ConfigurationSubGroupResponse;
import org.apache.cloudstack.api.response.ControlledEntityResponse;
import org.apache.cloudstack.api.response.ControlledViewEntityResponse;
import org.apache.cloudstack.api.response.CounterResponse;
import org.apache.cloudstack.api.response.CreateCmdResponse;
import org.apache.cloudstack.api.response.CreateSSHKeyPairResponse;
import org.apache.cloudstack.api.response.DataCenterGuestIpv6PrefixResponse;
import org.apache.cloudstack.api.response.DirectDownloadCertificateHostStatusResponse;
import org.apache.cloudstack.api.response.DirectDownloadCertificateResponse;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.api.response.EventResponse;
import org.apache.cloudstack.api.response.ExtractResponse;
import org.apache.cloudstack.api.response.FirewallResponse;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.GlobalLoadBalancerResponse;
import org.apache.cloudstack.api.response.GuestOSResponse;
import org.apache.cloudstack.api.response.GuestOsMappingResponse;
import org.apache.cloudstack.api.response.GuestVlanRangeResponse;
import org.apache.cloudstack.api.response.GuestVlanResponse;
import org.apache.cloudstack.api.response.HostForMigrationResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.HypervisorCapabilitiesResponse;
import org.apache.cloudstack.api.response.IPAddressResponse;
import org.apache.cloudstack.api.response.ImageStoreResponse;
import org.apache.cloudstack.api.response.InstanceGroupResponse;
import org.apache.cloudstack.api.response.InternalLoadBalancerElementResponse;
import org.apache.cloudstack.api.response.IpForwardingRuleResponse;
import org.apache.cloudstack.api.response.IpRangeResponse;
import org.apache.cloudstack.api.response.Ipv6RouteResponse;
import org.apache.cloudstack.api.response.IsolationMethodResponse;
import org.apache.cloudstack.api.response.LBHealthCheckPolicyResponse;
import org.apache.cloudstack.api.response.LBHealthCheckResponse;
import org.apache.cloudstack.api.response.LBStickinessPolicyResponse;
import org.apache.cloudstack.api.response.LBStickinessResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.LoadBalancerConfigResponse;
import org.apache.cloudstack.api.response.LoadBalancerResponse;
import org.apache.cloudstack.api.response.ManagementServerResponse;
import org.apache.cloudstack.api.response.NetworkACLItemResponse;
import org.apache.cloudstack.api.response.NetworkACLResponse;
import org.apache.cloudstack.api.response.NetworkOfferingResponse;
import org.apache.cloudstack.api.response.NetworkPermissionsResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.NicExtraDhcpOptionResponse;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.cloudstack.api.response.NicSecondaryIpResponse;
import org.apache.cloudstack.api.response.OvsProviderResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.PortableIpRangeResponse;
import org.apache.cloudstack.api.response.PortableIpResponse;
import org.apache.cloudstack.api.response.PrivateGatewayResponse;
import org.apache.cloudstack.api.response.ProjectAccountResponse;
import org.apache.cloudstack.api.response.ProjectInvitationResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ProviderResponse;
import org.apache.cloudstack.api.response.RegionResponse;
import org.apache.cloudstack.api.response.RemoteAccessVpnResponse;
import org.apache.cloudstack.api.response.ResourceCountResponse;
import org.apache.cloudstack.api.response.ResourceIconResponse;
import org.apache.cloudstack.api.response.ResourceLimitResponse;
import org.apache.cloudstack.api.response.ResourceTagResponse;
import org.apache.cloudstack.api.response.RollingMaintenanceHostSkippedResponse;
import org.apache.cloudstack.api.response.RollingMaintenanceHostUpdatedResponse;
import org.apache.cloudstack.api.response.RollingMaintenanceResponse;
import org.apache.cloudstack.api.response.RouterHealthCheckResultResponse;
import org.apache.cloudstack.api.response.SSHKeyPairResponse;
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
import org.apache.cloudstack.api.response.SystemVmInstanceResponse;
import org.apache.cloudstack.api.response.SystemVmResponse;
import org.apache.cloudstack.api.response.TemplatePermissionsResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.TrafficMonitorResponse;
import org.apache.cloudstack.api.response.TrafficTypeResponse;
import org.apache.cloudstack.api.response.UpgradeRouterTemplateResponse;
import org.apache.cloudstack.api.response.UsageRecordResponse;
import org.apache.cloudstack.api.response.UserDataResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VMSnapshotResponse;
import org.apache.cloudstack.api.response.VirtualRouterProviderResponse;
import org.apache.cloudstack.api.response.VlanIpRangeResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.api.response.VpcOfferingResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.cloudstack.api.response.VpnUsersResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.backup.BackupOffering;
import org.apache.cloudstack.backup.BackupSchedule;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.config.Configuration;
import org.apache.cloudstack.config.ConfigurationGroup;
import org.apache.cloudstack.config.ConfigurationSubGroup;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.direct.download.DirectDownloadCertificate;
import org.apache.cloudstack.direct.download.DirectDownloadCertificateHostMap;
import org.apache.cloudstack.direct.download.DirectDownloadManager;
import org.apache.cloudstack.direct.download.DirectDownloadManager.HostCertificateStatus.CertificateStatus;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.management.ManagementServerHost;
import org.apache.cloudstack.network.lb.ApplicationLoadBalancerRule;
import org.apache.cloudstack.network.lb.LoadBalancerConfig;
import org.apache.cloudstack.network.lb.LoadBalancerConfigKey;
import org.apache.cloudstack.region.PortableIp;
import org.apache.cloudstack.region.PortableIpRange;
import org.apache.cloudstack.region.Region;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.usage.Usage;
import org.apache.cloudstack.usage.UsageService;
import org.apache.cloudstack.usage.UsageTypes;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.api.VgpuTypesInfo;
import com.cloud.api.query.ViewResponseHelper;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.vo.AccountJoinVO;
import com.cloud.api.query.vo.AsyncJobJoinVO;
import com.cloud.api.query.vo.ControlledViewEntity;
import com.cloud.api.query.vo.DataCenterJoinVO;
import com.cloud.api.query.vo.DiskOfferingJoinVO;
import com.cloud.api.query.vo.DomainRouterJoinVO;
import com.cloud.api.query.vo.EventJoinVO;
import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.api.query.vo.ImageStoreJoinVO;
import com.cloud.api.query.vo.InstanceGroupJoinVO;
import com.cloud.api.query.vo.NetworkOfferingJoinVO;
import com.cloud.api.query.vo.ProjectAccountJoinVO;
import com.cloud.api.query.vo.ProjectInvitationJoinVO;
import com.cloud.api.query.vo.ProjectJoinVO;
import com.cloud.api.query.vo.ResourceTagJoinVO;
import com.cloud.api.query.vo.SecurityGroupJoinVO;
import com.cloud.api.query.vo.ServiceOfferingJoinVO;
import com.cloud.api.query.vo.StoragePoolJoinVO;
import com.cloud.api.query.vo.TemplateJoinVO;
import com.cloud.api.query.vo.UserAccountJoinVO;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.api.query.vo.VolumeJoinVO;
import com.cloud.api.query.vo.VpcOfferingJoinVO;
import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDaoImpl.SummedCapacity;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource.ResourceOwnerType;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.ResourceCount;
import com.cloud.configuration.ResourceLimit;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterGuestIpv6Prefix;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.StorageNetworkIpRange;
import com.cloud.dc.Vlan;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.event.Event;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.gpu.GPU;
import com.cloud.host.ControlState;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.HypervisorCapabilities;
import com.cloud.network.GuestVlan;
import com.cloud.network.GuestVlanRange;
import com.cloud.network.IpAddress;
import com.cloud.network.Ipv6Service;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkPermission;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.OvsProvider;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PhysicalNetworkTrafficType;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.RouterHealthCheckResult;
import com.cloud.network.Site2SiteCustomerGateway;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.VpnUser;
import com.cloud.network.VpnUserVO;
import com.cloud.network.as.AutoScalePolicy;
import com.cloud.network.as.AutoScaleVmGroup;
import com.cloud.network.as.AutoScaleVmProfile;
import com.cloud.network.as.AutoScaleVmProfileVO;
import com.cloud.network.as.Condition;
import com.cloud.network.as.ConditionVO;
import com.cloud.network.as.Counter;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.HealthCheckPolicy;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.StaticNatRule;
import com.cloud.network.rules.StickinessPolicy;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupVO;
import com.cloud.network.security.SecurityRule;
import com.cloud.network.security.SecurityRule.SecurityRuleType;
import com.cloud.network.vpc.NetworkACL;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.StaticRoute;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.network.vpc.VpcVO;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Detail;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.org.Cluster;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectAccount;
import com.cloud.projects.ProjectInvitation;
import com.cloud.region.ha.GlobalLoadBalancerRule;
import com.cloud.resource.RollingMaintenanceManager;
import com.cloud.server.ResourceIcon;
import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOS;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSHypervisor;
import com.cloud.storage.GuestOsCategory;
import com.cloud.storage.ImageStore;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Upload;
import com.cloud.storage.UploadVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotPolicy;
import com.cloud.storage.snapshot.SnapshotSchedule;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.SSHKeyPair;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.UserData;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Dhcp;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.security.CertificateHelper;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.InstanceGroup;
import com.cloud.vm.Nic;
import com.cloud.vm.NicExtraDhcpOptionVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicSecondaryIp;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.dao.NicExtraDhcpOptionDao;
import com.cloud.vm.dao.NicSecondaryIpVO;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

import sun.security.x509.X509CertImpl;

public class ApiResponseHelper implements ResponseGenerator {

    private static final Logger s_logger = Logger.getLogger(ApiResponseHelper.class);
    private static final DecimalFormat s_percentFormat = new DecimalFormat("##.##");

    @Inject
    private EntityManager _entityMgr;
    @Inject
    private UsageService _usageSvc;
    @Inject
    NetworkModel _ntwkModel;
    @Inject
    protected AccountManager _accountMgr;
    @Inject
    protected AsyncJobManager _jobMgr;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    SnapshotDataFactory snapshotfactory;
    @Inject
    private VolumeDao _volumeDao;
    @Inject
    private DataStoreManager _dataStoreMgr;
    @Inject
    private SnapshotDataStoreDao _snapshotStoreDao;
    @Inject
    private PrimaryDataStoreDao _storagePoolDao;
    @Inject
    private ClusterDetailsDao _clusterDetailsDao;
    @Inject
    private ResourceTagDao _resourceTagDao;
    @Inject
    private NicExtraDhcpOptionDao _nicExtraDhcpOptionDao;
    @Inject
    private IPAddressDao userIpAddressDao;
    @Inject
    NetworkDetailsDao networkDetailsDao;
    @Inject
    private VMSnapshotDao vmSnapshotDao;
    @Inject
    private BackupOfferingDao backupOfferingDao;
    @Inject
    private GuestOSCategoryDao _guestOsCategoryDao;
    @Inject
    private GuestOSDao _guestOsDao;
    @Inject
    private AnnotationDao annotationDao;
    @Inject
    private UserStatisticsDao userStatsDao;
    @Inject
    private NetworkDao networkDao;
    @Inject
    NetworkOfferingDao networkOfferingDao;
    @Inject
    Ipv6Service ipv6Service;
    @Inject
    UserVmJoinDao userVmJoinDao;
    @Inject
    NetworkServiceMapDao ntwkSrvcDao;

    @Override
    public UserResponse createUserResponse(User user) {
        UserAccountJoinVO vUser = ApiDBUtils.newUserView(user);
        return ApiDBUtils.newUserResponse(vUser);
    }

    // this method is used for response generation via createAccount (which
    // creates an account + user)
    @Override
    public AccountResponse createUserAccountResponse(ResponseView view, UserAccount user) {
        return ApiDBUtils.newAccountResponse(view, EnumSet.of(DomainDetails.all), ApiDBUtils.findAccountViewById(user.getAccountId()));
    }

    @Override
    public AccountResponse createAccountResponse(ResponseView view, Account account) {
        AccountJoinVO vUser = ApiDBUtils.newAccountView(account);
        return ApiDBUtils.newAccountResponse(view, EnumSet.of(DomainDetails.all), vUser);
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
        domainResponse.setCreated(domain.getCreated());
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
        resourceLimitResponse.setResourceType(limit.getType());

        if ((limit.getType() == ResourceType.primary_storage || limit.getType() == ResourceType.secondary_storage) && limit.getMax() >= 0) {
            resourceLimitResponse.setMax((long)Math.ceil((double)limit.getMax() / ResourceType.bytesToGiB));
        } else {
            resourceLimitResponse.setMax(limit.getMax());
        }
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

        resourceCountResponse.setResourceType(resourceCount.getType());
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
        Pair<String, String> configGroupAndSubGroup = _configMgr.getConfigurationGroupAndSubGroup(cfg.getName());
        cfgResponse.setGroup(configGroupAndSubGroup.first());
        cfgResponse.setSubGroup(configGroupAndSubGroup.second());
        cfgResponse.setDescription(cfg.getDescription());
        cfgResponse.setName(cfg.getName());
        if (cfg.isEncrypted()) {
            cfgResponse.setValue(DBEncryptionUtil.encrypt(cfg.getValue()));
        } else {
            cfgResponse.setValue(cfg.getValue());
        }
        cfgResponse.setDefaultValue(cfg.getDefaultValue());
        cfgResponse.setIsDynamic(cfg.isDynamic());
        cfgResponse.setComponent(cfg.getComponent());
        if (cfg.getParent() != null) {
            cfgResponse.setParent(cfg.getParent());
        }
        cfgResponse.setDisplayText(cfg.getDisplayText());
        cfgResponse.setType(_configMgr.getConfigurationType(cfg.getName()));
        if (cfg.getOptions() != null) {
            cfgResponse.setOptions(cfg.getOptions());
        }
        cfgResponse.setObjectName("configuration");

        return cfgResponse;
    }

    @Override
    public ConfigurationGroupResponse createConfigurationGroupResponse(ConfigurationGroup cfgGroup) {
        ConfigurationGroupResponse cfgGroupResponse = new ConfigurationGroupResponse();
        cfgGroupResponse.setGroupName(cfgGroup.getName());
        cfgGroupResponse.setDescription(cfgGroup.getDescription());
        cfgGroupResponse.setPrecedence(cfgGroup.getPrecedence());

        List<? extends ConfigurationSubGroup> subgroups = _configMgr.getConfigurationSubGroups(cfgGroup.getId());
        List<ConfigurationSubGroupResponse> cfgSubGroupResponses = new ArrayList<>();
        for (ConfigurationSubGroup subgroup : subgroups) {
            ConfigurationSubGroupResponse cfgSubGroupResponse = createConfigurationSubGroupResponse(subgroup);
            cfgSubGroupResponses.add(cfgSubGroupResponse);
        }
        cfgGroupResponse.setSubGroups(cfgSubGroupResponses);
        cfgGroupResponse.setObjectName("configurationgroup");
        return cfgGroupResponse;
    }

    private ConfigurationSubGroupResponse createConfigurationSubGroupResponse(ConfigurationSubGroup cfgSubGroup) {
        ConfigurationSubGroupResponse cfgSubGroupResponse = new ConfigurationSubGroupResponse();
        cfgSubGroupResponse.setSubGroupName(cfgSubGroup.getName());
        cfgSubGroupResponse.setPrecedence(cfgSubGroup.getPrecedence());
        cfgSubGroupResponse.setObjectName("subgroup");
        return cfgSubGroupResponse;
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
            snapshotResponse.setVirtualSize(volume.getSize());
            DataCenter zone = ApiDBUtils.findZoneById(volume.getDataCenterId());
            if (zone != null) {
                snapshotResponse.setZoneId(zone.getUuid());
            }

            if (volume.getVolumeType() == Volume.Type.ROOT && volume.getInstanceId() != null) {
                //TODO combine lines and 489 into a join in the volume dao
                VMInstanceVO instance = ApiDBUtils.findVMInstanceById(volume.getInstanceId());
                if (instance != null) {
                    GuestOS guestOs = ApiDBUtils.findGuestOSById(instance.getGuestOSId());
                    if (guestOs != null) {
                        snapshotResponse.setOsTypeId(guestOs.getUuid());
                        snapshotResponse.setOsDisplayName(guestOs.getDisplayName());
                    }
                }
            }
        }
        snapshotResponse.setCreated(snapshot.getCreated());
        snapshotResponse.setName(snapshot.getName());
        snapshotResponse.setIntervalType(ApiDBUtils.getSnapshotIntervalTypes(snapshot.getId()));
        snapshotResponse.setState(snapshot.getState());
        snapshotResponse.setLocationType(ApiDBUtils.getSnapshotLocationType(snapshot.getId()));

        SnapshotInfo snapshotInfo = null;

        if (snapshot instanceof SnapshotInfo) {
            snapshotInfo = (SnapshotInfo)snapshot;
        } else {
            DataStoreRole dataStoreRole = getDataStoreRole(snapshot, _snapshotStoreDao, _dataStoreMgr);

            snapshotInfo = snapshotfactory.getSnapshot(snapshot.getId(), dataStoreRole);
        }

        if (snapshotInfo == null) {
            s_logger.debug("Unable to find info for image store snapshot with uuid " + snapshot.getUuid());
            snapshotResponse.setRevertable(false);
        } else {
        snapshotResponse.setRevertable(snapshotInfo.isRevertable());
        snapshotResponse.setPhysicaSize(snapshotInfo.getPhysicalSize());
        }

        // set tag information
        List<? extends ResourceTag> tags = ApiDBUtils.listByResourceTypeAndId(ResourceObjectType.Snapshot, snapshot.getId());
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            CollectionUtils.addIgnoreNull(tagResponses, tagResponse);
        }
        snapshotResponse.setTags(new HashSet<>(tagResponses));
        snapshotResponse.setHasAnnotation(annotationDao.hasAnnotations(snapshot.getUuid(), AnnotationService.EntityType.SNAPSHOT.name(),
                _accountMgr.isRootAdmin(CallContext.current().getCallingAccount().getId())));

        snapshotResponse.setObjectName("snapshot");
        return snapshotResponse;
    }

    public static DataStoreRole getDataStoreRole(Snapshot snapshot, SnapshotDataStoreDao snapshotStoreDao, DataStoreManager dataStoreMgr) {
        SnapshotDataStoreVO snapshotStore = snapshotStoreDao.findBySnapshot(snapshot.getId(), DataStoreRole.Primary);

        if (snapshotStore == null) {
            return DataStoreRole.Image;
        }

        long storagePoolId = snapshotStore.getDataStoreId();
        DataStore dataStore = dataStoreMgr.getDataStore(storagePoolId, DataStoreRole.Primary);
        if (dataStore == null) {
            return DataStoreRole.Image;
        }

        Map<String, String> mapCapabilities = dataStore.getDriver().getCapabilities();

        if (mapCapabilities != null) {
            String value = mapCapabilities.get(DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT.toString());
            Boolean supportsStorageSystemSnapshots = new Boolean(value);

            if (supportsStorageSystemSnapshots) {
                return DataStoreRole.Primary;
            }
        }

        return DataStoreRole.Image;
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
        if (vm != null) {
            vmSnapshotResponse.setVirtualMachineId(vm.getUuid());
            vmSnapshotResponse.setVirtualMachineName(StringUtils.isEmpty(vm.getDisplayName()) ? vm.getHostName() : vm.getDisplayName());
            vmSnapshotResponse.setHypervisor(vm.getHypervisorType());
            DataCenterVO datacenter = ApiDBUtils.findZoneById(vm.getDataCenterId());
            if (datacenter != null) {
                vmSnapshotResponse.setZoneId(datacenter.getUuid());
                vmSnapshotResponse.setZoneName(datacenter.getName());
            }
        }
        if (vmSnapshot.getParent() != null) {
            VMSnapshot vmSnapshotParent = ApiDBUtils.getVMSnapshotById(vmSnapshot.getParent());
            if (vmSnapshotParent != null) {
                vmSnapshotResponse.setParent(vmSnapshotParent.getUuid());
                vmSnapshotResponse.setParentName(vmSnapshotParent.getDisplayName());
            }
        }
        populateOwner(vmSnapshotResponse, vmSnapshot);
        Project project = ApiDBUtils.findProjectByProjectAccountId(vmSnapshot.getAccountId());
        if (project != null) {
            vmSnapshotResponse.setProjectId(project.getUuid());
            vmSnapshotResponse.setProjectName(project.getName());
        }
        Account account = ApiDBUtils.findAccountById(vmSnapshot.getAccountId());
        if (account != null) {
            vmSnapshotResponse.setAccountName(account.getAccountName());
        }
        DomainVO domain = ApiDBUtils.findDomainById(vmSnapshot.getDomainId());
        if (domain != null) {
            vmSnapshotResponse.setDomainId(domain.getUuid());
            vmSnapshotResponse.setDomainName(domain.getName());
        }

        List<? extends ResourceTag> tags = _resourceTagDao.listBy(vmSnapshot.getId(), ResourceObjectType.VMSnapshot);
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, false);
            CollectionUtils.addIgnoreNull(tagResponses, tagResponse);
        }
        vmSnapshotResponse.setTags(new HashSet<>(tagResponses));
        vmSnapshotResponse.setHasAnnotation(annotationDao.hasAnnotations(vmSnapshot.getUuid(), AnnotationService.EntityType.VM_SNAPSHOT.name(),
                _accountMgr.isRootAdmin(CallContext.current().getCallingAccount().getId())));

        vmSnapshotResponse.setCurrent(vmSnapshot.getCurrent());
        vmSnapshotResponse.setType(vmSnapshot.getType().toString());
        vmSnapshotResponse.setObjectName("vmsnapshot");
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
        policyResponse.setForDisplay(policy.isDisplay());
        policyResponse.setObjectName("snapshotpolicy");

        List<? extends ResourceTag> tags = _resourceTagDao.listBy(policy.getId(), ResourceObjectType.SnapshotPolicy);
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, false);
            CollectionUtils.addIgnoreNull(tagResponses, tagResponse);
        }
        policyResponse.setTags(new HashSet<>(tagResponses));

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
    public HostForMigrationResponse createHostForMigrationResponse(Host host) {
        return createHostForMigrationResponse(host, EnumSet.of(HostDetails.all));
    }

    @Override
    public HostForMigrationResponse createHostForMigrationResponse(Host host, EnumSet<HostDetails> details) {
        List<HostJoinVO> viewHosts = ApiDBUtils.newHostView(host);
        List<HostForMigrationResponse> listHosts = ViewResponseHelper.createHostForMigrationResponse(details, viewHosts.toArray(new HostJoinVO[viewHosts.size()]));
        assert listHosts != null && listHosts.size() == 1 : "There should be one host returned";
        return listHosts.get(0);
    }

    @Override
    public VlanIpRangeResponse createVlanIpRangeResponse(Vlan vlan) {
        return createVlanIpRangeResponse(VlanIpRangeResponse.class, vlan);
    }

    @Override
    public VlanIpRangeResponse createVlanIpRangeResponse(Class<? extends VlanIpRangeResponse> subClass, Vlan vlan) {
        try {
            Long podId = ApiDBUtils.getPodIdForVlan(vlan.getId());

            VlanIpRangeResponse vlanResponse = subClass.newInstance();
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

            String gateway = vlan.getVlanGateway();
            String netmask = vlan.getVlanNetmask();
            vlanResponse.setGateway(gateway);
            vlanResponse.setNetmask(netmask);
            if (StringUtils.isNotEmpty(gateway) && StringUtils.isNotEmpty(netmask)) {
                vlanResponse.setCidr(NetUtils.getCidrFromGatewayAndNetmask(gateway, netmask));
            }

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
            } else {
                Domain domain = ApiDBUtils.getVlanDomain(vlan.getId());
                if (domain != null) {
                    populateDomain(vlanResponse, domain.getId());
                } else {
                    Long networkId = vlan.getNetworkId();
                    if (networkId != null) {
                        Network network = _ntwkModel.getNetwork(networkId);
                        if (network != null) {
                            Long accountId = network.getAccountId();
                            populateAccount(vlanResponse, accountId);
                            populateDomain(vlanResponse, ApiDBUtils.findAccountById(accountId).getDomainId());
                        }
                    }
                }
            }

            if (vlan.getPhysicalNetworkId() != null) {
                PhysicalNetwork pnw = ApiDBUtils.findPhysicalNetworkById(vlan.getPhysicalNetworkId());
                if (pnw != null) {
                    vlanResponse.setPhysicalNetworkId(pnw.getUuid());
                }
            }
            vlanResponse.setForSystemVms(isForSystemVms(vlan.getId()));
            vlanResponse.setObjectName("vlan");
            return vlanResponse;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new CloudRuntimeException("Failed to create Vlan IP Range response", e);
        }
    }

    /**
     * Return true if vlan IP range is dedicated for system vms (SSVM and CPVM), false if not
     * @param vlanId vlan id
     * @return true if VLAN IP range is dedicated to system vms
     */
    private boolean isForSystemVms(long vlanId){
        SearchBuilder<IPAddressVO> sb = userIpAddressDao.createSearchBuilder();
        sb.and("vlanId", sb.entity().getVlanId(), SearchCriteria.Op.EQ);
        SearchCriteria<IPAddressVO> sc = sb.create();
        sc.setParameters("vlanId", vlanId);
        IPAddressVO userIpAddresVO = userIpAddressDao.findOneBy(sc);
        return userIpAddresVO != null ? userIpAddresVO.isForSystemVms() : false;
    }

    @Override
    public IPAddressResponse createIPAddressResponse(ResponseView view, IpAddress ipAddr) {
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
                ipResponse.setVpcName(vpc.getName());
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
                ipResponse.setNetworkName(nw.getName());
            }
        }
        ipResponse.setState(ipAddr.getState().toString());

        if (ipAddr.getPhysicalNetworkId() != null) {
            PhysicalNetworkVO pnw = ApiDBUtils.findPhysicalNetworkById(ipAddr.getPhysicalNetworkId());
            if (pnw != null) {
                ipResponse.setPhysicalNetworkId(pnw.getUuid());
            }
        }

        // show vm info for shared networks
        showVmInfoForSharedNetworks(forVirtualNetworks, ipAddr, ipResponse);

        // show this info to full view only
        if (view == ResponseView.Full) {
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

        ipResponse.setForDisplay(ipAddr.isDisplay());

        ipResponse.setPortable(ipAddr.isPortable());

        //set tag information
        List<? extends ResourceTag> tags = ApiDBUtils.listByResourceTypeAndId(ResourceObjectType.PublicIpAddress, ipAddr.getId());
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            CollectionUtils.addIgnoreNull(tagResponses, tagResponse);
        }
        ipResponse.setTags(tagResponses);
        ipResponse.setHasAnnotation(annotationDao.hasAnnotations(ipAddr.getUuid(), AnnotationService.EntityType.PUBLIC_IP_ADDRESS.name(),
                _accountMgr.isRootAdmin(CallContext.current().getCallingAccount().getId())));

        ipResponse.setObjectName("ipaddress");
        return ipResponse;
    }

    private void showVmInfoForSharedNetworks(boolean forVirtualNetworks, IpAddress ipAddr, IPAddressResponse ipResponse) {
        if (!forVirtualNetworks) {
            NicVO nic = ApiDBUtils.findByIp4AddressAndNetworkId(ipAddr.getAddress().toString(), ipAddr.getNetworkId());

            if (nic == null) {  // find in nic_secondary_ips, user vm only
                NicSecondaryIpVO secondaryIp =
                        ApiDBUtils.findSecondaryIpByIp4AddressAndNetworkId(ipAddr.getAddress().toString(), ipAddr.getNetworkId());
                if (secondaryIp != null) {
                    UserVm vm = ApiDBUtils.findUserVmById(secondaryIp.getVmId());
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
            } else if (nic.getVmType() == VirtualMachine.Type.User) {
                UserVm vm = ApiDBUtils.findUserVmById(nic.getInstanceId());
                if (vm != null) {
                    ipResponse.setVirtualMachineId(vm.getUuid());
                    ipResponse.setVirtualMachineName(vm.getHostName());
                    if (vm.getDisplayName() != null) {
                        ipResponse.setVirtualMachineDisplayName(vm.getDisplayName());
                    } else {
                        ipResponse.setVirtualMachineDisplayName(vm.getHostName());
                    }
                }
            } else if (nic.getVmType() == VirtualMachine.Type.DomainRouter) {
                ipResponse.setIsSystem(true);
            }
        }
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
        lbResponse.setLbProtocol(loadBalancer.getLbProtocol());
        lbResponse.setForDisplay(loadBalancer.isDisplay());
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
            lbResponse.setZoneName(zone.getName());
        }

        //set tag information
        List<? extends ResourceTag> tags = ApiDBUtils.listByResourceTypeAndId(ResourceObjectType.LoadBalancer, loadBalancer.getId());
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            CollectionUtils.addIgnoreNull(tagResponses, tagResponse);
        }
        lbResponse.setTags(tagResponses);

        Network ntwk = ApiDBUtils.findNetworkById(loadBalancer.getNetworkId());
        lbResponse.setNetworkId(ntwk.getUuid());

        lbResponse.setCidrList(loadBalancer.getCidrList());

        lbResponse.setObjectName("loadbalancer");
        return lbResponse;
    }

    @Override
    public LoadBalancerConfigResponse createLoadBalancerConfigResponse(LoadBalancerConfig config) {
        Network network = null;
        Vpc vpc = null;
        LoadBalancer lb = null;
        if (config.getNetworkId() != null) {
            network = ApiDBUtils.findNetworkById(config.getNetworkId());
        }
        if (config.getVpcId() != null) {
            vpc = ApiDBUtils.findVpcById(config.getVpcId());
        }
        if (config.getLoadBalancerId() != null) {
            lb = ApiDBUtils.findLoadBalancerById(config.getLoadBalancerId());
        }
        return setLoadBalancerConfigResponse(network, vpc, lb, config);
    }

    @Override
    public List<LoadBalancerConfigResponse> createLoadBalancerConfigResponse(List<? extends LoadBalancerConfig> configs) {
        List<LoadBalancerConfigResponse> lbConfigResponses = new ArrayList<>();
        if (CollectionUtils.isEmpty(configs)) {
            return lbConfigResponses;
        }
        LoadBalancerConfig config = configs.get(0);
        Network network = null;
        Vpc vpc = null;
        LoadBalancer lb = null;
        if (config.getNetworkId() != null) {
            network = ApiDBUtils.findNetworkById(config.getNetworkId());
        }
        if (config.getVpcId() != null) {
            vpc = ApiDBUtils.findVpcById(config.getVpcId());
        }
        if (config.getLoadBalancerId() != null) {
            lb = ApiDBUtils.findLoadBalancerById(config.getLoadBalancerId());
        }
        for (LoadBalancerConfig lbConfig : configs) {
            LoadBalancerConfigResponse lbConfigResponse = setLoadBalancerConfigResponse(network, vpc, lb, lbConfig);
            lbConfigResponses.add(lbConfigResponse);
        }
        return lbConfigResponses;
    }

    private LoadBalancerConfigResponse setLoadBalancerConfigResponse(Network network, Vpc vpc, LoadBalancer lb, LoadBalancerConfig config) {
        LoadBalancerConfigResponse response = new LoadBalancerConfigResponse();
        if (config.getUuid() != null) {
            response.setId(config.getUuid());
        }
        response.setName(config.getName());
        response.setValue(config.getValue());
        response.setScope(String.valueOf(config.getScope()));
        if (network != null) {
            response.setNetworkId(network.getUuid());
        }
        if (vpc != null) {
            response.setVpcId(vpc.getUuid());
        }
        if (lb != null) {
            response.setLoadBalancerId(lb.getUuid());
        }
        response.setCreated(config.getCreated());
        LoadBalancerConfigKey configKey = LoadBalancerConfigKey.getConfigsByScopeAndName(config.getScope(), config.getName());
        if (configKey == null) {
            s_logger.warn(String.format("Unable to determine the load balancer config for scope %s and name %s", config.getScope(), config.getName()));
        } else {
            response.setDescription(configKey.displayText());
            response.setDefaultValue(configKey.defaultValue());
        }
        response.setObjectName("loadbalancerconfig");
        return response;
    }

    @Override
    public GlobalLoadBalancerResponse createGlobalLoadBalancerResponse(GlobalLoadBalancerRule globalLoadBalancerRule) {
        GlobalLoadBalancerResponse response = new GlobalLoadBalancerResponse();
        response.setAlgorithm(globalLoadBalancerRule.getAlgorithm());
        response.setStickyMethod(globalLoadBalancerRule.getPersistence());
        response.setServiceType(globalLoadBalancerRule.getServiceType());
        response.setServiceDomainName(globalLoadBalancerRule.getGslbDomain() + "." + ApiDBUtils.getDnsNameConfiguredForGslb());
        response.setName(globalLoadBalancerRule.getName());
        response.setDescription(globalLoadBalancerRule.getDescription());
        response.setRegionIdId(globalLoadBalancerRule.getRegion());
        response.setId(globalLoadBalancerRule.getUuid());
        populateOwner(response, globalLoadBalancerRule);
        response.setObjectName("globalloadbalancer");

        List<LoadBalancerResponse> siteLbResponses = new ArrayList<LoadBalancerResponse>();
        List<? extends LoadBalancer> siteLoadBalaners = ApiDBUtils.listSiteLoadBalancers(globalLoadBalancerRule.getId());
        for (LoadBalancer siteLb : siteLoadBalaners) {
            LoadBalancerResponse siteLbResponse = createLoadBalancerResponse(siteLb);
            siteLbResponses.add(siteLbResponse);
        }
        response.setSiteLoadBalancers(siteLbResponses);
        return response;
    }

    @Override
    public PodResponse createPodResponse(Pod pod, Boolean showCapacities) {
        String[] ipRange = new String[2];
        List<String> startIps = new ArrayList<String>();
        List<String> endIps = new ArrayList<String>();
        List<String> forSystemVms = new ArrayList<String>();
        List<String> vlanIds = new ArrayList<String>();

        List<IpRangeResponse> ipRanges = new ArrayList<>();

        if (pod.getDescription() != null && pod.getDescription().length() > 0) {
            final String[] existingPodIpRanges = pod.getDescription().split(",");

            for(String podIpRange: existingPodIpRanges) {
                IpRangeResponse ipRangeResponse = new IpRangeResponse();
                final String[] existingPodIpRange = podIpRange.split("-");

                String startIp = ((existingPodIpRange.length > 0) && (existingPodIpRange[0] != null)) ? existingPodIpRange[0] : "";
                ipRangeResponse.setStartIp(startIp);
                startIps.add(startIp);

                String endIp = ((existingPodIpRange.length > 1) && (existingPodIpRange[1] != null)) ? existingPodIpRange[1] : "";
                ipRangeResponse.setEndIp(endIp);
                endIps.add(endIp);

                String forSystemVm = (existingPodIpRange.length > 2) && (existingPodIpRange[2] != null) ? existingPodIpRange[2] : "0";
                ipRangeResponse.setForSystemVms(forSystemVm);
                forSystemVms.add(forSystemVm);

                String vlanId = (existingPodIpRange.length > 3) &&
                        (existingPodIpRange[3] != null && !existingPodIpRange[3].equals("untagged")) ?
                        BroadcastDomainType.Vlan.toUri(existingPodIpRange[3]).toString() :
                        BroadcastDomainType.Vlan.toUri(Vlan.UNTAGGED).toString();
                ipRangeResponse.setVlanId(vlanId);
                vlanIds.add(vlanId);

                ipRanges.add(ipRangeResponse);
            }
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
        podResponse.setIpRanges(ipRanges);
        podResponse.setStartIp(startIps);
        podResponse.setEndIp(endIps);
        podResponse.setForSystemVms(forSystemVms);
        podResponse.setVlanId(vlanIds);
        podResponse.setGateway(pod.getGateway());
        podResponse.setAllocationState(pod.getAllocationState().toString());
        if (showCapacities != null && showCapacities) {
            List<SummedCapacity> capacities = ApiDBUtils.getCapacityByClusterPodZone(null, pod.getId(), null);
            Set<CapacityResponse> capacityResponses = new HashSet<CapacityResponse>();
            for (SummedCapacity capacity : capacities) {
                CapacityResponse capacityResponse = new CapacityResponse();
                capacityResponse.setCapacityType(capacity.getCapacityType());
                capacityResponse.setCapacityName(CapacityVO.getCapacityName(capacity.getCapacityType()));
                capacityResponse.setCapacityUsed(capacity.getUsedCapacity() + capacity.getReservedCapacity());
                if (capacity.getCapacityType() == Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED) {
                    List<SummedCapacity> c = ApiDBUtils.findNonSharedStorageForClusterPodZone(null, pod.getId(), null);
                    capacityResponse.setCapacityTotal(capacity.getTotalCapacity() - c.get(0).getTotalCapacity());
                    capacityResponse.setCapacityUsed(capacity.getUsedCapacity() - c.get(0).getUsedCapacity());
                } else {
                    capacityResponse.setCapacityTotal(capacity.getTotalCapacity());
                }
                if (capacityResponse.getCapacityTotal() != 0) {
                    capacityResponse.setPercentUsed(s_percentFormat.format((float)capacityResponse.getCapacityUsed() / (float)capacityResponse.getCapacityTotal() * 100f));
                } else {
                    capacityResponse.setPercentUsed(s_percentFormat.format(0L));
                }
                capacityResponses.add(capacityResponse);
            }
            // Do it for stats as well.
            capacityResponses.addAll(getStatsCapacityresponse(null, null, pod.getId(), pod.getDataCenterId()));
            podResponse.setCapacities(new ArrayList<CapacityResponse>(capacityResponses));
        }

        podResponse.setHasAnnotation(annotationDao.hasAnnotations(pod.getUuid(), AnnotationService.EntityType.POD.name(),
                _accountMgr.isRootAdmin(CallContext.current().getCallingAccount().getId())));
        podResponse.setObjectName("pod");
        return podResponse;
    }

    @Override
    public ZoneResponse createZoneResponse(ResponseView view, DataCenter dataCenter, Boolean showCapacities, Boolean showResourceIcon) {
        DataCenterJoinVO vOffering = ApiDBUtils.newDataCenterView(dataCenter);
        return ApiDBUtils.newDataCenterResponse(view, vOffering, showCapacities, showResourceIcon);
    }

    public static List<CapacityResponse> getDataCenterCapacityResponse(Long zoneId) {
        List<SummedCapacity> capacities = ApiDBUtils.getCapacityByClusterPodZone(zoneId, null, null);
        Set<CapacityResponse> capacityResponses = new HashSet<CapacityResponse>();

        for (SummedCapacity capacity : capacities) {
            CapacityResponse capacityResponse = new CapacityResponse();
            capacityResponse.setCapacityType(capacity.getCapacityType());
            capacityResponse.setCapacityName(CapacityVO.getCapacityName(capacity.getCapacityType()));
            capacityResponse.setCapacityUsed(capacity.getUsedCapacity() + capacity.getReservedCapacity());
            if (capacity.getCapacityType() == Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED) {
                List<SummedCapacity> c = ApiDBUtils.findNonSharedStorageForClusterPodZone(zoneId, null, null);
                capacityResponse.setCapacityTotal(capacity.getTotalCapacity() - c.get(0).getTotalCapacity());
                capacityResponse.setCapacityUsed(capacity.getUsedCapacity() - c.get(0).getUsedCapacity());
            } else {
                capacityResponse.setCapacityTotal(capacity.getTotalCapacity());
            }
            if (capacityResponse.getCapacityTotal() != 0) {
                capacityResponse.setPercentUsed(s_percentFormat.format((float)capacityResponse.getCapacityUsed() / (float)capacityResponse.getCapacityTotal() * 100f));
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
            capacityResponse.setCapacityName(CapacityVO.getCapacityName(capacity.getCapacityType()));
            capacityResponse.setCapacityUsed(capacity.getUsedCapacity());
            capacityResponse.setCapacityTotal(capacity.getTotalCapacity());
            if (capacityResponse.getCapacityTotal() != 0) {
                capacityResponse.setPercentUsed(s_percentFormat.format((float)capacityResponse.getCapacityUsed() / (float)capacityResponse.getCapacityTotal() * 100f));
            } else {
                capacityResponse.setPercentUsed(s_percentFormat.format(0L));
            }
            capacityResponses.add(capacityResponse);
        }

        return capacityResponses;
    }

    @Override
    public DataCenterGuestIpv6PrefixResponse createDataCenterGuestIpv6PrefixResponse(DataCenterGuestIpv6Prefix prefix) {
        DataCenterGuestIpv6PrefixResponse response = new DataCenterGuestIpv6PrefixResponse();
        response.setId(prefix.getUuid());
        response.setPrefix(prefix.getPrefix());
        DataCenter dc = ApiDBUtils.findZoneById(prefix.getDataCenterId());
        response.setZoneId(dc.getUuid());
        Pair<Integer, Integer> usedTotal = ipv6Service.getUsedTotalIpv6SubnetForPrefix(prefix);
        int used = usedTotal.first();
        int total = usedTotal.second();
        response.setUsedSubnets(used);
        response.setAvailableSubnets(total - used);
        response.setTotalSubnets(total);
        response.setCreated(prefix.getCreated());
        return response;
    }

    @Override
    public VolumeResponse createVolumeResponse(ResponseView view, Volume volume) {
        List<VolumeJoinVO> viewVrs = ApiDBUtils.newVolumeView(volume);
        List<VolumeResponse> listVrs = ViewResponseHelper.createVolumeResponse(view, viewVrs.toArray(new VolumeJoinVO[viewVrs.size()]));
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
    public ImageStoreResponse createImageStoreResponse(ImageStore os) {
        List<ImageStoreJoinVO> viewStores = ApiDBUtils.newImageStoreView(os);
        List<ImageStoreResponse> listStores = ViewResponseHelper.createImageStoreResponse(viewStores.toArray(new ImageStoreJoinVO[viewStores.size()]));
        assert listStores != null && listStores.size() == 1 : "There should be one image data store returned";
        return listStores.get(0);
    }

    @Override
    public StoragePoolResponse createStoragePoolForMigrationResponse(StoragePool pool) {
        List<StoragePoolJoinVO> viewPools = ApiDBUtils.newStoragePoolView(pool);
        List<StoragePoolResponse> listPools = ViewResponseHelper.createStoragePoolForMigrationResponse(viewPools.toArray(new StoragePoolJoinVO[viewPools.size()]));
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
        String cpuOvercommitRatio = ApiDBUtils.findClusterDetails(cluster.getId(), "cpuOvercommitRatio");
        String memoryOvercommitRatio = ApiDBUtils.findClusterDetails(cluster.getId(), "memoryOvercommitRatio");
        clusterResponse.setCpuOvercommitRatio(cpuOvercommitRatio);
        clusterResponse.setMemoryOvercommitRatio(memoryOvercommitRatio);
        clusterResponse.setResourceDetails(_clusterDetailsDao.findDetails(cluster.getId()));

        if (showCapacities != null && showCapacities) {
            List<SummedCapacity> capacities = ApiDBUtils.getCapacityByClusterPodZone(null, null, cluster.getId());
            Set<CapacityResponse> capacityResponses = new HashSet<CapacityResponse>();

            for (SummedCapacity capacity : capacities) {
                CapacityResponse capacityResponse = new CapacityResponse();
                capacityResponse.setCapacityType(capacity.getCapacityType());
                capacityResponse.setCapacityName(CapacityVO.getCapacityName(capacity.getCapacityType()));
                capacityResponse.setCapacityUsed(capacity.getUsedCapacity() + capacity.getReservedCapacity());

                if (capacity.getCapacityType() == Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED) {
                    List<SummedCapacity> c = ApiDBUtils.findNonSharedStorageForClusterPodZone(null, null, cluster.getId());
                    capacityResponse.setCapacityTotal(capacity.getTotalCapacity() - c.get(0).getTotalCapacity());
                    capacityResponse.setCapacityUsed(capacity.getUsedCapacity() - c.get(0).getUsedCapacity());
                } else {
                    capacityResponse.setCapacityTotal(capacity.getTotalCapacity());
                }
                if (capacityResponse.getCapacityTotal() != 0) {
                    capacityResponse.setPercentUsed(s_percentFormat.format((float)capacityResponse.getCapacityUsed() / (float)capacityResponse.getCapacityTotal() * 100f));
                } else {
                    capacityResponse.setPercentUsed(s_percentFormat.format(0L));
                }
                capacityResponses.add(capacityResponse);
            }
            // Do it for stats as well.
            capacityResponses.addAll(getStatsCapacityresponse(null, cluster.getId(), pod.getId(), pod.getDataCenterId()));
            clusterResponse.setCapacitites(new ArrayList<CapacityResponse>(capacityResponses));
        }
        clusterResponse.setHasAnnotation(annotationDao.hasAnnotations(cluster.getUuid(), AnnotationService.EntityType.CLUSTER.name(),
                _accountMgr.isRootAdmin(CallContext.current().getCallingAccount().getId())));
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

        Network guestNtwk = ApiDBUtils.findNetworkById(fwRule.getNetworkId());
        response.setNetworkId(guestNtwk.getUuid());


        IpAddress ip = ApiDBUtils.findIpAddressById(fwRule.getSourceIpAddressId());

        if (ip != null)
        {
            response.setPublicIpAddressId(ip.getUuid());
            response.setPublicIpAddress(ip.getAddress().addr());
            if (fwRule.getDestinationIpAddress() != null)
            {
                response.setDestNatVmIp(fwRule.getDestinationIpAddress().toString());
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
        }
        FirewallRule.State state = fwRule.getState();
        String stateToSet = state.toString();
        if (state.equals(FirewallRule.State.Revoke)) {
            stateToSet = "Deleting";
        }

        // set tag information
        List<? extends ResourceTag> tags = ApiDBUtils.listByResourceTypeAndId(ResourceObjectType.PortForwardingRule, fwRule.getId());
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            CollectionUtils.addIgnoreNull(tagResponses, tagResponse);
        }
        response.setTags(tagResponses);

        response.setState(stateToSet);
        response.setForDisplay(fwRule.isDisplay());
        response.setObjectName("portforwardingrule");
        return response;
    }

    @Override
    public IpForwardingRuleResponse createIpForwardingRuleResponse(StaticNatRule fwRule) {
        IpForwardingRuleResponse response = new IpForwardingRuleResponse();
        response.setId(fwRule.getUuid());
        response.setProtocol(fwRule.getProtocol());

        IpAddress ip = ApiDBUtils.findIpAddressById(fwRule.getSourceIpAddressId());

        if (ip != null) {
            response.setPublicIpAddressId(ip.getId());
            response.setPublicIpAddress(ip.getAddress().addr());
            if (fwRule.getDestIpAddress() != null) {
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

    /*
    @Override
    public List<UserVmResponse> createUserVmResponse(String objectName, UserVm... userVms) {
        return createUserVmResponse(null, objectName, userVms);
    }

    @Override
    public List<UserVmResponse> createUserVmResponse(String objectName, EnumSet<VMDetails> details, UserVm... userVms) {
        return createUserVmResponse(null, objectName, userVms);
    }
    */

    @Override
    public List<UserVmResponse> createUserVmResponse(ResponseView view, String objectName, EnumSet<VMDetails> details, UserVm... userVms) {
        List<UserVmJoinVO> viewVms = ApiDBUtils.newUserVmView(userVms);
        return ViewResponseHelper.createUserVmResponse(view, objectName, details, viewVms.toArray(new UserVmJoinVO[viewVms.size()]));

    }

    @Override
    public List<UserVmResponse> createUserVmResponse(ResponseView view, String objectName, UserVm... userVms) {
        List<UserVmJoinVO> viewVms = ApiDBUtils.newUserVmView(userVms);
        return ViewResponseHelper.createUserVmResponse(view, objectName, viewVms.toArray(new UserVmJoinVO[viewVms.size()]));
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
        if (vm.getType() == Type.SecondaryStorageVm || vm.getType() == Type.ConsoleProxy || vm.getType() == Type.DomainRouter || vm.getType() == Type.NetScalerVm) {
            vmResponse.setId(vm.getUuid());
            vmResponse.setSystemVmType(vm.getType().toString().toLowerCase());
            vmResponse.setName(vm.getHostName());

            if (vm.getPodIdToDeployIn() != null) {
                HostPodVO pod = ApiDBUtils.findPodById(vm.getPodIdToDeployIn());
                if (pod != null) {
                    vmResponse.setPodId(pod.getUuid());
                    vmResponse.setPodName(pod.getName());
                }
            }
            VMTemplateVO template = ApiDBUtils.findTemplateById(vm.getTemplateId());
            if (template != null) {
                vmResponse.setTemplateId(template.getUuid());
                vmResponse.setTemplateName(template.getName());
            }
            vmResponse.setCreated(vm.getCreated());
            vmResponse.setHypervisor(vm.getHypervisorType().toString());

            if (vm.getHostId() != null) {
                Host host = ApiDBUtils.findHostById(vm.getHostId());
                if (host != null) {
                    vmResponse.setHostId(host.getUuid());
                    vmResponse.setHostName(host.getName());
                    vmResponse.setHostControlState(ControlState.getControlState(host.getStatus(), host.getResourceState()).toString());
                }
            }

            if (VirtualMachine.systemVMs.contains(vm.getType())) {
                Host systemVmHost = ApiDBUtils.findHostByTypeNameAndZoneId(vm.getDataCenterId(), vm.getHostName(),
                        Type.SecondaryStorageVm.equals(vm.getType()) ? Host.Type.SecondaryStorageVM : Host.Type.ConsoleProxy);
                if (systemVmHost != null) {
                    vmResponse.setAgentState(systemVmHost.getStatus());
                    vmResponse.setDisconnectedOn(systemVmHost.getDisconnectedOn());
                    vmResponse.setVersion(systemVmHost.getVersion());
                }
            }

            if (vm.getState() != null) {
                vmResponse.setState(vm.getState().toString());
            }

            vmResponse.setDynamicallyScalable(vm.isDynamicallyScalable());
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

            vmResponse.setHasAnnotation(annotationDao.hasAnnotations(vm.getUuid(), AnnotationService.EntityType.SYSTEM_VM.name(),
                    _accountMgr.isRootAdmin(CallContext.current().getCallingAccount().getId())));
            List<NicProfile> nicProfiles = ApiDBUtils.getNics(vm);
            for (NicProfile singleNicProfile : nicProfiles) {
                Network network = ApiDBUtils.findNetworkById(singleNicProfile.getNetworkId());
                if (network != null) {
                    if (network.getTrafficType() == TrafficType.Management) {
                        vmResponse.setPrivateIp(singleNicProfile.getIPv4Address());
                        vmResponse.setPrivateMacAddress(singleNicProfile.getMacAddress());
                        vmResponse.setPrivateNetmask(singleNicProfile.getIPv4Netmask());
                    } else if (network.getTrafficType() == TrafficType.Control) {
                        vmResponse.setLinkLocalIp(singleNicProfile.getIPv4Address());
                        vmResponse.setLinkLocalMacAddress(singleNicProfile.getMacAddress());
                        vmResponse.setLinkLocalNetmask(singleNicProfile.getIPv4Netmask());
                    } else if (network.getTrafficType() == TrafficType.Public) {
                        vmResponse.setPublicIp(singleNicProfile.getIPv4Address());
                        vmResponse.setPublicMacAddress(singleNicProfile.getMacAddress());
                        vmResponse.setPublicNetmask(singleNicProfile.getIPv4Netmask());
                        vmResponse.setGateway(singleNicProfile.getIPv4Gateway());
                    } else if (network.getTrafficType() == TrafficType.Guest) {
                        /*
                          * In basic zone, public ip has TrafficType.Guest in case EIP service is not enabled.
                          * When EIP service is enabled in the basic zone, system VM by default get the public
                          * IP allocated for EIP. So return the guest/public IP accordingly.
                          * */
                        NetworkOffering networkOffering = ApiDBUtils.findNetworkOfferingById(network.getNetworkOfferingId());
                        if (networkOffering.isElasticIp()) {
                            IpAddress ip = ApiDBUtils.findIpByAssociatedVmId(vm.getId());
                            if (ip != null) {
                                Vlan vlan = ApiDBUtils.findVlanById(ip.getVlanId());
                                vmResponse.setPublicIp(ip.getAddress().addr());
                                vmResponse.setPublicNetmask(vlan.getVlanNetmask());
                                vmResponse.setGateway(vlan.getVlanGateway());
                            }
                        } else {
                            vmResponse.setPublicIp(singleNicProfile.getIPv4Address());
                            vmResponse.setPublicMacAddress(singleNicProfile.getMacAddress());
                            vmResponse.setPublicNetmask(singleNicProfile.getIPv4Netmask());
                            vmResponse.setGateway(singleNicProfile.getIPv4Gateway());
                        }
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
    public DiskOfferingVO findDiskOfferingById(Long diskOfferingId) {
        return ApiDBUtils.findDiskOfferingById(diskOfferingId);
    }

    @Override
    public VpnUsersResponse createVpnUserResponse(VpnUser vpnUser) {
        VpnUsersResponse vpnResponse = new VpnUsersResponse();
        vpnResponse.setId(vpnUser.getUuid());
        vpnResponse.setUserName(vpnUser.getUsername());
        vpnResponse.setState(vpnUser.getState().toString());

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
        vpnResponse.setId(vpn.getUuid());
        vpnResponse.setForDisplay(vpn.isDisplay());
        vpnResponse.setObjectName("remoteaccessvpn");

        return vpnResponse;
    }

    @Override
    public TemplateResponse createTemplateUpdateResponse(ResponseView view, VirtualMachineTemplate result) {
        List<TemplateJoinVO> tvo = ApiDBUtils.newTemplateView(result);
        List<TemplateResponse> listVrs = ViewResponseHelper.createTemplateUpdateResponse(view, tvo.toArray(new TemplateJoinVO[tvo.size()]));
        assert listVrs != null && listVrs.size() == 1 : "There should be one template returned";
        return listVrs.get(0);
    }

    @Override
    public List<TemplateResponse> createTemplateResponses(ResponseView view, VirtualMachineTemplate result, Long zoneId, boolean readyOnly) {
        List<TemplateJoinVO> tvo = null;
        if (zoneId == null || zoneId == -1 || result.isCrossZones()) {
            tvo = ApiDBUtils.newTemplateView(result);
        } else {
            tvo = ApiDBUtils.newTemplateView(result, zoneId, readyOnly);

        }
        return ViewResponseHelper.createTemplateResponse(EnumSet.of(DomainDetails.all), view, tvo.toArray(new TemplateJoinVO[tvo.size()]));
    }

    @Override
    public List<TemplateResponse> createTemplateResponses(ResponseView view, VirtualMachineTemplate result,
                                                          List<Long> zoneIds, boolean readyOnly) {
        List<TemplateJoinVO> tvo = null;
        if (zoneIds == null) {
            return createTemplateResponses(view, result, (Long)null, readyOnly);
        } else {
            for (Long zoneId: zoneIds){
                if (tvo == null)
                    tvo = ApiDBUtils.newTemplateView(result, zoneId, readyOnly);
                else
                    tvo.addAll(ApiDBUtils.newTemplateView(result, zoneId, readyOnly));
            }
        }
        return ViewResponseHelper.createTemplateResponse(EnumSet.of(DomainDetails.all), view, tvo.toArray(new TemplateJoinVO[tvo.size()]));
    }

    @Override
    public List<TemplateResponse> createTemplateResponses(ResponseView view, long templateId, Long zoneId, boolean readyOnly) {
        VirtualMachineTemplate template = findTemplateById(templateId);
        return createTemplateResponses(view, template, zoneId, readyOnly);
    }

    @Override
    public List<TemplateResponse> createIsoResponses(ResponseView view, VirtualMachineTemplate result, Long zoneId, boolean readyOnly) {
        List<TemplateJoinVO> tvo = null;
        if (zoneId == null || zoneId == -1) {
            tvo = ApiDBUtils.newTemplateView(result);
        } else {
            tvo = ApiDBUtils.newTemplateView(result, zoneId, readyOnly);
        }

        return ViewResponseHelper.createIsoResponse(view, tvo.toArray(new TemplateJoinVO[tvo.size()]));
    }

    @Override
    public SecurityGroupResponse createSecurityGroupResponse(SecurityGroup group) {
        List<SecurityGroupJoinVO> viewSgs = ApiDBUtils.newSecurityGroupView(group);
        List<SecurityGroupResponse> listSgs = ViewResponseHelper.createSecurityGroupResponses(viewSgs);
        assert listSgs != null && listSgs.size() == 1 : "There should be one security group returned";
        return listSgs.get(0);
    }

    //TODO: we need to deprecate uploadVO, since extract is done in a synchronous fashion
    @Override
    public ExtractResponse createExtractResponse(Long id, Long zoneId, Long accountId, String mode, String url) {

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
        response.setUrl(url);
        response.setState(Upload.Status.DOWNLOAD_URL_CREATED.toString());
        Account account = ApiDBUtils.findAccountById(accountId);
        response.setAccountId(account.getUuid());

        return response;
    }

    @Override
    public ExtractResponse createExtractResponse(Long uploadId, Long id, Long zoneId, Long accountId, String mode, String url) {

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
        if (uploadId == null) {
            // region-wide image store
            response.setUrl(url);
            response.setState(Upload.Status.DOWNLOAD_URL_CREATED.toString());
        } else {
            UploadVO uploadInfo = ApiDBUtils.findUploadById(uploadId);
            response.setUploadId(uploadInfo.getUuid());
            response.setState(uploadInfo.getUploadState().toString());
            response.setUrl(uploadInfo.getUploadUrl());
        }
        Account account = ApiDBUtils.findAccountById(accountId);
        response.setAccountId(account.getUuid());

        return response;

    }

    @Override
    public String toSerializedString(CreateCmdResponse response, String responseType) {
        return ApiResponseSerializer.toSerializedString(response, responseType);
    }

    @Override
    public List<TemplateResponse> createTemplateResponses(ResponseView view, long templateId, Long snapshotId, Long volumeId, boolean readyOnly) {
        Long zoneId = null;

        if (snapshotId != null) {
            Snapshot snapshot = ApiDBUtils.findSnapshotById(snapshotId);
            VolumeVO volume = findVolumeById(snapshot.getVolumeId());

            // it seems that the volume can actually be removed from the DB at some point if it's deleted
            // if volume comes back null, use another technique to try to discover the zone
            if (volume == null) {
                SnapshotDataStoreVO snapshotStore = _snapshotStoreDao.findBySnapshot(snapshot.getId(), DataStoreRole.Primary);

                if (snapshotStore != null) {
                    long storagePoolId = snapshotStore.getDataStoreId();

                    StoragePoolVO storagePool = _storagePoolDao.findById(storagePoolId);

                    if (storagePool != null) {
                        zoneId = storagePool.getDataCenterId();
                    }
                }
            }
            else {
                zoneId = volume.getDataCenterId();
            }
        } else {
            VolumeVO volume = findVolumeById(volumeId);

            zoneId = volume.getDataCenterId();
        }

        if (zoneId == null) {
            throw new CloudRuntimeException("Unable to determine the zone ID");
        }

        return createTemplateResponses(view, templateId, zoneId, readyOnly);
    }

    @Override
    public List<TemplateResponse> createTemplateResponses(ResponseView view, long templateId, Long vmId) {
        UserVm vm = findUserVmById(vmId);
        Long hostId = (vm.getHostId() == null ? vm.getLastHostId() : vm.getHostId());
        Host host = findHostById(hostId);
        return createTemplateResponses(view, templateId, host.getDataCenterId(), true);
    }

    @Override
    public EventResponse createEventResponse(Event event) {
        EventJoinVO vEvent = ApiDBUtils.newEventView(event);
        return ApiDBUtils.newEventResponse(vEvent);
    }

    @Override
    public List<CapacityResponse> createCapacityResponse(List<? extends Capacity> result, DecimalFormat format) {
        List<CapacityResponse> capacityResponses = new ArrayList<CapacityResponse>();

        for (Capacity summedCapacity : result) {
            CapacityResponse capacityResponse = new CapacityResponse();
            capacityResponse.setCapacityTotal(summedCapacity.getTotalCapacity());
            if (summedCapacity.getAllocatedCapacity() != null) {
                capacityResponse.setCapacityAllocated(summedCapacity.getAllocatedCapacity());
            }
            capacityResponse.setCapacityType(summedCapacity.getCapacityType());
            capacityResponse.setCapacityName(CapacityVO.getCapacityName(summedCapacity.getCapacityType()));
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
            if (summedCapacity.getUsedPercentage() != null) {
                capacityResponse.setPercentUsed(format.format(summedCapacity.getUsedPercentage() * 100f));
            } else if (summedCapacity.getTotalCapacity() != 0) {
                capacityResponse.setPercentUsed(format.format((float)summedCapacity.getUsedCapacity() / (float)summedCapacity.getTotalCapacity() * 100f));
            } else {
                capacityResponse.setPercentUsed(format.format(0L));
            }

            capacityResponse.setObjectName("capacity");
            capacityResponses.add(capacityResponse);
        }

        List<VgpuTypesInfo> gpuCapacities;
        if (result.size() > 1 && (gpuCapacities = ApiDBUtils.getGpuCapacites(result.get(0).getDataCenterId(), result.get(0).getPodId(), result.get(0).getClusterId())) != null) {
            HashMap<String, Long> vgpuVMs = ApiDBUtils.getVgpuVmsCount(result.get(0).getDataCenterId(), result.get(0).getPodId(), result.get(0).getClusterId());

            float capacityUsed = 0;
            long capacityMax = 0;
            for (VgpuTypesInfo capacity : gpuCapacities) {
                if (vgpuVMs.containsKey(capacity.getGroupName().concat(capacity.getModelName()))) {
                    capacityUsed += (float)vgpuVMs.get(capacity.getGroupName().concat(capacity.getModelName())) / capacity.getMaxVpuPerGpu();
                }
                if (capacity.getModelName().equals(GPU.GPUType.passthrough.toString())) {
                    capacityMax += capacity.getMaxCapacity();
                }
            }

            DataCenter zone = ApiDBUtils.findZoneById(result.get(0).getDataCenterId());
            CapacityResponse capacityResponse = new CapacityResponse();
            if (zone != null) {
                capacityResponse.setZoneId(zone.getUuid());
                capacityResponse.setZoneName(zone.getName());
            }
            if (result.get(0).getPodId() != null) {
                HostPodVO pod = ApiDBUtils.findPodById(result.get(0).getPodId());
                capacityResponse.setPodId(pod.getUuid());
                capacityResponse.setPodName(pod.getName());
            }
            if (result.get(0).getClusterId() != null) {
                ClusterVO cluster = ApiDBUtils.findClusterById(result.get(0).getClusterId());
                capacityResponse.setClusterId(cluster.getUuid());
                capacityResponse.setClusterName(cluster.getName());
            }
            capacityResponse.setCapacityType(Capacity.CAPACITY_TYPE_GPU);
            capacityResponse.setCapacityName(CapacityVO.getCapacityName(Capacity.CAPACITY_TYPE_GPU));
            capacityResponse.setCapacityUsed((long)Math.ceil(capacityUsed));
            capacityResponse.setCapacityTotal(capacityMax);
            if (capacityMax > 0) {
                capacityResponse.setPercentUsed(format.format(capacityUsed / capacityMax * 100f));
            } else {
                capacityResponse.setPercentUsed(format.format(0));
            }
            capacityResponse.setObjectName("capacity");
            capacityResponses.add(capacityResponse);
        }
        return capacityResponses;
    }

    @Override
    public TemplatePermissionsResponse createTemplatePermissionsResponse(ResponseView view, List<String> accountNames, Long id) {
        Long templateOwnerDomain = null;
        VirtualMachineTemplate template = ApiDBUtils.findTemplateById(id);
        Account templateOwner = ApiDBUtils.findAccountById(template.getAccountId());
        if (view == ResponseView.Full) {
            // FIXME: we have just template id and need to get template owner
            // from that
            if (templateOwner != null) {
                templateOwnerDomain = templateOwner.getDomainId();
            }
        }

        TemplatePermissionsResponse response = new TemplatePermissionsResponse();
        response.setId(template.getUuid());
        response.setPublicTemplate(template.isPublicTemplate());
        if ((view == ResponseView.Full) && (templateOwnerDomain != null)) {
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
            if (account == null) {
                s_logger.error("Missing Account " + accountName + " in domain " + templateOwner.getDomainId());
                continue;
            }

            if (account.getType() != Account.Type.PROJECT) {
                regularAccounts.add(accountName);
            } else {
                // convert account to projectIds
                Project project = ApiDBUtils.findProjectByProjectAccountId(account.getId());

                if (project.getUuid() != null && !project.getUuid().isEmpty()) {
                    projectIds.add(project.getUuid());
                } else {
                    projectIds.add(String.valueOf(project.getId()));
                }
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
    public AsyncJobResponse queryJobResult(final QueryAsyncJobResultCmd cmd) {
        final Account caller = CallContext.current().getCallingAccount();

        final AsyncJob job = _entityMgr.findByIdIncludingRemoved(AsyncJob.class, cmd.getId());
        if (job == null) {
            throw new InvalidParameterValueException("Unable to find a job by id " + cmd.getId());
        }

        final User userJobOwner = _accountMgr.getUserIncludingRemoved(job.getUserId());
        final Account jobOwner = _accountMgr.getAccount(userJobOwner.getAccountId());

        //check permissions
        if (_accountMgr.isNormalUser(caller.getId())) {
            //regular users can see only jobs they own
            if (caller.getId() != jobOwner.getId()) {
                throw new PermissionDeniedException("Account " + caller + " is not authorized to see job id=" + job.getId());
            }
        } else if (_accountMgr.isDomainAdmin(caller.getId())) {
            _accountMgr.checkAccess(caller, null, true, jobOwner);
        }

        return createAsyncJobResponse(_jobMgr.queryJob(cmd.getId(), true));
    }

    public AsyncJobResponse createAsyncJobResponse(AsyncJob job) {
        AsyncJobJoinVO vJob = ApiDBUtils.newAsyncJobView(job);
        return ApiDBUtils.newAsyncJobResponse(vJob);
    }

    @Override
    public SecurityGroupResponse createSecurityGroupResponseFromSecurityGroupRule(List<? extends SecurityRule> securityRules) {
        SecurityGroupResponse response = new SecurityGroupResponse();
        Map<Long, Account> securiytGroupAccounts = new HashMap<Long, Account>();

        if ((securityRules != null) && !securityRules.isEmpty()) {
            SecurityGroupJoinVO securityGroup = ApiDBUtils.findSecurityGroupViewById(securityRules.get(0).getSecurityGroupId()).get(0);
            response.setId(securityGroup.getUuid());
            response.setName(securityGroup.getName());
            response.setDescription(securityGroup.getDescription());

            Account account = securiytGroupAccounts.get(securityGroup.getAccountId());

            if (securityGroup.getAccountType() == Account.Type.PROJECT) {
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
        if (!(offering instanceof NetworkOfferingJoinVO)) {
            offering = ApiDBUtils.newNetworkOfferingView(offering);
        }
        NetworkOfferingResponse response = ApiDBUtils.newNetworkOfferingResponse(offering);
        response.setNetworkRate(ApiDBUtils.getNetworkRate(offering.getId()));
        Long so = null;
        if (offering.getServiceOfferingId() != null) {
            so = offering.getServiceOfferingId();
        } else {
            so = ApiDBUtils.findDefaultRouterServiceOffering();
        }
        if (so != null) {
            ServiceOffering soffering = ApiDBUtils.findServiceOfferingById(so);
            if (soffering != null) {
                response.setServiceOfferingId(soffering.getUuid());
            }
        }
        Map<Service, Set<Provider>> serviceProviderMap = ApiDBUtils.listNetworkOfferingServices(offering.getId());
        List<ServiceResponse> serviceResponses = new ArrayList<ServiceResponse>();
        for (Map.Entry<Service,Set<Provider>> entry : serviceProviderMap.entrySet()) {
            Service service = entry.getKey();
            Set<Provider> srvc_providers = entry.getValue();
            ServiceResponse svcRsp = new ServiceResponse();
            // skip gateway service
            if (service == Service.Gateway) {
                continue;
            }
            svcRsp.setName(service.getName());
            List<ProviderResponse> providers = new ArrayList<ProviderResponse>();
            for (Provider provider : srvc_providers) {
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
                lbIsoaltion.setValue(offering.isDedicatedLB() ? "dedicated" : "shared");
                lbCapResponse.add(lbIsoaltion);

                CapabilityResponse eLb = new CapabilityResponse();
                eLb.setName(Capability.ElasticLb.getName());
                eLb.setValue(offering.isElasticLb() ? "true" : "false");
                lbCapResponse.add(eLb);

                CapabilityResponse inline = new CapabilityResponse();
                inline.setName(Capability.InlineMode.getName());
                inline.setValue(offering.isInline() ? "true" : "false");
                lbCapResponse.add(inline);

                CapabilityResponse vmAutoScaling = new CapabilityResponse();
                vmAutoScaling.setName(Capability.VmAutoScaling.getName());
                vmAutoScaling.setValue(offering.isSupportsVmAutoScaling() ? "true" : "false");
                lbCapResponse.add(vmAutoScaling);

                svcRsp.setCapabilities(lbCapResponse);
            } else if (Service.SourceNat == service) {
                List<CapabilityResponse> capabilities = new ArrayList<CapabilityResponse>();
                CapabilityResponse sharedSourceNat = new CapabilityResponse();
                sharedSourceNat.setName(Capability.SupportedSourceNatTypes.getName());
                sharedSourceNat.setValue(offering.isSharedSourceNat() ? "perzone" : "peraccount");
                capabilities.add(sharedSourceNat);

                CapabilityResponse redundantRouter = new CapabilityResponse();
                redundantRouter.setName(Capability.RedundantRouter.getName());
                redundantRouter.setValue(offering.isRedundantRouter() ? "true" : "false");
                capabilities.add(redundantRouter);

                svcRsp.setCapabilities(capabilities);
            } else if (service == Service.StaticNat) {
                List<CapabilityResponse> staticNatCapResponse = new ArrayList<CapabilityResponse>();

                CapabilityResponse eIp = new CapabilityResponse();
                eIp.setName(Capability.ElasticIp.getName());
                eIp.setValue(offering.isElasticIp() ? "true" : "false");
                staticNatCapResponse.add(eIp);

                CapabilityResponse associatePublicIp = new CapabilityResponse();
                associatePublicIp.setName(Capability.AssociatePublicIP.getName());
                associatePublicIp.setValue(offering.isAssociatePublicIP() ? "true" : "false");
                staticNatCapResponse.add(associatePublicIp);

                svcRsp.setCapabilities(staticNatCapResponse);
            }
            serviceResponses.add(svcRsp);
        }
        response.setForVpc(_configMgr.isOfferingForVpc(offering));
        response.setForTungsten(offering.isForTungsten());
        response.setServices(serviceResponses);
        //set network offering details
        Map<Detail, String> details = _ntwkModel.getNtwkOffDetails(offering.getId());
        if (details != null && !details.isEmpty()) {
            response.setDetails(details);
        }
        response.setHasAnnotation(annotationDao.hasAnnotations(offering.getUuid(), AnnotationService.EntityType.NETWORK_OFFERING.name(),
                _accountMgr.isRootAdmin(CallContext.current().getCallingAccount().getId())));
        return response;
    }

    private void createCapabilityResponse(List<CapabilityResponse> capabilityResponses,
                                          String name,
                                          String value,
                                          boolean canChoose,
                                          String objectName) {
        CapabilityResponse capabilityResponse = new CapabilityResponse();
        capabilityResponse.setName(name);
        capabilityResponse.setValue(value);
        capabilityResponse.setCanChoose(canChoose);
        capabilityResponse.setObjectName(objectName);

        capabilityResponses.add(capabilityResponse);
    }

    private void createCapabilityResponse(List<CapabilityResponse> capabilityResponses,
                                          String name,
                                          String value,
                                          boolean canChoose) {
        createCapabilityResponse(capabilityResponses, name, value, canChoose, null);
    }

    @Override
    public NetworkResponse createNetworkResponse(ResponseView view, Network network) {
        // need to get network profile in order to retrieve dns information from
        // there
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
        if (network.getNetworkCidr() != null) {
            response.setNetworkCidr((network.getNetworkCidr()));
        }
        // If network has reservation its entire network cidr is defined by
        // getNetworkCidr()
        // if no reservation is present then getCidr() will define the entire
        // network cidr
        if (network.getNetworkCidr() != null) {
            response.setNetmask(NetUtils.cidr2Netmask(network.getNetworkCidr()));
        }
        if (((network.getCidr()) != null) && (network.getNetworkCidr() == null)) {
            response.setNetmask(NetUtils.cidr2Netmask(network.getCidr()));
        }

        response.setIp6Gateway(network.getIp6Gateway());
        response.setIp6Cidr(network.getIp6Cidr());

        // create response for reserved IP ranges that can be used for
        // non-cloudstack purposes
        String reservation = null;
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

            if (startVmIp == startGuestIp && endVmIp < endGuestIp - 1) {
                reservation = (NetUtils.long2Ip(endVmIp + 1) + "-" + NetUtils.long2Ip(endGuestIp));
            }
            if (endVmIp == endGuestIp && startVmIp > startGuestIp + 1) {
                reservation = (NetUtils.long2Ip(startGuestIp) + "-" + NetUtils.long2Ip(startVmIp - 1));
            }
            if (startVmIp > startGuestIp + 1 && endVmIp < endGuestIp - 1) {
                reservation = (NetUtils.long2Ip(startGuestIp) + "-" + NetUtils.long2Ip(startVmIp - 1) + " ,  " + NetUtils.long2Ip(endVmIp + 1) + "-" + NetUtils.long2Ip(endGuestIp));
            }
        }
        response.setReservedIpRange(reservation);

        // return vlan information only to Root admin
        if (network.getBroadcastUri() != null && view == ResponseView.Full) {
            String broadcastUri = network.getBroadcastUri().toString();
            response.setBroadcastUri(broadcastUri);
            String vlan = "N/A";
            switch (BroadcastDomainType.getSchemeValue(network.getBroadcastUri())) {
            case Vlan:
            case Vxlan:
                vlan = BroadcastDomainType.getValue(network.getBroadcastUri());
                break;
            }
            // return vlan information only to Root admin
            response.setVlan(vlan);
        }

        // return network details only to Root admin
        if (view == ResponseView.Full) {
            Map<String, String> details = new HashMap<>();
            for (NetworkDetailVO detail: networkDetailsDao.listDetails(network.getId())) {
                details.put(detail.getName(),detail.getValue());
            }
            response.setDetails(details);
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
        NetworkOffering networkOffering = ApiDBUtils.findNetworkOfferingById(network.getNetworkOfferingId());
        if (networkOffering != null) {
            response.setNetworkOfferingId(networkOffering.getUuid());
            response.setNetworkOfferingName(networkOffering.getName());
            response.setNetworkOfferingDisplayText(networkOffering.getDisplayText());
            response.setNetworkOfferingConserveMode(networkOffering.isConserveMode());
            response.setIsSystem(networkOffering.isSystemOnly());
            response.setNetworkOfferingAvailability(networkOffering.getAvailability().toString());
            response.setIsPersistent(networkOffering.isPersistent());
            if (Network.GuestType.Isolated.equals(network.getGuestType()) && network.getVpcId() == null) {
                response.setEgressDefaultPolicy(networkOffering.isEgressDefaultPolicy());
            }
        }

        if (network.getAclType() != null) {
            response.setAclType(network.getAclType().toString());
        }
        response.setDisplayNetwork(network.getDisplayNetwork());
        response.setState(network.getState().toString());
        response.setRestartRequired(network.isRestartRequired());
        NetworkVO nw = ApiDBUtils.findNetworkById(network.getRelated());
        if (nw != null) {
            response.setRelated(nw.getUuid());
        }
        response.setNetworkDomain(network.getNetworkDomain());
        response.setPublicMtu(network.getPublicMtu());
        response.setPrivateMtu(network.getPrivateMtu());
        response.setDns1(profile.getDns1());
        response.setDns2(profile.getDns2());
        response.setIpv6Dns1(profile.getIp6Dns1());
        response.setIpv6Dns2(profile.getIp6Dns2());
        // populate capability
        Map<Service, Map<Capability, String>> serviceCapabilitiesMap = ApiDBUtils.getNetworkCapabilities(network.getId(), network.getDataCenterId());
        Map<Service, Set<Provider>> serviceProviderMap = ApiDBUtils.listNetworkOfferingServices(network.getNetworkOfferingId());
        List<ServiceResponse> serviceResponses = new ArrayList<ServiceResponse>();
        if (serviceCapabilitiesMap != null) {
            for (Map.Entry<Service, Map<Capability, String>>entry : serviceCapabilitiesMap.entrySet()) {
                Service service = entry.getKey();
                ServiceResponse serviceResponse = new ServiceResponse();
                // skip gateway service
                if (service == Service.Gateway) {
                    continue;
                }
                serviceResponse.setName(service.getName());

                // set list of capabilities for the service
                List<CapabilityResponse> capabilityResponses = new ArrayList<>();
                Map<Capability, String> serviceCapabilities = entry.getValue();
                if (serviceCapabilities != null) {
                    for (Map.Entry<Capability,String> ser_cap_entries : serviceCapabilities.entrySet()) {
                        Capability capability = ser_cap_entries.getKey();
                        String capabilityValue = ser_cap_entries.getValue();
                        if (Service.Lb == service && capability.getName().equals(Capability.SupportedLBIsolation.getName())) {
                             capabilityValue = networkOffering.isDedicatedLB() ? "dedicated" : "shared";
                        }

                        Set<String> capabilitySet = new HashSet<>(Arrays.asList(Capability.SupportedLBIsolation.getName(),
                                Capability.SupportedSourceNatTypes.getName(),
                                Capability.RedundantRouter.getName()));
                        boolean canChoose = capabilitySet.contains(capability.getName());

                        createCapabilityResponse(capabilityResponses, capability.getName(),
                                capabilityValue, canChoose, "capability");
                    }
                }

                if (Service.SourceNat == service) {
                    // overwrite
                    capabilityResponses = new ArrayList<>();
                    createCapabilityResponse(capabilityResponses, Capability.SupportedSourceNatTypes.getName(),
                            networkOffering.isSharedSourceNat() ? "perzone" : "peraccount", true);

                    createCapabilityResponse(capabilityResponses, Capability.RedundantRouter.getName(),
                            networkOffering.isRedundantRouter() ? "true" : "false", true);
                } else if (service == Service.StaticNat) {
                    createCapabilityResponse(capabilityResponses, Capability.ElasticIp.getName(),
                            networkOffering.isElasticIp() ? "true" : "false", false);

                    createCapabilityResponse(capabilityResponses, Capability.AssociatePublicIP.getName(),
                            networkOffering.isAssociatePublicIP() ? "true" : "false", false);
                } else if (Service.Lb == service) {
                    createCapabilityResponse(capabilityResponses, Capability.ElasticLb.getName(),
                            networkOffering.isElasticLb() ? "true" : "false", false);

                    createCapabilityResponse(capabilityResponses, Capability.InlineMode.getName(),
                            networkOffering.isInline() ? "true" : "false", false);
                }
                serviceResponse.setCapabilities(capabilityResponses);

                List<ProviderResponse> providers = new ArrayList<>();
                for (Provider provider : serviceProviderMap.get(service)) {
                    if (provider != null) {
                        ProviderResponse providerRsp = new ProviderResponse();
                        providerRsp.setName(provider.getName());
                        providers.add(providerRsp);
                    }
                }
                serviceResponse.setProviders(providers);

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
                response.setDomainName(domain.getName());
            }

        }

        response.setSpecifyIpRanges(network.getSpecifyIpRanges());
        if (network.getVpcId() != null) {
            Vpc vpc = ApiDBUtils.findVpcById(network.getVpcId());
            if (vpc != null) {
                response.setVpcId(vpc.getUuid());
                response.setVpcName(vpc.getName());
            }
        }

        setResponseAssociatedNetworkInformation(response, network.getId());

        response.setCanUseForDeploy(ApiDBUtils.canUseForDeploy(network));

        // set tag information
        List<? extends ResourceTag> tags = ApiDBUtils.listByResourceTypeAndId(ResourceObjectType.Network, network.getId());
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            CollectionUtils.addIgnoreNull(tagResponses, tagResponse);
        }
        response.setTags(tagResponses);
        response.setHasAnnotation(annotationDao.hasAnnotations(network.getUuid(), AnnotationService.EntityType.NETWORK.name(),
                _accountMgr.isRootAdmin(CallContext.current().getCallingAccount().getId())));

        if (network.getNetworkACLId() != null) {
            NetworkACL acl = ApiDBUtils.findByNetworkACLId(network.getNetworkACLId());
            if (acl != null) {
                response.setAclId(acl.getUuid());
                response.setAclName(acl.getName());
            }
        }

        response.setStrechedL2Subnet(network.isStrechedL2Network());
        if (network.isStrechedL2Network()) {
            Set<String> networkSpannedZones = new  HashSet<String>();
            List<VMInstanceVO> vmInstances = new ArrayList<VMInstanceVO>();
            vmInstances.addAll(ApiDBUtils.listUserVMsByNetworkId(network.getId()));
            vmInstances.addAll(ApiDBUtils.listDomainRoutersByNetworkId(network.getId()));
            for (VirtualMachine vm : vmInstances) {
                DataCenter vmZone = ApiDBUtils.findZoneById(vm.getDataCenterId());
                networkSpannedZones.add(vmZone.getUuid());
            }
            response.setNetworkSpannedZones(networkSpannedZones);
        }
        response.setExternalId(network.getExternalId());
        response.setRedundantRouter(network.isRedundant());
        response.setCreated(network.getCreated());
        response.setSupportsVmAutoScaling(networkOfferingDao.findByIdIncludingRemoved(network.getNetworkOfferingId()).isSupportsVmAutoScaling());

        Long bytesReceived = 0L;
        Long bytesSent = 0L;
        SearchBuilder<UserStatisticsVO> sb = userStatsDao.createSearchBuilder();
        sb.and("networkId", sb.entity().getNetworkId(), Op.EQ);
        SearchCriteria<UserStatisticsVO> sc = sb.create();
        sc.setParameters("networkId", network.getId());
        for (UserStatisticsVO stat: userStatsDao.search(sc, null)) {
            bytesReceived += stat.getNetBytesReceived() + stat.getCurrentBytesReceived();
            bytesSent += stat.getNetBytesSent() + stat.getCurrentBytesSent();
        }
        response.setBytesReceived(bytesReceived);
        response.setBytesSent(bytesSent);

        if (networkOfferingDao.isIpv6Supported(network.getNetworkOfferingId())) {
            response.setInternetProtocol(networkOfferingDao.getNetworkOfferingInternetProtocol(network.getNetworkOfferingId(), NetUtils.InternetProtocol.IPv4).toString());
            response.setIpv6Routing(Network.Routing.Static.toString());
            response.setIpv6Routes(new LinkedHashSet<>());
            if (Network.GuestType.Isolated.equals(networkOffering.getGuestType())) {
                List<String> ipv6Addresses = ipv6Service.getPublicIpv6AddressesForNetwork(network);
                for (String address : ipv6Addresses) {
                    Ipv6RouteResponse route = new Ipv6RouteResponse(network.getIp6Cidr(), address);
                    response.addIpv6Route(route);
                }
            }
        }

        response.setObjectName("network");
        return response;
    }

    private void setResponseAssociatedNetworkInformation(BaseResponseWithAssociatedNetwork response, Long networkId) {
        final NetworkDetailVO detail = networkDetailsDao.findDetail(networkId, Network.AssociatedNetworkId);
        if (detail != null) {
            Long associatedNetworkId = Long.valueOf(detail.getValue());
            NetworkVO associatedNetwork = ApiDBUtils.findNetworkById(associatedNetworkId);
            if (associatedNetwork != null) {
                response.setAssociatedNetworkId(associatedNetwork.getUuid());
                response.setAssociatedNetworkName(associatedNetwork.getName());
            }
        }
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
        List<ProjectResponse> listPrjs = ViewResponseHelper.createProjectResponse(EnumSet.of(DomainDetails.all), viewPrjs.toArray(new ProjectJoinVO[viewPrjs.size()]));
        assert listPrjs != null && listPrjs.size() == 1 : "There should be one project  returned";
        return listPrjs.get(0);
    }

    @Override
    public FirewallResponse createFirewallResponse(FirewallRule fwRule) {
        FirewallResponse response = new FirewallResponse();

        response.setId(fwRule.getUuid());
        response.setProtocol(fwRule.getProtocol());
        if (fwRule.getSourcePortStart() != null) {
            response.setStartPort(fwRule.getSourcePortStart());
        }

        if (fwRule.getSourcePortEnd() != null) {
            response.setEndPort(fwRule.getSourcePortEnd());
        }

        List<String> cidrs = ApiDBUtils.findFirewallSourceCidrs(fwRule.getId());
        response.setCidrList(StringUtils.join(cidrs, ","));

        if(fwRule.getTrafficType() == FirewallRule.TrafficType.Egress){
            List<String> destCidrs = ApiDBUtils.findFirewallDestCidrs(fwRule.getId());
            response.setDestCidr(StringUtils.join(destCidrs,","));
        }

        if (fwRule.getTrafficType() == FirewallRule.TrafficType.Ingress) {
            IpAddress ip = ApiDBUtils.findIpAddressById(fwRule.getSourceIpAddressId());
            response.setPublicIpAddressId(ip.getUuid());
            response.setPublicIpAddress(ip.getAddress().addr());
        }

            Network network = ApiDBUtils.findNetworkById(fwRule.getNetworkId());
            response.setNetworkId(network.getUuid());

        FirewallRule.State state = fwRule.getState();
        String stateToSet = state.toString();
        if (state.equals(FirewallRule.State.Revoke)) {
            stateToSet = "Deleting";
        }

        response.setIcmpCode(fwRule.getIcmpCode());
        response.setIcmpType(fwRule.getIcmpType());
        response.setForDisplay(fwRule.isDisplay());

        // set tag information
        List<? extends ResourceTag> tags = ApiDBUtils.listByResourceTypeAndId(ResourceObjectType.FirewallRule, fwRule.getId());
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            CollectionUtils.addIgnoreNull(tagResponses, tagResponse);
        }
        response.setTags(tagResponses);

        response.setState(stateToSet);
        response.setObjectName("firewallrule");
        return response;
    }

    @Override
    public NetworkACLItemResponse createNetworkACLItemResponse(NetworkACLItem aclItem) {
        NetworkACLItemResponse response = new NetworkACLItemResponse();

        response.setId(aclItem.getUuid());
        response.setProtocol(aclItem.getProtocol());
        if (aclItem.getSourcePortStart() != null) {
            response.setStartPort(Integer.toString(aclItem.getSourcePortStart()));
        }

        if (aclItem.getSourcePortEnd() != null) {
            response.setEndPort(Integer.toString(aclItem.getSourcePortEnd()));
        }

        response.setCidrList(StringUtils.join(aclItem.getSourceCidrList(), ","));

        response.setTrafficType(aclItem.getTrafficType().toString());

        NetworkACLItem.State state = aclItem.getState();
        String stateToSet = state.toString();
        if (state.equals(NetworkACLItem.State.Revoke)) {
            stateToSet = "Deleting";
        }

        response.setIcmpCode(aclItem.getIcmpCode());
        response.setIcmpType(aclItem.getIcmpType());

        response.setState(stateToSet);
        response.setNumber(aclItem.getNumber());
        response.setAction(aclItem.getAction().toString());
        response.setForDisplay(aclItem.isDisplay());

        NetworkACL acl = ApiDBUtils.findByNetworkACLId(aclItem.getAclId());
        if (acl != null) {
            response.setAclId(acl.getUuid());
            response.setAclName(acl.getName());
        }

        //set tag information
        List<? extends ResourceTag> tags = ApiDBUtils.listByResourceTypeAndId(ResourceObjectType.NetworkACL, aclItem.getId());
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            CollectionUtils.addIgnoreNull(tagResponses, tagResponse);
        }
        response.setTags(tagResponses);
        response.setReason(aclItem.getReason());
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
        hpvCapabilitiesResponse.setMaxDataVolumesLimit(hpvCapabilities.getMaxDataVolumesLimit());
        hpvCapabilitiesResponse.setMaxHostsPerCluster(hpvCapabilities.getMaxHostsPerCluster());
        hpvCapabilitiesResponse.setIsStorageMotionSupported(hpvCapabilities.isStorageMotionSupported());
        hpvCapabilitiesResponse.setVmSnapshotEnabled(hpvCapabilities.isVmSnapshotEnabled());
        return hpvCapabilitiesResponse;
    }

    // TODO: we may need to refactor once ControlledEntityResponse and
    // ControlledEntity id to uuid conversion are all done.
    // currently code is scattered in
    private void populateOwner(ControlledEntityResponse response, ControlledEntity object) {
        Account account = ApiDBUtils.findAccountById(object.getAccountId());

        if (account.getType() == Account.Type.PROJECT) {
            // find the project
            Project project = ApiDBUtils.findProjectByProjectAccountId(account.getId());
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

        if (object.getAccountType() == Account.Type.PROJECT) {
            response.setProjectId(object.getProjectUuid());
            response.setProjectName(object.getProjectName());
        } else {
            response.setAccountName(object.getAccountName());
        }

        response.setDomainId(object.getDomainUuid());
        response.setDomainName(object.getDomainName());
    }

    private void populateAccount(ControlledEntityResponse response, long accountId) {
        Account account = ApiDBUtils.findAccountById(accountId);
        if (account == null) {
            s_logger.debug("Unable to find account with id: " + accountId);
        } else if (account.getType() == Account.Type.PROJECT) {
            // find the project
            Project project = ApiDBUtils.findProjectByProjectAccountId(account.getId());
            if (project != null) {
                response.setProjectId(project.getUuid());
                response.setProjectName(project.getName());
                response.setAccountName(account.getAccountName());
            } else {
                s_logger.debug("Unable to find project with id: " + account.getId());
            }
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
            VirtualRouter router = (VirtualRouter)vm;
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
            response.setZoneName(zone.getName());
        }
        response.setNetworkSpeed(result.getSpeed());
        response.setVlan(result.getVnetString());
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
    public GuestVlanRangeResponse createDedicatedGuestVlanRangeResponse(GuestVlanRange vlan) {
        GuestVlanRangeResponse guestVlanRangeResponse = new GuestVlanRangeResponse();

        guestVlanRangeResponse.setId(vlan.getUuid());
        Long accountId = ApiDBUtils.getAccountIdForGuestVlan(vlan.getId());
        Account owner = ApiDBUtils.findAccountById(accountId);
        if (owner != null) {
            populateAccount(guestVlanRangeResponse, owner.getId());
            populateDomain(guestVlanRangeResponse, owner.getDomainId());
        }
        guestVlanRangeResponse.setGuestVlanRange(vlan.getGuestVlanRange());
        guestVlanRangeResponse.setPhysicalNetworkId(vlan.getPhysicalNetworkId());
        PhysicalNetworkVO physicalNetwork = ApiDBUtils.findPhysicalNetworkById(vlan.getPhysicalNetworkId());
        guestVlanRangeResponse.setZoneId(physicalNetwork.getDataCenterId());

        return guestVlanRangeResponse;
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
            if (cap.getName().equals(Capability.SupportedLBIsolation.getName()) || cap.getName().equals(Capability.SupportedSourceNatTypes.getName())
                    || cap.getName().equals(Capability.RedundantRouter.getName())) {
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
            // return only Virtual Router/JuniperSRX/CiscoVnmc as a provider for the firewall
            if (service == Service.Firewall
                    && !(serviceProvider == Provider.VirtualRouter || serviceProvider == Provider.CiscoVnmc || serviceProvider == Provider.PaloAlto || serviceProvider == Provider.BigSwitchBcf || serviceProvider == Provider.Tungsten)) {
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

        Provider serviceProvider = Provider.getProvider(result.getProviderName());
        boolean canEnableIndividualServices = ApiDBUtils.canElementEnableIndividualServices(serviceProvider);
        response.setCanEnableIndividualServices(canEnableIndividualServices);

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
        response.setHypervLabel(result.getHypervNetworkLabel());
        response.setOvm3Label(result.getOvm3NetworkLabel());

        response.setObjectName("traffictype");
        return response;
    }

    @Override
    public VirtualRouterProviderResponse createVirtualRouterProviderResponse(VirtualRouterProvider result) {
        //generate only response of the VR/VPCVR provider type
        if (!(result.getType() == VirtualRouterProvider.Type.VirtualRouter || result.getType() == VirtualRouterProvider.Type.VPCVirtualRouter)) {
            return null;
        }
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
    public OvsProviderResponse createOvsProviderResponse(OvsProvider result) {

        OvsProviderResponse response = new OvsProviderResponse();
        response.setId(result.getUuid());
        PhysicalNetworkServiceProvider nsp = ApiDBUtils.findPhysicalNetworkServiceProviderById(result.getNspId());
        if (nsp != null) {
            response.setNspId(nsp.getUuid());
        }
        response.setEnabled(result.isEnabled());

        response.setObjectName("ovselement");
        return response;
    }

    @Override
    public LBStickinessResponse createLBStickinessPolicyResponse(StickinessPolicy stickinessPolicy, LoadBalancer lb) {
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
        LBStickinessPolicyResponse ruleResponse = new LBStickinessPolicyResponse(stickinessPolicy);
        responses.add(ruleResponse);

        spResponse.setRules(responses);

        spResponse.setObjectName("stickinesspolicies");
        return spResponse;
    }

    @Override
    public LBStickinessResponse createLBStickinessPolicyResponse(List<? extends StickinessPolicy> stickinessPolicies, LoadBalancer lb) {
        LBStickinessResponse spResponse = new LBStickinessResponse();

        if (lb == null) {
            return spResponse;
        }
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
    public LBHealthCheckResponse createLBHealthCheckPolicyResponse(List<? extends HealthCheckPolicy> healthcheckPolicies, LoadBalancer lb) {
        LBHealthCheckResponse hcResponse = new LBHealthCheckResponse();

        if (lb == null) {
            return hcResponse;
        }
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
        response.setGslbServiceEnabled(region.checkIfServiceEnabled(Region.Service.Gslb));
        response.setPortableipServiceEnabled(region.checkIfServiceEnabled(Region.Service.PortableIp));
        return response;
    }

    @Override
    public ResourceTagResponse createResourceTagResponse(ResourceTag resourceTag, boolean keyValueOnly) {
        ResourceTagJoinVO rto = ApiDBUtils.newResourceTagView(resourceTag);
        if(rto == null)
            return null;
        return ApiDBUtils.newResourceTagResponse(rto, keyValueOnly);
    }

    @Override
    public VpcOfferingResponse createVpcOfferingResponse(VpcOffering offering) {
        if (!(offering instanceof VpcOfferingJoinVO)) {
            offering = ApiDBUtils.newVpcOfferingView(offering);
        }
        VpcOfferingResponse response = ApiDBUtils.newVpcOfferingResponse(offering);
        Map<Service, Set<Provider>> serviceProviderMap = ApiDBUtils.listVpcOffServices(offering.getId());
        List<ServiceResponse> serviceResponses = new ArrayList<ServiceResponse>();
        for (Map.Entry<Service, Set<Provider>> entry : serviceProviderMap.entrySet()) {
            Service service = entry.getKey();
            Set<Provider> srvc_providers = entry.getValue();

            ServiceResponse svcRsp = new ServiceResponse();
            // skip gateway service
            if (service == Service.Gateway) {
                continue;
            }
            svcRsp.setName(service.getName());
            List<ProviderResponse> providers = new ArrayList<ProviderResponse>();
            for (Provider provider : srvc_providers) {
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
        return response;
    }

    @Override
    public VpcResponse createVpcResponse(ResponseView view, Vpc vpc) {
        VpcResponse response = new VpcResponse();
        response.setId(vpc.getUuid());
        response.setName(vpc.getName());
        response.setDisplayText(vpc.getDisplayText());
        response.setCreated(vpc.getCreated());
        response.setState(vpc.getState().name());
        VpcOffering voff = ApiDBUtils.findVpcOfferingById(vpc.getVpcOfferingId());
        if (voff != null) {
            response.setVpcOfferingId(voff.getUuid());
            response.setVpcOfferingName(voff.getName());
        }
        response.setCidr(vpc.getCidr());
        response.setRestartRequired(vpc.isRestartRequired());
        response.setNetworkDomain(vpc.getNetworkDomain());
        response.setForDisplay(vpc.isDisplay());
        response.setUsesDistributedRouter(vpc.usesDistributedRouter());
        response.setRedundantRouter(vpc.isRedundant());
        response.setRegionLevelVpc(vpc.isRegionLevelVpc());

        Map<Service, Set<Provider>> serviceProviderMap = ApiDBUtils.listVpcOffServices(vpc.getVpcOfferingId());
        List<ServiceResponse> serviceResponses = new ArrayList<ServiceResponse>();
        for (Map.Entry<Service,Set<Provider>>entry : serviceProviderMap.entrySet()) {
            Service service = entry.getKey();
            Set<Provider> serviceProviders = entry.getValue();
            ServiceResponse svcRsp = new ServiceResponse();
            // skip gateway service
            if (service == Service.Gateway) {
                continue;
            }
            svcRsp.setName(service.getName());
            List<ProviderResponse> providers = new ArrayList<ProviderResponse>();
            for (Provider provider : serviceProviders) {
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
            NetworkResponse ntwkRsp = createNetworkResponse(view, network);
            networkResponses.add(ntwkRsp);
        }

        DataCenter zone = ApiDBUtils.findZoneById(vpc.getZoneId());
        if (zone != null) {
            response.setZoneId(zone.getUuid());
            response.setZoneName(zone.getName());
        }

        response.setNetworks(networkResponses);
        response.setServices(serviceResponses);
        response.setPublicMtu(vpc.getPublicMtu());
        populateOwner(response, vpc);

        // set tag information
        List<? extends ResourceTag> tags = ApiDBUtils.listByResourceTypeAndId(ResourceObjectType.Vpc, vpc.getId());
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            CollectionUtils.addIgnoreNull(tagResponses, tagResponse);
        }
        response.setTags(tagResponses);
        response.setHasAnnotation(annotationDao.hasAnnotations(vpc.getUuid(), AnnotationService.EntityType.VPC.name(),
                _accountMgr.isRootAdmin(CallContext.current().getCallingAccount().getId())));
        ipv6Service.updateIpv6RoutesForVpcResponse(vpc, response);
        response.setDns1(vpc.getIp4Dns1());
        response.setDns2(vpc.getIp4Dns2());
        response.setIpv6Dns1(vpc.getIp6Dns1());
        response.setIpv6Dns2(vpc.getIp6Dns2());
        response.setObjectName("vpc");
        return response;
    }

    @Override
    public PrivateGatewayResponse createPrivateGatewayResponse(PrivateGateway result) {
        PrivateGatewayResponse response = new PrivateGatewayResponse();
        response.setId(result.getUuid());
        response.setBroadcastUri(result.getBroadcastUri());
        response.setGateway(result.getGateway());
        response.setNetmask(result.getNetmask());
        if (result.getVpcId() != null) {
            Vpc vpc = ApiDBUtils.findVpcById(result.getVpcId());
            response.setVpcId(vpc.getUuid());
            response.setVpcName(vpc.getName());
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
        response.setSourceNat(result.getSourceNat());

        NetworkACL acl =  ApiDBUtils.findByNetworkACLId(result.getNetworkACLId());
        if (acl != null) {
            response.setAclId(acl.getUuid());
            response.setAclName(acl.getName());
        }

        setResponseAssociatedNetworkInformation(response, result.getNetworkId());

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
        response.setProvider(counter.getProvider());
        response.setObjectName("counter");
        return response;
    }

    @Override
    public ConditionResponse createConditionResponse(Condition condition) {
        ConditionResponse response = new ConditionResponse();
        response.setId(condition.getUuid());
        Counter counter = ApiDBUtils.getCounter(condition.getCounterId());
        response.setCounterId(counter.getUuid());
        response.setCounterName(counter.getName());
        CounterResponse counterResponse = createCounterResponse(counter);
        response.setCounterResponse(counterResponse);
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
        response.setUserData(profile.getUserData());
        response.setOtherDeployParams(profile.getOtherDeployParamsList());
        response.setCounterParams(profile.getCounterParams());
        response.setExpungeVmGracePeriod(profile.getExpungeVmGracePeriod());
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
        response.setName(policy.getName());
        response.setDuration(policy.getDuration());
        response.setQuietTime(policy.getQuietTime());
        response.setAction(policy.getAction().toString());
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
        response.setName(vmGroup.getName());
        response.setMinMembers(vmGroup.getMinMembers());
        response.setMaxMembers(vmGroup.getMaxMembers());
        response.setState(vmGroup.getState().toString());
        response.setInterval(vmGroup.getInterval());
        response.setForDisplay(vmGroup.isDisplay());
        response.setCreated(vmGroup.getCreated());
        AutoScaleVmProfileVO profile = ApiDBUtils.findAutoScaleVmProfileById(vmGroup.getProfileId());
        if (profile != null) {
            response.setProfileId(profile.getUuid());
        }
        response.setAvailableVirtualMachineCount(ApiDBUtils.countAvailableVmsByGroupId(vmGroup.getId()));
        LoadBalancerVO fw = ApiDBUtils.findLoadBalancerById(vmGroup.getLoadBalancerId());
        if (fw != null) {
            response.setLoadBalancerId(fw.getUuid());

            NetworkVO network = ApiDBUtils.findNetworkById(fw.getNetworkId());

            if (network != null) {
                response.setNetworkName(network.getName());
                response.setNetworkId(network.getUuid());

                String provider = ntwkSrvcDao.getProviderForServiceInNetwork(network.getId(), Service.Lb);
                if (provider != null) {
                    response.setLbProvider(provider);
                } else {
                    response.setLbProvider(Network.Provider.None.toString());
                }
            }

            IPAddressVO publicIp = ApiDBUtils.findIpAddressById(fw.getSourceIpAddressId());
            if (publicIp != null) {
                response.setPublicIpId(publicIp.getUuid());
                response.setPublicIp(publicIp.getAddress().addr());
                response.setPublicPort(Integer.toString(fw.getSourcePortStart()));
                response.setPrivatePort(Integer.toString(fw.getDefaultPortStart()));
            }
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

        response.setHasAnnotation(annotationDao.hasAnnotations(vmGroup.getUuid(), AnnotationService.EntityType.AUTOSCALE_VM_GROUP.name(),
                _accountMgr.isRootAdmin(CallContext.current().getCallingAccount().getId())));

        populateOwner(response, vmGroup);
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
        if (state.equals(StaticRoute.State.Revoke)) {
            state = StaticRoute.State.Deleting;
        }
        response.setState(state.toString());
        populateAccount(response, result.getAccountId());
        populateDomain(response, result.getDomainId());

        // set tag information
        List<? extends ResourceTag> tags = ApiDBUtils.listByResourceTypeAndId(ResourceObjectType.StaticRoute, result.getId());
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            CollectionUtils.addIgnoreNull(tagResponses,tagResponse);
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
            response.setVpcId(vpc.getUuid());
            response.setVpcName(vpc.getName());
        }
        response.setRemoved(result.getRemoved());
        response.setForDisplay(result.isDisplay());
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
        response.setEncap(result.getEncap());
        response.setRemoved(result.getRemoved());
        response.setIkeVersion(result.getIkeVersion());
        response.setSplitConnections(result.getSplitConnections());
        response.setObjectName("vpncustomergateway");
        response.setHasAnnotation(annotationDao.hasAnnotations(result.getUuid(), AnnotationService.EntityType.VPN_CUSTOMER_GATEWAY.name(),
                _accountMgr.isRootAdmin(CallContext.current().getCallingAccount().getId())));

        populateAccount(response, result.getAccountId());
        populateDomain(response, result.getDomainId());

        return response;
    }

    @Override
    public Site2SiteVpnConnectionResponse createSite2SiteVpnConnectionResponse(Site2SiteVpnConnection result) {
        Site2SiteVpnConnectionResponse response = new Site2SiteVpnConnectionResponse();
        response.setId(result.getUuid());
        response.setPassive(result.isPassive());

        Long vpnGatewayId = result.getVpnGatewayId();
        if (vpnGatewayId != null) {
            Site2SiteVpnGateway vpnGateway = ApiDBUtils.findVpnGatewayById(vpnGatewayId);
            if (vpnGateway != null) {
                response.setVpnGatewayId(vpnGateway.getUuid());
                long ipId = vpnGateway.getAddrId();
                IPAddressVO ipObj = ApiDBUtils.findIpAddressById(ipId);
                response.setIp(ipObj.getAddress().addr());
            }
        }

        Long customerGatewayId = result.getCustomerGatewayId();
        if (customerGatewayId != null) {
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
                response.setEncap(customerGateway.getEncap());
                response.setIkeVersion(customerGateway.getIkeVersion());
                response.setSplitConnections(customerGateway.getSplitConnections());
            }
        }

        populateAccount(response, result.getAccountId());
        populateDomain(response, result.getDomainId());

        response.setState(result.getState().toString());
        response.setCreated(result.getCreated());
        response.setRemoved(result.getRemoved());
        response.setForDisplay(result.isDisplay());
        response.setObjectName("vpnconnection");
        return response;
    }

    @Override
    public GuestOSResponse createGuestOSResponse(GuestOS guestOS) {
        GuestOSResponse response = new GuestOSResponse();
        response.setDescription(guestOS.getDisplayName());
        response.setId(guestOS.getUuid());
        response.setIsUserDefined(guestOS.getIsUserDefined());
        GuestOSCategoryVO category = ApiDBUtils.findGuestOsCategoryById(guestOS.getCategoryId());
        if (category != null) {
            response.setOsCategoryId(category.getUuid());
        }

        response.setObjectName("ostype");
        return response;
    }

    @Override
    public GuestOsMappingResponse createGuestOSMappingResponse(GuestOSHypervisor guestOSHypervisor) {
        GuestOsMappingResponse response = new GuestOsMappingResponse();
        response.setId(guestOSHypervisor.getUuid());
        response.setHypervisor(guestOSHypervisor.getHypervisorType());
        response.setHypervisorVersion(guestOSHypervisor.getHypervisorVersion());
        response.setOsNameForHypervisor((guestOSHypervisor.getGuestOsName()));
        response.setIsUserDefined(Boolean.valueOf(guestOSHypervisor.getIsUserDefined()).toString());
        GuestOS guestOs = ApiDBUtils.findGuestOSById(guestOSHypervisor.getGuestOsId());
        if (guestOs != null) {
            response.setOsStdName(guestOs.getDisplayName());
            response.setOsTypeId(guestOs.getUuid());
        }

        response.setObjectName("guestosmapping");
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
    public Map<String, Set<ResourceTagResponse>> getUsageResourceTags()
    {
        try {
            return _resourceTagDao.listTags();
        } catch(Exception ex) {
            s_logger.warn("Failed to get resource details for Usage data due to exception : ", ex);
        }
        return null;
    }

    @Override
    public UsageRecordResponse createUsageResponse(Usage usageRecord) {
        return createUsageResponse(usageRecord, null, false);
    }

    @Override
    public UsageRecordResponse createUsageResponse(Usage usageRecord, Map<String, Set<ResourceTagResponse>> resourceTagResponseMap, boolean oldFormat) {
        UsageRecordResponse usageRecResponse = new UsageRecordResponse();
        Account account = ApiDBUtils.findAccountById(usageRecord.getAccountId());
        if (account.getType() == Account.Type.PROJECT) {
            //find the project
            Project project = ApiDBUtils.findProjectByProjectAccountIdIncludingRemoved(account.getId());
            if (project != null) {
                usageRecResponse.setProjectId(project.getUuid());
                usageRecResponse.setProjectName(project.getName());
            }
        } else {
            usageRecResponse.setAccountId(account.getUuid());
            usageRecResponse.setAccountName(account.getAccountName());
        }

        Domain domain = ApiDBUtils.findDomainById(usageRecord.getDomainId());
        if (domain != null) {
            usageRecResponse.setDomainId(domain.getUuid());
            usageRecResponse.setDomainName(domain.getName());
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
        VMInstanceVO vmInstance = null;
        if (usageRecord.getVmInstanceId() != null) {
            vmInstance = _entityMgr.findByIdIncludingRemoved(VMInstanceVO.class, usageRecord.getVmInstanceId());
            if (vmInstance != null) {
                usageRecResponse.setVirtualMachineId(vmInstance.getUuid());
            }
        }
        usageRecResponse.setResourceName(usageRecord.getVmName());
        VMTemplateVO template = null;
        if (usageRecord.getTemplateId() != null) {
            template = ApiDBUtils.findTemplateById(usageRecord.getTemplateId());
            if (template != null) {
                usageRecResponse.setTemplateId(template.getUuid());
            }
        }

        ResourceTag.ResourceObjectType resourceType = null;
        Long resourceId = null;
        if (usageRecord.getUsageType() == UsageTypes.RUNNING_VM || usageRecord.getUsageType() == UsageTypes.ALLOCATED_VM) {
            ServiceOfferingVO svcOffering = _entityMgr.findByIdIncludingRemoved(ServiceOfferingVO.class, usageRecord.getOfferingId().toString());
            //Service Offering Id
            if(svcOffering != null) {
                usageRecResponse.setOfferingId(svcOffering.getUuid());
            }
            //VM Instance ID
            VMInstanceVO vm = null;
            if (usageRecord.getUsageId() != null && usageRecord.getUsageId().equals(usageRecord.getVmInstanceId())) {
                vm = vmInstance;
            } else {
                vm = _entityMgr.findByIdIncludingRemoved(VMInstanceVO.class, usageRecord.getUsageId().toString());
            }
            if (vm != null) {
                resourceType = ResourceTag.ResourceObjectType.UserVm;
                usageRecResponse.setUsageId(vm.getUuid());
                resourceId = vm.getId();
                final GuestOS guestOS = _guestOsDao.findById(vm.getGuestOSId());
                if (guestOS != null) {
                    usageRecResponse.setOsTypeId(guestOS.getUuid());
                    usageRecResponse.setOsDisplayName(guestOS.getDisplayName());
                    final GuestOsCategory guestOsCategory = _guestOsCategoryDao.findById(guestOS.getCategoryId());
                    if (guestOsCategory != null) {
                        usageRecResponse.setOsCategoryId(guestOsCategory.getUuid());
                        usageRecResponse.setOsCategoryName(guestOsCategory.getName());
                    }
                }
            }
            //Hypervisor Type
            usageRecResponse.setType(usageRecord.getType());
            //Dynamic compute offerings details
            if(usageRecord.getCpuCores() != null) {
                usageRecResponse.setCpuNumber(usageRecord.getCpuCores());
            } else if (svcOffering.getCpu() != null){
                usageRecResponse.setCpuNumber(svcOffering.getCpu().longValue());
            }
            if(usageRecord.getCpuSpeed() != null) {
                usageRecResponse.setCpuSpeed(usageRecord.getCpuSpeed());
            } else if(svcOffering.getSpeed() != null){
                usageRecResponse.setCpuSpeed(svcOffering.getSpeed().longValue());
            }
            if(usageRecord.getMemory() != null) {
                usageRecResponse.setMemory(usageRecord.getMemory());
            } else if(svcOffering.getRamSize() != null) {
                usageRecResponse.setMemory(svcOffering.getRamSize().longValue());
            }
            if (!oldFormat) {
                final StringBuilder builder = new StringBuilder();
                if (usageRecord.getUsageType() == UsageTypes.RUNNING_VM) {
                    builder.append("Running VM usage ");
                } else if(usageRecord.getUsageType() == UsageTypes.ALLOCATED_VM) {
                    builder.append("Allocated VM usage ");
                }
                if (vm != null) {
                    builder.append("for ").append(vm.getHostName()).append(" (").append(vm.getInstanceName()).append(") (").append(vm.getUuid()).append(") ");
                }
                if (svcOffering != null) {
                    builder.append("using service offering ").append(svcOffering.getName()).append(" (").append(svcOffering.getUuid()).append(") ");
                }
                if (template != null) {
                    builder.append("and template ").append(template.getName()).append(" (").append(template.getUuid()).append(")");
                }
                usageRecResponse.setDescription(builder.toString());
            }
        } else if (usageRecord.getUsageType() == UsageTypes.IP_ADDRESS) {
            //IP Address ID
            IPAddressVO ip = _entityMgr.findByIdIncludingRemoved(IPAddressVO.class, usageRecord.getUsageId().toString());
            if (ip != null) {
                Long networkId = ip.getAssociatedWithNetworkId();
                if (networkId == null) {
                    networkId = ip.getSourceNetworkId();
                }
                resourceType = ResourceObjectType.PublicIpAddress;
                resourceId = ip.getId();
                usageRecResponse.setUsageId(ip.getUuid());
            }
            //isSourceNAT
            usageRecResponse.setSourceNat((usageRecord.getType().equals("SourceNat")) ? true : false);
            //isSystem
            usageRecResponse.setSystem((usageRecord.getSize() == 1) ? true : false);
        } else if (usageRecord.getUsageType() == UsageTypes.NETWORK_BYTES_SENT || usageRecord.getUsageType() == UsageTypes.NETWORK_BYTES_RECEIVED) {
            //Device Type
            resourceType = ResourceObjectType.UserVm;
            usageRecResponse.setType(usageRecord.getType());
            VMInstanceVO vm = null;
            HostVO host = null;
            if (usageRecord.getType().equals("DomainRouter") || usageRecord.getType().equals("UserVm")) {
                //Domain Router Id
                vm = _entityMgr.findByIdIncludingRemoved(VMInstanceVO.class, usageRecord.getUsageId().toString());
                if (vm != null) {
                    resourceId = vm.getId();
                    usageRecResponse.setUsageId(vm.getUuid());
                }
            } else {
                //External Device Host Id
                host = _entityMgr.findByIdIncludingRemoved(HostVO.class, usageRecord.getUsageId().toString());
                if (host != null) {
                    usageRecResponse.setUsageId(host.getUuid());
                }
            }
            //Network ID
            NetworkVO network = null;
            if((usageRecord.getNetworkId() != null) && (usageRecord.getNetworkId() != 0)) {
                network = _entityMgr.findByIdIncludingRemoved(NetworkVO.class, usageRecord.getNetworkId().toString());
                if (network != null) {
                    resourceType = ResourceObjectType.Network;
                    if (network.getTrafficType() == TrafficType.Public) {
                        VirtualRouter router = ApiDBUtils.findDomainRouterById(usageRecord.getUsageId());
                        Vpc vpc = ApiDBUtils.findVpcByIdIncludingRemoved(router.getVpcId());
                        usageRecResponse.setVpcId(vpc.getUuid());
                        resourceId = vpc.getId();
                    } else {
                        usageRecResponse.setNetworkId(network.getUuid());
                        resourceId = network.getId();
                    }
                    usageRecResponse.setResourceName(network.getName());
                }
            }
            if (!oldFormat) {
                final StringBuilder builder = new StringBuilder();
                if (usageRecord.getUsageType() == UsageTypes.NETWORK_BYTES_SENT) {
                    builder.append("Bytes sent by network ");
                } else if (usageRecord.getUsageType() == UsageTypes.NETWORK_BYTES_RECEIVED) {
                    builder.append("Bytes received by network ");
                }
                if (network != null) {
                    if (network.getName() != null) {
                        builder.append(network.getName());
                    }
                    if (network.getUuid() != null){
                        builder.append(" (").append(network.getUuid()).append(") ");
                    }
                    builder.append(" " + toHumanReadableSize(usageRecord.getRawUsage().longValue())  + " ");
                }
                if (vm != null) {
                    builder.append("using router ").append(vm.getInstanceName()).append(" (").append(vm.getUuid()).append(")");
                } else if (host != null) {
                    builder.append("using host ").append(host.getName()).append(" (").append(host.getUuid()).append(")");
                }
                usageRecResponse.setDescription(builder.toString());
            }
        } else if (usageRecord.getUsageType() == UsageTypes.VM_DISK_IO_READ || usageRecord.getUsageType() == UsageTypes.VM_DISK_IO_WRITE
                || usageRecord.getUsageType() == UsageTypes.VM_DISK_BYTES_READ || usageRecord.getUsageType() == UsageTypes.VM_DISK_BYTES_WRITE) {
            //Device Type
            usageRecResponse.setType(usageRecord.getType());
            resourceType = ResourceObjectType.Volume;
            //Volume ID
            VolumeVO volume = _entityMgr.findByIdIncludingRemoved(VolumeVO.class, usageRecord.getUsageId().toString());
            if (volume != null) {
                usageRecResponse.setUsageId(volume.getUuid());
                resourceId = volume.getId();
            }
            if (!oldFormat) {
                final StringBuilder builder = new StringBuilder();
                if (usageRecord.getUsageType() == UsageTypes.VM_DISK_IO_READ) {
                    builder.append("Disk I/O read requests");
                } else if (usageRecord.getUsageType() == UsageTypes.VM_DISK_IO_WRITE) {
                    builder.append("Disk I/O write requests");
                } else if (usageRecord.getUsageType() == UsageTypes.VM_DISK_BYTES_READ) {
                    builder.append("Disk I/O read bytes");
                } else if (usageRecord.getUsageType() == UsageTypes.VM_DISK_BYTES_WRITE) {
                    builder.append("Disk I/O write bytes");
                }
                if (vmInstance != null) {
                    builder.append(" for VM ").append(vmInstance.getHostName()).append(" (").append(vmInstance.getUuid()).append(")");
                }
                if (volume != null) {
                    builder.append(" and volume ").append(volume.getName()).append(" (").append(volume.getUuid()).append(")");
                }
                if (usageRecord.getRawUsage()!= null){
                    builder.append(" " + toHumanReadableSize(usageRecord.getRawUsage().longValue()));
                }
                usageRecResponse.setDescription(builder.toString());
            }
        } else if (usageRecord.getUsageType() == UsageTypes.VOLUME) {
            //Volume ID
            VolumeVO volume = _entityMgr.findByIdIncludingRemoved(VolumeVO.class, usageRecord.getUsageId().toString());
            resourceType = ResourceObjectType.Volume;
            if (volume != null) {
                usageRecResponse.setUsageId(volume.getUuid());
                resourceId = volume.getId();
            }
            //Volume Size
            usageRecResponse.setSize(usageRecord.getSize());
            //Disk Offering Id
            DiskOfferingVO diskOff = null;
            if (usageRecord.getOfferingId() != null) {
                diskOff = _entityMgr.findByIdIncludingRemoved(DiskOfferingVO.class, usageRecord.getOfferingId().toString());
                usageRecResponse.setOfferingId(diskOff.getUuid());
            }
            if (!oldFormat) {
                final StringBuilder builder = new StringBuilder();
                builder.append("Volume usage ");
                if (volume != null) {
                    builder.append("for ").append(volume.getName()).append(" (").append(volume.getUuid()).append(")");
                }
                if (diskOff != null) {
                    builder.append(" with disk offering ").append(diskOff.getName()).append(" (").append(diskOff.getUuid()).append(")");
                }
                if (template != null) {
                    builder.append(" and template ").append(template.getName()).append(" (").append(template.getUuid()).append(")");
                }
                if (usageRecord.getSize() != null) {
                    builder.append(" and size " + toHumanReadableSize(usageRecord.getSize()));
                }
                usageRecResponse.setDescription(builder.toString());
            }
        } else if (usageRecord.getUsageType() == UsageTypes.TEMPLATE || usageRecord.getUsageType() == UsageTypes.ISO) {
            //Template/ISO ID
            VMTemplateVO tmpl = _entityMgr.findByIdIncludingRemoved(VMTemplateVO.class, usageRecord.getUsageId().toString());
            if (tmpl != null) {
                usageRecResponse.setUsageId(tmpl.getUuid());
                resourceId = tmpl.getId();
            }
            //Template/ISO Size
            usageRecResponse.setSize(usageRecord.getSize());
            if (usageRecord.getUsageType() == UsageTypes.ISO) {
                usageRecResponse.setVirtualSize(usageRecord.getSize());
                resourceType = ResourceObjectType.ISO;
            } else {
                usageRecResponse.setVirtualSize(usageRecord.getVirtualSize());
                resourceType = ResourceObjectType.Template;
            }
            if (!oldFormat) {
                final StringBuilder builder = new StringBuilder();
                if (usageRecord.getUsageType() == UsageTypes.TEMPLATE) {
                    builder.append("Template usage");
                } else if (usageRecord.getUsageType() == UsageTypes.ISO) {
                    builder.append("ISO usage");
                }
                if (tmpl != null) {
                    builder.append(" for ").append(tmpl.getName()).append(" (").append(tmpl.getUuid()).append(") ")
                            .append("with size ").append(toHumanReadableSize(usageRecord.getSize())).append(" and virtual size ").append(toHumanReadableSize(usageRecord.getVirtualSize()));
                }
                usageRecResponse.setDescription(builder.toString());
            }
        } else if (usageRecord.getUsageType() == UsageTypes.SNAPSHOT) {
            //Snapshot ID
            SnapshotVO snap = _entityMgr.findByIdIncludingRemoved(SnapshotVO.class, usageRecord.getUsageId().toString());
            resourceType = ResourceObjectType.Snapshot;
            if (snap != null) {
                usageRecResponse.setUsageId(snap.getUuid());
                resourceId = snap.getId();
            }
            //Snapshot Size
            usageRecResponse.setSize(usageRecord.getSize());
            if (!oldFormat) {
                final StringBuilder builder = new StringBuilder();
                builder.append("Snapshot usage ");
                if (snap != null) {
                    builder.append("for ").append(snap.getName()).append(" (").append(snap.getUuid()).append(") ")
                            .append("with size ").append(toHumanReadableSize(usageRecord.getSize()));
                }
                usageRecResponse.setDescription(builder.toString());
            }
        } else if (usageRecord.getUsageType() == UsageTypes.LOAD_BALANCER_POLICY) {
            //Load Balancer Policy ID
            LoadBalancerVO lb = _entityMgr.findByIdIncludingRemoved(LoadBalancerVO.class, usageRecord.getUsageId().toString());
            resourceType = ResourceObjectType.LoadBalancer;
            if (lb != null) {
                usageRecResponse.setUsageId(lb.getUuid());
                resourceId = lb.getId();
            }
            if (!oldFormat) {
                final StringBuilder builder = new StringBuilder();
                builder.append("Loadbalancer policy usage ");
                if (lb != null) {
                    builder.append(lb.getName()).append(" (").append(lb.getUuid()).append(")");
                }
                usageRecResponse.setDescription(builder.toString());
            }
        } else if (usageRecord.getUsageType() == UsageTypes.PORT_FORWARDING_RULE) {
            //Port Forwarding Rule ID
            PortForwardingRuleVO pf = _entityMgr.findByIdIncludingRemoved(PortForwardingRuleVO.class, usageRecord.getUsageId().toString());
            resourceType = ResourceObjectType.PortForwardingRule;
            if (pf != null) {
                usageRecResponse.setUsageId(pf.getUuid());
                resourceId = pf.getId();
            }
            if (!oldFormat) {
                final StringBuilder builder = new StringBuilder();
                builder.append("Port forwarding rule usage");
                if (pf != null) {
                    builder.append(" (").append(pf.getUuid()).append(")");
                }
                usageRecResponse.setDescription(builder.toString());
            }
        } else if (usageRecord.getUsageType() == UsageTypes.NETWORK_OFFERING) {
            //Network Offering Id
            NetworkOfferingVO netOff = _entityMgr.findByIdIncludingRemoved(NetworkOfferingVO.class, usageRecord.getOfferingId().toString());
            usageRecResponse.setOfferingId(netOff.getUuid());
            //is Default
            usageRecResponse.setDefault(usageRecord.getUsageId() == 1);
            if (!oldFormat) {
                final StringBuilder builder = new StringBuilder();
                builder.append("Network offering ");
                if (netOff != null) {
                    builder.append(netOff.getName()).append(" (").append(netOff.getUuid()).append(") usage ");
                }
                if (vmInstance != null) {
                    builder.append("for VM ").append(vmInstance.getHostName()).append(" (").append(vmInstance.getUuid()).append(") ");
                }
                usageRecResponse.setDescription(builder.toString());
            }
        } else if (usageRecord.getUsageType() == UsageTypes.VPN_USERS) {
            //VPN User ID
            VpnUserVO vpnUser = _entityMgr.findByIdIncludingRemoved(VpnUserVO.class, usageRecord.getUsageId().toString());
            if (vpnUser != null) {
                usageRecResponse.setUsageId(vpnUser.getUuid());
            }
            if (!oldFormat) {
                final StringBuilder builder = new StringBuilder();
                builder.append("VPN usage ");
                if (vpnUser != null) {
                    builder.append("for user ").append(vpnUser.getUsername()).append(" (").append(vpnUser.getUuid()).append(")");
                }
                usageRecResponse.setDescription(builder.toString());
            }
        } else if (usageRecord.getUsageType() == UsageTypes.SECURITY_GROUP) {
            //Security Group Id
            SecurityGroupVO sg = _entityMgr.findByIdIncludingRemoved(SecurityGroupVO.class, usageRecord.getUsageId().toString());
            resourceType = ResourceObjectType.SecurityGroup;
            if (sg != null) {
                resourceId = sg.getId();
                usageRecResponse.setUsageId(sg.getUuid());
            }
            if (!oldFormat) {
                final StringBuilder builder = new StringBuilder();
                builder.append("Security group");
                if (sg != null) {
                    builder.append(" ").append(sg.getName()).append(" (").append(sg.getUuid()).append(") usage");
                }
                if (vmInstance != null) {
                    builder.append(" for VM ").append(vmInstance.getHostName()).append(" (").append(vmInstance.getUuid()).append(")");
                }
                usageRecResponse.setDescription(builder.toString());
            }
        } else if (usageRecord.getUsageType() == UsageTypes.BACKUP) {
            resourceType = ResourceObjectType.Backup;
            final StringBuilder builder = new StringBuilder();
            builder.append("Backup usage of size ").append(usageRecord.getUsageDisplay());
            if (vmInstance != null) {
                resourceId = vmInstance.getId();
                usageRecResponse.setResourceName(vmInstance.getInstanceName());
                usageRecResponse.setUsageId(vmInstance.getUuid());
                builder.append(" for VM ").append(vmInstance.getHostName())
                        .append(" (").append(vmInstance.getUuid()).append(")");
                final BackupOffering backupOffering = backupOfferingDao.findByIdIncludingRemoved(usageRecord.getOfferingId());
                if (backupOffering != null) {
                    builder.append(" and backup offering ").append(backupOffering.getName())
                            .append(" (").append(backupOffering.getUuid()).append(", user ad-hoc/scheduled backup allowed: ")
                            .append(backupOffering.isUserDrivenBackupAllowed()).append(")");
                }

            }
            usageRecResponse.setDescription(builder.toString());
        } else if (usageRecord.getUsageType() == UsageTypes.VM_SNAPSHOT) {
            resourceType = ResourceObjectType.VMSnapshot;
            VMSnapshotVO vmSnapshotVO = null;
            if (usageRecord.getUsageId() != null) {
                vmSnapshotVO = vmSnapshotDao.findByIdIncludingRemoved(usageRecord.getUsageId());
                if (vmSnapshotVO != null) {
                    resourceId = vmSnapshotVO.getId();
                    usageRecResponse.setResourceName(vmSnapshotVO.getDisplayName());
                    usageRecResponse.setUsageId(vmSnapshotVO.getUuid());
                }
            }
            usageRecResponse.setSize(usageRecord.getSize());
            if (usageRecord.getVirtualSize() != null) {
                usageRecResponse.setVirtualSize(usageRecord.getVirtualSize());
            }
            if (usageRecord.getOfferingId() != null) {
                usageRecResponse.setOfferingId(usageRecord.getOfferingId().toString());
            }
            if (!oldFormat) {
                VolumeVO volume = null;
                if (vmSnapshotVO == null && usageRecord.getUsageId() != null) {
                     volume = _entityMgr.findByIdIncludingRemoved(VolumeVO.class, usageRecord.getUsageId().toString());
                }

                DiskOfferingVO diskOff = null;
                if (usageRecord.getOfferingId() != null) {
                    diskOff = _entityMgr.findByIdIncludingRemoved(DiskOfferingVO.class, usageRecord.getOfferingId());
                }
                final StringBuilder builder = new StringBuilder();
                builder.append("VMSnapshot usage");
                if (vmSnapshotVO != null) {
                    builder.append(" Id: ").append(vmSnapshotVO.getUuid());
                }
                if (vmInstance != null) {
                    builder.append(" for VM ").append(vmInstance.getHostName()).append(" (").append(vmInstance.getUuid()).append(")");
                }
                if (volume != null) {
                    builder.append(" with volume ").append(volume.getName()).append(" (").append(volume.getUuid()).append(")");
                }
                if (diskOff != null) {
                    builder.append(" using disk offering ").append(diskOff.getName()).append(" (").append(diskOff.getUuid()).append(")");
                }
                if (usageRecord.getSize() != null){
                    builder.append(" and size " + toHumanReadableSize(usageRecord.getSize()));
                }
                usageRecResponse.setDescription(builder.toString());
            }
        } else if (usageRecord.getUsageType() == UsageTypes.VOLUME_SECONDARY) {
            VolumeVO volume = _entityMgr.findByIdIncludingRemoved(VolumeVO.class, usageRecord.getUsageId().toString());
            if (!oldFormat) {
                final StringBuilder builder = new StringBuilder();
                builder.append("Volume on secondary storage usage");
                if (volume != null) {
                    builder.append(" for ").append(volume.getName()).append(" (").append(volume.getUuid()).append(") ")
                            .append("with size ").append(toHumanReadableSize(usageRecord.getSize()));
                }
                usageRecResponse.setDescription(builder.toString());
            }
        } else if (usageRecord.getUsageType() == UsageTypes.VM_SNAPSHOT_ON_PRIMARY) {
            resourceType = ResourceObjectType.VMSnapshot;
            VMSnapshotVO vmSnapshotVO = null;
            if (usageRecord.getUsageId() != null) {
                vmSnapshotVO = vmSnapshotDao.findByIdIncludingRemoved(usageRecord.getUsageId());
                if (vmSnapshotVO != null) {
                    resourceId = vmSnapshotVO.getId();
                    usageRecResponse.setResourceName(vmSnapshotVO.getDisplayName());
                    usageRecResponse.setUsageId(vmSnapshotVO.getUuid());
                }
            }
            usageRecResponse.setSize(usageRecord.getVirtualSize());
            if (!oldFormat) {
                final StringBuilder builder = new StringBuilder();
                builder.append("VMSnapshot on primary storage usage");
                if (vmSnapshotVO != null) {
                    builder.append(" Id: ").append(vmSnapshotVO.getUuid());
                }
                if (vmInstance != null) {
                    builder.append(" for VM ").append(vmInstance.getHostName()).append(" (").append(vmInstance.getUuid()).append(") ")
                            .append("with size ").append(toHumanReadableSize(usageRecord.getVirtualSize()));
                }
                usageRecResponse.setDescription(builder.toString());
            }
        }
        if(resourceTagResponseMap != null && resourceTagResponseMap.get(resourceId + ":" + resourceType) != null) {
             usageRecResponse.setTags(resourceTagResponseMap.get(resourceId + ":" + resourceType));
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
        if (inputDate == null) {
            return null;
        }

        TimeZone tz = _usageSvc.getUsageTimezone();
        Calendar cal = Calendar.getInstance(tz);
        cal.setTime(inputDate);

        StringBuilder sb = new StringBuilder(32);
        sb.append(cal.get(Calendar.YEAR)).append('-');

        int month = cal.get(Calendar.MONTH) + 1;
        if (month < 10) {
            sb.append('0');
        }
        sb.append(month).append('-');

        int day = cal.get(Calendar.DAY_OF_MONTH);
        if (day < 10) {
            sb.append('0');
        }
        sb.append(day);

        sb.append("'T'");

        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour < 10) {
            sb.append('0');
        }
        sb.append(hour).append(':');

        int minute = cal.get(Calendar.MINUTE);
        if (minute < 10) {
            sb.append('0');
        }
        sb.append(minute).append(':');

        int seconds = cal.get(Calendar.SECOND);
        if (seconds < 10) {
            sb.append('0');
        }
        sb.append(seconds);

        double offset = cal.get(Calendar.ZONE_OFFSET);
        if (tz.inDaylightTime(inputDate)) {
            offset += (1.0 * tz.getDSTSavings()); // add the timezone's DST
            // value (typically 1 hour
            // expressed in milliseconds)
        }

        offset = offset / (1000d * 60d * 60d);
        int hourOffset = (int)offset;
        double decimalVal = Math.abs(offset) - Math.abs(hourOffset);
        int minuteOffset = (int)(decimalVal * 60);

        if (hourOffset < 0) {
            if (hourOffset > -10) {
                sb.append("-0");
            } else {
                sb.append('-');
            }
            sb.append(Math.abs(hourOffset));
        } else {
            if (hourOffset < 10) {
                sb.append("+0");
            } else {
                sb.append("+");
            }
            sb.append(hourOffset);
        }

        sb.append(':');

        if (minuteOffset == 0) {
            sb.append("00");
        } else if (minuteOffset < 10) {
            sb.append('0').append(minuteOffset);
        } else {
            sb.append(minuteOffset);
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

    @Override
    public NicSecondaryIpResponse createSecondaryIPToNicResponse(NicSecondaryIp result) {
        NicSecondaryIpResponse response = new NicSecondaryIpResponse();
        NicVO nic = _entityMgr.findById(NicVO.class, result.getNicId());
        NetworkVO network = _entityMgr.findById(NetworkVO.class, result.getNetworkId());
        response.setId(result.getUuid());
        setResponseIpAddress(result, response);
        response.setNicId(nic.getUuid());
        response.setNwId(network.getUuid());
        response.setObjectName("nicsecondaryip");
        return response;
    }

    /**
     * Set the NicSecondaryIpResponse object with the IP address that is not null (IPv4 or IPv6)
     */
    public static void setResponseIpAddress(NicSecondaryIp result, NicSecondaryIpResponse response) {
        if (result.getIp4Address() != null) {
            response.setIpAddr(result.getIp4Address());
        } else if (result.getIp6Address() != null) {
            response.setIpAddr(result.getIp6Address());
        }
    }

    /**
     * The resulting Response attempts to be in line with what is returned from
     * @see com.cloud.api.query.dao.UserVmJoinDaoImpl#setUserVmResponse(ResponseView, UserVmResponse, UserVmJoinVO)
     */
    @Override
    public NicResponse createNicResponse(Nic result) {
        NicResponse response = new NicResponse();
        NetworkVO network = _entityMgr.findById(NetworkVO.class, result.getNetworkId());
        VMInstanceVO vm = _entityMgr.findById(VMInstanceVO.class, result.getInstanceId());
        List<NicExtraDhcpOptionVO> nicExtraDhcpOptionVOs = _nicExtraDhcpOptionDao.listByNicId(result.getId());

        // The numbered comments are to keep track of the data returned from here and UserVmJoinDaoImpl.setUserVmResponse()
        // the data can't be identical but some tidying up/unifying might be possible
        /*1: nicUuid*/
        response.setId(result.getUuid());
        /*2: networkUuid*/
        response.setNetworkid(network.getUuid());
        /*3: vmId*/
        if (vm != null) {
            response.setVmId(vm.getUuid());
        }

        if (network.getTrafficType() != null) {
            /*4: trafficType*/
            response.setTrafficType(network.getTrafficType().toString());
        }
        if (network.getGuestType() != null) {
            /*5: guestType*/
            response.setType(network.getGuestType().toString());
        }
        /*6: ipAddress*/
        response.setIpaddress(result.getIPv4Address());
        /*7: gateway*/
        response.setGateway(result.getIPv4Gateway());
        /*8: netmask*/
        response.setNetmask(result.getIPv4Netmask());
        /*9: networkName*/
        response.setNetworkName(network.getName());
        /*10: macAddress*/
        response.setMacAddress(result.getMacAddress());
        /*11: IPv6Address*/
        if (result.getIPv6Address() != null) {
            response.setIp6Address(result.getIPv6Address());
        }
        /*12: IPv6Gateway*/
        if (result.getIPv6Gateway() != null) {
            response.setIp6Gateway(result.getIPv6Gateway());
        }
        /*13: IPv6Cidr*/
        if (result.getIPv6Cidr() != null) {
            response.setIp6Cidr(result.getIPv6Cidr());
        }
        /*14: deviceId*/
        response.setDeviceId(String.valueOf(result.getDeviceId()));
        /*15: broadcastURI*/
        if (result.getBroadcastUri() != null) {
            response.setBroadcastUri(result.getBroadcastUri().toString());
        }
        /*16: isolationURI*/
        if (result.getIsolationUri() != null) {
            response.setIsolationUri(result.getIsolationUri().toString());
        }
        /*17: default*/
        response.setIsDefault(result.isDefaultNic());
        if (result.getSecondaryIp()) {
            List<NicSecondaryIpVO> secondaryIps = ApiDBUtils.findNicSecondaryIps(result.getId());
            if (secondaryIps != null) {
                List<NicSecondaryIpResponse> ipList = new ArrayList<NicSecondaryIpResponse>();
                for (NicSecondaryIpVO ip : secondaryIps) {
                    NicSecondaryIpResponse ipRes = new NicSecondaryIpResponse();
                    ipRes.setId(ip.getUuid());
                    setResponseIpAddress(ip, ipRes);
                    ipList.add(ipRes);
                }
                response.setSecondaryIps(ipList);
            }
        }
        /*18: extra dhcp options */
        List<NicExtraDhcpOptionResponse> nicExtraDhcpOptionResponses = nicExtraDhcpOptionVOs
                .stream()
                .map(vo -> new NicExtraDhcpOptionResponse(Dhcp.DhcpOptionCode.valueOfInt(vo.getCode()).getName(), vo.getCode(), vo.getValue()))
                .collect(Collectors.toList());

        response.setExtraDhcpOptions(nicExtraDhcpOptionResponses);

        if (result instanceof NicVO){
            if (((NicVO)result).getNsxLogicalSwitchUuid() != null){
                response.setNsxLogicalSwitch(((NicVO)result).getNsxLogicalSwitchUuid());
            }
            if (((NicVO)result).getNsxLogicalSwitchPortUuid() != null){
                response.setNsxLogicalSwitchPort(((NicVO)result).getNsxLogicalSwitchPortUuid());
            }
        }

        UserVmJoinVO userVm =  userVmJoinDao.findById(vm.getId());
        if (userVm != null && userVm.getVpcUuid() != null) {
            response.setVpcId(userVm.getVpcUuid());
            VpcVO vpc = _entityMgr.findByUuidIncludingRemoved(VpcVO.class, userVm.getVpcUuid());
            response.setVpcName(vpc.getName());
        }
        return response;
    }

    @Override
    public ApplicationLoadBalancerResponse createLoadBalancerContainerReponse(ApplicationLoadBalancerRule lb, Map<Ip, UserVm> lbInstances) {

        ApplicationLoadBalancerResponse lbResponse = new ApplicationLoadBalancerResponse();
        lbResponse.setId(lb.getUuid());
        lbResponse.setName(lb.getName());
        lbResponse.setDescription(lb.getDescription());
        lbResponse.setAlgorithm(lb.getAlgorithm());
        lbResponse.setForDisplay(lb.isDisplay());
        Network nw = ApiDBUtils.findNetworkById(lb.getNetworkId());
        lbResponse.setNetworkId(nw.getUuid());
        populateOwner(lbResponse, lb);

        if (lb.getScheme() == Scheme.Internal) {
            lbResponse.setSourceIp(lb.getSourceIp().addr());
            //TODO - create the view for the load balancer rule to reflect the network uuid
            Network network = ApiDBUtils.findNetworkById(lb.getNetworkId());
            lbResponse.setSourceIpNetworkId(network.getUuid());
        } else {
            //for public, populate the ip information from the ip address
            IpAddress publicIp = ApiDBUtils.findIpAddressById(lb.getSourceIpAddressId());
            lbResponse.setSourceIp(publicIp.getAddress().addr());
            Network ntwk = ApiDBUtils.findNetworkById(publicIp.getNetworkId());
            lbResponse.setSourceIpNetworkId(ntwk.getUuid());
        }

        //set load balancer rules information (only one rule per load balancer in this release)
        List<ApplicationLoadBalancerRuleResponse> ruleResponses = new ArrayList<ApplicationLoadBalancerRuleResponse>();
        ApplicationLoadBalancerRuleResponse ruleResponse = new ApplicationLoadBalancerRuleResponse();
        ruleResponse.setInstancePort(lb.getDefaultPortStart());
        ruleResponse.setSourcePort(lb.getSourcePortStart());
        FirewallRule.State stateToSet = lb.getState();
        if (stateToSet.equals(FirewallRule.State.Revoke)) {
            stateToSet = FirewallRule.State.Deleting;
        }
        ruleResponse.setState(stateToSet.toString());
        ruleResponse.setObjectName("loadbalancerrule");
        ruleResponses.add(ruleResponse);
        lbResponse.setLbRules(ruleResponses);

        //set Lb instances information
        List<ApplicationLoadBalancerInstanceResponse> instanceResponses = new ArrayList<ApplicationLoadBalancerInstanceResponse>();
        for (Map.Entry<Ip,UserVm> entry : lbInstances.entrySet()) {
            Ip ip = entry.getKey();
            UserVm vm = entry.getValue();
            ApplicationLoadBalancerInstanceResponse instanceResponse = new ApplicationLoadBalancerInstanceResponse();
            instanceResponse.setIpAddress(ip.addr());
            instanceResponse.setId(vm.getUuid());
            instanceResponse.setName(vm.getInstanceName());
            instanceResponse.setObjectName("loadbalancerinstance");
            instanceResponses.add(instanceResponse);
        }

        lbResponse.setLbInstances(instanceResponses);

        //set tag information
        List<? extends ResourceTag> tags = ApiDBUtils.listByResourceTypeAndId(ResourceObjectType.LoadBalancer, lb.getId());
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            CollectionUtils.addIgnoreNull(tagResponses,tagResponse);
        }
        lbResponse.setTags(tagResponses);

        lbResponse.setObjectName("loadbalancer");
        return lbResponse;
    }

    @Override
    public AffinityGroupResponse createAffinityGroupResponse(AffinityGroup group) {

        AffinityGroupResponse response = new AffinityGroupResponse();

        Account account = ApiDBUtils.findAccountById(group.getAccountId());
        response.setId(group.getUuid());
        response.setAccountName(account.getAccountName());
        response.setName(group.getName());
        response.setType(group.getType());
        response.setDescription(group.getDescription());
        Domain domain = ApiDBUtils.findDomainById(account.getDomainId());
        if (domain != null) {
            response.setDomainId(domain.getUuid());
            response.setDomainName(domain.getName());
        }

        response.setObjectName("affinitygroup");
        return response;
    }

    @Override
    public Long getAffinityGroupId(String groupName, long accountId) {
        AffinityGroup ag = ApiDBUtils.getAffinityGroup(groupName, accountId);
        if (ag == null) {
            return null;
        } else {
            return ag.getId();
        }
    }


    @Override
    public PortableIpRangeResponse createPortableIPRangeResponse(PortableIpRange ipRange) {
        PortableIpRangeResponse response = new PortableIpRangeResponse();
        response.setId(ipRange.getUuid());
        String ipRangeStr = ipRange.getIpRange();
        if (ipRangeStr != null) {
            String[] range = ipRangeStr.split("-");
            response.setStartIp(range[0]);
            response.setEndIp(range[1]);
        }
        response.setVlan(ipRange.getVlanTag());
        response.setGateway(ipRange.getGateway());
        response.setNetmask(ipRange.getNetmask());
        response.setRegionId(ipRange.getRegionId());
        response.setObjectName("portableiprange");
        return response;
    }

    @Override
    public PortableIpResponse createPortableIPResponse(PortableIp portableIp) {
        PortableIpResponse response = new PortableIpResponse();
        response.setAddress(portableIp.getAddress());
        Long accountId =  portableIp.getAllocatedInDomainId();
        if (accountId != null) {
            Account account = ApiDBUtils.findAccountById(accountId);
            response.setAllocatedToAccountId(account.getAccountName());
            Domain domain = ApiDBUtils.findDomainById(account.getDomainId());
            response.setAllocatedInDomainId(domain.getUuid());
        }

        response.setAllocatedTime(portableIp.getAllocatedTime());

        if (portableIp.getAssociatedDataCenterId() != null) {
            DataCenter zone = ApiDBUtils.findZoneById(portableIp.getAssociatedDataCenterId());
            if (zone != null) {
                response.setAssociatedDataCenterId(zone.getUuid());
            }
        }

        if (portableIp.getPhysicalNetworkId() != null) {
            PhysicalNetwork pnw = ApiDBUtils.findPhysicalNetworkById(portableIp.getPhysicalNetworkId());
            if (pnw != null) {
                response.setPhysicalNetworkId(pnw.getUuid());
            }
        }

        if (portableIp.getAssociatedWithNetworkId() != null) {
            Network ntwk = ApiDBUtils.findNetworkById(portableIp.getAssociatedWithNetworkId());
            if (ntwk != null) {
                response.setAssociatedWithNetworkId(ntwk.getUuid());
            }
        }

        if (portableIp.getAssociatedWithVpcId() != null) {
            Vpc vpc = ApiDBUtils.findVpcById(portableIp.getAssociatedWithVpcId());
            if (vpc != null) {
                response.setAssociatedWithVpcId(vpc.getUuid());
            }
        }

        response.setState(portableIp.getState().name());
        response.setObjectName("portableip");
        return response;
    }

    @Override
    public InternalLoadBalancerElementResponse createInternalLbElementResponse(VirtualRouterProvider result) {
        if (result.getType() != VirtualRouterProvider.Type.InternalLbVm) {
            return null;
        }
        InternalLoadBalancerElementResponse response = new InternalLoadBalancerElementResponse();
        response.setId(result.getUuid());
        PhysicalNetworkServiceProvider nsp = ApiDBUtils.findPhysicalNetworkServiceProviderById(result.getNspId());
        if (nsp != null) {
            response.setNspId(nsp.getUuid());
        }
        response.setEnabled(result.isEnabled());

        response.setObjectName("internalloadbalancerelement");
        return response;
    }

    @Override
    public IsolationMethodResponse createIsolationMethodResponse(IsolationType method) {
        IsolationMethodResponse response = new IsolationMethodResponse();
        response.setIsolationMethodName(method.toString());
        response.setObjectName("isolationmethod");
        return response;
    }

    @Override
    public NetworkACLResponse createNetworkACLResponse(NetworkACL networkACL) {
        NetworkACLResponse response = new NetworkACLResponse();
        response.setId(networkACL.getUuid());
        response.setName(networkACL.getName());
        response.setDescription(networkACL.getDescription());
        response.setForDisplay(networkACL.isDisplay());
        Vpc vpc = ApiDBUtils.findVpcById(networkACL.getVpcId());
        if (vpc != null) {
            response.setVpcId(vpc.getUuid());
            response.setVpcName(vpc.getName());
        }
        response.setObjectName("networkacllist");
        return response;
    }

    @Override
    public ListResponse<UpgradeRouterTemplateResponse> createUpgradeRouterTemplateResponse(List<Long> jobIds) {
        ListResponse<UpgradeRouterTemplateResponse> response = new ListResponse<UpgradeRouterTemplateResponse>();
        List<UpgradeRouterTemplateResponse> responses = new ArrayList<UpgradeRouterTemplateResponse>();
        for (Long jobId : jobIds) {
            UpgradeRouterTemplateResponse routerResponse = new UpgradeRouterTemplateResponse();
            AsyncJob job = _entityMgr.findById(AsyncJob.class, jobId);
            routerResponse.setAsyncJobId((job.getUuid()));
            routerResponse.setObjectName("asyncjobs");
            responses.add(routerResponse);
        }
        response.setResponses(responses);
        return response;
    }

    @Override
    public SSHKeyPairResponse createSSHKeyPairResponse(SSHKeyPair sshkeyPair, boolean privatekey) {
        SSHKeyPairResponse response = new SSHKeyPairResponse(sshkeyPair.getUuid(), sshkeyPair.getName(), sshkeyPair.getFingerprint());
        if (privatekey) {
            response = new CreateSSHKeyPairResponse(sshkeyPair.getUuid(), sshkeyPair.getName(),
                    sshkeyPair.getFingerprint(), sshkeyPair.getPrivateKey());
        }
        Account account = ApiDBUtils.findAccountById(sshkeyPair.getAccountId());
        if (account.getType() == Account.Type.PROJECT) {
            Project project = ApiDBUtils.findProjectByProjectAccountIdIncludingRemoved(account.getAccountId());
            response.setProjectId(project.getUuid());
            response.setProjectName(project.getName());
        } else {
            response.setAccountName(account.getAccountName());
        }
        Domain domain = ApiDBUtils.findDomainById(sshkeyPair.getDomainId());
        response.setDomainId(domain.getUuid());
        response.setDomainName(domain.getName());
        response.setHasAnnotation(annotationDao.hasAnnotations(sshkeyPair.getUuid(), AnnotationService.EntityType.SSH_KEYPAIR.name(),
                _accountMgr.isRootAdmin(CallContext.current().getCallingAccount().getId())));
        return response;
    }

    @Override
    public UserDataResponse createUserDataResponse(UserData userData) {
        UserDataResponse response = new UserDataResponse(userData.getUuid(), userData.getName(), userData.getUserData(), userData.getParams());
        Account account = ApiDBUtils.findAccountById(userData.getAccountId());
        response.setAccountId(account.getUuid());
        response.setAccountName(account.getAccountName());
        Domain domain = ApiDBUtils.findDomainById(userData.getDomainId());
        response.setDomainId(domain.getUuid());
        response.setDomainName(domain.getName());
        response.setHasAnnotation(annotationDao.hasAnnotations(userData.getUuid(), AnnotationService.EntityType.USER_DATA.name(),
                _accountMgr.isRootAdmin(CallContext.current().getCallingAccount().getId())));
        return response;
    }

    @Override
    public BackupResponse createBackupResponse(Backup backup) {
        return ApiDBUtils.newBackupResponse(backup);
    }

    @Override
    public BackupScheduleResponse createBackupScheduleResponse(BackupSchedule schedule) {
        return ApiDBUtils.newBackupScheduleResponse(schedule);
    }

    @Override
    public BackupOfferingResponse createBackupOfferingResponse(BackupOffering policy) {
        return ApiDBUtils.newBackupOfferingResponse(policy);
    }

    public ManagementServerResponse createManagementResponse(ManagementServerHost mgmt) {
        ManagementServerResponse response = new ManagementServerResponse();
        response.setId(mgmt.getUuid());
        response.setName(mgmt.getName());
        response.setVersion(mgmt.getVersion());
        response.setState(mgmt.getState());
        return response;
    }

    @Override
    public List<RouterHealthCheckResultResponse> createHealthCheckResponse(VirtualMachine router, List<RouterHealthCheckResult> healthCheckResults) {
        List<RouterHealthCheckResultResponse> responses = new ArrayList<>(healthCheckResults.size());
        for (RouterHealthCheckResult hcResult : healthCheckResults) {
            RouterHealthCheckResultResponse healthCheckResponse = new RouterHealthCheckResultResponse();
            healthCheckResponse.setObjectName("routerhealthchecks");
            healthCheckResponse.setCheckName(hcResult.getCheckName());
            healthCheckResponse.setCheckType(hcResult.getCheckType());
            healthCheckResponse.setResult(hcResult.getCheckResult());
            healthCheckResponse.setLastUpdated(hcResult.getLastUpdateTime());
            healthCheckResponse.setDetails(hcResult.getParsedCheckDetails());
            responses.add(healthCheckResponse);
        }
        return responses;
    }

    @Override
    public RollingMaintenanceResponse createRollingMaintenanceResponse(Boolean success, String details, List<RollingMaintenanceManager.HostUpdated> hostsUpdated, List<RollingMaintenanceManager.HostSkipped> hostsSkipped) {
        RollingMaintenanceResponse response = new RollingMaintenanceResponse(success, details);
        List<RollingMaintenanceHostUpdatedResponse> updated = new ArrayList<>();
        for (RollingMaintenanceManager.HostUpdated h : hostsUpdated) {
            RollingMaintenanceHostUpdatedResponse r = new RollingMaintenanceHostUpdatedResponse();
            r.setHostId(h.getHost().getUuid());
            r.setHostName(h.getHost().getName());
            r.setStartDate(getDateStringInternal(h.getStart()));
            r.setEndDate(getDateStringInternal(h.getEnd()));
            r.setOutput(h.getOutputMsg());
            updated.add(r);
        }
        List<RollingMaintenanceHostSkippedResponse> skipped = new ArrayList<>();
        for (RollingMaintenanceManager.HostSkipped h : hostsSkipped) {
            RollingMaintenanceHostSkippedResponse r = new RollingMaintenanceHostSkippedResponse();
            r.setHostId(h.getHost().getUuid());
            r.setHostName(h.getHost().getName());
            r.setReason(h.getReason());
            skipped.add(r);
        }
        response.setUpdatedHosts(updated);
        response.setSkippedHosts(skipped);
        response.setObjectName("rollingmaintenance");
        return response;
    }

    @Override
    public ResourceIconResponse createResourceIconResponse(ResourceIcon resourceIcon) {
        return  ApiDBUtils.newResourceIconResponse(resourceIcon);
    }

    @Override
    public GuestVlanResponse createGuestVlanResponse(GuestVlan guestVlan) {
        GuestVlanResponse guestVlanResponse = new GuestVlanResponse();

        Account owner = null;
        if (guestVlan.getAccountId() != null) {
            owner = ApiDBUtils.findAccountById(guestVlan.getAccountId());
        } else if (guestVlan.getAccountGuestVlanMapId() != null) {
            Long accountId = ApiDBUtils.getAccountIdForGuestVlan(guestVlan.getAccountGuestVlanMapId());
            owner = ApiDBUtils.findAccountById(accountId);
        }
        if (owner != null) {
            populateAccount(guestVlanResponse, owner.getId());
            populateDomain(guestVlanResponse, owner.getDomainId());
        }
        guestVlanResponse.setId(guestVlan.getId());
        guestVlanResponse.setGuestVlan(guestVlan.getVnet());
        DataCenterVO zone = ApiDBUtils.findZoneById(guestVlan.getDataCenterId());
        if (zone != null) {
            guestVlanResponse.setZoneId(zone.getUuid());
            guestVlanResponse.setZoneName(zone.getName());
        }
        PhysicalNetworkVO pnw = ApiDBUtils.findPhysicalNetworkById(guestVlan.getPhysicalNetworkId());
        if (pnw != null) {
            guestVlanResponse.setPhysicalNetworkId(pnw.getUuid());
            guestVlanResponse.setPhysicalNetworkName(pnw.getName());
        }
        if (guestVlan.getAccountGuestVlanMapId() != null) {
            guestVlanResponse.setDedicated(true);
        } else {
            guestVlanResponse.setDedicated(false);
        }
        if (guestVlan.getTakenAt() != null) {
            guestVlanResponse.setAllocationState("Allocated");
            guestVlanResponse.setTaken(guestVlan.getTakenAt());
        } else {
            guestVlanResponse.setAllocationState("Free");
        }

        List<NetworkVO> networks = networkDao.listByZoneAndUriAndGuestType(guestVlan.getDataCenterId(), guestVlan.getVnet(), null);
        List<NetworkResponse> networkResponses = new ArrayList<NetworkResponse>();
        for (Network network : networks) {
            NetworkResponse ntwkRsp = createNetworkResponse(ResponseView.Full, network);
            networkResponses.add(ntwkRsp);
        }
        guestVlanResponse.setNetworks(networkResponses);

        return guestVlanResponse;
    }

    @Override
    public NetworkPermissionsResponse createNetworkPermissionsResponse(NetworkPermission permission) {
        Long networkOwnerDomain = null;
        Network network = ApiDBUtils.findNetworkById(permission.getNetworkId());

        NetworkPermissionsResponse response = new NetworkPermissionsResponse();
        response.setNetworkId(network.getUuid());

        Account networkOwner = ApiDBUtils.findAccountById(network.getAccountId());
        if (networkOwner != null) {
            networkOwnerDomain = networkOwner.getDomainId();
            if (networkOwnerDomain != null) {
                Domain domain = ApiDBUtils.findDomainById(networkOwnerDomain);
                if (domain != null) {
                    response.setDomainId(domain.getUuid());
                    response.setDomainName(domain.getName());
                }
            }
        }

        Account account = ApiDBUtils.findAccountById(permission.getAccountId());
        response.setAccountName(account.getName());
        response.setAccountId(account.getUuid());
        if (account.getType() == Account.Type.PROJECT) {
            // convert account to projectIds
            Project project = ApiDBUtils.findProjectByProjectAccountId(account.getId());

            if (project.getUuid() != null && !project.getUuid().isEmpty()) {
                response.setProjectId(project.getUuid());
            } else {
                response.setProjectId(String.valueOf(project.getId()));
            }
            response.setProjectName(project.getName());
        }

        response.setObjectName("networkpermission");
        return response;
    }

    protected void handleCertificateResponse(String certStr, DirectDownloadCertificateResponse response) {
        try {
            Certificate cert = CertificateHelper.buildCertificate(certStr);
            if (cert instanceof X509CertImpl) {
                X509CertImpl certificate = (X509CertImpl) cert;
                response.setVersion(String.valueOf(certificate.getVersion()));
                response.setSubject(certificate.getSubjectDN().toString());
                response.setIssuer(certificate.getIssuerDN().toString());
                response.setSerialNum(certificate.getSerialNumberObject().toString());
                response.setValidity(String.format("From: [%s] - To: [%s]", certificate.getNotBefore(), certificate.getNotAfter()));
            }
        } catch (CertificateException e) {
            s_logger.error("Error parsing direct download certificate: " + certStr, e);
        }
    }

    @Override
    public DirectDownloadCertificateResponse createDirectDownloadCertificateResponse(DirectDownloadCertificate certificate) {
        DirectDownloadCertificateResponse response = new DirectDownloadCertificateResponse();
        DataCenterVO datacenter = ApiDBUtils.findZoneById(certificate.getZoneId());
        if (datacenter != null) {
            response.setZoneId(datacenter.getUuid());
            response.setZoneName(datacenter.getName());
        }
        response.setId(certificate.getUuid());
        response.setAlias(certificate.getAlias());
        handleCertificateResponse(certificate.getCertificate(), response);
        response.setHypervisor(certificate.getHypervisorType().name());
        response.setObjectName("directdownloadcertificate");
        return response;
    }

    @Override
    public List<DirectDownloadCertificateHostStatusResponse> createDirectDownloadCertificateHostMapResponse(List<DirectDownloadCertificateHostMap> hostMappings) {
        if (CollectionUtils.isEmpty(hostMappings)) {
            return new ArrayList<>();
        }
        List<DirectDownloadCertificateHostStatusResponse> responses = new ArrayList<>(hostMappings.size());
        for (DirectDownloadCertificateHostMap map : hostMappings) {
            DirectDownloadCertificateHostStatusResponse response = new DirectDownloadCertificateHostStatusResponse();
            HostVO host = ApiDBUtils.findHostById(map.getHostId());
            if (host != null) {
                response.setHostId(host.getUuid());
                response.setHostName(host.getName());
            }
            response.setStatus(map.isRevoked() ? CertificateStatus.REVOKED.name() : CertificateStatus.UPLOADED.name());
            response.setObjectName("directdownloadcertificatehoststatus");
            responses.add(response);
        }
        return responses;
    }

    private DirectDownloadCertificateHostStatusResponse getDirectDownloadHostStatusResponseInternal(Host host, CertificateStatus status, String details) {
        DirectDownloadCertificateHostStatusResponse response = new DirectDownloadCertificateHostStatusResponse();
        if (host != null) {
            response.setHostId(host.getUuid());
            response.setHostName(host.getName());
        }
        response.setStatus(status.name());
        response.setDetails(details);
        response.setObjectName("directdownloadcertificatehoststatus");
        return response;
    }

    @Override
    public DirectDownloadCertificateHostStatusResponse createDirectDownloadCertificateHostStatusResponse(DirectDownloadManager.HostCertificateStatus hostStatus) {
        Host host = hostStatus.getHost();
        CertificateStatus status = hostStatus.getStatus();
        return getDirectDownloadHostStatusResponseInternal(host, status, hostStatus.getDetails());
    }

    @Override
    public DirectDownloadCertificateHostStatusResponse createDirectDownloadCertificateProvisionResponse(Long certificateId, Long hostId, Pair<Boolean, String> result) {
        HostVO host = ApiDBUtils.findHostById(hostId);
        CertificateStatus status = result != null && result.first() ? CertificateStatus.UPLOADED : CertificateStatus.FAILED;
        return getDirectDownloadHostStatusResponseInternal(host, status, result != null ? result.second() : "provision certificate failure");
    }

    @Override
    public FirewallResponse createIpv6FirewallRuleResponse(FirewallRule fwRule) {
        FirewallResponse response = new FirewallResponse();

        response.setId(fwRule.getUuid());
        response.setProtocol(fwRule.getProtocol());
        List<String> cidrs = ApiDBUtils.findFirewallSourceCidrs(fwRule.getId());
        response.setCidrList(StringUtils.join(cidrs, ","));
        List<String> destinationCidrs = ApiDBUtils.findFirewallDestCidrs(fwRule.getId());
        response.setDestCidr(StringUtils.join(destinationCidrs, ","));
        response.setTrafficType(fwRule.getTrafficType().toString());
        response.setProtocol(fwRule.getProtocol());
        response.setStartPort(fwRule.getSourcePortStart());
        response.setEndPort(fwRule.getSourcePortEnd());
        response.setIcmpCode(fwRule.getIcmpCode());
        response.setIcmpType(fwRule.getIcmpType());

        Network network = ApiDBUtils.findNetworkById(fwRule.getNetworkId());
        response.setNetworkId(network.getUuid());

        FirewallRule.State state = fwRule.getState();
        String stateToSet = state.toString();
        if (state.equals(FirewallRule.State.Revoke)) {
            stateToSet = "Deleting";
        }

        response.setForDisplay(fwRule.isDisplay());

        // set tag information
        List<? extends ResourceTag> tags = ApiDBUtils.listByResourceTypeAndId(ResourceObjectType.FirewallRule, fwRule.getId());
        List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
        for (ResourceTag tag : tags) {
            ResourceTagResponse tagResponse = createResourceTagResponse(tag, true);
            CollectionUtils.addIgnoreNull(tagResponses, tagResponse);
        }
        response.setTags(tagResponses);

        response.setState(stateToSet);
        response.setObjectName("firewallrule");
        return response;
    }
}
