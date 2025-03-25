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
package com.cloud.api.query;

import static com.cloud.vm.VmDetailConstants.SSH_PUBLIC_KEY;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.affinity.AffinityGroupDomainMapVO;
import org.apache.cloudstack.affinity.AffinityGroupResponse;
import org.apache.cloudstack.affinity.AffinityGroupVMMapVO;
import org.apache.cloudstack.affinity.dao.AffinityGroupDomainMapDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.api.ResourceDetail;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.admin.account.ListAccountsCmdByAdmin;
import org.apache.cloudstack.api.command.admin.domain.ListDomainsCmd;
import org.apache.cloudstack.api.command.admin.domain.ListDomainsCmdByAdmin;
import org.apache.cloudstack.api.command.admin.host.ListHostTagsCmd;
import org.apache.cloudstack.api.command.admin.host.ListHostsCmd;
import org.apache.cloudstack.api.command.admin.internallb.ListInternalLBVMsCmd;
import org.apache.cloudstack.api.command.admin.iso.ListIsosCmdByAdmin;
import org.apache.cloudstack.api.command.admin.management.ListMgmtsCmd;
import org.apache.cloudstack.api.command.admin.resource.icon.ListResourceIconCmd;
import org.apache.cloudstack.api.command.admin.router.GetRouterHealthCheckResultsCmd;
import org.apache.cloudstack.api.command.admin.router.ListRoutersCmd;
import org.apache.cloudstack.api.command.admin.snapshot.ListSnapshotsCmdByAdmin;
import org.apache.cloudstack.api.command.admin.storage.ListImageStoresCmd;
import org.apache.cloudstack.api.command.admin.storage.ListObjectStoragePoolsCmd;
import org.apache.cloudstack.api.command.admin.storage.ListSecondaryStagingStoresCmd;
import org.apache.cloudstack.api.command.admin.storage.ListStoragePoolsCmd;
import org.apache.cloudstack.api.command.admin.storage.ListStorageTagsCmd;
import org.apache.cloudstack.api.command.admin.storage.heuristics.ListSecondaryStorageSelectorsCmd;
import org.apache.cloudstack.api.command.admin.template.ListTemplatesCmdByAdmin;
import org.apache.cloudstack.api.command.admin.user.ListUsersCmd;
import org.apache.cloudstack.api.command.admin.vm.ListAffectedVmsForStorageScopeChangeCmd;
import org.apache.cloudstack.api.command.admin.zone.ListZonesCmdByAdmin;
import org.apache.cloudstack.api.command.user.account.ListAccountsCmd;
import org.apache.cloudstack.api.command.user.account.ListProjectAccountsCmd;
import org.apache.cloudstack.api.command.user.address.ListQuarantinedIpsCmd;
import org.apache.cloudstack.api.command.user.affinitygroup.ListAffinityGroupsCmd;
import org.apache.cloudstack.api.command.user.bucket.ListBucketsCmd;
import org.apache.cloudstack.api.command.user.event.ListEventsCmd;
import org.apache.cloudstack.api.command.user.iso.ListIsosCmd;
import org.apache.cloudstack.api.command.user.job.ListAsyncJobsCmd;
import org.apache.cloudstack.api.command.user.offering.ListDiskOfferingsCmd;
import org.apache.cloudstack.api.command.user.offering.ListServiceOfferingsCmd;
import org.apache.cloudstack.api.command.user.project.ListProjectInvitationsCmd;
import org.apache.cloudstack.api.command.user.project.ListProjectsCmd;
import org.apache.cloudstack.api.command.user.resource.ListDetailOptionsCmd;
import org.apache.cloudstack.api.command.user.securitygroup.ListSecurityGroupsCmd;
import org.apache.cloudstack.api.command.user.snapshot.CopySnapshotCmd;
import org.apache.cloudstack.api.command.user.snapshot.ListSnapshotsCmd;
import org.apache.cloudstack.api.command.user.tag.ListTagsCmd;
import org.apache.cloudstack.api.command.user.template.ListTemplatesCmd;
import org.apache.cloudstack.api.command.user.template.ListVnfTemplatesCmd;
import org.apache.cloudstack.api.command.user.vm.ListVMsCmd;
import org.apache.cloudstack.api.command.user.vmgroup.ListVMGroupsCmd;
import org.apache.cloudstack.api.command.user.volume.ListResourceDetailsCmd;
import org.apache.cloudstack.api.command.user.volume.ListVolumesCmd;
import org.apache.cloudstack.api.command.user.zone.ListZonesCmd;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.AsyncJobResponse;
import org.apache.cloudstack.api.response.BucketResponse;
import org.apache.cloudstack.api.response.DetailOptionsResponse;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.api.response.EventResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.HostTagResponse;
import org.apache.cloudstack.api.response.ImageStoreResponse;
import org.apache.cloudstack.api.response.InstanceGroupResponse;
import org.apache.cloudstack.api.response.IpQuarantineResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ManagementServerResponse;
import org.apache.cloudstack.api.response.ObjectStoreResponse;
import org.apache.cloudstack.api.response.PeerManagementServerNodeResponse;
import org.apache.cloudstack.api.response.ProjectAccountResponse;
import org.apache.cloudstack.api.response.ProjectInvitationResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ResourceDetailResponse;
import org.apache.cloudstack.api.response.ResourceIconResponse;
import org.apache.cloudstack.api.response.ResourceTagResponse;
import org.apache.cloudstack.api.response.RouterHealthCheckResultResponse;
import org.apache.cloudstack.api.response.SecondaryStorageHeuristicsResponse;
import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.SnapshotResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.StorageTagResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VirtualMachineResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.backup.BackupOfferingVO;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateState;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagementVO;
import org.apache.cloudstack.outofbandmanagement.dao.OutOfBandManagementDao;
import org.apache.cloudstack.query.QueryService;
import org.apache.cloudstack.resourcedetail.DiskOfferingDetailVO;
import org.apache.cloudstack.resourcedetail.dao.DiskOfferingDetailsDao;
import org.apache.cloudstack.secstorage.HeuristicVO;
import org.apache.cloudstack.secstorage.dao.SecondaryStorageHeuristicDao;
import org.apache.cloudstack.secstorage.heuristics.Heuristic;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloud.api.query.dao.AccountJoinDao;
import com.cloud.api.query.dao.AffinityGroupJoinDao;
import com.cloud.api.query.dao.AsyncJobJoinDao;
import com.cloud.api.query.dao.DataCenterJoinDao;
import com.cloud.api.query.dao.DiskOfferingJoinDao;
import com.cloud.api.query.dao.DomainJoinDao;
import com.cloud.api.query.dao.DomainRouterJoinDao;
import com.cloud.api.query.dao.HostJoinDao;
import com.cloud.api.query.dao.ImageStoreJoinDao;
import com.cloud.api.query.dao.InstanceGroupJoinDao;
import com.cloud.api.query.dao.ManagementServerJoinDao;
import com.cloud.api.query.dao.ProjectAccountJoinDao;
import com.cloud.api.query.dao.ProjectInvitationJoinDao;
import com.cloud.api.query.dao.ProjectJoinDao;
import com.cloud.api.query.dao.ResourceTagJoinDao;
import com.cloud.api.query.dao.SecurityGroupJoinDao;
import com.cloud.api.query.dao.ServiceOfferingJoinDao;
import com.cloud.api.query.dao.SnapshotJoinDao;
import com.cloud.api.query.dao.StoragePoolJoinDao;
import com.cloud.api.query.dao.TemplateJoinDao;
import com.cloud.api.query.dao.UserAccountJoinDao;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.dao.VolumeJoinDao;
import com.cloud.api.query.vo.AccountJoinVO;
import com.cloud.api.query.vo.AffinityGroupJoinVO;
import com.cloud.api.query.vo.AsyncJobJoinVO;
import com.cloud.api.query.vo.DataCenterJoinVO;
import com.cloud.api.query.vo.DiskOfferingJoinVO;
import com.cloud.api.query.vo.DomainJoinVO;
import com.cloud.api.query.vo.DomainRouterJoinVO;
import com.cloud.api.query.vo.EventJoinVO;
import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.api.query.vo.ImageStoreJoinVO;
import com.cloud.api.query.vo.InstanceGroupJoinVO;
import com.cloud.api.query.vo.ManagementServerJoinVO;
import com.cloud.api.query.vo.ProjectAccountJoinVO;
import com.cloud.api.query.vo.ProjectInvitationJoinVO;
import com.cloud.api.query.vo.ProjectJoinVO;
import com.cloud.api.query.vo.ResourceTagJoinVO;
import com.cloud.api.query.vo.SecurityGroupJoinVO;
import com.cloud.api.query.vo.ServiceOfferingJoinVO;
import com.cloud.api.query.vo.SnapshotJoinVO;
import com.cloud.api.query.vo.StoragePoolJoinVO;
import com.cloud.api.query.vo.TemplateJoinVO;
import com.cloud.api.query.vo.UserAccountJoinVO;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.api.query.vo.VolumeJoinVO;
import com.cloud.cluster.ManagementServerHostPeerJoinVO;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.cluster.dao.ManagementServerHostPeerJoinDao;
import com.cloud.cpu.CPU;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DedicatedResourceVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.event.dao.EventJoinDao;
import com.cloud.exception.CloudAuthenticationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.Host;
import com.cloud.host.HostTagVO;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostTagsDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.PublicIpQuarantine;
import com.cloud.network.RouterHealthCheckResult;
import com.cloud.network.VNF;
import com.cloud.network.VpcVirtualNetworkApplianceService;
import com.cloud.network.as.AutoScaleVmGroupVmMapVO;
import com.cloud.network.as.dao.AutoScaleVmGroupDao;
import com.cloud.network.as.dao.AutoScaleVmGroupVmMapDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PublicIpQuarantineDao;
import com.cloud.network.dao.RouterHealthCheckResultDao;
import com.cloud.network.dao.RouterHealthCheckResultVO;
import com.cloud.network.router.VirtualNetworkApplianceManager;
import com.cloud.network.security.SecurityGroupVMMapVO;
import com.cloud.network.security.dao.SecurityGroupVMMapDao;
import com.cloud.network.vo.PublicIpQuarantineVO;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Grouping;
import com.cloud.projects.Project;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.projects.ProjectInvitation;
import com.cloud.projects.ProjectManager;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectAccountDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.projects.dao.ProjectInvitationDao;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.icon.dao.ResourceIconDao;
import com.cloud.server.ResourceManagerUtil;
import com.cloud.server.ResourceMetaDataService;
import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.service.ServiceOfferingDetailsVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.BucketVO;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.StoragePoolTagVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiServiceImpl;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.BucketDao;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.StoragePoolTagsDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.template.VirtualMachineTemplate.State;
import com.cloud.template.VirtualMachineTemplate.TemplateFilter;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.DomainManager;
import com.cloud.user.SSHKeyPairVO;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.InstanceGroupVMMapVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.InstanceGroupVMMapDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;

@Component
public class QueryManagerImpl extends MutualExclusiveIdsManagerBase implements QueryService, Configurable {


    private static final String ID_FIELD = "id";

    @Inject
    AccountManager accountMgr;

    @Inject
    ProjectManager _projectMgr;

    @Inject
    DomainDao _domainDao;

    @Inject
    DomainJoinDao _domainJoinDao;

    @Inject
    UserAccountJoinDao _userAccountJoinDao;

    @Inject
    EventDao eventDao;

    @Inject
    EventJoinDao _eventJoinDao;

    @Inject
    ResourceTagJoinDao _resourceTagJoinDao;

    @Inject
    InstanceGroupJoinDao _vmGroupJoinDao;

    @Inject
    UserVmJoinDao _userVmJoinDao;

    @Inject
    UserVmDao userVmDao;

    @Inject
    VMInstanceDao _vmInstanceDao;

    @Inject
    SecurityGroupJoinDao _securityGroupJoinDao;

    @Inject
    SecurityGroupVMMapDao securityGroupVMMapDao;

    @Inject
    DomainRouterJoinDao _routerJoinDao;

    @Inject
    ProjectInvitationJoinDao _projectInvitationJoinDao;

    @Inject
    ProjectJoinDao _projectJoinDao;

    @Inject
    ProjectDao _projectDao;

    @Inject
    ProjectAccountDao _projectAccountDao;

    @Inject
    ProjectAccountJoinDao _projectAccountJoinDao;

    @Inject
    HostJoinDao hostJoinDao;

    @Inject
    VolumeJoinDao _volumeJoinDao;

    @Inject
    AccountDao _accountDao;

    @Inject
    AccountJoinDao _accountJoinDao;

    @Inject
    AsyncJobJoinDao _jobJoinDao;

    @Inject
    StoragePoolJoinDao _poolJoinDao;

    @Inject
    StoragePoolTagsDao _storageTagDao;

    @Inject
    HostTagsDao _hostTagDao;

    @Inject
    ImageStoreJoinDao _imageStoreJoinDao;

    @Inject
    DiskOfferingJoinDao _diskOfferingJoinDao;

    @Inject
    DiskOfferingDetailsDao _diskOfferingDetailsDao;

    @Inject
    ServiceOfferingJoinDao _srvOfferingJoinDao;

    @Inject
    ServiceOfferingDao _srvOfferingDao;

    @Inject
    ServiceOfferingDetailsDao _srvOfferingDetailsDao;

    @Inject
    DiskOfferingDao _diskOfferingDao;

    @Inject
    DataCenterJoinDao _dcJoinDao;

    @Inject
    DomainRouterDao _routerDao;

    @Inject
    HighAvailabilityManager _haMgr;

    @Inject
    VMTemplateDao _templateDao;

    @Inject
    TemplateJoinDao _templateJoinDao;

    @Inject
    ResourceManager _resourceMgr;
    @Inject
    ResourceMetaDataService _resourceMetaDataMgr;

    @Inject
    ResourceManagerUtil resourceManagerUtil;

    @Inject
    AffinityGroupVMMapDao _affinityGroupVMMapDao;

    @Inject
    AffinityGroupJoinDao _affinityGroupJoinDao;

    @Inject
    DedicatedResourceDao _dedicatedDao;

    @Inject
    DomainManager _domainMgr;

    @Inject
    AffinityGroupDomainMapDao _affinityGroupDomainMapDao;

    @Inject
    ResourceTagDao resourceTagDao;

    @Inject
    DataStoreManager dataStoreManager;

    @Inject
    ManagementServerJoinDao managementServerJoinDao;

    @Inject
    VpcVirtualNetworkApplianceService routerService;

    @Inject
    ResponseGenerator responseGenerator;

    @Inject
    RouterHealthCheckResultDao routerHealthCheckResultDao;

    @Inject
    PrimaryDataStoreDao storagePoolDao;

    @Inject
    StoragePoolDetailsDao _storagePoolDetailsDao;

    @Inject
    ProjectInvitationDao projectInvitationDao;

    @Inject
    TemplateDataStoreDao templateDataStoreDao;

    @Inject
    VMTemplatePoolDao templatePoolDao;

    @Inject
    SnapshotDataStoreDao snapshotDataStoreDao;

    @Inject
    UserDao userDao;

    @Inject
    VirtualMachineManager virtualMachineManager;

    @Inject
    VolumeDao volumeDao;

    @Inject
    ResourceIconDao resourceIconDao;
    @Inject
    StorageManager storageManager;

    @Inject
    ManagementServerHostDao msHostDao;

    @Inject
    SecondaryStorageHeuristicDao secondaryStorageHeuristicDao;

    @Inject
    NetworkDao networkDao;

    @Inject
    IPAddressDao ipAddressDao;

    @Inject
    NicDao nicDao;

    @Inject
    HostDao hostDao;

    @Inject
    OutOfBandManagementDao outOfBandManagementDao;

    @Inject
    InstanceGroupVMMapDao instanceGroupVMMapDao;

    @Inject
    AffinityGroupVMMapDao affinityGroupVMMapDao;

    @Inject
    UserVmDetailsDao userVmDetailsDao;

    @Inject
    SSHKeyPairDao sshKeyPairDao;

    @Inject
    BackupOfferingDao backupOfferingDao;

    @Inject
    AutoScaleVmGroupDao autoScaleVmGroupDao;

    @Inject
    AutoScaleVmGroupVmMapDao autoScaleVmGroupVmMapDao;

    @Inject
    SnapshotJoinDao snapshotJoinDao;

    @Inject
    ObjectStoreDao objectStoreDao;

    @Inject
    BucketDao bucketDao;

    @Inject
    EntityManager entityManager;

    @Inject
    PublicIpQuarantineDao publicIpQuarantineDao;

    @Inject
    StoragePoolHostDao storagePoolHostDao;

    @Inject
    ClusterDao clusterDao;

    @Inject
    ManagementServerHostPeerJoinDao mshostPeerJoinDao;


    private SearchCriteria<ServiceOfferingJoinVO> getMinimumCpuServiceOfferingJoinSearchCriteria(int cpu) {
        SearchCriteria<ServiceOfferingJoinVO> sc = _srvOfferingJoinDao.createSearchCriteria();
        SearchCriteria<ServiceOfferingJoinVO> sc1 = _srvOfferingJoinDao.createSearchCriteria();
        sc1.addAnd("cpu", Op.GTEQ, cpu);
        sc.addOr("cpu", Op.SC, sc1);
        SearchCriteria<ServiceOfferingJoinVO> sc2 = _srvOfferingJoinDao.createSearchCriteria();
        sc2.addAnd("cpu", Op.NULL);
        sc2.addAnd("maxCpu", Op.NULL);
        sc.addOr("cpu", Op.SC, sc2);
        SearchCriteria<ServiceOfferingJoinVO> sc3 = _srvOfferingJoinDao.createSearchCriteria();
        sc3.addAnd("cpu", Op.NULL);
        sc3.addAnd("maxCpu", Op.GTEQ, cpu);
        sc.addOr("cpu", Op.SC, sc3);
        return sc;
    }

    private SearchCriteria<ServiceOfferingJoinVO> getMinimumMemoryServiceOfferingJoinSearchCriteria(int memory) {
        SearchCriteria<ServiceOfferingJoinVO> sc = _srvOfferingJoinDao.createSearchCriteria();
        SearchCriteria<ServiceOfferingJoinVO> sc1 = _srvOfferingJoinDao.createSearchCriteria();
        sc1.addAnd("ramSize", Op.GTEQ, memory);
        sc.addOr("ramSize", Op.SC, sc1);
        SearchCriteria<ServiceOfferingJoinVO> sc2 = _srvOfferingJoinDao.createSearchCriteria();
        sc2.addAnd("ramSize", Op.NULL);
        sc2.addAnd("maxMemory", Op.NULL);
        sc.addOr("ramSize", Op.SC, sc2);
        SearchCriteria<ServiceOfferingJoinVO> sc3 = _srvOfferingJoinDao.createSearchCriteria();
        sc3.addAnd("ramSize", Op.NULL);
        sc3.addAnd("maxMemory", Op.GTEQ, memory);
        sc.addOr("ramSize", Op.SC, sc3);
        return sc;
    }

    private SearchCriteria<ServiceOfferingJoinVO> getMinimumCpuSpeedServiceOfferingJoinSearchCriteria(int speed) {
        SearchCriteria<ServiceOfferingJoinVO> sc = _srvOfferingJoinDao.createSearchCriteria();
        sc.addOr("speed", Op.GTEQ, speed);
        sc.addOr("speed", Op.NULL);
        return sc;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.cloud.api.query.QueryService#searchForUsers(org.apache.cloudstack
     * .api.command.admin.user.ListUsersCmd)
     */
    @Override
    public ListResponse<UserResponse> searchForUsers(ResponseView responseView, ListUsersCmd cmd) throws PermissionDeniedException {
        Pair<List<UserAccountJoinVO>, Integer> result = searchForUsersInternal(cmd);
        ListResponse<UserResponse> response = new ListResponse<>();
        if (CallContext.current().getCallingAccount().getType() == Account.Type.ADMIN) {
            responseView = ResponseView.Full;
        }
        List<UserResponse> userResponses = ViewResponseHelper.createUserResponse(responseView, CallContext.current().getCallingAccount().getDomainId(),
                result.first().toArray(new UserAccountJoinVO[result.first().size()]));
        response.setResponses(userResponses, result.second());
        return response;
    }

    public ListResponse<UserResponse> searchForUsers(Long domainId, boolean recursive) throws PermissionDeniedException {
        Account caller = CallContext.current().getCallingAccount();

        List<Long> permittedAccounts = new ArrayList<Long>();

        boolean listAll = true;
        Long id = null;

        if (caller.getType() == Account.Type.NORMAL) {
            long currentId = CallContext.current().getCallingUser().getId();
            if (id != null && currentId != id.longValue()) {
                throw new PermissionDeniedException("Calling user is not authorized to see the user requested by id");
            }
            id = currentId;
        }
        Object username = null;
        Object type = null;
        String accountName = null;
        Object state = null;
        String keyword = null;

        Pair<List<UserAccountJoinVO>, Integer> result =  getUserListInternal(caller, permittedAccounts, listAll, id,
                username, type, accountName, state, keyword, null, domainId, recursive, null);
        ListResponse<UserResponse> response = new ListResponse<UserResponse>();
        List<UserResponse> userResponses = ViewResponseHelper.createUserResponse(ResponseView.Restricted, CallContext.current().getCallingAccount().getDomainId(),
                result.first().toArray(new UserAccountJoinVO[result.first().size()]));
        response.setResponses(userResponses, result.second());
        return response;
    }

    private Pair<List<UserAccountJoinVO>, Integer> searchForUsersInternal(ListUsersCmd cmd) throws PermissionDeniedException {
        Account caller = CallContext.current().getCallingAccount();

        List<Long> permittedAccounts = new ArrayList<Long>();

        boolean listAll = cmd.listAll();
        Long id = cmd.getId();
        if (caller.getType() == Account.Type.NORMAL) {
            long currentId = CallContext.current().getCallingUser().getId();
            if (id != null && currentId != id.longValue()) {
                throw new PermissionDeniedException("Calling user is not authorized to see the user requested by id");
            }
            id = currentId;
        }
        Object username = cmd.getUsername();
        Object type = cmd.getAccountType();
        String accountName = cmd.getAccountName();
        Object state = cmd.getState();
        String keyword = cmd.getKeyword();
        String apiKeyAccess = cmd.getApiKeyAccess();

        Long domainId = cmd.getDomainId();
        boolean recursive = cmd.isRecursive();
        Long pageSizeVal = cmd.getPageSizeVal();
        Long startIndex = cmd.getStartIndex();

        Filter searchFilter = new Filter(UserAccountJoinVO.class, "id", true, startIndex, pageSizeVal);

        return getUserListInternal(caller, permittedAccounts, listAll, id, username, type, accountName, state, keyword, apiKeyAccess, domainId, recursive, searchFilter);
    }

    private Pair<List<UserAccountJoinVO>, Integer> getUserListInternal(Account caller, List<Long> permittedAccounts, boolean listAll, Long id, Object username, Object type,
            String accountName, Object state, String keyword, String apiKeyAccess, Long domainId, boolean recursive, Filter searchFilter) {
        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(domainId, recursive, null);
        accountMgr.buildACLSearchParameters(caller, id, accountName, null, permittedAccounts, domainIdRecursiveListProject, listAll, false);
        domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        SearchBuilder<UserAccountJoinVO> sb = _userAccountJoinDao.createSearchBuilder();
        accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        sb.and("username", sb.entity().getUsername(), Op.LIKE);
        if (id != null && id == 1) {
            // system user should NOT be searchable
            List<UserAccountJoinVO> emptyList = new ArrayList<UserAccountJoinVO>();
            return new Pair<List<UserAccountJoinVO>, Integer>(emptyList, 0);
        } else if (id != null) {
            sb.and("id", sb.entity().getId(), Op.EQ);
        } else {
            // this condition is used to exclude system user from the search
            // results
            sb.and("id", sb.entity().getId(), Op.NEQ);
        }

        sb.and("type", sb.entity().getAccountType(), Op.EQ);
        sb.and("domainId", sb.entity().getDomainId(), Op.EQ);
        sb.and("accountName", sb.entity().getAccountName(), Op.EQ);
        sb.and("state", sb.entity().getState(), Op.EQ);
        if (apiKeyAccess != null) {
            sb.and("apiKeyAccess", sb.entity().getApiKeyAccess(), Op.EQ);
        }

        if ((accountName == null) && (domainId != null)) {
            sb.and("domainPath", sb.entity().getDomainPath(), Op.LIKE);
        }

        SearchCriteria<UserAccountJoinVO> sc = sb.create();

        // building ACL condition
        accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (keyword != null) {
            SearchCriteria<UserAccountJoinVO> ssc = _userAccountJoinDao.createSearchCriteria();
            ssc.addOr("username", Op.LIKE, "%" + keyword + "%");
            ssc.addOr("firstname", Op.LIKE, "%" + keyword + "%");
            ssc.addOr("lastname", Op.LIKE, "%" + keyword + "%");
            ssc.addOr("email", Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", Op.LIKE, "%" + keyword + "%");
            ssc.addOr("accountName", Op.LIKE, "%" + keyword + "%");
            if (EnumUtils.isValidEnum(Account.Type.class, keyword.toUpperCase())) {
                ssc.addOr("accountType", Op.EQ, EnumUtils.getEnum(Account.Type.class, keyword.toUpperCase()));
            }

            sc.addAnd("username", Op.SC, ssc);
        }

        if (username != null) {
            sc.setParameters("username", username);
        }

        if (id != null) {
            sc.setParameters("id", id);
        } else {
            // Don't return system user, search builder with NEQ
            sc.setParameters("id", 1);
        }

        if (type != null) {
            sc.setParameters("type", type);
        }

        if (accountName != null) {
            sc.setParameters("accountName", accountName);
            if (domainId != null) {
                sc.setParameters("domainId", domainId);
            }
        } else if (domainId != null) {
            DomainVO domainVO = _domainDao.findById(domainId);
            sc.setParameters("domainPath", domainVO.getPath() + "%");
        }

        if (state != null) {
            sc.setParameters("state", state);
        }

        if (apiKeyAccess != null) {
            try {
                ApiConstants.ApiKeyAccess access = ApiConstants.ApiKeyAccess.valueOf(apiKeyAccess.toUpperCase());
                sc.setParameters("apiKeyAccess", access.toBoolean());
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterValueException("ApiKeyAccess value can only be Enabled/Disabled/Inherit");
            }
        }

        return _userAccountJoinDao.searchAndCount(sc, searchFilter);
    }

    @Override
    public ListResponse<EventResponse> searchForEvents(ListEventsCmd cmd) {
        Pair<List<EventJoinVO>, Integer> result = searchForEventsInternal(cmd);
        ListResponse<EventResponse> response = new ListResponse<>();
        List<EventResponse> eventResponses = ViewResponseHelper.createEventResponse(result.first().toArray(new EventJoinVO[result.first().size()]));
        response.setResponses(eventResponses, result.second());
        return response;
    }

    private Pair<List<EventJoinVO>, Integer> searchForEventsInternal(ListEventsCmd cmd) {
        Pair<List<Long>, Integer> eventIdPage = searchForEventIdsAndCount(cmd);

        Integer count = eventIdPage.second();
        Long[] idArray = eventIdPage.first().toArray(new Long[0]);

        /**
         * Need to check array empty, because {@link com.cloud.utils.db.GenericDaoBase#searchAndCount(SearchCriteria, Filter, boolean)}
         * makes two calls: first to get objects and second to get count.
         * List events has start date filter, there is highly possible cause where no objects loaded
         * and next millisecond new event added and finally we ended up with count = 1 and no ids.
         */
        if (count == 0 || idArray.length < 1) {
            count = 0;
        }

        List<EventJoinVO> events = _eventJoinDao.searchByIds(idArray);
        return new Pair<>(events, count);
    }

    private Pair<List<Long>, Integer> searchForEventIdsAndCount(ListEventsCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        boolean isRootAdmin = accountMgr.isRootAdmin(caller.getId());
        List<Long> permittedAccounts = new ArrayList<>();

        Long id = cmd.getId();
        String type = cmd.getType();
        String level = cmd.getLevel();
        Date startDate = cmd.getStartDate();
        Date endDate = cmd.getEndDate();
        String keyword = cmd.getKeyword();
        Integer entryTime = cmd.getEntryTime();
        Integer duration = cmd.getDuration();
        Long startId = cmd.getStartId();
        final String resourceUuid = cmd.getResourceId();
        final String resourceTypeStr = cmd.getResourceType();
        ApiCommandResourceType resourceType = null;
        Long resourceId = null;
        if (resourceTypeStr != null) {
            resourceType = ApiCommandResourceType.fromString(resourceTypeStr);
            if (resourceType == null) {
                throw new InvalidParameterValueException(String.format("Invalid %s", ApiConstants.RESOURCE_TYPE));
            }
        }
        if (resourceUuid != null) {
            if (resourceTypeStr == null) {
                throw new InvalidParameterValueException(String.format("%s parameter must be used with %s parameter", ApiConstants.RESOURCE_ID, ApiConstants.RESOURCE_TYPE));
            }
            try {
                UUID.fromString(resourceUuid);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterValueException(String.format("Invalid %s", ApiConstants.RESOURCE_ID));
            }
            Object object = entityManager.findByUuidIncludingRemoved(resourceType.getAssociatedClass(), resourceUuid);
            if (object instanceof InternalIdentity) {
                resourceId = ((InternalIdentity)object).getId();
            }
            if (resourceId == null) {
                throw new InvalidParameterValueException(String.format("Invalid %s", ApiConstants.RESOURCE_ID));
            }
            if (!isRootAdmin && object instanceof ControlledEntity) {
                ControlledEntity entity = (ControlledEntity)object;
                accountMgr.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.ListEntry, entity.getAccountId() == caller.getId(), entity);
            }
        }

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<>(cmd.getDomainId(), cmd.isRecursive(), null);
        accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(EventVO.class, "createDate", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        // additional order by since createdDate does not have milliseconds
        // and two events, created within one second can be incorrectly ordered (for example VM.CREATE Completed before Scheduled)
        searchFilter.addOrderBy(EventVO.class, "id", false);

        SearchBuilder<EventVO> eventSearchBuilder = eventDao.createSearchBuilder();
        eventSearchBuilder.select(null, Func.DISTINCT, eventSearchBuilder.entity().getId());
        accountMgr.buildACLSearchBuilder(eventSearchBuilder, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        eventSearchBuilder.and("id", eventSearchBuilder.entity().getId(), SearchCriteria.Op.EQ);
        eventSearchBuilder.and("levelL", eventSearchBuilder.entity().getLevel(), SearchCriteria.Op.LIKE);
        eventSearchBuilder.and("levelEQ", eventSearchBuilder.entity().getLevel(), SearchCriteria.Op.EQ);
        eventSearchBuilder.and("type", eventSearchBuilder.entity().getType(), SearchCriteria.Op.EQ);
        eventSearchBuilder.and("createDateB", eventSearchBuilder.entity().getCreateDate(), SearchCriteria.Op.BETWEEN);
        eventSearchBuilder.and("createDateG", eventSearchBuilder.entity().getCreateDate(), SearchCriteria.Op.GTEQ);
        eventSearchBuilder.and("createDateL", eventSearchBuilder.entity().getCreateDate(), SearchCriteria.Op.LTEQ);
        eventSearchBuilder.and("state", eventSearchBuilder.entity().getState(), SearchCriteria.Op.NEQ);
        eventSearchBuilder.or("startId", eventSearchBuilder.entity().getStartId(), SearchCriteria.Op.EQ);
        eventSearchBuilder.and("createDate", eventSearchBuilder.entity().getCreateDate(), SearchCriteria.Op.BETWEEN);
        eventSearchBuilder.and("displayEvent", eventSearchBuilder.entity().isDisplay(), SearchCriteria.Op.EQ);
        eventSearchBuilder.and("archived", eventSearchBuilder.entity().getArchived(), SearchCriteria.Op.EQ);
        eventSearchBuilder.and("resourceId", eventSearchBuilder.entity().getResourceId(), SearchCriteria.Op.EQ);
        eventSearchBuilder.and("resourceType", eventSearchBuilder.entity().getResourceType(), SearchCriteria.Op.EQ);

        if (keyword != null) {
            eventSearchBuilder.and().op("keywordType", eventSearchBuilder.entity().getType(), SearchCriteria.Op.LIKE);
            eventSearchBuilder.or("keywordDescription", eventSearchBuilder.entity().getDescription(), SearchCriteria.Op.LIKE);
            eventSearchBuilder.or("keywordLevel", eventSearchBuilder.entity().getLevel(), SearchCriteria.Op.LIKE);
            eventSearchBuilder.cp();
        }

        SearchCriteria<EventVO> sc = eventSearchBuilder.create();
        // building ACL condition
        accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        // For end users display only enabled events
        if (!accountMgr.isRootAdmin(caller.getId())) {
            sc.setParameters("displayEvent", true);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (startId != null) {
            sc.setParameters("startId", startId);
            if (id == null) {
                sc.setParameters("id", startId);
            }
        }

        if (keyword != null) {
            sc.setParameters("keywordType", "%" + keyword + "%");
            sc.setParameters("keywordDescription", "%" + keyword + "%");
            sc.setParameters("keywordLevel", "%" + keyword + "%");
        }

        if (level != null) {
            sc.setParameters("levelEQ", level);
        }

        if (type != null) {
            sc.setParameters("type", type);
        }

        if (startDate != null && endDate != null) {
            sc.setParameters("createDateB", startDate, endDate);
        } else if (startDate != null) {
            sc.setParameters("createDateG", startDate);
        } else if (endDate != null) {
            sc.setParameters("createDateL", endDate);
        }

        if (resourceId != null) {
            sc.setParameters("resourceId", resourceId);
        }

        if (resourceType != null) {
            sc.setParameters("resourceType", resourceType.toString());
        }

        if (id == null) {
            sc.setParameters("archived", cmd.getArchived());
        }

        Pair<List<Long>, Integer> eventPair;
        // event_view will not have duplicate rows for each event, so
        // searchAndCount should be good enough.
        if ((entryTime != null) && (duration != null)) {
            // TODO: waiting for response from dev list, logic is mystery to
            // me!!
            /*
             * if (entryTime <= duration) { throw new
             * InvalidParameterValueException
             * ("Entry time must be greater than duration"); } Calendar calMin =
             * Calendar.getInstance(); Calendar calMax = Calendar.getInstance();
             * calMin.add(Calendar.SECOND, -entryTime);
             * calMax.add(Calendar.SECOND, -duration); Date minTime =
             * calMin.getTime(); Date maxTime = calMax.getTime();
             *
             * sc.setParameters("state", com.cloud.event.Event.State.Completed);
             * sc.setParameters("startId", 0); sc.setParameters("createDate",
             * minTime, maxTime); List<EventJoinVO> startedEvents =
             * _eventJoinDao.searchAllEvents(sc, searchFilter);
             * List<EventJoinVO> pendingEvents = new ArrayList<EventJoinVO>();
             * for (EventVO event : startedEvents) { EventVO completedEvent =
             * _eventDao.findCompletedEvent(event.getId()); if (completedEvent
             * == null) { pendingEvents.add(event); } } return pendingEvents;
             */
            eventPair = new Pair<>(new ArrayList<>(), 0);
        } else {
            Pair<List<EventVO>, Integer> uniqueEventPair = eventDao.searchAndCount(sc, searchFilter);
            Integer count = uniqueEventPair.second();
            List<Long> eventIds = uniqueEventPair.first().stream().map(EventVO::getId).collect(Collectors.toList());
            eventPair = new Pair<>(eventIds, count);
        }
        return eventPair;
    }

    @Override
    public ListResponse<ResourceTagResponse> listTags(ListTagsCmd cmd) {
        Pair<List<ResourceTagJoinVO>, Integer> tags = listTagsInternal(cmd);
        ListResponse<ResourceTagResponse> response = new ListResponse<>();
        List<ResourceTagResponse> tagResponses = ViewResponseHelper.createResourceTagResponse(false, tags.first().toArray(new ResourceTagJoinVO[tags.first().size()]));
        response.setResponses(tagResponses, tags.second());
        return response;
    }

    private Pair<List<ResourceTagJoinVO>, Integer> listTagsInternal(ListTagsCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<>();
        String key = cmd.getKey();
        String value = cmd.getValue();
        String resourceId = cmd.getResourceId();
        String resourceType = cmd.getResourceType();
        String customerName = cmd.getCustomer();
        boolean listAll = cmd.listAll();
        Long projectId = cmd.getProjectId();

        if (projectId == null && ResourceObjectType.Project.name().equalsIgnoreCase(resourceType) && StringUtils.isNotEmpty(resourceId)) {
            try {
                projectId = Long.parseLong(resourceId);
            } catch (final NumberFormatException e) {
                final ProjectVO project = _projectDao.findByUuidIncludingRemoved(resourceId);
                if (project != null) {
                    projectId = project.getId();
                }
            }
        }

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<>(cmd.getDomainId(), cmd.isRecursive(), null);

        accountMgr.buildACLSearchParameters(caller, null, cmd.getAccountName(), projectId, permittedAccounts, domainIdRecursiveListProject, listAll, false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        Filter searchFilter = new Filter(ResourceTagJoinVO.class, "resourceType", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        SearchBuilder<ResourceTagJoinVO> sb = _resourceTagJoinDao.createSearchBuilder();
        accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("key", sb.entity().getKey(), SearchCriteria.Op.EQ);
        sb.and("value", sb.entity().getValue(), SearchCriteria.Op.EQ);

        if (resourceId != null) {
            sb.and("resourceId", sb.entity().getResourceId(), SearchCriteria.Op.EQ);
            sb.and("resourceUuid", sb.entity().getResourceUuid(), SearchCriteria.Op.EQ);
        }

        sb.and("resourceType", sb.entity().getResourceType(), SearchCriteria.Op.EQ);
        sb.and("customer", sb.entity().getCustomer(), SearchCriteria.Op.EQ);

        // now set the SC criteria...
        SearchCriteria<ResourceTagJoinVO> sc = sb.create();
        accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (key != null) {
            sc.setParameters("key", key);
        }

        if (value != null) {
            sc.setParameters("value", value);
        }

        if (resourceId != null) {
            try {
                long rid = Long.parseLong(resourceId);
                sc.setParameters("resourceId", rid);
            } catch (NumberFormatException ex) {
                // internal id instead of resource id is passed
                sc.setParameters("resourceUuid", resourceId);
            }
        }

        if (resourceType != null) {
            sc.setParameters("resourceType", resourceType);
        }

        if (customerName != null) {
            sc.setParameters("customer", customerName);
        }

        return _resourceTagJoinDao.searchAndCount(sc, searchFilter);
    }

    @Override
    public ListResponse<InstanceGroupResponse> searchForVmGroups(ListVMGroupsCmd cmd) {
        Pair<List<InstanceGroupJoinVO>, Integer> groups = searchForVmGroupsInternal(cmd);
        ListResponse<InstanceGroupResponse> response = new ListResponse<>();
        List<InstanceGroupResponse> grpResponses = ViewResponseHelper.createInstanceGroupResponse(groups.first().toArray(new InstanceGroupJoinVO[groups.first().size()]));
        response.setResponses(grpResponses, groups.second());
        return response;
    }

    private Pair<List<InstanceGroupJoinVO>, Integer> searchForVmGroupsInternal(ListVMGroupsCmd cmd) {
        Long id = cmd.getId();
        String name = cmd.getGroupName();
        String keyword = cmd.getKeyword();

        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<>(cmd.getDomainId(), cmd.isRecursive(), null);
        accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(InstanceGroupJoinVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        SearchBuilder<InstanceGroupJoinVO> sb = _vmGroupJoinDao.createSearchBuilder();
        accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);

        SearchCriteria<InstanceGroupJoinVO> sc = sb.create();
        accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (keyword != null) {
            SearchCriteria<InstanceGroupJoinVO> ssc = _vmGroupJoinDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (name != null) {
            sc.setParameters("name", name);
        }

        return _vmGroupJoinDao.searchAndCount(sc, searchFilter);
    }

    @Override
    public ListResponse<UserVmResponse> searchForUserVMs(ListVMsCmd cmd) {
        Pair<List<UserVmJoinVO>, Integer> result = searchForUserVMsInternal(cmd);
        ListResponse<UserVmResponse> response = new ListResponse<>();

        if (cmd.getRetrieveOnlyResourceCount()) {
            response.setResponses(new ArrayList<>(), result.second());
            return response;
        }

        ResponseView respView = ResponseView.Restricted;
        Account caller = CallContext.current().getCallingAccount();
        if (accountMgr.isRootAdmin(caller.getId())) {
            respView = ResponseView.Full;
        }
        List<UserVmResponse> vmResponses = ViewResponseHelper.createUserVmResponse(respView, "virtualmachine", cmd.getDetails(), cmd.getAccumulate(), cmd.getShowUserData(),
                result.first().toArray(new UserVmJoinVO[result.first().size()]));

        response.setResponses(vmResponses, result.second());
        return response;
    }

    @Override
    public ListResponse<VirtualMachineResponse> listAffectedVmsForStorageScopeChange(ListAffectedVmsForStorageScopeChangeCmd cmd) {
        Long poolId = cmd.getStorageId();
        StoragePoolVO pool = storagePoolDao.findById(poolId);
        if (pool == null) {
            throw new IllegalArgumentException("Unable to find storage pool with ID: " + poolId);
        }

        ListResponse<VirtualMachineResponse> response = new ListResponse<>();
        List<VirtualMachineResponse> responsesList = new ArrayList<>();
        if (pool.getScope() != ScopeType.ZONE) {
            response.setResponses(responsesList, 0);
            return response;
        }

        Pair<List<VMInstanceVO>, Integer> vms = _vmInstanceDao.listByVmsNotInClusterUsingPool(cmd.getClusterIdForScopeChange(), poolId);
        for (VMInstanceVO vm : vms.first()) {
            VirtualMachineResponse resp = new VirtualMachineResponse();
            resp.setObjectName(VirtualMachine.class.getSimpleName().toLowerCase());
            resp.setId(vm.getUuid());
            resp.setVmType(vm.getType().toString());

            UserVmJoinVO userVM = null;
            if (!vm.getType().isUsedBySystem()) {
                userVM = _userVmJoinDao.findById(vm.getId());
            }
            if (userVM != null) {
                if (userVM.getDisplayName() != null) {
                    resp.setVmName(userVM.getDisplayName());
                } else {
                    resp.setVmName(userVM.getName());
                }
            } else {
                resp.setVmName(vm.getInstanceName());
            }

            HostVO host = hostDao.findById(vm.getHostId());
            if (host != null) {
                resp.setHostId(host.getUuid());
                resp.setHostName(host.getName());
                ClusterVO cluster = clusterDao.findById(host.getClusterId());
                if (cluster != null) {
                    resp.setClusterId(cluster.getUuid());
                    resp.setClusterName(cluster.getName());
                }
            }
            responsesList.add(resp);
        }
        response.setResponses(responsesList, vms.second());
        return response;
    }

    private Object getObjectPossibleMethodValue(Object obj, String methodName) {
        Object result = null;

        try {
            Method m = obj.getClass().getMethod(methodName);
            result = m.invoke(obj);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {}

        return result;
    }

    private Pair<List<UserVmJoinVO>, Integer> searchForUserVMsInternal(ListVMsCmd cmd) {
        Pair<List<Long>, Integer> vmIdPage = searchForUserVMIdsAndCount(cmd);

        Integer count = vmIdPage.second();
        Long[] idArray = vmIdPage.first().toArray(new Long[0]);

        if (count == 0) {
            return new Pair<>(new ArrayList<>(), count);
        }

        // search vm details by ids
        List<UserVmJoinVO> vms = _userVmJoinDao.searchByIds(idArray);
        return new Pair<>(vms, count);
    }

    private Pair<List<Long>, Integer> searchForUserVMIdsAndCount(ListVMsCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<>();
        boolean listAll = cmd.listAll();
        Long id = cmd.getId();
        Boolean display = cmd.getDisplay();
        String hypervisor = cmd.getHypervisor();
        String state = cmd.getState();
        Long zoneId = cmd.getZoneId();
        Long templateId = cmd.getTemplateId();
        Long serviceOfferingId = cmd.getServiceOfferingId();
        Boolean isHaEnabled = cmd.getHaEnabled();
        String keyword = cmd.getKeyword();
        Long networkId = cmd.getNetworkId();
        Long isoId = cmd.getIsoId();
        String vmHostName = cmd.getName();
        Long hostId = null;
        Long podId = null;
        Long clusterId = null;
        Long groupId = cmd.getGroupId();
        Long vpcId = cmd.getVpcId();
        Long affinityGroupId = cmd.getAffinityGroupId();
        String keyPairName = cmd.getKeyPairName();
        Long securityGroupId = cmd.getSecurityGroupId();
        Long autoScaleVmGroupId = cmd.getAutoScaleVmGroupId();
        Long backupOfferingId = cmd.getBackupOfferingId();
        Long storageId = null;
        StoragePoolVO pool = null;
        Long userId = cmd.getUserId();
        Long userdataId = cmd.getUserdataId();
        Map<String, String> tags = cmd.getTags();

        boolean isAdmin = false;
        boolean isRootAdmin = false;

        if (accountMgr.isAdmin(caller.getId())) {
            isAdmin = true;
        }

        if (accountMgr.isRootAdmin(caller.getId())) {
            isRootAdmin = true;
            podId = (Long) getObjectPossibleMethodValue(cmd, "getPodId");
            clusterId = (Long) getObjectPossibleMethodValue(cmd, "getClusterId");
            hostId = (Long) getObjectPossibleMethodValue(cmd, "getHostId");
            storageId = (Long) getObjectPossibleMethodValue(cmd, "getStorageId");
            if (storageId != null) {
                pool = storagePoolDao.findById( storageId);
                if (pool == null) {
                    throw new InvalidParameterValueException("Unable to find specified storage pool");
                }
            }
        }

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<>(cmd.getDomainId(), cmd.isRecursive(), null);
        accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, listAll, false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(UserVmVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        List<Long> ids;
        if (cmd.getId() != null) {
            if (cmd.getIds() != null && !cmd.getIds().isEmpty()) {
                throw new InvalidParameterValueException("Specify either id or ids but not both parameters");
            }
            ids = new ArrayList<>();
            ids.add(cmd.getId());
        } else {
            ids = cmd.getIds();
        }

        SearchBuilder<UserVmVO> userVmSearchBuilder = userVmDao.createSearchBuilder();
        userVmSearchBuilder.select(null, Func.DISTINCT, userVmSearchBuilder.entity().getId());
        accountMgr.buildACLSearchBuilder(userVmSearchBuilder, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (HypervisorType.getType(hypervisor) == HypervisorType.None && hypervisor != null) {
            // invalid hypervisor type input
            throw new InvalidParameterValueException("Invalid HypervisorType " + hypervisor);
        }

        if (ids != null && !ids.isEmpty()) {
            userVmSearchBuilder.and("idIN", userVmSearchBuilder.entity().getId(), Op.IN);
        }

        userVmSearchBuilder.and("displayName", userVmSearchBuilder.entity().getDisplayName(), Op.LIKE);
        userVmSearchBuilder.and("stateEQ", userVmSearchBuilder.entity().getState(), Op.EQ);
        userVmSearchBuilder.and("stateNEQ", userVmSearchBuilder.entity().getState(), Op.NEQ);
        userVmSearchBuilder.and("stateNIN", userVmSearchBuilder.entity().getState(), Op.NIN);

        if (hostId != null) {
            userVmSearchBuilder.and("hostId", userVmSearchBuilder.entity().getHostId(), Op.EQ);
        }

        if (zoneId != null) {
            userVmSearchBuilder.and("dataCenterId", userVmSearchBuilder.entity().getDataCenterId(), Op.EQ);
        }

        if (templateId != null) {
            userVmSearchBuilder.and("templateId", userVmSearchBuilder.entity().getTemplateId(), Op.EQ);
        }

        if (userdataId != null) {
            userVmSearchBuilder.and("userdataId", userVmSearchBuilder.entity().getUserDataId(), Op.EQ);
        }

        if (hypervisor != null) {
            userVmSearchBuilder.and("hypervisorType", userVmSearchBuilder.entity().getHypervisorType(), Op.EQ);
        }

        if (vmHostName != null) {
            userVmSearchBuilder.and("name", userVmSearchBuilder.entity().getHostName(), Op.EQ);
        }

        if (serviceOfferingId != null) {
            userVmSearchBuilder.and("serviceOfferingId", userVmSearchBuilder.entity().getServiceOfferingId(), Op.EQ);
        }
        if (display != null) {
            userVmSearchBuilder.and("display", userVmSearchBuilder.entity().isDisplayVm(), Op.EQ);
        }

        if (!isRootAdmin) {
            userVmSearchBuilder.and("displayVm", userVmSearchBuilder.entity().isDisplayVm(), Op.EQ);
        }

        if (isHaEnabled != null) {
            userVmSearchBuilder.and("haEnabled", userVmSearchBuilder.entity().isHaEnabled(), Op.EQ);
        }

        if (isoId != null) {
            userVmSearchBuilder.and("isoId", userVmSearchBuilder.entity().getIsoId(), Op.EQ);
        }

        if (userId != null) {
            userVmSearchBuilder.and("userId", userVmSearchBuilder.entity().getUserId(), Op.EQ);
        }

        if (podId != null) {
            userVmSearchBuilder.and("podId", userVmSearchBuilder.entity().getPodIdToDeployIn(), Op.EQ);
        }

        if (networkId != null || vpcId != null) {
            SearchBuilder<NicVO> nicSearch = nicDao.createSearchBuilder();
            nicSearch.and("networkId", nicSearch.entity().getNetworkId(), Op.EQ);
            nicSearch.and("removed", nicSearch.entity().getRemoved(), Op.NULL);
            if (vpcId != null) {
                SearchBuilder<NetworkVO> networkSearch = networkDao.createSearchBuilder();
                networkSearch.and("vpcId", networkSearch.entity().getVpcId(), Op.EQ);
                nicSearch.join("vpc", networkSearch, networkSearch.entity().getId(), nicSearch.entity().getNetworkId(), JoinBuilder.JoinType.INNER);
            }
            userVmSearchBuilder.join("nic", nicSearch, nicSearch.entity().getInstanceId(), userVmSearchBuilder.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        if (clusterId != null) {
            userVmSearchBuilder.and().op("hostIdIn", userVmSearchBuilder.entity().getHostId(), Op.IN);
            userVmSearchBuilder.or().op("lastHostIdIn", userVmSearchBuilder.entity().getLastHostId(), Op.IN);
            userVmSearchBuilder.and(userVmSearchBuilder.entity().getState(), Op.EQ).values(VirtualMachine.State.Stopped);
            userVmSearchBuilder.cp().cp();
        }

        if (groupId != null && groupId != -1) {
            SearchBuilder<InstanceGroupVMMapVO> instanceGroupSearch = instanceGroupVMMapDao.createSearchBuilder();
            instanceGroupSearch.and("groupId", instanceGroupSearch.entity().getGroupId(), Op.EQ);
            userVmSearchBuilder.join("instanceGroup", instanceGroupSearch, instanceGroupSearch.entity().getInstanceId(), userVmSearchBuilder.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        if (affinityGroupId != null && affinityGroupId != -1) {
            SearchBuilder<AffinityGroupVMMapVO> affinityGroupSearch = affinityGroupVMMapDao.createSearchBuilder();
            affinityGroupSearch.and("affinityGroupId", affinityGroupSearch.entity().getAffinityGroupId(), Op.EQ);
            userVmSearchBuilder.join("affinityGroup", affinityGroupSearch, affinityGroupSearch.entity().getInstanceId(), userVmSearchBuilder.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        if (securityGroupId != null && securityGroupId != -1) {
            SearchBuilder<SecurityGroupVMMapVO> securityGroupSearch = securityGroupVMMapDao.createSearchBuilder();
            securityGroupSearch.and("securityGroupId", securityGroupSearch.entity().getSecurityGroupId(), Op.EQ);
            userVmSearchBuilder.join("securityGroup", securityGroupSearch, securityGroupSearch.entity().getInstanceId(), userVmSearchBuilder.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        if (storageId != null) {
            SearchBuilder<VolumeVO> volumeSearch = volumeDao.createSearchBuilder();
            if (pool.getPoolType().equals(Storage.StoragePoolType.DatastoreCluster)) {
                volumeSearch.and("storagePoolId", volumeSearch.entity().getPoolId(), Op.IN);
            } else {
                volumeSearch.and("storagePoolId", volumeSearch.entity().getPoolId(), Op.EQ);
            }
            userVmSearchBuilder.join("volume", volumeSearch, volumeSearch.entity().getInstanceId(), userVmSearchBuilder.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        if (tags != null && !tags.isEmpty()) {
            SearchBuilder<ResourceTagVO> resourceTagSearch = resourceTagDao.createSearchBuilder();
            resourceTagSearch.and("resourceType", resourceTagSearch.entity().getResourceType(), Op.EQ);
            resourceTagSearch.and().op();
            for (int count = 0; count < tags.size(); count++) {
                if (count == 0) {
                    resourceTagSearch.op("tagKey" + String.valueOf(count), resourceTagSearch.entity().getKey(), Op.EQ);
                } else {
                    resourceTagSearch.or().op("tagKey" + String.valueOf(count), resourceTagSearch.entity().getKey(), Op.EQ);
                }
                resourceTagSearch.and("tagValue" + String.valueOf(count), resourceTagSearch.entity().getValue(), Op.EQ);
                resourceTagSearch.cp();
            }
            resourceTagSearch.cp();

            userVmSearchBuilder.join("tags", resourceTagSearch, resourceTagSearch.entity().getResourceId(), userVmSearchBuilder.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        if (keyPairName != null) {
            SearchBuilder<UserVmDetailVO> vmDetailSearchKeys = userVmDetailsDao.createSearchBuilder();
            SearchBuilder<UserVmDetailVO> vmDetailSearchVmIds = userVmDetailsDao.createSearchBuilder();
            vmDetailSearchKeys.and(vmDetailSearchKeys.entity().getName(), Op.EQ).values(SSH_PUBLIC_KEY);

            SearchBuilder<SSHKeyPairVO> sshKeyPairSearch = sshKeyPairDao.createSearchBuilder();
            sshKeyPairSearch.and("keyPairName", sshKeyPairSearch.entity().getName(), Op.EQ);

            sshKeyPairSearch.join("keyPairToDetailValueJoin", vmDetailSearchKeys, vmDetailSearchKeys.entity().getValue(), sshKeyPairSearch.entity().getPublicKey(), JoinBuilder.JoinType.INNER);
            userVmSearchBuilder.join("userVmToDetailJoin", vmDetailSearchVmIds, vmDetailSearchVmIds.entity().getResourceId(), userVmSearchBuilder.entity().getId(), JoinBuilder.JoinType.INNER);
            userVmSearchBuilder.join("userVmToKeyPairJoin", sshKeyPairSearch, sshKeyPairSearch.entity().getAccountId(), userVmSearchBuilder.entity().getAccountId(), JoinBuilder.JoinType.INNER);
        }

        if (keyword != null) {
            userVmSearchBuilder.and().op("keywordDisplayName", userVmSearchBuilder.entity().getDisplayName(), Op.LIKE);
            userVmSearchBuilder.or("keywordName", userVmSearchBuilder.entity().getHostName(), Op.LIKE);
            userVmSearchBuilder.or("keywordState", userVmSearchBuilder.entity().getState(), Op.EQ);
            if (isRootAdmin) {
                userVmSearchBuilder.or("keywordInstanceName", userVmSearchBuilder.entity().getInstanceName(), Op.LIKE );
            }

            SearchBuilder<IPAddressVO> ipAddressSearch = ipAddressDao.createSearchBuilder();
            userVmSearchBuilder.join("ipAddressSearch", ipAddressSearch,
                    ipAddressSearch.entity().getAssociatedWithVmId(), userVmSearchBuilder.entity().getId(), JoinBuilder.JoinType.LEFT);

            SearchBuilder<NicVO> nicSearch = nicDao.createSearchBuilder();
            userVmSearchBuilder.join("nicSearch", nicSearch, JoinBuilder.JoinType.LEFT,
                    JoinBuilder.JoinCondition.AND,
                    nicSearch.entity().getInstanceId(), userVmSearchBuilder.entity().getId(),
                    nicSearch.entity().getRemoved(), userVmSearchBuilder.entity().setLong(null));

            userVmSearchBuilder.or("ipAddressSearch", "keywordPublicIpAddress", ipAddressSearch.entity().getAddress(), Op.LIKE);

            userVmSearchBuilder.or("nicSearch", "keywordIpAddress", nicSearch.entity().getIPv4Address(), Op.LIKE);
            userVmSearchBuilder.or("nicSearch", "keywordIp6Address", nicSearch.entity().getIPv6Address(), Op.LIKE);

            userVmSearchBuilder.cp();
        }

        if (backupOfferingId != null) {
            SearchBuilder<BackupOfferingVO> backupOfferingSearch = backupOfferingDao.createSearchBuilder();
            backupOfferingSearch.and("backupOfferingId", backupOfferingSearch.entity().getId(), Op.EQ);
            userVmSearchBuilder.join("backupOffering", backupOfferingSearch, backupOfferingSearch.entity().getId(), userVmSearchBuilder.entity().getBackupOfferingId(), JoinBuilder.JoinType.INNER);
        }

        if (autoScaleVmGroupId != null) {
            SearchBuilder<AutoScaleVmGroupVmMapVO> autoScaleMapSearch = autoScaleVmGroupVmMapDao.createSearchBuilder();
            autoScaleMapSearch.and("autoScaleVmGroupId", autoScaleMapSearch.entity().getVmGroupId(), Op.EQ);
            userVmSearchBuilder.join("autoScaleVmGroup", autoScaleMapSearch, autoScaleMapSearch.entity().getInstanceId(), userVmSearchBuilder.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        Boolean isVnf = cmd.getVnf();
        if (isVnf != null) {
            SearchBuilder<VMTemplateVO> templateSearch = _templateDao.createSearchBuilder();
            templateSearch.and("templateTypeEQ", templateSearch.entity().getTemplateType(), Op.EQ);
            templateSearch.and("templateTypeNEQ", templateSearch.entity().getTemplateType(), Op.NEQ);

            userVmSearchBuilder.join("vmTemplate", templateSearch, templateSearch.entity().getId(), userVmSearchBuilder.entity().getTemplateId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<UserVmVO> userVmSearchCriteria = userVmSearchBuilder.create();
        accountMgr.buildACLSearchCriteria(userVmSearchCriteria, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (serviceOfferingId != null) {
            userVmSearchCriteria.setParameters("serviceOfferingId", serviceOfferingId);
        }

        if (state != null) {
            if (state.equalsIgnoreCase("present")) {
                userVmSearchCriteria.setParameters("stateNIN", "Destroyed", "Expunging");
            } else {
                userVmSearchCriteria.setParameters("stateEQ", state);
            }
        }

        if (hypervisor != null) {
            userVmSearchCriteria.setParameters("hypervisorType", hypervisor);
        }

        // Don't show Destroyed and Expunging vms to the end user if the AllowUserViewDestroyedVM flag is not set.
        if (!isAdmin && !AllowUserViewDestroyedVM.valueIn(caller.getAccountId())) {
            userVmSearchCriteria.setParameters("stateNIN", "Destroyed", "Expunging");
        }

        if (zoneId != null) {
            userVmSearchCriteria.setParameters("dataCenterId", zoneId);
        }

        if (templateId != null) {
            userVmSearchCriteria.setParameters("templateId", templateId);
        }

        if (userdataId != null) {
            userVmSearchCriteria.setParameters("userdataId", userdataId);
        }

        if (display != null) {
            userVmSearchCriteria.setParameters("display", display);
        }

        if (isHaEnabled != null) {
            userVmSearchCriteria.setParameters("haEnabled", isHaEnabled);
        }

        if (isoId != null) {
            userVmSearchCriteria.setParameters("isoId", isoId);
        }

        if (ids != null && !ids.isEmpty()) {
            userVmSearchCriteria.setParameters("idIN", ids.toArray());
        }

        if (vmHostName != null) {
            userVmSearchCriteria.setParameters("name", vmHostName);
        }

        if (groupId != null && groupId != -1) {
            userVmSearchCriteria.setJoinParameters("instanceGroup","groupId", groupId);
        }

        if (affinityGroupId != null && affinityGroupId != -1) {
            userVmSearchCriteria.setJoinParameters("affinityGroup", "affinityGroupId", affinityGroupId);
        }

        if (securityGroupId != null && securityGroupId != -1) {
            userVmSearchCriteria.setJoinParameters("securityGroup","securityGroupId", securityGroupId);
        }

        if (keyword != null) {
            String keywordMatch = "%" + keyword + "%";
            userVmSearchCriteria.setParameters("keywordDisplayName", keywordMatch);
            userVmSearchCriteria.setParameters("keywordName", keywordMatch);
            userVmSearchCriteria.setParameters("keywordState", keyword);
            userVmSearchCriteria.setParameters("keywordIpAddress", keywordMatch);
            userVmSearchCriteria.setParameters("keywordPublicIpAddress", keywordMatch);
            userVmSearchCriteria.setParameters("keywordIp6Address", keywordMatch);
            if (isRootAdmin) {
                userVmSearchCriteria.setParameters("keywordInstanceName", keywordMatch);
            }
        }

        if (tags != null && !tags.isEmpty()) {
            int count = 0;
            userVmSearchCriteria.setJoinParameters("tags","resourceType", ResourceObjectType.UserVm);
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                userVmSearchCriteria.setJoinParameters("tags", "tagKey" + String.valueOf(count), entry.getKey());
                userVmSearchCriteria.setJoinParameters("tags", "tagValue" + String.valueOf(count), entry.getValue());
                count++;
            }
        }

        if (keyPairName != null) {
            userVmSearchCriteria.setJoinParameters("userVmToKeyPairJoin", "keyPairName", keyPairName);
        }

        if (networkId != null) {
            userVmSearchCriteria.setJoinParameters("nic", "networkId", networkId);
        }

        if (vpcId != null) {
            userVmSearchCriteria.getJoin("nic").setJoinParameters("vpc", "vpcId", vpcId);
        }

        if (userId != null) {
            userVmSearchCriteria.setParameters("userId", userId);
        }

        if (backupOfferingId != null) {
            userVmSearchCriteria.setJoinParameters("backupOffering", "backupOfferingId", backupOfferingId);
        }

        if (autoScaleVmGroupId != null) {
            userVmSearchCriteria.setJoinParameters("autoScaleVmGroup", "autoScaleVmGroupId", autoScaleVmGroupId);
        }

        if (isVnf != null) {
            if (isVnf) {
                userVmSearchCriteria.setJoinParameters("vmTemplate", "templateTypeEQ", TemplateType.VNF);
            } else {
                userVmSearchCriteria.setJoinParameters("vmTemplate", "templateTypeNEQ", TemplateType.VNF);
            }
        }

        if (isRootAdmin) {
            if (podId != null) {
                userVmSearchCriteria.setParameters("podId", podId);
                if (state == null) {
                    userVmSearchCriteria.setParameters("stateNEQ", "Destroyed");
                }
            }

            if (clusterId != null) {
                List<HostJoinVO> hosts = hostJoinDao.findByClusterId(clusterId, Host.Type.Routing);
                if (CollectionUtils.isEmpty(hosts)) {
                    // cluster has no hosts, so we cannot find VMs, cancel search.
                    return new Pair<>(new ArrayList<>(), 0);
                }
                List<Long> hostIds = hosts.stream().map(HostJoinVO::getId).collect(Collectors.toList());
                userVmSearchCriteria.setParameters("hostIdIn", hostIds.toArray());
                userVmSearchCriteria.setParameters("lastHostIdIn", hostIds.toArray());
            }

            if (hostId != null) {
                userVmSearchCriteria.setParameters("hostId", hostId);
            }

            if (storageId != null && pool != null) {
                if (pool.getPoolType().equals(Storage.StoragePoolType.DatastoreCluster)) {
                    List<StoragePoolVO> childDatastores = storagePoolDao.listChildStoragePoolsInDatastoreCluster(storageId);
                    List<Long> childDatastoreIds = childDatastores.stream().map(mo -> mo.getId()).collect(Collectors.toList());
                    userVmSearchCriteria.setJoinParameters("volume", "storagePoolId", childDatastoreIds.toArray());
                } else {
                    userVmSearchCriteria.setJoinParameters("volume", "storagePoolId", storageId);
                }
            }
        } else {
            userVmSearchCriteria.setParameters("displayVm", 1);
        }

        Pair<List<UserVmVO>, Integer> uniqueVmPair = userVmDao.searchAndDistinctCount(userVmSearchCriteria, searchFilter, new String[]{"vm_instance.id"});
        Integer count = uniqueVmPair.second();

        List<Long> vmIds = uniqueVmPair.first().stream().map(VMInstanceVO::getId).collect(Collectors.toList());
        return new Pair<>(vmIds, count);
    }

    @Override
    public ListResponse<SecurityGroupResponse> searchForSecurityGroups(ListSecurityGroupsCmd cmd) {
        Pair<List<SecurityGroupJoinVO>, Integer> result = searchForSecurityGroupsInternal(cmd);
        ListResponse<SecurityGroupResponse> response = new ListResponse<>();
        List<SecurityGroupResponse> routerResponses = ViewResponseHelper.createSecurityGroupResponses(result.first());
        response.setResponses(routerResponses, result.second());
        return response;
    }

    private Pair<List<SecurityGroupJoinVO>, Integer> searchForSecurityGroupsInternal(ListSecurityGroupsCmd cmd) throws PermissionDeniedException, InvalidParameterValueException {
        Account caller = CallContext.current().getCallingAccount();
        Long instanceId = cmd.getVirtualMachineId();
        String securityGroup = cmd.getSecurityGroupName();
        Long id = cmd.getId();
        Object keyword = cmd.getKeyword();
        List<Long> permittedAccounts = new ArrayList<>();
        Map<String, String> tags = cmd.getTags();

        if (instanceId != null) {
            UserVmVO userVM = userVmDao.findById(instanceId);
            if (userVM == null) {
                throw new InvalidParameterValueException("Unable to list network groups for virtual machine instance " + instanceId + "; instance not found.");
            }
            accountMgr.checkAccess(caller, null, true, userVM);
            return listSecurityGroupRulesByVM(instanceId.longValue(), cmd.getStartIndex(), cmd.getPageSizeVal());
        }

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<>(cmd.getDomainId(), cmd.isRecursive(), null);
        accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(SecurityGroupJoinVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<SecurityGroupJoinVO> sb = _securityGroupJoinDao.createSearchBuilder();
        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct
        // ids
        accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);

        SearchCriteria<SecurityGroupJoinVO> sc = sb.create();
        accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (tags != null && !tags.isEmpty()) {
            SearchCriteria<SecurityGroupJoinVO> tagSc = _securityGroupJoinDao.createSearchCriteria();
            for (String key : tags.keySet()) {
                SearchCriteria<SecurityGroupJoinVO> tsc = _securityGroupJoinDao.createSearchCriteria();
                tsc.addAnd("tagKey", SearchCriteria.Op.EQ, key);
                tsc.addAnd("tagValue", SearchCriteria.Op.EQ, tags.get(key));
                tagSc.addOr("tagKey", SearchCriteria.Op.SC, tsc);
            }
            sc.addAnd("tagKey", SearchCriteria.Op.SC, tagSc);
        }

        if (securityGroup != null) {
            sc.setParameters("name", securityGroup);
        }

        if (keyword != null) {
            SearchCriteria<SecurityGroupJoinVO> ssc = _securityGroupJoinDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        // search security group together with rules
        Pair<List<SecurityGroupJoinVO>, Integer> uniqueSgPair = _securityGroupJoinDao.searchAndCount(sc, searchFilter);
        Integer count = uniqueSgPair.second();
        if (count.intValue() == 0) {
            // handle empty result cases
            return uniqueSgPair;
        }

        List<SecurityGroupJoinVO> uniqueSgs = uniqueSgPair.first();
        Long[] sgIds = new Long[uniqueSgs.size()];
        int i = 0;
        for (SecurityGroupJoinVO v : uniqueSgs) {
            sgIds[i++] = v.getId();
        }
        List<SecurityGroupJoinVO> sgs = _securityGroupJoinDao.searchByIds(sgIds);
        return new Pair<>(sgs, count);
    }

    private Pair<List<SecurityGroupJoinVO>, Integer> listSecurityGroupRulesByVM(long vmId, long pageInd, long pageSize) {
        Filter sf = new Filter(SecurityGroupVMMapVO.class, null, true, pageInd, pageSize);
        Pair<List<SecurityGroupVMMapVO>, Integer> sgVmMappingPair = securityGroupVMMapDao.listByInstanceId(vmId, sf);
        Integer count = sgVmMappingPair.second();
        if (count.intValue() == 0) {
            // handle empty result cases
            return new Pair<>(new ArrayList<>(), count);
        }
        List<SecurityGroupVMMapVO> sgVmMappings = sgVmMappingPair.first();
        Long[] sgIds = new Long[sgVmMappings.size()];
        int i = 0;
        for (SecurityGroupVMMapVO sgVm : sgVmMappings) {
            sgIds[i++] = sgVm.getSecurityGroupId();
        }
        List<SecurityGroupJoinVO> sgs = _securityGroupJoinDao.searchByIds(sgIds);
        return new Pair<>(sgs, count);
    }

    @Override
    public ListResponse<DomainRouterResponse> searchForRouters(ListRoutersCmd cmd) {
        Pair<List<DomainRouterJoinVO>, Integer> result = searchForRoutersInternal(cmd, cmd.getId(), cmd.getRouterName(), cmd.getState(), cmd.getZoneId(), cmd.getPodId(), cmd.getClusterId(),
                cmd.getHostId(), cmd.getKeyword(), cmd.getNetworkId(), cmd.getVpcId(), cmd.getForVpc(), cmd.getRole(), cmd.getVersion(), cmd.isHealthCheckFailed());
        ListResponse<DomainRouterResponse> response = new ListResponse<>();
        List<DomainRouterResponse> routerResponses = ViewResponseHelper.createDomainRouterResponse(result.first().toArray(new DomainRouterJoinVO[result.first().size()]));
        if (VirtualNetworkApplianceManager.RouterHealthChecksEnabled.value()) {
            for (DomainRouterResponse res : routerResponses) {
                DomainRouterVO resRouter = _routerDao.findByUuid(res.getId());
                res.setHealthChecksFailed(routerHealthCheckResultDao.hasFailingChecks(resRouter.getId()));
                if (cmd.shouldFetchHealthCheckResults()) {
                    res.setHealthCheckResults(responseGenerator.createHealthCheckResponse(resRouter,
                            new ArrayList<>(routerHealthCheckResultDao.getHealthCheckResults(resRouter.getId()))));
                }
            }
        }
        response.setResponses(routerResponses, result.second());
        return response;
    }

    @Override
    public ListResponse<DomainRouterResponse> searchForInternalLbVms(ListInternalLBVMsCmd cmd) {
        Pair<List<DomainRouterJoinVO>, Integer> result = searchForRoutersInternal(cmd, cmd.getId(), cmd.getRouterName(), cmd.getState(), cmd.getZoneId(), cmd.getPodId(), null, cmd.getHostId(),
                cmd.getKeyword(), cmd.getNetworkId(), cmd.getVpcId(), cmd.getForVpc(), cmd.getRole(), null, null);
        ListResponse<DomainRouterResponse> response = new ListResponse<>();
        List<DomainRouterResponse> routerResponses = ViewResponseHelper.createDomainRouterResponse(result.first().toArray(new DomainRouterJoinVO[result.first().size()]));
        if (VirtualNetworkApplianceManager.RouterHealthChecksEnabled.value()) {
            for (DomainRouterResponse res : routerResponses) {
                DomainRouterVO resRouter = _routerDao.findByUuid(res.getId());
                res.setHealthChecksFailed(routerHealthCheckResultDao.hasFailingChecks(resRouter.getId()));
                if (cmd.shouldFetchHealthCheckResults()) {
                    res.setHealthCheckResults(responseGenerator.createHealthCheckResponse(resRouter,
                            new ArrayList<>(routerHealthCheckResultDao.getHealthCheckResults(resRouter.getId()))));
                }
            }
        }

        response.setResponses(routerResponses, result.second());
        return response;
    }

    private Pair<List<DomainRouterJoinVO>, Integer> searchForRoutersInternal(BaseListProjectAndAccountResourcesCmd cmd, Long id, String name, String state, Long zoneId, Long podId, Long clusterId,
            Long hostId, String keyword, Long networkId, Long vpcId, Boolean forVpc, String role, String version, Boolean isHealthCheckFailed) {

        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<>(cmd.getDomainId(), cmd.isRecursive(), null);
        accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        Filter searchFilter = new Filter(DomainRouterJoinVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        SearchBuilder<DomainRouterJoinVO> sb = _routerJoinDao.createSearchBuilder();
        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct
        // ids to get
        // number of
        // records with
        // pagination
        accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("name", sb.entity().getInstanceName(), SearchCriteria.Op.EQ);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        sb.and("clusterId", sb.entity().getClusterId(), SearchCriteria.Op.EQ);
        sb.and("hostId", sb.entity().getHostId(), SearchCriteria.Op.EQ);
        sb.and("vpcId", sb.entity().getVpcId(), SearchCriteria.Op.EQ);
        sb.and("role", sb.entity().getRole(), SearchCriteria.Op.EQ);
        sb.and("version", sb.entity().getTemplateVersion(), SearchCriteria.Op.LIKE);

        if (forVpc != null) {
            if (forVpc) {
                sb.and("forVpc", sb.entity().getVpcId(), SearchCriteria.Op.NNULL);
            } else {
                sb.and("forVpc", sb.entity().getVpcId(), SearchCriteria.Op.NULL);
            }
        }

        if (networkId != null) {
            sb.and("networkId", sb.entity().getNetworkId(), SearchCriteria.Op.EQ);
        }

        List<Long> routersWithFailures = null;
        if (isHealthCheckFailed != null) {
            GenericSearchBuilder<RouterHealthCheckResultVO, Long> routerHealthCheckResultSearch = routerHealthCheckResultDao.createSearchBuilder(Long.class);
            routerHealthCheckResultSearch.and("checkResult", routerHealthCheckResultSearch.entity().getCheckResult(), SearchCriteria.Op.EQ);
            routerHealthCheckResultSearch.selectFields(routerHealthCheckResultSearch.entity().getRouterId());
            routerHealthCheckResultSearch.done();
            SearchCriteria<Long> ssc = routerHealthCheckResultSearch.create();
            ssc.setParameters("checkResult", false);
            routersWithFailures = routerHealthCheckResultDao.customSearch(ssc, null);

            if (routersWithFailures != null && ! routersWithFailures.isEmpty()) {
                if (isHealthCheckFailed) {
                    sb.and("routerId", sb.entity().getId(), SearchCriteria.Op.IN);
                } else {
                    sb.and("routerId", sb.entity().getId(), SearchCriteria.Op.NIN);
                }
            } else if (isHealthCheckFailed) {
                return new Pair<>(Collections.emptyList(), 0);
            }
        }

        SearchCriteria<DomainRouterJoinVO> sc = sb.create();
        accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (keyword != null) {
            SearchCriteria<DomainRouterJoinVO> ssc = _routerJoinDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("instanceName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("networkName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("vpcName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("redundantState", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("instanceName", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.setParameters("name", name);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (state != null) {
            sc.setParameters("state", state);
        }

        if (zoneId != null) {
            sc.setParameters("dataCenterId", zoneId);
        }

        if (podId != null) {
            sc.setParameters("podId", podId);
        }

        if (clusterId != null) {
            sc.setParameters("clusterId", clusterId);
        }

        if (hostId != null) {
            sc.setParameters("hostId", hostId);
        }

        if (networkId != null) {
            sc.setParameters("networkId", networkId);
        }

        if (vpcId != null) {
            sc.setParameters("vpcId", vpcId);
        }

        if (role != null) {
            sc.setParameters("role", role);
        }

        if (version != null) {
            sc.setParameters("version", "Cloudstack Release " + version + "%");
        }

        if (routersWithFailures != null && ! routersWithFailures.isEmpty()) {
            sc.setParameters("routerId", routersWithFailures.toArray(new Object[routersWithFailures.size()]));
        }

        // search VR details by ids
        Pair<List<DomainRouterJoinVO>, Integer> uniqueVrPair = _routerJoinDao.searchAndCount(sc, searchFilter);
        Integer count = uniqueVrPair.second();
        if (count.intValue() == 0) {
            // empty result
            return uniqueVrPair;
        }
        List<DomainRouterJoinVO> uniqueVrs = uniqueVrPair.first();
        Long[] vrIds = new Long[uniqueVrs.size()];
        int i = 0;
        for (DomainRouterJoinVO v : uniqueVrs) {
            vrIds[i++] = v.getId();
        }
        List<DomainRouterJoinVO> vrs = _routerJoinDao.searchByIds(vrIds);
        return new Pair<>(vrs, count);
    }

    @Override
    public ListResponse<ProjectResponse> listProjects(ListProjectsCmd cmd) {
        Pair<List<ProjectJoinVO>, Integer> projects = listProjectsInternal(cmd);
        ListResponse<ProjectResponse> response = new ListResponse<>();
        List<ProjectResponse> projectResponses = ViewResponseHelper.createProjectResponse(cmd.getDetails(), projects.first().toArray(new ProjectJoinVO[projects.first().size()]));
        response.setResponses(projectResponses, projects.second());
        return response;
    }

    private Pair<List<ProjectJoinVO>, Integer> listProjectsInternal(ListProjectsCmd cmd) {

        Long id = cmd.getId();
        String name = cmd.getName();
        String displayText = cmd.getDisplayText();
        String state = cmd.getState();
        String accountName = cmd.getAccountName();
        String username = cmd.getUsername();
        Long domainId = cmd.getDomainId();
        String keyword = cmd.getKeyword();
        Long startIndex = cmd.getStartIndex();
        Long pageSize = cmd.getPageSizeVal();
        boolean listAll = cmd.listAll();
        boolean isRecursive = cmd.isRecursive();
        cmd.getTags();


        Account caller = CallContext.current().getCallingAccount();
        User user = CallContext.current().getCallingUser();
        Long accountId = null;
        Long userId = null;
        String path = null;

        Filter searchFilter = new Filter(ProjectJoinVO.class, "id", false, startIndex, pageSize);
        SearchBuilder<ProjectJoinVO> sb = _projectJoinDao.createSearchBuilder();
        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct
        // ids

        if (accountMgr.isAdmin(caller.getId())) {
            if (domainId != null) {
                DomainVO domain = _domainDao.findById(domainId);
                if (domain == null) {
                    throw new InvalidParameterValueException("Domain id=" + domainId + " doesn't exist in the system");
                }

                accountMgr.checkAccess(caller, domain);

                if (accountName != null) {
                    Account owner = accountMgr.getActiveAccountByName(accountName, domainId);
                    if (owner == null) {
                        throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                    }
                    accountId = owner.getId();
                }
                if (StringUtils.isNotEmpty(username)) {
                    User owner = userDao.getUserByName(username, domainId);
                    if (owner == null) {
                        throw new InvalidParameterValueException("Unable to find user " + username + " in domain " + domainId);
                    }
                    userId = owner.getId();
                    if (accountName == null) {
                        accountId = owner.getAccountId();
                    }
                }
            } else { // domainId == null
                if (accountName != null) {
                    throw new InvalidParameterValueException("could not find account " + accountName + " because domain is not specified");
                }
                if (StringUtils.isNotEmpty(username)) {
                    throw new InvalidParameterValueException("could not find user " + username + " because domain is not specified");
                }
            }
        } else {
            if (accountName != null && !accountName.equals(caller.getAccountName())) {
                throw new PermissionDeniedException("Can't list account " + accountName + " projects; unauthorized");
            }

            if (domainId != null && !domainId.equals(caller.getDomainId())) {
                throw new PermissionDeniedException("Can't list domain id= " + domainId + " projects; unauthorized");
            }

            if (StringUtils.isNotEmpty(username) && !username.equals(user.getUsername())) {
                throw new PermissionDeniedException("Can't list user " + username + " projects; unauthorized");
            }

            accountId = caller.getId();
            userId = user.getId();
        }

        if (domainId == null && accountId == null && (accountMgr.isNormalUser(caller.getId()) || !listAll)) {
            accountId = caller.getId();
            userId = user.getId();
        } else if (accountMgr.isDomainAdmin(caller.getId()) || (isRecursive && !listAll)) {
            DomainVO domain = _domainDao.findById(caller.getDomainId());
            path = domain.getPath();
        }

        if (path != null) {
            sb.and("domainPath", sb.entity().getDomainPath(), SearchCriteria.Op.LIKE);
        }

        if (accountId != null) {
            if (userId == null) {
                sb.and().op("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
                sb.and("userIdNull", sb.entity().getUserId(), Op.NULL);
                sb.cp();
            } else {
                sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
            }
        }

        if (userId != null) {
            sb.and().op("userId", sb.entity().getUserId(), Op.EQ);
            sb.or("userIdNull", sb.entity().getUserId(), Op.NULL);
            sb.cp();
        }

        SearchCriteria<ProjectJoinVO> sc = sb.create();

        if (id != null) {
            sc.addAnd("id", Op.EQ, id);
        }

        if (domainId != null && !isRecursive) {
            sc.addAnd("domainId", Op.EQ, domainId);
        }

        if (name != null) {
            sc.addAnd("name", Op.EQ, name);
        }

        if (displayText != null) {
            sc.addAnd("displayText", Op.EQ, displayText);
        }

        if (accountId != null) {
            sc.setParameters("accountId", accountId);
        }

        if (userId != null) {
            sc.setParameters("userId", userId);
        }

        if (state != null) {
            sc.addAnd("state", Op.EQ, state);
        }

        if (keyword != null) {
            SearchCriteria<ProjectJoinVO> ssc = _projectJoinDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("displayText", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (path != null) {
            sc.setParameters("domainPath", path);
        }

        // search distinct projects to get count
        Pair<List<ProjectJoinVO>, Integer> uniquePrjPair = _projectJoinDao.searchAndCount(sc, searchFilter);
        Integer count = uniquePrjPair.second();
        if (count.intValue() == 0) {
            // handle empty result cases
            return uniquePrjPair;
        }
        List<ProjectJoinVO> uniquePrjs = uniquePrjPair.first();
        Long[] prjIds = new Long[uniquePrjs.size()];
        int i = 0;
        for (ProjectJoinVO v : uniquePrjs) {
            prjIds[i++] = v.getId();
        }
        List<ProjectJoinVO> prjs = _projectJoinDao.searchByIds(prjIds);
        return new Pair<>(prjs, count);
    }

    @Override
    public ListResponse<ProjectInvitationResponse> listProjectInvitations(ListProjectInvitationsCmd cmd) {
        Pair<List<ProjectInvitationJoinVO>, Integer> invites = listProjectInvitationsInternal(cmd);
        ListResponse<ProjectInvitationResponse> response = new ListResponse<>();
        List<ProjectInvitationResponse> projectInvitationResponses = ViewResponseHelper.createProjectInvitationResponse(invites.first().toArray(new ProjectInvitationJoinVO[invites.first().size()]));

        response.setResponses(projectInvitationResponses, invites.second());
        return response;
    }

    public Pair<List<ProjectInvitationJoinVO>, Integer> listProjectInvitationsInternal(ListProjectInvitationsCmd cmd) {
        Long id = cmd.getId();
        Long projectId = cmd.getProjectId();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        String state = cmd.getState();
        boolean activeOnly = cmd.isActiveOnly();
        Long startIndex = cmd.getStartIndex();
        Long pageSizeVal = cmd.getPageSizeVal();
        Long userId = cmd.getUserId();
        boolean isRecursive = cmd.isRecursive();
        boolean listAll = cmd.listAll();

        Account caller = CallContext.current().getCallingAccount();
        User callingUser = CallContext.current().getCallingUser();
        List<Long> permittedAccounts = new ArrayList<>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<>(domainId, isRecursive, null);
        accountMgr.buildACLSearchParameters(caller, id, accountName, projectId, permittedAccounts, domainIdRecursiveListProject, listAll, true);
        domainId = domainIdRecursiveListProject.first();
        isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(ProjectInvitationJoinVO.class, "id", true, startIndex, pageSizeVal);
        SearchBuilder<ProjectInvitationJoinVO> sb = _projectInvitationJoinDao.createSearchBuilder();
        accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        ProjectInvitation invitation = projectInvitationDao.findByUserIdProjectId(callingUser.getId(), callingUser.getAccountId(), projectId == null ? -1 : projectId);
        sb.and("projectId", sb.entity().getProjectId(), SearchCriteria.Op.EQ);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("created", sb.entity().getCreated(), SearchCriteria.Op.GT);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);

        SearchCriteria<ProjectInvitationJoinVO> sc = sb.create();
        accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (projectId != null) {
            sc.setParameters("projectId", projectId);
        }

        if (invitation != null) {
            sc.setParameters("userId", invitation.getForUserId());
        } else if (userId != null) {
            sc.setParameters("userId", userId);
        }

        if (state != null) {
            sc.setParameters("state", state);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (activeOnly) {
            sc.setParameters("state", ProjectInvitation.State.Pending);
            sc.setParameters("created", new Date((DateUtil.currentGMTTime().getTime()) - _projectMgr.getInvitationTimeout()));
        }

        Pair<List<ProjectInvitationJoinVO>, Integer> projectInvitations = _projectInvitationJoinDao.searchAndCount(sc, searchFilter);
        List<ProjectInvitationJoinVO> invitations = projectInvitations.first();
        invitations = invitations.stream().filter(invite -> invite.getUserId() == null || Long.parseLong(invite.getUserId()) == callingUser.getId()).collect(Collectors.toList());
        return new Pair<>(invitations, invitations.size());


    }

    @Override
    public ListResponse<ProjectAccountResponse> listProjectAccounts(ListProjectAccountsCmd cmd) {
        Pair<List<ProjectAccountJoinVO>, Integer> projectAccounts = listProjectAccountsInternal(cmd);
        ListResponse<ProjectAccountResponse> response = new ListResponse<>();
        List<ProjectAccountResponse> projectResponses = ViewResponseHelper.createProjectAccountResponse(projectAccounts.first().toArray(new ProjectAccountJoinVO[projectAccounts.first().size()]));
        response.setResponses(projectResponses, projectAccounts.second());
        return response;
    }

    public Pair<List<ProjectAccountJoinVO>, Integer> listProjectAccountsInternal(ListProjectAccountsCmd cmd) {
        long projectId = cmd.getProjectId();
        String accountName = cmd.getAccountName();
        Long userId = cmd.getUserId();
        String role = cmd.getRole();
        Long startIndex = cmd.getStartIndex();
        Long pageSizeVal = cmd.getPageSizeVal();
        Long projectRoleId = cmd.getProjectRoleId();
        // long projectId, String accountName, String role, Long startIndex,
        // Long pageSizeVal) {
        Account caller = CallContext.current().getCallingAccount();
        User callingUser = CallContext.current().getCallingUser();
        // check that the project exists
        Project project = _projectDao.findById(projectId);

        if (project == null) {
            throw new InvalidParameterValueException("Unable to find the project id=" + projectId);
        }

        // verify permissions - only accounts belonging to the project can list
        // project's account
        if (!accountMgr.isAdmin(caller.getId()) && _projectAccountDao.findByProjectIdUserId(projectId, callingUser.getAccountId(), callingUser.getId()) == null &&
        _projectAccountDao.findByProjectIdAccountId(projectId, caller.getAccountId()) == null) {
            throw new PermissionDeniedException("Account " + caller + " is not authorized to list users of the project id=" + projectId);
        }

        Filter searchFilter = new Filter(ProjectAccountJoinVO.class, "id", false, startIndex, pageSizeVal);
        SearchBuilder<ProjectAccountJoinVO> sb = _projectAccountJoinDao.createSearchBuilder();
        sb.and("accountRole", sb.entity().getAccountRole(), Op.EQ);
        sb.and("projectId", sb.entity().getProjectId(), Op.EQ);

        if (accountName != null) {
            sb.and("accountName", sb.entity().getAccountName(), Op.EQ);
        }

        if (userId != null) {
            sb.and("userId", sb.entity().getUserId(), Op.EQ);
        }
        SearchCriteria<ProjectAccountJoinVO> sc = sb.create();

        sc.setParameters("projectId", projectId);

        if (role != null) {
            sc.setParameters("accountRole", role);
        }

        if (accountName != null) {
            sc.setParameters("accountName", accountName);
        }

        if (projectRoleId != null) {
            sc.setParameters("projectRoleId", projectRoleId);
        }

        if (userId != null) {
            sc.setParameters("userId", userId);
        }

        return _projectAccountJoinDao.searchAndCount(sc, searchFilter);
    }

    @Override
    public ListResponse<HostResponse> searchForServers(ListHostsCmd cmd) {
        // FIXME: do we need to support list hosts with VmId, maybe we should
        // create another command just for this
        // Right now it is handled separately outside this QueryService
        logger.debug(">>>Searching for hosts>>>");
        Pair<List<HostJoinVO>, Integer> hosts = searchForServersInternal(cmd);
        ListResponse<HostResponse> response = new ListResponse<>();
        logger.debug(">>>Generating Response>>>");
        List<HostResponse> hostResponses = ViewResponseHelper.createHostResponse(cmd.getDetails(), hosts.first().toArray(new HostJoinVO[hosts.first().size()]));
        response.setResponses(hostResponses, hosts.second());
        return response;
    }

    public Pair<List<HostJoinVO>, Integer> searchForServersInternal(ListHostsCmd cmd) {
        Pair<List<Long>, Integer> serverIdPage = searchForServerIdsAndCount(cmd);

        Integer count = serverIdPage.second();
        Long[] idArray = serverIdPage.first().toArray(new Long[0]);

        if (count == 0) {
            return new Pair<>(new ArrayList<>(), count);
        }

        List<HostJoinVO> servers = hostJoinDao.searchByIds(idArray);
        return new Pair<>(servers, count);
    }
    public Pair<List<Long>, Integer> searchForServerIdsAndCount(ListHostsCmd cmd) {
        Long zoneId = accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), cmd.getZoneId());
        Object name = cmd.getHostName();
        Object type = cmd.getType();
        Object state = cmd.getState();
        Object pod = cmd.getPodId();
        Object cluster = cmd.getClusterId();
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();
        Object outOfBandManagementEnabled = cmd.isOutOfBandManagementEnabled();
        Object powerState = cmd.getHostOutOfBandManagementPowerState();
        Object resourceState = cmd.getResourceState();
        Object haHosts = cmd.getHaHost();
        Long startIndex = cmd.getStartIndex();
        Long pageSize = cmd.getPageSizeVal();
        Hypervisor.HypervisorType hypervisorType = cmd.getHypervisor();

        Filter searchFilter = new Filter(HostVO.class, "id", Boolean.TRUE, startIndex, pageSize);

        SearchBuilder<HostVO> hostSearchBuilder = hostDao.createSearchBuilder();
        hostSearchBuilder.select(null, Func.DISTINCT, hostSearchBuilder.entity().getId()); // select distinct
        // ids
        hostSearchBuilder.and("id", hostSearchBuilder.entity().getId(), SearchCriteria.Op.EQ);
        hostSearchBuilder.and("name", hostSearchBuilder.entity().getName(), SearchCriteria.Op.EQ);
        hostSearchBuilder.and("type", hostSearchBuilder.entity().getType(), SearchCriteria.Op.EQ);
        hostSearchBuilder.and("status", hostSearchBuilder.entity().getStatus(), SearchCriteria.Op.EQ);
        hostSearchBuilder.and("dataCenterId", hostSearchBuilder.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        hostSearchBuilder.and("podId", hostSearchBuilder.entity().getPodId(), SearchCriteria.Op.EQ);
        hostSearchBuilder.and("clusterId", hostSearchBuilder.entity().getClusterId(), SearchCriteria.Op.EQ);
        hostSearchBuilder.and("resourceState", hostSearchBuilder.entity().getResourceState(), SearchCriteria.Op.EQ);
        hostSearchBuilder.and("hypervisor_type", hostSearchBuilder.entity().getHypervisorType(), SearchCriteria.Op.EQ);

        if (keyword != null) {
            hostSearchBuilder.and().op("keywordName", hostSearchBuilder.entity().getName(), SearchCriteria.Op.LIKE);
            hostSearchBuilder.or("keywordStatus", hostSearchBuilder.entity().getStatus(), SearchCriteria.Op.LIKE);
            hostSearchBuilder.or("keywordType", hostSearchBuilder.entity().getType(), SearchCriteria.Op.LIKE);
            hostSearchBuilder.cp();
        }

        if (outOfBandManagementEnabled != null || powerState != null) {
            SearchBuilder<OutOfBandManagementVO> oobmSearch = outOfBandManagementDao.createSearchBuilder();
            oobmSearch.and("oobmEnabled", oobmSearch.entity().isEnabled(), SearchCriteria.Op.EQ);
            oobmSearch.and("powerState", oobmSearch.entity().getPowerState(), SearchCriteria.Op.EQ);

            hostSearchBuilder.join("oobmSearch", oobmSearch, hostSearchBuilder.entity().getId(), oobmSearch.entity().getHostId(), JoinBuilder.JoinType.INNER);
        }

        String haTag = _haMgr.getHaTag();
        if (haHosts != null && haTag != null && !haTag.isEmpty()) {
            SearchBuilder<HostTagVO> hostTagSearchBuilder = _hostTagDao.createSearchBuilder();
            if ((Boolean)haHosts) {
                hostTagSearchBuilder.and("tag", hostTagSearchBuilder.entity().getTag(), SearchCriteria.Op.EQ);
            } else {
                hostTagSearchBuilder.and().op("tag", hostTagSearchBuilder.entity().getTag(), Op.NEQ);
                hostTagSearchBuilder.or("tagNull", hostTagSearchBuilder.entity().getTag(), Op.NULL);
                hostTagSearchBuilder.cp();
            }
            hostSearchBuilder.join("hostTagSearch", hostTagSearchBuilder, hostSearchBuilder.entity().getId(), hostTagSearchBuilder.entity().getHostId(), JoinBuilder.JoinType.LEFT);
        }

        SearchCriteria<HostVO> sc = hostSearchBuilder.create();

        if (keyword != null) {
            sc.setParameters("keywordName", "%" + keyword + "%");
            sc.setParameters("keywordStatus", "%" + keyword + "%");
            sc.setParameters("keywordType", "%" + keyword + "%");
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (name != null) {
            sc.setParameters("name", name);
        }
        if (type != null) {
            sc.setParameters("type", type);
        }
        if (state != null) {
            sc.setParameters("status", state);
        }
        if (zoneId != null) {
            sc.setParameters("dataCenterId", zoneId);
        }
        if (pod != null) {
            sc.setParameters("podId", pod);
        }
        if (cluster != null) {
            sc.setParameters("clusterId", cluster);
        }

        if (outOfBandManagementEnabled != null) {
            sc.setJoinParameters("oobmSearch", "oobmEnabled", outOfBandManagementEnabled);
        }

        if (powerState != null) {
            sc.setJoinParameters("oobmSearch", "powerState", powerState);
        }

        if (resourceState != null) {
            sc.setParameters("resourceState", resourceState);
        }

        if (haHosts != null && haTag != null && !haTag.isEmpty()) {
            sc.setJoinParameters("hostTagSearch", "tag", haTag);
        }

        if (hypervisorType != HypervisorType.None && hypervisorType != HypervisorType.Any) {
            sc.setParameters("hypervisor_type", hypervisorType);
        }

        Pair<List<HostVO>, Integer> uniqueHostPair = hostDao.searchAndCount(sc, searchFilter);
        Integer count = uniqueHostPair.second();
        List<Long> hostIds = uniqueHostPair.first().stream().map(HostVO::getId).collect(Collectors.toList());
        return new Pair<>(hostIds, count);
    }

    @Override
    public ListResponse<VolumeResponse> searchForVolumes(ListVolumesCmd cmd) {
        Pair<List<VolumeJoinVO>, Integer> result = searchForVolumesInternal(cmd);
        ListResponse<VolumeResponse> response = new ListResponse<>();

        if (cmd.getRetrieveOnlyResourceCount()) {
            response.setResponses(new ArrayList<>(), result.second());
            return response;
        }

        ResponseView respView = cmd.getResponseView();
        Account account = CallContext.current().getCallingAccount();
        if (accountMgr.isRootAdmin(account.getAccountId())) {
            respView = ResponseView.Full;
        }

        List<VolumeResponse> volumeResponses = ViewResponseHelper.createVolumeResponse(respView, result.first().toArray(new VolumeJoinVO[result.first().size()]));

        for (VolumeResponse vr : volumeResponses) {
            String poolId = vr.getStoragePoolId();
            if (poolId == null) {
                continue;
            }

            DataStore store = dataStoreManager.getPrimaryDataStore(poolId);
            if (store == null) {
                continue;
            }

            DataStoreDriver driver = store.getDriver();
            if (driver == null) {
                continue;
            }

            Map<String, String> caps = driver.getCapabilities();
            if (caps != null) {
                boolean quiescevm = Boolean.parseBoolean(caps.get(DataStoreCapabilities.VOLUME_SNAPSHOT_QUIESCEVM.toString()));
                vr.setNeedQuiescevm(quiescevm);

                boolean supportsStorageSnapshot = Boolean.parseBoolean(caps.get(DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT.toString()));
                vr.setSupportsStorageSnapshot(supportsStorageSnapshot);
            }
        }
        response.setResponses(volumeResponses, result.second());
        return response;
    }

    private Pair<List<VolumeJoinVO>, Integer> searchForVolumesInternal(ListVolumesCmd cmd) {
        Pair<List<Long>, Integer> volumeIdPage = searchForVolumeIdsAndCount(cmd);

        Integer count = volumeIdPage.second();
        Long[] idArray = volumeIdPage.first().toArray(new Long[0]);

        if (count == 0) {
            return new Pair<>(new ArrayList<>(), count);
        }

        List<VolumeJoinVO> vms = _volumeJoinDao.searchByIds(idArray);
        return new Pair<>(vms, count);
    }
    private Pair<List<Long>, Integer> searchForVolumeIdsAndCount(ListVolumesCmd cmd) {

        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<>();

        Long id = cmd.getId();
        Long vmInstanceId = cmd.getVirtualMachineId();
        String name = cmd.getVolumeName();
        String keyword = cmd.getKeyword();
        String type = cmd.getType();
        Map<String, String> tags = cmd.getTags();
        String storageId = cmd.getStorageId();
        Long clusterId = cmd.getClusterId();
        Long serviceOfferingId = cmd.getServiceOfferingId();
        Long diskOfferingId = cmd.getDiskOfferingId();
        Boolean display = cmd.getDisplay();
        String state = cmd.getState();
        boolean shouldListSystemVms = shouldListSystemVms(cmd, caller.getId());

        Long zoneId = cmd.getZoneId();
        Long podId = cmd.getPodId();

        List<Long> ids = getIdsListFromCmd(cmd.getId(), cmd.getIds());

        if (diskOfferingId == null && serviceOfferingId != null) {
            ServiceOfferingVO serviceOffering = _srvOfferingDao.findById(serviceOfferingId);
            if (serviceOffering != null) {
                diskOfferingId = serviceOffering.getDiskOfferingId();
            }
        }

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<>(cmd.getDomainId(), cmd.isRecursive(), null);
        accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        Filter searchFilter = new Filter(VolumeVO.class, "created", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        SearchBuilder<VolumeVO> volumeSearchBuilder = volumeDao.createSearchBuilder();
        volumeSearchBuilder.select(null, Func.DISTINCT, volumeSearchBuilder.entity().getId()); // select distinct
        accountMgr.buildACLSearchBuilder(volumeSearchBuilder, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (CollectionUtils.isNotEmpty(ids)) {
            volumeSearchBuilder.and("idIN", volumeSearchBuilder.entity().getId(), SearchCriteria.Op.IN);
        }

        volumeSearchBuilder.and("name", volumeSearchBuilder.entity().getName(), SearchCriteria.Op.EQ);
        volumeSearchBuilder.and("volumeType", volumeSearchBuilder.entity().getVolumeType(), SearchCriteria.Op.LIKE);
        volumeSearchBuilder.and("uuid", volumeSearchBuilder.entity().getUuid(), SearchCriteria.Op.NNULL);
        volumeSearchBuilder.and("instanceId", volumeSearchBuilder.entity().getInstanceId(), SearchCriteria.Op.EQ);
        volumeSearchBuilder.and("dataCenterId", volumeSearchBuilder.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        if (cmd.isEncrypted() != null) {
            if (cmd.isEncrypted()) {
                volumeSearchBuilder.and("encryptFormat", volumeSearchBuilder.entity().getEncryptFormat(), SearchCriteria.Op.NNULL);
            } else {
                volumeSearchBuilder.and("encryptFormat", volumeSearchBuilder.entity().getEncryptFormat(), SearchCriteria.Op.NULL);
            }
        }

        if (keyword != null) {
            volumeSearchBuilder.and().op("keywordName", volumeSearchBuilder.entity().getName(), SearchCriteria.Op.LIKE);
            volumeSearchBuilder.or("keywordVolumeType", volumeSearchBuilder.entity().getVolumeType(), SearchCriteria.Op.LIKE);
            volumeSearchBuilder.or("keywordState", volumeSearchBuilder.entity().getState(), SearchCriteria.Op.LIKE);
            volumeSearchBuilder.cp();
        }

        StoragePoolVO poolVO = null;
        if (storageId != null) {
            poolVO = storagePoolDao.findByUuid(storageId);
            if (poolVO == null) {
                throw new InvalidParameterValueException("Unable to find storage pool by uuid " + storageId);
            } else if (poolVO.getPoolType() == Storage.StoragePoolType.DatastoreCluster) {
                volumeSearchBuilder.and("storageId", volumeSearchBuilder.entity().getPoolId(), SearchCriteria.Op.IN);
            } else {
                volumeSearchBuilder.and("storageId", volumeSearchBuilder.entity().getPoolId(), SearchCriteria.Op.EQ);
            }
        }

        if (clusterId != null || podId != null) {
            SearchBuilder<StoragePoolVO> storagePoolSearch = storagePoolDao.createSearchBuilder();
            storagePoolSearch.and("clusterId", storagePoolSearch.entity().getClusterId(), SearchCriteria.Op.EQ);
            storagePoolSearch.and("podId", storagePoolSearch.entity().getPodId(), SearchCriteria.Op.EQ);
            volumeSearchBuilder.join("storagePoolSearch", storagePoolSearch, storagePoolSearch.entity().getId(), volumeSearchBuilder.entity().getPoolId(), JoinBuilder.JoinType.INNER);
        }

        volumeSearchBuilder.and("diskOfferingId", volumeSearchBuilder.entity().getDiskOfferingId(), SearchCriteria.Op.EQ);
        volumeSearchBuilder.and("display", volumeSearchBuilder.entity().isDisplayVolume(), SearchCriteria.Op.EQ);
        volumeSearchBuilder.and("state", volumeSearchBuilder.entity().getState(), SearchCriteria.Op.EQ);
        volumeSearchBuilder.and("stateNEQ", volumeSearchBuilder.entity().getState(), SearchCriteria.Op.NEQ);

        // Need to test thoroughly
        if (!shouldListSystemVms) {
            SearchBuilder<VMInstanceVO> vmSearch = _vmInstanceDao.createSearchBuilder();
            SearchBuilder<ServiceOfferingVO> serviceOfferingSearch = _srvOfferingDao.createSearchBuilder();
            vmSearch.and().op("svmType", vmSearch.entity().getType(), SearchCriteria.Op.NIN);
            vmSearch.or("vmSearchNulltype", vmSearch.entity().getType(), SearchCriteria.Op.NULL);
            vmSearch.cp();

            serviceOfferingSearch.and().op("systemUse", serviceOfferingSearch.entity().isSystemUse(), SearchCriteria.Op.NEQ);
            serviceOfferingSearch.or("serviceOfferingSearchNulltype", serviceOfferingSearch.entity().isSystemUse(), SearchCriteria.Op.NULL);
            serviceOfferingSearch.cp();

            vmSearch.join("serviceOfferingSearch", serviceOfferingSearch, serviceOfferingSearch.entity().getId(), vmSearch.entity().getServiceOfferingId(), JoinBuilder.JoinType.LEFT);

            volumeSearchBuilder.join("vmSearch", vmSearch, vmSearch.entity().getId(), volumeSearchBuilder.entity().getInstanceId(), JoinBuilder.JoinType.LEFT);

        }

        if (MapUtils.isNotEmpty(tags)) {
            SearchBuilder<ResourceTagVO> resourceTagSearch = resourceTagDao.createSearchBuilder();
            resourceTagSearch.and("resourceType", resourceTagSearch.entity().getResourceType(), Op.EQ);
            resourceTagSearch.and().op();
            for (int count = 0; count < tags.size(); count++) {
                if (count == 0) {
                    resourceTagSearch.op("tagKey" + count, resourceTagSearch.entity().getKey(), Op.EQ);
                } else {
                    resourceTagSearch.or().op("tagKey" + count, resourceTagSearch.entity().getKey(), Op.EQ);
                }
                resourceTagSearch.and("tagValue" + count, resourceTagSearch.entity().getValue(), Op.EQ);
                resourceTagSearch.cp();
            }
            resourceTagSearch.cp();

            volumeSearchBuilder.join("tags", resourceTagSearch, resourceTagSearch.entity().getResourceId(), volumeSearchBuilder.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        // now set the SC criteria...
        SearchCriteria<VolumeVO> sc = volumeSearchBuilder.create();
        accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (keyword != null) {
            sc.setParameters("keywordName", "%" + keyword + "%");
            sc.setParameters("keywordVolumeType", "%" + keyword + "%");
            sc.setParameters("keywordState", "%" + keyword + "%");
        }

        if (name != null) {
            sc.setParameters("name", name);
        }

        if (display != null) {
            sc.setParameters("display", display);
        }

        setIdsListToSearchCriteria(sc, ids);

        if (!shouldListSystemVms) {
            sc.setJoinParameters("vmSearch", "svmType", VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm, VirtualMachine.Type.DomainRouter);
            sc.getJoin("vmSearch").setJoinParameters("serviceOfferingSearch", "systemUse", 1);
        }

        if (MapUtils.isNotEmpty(tags)) {
            int count = 0;
            sc.setJoinParameters("tags", "resourceType", ResourceObjectType.Volume);
            for (Map.Entry<String, String> entry  : tags.entrySet()) {
                sc.setJoinParameters("tags", "tagKey" + count, entry.getKey());
                sc.setJoinParameters("tags", "tagValue" + count, entry.getValue());
                count++;
            }
        }

        if (diskOfferingId != null) {
            sc.setParameters("diskOfferingId", diskOfferingId);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (type != null) {
            sc.setParameters("volumeType", "%" + type + "%");
        }
        if (vmInstanceId != null) {
            sc.setParameters("instanceId", vmInstanceId);
        }
        if (zoneId != null) {
            sc.setParameters("dataCenterId", zoneId);
        }

        if (storageId != null) {
            if (poolVO.getPoolType() == Storage.StoragePoolType.DatastoreCluster) {
                List<StoragePoolVO> childDataStores = storagePoolDao.listChildStoragePoolsInDatastoreCluster(poolVO.getId());
                sc.setParameters("storageId", childDataStores.stream().map(StoragePoolVO::getId).toArray());
            } else {
                sc.setParameters("storageId", poolVO.getId());
            }
        }

        if (clusterId != null) {
            sc.setJoinParameters("storagePoolSearch", "clusterId", clusterId);
        }
        if (podId != null) {
            sc.setJoinParameters("storagePoolSearch", "podId", podId);
        }

        if (state != null) {
            sc.setParameters("state", state);
        } else if (!accountMgr.isAdmin(caller.getId())) {
            sc.setParameters("stateNEQ", Volume.State.Expunged);
        }

        // search Volume details by ids
        Pair<List<VolumeVO>, Integer> uniqueVolPair = volumeDao.searchAndCount(sc, searchFilter);
        Integer count = uniqueVolPair.second();
        List<Long> vmIds = uniqueVolPair.first().stream().map(VolumeVO::getId).collect(Collectors.toList());
        return new Pair<>(vmIds, count);
    }

    private boolean shouldListSystemVms(ListVolumesCmd cmd, Long callerId) {
        return Boolean.TRUE.equals(cmd.getListSystemVms()) && accountMgr.isRootAdmin(callerId);
    }

    @Override
    public ListResponse<DomainResponse> searchForDomains(ListDomainsCmd cmd) {
        Pair<List<DomainJoinVO>, Integer> result = searchForDomainsInternal(cmd);
        ListResponse<DomainResponse> response = new ListResponse<>();

        ResponseView respView = ResponseView.Restricted;
        if (cmd instanceof ListDomainsCmdByAdmin) {
            respView = ResponseView.Full;
        }

        List<DomainResponse> domainResponses = ViewResponseHelper.createDomainResponse(respView, cmd.getDetails(), result.first());
        response.setResponses(domainResponses, result.second());
        return response;
    }

    private Pair<List<DomainJoinVO>, Integer> searchForDomainsInternal(ListDomainsCmd cmd) {
        Pair<List<Long>, Integer> domainIdPage = searchForDomainIdsAndCount(cmd);

        Integer count = domainIdPage.second();
        Long[] idArray = domainIdPage.first().toArray(new Long[0]);

        if (count == 0) {
            return new Pair<>(new ArrayList<>(), count);
        }

        List<DomainJoinVO> domains = _domainJoinDao.searchByIds(idArray);
        return new Pair<>(domains, count);
    }

    private Pair<List<Long>, Integer> searchForDomainIdsAndCount(ListDomainsCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Long domainId = cmd.getId();
        boolean listAll = cmd.listAll();
        boolean isRecursive = false;
        Domain domain = null;

        if (domainId != null) {
            domain = _domainDao.findById(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Domain id=" + domainId + " doesn't exist");
            }
            accountMgr.checkAccess(caller, domain);
        } else {
            if (caller.getType() != Account.Type.ADMIN) {
                domainId = caller.getDomainId();
            }
            if (listAll) {
                isRecursive = true;
            }
        }

        Filter searchFilter = new Filter(DomainVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        String domainName = cmd.getDomainName();
        Integer level = cmd.getLevel();
        Object keyword = cmd.getKeyword();

        SearchBuilder<DomainVO> domainSearchBuilder = _domainDao.createSearchBuilder();
        domainSearchBuilder.select(null, Func.DISTINCT, domainSearchBuilder.entity().getId()); // select distinct
        domainSearchBuilder.and("id", domainSearchBuilder.entity().getId(), SearchCriteria.Op.EQ);
        domainSearchBuilder.and("name", domainSearchBuilder.entity().getName(), SearchCriteria.Op.EQ);
        domainSearchBuilder.and("level", domainSearchBuilder.entity().getLevel(), SearchCriteria.Op.EQ);
        domainSearchBuilder.and("path", domainSearchBuilder.entity().getPath(), SearchCriteria.Op.LIKE);
        domainSearchBuilder.and("state", domainSearchBuilder.entity().getState(), SearchCriteria.Op.EQ);

        if (keyword != null) {
            domainSearchBuilder.and("keywordName", domainSearchBuilder.entity().getName(), SearchCriteria.Op.LIKE);
        }

        SearchCriteria<DomainVO> sc = domainSearchBuilder.create();

        if (keyword != null) {
            sc.setParameters("keywordName", "%" + keyword + "%");
        }

        if (domainName != null) {
            sc.setParameters("name", domainName);
        }

        if (level != null) {
            sc.setParameters("level", level);
        }

        if (domainId != null) {
            if (isRecursive) {
                if (domain == null) {
                    domain = _domainDao.findById(domainId);
                }
                sc.setParameters("path", domain.getPath() + "%");
            } else {
                sc.setParameters("id", domainId);
            }
        }

        // return only Active domains to the API
        sc.setParameters("state", Domain.State.Active);

        Pair<List<DomainVO>, Integer> uniqueDomainPair = _domainDao.searchAndCount(sc, searchFilter);
        Integer count = uniqueDomainPair.second();
        List<Long> domainIds = uniqueDomainPair.first().stream().map(DomainVO::getId).collect(Collectors.toList());
        return new Pair<>(domainIds, count);
    }

    @Override
    public ListResponse<AccountResponse> searchForAccounts(ListAccountsCmd cmd) {
        Pair<List<AccountJoinVO>, Integer> result = searchForAccountsInternal(cmd);
        ListResponse<AccountResponse> response = new ListResponse<>();

        ResponseView respView = ResponseView.Restricted;
        if (cmd instanceof ListAccountsCmdByAdmin) {
            respView = ResponseView.Full;
        }

        List<AccountResponse> accountResponses = ViewResponseHelper.createAccountResponse(respView, cmd.getDetails(), result.first().toArray(new AccountJoinVO[result.first().size()]));
        response.setResponses(accountResponses, result.second());
        return response;
    }

    private Pair<List<AccountJoinVO>, Integer> searchForAccountsInternal(ListAccountsCmd cmd) {

        Pair<List<Long>, Integer> accountIdPage = searchForAccountIdsAndCount(cmd);

        Integer count = accountIdPage.second();
        Long[] idArray = accountIdPage.first().toArray(new Long[0]);

        if (count == 0) {
            return new Pair<>(new ArrayList<>(), count);
        }

        List<AccountJoinVO> accounts = _accountJoinDao.searchByIds(idArray);
        return new Pair<>(accounts, count);
    }

    private Pair<List<Long>, Integer> searchForAccountIdsAndCount(ListAccountsCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Long domainId = cmd.getDomainId();
        Long accountId = cmd.getId();
        String accountName = cmd.getSearchName();
        boolean isRecursive = cmd.isRecursive();
        boolean listAll = cmd.listAll();
        boolean callerIsAdmin = accountMgr.isAdmin(caller.getId());
        Account account;
        Domain domain = null;

        // if "domainid" specified, perform validation
        if (domainId != null) {
            // ensure existence...
            domain = _domainDao.findById(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Domain id=" + domainId + " doesn't exist");
            }
            // ... and check access rights.
            accountMgr.checkAccess(caller, domain);
        }

        // if no "id" specified...
        if (accountId == null) {
            // listall only has significance if they are an admin
            boolean isDomainListAllAllowed = AllowUserViewAllDomainAccounts.valueIn(caller.getDomainId());
            if ((listAll && callerIsAdmin) || isDomainListAllAllowed) {
                // if no domain id specified, use caller's domain
                if (domainId == null) {
                    domainId = caller.getDomainId();
                }
                // mark recursive
                isRecursive = true;
            } else if (!callerIsAdmin || domainId == null) {
                accountId = caller.getAccountId();
            }
        } else if (domainId != null && accountName != null) {
            // if they're looking for an account by name
            account = _accountDao.findActiveAccount(accountName, domainId);
            if (account == null || account.getId() == Account.ACCOUNT_ID_SYSTEM) {
                throw new InvalidParameterValueException("Unable to find account by name " + accountName + " in domain " + domainId);
            }
            accountMgr.checkAccess(caller, null, true, account);
        } else {
            // if they specified an "id"...
            if (domainId == null) {
                account = _accountDao.findById(accountId);
            } else {
                account = _accountDao.findActiveAccountById(accountId, domainId);
            }
            if (account == null || account.getId() == Account.ACCOUNT_ID_SYSTEM) {
                throw new InvalidParameterValueException("Unable to find account by id " + accountId + (domainId == null ? "" : " in domain " + domainId));
            }
            accountMgr.checkAccess(caller, null, true, account);
        }

        Filter searchFilter = new Filter(AccountVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object type = cmd.getAccountType();
        Object state = cmd.getState();
        Object isCleanupRequired = cmd.isCleanupRequired();
        Object keyword = cmd.getKeyword();
        String apiKeyAccess = cmd.getApiKeyAccess();

        SearchBuilder<AccountVO> accountSearchBuilder = _accountDao.createSearchBuilder();
        accountSearchBuilder.select(null, Func.DISTINCT, accountSearchBuilder.entity().getId()); // select distinct
        accountSearchBuilder.and("accountName", accountSearchBuilder.entity().getAccountName(), SearchCriteria.Op.EQ);
        accountSearchBuilder.and("domainId", accountSearchBuilder.entity().getDomainId(), SearchCriteria.Op.EQ);
        accountSearchBuilder.and("id", accountSearchBuilder.entity().getId(), SearchCriteria.Op.EQ);
        accountSearchBuilder.and("type", accountSearchBuilder.entity().getType(), SearchCriteria.Op.EQ);
        accountSearchBuilder.and("state", accountSearchBuilder.entity().getState(), SearchCriteria.Op.EQ);
        accountSearchBuilder.and("needsCleanup", accountSearchBuilder.entity().getNeedsCleanup(), SearchCriteria.Op.EQ);
        accountSearchBuilder.and("typeNEQ", accountSearchBuilder.entity().getType(), SearchCriteria.Op.NEQ);
        accountSearchBuilder.and("idNEQ", accountSearchBuilder.entity().getId(), SearchCriteria.Op.NEQ);
        accountSearchBuilder.and("type2NEQ", accountSearchBuilder.entity().getType(), SearchCriteria.Op.NEQ);
        if (apiKeyAccess != null) {
            accountSearchBuilder.and("apiKeyAccess", accountSearchBuilder.entity().getApiKeyAccess(), Op.EQ);
        }

        if (domainId != null && isRecursive) {
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            accountSearchBuilder.join("domainSearch", domainSearch, domainSearch.entity().getId(), accountSearchBuilder.entity().getDomainId(), JoinBuilder.JoinType.INNER);
        }

        if (keyword != null) {
            accountSearchBuilder.and().op("keywordAccountName", accountSearchBuilder.entity().getAccountName(), SearchCriteria.Op.LIKE);
            accountSearchBuilder.or("keywordState", accountSearchBuilder.entity().getState(), SearchCriteria.Op.LIKE);
            accountSearchBuilder.cp();
        }

        SearchCriteria<AccountVO> sc = accountSearchBuilder.create();

        // don't return account of type project to the end user
        sc.setParameters("typeNEQ", Account.Type.PROJECT);

        // don't return system account...
        sc.setParameters("idNEQ", Account.ACCOUNT_ID_SYSTEM);

        // do not return account of type domain admin to the end user
        if (!callerIsAdmin) {
            sc.setParameters("type2NEQ", Account.Type.DOMAIN_ADMIN);
        }

        if (keyword != null) {
            sc.setParameters("keywordAccountName", "%" + keyword + "%");
            sc.setParameters("keywordState", "%" + keyword + "%");
        }

        if (type != null) {
            sc.setParameters("type", type);
        }

        if (state != null) {
            sc.setParameters("state", state);
        }

        if (isCleanupRequired != null) {
            sc.setParameters("needsCleanup", isCleanupRequired);
        }

        if (accountName != null) {
            sc.setParameters("accountName", accountName);
        }

        if (accountId != null) {
            sc.setParameters("id", accountId);
        }

        if (domainId != null) {
            if (isRecursive) {
                // will happen if no "domainid" was specified in the request...
                if (domain == null) {
                    domain = _domainDao.findById(domainId);
                }
                sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
            } else {
                sc.setParameters("domainId", domainId);
            }
        }

        if (apiKeyAccess != null) {
            try {
                ApiConstants.ApiKeyAccess access = ApiConstants.ApiKeyAccess.valueOf(apiKeyAccess.toUpperCase());
                sc.setParameters("apiKeyAccess", access.toBoolean());
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterValueException("ApiKeyAccess value can only be Enabled/Disabled/Inherit");
            }
        }

        Pair<List<AccountVO>, Integer> uniqueAccountPair = _accountDao.searchAndCount(sc, searchFilter);
        Integer count = uniqueAccountPair.second();
        List<Long> accountIds = uniqueAccountPair.first().stream().map(AccountVO::getId).collect(Collectors.toList());
        return new Pair<>(accountIds, count);
    }

    @Override
    public ListResponse<AsyncJobResponse> searchForAsyncJobs(ListAsyncJobsCmd cmd) {
        Pair<List<AsyncJobJoinVO>, Integer> result = searchForAsyncJobsInternal(cmd);
        ListResponse<AsyncJobResponse> response = new ListResponse<>();
        List<AsyncJobResponse> jobResponses = ViewResponseHelper.createAsyncJobResponse(result.first().toArray(new AsyncJobJoinVO[result.first().size()]));
        response.setResponses(jobResponses, result.second());
        return response;
    }

    private Pair<List<AsyncJobJoinVO>, Integer> searchForAsyncJobsInternal(ListAsyncJobsCmd cmd) {

        Account caller = CallContext.current().getCallingAccount();

        List<Long> permittedAccounts = new ArrayList<>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<>(cmd.getDomainId(), cmd.isRecursive(), null);
        accountMgr.buildACLSearchParameters(caller, null, cmd.getAccountName(), null, permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(AsyncJobJoinVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<AsyncJobJoinVO> sb = _jobJoinDao.createSearchBuilder();
        sb.and("instanceTypeNEQ", sb.entity().getInstanceType(), SearchCriteria.Op.NEQ);
        sb.and("accountIdIN", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        boolean accountJoinIsDone = false;
        if (permittedAccounts.isEmpty() && domainId != null) {
            sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
            sb.and("path", sb.entity().getDomainPath(), SearchCriteria.Op.LIKE);
            accountJoinIsDone = true;
        }

        if (listProjectResourcesCriteria != null) {

            if (listProjectResourcesCriteria == Project.ListProjectResourcesCriteria.ListProjectResourcesOnly) {
                sb.and("type", sb.entity().getAccountType(), SearchCriteria.Op.EQ);
            } else if (listProjectResourcesCriteria == Project.ListProjectResourcesCriteria.SkipProjectResources) {
                sb.and("type", sb.entity().getAccountType(), SearchCriteria.Op.NEQ);
            }

            if (!accountJoinIsDone) {
                sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
                sb.and("path", sb.entity().getDomainPath(), SearchCriteria.Op.LIKE);
            }
        }

        if (cmd.getManagementServerId() != null) {
            sb.and("executingMsid", sb.entity().getExecutingMsid(), SearchCriteria.Op.EQ);
        }

        Object keyword = cmd.getKeyword();
        Object startDate = cmd.getStartDate();

        SearchCriteria<AsyncJobJoinVO> sc = sb.create();
        sc.setParameters("instanceTypeNEQ", AsyncJobVO.PSEUDO_JOB_INSTANCE_TYPE);
        if (listProjectResourcesCriteria != null) {
            sc.setParameters("type", Account.Type.PROJECT);
        }

        if (!permittedAccounts.isEmpty()) {
            sc.setParameters("accountIdIN", permittedAccounts.toArray());
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            if (isRecursive) {
                sc.setParameters("path", domain.getPath() + "%");
            } else {
                sc.setParameters("domainId", domainId);
            }
        }

        if (keyword != null) {
            sc.addAnd("cmd", SearchCriteria.Op.LIKE, "%" + keyword + "%");
        }

        if (startDate != null) {
            sc.addAnd("created", SearchCriteria.Op.GTEQ, startDate);
        }

        if (cmd.getManagementServerId() != null) {
            ManagementServerHostVO msHost = msHostDao.findById(cmd.getManagementServerId());
            sc.setParameters("executingMsid", msHost.getMsid());
        }

        return _jobJoinDao.searchAndCount(sc, searchFilter);
    }

    @Override
    public ListResponse<StoragePoolResponse> searchForStoragePools(ListStoragePoolsCmd cmd) {
        Pair<List<StoragePoolJoinVO>, Integer> result = (ScopeType.HOST.name().equalsIgnoreCase(cmd.getScope()) && cmd.getHostId() != null) ?
                searchForLocalStorages(cmd) : searchForStoragePoolsInternal(cmd);
        return createStoragesPoolResponse(result, cmd.getCustomStats());
    }

    private Pair<List<StoragePoolJoinVO>, Integer> searchForLocalStorages(ListStoragePoolsCmd cmd) {
        long id = cmd.getHostId();
        List<StoragePoolHostVO> localstoragePools = storagePoolHostDao.listByHostId(id);
        Long[] poolIds = new Long[localstoragePools.size()];
        int i = 0;
        for(StoragePoolHostVO localstoragePool : localstoragePools) {
            StoragePool storagePool = storagePoolDao.findById(localstoragePool.getPoolId());
            if (storagePool != null && storagePool.isLocal()) {
                poolIds[i++] = localstoragePool.getPoolId();
            }
        }
        List<StoragePoolJoinVO> pools = _poolJoinDao.searchByIds(poolIds);
        return new Pair<>(pools, pools.size());
    }

    private void setPoolResponseNFSMountOptions(StoragePoolResponse poolResponse, Long poolId) {
        if (Storage.StoragePoolType.NetworkFilesystem.toString().equals(poolResponse.getType()) &&
                HypervisorType.KVM.toString().equals(poolResponse.getHypervisor())) {
            StoragePoolDetailVO detail = _storagePoolDetailsDao.findDetail(poolId, ApiConstants.NFS_MOUNT_OPTIONS);
            if (detail != null) {
                poolResponse.setNfsMountOpts(detail.getValue());
            }
        }
    }

    private ListResponse<StoragePoolResponse> createStoragesPoolResponse(Pair<List<StoragePoolJoinVO>, Integer> storagePools, boolean getCustomStats) {
        ListResponse<StoragePoolResponse> response = new ListResponse<>();

        List<StoragePoolResponse> poolResponses = ViewResponseHelper.createStoragePoolResponse(getCustomStats, storagePools.first().toArray(new StoragePoolJoinVO[storagePools.first().size()]));
        Map<String, Long> poolUuidToIdMap = storagePools.first().stream().collect(Collectors.toMap(StoragePoolJoinVO::getUuid, StoragePoolJoinVO::getId, (a, b) -> a));
        for (StoragePoolResponse poolResponse : poolResponses) {
            DataStore store = dataStoreManager.getPrimaryDataStore(poolResponse.getId());
            if (store != null) {
                DataStoreDriver driver = store.getDriver();
                if (driver != null && driver.getCapabilities() != null) {
                    Map<String, String> caps = driver.getCapabilities();
                    if (Storage.StoragePoolType.NetworkFilesystem.toString().equals(poolResponse.getType()) &&
                        HypervisorType.VMware.toString().equals(poolResponse.getHypervisor())) {
                        StoragePoolDetailVO detail = _storagePoolDetailsDao.findDetail(poolUuidToIdMap.get(poolResponse.getId()), Storage.Capability.HARDWARE_ACCELERATION.toString());
                        if (detail != null) {
                            caps.put(Storage.Capability.HARDWARE_ACCELERATION.toString(), detail.getValue());
                        }
                    }
                    poolResponse.setCaps(caps);
                }
            }
            setPoolResponseNFSMountOptions(poolResponse, poolUuidToIdMap.get(poolResponse.getId()));
        }

        response.setResponses(poolResponses, storagePools.second());
        return response;
    }

    private Pair<List<StoragePoolJoinVO>, Integer> searchForStoragePoolsInternal(ListStoragePoolsCmd cmd) {
        ScopeType scopeType = ScopeType.validateAndGetScopeType(cmd.getScope());
        StoragePoolStatus status = StoragePoolStatus.validateAndGetStatus(cmd.getStatus());

        Long zoneId = accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), cmd.getZoneId());
        Long id = cmd.getId();
        String name = cmd.getStoragePoolName();
        String path = cmd.getPath();
        Long pod = cmd.getPodId();
        Long cluster = cmd.getClusterId();
        String address = cmd.getIpAddress();
        String keyword = cmd.getKeyword();

        Long startIndex = cmd.getStartIndex();
        Long pageSize = cmd.getPageSizeVal();

        Filter searchFilter = new Filter(StoragePoolVO.class, "id", Boolean.TRUE, startIndex, pageSize);

        Pair<List<Long>, Integer> uniquePoolPair = storagePoolDao.searchForIdsAndCount(id, name, zoneId, path, pod,
                cluster, address, scopeType, status, keyword, searchFilter);

        List<StoragePoolJoinVO> storagePools = _poolJoinDao.searchByIds(uniquePoolPair.first().toArray(new Long[0]));

        return new Pair<>(storagePools, uniquePoolPair.second());
    }

    @Override
    public ListResponse<StorageTagResponse> searchForStorageTags(ListStorageTagsCmd cmd) {
        Pair<List<StoragePoolTagVO>, Integer> result = searchForStorageTagsInternal(cmd);
        ListResponse<StorageTagResponse> response = new ListResponse<>();
        List<StorageTagResponse> tagResponses = ViewResponseHelper.createStorageTagResponse(result.first().toArray(new StoragePoolTagVO[result.first().size()]));

        response.setResponses(tagResponses, result.second());

        return response;
    }

    private Pair<List<StoragePoolTagVO>, Integer> searchForStorageTagsInternal(ListStorageTagsCmd cmd) {
        Filter searchFilter = new Filter(StoragePoolTagVO.class, "id", Boolean.TRUE, null, null);

        SearchBuilder<StoragePoolTagVO> sb = _storageTagDao.createSearchBuilder();

        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct

        SearchCriteria<StoragePoolTagVO> sc = sb.create();

        // search storage tag details by ids
        Pair<List<StoragePoolTagVO>, Integer> uniqueTagPair = _storageTagDao.searchAndCount(sc, searchFilter);
        Integer count = uniqueTagPair.second();

        if (count.intValue() == 0) {
            return uniqueTagPair;
        }

        List<StoragePoolTagVO> uniqueTags = uniqueTagPair.first();
        Long[] vrIds = new Long[uniqueTags.size()];
        int i = 0;

        for (StoragePoolTagVO v : uniqueTags) {
            vrIds[i++] = v.getId();
        }

        List<StoragePoolTagVO> vrs = _storageTagDao.searchByIds(vrIds);

        return new Pair<>(vrs, count);
    }

    @Override
    public ListResponse<HostTagResponse> searchForHostTags(ListHostTagsCmd cmd) {
        Pair<List<HostTagVO>, Integer> result = searchForHostTagsInternal(cmd);
        ListResponse<HostTagResponse> response = new ListResponse<>();
        List<HostTagResponse> tagResponses = ViewResponseHelper.createHostTagResponse(result.first().toArray(new HostTagVO[result.first().size()]));

        response.setResponses(tagResponses, result.second());

        return response;
    }

    private Pair<List<HostTagVO>, Integer> searchForHostTagsInternal(ListHostTagsCmd cmd) {
        Filter searchFilter = new Filter(HostTagVO.class, "id", Boolean.TRUE, null, null);

        SearchBuilder<HostTagVO> sb = _hostTagDao.createSearchBuilder();

        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct

        SearchCriteria<HostTagVO> sc = sb.create();

        // search host tag details by ids
        Pair<List<HostTagVO>, Integer> uniqueTagPair = _hostTagDao.searchAndCount(sc, searchFilter);
        Integer count = uniqueTagPair.second();

        if (count.intValue() == 0) {
            return uniqueTagPair;
        }

        List<HostTagVO> uniqueTags = uniqueTagPair.first();
        Long[] vrIds = new Long[uniqueTags.size()];
        int i = 0;

        for (HostTagVO v : uniqueTags) {
            vrIds[i++] = v.getId();
        }

        List<HostTagVO> vrs = _hostTagDao.searchByIds(vrIds);

        return new Pair<>(vrs, count);
    }

    @Override
    public ListResponse<ImageStoreResponse> searchForImageStores(ListImageStoresCmd cmd) {
        Pair<List<ImageStoreJoinVO>, Integer> result = searchForImageStoresInternal(cmd);
        ListResponse<ImageStoreResponse> response = new ListResponse<>();

        List<ImageStoreResponse> poolResponses = ViewResponseHelper.createImageStoreResponse(result.first().toArray(new ImageStoreJoinVO[result.first().size()]));
        response.setResponses(poolResponses, result.second());
        return response;
    }

    private Pair<List<ImageStoreJoinVO>, Integer> searchForImageStoresInternal(ListImageStoresCmd cmd) {

        Long zoneId = accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), cmd.getZoneId());
        Object id = cmd.getId();
        Object name = cmd.getStoreName();
        String provider = cmd.getProvider();
        String protocol = cmd.getProtocol();
        Object keyword = cmd.getKeyword();
        Long startIndex = cmd.getStartIndex();
        Long pageSize = cmd.getPageSizeVal();
        Boolean readonly = cmd.getReadonly();

        Filter searchFilter = new Filter(ImageStoreJoinVO.class, "id", Boolean.TRUE, startIndex, pageSize);

        SearchBuilder<ImageStoreJoinVO> sb = _imageStoreJoinDao.createSearchBuilder();
        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct
        // ids
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getZoneId(), SearchCriteria.Op.EQ);
        sb.and("protocol", sb.entity().getProtocol(), SearchCriteria.Op.EQ);
        sb.and("provider", sb.entity().getProviderName(), SearchCriteria.Op.EQ);
        sb.and("role", sb.entity().getRole(), SearchCriteria.Op.EQ);
        sb.and("readonly", sb.entity().isReadonly(), Op.EQ);

        SearchCriteria<ImageStoreJoinVO> sc = sb.create();
        sc.setParameters("role", DataStoreRole.Image);

        if (keyword != null) {
            SearchCriteria<ImageStoreJoinVO> ssc = _imageStoreJoinDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("providerName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (name != null) {
            sc.setParameters("name", name);
        }

        if (zoneId != null) {
            sc.setParameters("dataCenterId", zoneId);
        }
        if (provider != null) {
            sc.setParameters("provider", provider);
        }
        if (protocol != null) {
            sc.setParameters("protocol", protocol);
        }
        if (readonly != null) {
            sc.setParameters("readonly", readonly);
        }

        // search Store details by ids
        Pair<List<ImageStoreJoinVO>, Integer> uniqueStorePair = _imageStoreJoinDao.searchAndCount(sc, searchFilter);
        Integer count = uniqueStorePair.second();
        if (count.intValue() == 0) {
            // empty result
            return uniqueStorePair;
        }
        List<ImageStoreJoinVO> uniqueStores = uniqueStorePair.first();
        Long[] vrIds = new Long[uniqueStores.size()];
        int i = 0;
        for (ImageStoreJoinVO v : uniqueStores) {
            vrIds[i++] = v.getId();
        }
        List<ImageStoreJoinVO> vrs = _imageStoreJoinDao.searchByIds(vrIds);
        return new Pair<>(vrs, count);

    }

    @Override
    public ListResponse<ImageStoreResponse> searchForSecondaryStagingStores(ListSecondaryStagingStoresCmd cmd) {
        Pair<List<ImageStoreJoinVO>, Integer> result = searchForCacheStoresInternal(cmd);
        ListResponse<ImageStoreResponse> response = new ListResponse<>();

        List<ImageStoreResponse> poolResponses = ViewResponseHelper.createImageStoreResponse(result.first().toArray(new ImageStoreJoinVO[result.first().size()]));
        response.setResponses(poolResponses, result.second());
        return response;
    }

    private Pair<List<ImageStoreJoinVO>, Integer> searchForCacheStoresInternal(ListSecondaryStagingStoresCmd cmd) {

        Long zoneId = accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), cmd.getZoneId());
        Object id = cmd.getId();
        Object name = cmd.getStoreName();
        String provider = cmd.getProvider();
        String protocol = cmd.getProtocol();
        Object keyword = cmd.getKeyword();
        Long startIndex = cmd.getStartIndex();
        Long pageSize = cmd.getPageSizeVal();

        Filter searchFilter = new Filter(ImageStoreJoinVO.class, "id", Boolean.TRUE, startIndex, pageSize);

        SearchBuilder<ImageStoreJoinVO> sb = _imageStoreJoinDao.createSearchBuilder();
        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct
        // ids
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getZoneId(), SearchCriteria.Op.EQ);
        sb.and("protocol", sb.entity().getProtocol(), SearchCriteria.Op.EQ);
        sb.and("provider", sb.entity().getProviderName(), SearchCriteria.Op.EQ);
        sb.and("role", sb.entity().getRole(), SearchCriteria.Op.EQ);

        SearchCriteria<ImageStoreJoinVO> sc = sb.create();
        sc.setParameters("role", DataStoreRole.ImageCache);

        if (keyword != null) {
            SearchCriteria<ImageStoreJoinVO> ssc = _imageStoreJoinDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("provider", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (name != null) {
            sc.setParameters("name", name);
        }

        if (zoneId != null) {
            sc.setParameters("dataCenterId", zoneId);
        }
        if (provider != null) {
            sc.setParameters("provider", provider);
        }
        if (protocol != null) {
            sc.setParameters("protocol", protocol);
        }

        // search Store details by ids
        Pair<List<ImageStoreJoinVO>, Integer> uniqueStorePair = _imageStoreJoinDao.searchAndCount(sc, searchFilter);
        Integer count = uniqueStorePair.second();
        if (count.intValue() == 0) {
            // empty result
            return uniqueStorePair;
        }
        List<ImageStoreJoinVO> uniqueStores = uniqueStorePair.first();
        Long[] vrIds = new Long[uniqueStores.size()];
        int i = 0;
        for (ImageStoreJoinVO v : uniqueStores) {
            vrIds[i++] = v.getId();
        }
        List<ImageStoreJoinVO> vrs = _imageStoreJoinDao.searchByIds(vrIds);
        return new Pair<>(vrs, count);

    }

    @Override
    public ListResponse<DiskOfferingResponse> searchForDiskOfferings(ListDiskOfferingsCmd cmd) {
        Pair<List<DiskOfferingJoinVO>, Integer> result = searchForDiskOfferingsInternal(cmd);
        ListResponse<DiskOfferingResponse> response = new ListResponse<>();
        List<DiskOfferingResponse> offeringResponses = ViewResponseHelper.createDiskOfferingResponses(cmd.getVirtualMachineId(), result.first());
        response.setResponses(offeringResponses, result.second());
        return response;
    }

    private Pair<List<DiskOfferingJoinVO>, Integer> searchForDiskOfferingsInternal(ListDiskOfferingsCmd cmd) {
        Ternary<List<Long>, Integer, String[]> diskOfferingIdPage = searchForDiskOfferingsIdsAndCount(cmd);

        Integer count = diskOfferingIdPage.second();
        Long[] idArray = diskOfferingIdPage.first().toArray(new Long[0]);
        String[] requiredTagsArray = diskOfferingIdPage.third();

        if (count == 0) {
            return new Pair<>(new ArrayList<>(), count);
        }

        List<DiskOfferingJoinVO> diskOfferings = _diskOfferingJoinDao.searchByIds(idArray);

        if (requiredTagsArray.length != 0) {
            ListIterator<DiskOfferingJoinVO> iteratorForTagsChecking = diskOfferings.listIterator();
            while (iteratorForTagsChecking.hasNext()) {
                DiskOfferingJoinVO offering = iteratorForTagsChecking.next();
                String offeringTags = offering.getTags();
                String[] offeringTagsArray = (offeringTags == null || offeringTags.isEmpty()) ? new String[0] : offeringTags.split(",");
                if (!CollectionUtils.isSubCollection(Arrays.asList(requiredTagsArray), Arrays.asList(offeringTagsArray))) {
                    iteratorForTagsChecking.remove();
                }
            }
        }
        return new Pair<>(diskOfferings, count);
    }

    private Ternary<List<Long>, Integer, String[]> searchForDiskOfferingsIdsAndCount(ListDiskOfferingsCmd cmd) {
        // Note
        // The list method for offerings is being modified in accordance with
        // discussion with Will/Kevin
        // For now, we will be listing the following based on the usertype
        // 1. For root, we will list all offerings
        // 2. For domainAdmin and regular users, we will list everything in
        // their domains+parent domains ... all the way
        // till
        // root

        Account account = CallContext.current().getCallingAccount();
        Object name = cmd.getDiskOfferingName();
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();
        Long domainId = cmd.getDomainId();
        Boolean isRootAdmin = accountMgr.isRootAdmin(account.getAccountId());
        Long projectId = cmd.getProjectId();
        String accountName = cmd.getAccountName();
        Boolean isRecursive = cmd.isRecursive();
        Long zoneId = cmd.getZoneId();
        Long volumeId = cmd.getVolumeId();
        Long storagePoolId = cmd.getStoragePoolId();
        Boolean encrypt = cmd.getEncrypt();
        String storageType = cmd.getStorageType();
        DiskOffering.State state = cmd.getState();
        final Long vmId = cmd.getVirtualMachineId();

        Filter searchFilter = new Filter(DiskOfferingVO.class, "sortKey", SortKeyAscending.value(), cmd.getStartIndex(), cmd.getPageSizeVal());
        searchFilter.addOrderBy(DiskOfferingVO.class, "id", true);
        SearchBuilder<DiskOfferingVO> diskOfferingSearch = _diskOfferingDao.createSearchBuilder();
        diskOfferingSearch.select(null, Func.DISTINCT, diskOfferingSearch.entity().getId()); // select distinct

        diskOfferingSearch.and("computeOnly", diskOfferingSearch.entity().isComputeOnly(), Op.EQ);

        if (state != null) {
            diskOfferingSearch.and("state", diskOfferingSearch.entity().getState(), Op.EQ);
        }

        // Keeping this logic consistent with domain specific zones
        // if a domainId is provided, we just return the disk offering
        // associated with this domain
        if (domainId != null && accountName == null) {
            if (accountMgr.isRootAdmin(account.getId()) || isPermissible(account.getDomainId(), domainId)) {
                // check if the user's domain == do's domain || user's domain is
                // a child of so's domain for non-root users
                SearchBuilder<DiskOfferingDetailVO> domainDetailsSearch = _diskOfferingDetailsDao.createSearchBuilder();
                domainDetailsSearch.and("domainId", domainDetailsSearch.entity().getValue(), Op.EQ);

                diskOfferingSearch.join("domainDetailsSearch", domainDetailsSearch, JoinBuilder.JoinType.LEFT, JoinBuilder.JoinCondition.AND,
                        diskOfferingSearch.entity().getId(), domainDetailsSearch.entity().getResourceId(),
                        domainDetailsSearch.entity().getName(), diskOfferingSearch.entity().setString(ApiConstants.DOMAIN_ID));

                if (!isRootAdmin) {
                    diskOfferingSearch.and("displayOffering", diskOfferingSearch.entity().getDisplayOffering(), Op.EQ);
                }

                SearchCriteria<DiskOfferingVO> sc = diskOfferingSearch.create();
                sc.setParameters("computeOnly", false);
                sc.setParameters("activeState", DiskOffering.State.Active);

                sc.setJoinParameters("domainDetailsSearch", "domainId", domainId);

                Pair<List<DiskOfferingVO>, Integer> uniquePairs = _diskOfferingDao.searchAndCount(sc, searchFilter);
                List<Long> idsArray = uniquePairs.first().stream().map(DiskOfferingVO::getId).collect(Collectors.toList());
                return new Ternary<>(idsArray, uniquePairs.second(), new String[0]);
            } else {
                throw new PermissionDeniedException("The account:" + account.getAccountName() + " does not fall in the same domain hierarchy as the disk offering");
            }
        }

        // For non-root users, only return all offerings for the user's domain,
        // and everything above till root
        if ((accountMgr.isNormalUser(account.getId()) || accountMgr.isDomainAdmin(account.getId())) || account.getType() == Account.Type.RESOURCE_DOMAIN_ADMIN) {
            if (isRecursive) { // domain + all sub-domains
                if (account.getType() == Account.Type.NORMAL) {
                    throw new InvalidParameterValueException("Only ROOT admins and Domain admins can list disk offerings with isrecursive=true");
                }
            }
        }

        if (volumeId != null && storagePoolId != null) {
            throw new InvalidParameterValueException("Both volume ID and storage pool ID are not allowed at the same time");
        }

        if (keyword != null) {
            diskOfferingSearch.and().op("keywordDisplayText", diskOfferingSearch.entity().getDisplayText(), Op.LIKE);
            diskOfferingSearch.or("keywordName", diskOfferingSearch.entity().getName(), Op.LIKE);
            diskOfferingSearch.cp();
        }

        if (id != null) {
            diskOfferingSearch.and("id", diskOfferingSearch.entity().getId(), Op.EQ);
        }

        if (name != null) {
            diskOfferingSearch.and("name", diskOfferingSearch.entity().getName(), Op.EQ);
        }

        if (encrypt != null) {
            diskOfferingSearch.and("encrypt", diskOfferingSearch.entity().getEncrypt(), Op.EQ);
        }

        if (storageType != null || zoneId != null) {
            diskOfferingSearch.and("useLocalStorage", diskOfferingSearch.entity().isUseLocalStorage(), Op.EQ);
        }

        if (zoneId != null) {
            SearchBuilder<DiskOfferingDetailVO> zoneDetailSearch = _diskOfferingDetailsDao.createSearchBuilder();
            zoneDetailSearch.and().op("zoneId", zoneDetailSearch.entity().getValue(), Op.EQ);
            zoneDetailSearch.or("zoneIdNull", zoneDetailSearch.entity().getId(), Op.NULL);
            zoneDetailSearch.cp();

            diskOfferingSearch.join("zoneDetailSearch", zoneDetailSearch, JoinBuilder.JoinType.LEFT, JoinBuilder.JoinCondition.AND,
                    diskOfferingSearch.entity().getId(), zoneDetailSearch.entity().getResourceId(),
                    zoneDetailSearch.entity().getName(), diskOfferingSearch.entity().setString(ApiConstants.ZONE_ID));
        }

        DiskOffering currentDiskOffering = null;
        Volume volume = null;
        if (volumeId != null) {
            volume = volumeDao.findById(volumeId);
            if (volume == null) {
                throw new InvalidParameterValueException(String.format("Unable to find a volume with specified id %s", volumeId));
            }
            currentDiskOffering = _diskOfferingDao.findByIdIncludingRemoved(volume.getDiskOfferingId());
            if (!currentDiskOffering.isComputeOnly() && currentDiskOffering.getDiskSizeStrictness()) {
                diskOfferingSearch.and().op("diskSize", diskOfferingSearch.entity().getDiskSize(), Op.EQ);
                diskOfferingSearch.or("customized", diskOfferingSearch.entity().isCustomized(), Op.EQ);
                diskOfferingSearch.cp();
            }
            diskOfferingSearch.and("idNEQ", diskOfferingSearch.entity().getId(), Op.NEQ);
            diskOfferingSearch.and("diskSizeStrictness", diskOfferingSearch.entity().getDiskSizeStrictness(), Op.EQ);
        }

        account = accountMgr.finalizeOwner(account, accountName, domainId, projectId);
        if (!Account.Type.ADMIN.equals(account.getType())) {
            SearchBuilder<DiskOfferingDetailVO> domainDetailsSearch = _diskOfferingDetailsDao.createSearchBuilder();
            domainDetailsSearch.and().op("domainIdIN", domainDetailsSearch.entity().getValue(), Op.IN);
            domainDetailsSearch.or("domainIdNull", domainDetailsSearch.entity().getId(), Op.NULL);
            domainDetailsSearch.cp();

            diskOfferingSearch.join("domainDetailsSearch", domainDetailsSearch, JoinBuilder.JoinType.LEFT, JoinBuilder.JoinCondition.AND,
                    diskOfferingSearch.entity().getId(), domainDetailsSearch.entity().getResourceId(),
                    domainDetailsSearch.entity().getName(), diskOfferingSearch.entity().setString(ApiConstants.DOMAIN_ID));
        }

        SearchCriteria<DiskOfferingVO> sc = diskOfferingSearch.create();

        sc.setParameters("computeOnly", false);

        if (state != null) {
            sc.setParameters("state", state);
        }

        if (keyword != null) {
            sc.setParameters("keywordDisplayText", "%" + keyword + "%");
            sc.setParameters("keywordName", "%" + keyword + "%");
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (name != null) {
            sc.setParameters("name", name);
        }

        if (encrypt != null) {
            sc.setParameters("encrypt", encrypt);
        }

        if (storageType != null) {
            if (storageType.equalsIgnoreCase(ServiceOffering.StorageType.local.toString())) {
                sc.setParameters("useLocalStorage", true);

            } else if (storageType.equalsIgnoreCase(ServiceOffering.StorageType.shared.toString())) {
                sc.setParameters("useLocalStorage", false);
            }
        }

        if (zoneId != null) {
            sc.setJoinParameters("zoneDetailSearch", "zoneId", zoneId);

            DataCenterJoinVO zone = _dcJoinDao.findById(zoneId);
            if (DataCenter.Type.Edge.equals(zone.getType())) {
                sc.setParameters("useLocalStorage", true);
            }
        }

        if (volumeId != null) {
            if (!currentDiskOffering.isComputeOnly() && currentDiskOffering.getDiskSizeStrictness()) {
                sc.setParameters("diskSize", volume.getSize());
                sc.setParameters("customized", true);
            }
            sc.setParameters("idNEQ", currentDiskOffering.getId());
            sc.setParameters("diskSizeStrictness", currentDiskOffering.getDiskSizeStrictness());
        }

        // Filter offerings that are not associated with caller's domain
        if (!Account.Type.ADMIN.equals(account.getType())) {
            Domain callerDomain = _domainDao.findById(account.getDomainId());
            List<Long> domainIds = findRelatedDomainIds(callerDomain, isRecursive);

            sc.setJoinParameters("domainDetailsSearch", "domainIdIN", domainIds.toArray());
        }

        if (vmId != null) {
            UserVmVO vm = userVmDao.findById(vmId);
            if (vm == null) {
                throw new InvalidParameterValueException("Unable to find the VM instance with the specified ID");
            }
            if (!isRootAdmin) {
                accountMgr.checkAccess(account, null, false, vm);
            }
        }

        Pair<List<DiskOfferingVO>, Integer> uniquePairs = _diskOfferingDao.searchAndCount(sc, searchFilter);
        String[] requiredTagsArray = new String[0];
        if (CollectionUtils.isNotEmpty(uniquePairs.first()) && VolumeApiServiceImpl.MatchStoragePoolTagsWithDiskOffering.valueIn(zoneId)) {
            if (volumeId != null) {
                requiredTagsArray = currentDiskOffering.getTagsArray();
            } else if (storagePoolId != null) {
                requiredTagsArray = _storageTagDao.getStoragePoolTags(storagePoolId).toArray(new String[0]);
            }
        }
        List<Long> idsArray = uniquePairs.first().stream().map(DiskOfferingVO::getId).collect(Collectors.toList());

        return new Ternary<>(idsArray, uniquePairs.second(), requiredTagsArray);
    }

    private void useStorageType(SearchCriteria<?> sc, String storageType) {
        if (storageType != null) {
            if (storageType.equalsIgnoreCase(ServiceOffering.StorageType.local.toString())) {
                sc.addAnd("useLocalStorage", Op.EQ, true);

            } else if (storageType.equalsIgnoreCase(ServiceOffering.StorageType.shared.toString())) {
                sc.addAnd("useLocalStorage", Op.EQ, false);
            }
        }
    }

    private List<Long> findRelatedDomainIds(Domain domain, boolean isRecursive) {
        List<Long> domainIds = _domainDao.getDomainParentIds(domain.getId())
            .stream().collect(Collectors.toList());
        if (isRecursive) {
            List<Long> childrenIds = _domainDao.getDomainChildrenIds(domain.getPath());
            if (childrenIds != null && !childrenIds.isEmpty())
            domainIds.addAll(childrenIds);
        }
        return domainIds;
    }

    @Override
    public ListResponse<ServiceOfferingResponse> searchForServiceOfferings(ListServiceOfferingsCmd cmd) {
        Pair<List<ServiceOfferingJoinVO>, Integer> result = searchForServiceOfferingsInternal(cmd);
        result.first();
        ListResponse<ServiceOfferingResponse> response = new ListResponse<>();
        List<ServiceOfferingResponse> offeringResponses = ViewResponseHelper.createServiceOfferingResponse(result.first().toArray(new ServiceOfferingJoinVO[result.first().size()]));
        response.setResponses(offeringResponses, result.second());
        return response;
    }

    protected List<String> getHostTagsFromTemplateForServiceOfferingsListing(Account caller, Long templateId) {
        List<String> hostTags = new ArrayList<>();
        if (templateId == null) {
            return hostTags;
        }
        VMTemplateVO template = _templateDao.findByIdIncludingRemoved(templateId);
        if (template == null) {
            throw new InvalidParameterValueException("Unable to find template with the specified ID");
        }
        if (caller.getType() != Account.Type.ADMIN) {
            accountMgr.checkAccess(caller, null, false, template);
        }
        if (StringUtils.isNotEmpty(template.getTemplateTag())) {
            hostTags.add(template.getTemplateTag());
        }
        return hostTags;
    }

    private Pair<List<ServiceOfferingJoinVO>, Integer> searchForServiceOfferingsInternal(ListServiceOfferingsCmd cmd) {
        Pair<List<Long>, Integer> offeringIdPage = searchForServiceOfferingIdsAndCount(cmd);

        Integer count = offeringIdPage.second();
        Long[] idArray = offeringIdPage.first().toArray(new Long[0]);

        if (count == 0) {
            return new Pair<>(new ArrayList<>(), count);
        }

        List<ServiceOfferingJoinVO> srvOfferings = _srvOfferingJoinDao.searchByIds(idArray);
        return new Pair<>(srvOfferings, count);
    }

    private Pair<List<Long>, Integer> searchForServiceOfferingIdsAndCount(ListServiceOfferingsCmd cmd) {
        // Note
        // The filteredOfferings method for offerings is being modified in accordance with
        // discussion with Will/Kevin
        // For now, we will be listing the following based on the usertype
        // 1. For root, we will filteredOfferings all offerings
        // 2. For domainAdmin and regular users, we will filteredOfferings everything in
        // their domains+parent domains ... all the way
        // till
        // root
        Account caller = CallContext.current().getCallingAccount();
        Long projectId = cmd.getProjectId();
        String accountName = cmd.getAccountName();
        Object name = cmd.getServiceOfferingName();
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();
        Long vmId = cmd.getVirtualMachineId();
        Long domainId = cmd.getDomainId();
        Boolean isSystem = cmd.getIsSystem();
        String vmTypeStr = cmd.getSystemVmType();
        ServiceOfferingVO currentVmOffering = null;
        DiskOfferingVO diskOffering = null;
        Boolean isRecursive = cmd.isRecursive();
        Long zoneId = cmd.getZoneId();
        Integer cpuNumber = cmd.getCpuNumber();
        Integer memory = cmd.getMemory();
        Integer cpuSpeed = cmd.getCpuSpeed();
        Boolean encryptRoot = cmd.getEncryptRoot();
        String storageType = cmd.getStorageType();
        ServiceOffering.State state = cmd.getState();
        final Long templateId = cmd.getTemplateId();

        final Account owner = accountMgr.finalizeOwner(caller, accountName, domainId, projectId);

        if (!accountMgr.isRootAdmin(caller.getId()) && isSystem) {
            throw new InvalidParameterValueException("Only ROOT admins can access system offerings.");
        }

        // Keeping this logic consistent with domain specific zones
        // if a domainId is provided, we just return the so associated with this
        // domain
        if (domainId != null && !accountMgr.isRootAdmin(caller.getId())) {
            // check if the user's domain == so's domain || user's domain is a
            // child of so's domain
            if (!isPermissible(owner.getDomainId(), domainId)) {
                throw new PermissionDeniedException("The account:" + owner.getAccountName() + " does not fall in the same domain hierarchy as the service offering");
            }
        }

        VMInstanceVO vmInstance = null;
        if (vmId != null) {
            vmInstance = _vmInstanceDao.findById(vmId);
            if ((vmInstance == null) || (vmInstance.getRemoved() != null)) {
                InvalidParameterValueException ex = new InvalidParameterValueException("unable to find a virtual machine with specified id");
                ex.addProxyObject(vmId.toString(), "vmId");
                throw ex;
            }
            accountMgr.checkAccess(owner, null, true, vmInstance);
        }

        Filter searchFilter = new Filter(ServiceOfferingVO.class, "sortKey", SortKeyAscending.value(), cmd.getStartIndex(), cmd.getPageSizeVal());
        searchFilter.addOrderBy(ServiceOfferingVO.class, "id", true);

        SearchBuilder<ServiceOfferingVO> serviceOfferingSearch = _srvOfferingDao.createSearchBuilder();
        serviceOfferingSearch.select(null, Func.DISTINCT, serviceOfferingSearch.entity().getId()); // select distinct

        if (state != null) {
            serviceOfferingSearch.and("state", serviceOfferingSearch.entity().getState(), Op.EQ);
        }

        if (vmId != null) {
            currentVmOffering = _srvOfferingDao.findByIdIncludingRemoved(vmInstance.getId(), vmInstance.getServiceOfferingId());
            diskOffering = _diskOfferingDao.findByIdIncludingRemoved(currentVmOffering.getDiskOfferingId());
            if (!currentVmOffering.isDynamic()) {
                serviceOfferingSearch.and("idNEQ", serviceOfferingSearch.entity().getId(), SearchCriteria.Op.NEQ);
            }

            if (currentVmOffering.getDiskOfferingStrictness()) {
                serviceOfferingSearch.and("diskOfferingId", serviceOfferingSearch.entity().getDiskOfferingId(), SearchCriteria.Op.EQ);
            }
            serviceOfferingSearch.and("diskOfferingStrictness", serviceOfferingSearch.entity().getDiskOfferingStrictness(), SearchCriteria.Op.EQ);

            // In case vm is running return only offerings greater than equal to current offering compute and offering's dynamic scalability should match
            if (vmInstance.getState() == VirtualMachine.State.Running) {
                Integer vmCpu = currentVmOffering.getCpu();
                Integer vmMemory = currentVmOffering.getRamSize();
                Integer vmSpeed = currentVmOffering.getSpeed();
                if ((vmCpu == null || vmMemory == null || vmSpeed == null) && VirtualMachine.Type.User.equals(vmInstance.getType())) {
                    UserVmVO userVmVO = userVmDao.findById(vmId);
                    userVmDao.loadDetails(userVmVO);
                    Map<String, String> details = userVmVO.getDetails();
                    vmCpu = NumbersUtil.parseInt(details.get(ApiConstants.CPU_NUMBER), 0);
                    if (vmSpeed == null) {
                        vmSpeed = NumbersUtil.parseInt(details.get(ApiConstants.CPU_SPEED), 0);
                    }
                    vmMemory = NumbersUtil.parseInt(details.get(ApiConstants.MEMORY), 0);
                }
                if (vmCpu != null && vmCpu > 0) {
                    /*
                            (service_offering.cpu >= ?)
                             OR (
                                service_offering.cpu IS NULL
                                AND (maxComputeDetailsSearch.value IS NULL OR  maxComputeDetailsSearch.value >= ?)
                            )
                     */
                    SearchBuilder<ServiceOfferingDetailsVO> maxComputeDetailsSearch = _srvOfferingDetailsDao.createSearchBuilder();

                    serviceOfferingSearch.join("maxComputeDetailsSearch", maxComputeDetailsSearch, JoinBuilder.JoinType.LEFT, JoinBuilder.JoinCondition.AND,
                            serviceOfferingSearch.entity().getId(), maxComputeDetailsSearch.entity().getResourceId(),
                            maxComputeDetailsSearch.entity().getName(), serviceOfferingSearch.entity().setString(ApiConstants.MAX_CPU_NUMBER));

                    serviceOfferingSearch.and().op("vmCpu", serviceOfferingSearch.entity().getCpu(), Op.GTEQ);
                    serviceOfferingSearch.or().op("vmCpuNull", serviceOfferingSearch.entity().getCpu(), Op.NULL);
                    serviceOfferingSearch.and().op("maxComputeDetailsSearch", "vmMaxComputeNull", maxComputeDetailsSearch.entity().getValue(), Op.NULL);
                    serviceOfferingSearch.or("maxComputeDetailsSearch", "vmMaxComputeGTEQ", maxComputeDetailsSearch.entity().getValue(), Op.GTEQ).cp();

                    serviceOfferingSearch.cp().cp();

                }
                if (vmSpeed != null && vmSpeed > 0) {
                    serviceOfferingSearch.and().op("speedNULL", serviceOfferingSearch.entity().getSpeed(), Op.NULL);
                    serviceOfferingSearch.or("speedGTEQ", serviceOfferingSearch.entity().getSpeed(), Op.GTEQ);
                    serviceOfferingSearch.cp();
                }
                if (vmMemory != null && vmMemory > 0) {
                    /*
                        (service_offering.ram_size >= ?)
                        OR (
                          service_offering.ram_size IS NULL
                          AND (max_memory_details.value IS NULL OR max_memory_details.value >= ?)
                        )
                     */
                    SearchBuilder<ServiceOfferingDetailsVO> maxMemoryDetailsSearch = _srvOfferingDetailsDao.createSearchBuilder();

                    serviceOfferingSearch.join("maxMemoryDetailsSearch", maxMemoryDetailsSearch, JoinBuilder.JoinType.LEFT, JoinBuilder.JoinCondition.AND,
                            serviceOfferingSearch.entity().getId(), maxMemoryDetailsSearch.entity().getResourceId(),
                            maxMemoryDetailsSearch.entity().getName(), serviceOfferingSearch.entity().setString("maxmemory"));

                    serviceOfferingSearch.and().op("vmMemory", serviceOfferingSearch.entity().getRamSize(), Op.GTEQ);
                    serviceOfferingSearch.or().op("vmMemoryNull", serviceOfferingSearch.entity().getRamSize(), Op.NULL);
                    serviceOfferingSearch.and().op("maxMemoryDetailsSearch", "vmMaxMemoryNull", maxMemoryDetailsSearch.entity().getValue(), Op.NULL);
                    serviceOfferingSearch.or("maxMemoryDetailsSearch", "vmMaxMemoryGTEQ", maxMemoryDetailsSearch.entity().getValue(), Op.GTEQ).cp();

                    serviceOfferingSearch.cp().cp();
                }
                serviceOfferingSearch.and("dynamicScalingEnabled", serviceOfferingSearch.entity().isDynamicScalingEnabled(), SearchCriteria.Op.EQ);
            }
        }

        if ((accountMgr.isNormalUser(owner.getId()) || accountMgr.isDomainAdmin(owner.getId())) || owner.getType() == Account.Type.RESOURCE_DOMAIN_ADMIN) {
            // For non-root users.
            if (isSystem) {
                throw new InvalidParameterValueException("Only root admins can access system's offering");
            }
            if (isRecursive) { // domain + all sub-domains
                if (owner.getType() == Account.Type.NORMAL) {
                    throw new InvalidParameterValueException("Only ROOT admins and Domain admins can list service offerings with isrecursive=true");
                }
            }
        } else {
            // for root users
            if (owner.getDomainId() != 1 && isSystem) { // NON ROOT admin
                throw new InvalidParameterValueException("Non ROOT admins cannot access system's offering");
            }
            if (domainId != null && accountName == null) {
                SearchBuilder<ServiceOfferingDetailsVO> srvOffrDomainDetailSearch = _srvOfferingDetailsDao.createSearchBuilder();
                srvOffrDomainDetailSearch.and("domainId", srvOffrDomainDetailSearch.entity().getValue(), Op.EQ);
                serviceOfferingSearch.join("domainDetailSearch", srvOffrDomainDetailSearch, JoinBuilder.JoinType.LEFT, JoinBuilder.JoinCondition.AND,
                        serviceOfferingSearch.entity().getId(), srvOffrDomainDetailSearch.entity().getResourceId(),
                        srvOffrDomainDetailSearch.entity().getName(), serviceOfferingSearch.entity().setString(ApiConstants.DOMAIN_ID));
            }
        }

        if (keyword != null) {
            serviceOfferingSearch.and().op("keywordName", serviceOfferingSearch.entity().getName(), SearchCriteria.Op.LIKE);
            serviceOfferingSearch.or("keywordDisplayText", serviceOfferingSearch.entity().getDisplayText(), SearchCriteria.Op.LIKE);
            serviceOfferingSearch.cp();
        }

        if (id != null) {
            serviceOfferingSearch.and("id", serviceOfferingSearch.entity().getId(), SearchCriteria.Op.EQ);
        }

        if (isSystem != null) {
            // note that for non-root users, isSystem is always false when
            // control comes to here
            serviceOfferingSearch.and("systemUse", serviceOfferingSearch.entity().isSystemUse(), SearchCriteria.Op.EQ);
        }

        if (name != null) {
            serviceOfferingSearch.and("name", serviceOfferingSearch.entity().getName(), SearchCriteria.Op.EQ);
        }

        if (vmTypeStr != null) {
            serviceOfferingSearch.and("svmType", serviceOfferingSearch.entity().getVmType(), SearchCriteria.Op.EQ);
        }
        DataCenterJoinVO zone = null;
        if (zoneId != null) {
            SearchBuilder<ServiceOfferingDetailsVO> srvOffrZoneDetailSearch = _srvOfferingDetailsDao.createSearchBuilder();
            srvOffrZoneDetailSearch.and().op("zoneId", srvOffrZoneDetailSearch.entity().getValue(), Op.EQ);
            srvOffrZoneDetailSearch.or("idNull", srvOffrZoneDetailSearch.entity().getId(), Op.NULL);
            srvOffrZoneDetailSearch.cp();

            serviceOfferingSearch.join("ZoneDetailSearch", srvOffrZoneDetailSearch, JoinBuilder.JoinType.LEFT, JoinBuilder.JoinCondition.AND,
                    serviceOfferingSearch.entity().getId(), srvOffrZoneDetailSearch.entity().getResourceId(),
                    srvOffrZoneDetailSearch.entity().getName(), serviceOfferingSearch.entity().setString(ApiConstants.ZONE_ID));
            zone = _dcJoinDao.findById(zoneId);
        }

        if (encryptRoot != null || vmId != null || (zone != null && DataCenter.Type.Edge.equals(zone.getType()))) {
            SearchBuilder<DiskOfferingVO> diskOfferingSearch = _diskOfferingDao.createSearchBuilder();
            diskOfferingSearch.and("useLocalStorage", diskOfferingSearch.entity().isUseLocalStorage(), SearchCriteria.Op.EQ);
            diskOfferingSearch.and("encrypt", diskOfferingSearch.entity().getEncrypt(), SearchCriteria.Op.EQ);

            if (diskOffering != null) {
                List<String> storageTags = com.cloud.utils.StringUtils.csvTagsToList(diskOffering.getTags());
                if (!storageTags.isEmpty() && VolumeApiServiceImpl.MatchStoragePoolTagsWithDiskOffering.value()) {
                    for (String tag : storageTags) {
                        diskOfferingSearch.and("storageTag" + tag, diskOfferingSearch.entity().getTags(), Op.FIND_IN_SET);
                    }
                }
            }

            serviceOfferingSearch.join("diskOfferingSearch", diskOfferingSearch, JoinBuilder.JoinType.INNER, JoinBuilder.JoinCondition.AND,
                    serviceOfferingSearch.entity().getDiskOfferingId(), diskOfferingSearch.entity().getId(),
                    serviceOfferingSearch.entity().setString("Active"), diskOfferingSearch.entity().getState());
        }

        if (cpuNumber != null) {
            SearchBuilder<ServiceOfferingDetailsVO> maxComputeDetailsSearch = (SearchBuilder<ServiceOfferingDetailsVO>) serviceOfferingSearch.getJoinSB("maxComputeDetailsSearch");
            if (maxComputeDetailsSearch == null) {
                maxComputeDetailsSearch = _srvOfferingDetailsDao.createSearchBuilder();
                serviceOfferingSearch.join("maxComputeDetailsSearch", maxComputeDetailsSearch, JoinBuilder.JoinType.LEFT, JoinBuilder.JoinCondition.AND,
                        serviceOfferingSearch.entity().getId(), maxComputeDetailsSearch.entity().getResourceId(),
                        maxComputeDetailsSearch.entity().getName(), serviceOfferingSearch.entity().setString(ApiConstants.MAX_CPU_NUMBER));
            }

            SearchBuilder<ServiceOfferingDetailsVO> minComputeDetailsSearch = _srvOfferingDetailsDao.createSearchBuilder();

            serviceOfferingSearch.join("minComputeDetailsSearch", minComputeDetailsSearch, JoinBuilder.JoinType.LEFT, JoinBuilder.JoinCondition.AND,
                    serviceOfferingSearch.entity().getId(), minComputeDetailsSearch.entity().getResourceId(),
                    minComputeDetailsSearch.entity().getName(), serviceOfferingSearch.entity().setString(ApiConstants.MIN_CPU_NUMBER));

            /*
                (min_cpu IS NULL AND cpu IS NULL AND max_cpu IS NULL)
                OR (cpu = X)
                OR (min_cpu <= X AND max_cpu >= X)

                AND (
                    (min_compute_details.value is NULL AND cpu is NULL)
                    OR (min_compute_details.value is NULL AND cpu >= X)
                    OR min_compute_details.value >= X
                    OR (
                        ((min_compute_details.value is NULL AND cpu <= X) OR min_compute_details.value <= X)
                        AND ((max_compute_details.value is NULL AND cpu >= X) OR max_compute_details.value >= X)
                    )
                )
             */
            serviceOfferingSearch.and().op().op("minComputeDetailsSearch", "cpuConstraintMinComputeNull", minComputeDetailsSearch.entity().getValue(), Op.NULL);
            serviceOfferingSearch.and("cpuConstraintNull", serviceOfferingSearch.entity().getCpu(), Op.NULL).cp();

            serviceOfferingSearch.or().op("minComputeDetailsSearch", "cpuConstraintMinComputeNull", minComputeDetailsSearch.entity().getValue(), Op.NULL);
            serviceOfferingSearch.and("cpuNumber", serviceOfferingSearch.entity().getCpu(), Op.GTEQ).cp();
            serviceOfferingSearch.or("cpuNumber", serviceOfferingSearch.entity().getCpu(), Op.GTEQ);

            serviceOfferingSearch.or().op().op();
            serviceOfferingSearch.op("minComputeDetailsSearch", "cpuConstraintMinComputeNull", minComputeDetailsSearch.entity().getValue(), Op.NULL);
            serviceOfferingSearch.and("cpuNumber", serviceOfferingSearch.entity().getCpu(), Op.LTEQ).cp();
            serviceOfferingSearch.or("minComputeDetailsSearch", "cpuNumber", minComputeDetailsSearch.entity().getValue(), Op.LTEQ).cp();
            serviceOfferingSearch.and().op().op("maxComputeDetailsSearch", "cpuConstraintMaxComputeNull", maxComputeDetailsSearch.entity().getValue(), Op.NULL);
            serviceOfferingSearch.and("cpuNumber", serviceOfferingSearch.entity().getCpu(), Op.GTEQ).cp();
            serviceOfferingSearch.or("maxComputeDetailsSearch", "cpuNumber", maxComputeDetailsSearch.entity().getValue(), Op.GTEQ).cp();
            serviceOfferingSearch.cp().cp();
        }

        if (memory != null) {
            SearchBuilder<ServiceOfferingDetailsVO> maxMemoryDetailsSearch = (SearchBuilder<ServiceOfferingDetailsVO>) serviceOfferingSearch.getJoinSB("maxMemoryDetailsSearch");
            if (maxMemoryDetailsSearch == null) {
                maxMemoryDetailsSearch = _srvOfferingDetailsDao.createSearchBuilder();
                serviceOfferingSearch.join("maxMemoryDetailsSearch", maxMemoryDetailsSearch, JoinBuilder.JoinType.LEFT, JoinBuilder.JoinCondition.AND,
                        serviceOfferingSearch.entity().getId(), maxMemoryDetailsSearch.entity().getResourceId(),
                        maxMemoryDetailsSearch.entity().getName(), serviceOfferingSearch.entity().setString("maxmemory"));
            }

            SearchBuilder<ServiceOfferingDetailsVO> minMemoryDetailsSearch = _srvOfferingDetailsDao.createSearchBuilder();

            serviceOfferingSearch.join("minMemoryDetailsSearch", minMemoryDetailsSearch, JoinBuilder.JoinType.LEFT, JoinBuilder.JoinCondition.AND,
                    serviceOfferingSearch.entity().getId(), minMemoryDetailsSearch.entity().getResourceId(),
                    minMemoryDetailsSearch.entity().getName(), serviceOfferingSearch.entity().setString("minmemory"));

            /*
                (min_ram_size IS NULL AND ram_size IS NULL AND max_ram_size IS NULL)
                OR (ram_size = X)
                OR (min_ram_size <= X AND max_ram_size >= X)
             */

            serviceOfferingSearch.and().op().op("minMemoryDetailsSearch", "memoryConstraintMinMemoryNull", minMemoryDetailsSearch.entity().getValue(), Op.NULL);
            serviceOfferingSearch.and("memoryConstraintNull", serviceOfferingSearch.entity().getRamSize(), Op.NULL).cp();

            serviceOfferingSearch.or().op("minMemoryDetailsSearch", "memoryConstraintMinMemoryNull", minMemoryDetailsSearch.entity().getValue(), Op.NULL);
            serviceOfferingSearch.and("memory", serviceOfferingSearch.entity().getRamSize(), Op.GTEQ).cp();
            serviceOfferingSearch.or("memory", serviceOfferingSearch.entity().getRamSize(), Op.GTEQ);

            serviceOfferingSearch.or().op().op();
            serviceOfferingSearch.op("minMemoryDetailsSearch", "memoryConstraintMinMemoryNull", minMemoryDetailsSearch.entity().getValue(), Op.NULL);
            serviceOfferingSearch.and("memory", serviceOfferingSearch.entity().getRamSize(), Op.LTEQ).cp();
            serviceOfferingSearch.or("minMemoryDetailsSearch", "memory", minMemoryDetailsSearch.entity().getValue(), Op.LTEQ).cp();
            serviceOfferingSearch.and().op().op("maxMemoryDetailsSearch", "memoryConstraintMaxMemoryNull", maxMemoryDetailsSearch.entity().getValue(), Op.NULL);
            serviceOfferingSearch.and("memory", serviceOfferingSearch.entity().getRamSize(), Op.GTEQ).cp();
            serviceOfferingSearch.or("maxMemoryDetailsSearch", "memory", maxMemoryDetailsSearch.entity().getValue(), Op.GTEQ).cp();
            serviceOfferingSearch.cp().cp();
        }

        if (cpuSpeed != null) {
            serviceOfferingSearch.and().op("speedNull", serviceOfferingSearch.entity().getSpeed(), Op.NULL);
            serviceOfferingSearch.or("speedGTEQ", serviceOfferingSearch.entity().getSpeed(), Op.GTEQ);
            serviceOfferingSearch.cp();
        }

        // Filter offerings that are not associated with caller's domain
        // Fetch the offering ids from the details table since theres no smart way to filter them in the join ... yet!
        if (owner.getType() != Account.Type.ADMIN) {
            SearchBuilder<ServiceOfferingDetailsVO> srvOffrDomainDetailSearch = _srvOfferingDetailsDao.createSearchBuilder();
            srvOffrDomainDetailSearch.and().op("domainIdIN", srvOffrDomainDetailSearch.entity().getValue(), Op.IN);
            srvOffrDomainDetailSearch.or("idNull", srvOffrDomainDetailSearch.entity().getValue(), Op.NULL);
            srvOffrDomainDetailSearch.cp();
            serviceOfferingSearch.join("domainDetailSearchNormalUser", srvOffrDomainDetailSearch, JoinBuilder.JoinType.LEFT, JoinBuilder.JoinCondition.AND,
                    serviceOfferingSearch.entity().getId(), srvOffrDomainDetailSearch.entity().getResourceId(),
                    srvOffrDomainDetailSearch.entity().getName(), serviceOfferingSearch.entity().setString(ApiConstants.DOMAIN_ID));
        }

        List<String> hostTags = new ArrayList<>();
        if (currentVmOffering != null) {
            hostTags.addAll(com.cloud.utils.StringUtils.csvTagsToList(currentVmOffering.getHostTag()));
        }

        if (!hostTags.isEmpty()) {
            serviceOfferingSearch.and().op("hostTag", serviceOfferingSearch.entity().getHostTag(), Op.NULL);
            serviceOfferingSearch.or();
            boolean flag = true;
            for(String tag : hostTags) {
                if (flag) {
                    flag = false;
                    serviceOfferingSearch.op("hostTag" + tag, serviceOfferingSearch.entity().getHostTag(), Op.FIND_IN_SET);
                } else {
                    serviceOfferingSearch.and("hostTag" + tag, serviceOfferingSearch.entity().getHostTag(), Op.FIND_IN_SET);
                }
            }
            serviceOfferingSearch.cp().cp();
        }

        SearchCriteria<ServiceOfferingVO> sc = serviceOfferingSearch.create();
        if (state != null) {
            sc.setParameters("state", state);
        }

        if (vmId != null) {
            if (!currentVmOffering.isDynamic()) {
                sc.setParameters("idNEQ", currentVmOffering.getId());
            }

            if (currentVmOffering.getDiskOfferingStrictness()) {
                sc.setParameters("diskOfferingId", currentVmOffering.getDiskOfferingId());
                sc.setParameters("diskOfferingStrictness", true);
            } else {
                sc.setParameters("diskOfferingStrictness", false);
            }

            boolean isRootVolumeUsingLocalStorage = virtualMachineManager.isRootVolumeOnLocalStorage(vmId);

            // 1. Only return offerings with the same storage type than the storage pool where the VM's root volume is allocated
            sc.setJoinParameters("diskOfferingSearch", "useLocalStorage", isRootVolumeUsingLocalStorage);

            // 2.In case vm is running return only offerings greater than equal to current offering compute and offering's dynamic scalability should match
            if (vmInstance.getState() == VirtualMachine.State.Running) {
                Integer vmCpu = currentVmOffering.getCpu();
                Integer vmMemory = currentVmOffering.getRamSize();
                Integer vmSpeed = currentVmOffering.getSpeed();
                if ((vmCpu == null || vmMemory == null || vmSpeed == null) && VirtualMachine.Type.User.equals(vmInstance.getType())) {
                    UserVmVO userVmVO = userVmDao.findById(vmId);
                    userVmDao.loadDetails(userVmVO);
                    Map<String, String> details = userVmVO.getDetails();
                    vmCpu = NumbersUtil.parseInt(details.get(ApiConstants.CPU_NUMBER), 0);
                    if (vmSpeed == null) {
                        vmSpeed = NumbersUtil.parseInt(details.get(ApiConstants.CPU_SPEED), 0);
                    }
                    vmMemory = NumbersUtil.parseInt(details.get(ApiConstants.MEMORY), 0);
                }
                if (vmCpu != null && vmCpu > 0) {
                    sc.setParameters("vmCpu", vmCpu);
                    sc.setParameters("vmMaxComputeGTEQ", vmCpu);
                }
                if (vmSpeed != null && vmSpeed > 0) {
                    sc.setParameters("speedGTEQ", vmSpeed);
                }
                if (vmMemory != null && vmMemory > 0) {
                    sc.setParameters("vmMemory", vmMemory);
                    sc.setParameters("vmMaxMemoryGTEQ", vmMemory);
                }
                sc.setParameters("dynamicScalingEnabled", currentVmOffering.isDynamicScalingEnabled());
            }
        }

        if ((!accountMgr.isNormalUser(caller.getId()) && !accountMgr.isDomainAdmin(caller.getId())) && caller.getType() != Account.Type.RESOURCE_DOMAIN_ADMIN) {
            if (domainId != null && accountName == null) {
                sc.setJoinParameters("domainDetailSearch", "domainId", domainId);
            }
        }

        if (keyword != null) {
            sc.setParameters("keywordName", "%" + keyword + "%");
            sc.setParameters("keywordDisplayText", "%" + keyword + "%");
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (isSystem != null) {
            // note that for non-root users, isSystem is always false when
            // control comes to here
            sc.setParameters("systemUse", isSystem);
        }

        if (encryptRoot != null) {
            sc.setJoinParameters("diskOfferingSearch", "encrypt", encryptRoot);
        }

        if (name != null) {
            sc.setParameters("name", name);
        }

        if (vmTypeStr != null) {
            sc.setParameters("svmType", vmTypeStr);
        }

        useStorageType(sc, storageType);

        if (zoneId != null) {
            sc.setJoinParameters("ZoneDetailSearch", "zoneId", zoneId);

            if (DataCenter.Type.Edge.equals(zone.getType())) {
                sc.setJoinParameters("diskOfferingSearch", "useLocalStorage", true);
            }
        }

        if (cpuNumber != null) {
            sc.setParameters("cpuNumber", cpuNumber);
        }

        if (memory != null) {
            sc.setParameters("memory", memory);
        }

        if (cpuSpeed != null) {
            sc.setParameters("speedGTEQ", cpuSpeed);
        }

        if (owner.getType() != Account.Type.ADMIN) {
            Domain callerDomain = _domainDao.findById(owner.getDomainId());
            List<Long> domainIds = findRelatedDomainIds(callerDomain, isRecursive);

            sc.setJoinParameters("domainDetailSearchNormalUser", "domainIdIN", domainIds.toArray());
        }

        if (diskOffering != null) {
            List<String> storageTags = com.cloud.utils.StringUtils.csvTagsToList(diskOffering.getTags());
            if (!storageTags.isEmpty() && VolumeApiServiceImpl.MatchStoragePoolTagsWithDiskOffering.value()) {
                for (String tag : storageTags) {
                    sc.setJoinParameters("diskOfferingSearch", "storageTag" + tag, tag);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(hostTags)) {
            for (String tag : hostTags) {
                sc.setParameters("hostTag" + tag, tag);
            }
        }

        Pair<List<ServiceOfferingVO>, Integer> uniquePair = _srvOfferingDao.searchAndCount(sc, searchFilter);
        Integer count = uniquePair.second();
        List<Long> offeringIds = uniquePair.first().stream().map(ServiceOfferingVO::getId).collect(Collectors.toList());
        return new Pair<>(offeringIds, count);
    }

    @Override
    public ListResponse<ZoneResponse> listDataCenters(ListZonesCmd cmd) {
        Pair<List<DataCenterJoinVO>, Integer> result = listDataCentersInternal(cmd);
        ListResponse<ZoneResponse> response = new ListResponse<>();

        ResponseView respView = ResponseView.Restricted;
        if (cmd instanceof ListZonesCmdByAdmin || CallContext.current().getCallingAccount().getType() == Account.Type.ADMIN) {
            respView = ResponseView.Full;
        }

        List<ZoneResponse> dcResponses = ViewResponseHelper.createDataCenterResponse(respView, cmd.getShowCapacities(), cmd.getShowIcon(), result.first().toArray(new DataCenterJoinVO[result.first().size()]));
        response.setResponses(dcResponses, result.second());
        return response;
    }

    private Pair<List<DataCenterJoinVO>, Integer> listDataCentersInternal(ListZonesCmd cmd) {
        Account account = CallContext.current().getCallingAccount();
        Long domainId = cmd.getDomainId();
        Long id = cmd.getId();
        List<Long> ids = getIdsListFromCmd(cmd.getId(), cmd.getIds());
        String keyword = cmd.getKeyword();
        String name = cmd.getName();
        String networkType = cmd.getNetworkType();
        Map<String, String> resourceTags = cmd.getTags();

        SearchBuilder<DataCenterJoinVO> sb = _dcJoinDao.createSearchBuilder();
        if (resourceTags != null && !resourceTags.isEmpty()) {
            SearchBuilder<ResourceTagVO> tagSearch = resourceTagDao.createSearchBuilder();
            for (int count = 0; count < resourceTags.size(); count++) {
                tagSearch.or().op("key" + String.valueOf(count), tagSearch.entity().getKey(), SearchCriteria.Op.EQ);
                tagSearch.and("value" + String.valueOf(count), tagSearch.entity().getValue(), SearchCriteria.Op.EQ);
                tagSearch.cp();
            }
            tagSearch.and("resourceType", tagSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
            sb.groupBy(sb.entity().getId());
            sb.join("tagSearch", tagSearch, sb.entity().getId(), tagSearch.entity().getResourceId(), JoinBuilder.JoinType.INNER);
        }

        Filter searchFilter = new Filter(DataCenterJoinVO.class, "sortKey", SortKeyAscending.value(), cmd.getStartIndex(), cmd.getPageSizeVal());
        searchFilter.addOrderBy(DataCenterJoinVO.class, "id", true);
        SearchCriteria<DataCenterJoinVO> sc = sb.create();

        if (networkType != null) {
            sc.addAnd("networkType", SearchCriteria.Op.EQ, networkType);
        }

        if (CollectionUtils.isNotEmpty(ids)) {
            sc.addAnd("id", SearchCriteria.Op.IN, ids.toArray());
        }

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        } else if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.EQ, name);
        } else {
            if (keyword != null) {
                SearchCriteria<DataCenterJoinVO> ssc = _dcJoinDao.createSearchCriteria();
                ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
                ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");
                sc.addAnd("name", SearchCriteria.Op.SC, ssc);
            }

            /*
             * List all resources due to Explicit Dedication except the
             * dedicated resources of other account
             */
            if (domainId != null) { //
                // for domainId != null // right now, we made the decision to
                // only list zones associated // with this domain, private zone
                sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);

                if (accountMgr.isNormalUser(account.getId())) {
                    // accountId == null (zones dedicated to a domain) or
                    // accountId = caller
                    SearchCriteria<DataCenterJoinVO> sdc = _dcJoinDao.createSearchCriteria();
                    sdc.addOr("accountId", SearchCriteria.Op.EQ, account.getId());
                    sdc.addOr("accountId", SearchCriteria.Op.NULL);

                    sc.addAnd("accountId", SearchCriteria.Op.SC, sdc);
                }

            } else if (accountMgr.isNormalUser(account.getId())) {
                // it was decided to return all zones for the user's domain, and
                // everything above till root
                // list all zones belonging to this domain, and all of its
                // parents
                // check the parent, if not null, add zones for that parent to
                // list

                // find all domain Id up to root domain for this account
                List<Long> domainIds = new ArrayList<>();
                DomainVO domainRecord = _domainDao.findById(account.getDomainId());
                if (domainRecord == null) {
                    logger.error("Could not find the domainId for account: {}", account);
                    throw new CloudAuthenticationException("Could not find the domainId for account:" + account.getAccountName());
                }
                domainIds.add(domainRecord.getId());
                while (domainRecord.getParent() != null) {
                    domainRecord = _domainDao.findById(domainRecord.getParent());
                    domainIds.add(domainRecord.getId());
                }
                // domainId == null (public zones) or domainId IN [all domain id
                // up to root domain]
                SearchCriteria<DataCenterJoinVO> sdc = _dcJoinDao.createSearchCriteria();
                sdc.addOr("domainId", SearchCriteria.Op.IN, domainIds.toArray());
                sdc.addOr("domainId", SearchCriteria.Op.NULL);
                sc.addAnd("domainId", SearchCriteria.Op.SC, sdc);

                // remove disabled zones
                sc.addAnd("allocationState", SearchCriteria.Op.NEQ, Grouping.AllocationState.Disabled);

                // accountId == null (zones dedicated to a domain) or
                // accountId = caller
                SearchCriteria<DataCenterJoinVO> sdc2 = _dcJoinDao.createSearchCriteria();
                sdc2.addOr("accountId", SearchCriteria.Op.EQ, account.getId());
                sdc2.addOr("accountId", SearchCriteria.Op.NULL);

                sc.addAnd("accountId", SearchCriteria.Op.SC, sdc2);

                // remove Dedicated zones not dedicated to this domainId or
                // subdomainId
                List<Long> dedicatedZoneIds = removeDedicatedZoneNotSuitabe(domainIds);
                if (!dedicatedZoneIds.isEmpty()) {
                    sdc.addAnd("id", SearchCriteria.Op.NIN, dedicatedZoneIds.toArray(new Object[dedicatedZoneIds.size()]));
                }

            } else if (accountMgr.isDomainAdmin(account.getId()) || account.getType() == Account.Type.RESOURCE_DOMAIN_ADMIN) {
                // it was decided to return all zones for the domain admin, and
                // everything above till root, as well as zones till the domain
                // leaf
                List<Long> domainIds = new ArrayList<>();
                DomainVO domainRecord = _domainDao.findById(account.getDomainId());
                if (domainRecord == null) {
                    logger.error("Could not find the domainId for account: {}", account);
                    throw new CloudAuthenticationException("Could not find the domainId for account:" + account.getAccountName());
                }
                domainIds.add(domainRecord.getId());
                // find all domain Ids till leaf
                List<DomainVO> allChildDomains = _domainDao.findAllChildren(domainRecord.getPath(), domainRecord.getId());
                for (DomainVO domain : allChildDomains) {
                    domainIds.add(domain.getId());
                }
                // then find all domain Id up to root domain for this account
                while (domainRecord.getParent() != null) {
                    domainRecord = _domainDao.findById(domainRecord.getParent());
                    domainIds.add(domainRecord.getId());
                }

                // domainId == null (public zones) or domainId IN [all domain id
                // up to root domain]
                SearchCriteria<DataCenterJoinVO> sdc = _dcJoinDao.createSearchCriteria();
                sdc.addOr("domainId", SearchCriteria.Op.IN, domainIds.toArray());
                sdc.addOr("domainId", SearchCriteria.Op.NULL);
                sc.addAnd("domainId", SearchCriteria.Op.SC, sdc);

                // remove disabled zones
                sc.addAnd("allocationState", SearchCriteria.Op.NEQ, Grouping.AllocationState.Disabled);

                // remove Dedicated zones not dedicated to this domainId or
                // subdomainId
                List<Long> dedicatedZoneIds = removeDedicatedZoneNotSuitabe(domainIds);
                if (!dedicatedZoneIds.isEmpty()) {
                    sdc.addAnd("id", SearchCriteria.Op.NIN, dedicatedZoneIds.toArray(new Object[dedicatedZoneIds.size()]));
                }
            }

            // handle available=FALSE option, only return zones with at least
            // one VM running there
            Boolean available = cmd.isAvailable();
            if (account != null) {
                if (Boolean.FALSE.equals(available)) {
                    Set<Long> dcIds = new HashSet<>(); // data centers with
                    // at least one VM
                    // running
                    List<DomainRouterVO> routers = _routerDao.listBy(account.getId());
                    for (DomainRouterVO router : routers) {
                        dcIds.add(router.getDataCenterId());
                    }
                    if (dcIds.size() == 0) {
                        return new Pair<>(new ArrayList<>(), 0);
                    } else {
                        sc.addAnd("id", SearchCriteria.Op.IN, dcIds.toArray());
                    }

                }
            }
        }

        if (resourceTags != null && !resourceTags.isEmpty()) {
            int count = 0;
            sc.setJoinParameters("tagSearch", "resourceType", ResourceObjectType.Zone.toString());
            for (Map.Entry<String, String> entry : resourceTags.entrySet()) {
                sc.setJoinParameters("tagSearch", "key" + String.valueOf(count), entry.getKey());
                sc.setJoinParameters("tagSearch", "value" + String.valueOf(count), entry.getValue());
                count++;
            }
        }

        return _dcJoinDao.searchAndCount(sc, searchFilter);
    }

    private List<Long> removeDedicatedZoneNotSuitabe(List<Long> domainIds) {
        // remove dedicated zone of other domain
        List<Long> dedicatedZoneIds = new ArrayList<>();
        List<DedicatedResourceVO> dedicatedResources = _dedicatedDao.listZonesNotInDomainIds(domainIds);
        for (DedicatedResourceVO dr : dedicatedResources) {
            if (dr != null) {
                dedicatedZoneIds.add(dr.getDataCenterId());
            }
        }
        return dedicatedZoneIds;
    }

    // This method is used for permissions check for both disk and service
    // offerings
    private boolean isPermissible(Long accountDomainId, Long offeringDomainId) {

        if (accountDomainId.equals(offeringDomainId)) {
            return true; // account and service offering in same domain
        }

        DomainVO domainRecord = _domainDao.findById(accountDomainId);

        if (domainRecord != null) {
            while (true) {
                if (domainRecord.getId() == offeringDomainId) {
                    return true;
                }

                // try and move on to the next domain
                if (domainRecord.getParent() != null) {
                    domainRecord = _domainDao.findById(domainRecord.getParent());
                } else {
                    break;
                }
            }
        }

        return false;
    }

    @Override
    public ListResponse<TemplateResponse> listTemplates(ListTemplatesCmd cmd) {
        Pair<List<TemplateJoinVO>, Integer> result = searchForTemplatesInternal(cmd);
        ListResponse<TemplateResponse> response = new ListResponse<>();

        ResponseView respView = ResponseView.Restricted;
        if (cmd instanceof ListTemplatesCmdByAdmin) {
            respView = ResponseView.Full;
        }

        List<TemplateResponse> templateResponses = ViewResponseHelper.createTemplateResponse(cmd.getDetails(), respView, result.first().toArray(new TemplateJoinVO[result.first().size()]));
        response.setResponses(templateResponses, result.second());
        return response;
    }

    private Pair<List<TemplateJoinVO>, Integer> searchForTemplatesInternal(ListTemplatesCmd cmd) {
        TemplateFilter templateFilter = TemplateFilter.valueOf(cmd.getTemplateFilter());
        Long id = cmd.getId();
        Map<String, String> tags = cmd.getTags();
        boolean showRemovedTmpl = cmd.getShowRemoved();
        Account caller = CallContext.current().getCallingAccount();
        Long parentTemplateId = cmd.getParentTemplateId();

        boolean listAll = false;
        if (templateFilter != null && templateFilter == TemplateFilter.all) {
            if (caller.getType() == Account.Type.NORMAL) {
                throw new InvalidParameterValueException("Filter " + TemplateFilter.all + " can be specified by admin only");
            }
            listAll = true;
        }

        List<Long> permittedAccountIds = new ArrayList<>();
        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<>(cmd.getDomainId(), cmd.isRecursive(), null);
        accountMgr.buildACLSearchParameters(
                caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccountIds,
                domainIdRecursiveListProject, listAll, false
        );
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        List<Account> permittedAccounts = new ArrayList<>();
        for (Long accountId : permittedAccountIds) {
            permittedAccounts.add(accountMgr.getAccount(accountId));
        }

        boolean showDomr = ((templateFilter != TemplateFilter.selfexecutable) && (templateFilter != TemplateFilter.featured));
        HypervisorType hypervisorType = HypervisorType.getType(cmd.getHypervisor());

        String templateType = cmd.getTemplateType();
        if (cmd instanceof ListVnfTemplatesCmd) {
            if (templateType == null) {
                templateType = TemplateType.VNF.name();
            } else if (!TemplateType.VNF.name().equals(templateType)) {
                throw new InvalidParameterValueException("Template type must be VNF when list VNF templates");
            }
        }
        Boolean isVnf = cmd.getVnf();

        return searchForTemplatesInternal(id, cmd.getTemplateName(), cmd.getKeyword(), templateFilter, false,
                null, cmd.getPageSizeVal(), cmd.getStartIndex(), cmd.getZoneId(), cmd.getStoragePoolId(),
                cmd.getImageStoreId(), hypervisorType, showDomr, cmd.listInReadyState(), permittedAccounts, caller,
                listProjectResourcesCriteria, tags, showRemovedTmpl, cmd.getIds(), parentTemplateId, cmd.getShowUnique(),
                templateType, isVnf, cmd.getArch());
    }

    private Pair<List<TemplateJoinVO>, Integer> searchForTemplatesInternal(Long templateId, String name, String keyword,
            TemplateFilter templateFilter, boolean isIso, Boolean bootable, Long pageSize,
            Long startIndex, Long zoneId, Long storagePoolId, Long imageStoreId, HypervisorType hyperType,
            boolean showDomr, boolean onlyReady, List<Account> permittedAccounts, Account caller,
            ListProjectResourcesCriteria listProjectResourcesCriteria, Map<String, String> tags,
            boolean showRemovedTmpl, List<Long> ids, Long parentTemplateId, Boolean showUnique, String templateType,
            Boolean isVnf, CPU.CPUArch arch) {

        // check if zone is configured, if not, just return empty list
        List<HypervisorType> hypers = null;
        if (!isIso) {
            hypers = _resourceMgr.listAvailHypervisorInZone(null);
            if (hypers == null || hypers.isEmpty()) {
                return new Pair<>(new ArrayList<>(), 0);
            }
        }

        VMTemplateVO template;

        Filter searchFilter = new Filter(TemplateJoinVO.class, "sortKey", SortKeyAscending.value(), startIndex, pageSize);
        searchFilter.addOrderBy(TemplateJoinVO.class, "tempZonePair", SortKeyAscending.value());

        SearchBuilder<TemplateJoinVO> sb = _templateJoinDao.createSearchBuilder();
        if (showUnique) {
            sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct templateId
        } else {
            sb.select(null, Func.DISTINCT, sb.entity().getTempZonePair()); // select distinct (templateId, zoneId) pair
        }
        if (ids != null && !ids.isEmpty()) {
            sb.and("idIN", sb.entity().getId(), SearchCriteria.Op.IN);
        }

        if (storagePoolId != null) {
            SearchBuilder<VMTemplateStoragePoolVO> storagePoolSb = templatePoolDao.createSearchBuilder();
            storagePoolSb.and("pool_id", storagePoolSb.entity().getPoolId(), SearchCriteria.Op.EQ);
            sb.join("storagePool", storagePoolSb, storagePoolSb.entity().getTemplateId(), sb.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<TemplateJoinVO> sc = sb.create();

        if (imageStoreId != null) {
            sc.addAnd("dataStoreId", SearchCriteria.Op.EQ, imageStoreId);
        }

        if (arch != null) {
            sc.addAnd("arch", SearchCriteria.Op.EQ, arch);
        }

        if (storagePoolId != null) {
            sc.setJoinParameters("storagePool", "pool_id", storagePoolId);
        }

        // verify templateId parameter and specially handle it
        if (templateId != null) {
            template = _templateDao.findByIdIncludingRemoved(templateId); // Done for backward compatibility - Bug-5221
            if (template == null) {
                throw new InvalidParameterValueException("Please specify a valid template ID.");
            }// If ISO requested then it should be ISO.
            if (isIso && template.getFormat() != ImageFormat.ISO) {
                logger.error("Template {} is not an ISO", template);
                InvalidParameterValueException ex = new InvalidParameterValueException("Specified Template Id is not an ISO");
                ex.addProxyObject(template.getUuid(), "templateId");
                throw ex;
            }// If ISO not requested then it shouldn't be an ISO.
            if (!isIso && template.getFormat() == ImageFormat.ISO) {
                logger.error("Incorrect format of the template: {}", template);
                InvalidParameterValueException ex = new InvalidParameterValueException("Incorrect format " + template.getFormat() + " of the specified template id");
                ex.addProxyObject(template.getUuid(), "templateId");
                throw ex;
            }
            if (!template.isPublicTemplate() && caller.getType() == Account.Type.DOMAIN_ADMIN) {
                Account template_acc = accountMgr.getAccount(template.getAccountId());
                DomainVO domain = _domainDao.findById(template_acc.getDomainId());
                accountMgr.checkAccess(caller, domain);
            }

            // if template is not public, perform permission check here
            else if (!template.isPublicTemplate() && caller.getType() != Account.Type.ADMIN) {
                accountMgr.checkAccess(caller, null, false, template);
            } else if (template.isPublicTemplate()) {
                accountMgr.checkAccess(caller, null, false, template);
            }

            // if templateId is specified, then we will just use the id to
            // search and ignore other query parameters
            sc.addAnd("id", SearchCriteria.Op.EQ, templateId);
        } else {

            DomainVO domain;
            if (!permittedAccounts.isEmpty()) {
                domain = _domainDao.findById(permittedAccounts.get(0).getDomainId());
            } else {
                domain = _domainDao.findById(Domain.ROOT_DOMAIN);
            }

            setIdsListToSearchCriteria(sc, ids);

            // add criteria for project or not
            if (listProjectResourcesCriteria == ListProjectResourcesCriteria.SkipProjectResources) {
                sc.addAnd("accountType", SearchCriteria.Op.NEQ, Account.Type.PROJECT);
            } else if (listProjectResourcesCriteria == ListProjectResourcesCriteria.ListProjectResourcesOnly) {
                sc.addAnd("accountType", SearchCriteria.Op.EQ, Account.Type.PROJECT);
            }

            // add criteria for domain path in case of domain admin
            if ((templateFilter == TemplateFilter.self || templateFilter == TemplateFilter.selfexecutable)
                    && (caller.getType() == Account.Type.DOMAIN_ADMIN || caller.getType() == Account.Type.RESOURCE_DOMAIN_ADMIN)) {
                sc.addAnd("domainPath", SearchCriteria.Op.LIKE, domain.getPath() + "%");
            }

            List<Long> relatedDomainIds = new ArrayList<>();
            List<Long> permittedAccountIds = new ArrayList<>();
            if (!permittedAccounts.isEmpty()) {
                for (Account account : permittedAccounts) {
                    permittedAccountIds.add(account.getId());
                    boolean publicTemplates = (templateFilter == TemplateFilter.featured || templateFilter == TemplateFilter.community);

                    // get all parent domain ID's all the way till root domain
                    DomainVO domainTreeNode;
                    //if template filter is featured, or community, all child domains should be included in search
                    if (publicTemplates) {
                        domainTreeNode = _domainDao.findById(Domain.ROOT_DOMAIN);

                    } else {
                        domainTreeNode = _domainDao.findById(account.getDomainId());
                    }
                    relatedDomainIds.add(domainTreeNode.getId());
                    while (domainTreeNode.getParent() != null) {
                        domainTreeNode = _domainDao.findById(domainTreeNode.getParent());
                        relatedDomainIds.add(domainTreeNode.getId());
                    }

                    // get all child domain ID's
                    if (accountMgr.isAdmin(account.getId()) || publicTemplates) {
                        List<DomainVO> allChildDomains = _domainDao.findAllChildren(domainTreeNode.getPath(), domainTreeNode.getId());
                        for (DomainVO childDomain : allChildDomains) {
                            relatedDomainIds.add(childDomain.getId());
                        }
                    }
                }
            }

            // control different template filters
            if (templateFilter == TemplateFilter.featured || templateFilter == TemplateFilter.community) {
                sc.addAnd("publicTemplate", SearchCriteria.Op.EQ, true);
                if (templateFilter == TemplateFilter.featured) {
                    sc.addAnd("featured", SearchCriteria.Op.EQ, true);
                } else {
                    sc.addAnd("featured", SearchCriteria.Op.EQ, false);
                }
                if (!permittedAccounts.isEmpty()) {
                    SearchCriteria<TemplateJoinVO> scc = _templateJoinDao.createSearchCriteria();
                    scc.addOr("domainId", SearchCriteria.Op.IN, relatedDomainIds.toArray());
                    scc.addOr("domainId", SearchCriteria.Op.NULL);
                    sc.addAnd("domainId", SearchCriteria.Op.SC, scc);
                }
            } else if (templateFilter == TemplateFilter.self || templateFilter == TemplateFilter.selfexecutable) {
                if (!permittedAccounts.isEmpty()) {
                    sc.addAnd("accountId", SearchCriteria.Op.IN, permittedAccountIds.toArray());
                }
            } else if (templateFilter == TemplateFilter.sharedexecutable || templateFilter == TemplateFilter.shared) {
                // only show templates shared by others
                if (permittedAccounts.isEmpty()) {
                    return new Pair<>(new ArrayList<>(), 0);
                }
                sc.addAnd("sharedAccountId", SearchCriteria.Op.IN, permittedAccountIds.toArray());
            } else if (templateFilter == TemplateFilter.executable) {
                SearchCriteria<TemplateJoinVO> scc = _templateJoinDao.createSearchCriteria();
                scc.addOr("publicTemplate", SearchCriteria.Op.EQ, true);
                if (!permittedAccounts.isEmpty()) {
                    scc.addOr("accountId", SearchCriteria.Op.IN, permittedAccountIds.toArray());
                }
                sc.addAnd("publicTemplate", SearchCriteria.Op.SC, scc);
            } else if (templateFilter == TemplateFilter.all && caller.getType() != Account.Type.ADMIN) {
                SearchCriteria<TemplateJoinVO> scc = _templateJoinDao.createSearchCriteria();
                scc.addOr("publicTemplate", SearchCriteria.Op.EQ, true);

                if (listProjectResourcesCriteria == ListProjectResourcesCriteria.SkipProjectResources) {
                    scc.addOr("domainPath", SearchCriteria.Op.LIKE, _domainDao.findById(caller.getDomainId()).getPath() + "%");
                } else {
                    if (!permittedAccounts.isEmpty()) {
                        scc.addOr("accountId", SearchCriteria.Op.IN, permittedAccountIds.toArray());
                        scc.addOr("sharedAccountId", SearchCriteria.Op.IN, permittedAccountIds.toArray());
                    }
                }
                sc.addAnd("publicTemplate", SearchCriteria.Op.SC, scc);
            }
        }

        applyPublicTemplateSharingRestrictions(sc, caller);

        return templateChecks(isIso, hypers, tags, name, keyword, hyperType, onlyReady, bootable, zoneId, showDomr, caller,
                showRemovedTmpl, parentTemplateId, showUnique, templateType, isVnf, searchFilter, sc);
    }

    /**
     * If the caller is not a root admin, restricts the search to return only public templates from the domain which
     * the caller belongs to and domains with the setting 'share.public.templates.with.other.domains' enabled.
     */
    protected void applyPublicTemplateSharingRestrictions(SearchCriteria<TemplateJoinVO> sc, Account caller) {
        if (caller.getType() == Account.Type.ADMIN) {
            logger.debug(String.format("Account [%s] is a root admin. Therefore, it has access to all public templates.", caller));
            return;
        }

        List<TemplateJoinVO> publicTemplates = _templateJoinDao.listPublicTemplates();

        Set<Long> unsharableDomainIds = new HashSet<>();
        for (TemplateJoinVO template : publicTemplates) {
            addDomainIdToSetIfDomainDoesNotShareTemplates(template.getDomainId(), caller, unsharableDomainIds);
        }

        if (!unsharableDomainIds.isEmpty()) {
            logger.info(String.format("The public templates belonging to the domains [%s] will not be listed to account [%s] as they have the configuration [%s] marked as 'false'.", unsharableDomainIds, caller, QueryService.SharePublicTemplatesWithOtherDomains.key()));
            sc.addAnd("domainId", SearchCriteria.Op.NOTIN, unsharableDomainIds.toArray());
        }
    }

    /**
     * Adds the provided domain ID to the set if the domain does not share templates with the account. That is, if:
     * (1) the template does not belong to the domain of the account AND
     * (2) the domain of the template has the setting 'share.public.templates.with.other.domains' disabled.
     */
    protected void addDomainIdToSetIfDomainDoesNotShareTemplates(long domainId, Account account, Set<Long> unsharableDomainIds) {
        if (domainId == account.getDomainId()) {
            logger.trace(String.format("Domain [%s] will not be added to the set of domains with unshared templates since the account [%s] belongs to it.", domainId, account));
            return;
        }

        if (unsharableDomainIds.contains(domainId)) {
            logger.trace(String.format("Domain [%s] is already on the set of domains with unshared templates.", domainId));
            return;
        }

        if (!checkIfDomainSharesTemplates(domainId)) {
            logger.debug(String.format("Domain [%s] will be added to the set of domains with unshared templates as configuration [%s] is false.", domainId, QueryService.SharePublicTemplatesWithOtherDomains.key()));
            unsharableDomainIds.add(domainId);
        }
    }

    protected boolean checkIfDomainSharesTemplates(Long domainId) {
        return QueryService.SharePublicTemplatesWithOtherDomains.valueIn(domainId);
    }

    private Pair<List<TemplateJoinVO>, Integer> templateChecks(boolean isIso, List<HypervisorType> hypers, Map<String, String> tags, String name, String keyword,
                                                               HypervisorType hyperType, boolean onlyReady, Boolean bootable, Long zoneId, boolean showDomr, Account caller,
                                                               boolean showRemovedTmpl, Long parentTemplateId, Boolean showUnique, String templateType, Boolean isVnf,
                                                               Filter searchFilter, SearchCriteria<TemplateJoinVO> sc) {
        if (!isIso) {
            // add hypervisor criteria for template case
            if (hypers != null && !hypers.isEmpty()) {
                String[] relatedHypers = new String[hypers.size()];
                for (int i = 0; i < hypers.size(); i++) {
                    relatedHypers[i] = hypers.get(i).toString();
                }
                sc.addAnd("hypervisorType", SearchCriteria.Op.IN, relatedHypers);
            }
        }

        // add tags criteria
        if (tags != null && !tags.isEmpty()) {
            SearchCriteria<TemplateJoinVO> scc = _templateJoinDao.createSearchCriteria();
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                SearchCriteria<TemplateJoinVO> scTag = _templateJoinDao.createSearchCriteria();
                scTag.addAnd("tagKey", SearchCriteria.Op.EQ, entry.getKey());
                scTag.addAnd("tagValue", SearchCriteria.Op.EQ, entry.getValue());
                if (isIso) {
                    scTag.addAnd("tagResourceType", SearchCriteria.Op.EQ, ResourceObjectType.ISO);
                } else {
                    scTag.addAnd("tagResourceType", SearchCriteria.Op.EQ, ResourceObjectType.Template);
                }
                scc.addOr("tagKey", SearchCriteria.Op.SC, scTag);
            }
            sc.addAnd("tagKey", SearchCriteria.Op.SC, scc);
        }

        // other criteria

        if (keyword != null) {
            SearchCriteria<TemplateJoinVO> scc = _templateJoinDao.createSearchCriteria();
            scc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            scc.addOr("displayText", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, scc);
        }
        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.EQ, name);
        }

        SearchCriteria.Op op = isIso ? Op.EQ : Op.NEQ;
        sc.addAnd("format", op, "ISO");

        if (!hyperType.equals(HypervisorType.None)) {
            sc.addAnd("hypervisorType", SearchCriteria.Op.EQ, hyperType);
        }

        if (bootable != null) {
            sc.addAnd("bootable", SearchCriteria.Op.EQ, bootable);
        }

        if (onlyReady) {
            SearchCriteria<TemplateJoinVO> readySc = _templateJoinDao.createSearchCriteria();
            readySc.addOr("state", SearchCriteria.Op.EQ, TemplateState.Ready);
            readySc.addOr("format", SearchCriteria.Op.EQ, ImageFormat.BAREMETAL);
            SearchCriteria<TemplateJoinVO> isoPerhostSc = _templateJoinDao.createSearchCriteria();
            isoPerhostSc.addAnd("format", SearchCriteria.Op.EQ, ImageFormat.ISO);
            isoPerhostSc.addAnd("templateType", SearchCriteria.Op.EQ, TemplateType.PERHOST);
            readySc.addOr("templateType", SearchCriteria.Op.SC, isoPerhostSc);
            sc.addAnd("state", SearchCriteria.Op.SC, readySc);
        }

        if (!showDomr) {
            // excluding system template
            sc.addAnd("templateType", SearchCriteria.Op.NEQ, Storage.TemplateType.SYSTEM);
        }

        if (zoneId != null) {
            SearchCriteria<TemplateJoinVO> zoneSc = _templateJoinDao.createSearchCriteria();
            zoneSc.addOr("dataCenterId", SearchCriteria.Op.EQ, zoneId);
            zoneSc.addOr("dataStoreScope", SearchCriteria.Op.EQ, ScopeType.REGION);
            // handle the case where TemplateManager.VMWARE_TOOLS_ISO and TemplateManager.VMWARE_TOOLS_ISO do not
            // have data_center information in template_view
            SearchCriteria<TemplateJoinVO> isoPerhostSc = _templateJoinDao.createSearchCriteria();
            isoPerhostSc.addAnd("format", SearchCriteria.Op.EQ, ImageFormat.ISO);
            isoPerhostSc.addAnd("templateType", SearchCriteria.Op.EQ, TemplateType.PERHOST);
            zoneSc.addOr("templateType", SearchCriteria.Op.SC, isoPerhostSc);
            sc.addAnd("dataCenterId", SearchCriteria.Op.SC, zoneSc);
        }

        if (parentTemplateId != null) {
            sc.addAnd("parentTemplateId", SearchCriteria.Op.EQ, parentTemplateId);
        }

        if (templateType != null) {
            sc.addAnd("templateType", SearchCriteria.Op.EQ, templateType);
        }

        if (isVnf != null) {
            if (isVnf) {
                sc.addAnd("templateType", SearchCriteria.Op.EQ, TemplateType.VNF);
            } else {
                sc.addAnd("templateType", SearchCriteria.Op.NEQ, TemplateType.VNF);
            }
        }

        // don't return removed template, this should not be needed since we
        // changed annotation for removed field in TemplateJoinVO.
        // sc.addAnd("removed", SearchCriteria.Op.NULL);

        // search unique templates and find details by Ids
        Pair<List<TemplateJoinVO>, Integer> uniqueTmplPair;
        if (showRemovedTmpl) {
            uniqueTmplPair = _templateJoinDao.searchIncludingRemovedAndCount(sc, searchFilter);
        } else {
            sc.addAnd("templateState", SearchCriteria.Op.IN, new State[] {State.Active, State.UploadAbandoned, State.UploadError, State.NotUploaded, State.UploadInProgress});
            if (showUnique) {
                final String[] distinctColumns = {"template_view.id"};
                uniqueTmplPair = _templateJoinDao.searchAndDistinctCount(sc, searchFilter, distinctColumns);
            } else {
                final String[] distinctColumns = {"temp_zone_pair"};
                uniqueTmplPair = _templateJoinDao.searchAndDistinctCount(sc, searchFilter, distinctColumns);
            }
        }

        return findTemplatesByIdOrTempZonePair(uniqueTmplPair, showRemovedTmpl, showUnique, caller);

        // TODO: revisit the special logic for iso search in
        // VMTemplateDaoImpl.searchForTemplates and understand why we need to
        // specially handle ISO. The original logic is very twisted and no idea
        // about what the code was doing.
    }

    // findTemplatesByIdOrTempZonePair returns the templates with the given ids if showUnique is true, or else by the TempZonePair
    private Pair<List<TemplateJoinVO>, Integer> findTemplatesByIdOrTempZonePair(Pair<List<TemplateJoinVO>, Integer> templateDataPair,
                                                                                boolean showRemoved, boolean showUnique, Account caller) {
        Integer count = templateDataPair.second();
        if (count.intValue() == 0) {
            // empty result
            return templateDataPair;
        }
        List<TemplateJoinVO> templateData = templateDataPair.first();
        List<TemplateJoinVO> templates;
        if (showUnique) {
            Long[] templateIds = templateData.stream().map(template -> template.getId()).toArray(Long[]::new);
            templates = _templateJoinDao.findByDistinctIds(templateIds);
        } else {
            String[] templateZonePairs = templateData.stream().map(template -> template.getTempZonePair()).toArray(String[]::new);
            templates = _templateJoinDao.searchByTemplateZonePair(showRemoved, templateZonePairs);
        }

        return new Pair<>(templates, count);
    }

    @Override
    public ListResponse<TemplateResponse> listIsos(ListIsosCmd cmd) {
        Pair<List<TemplateJoinVO>, Integer> result = searchForIsosInternal(cmd);
        ListResponse<TemplateResponse> response = new ListResponse<>();

        ResponseView respView = ResponseView.Restricted;
        if (cmd instanceof ListIsosCmdByAdmin) {
            respView = ResponseView.Full;
        }

        List<TemplateResponse> templateResponses = ViewResponseHelper.createIsoResponse(respView, result.first().toArray(new TemplateJoinVO[result.first().size()]));
        response.setResponses(templateResponses, result.second());
        return response;
    }

    private Pair<List<TemplateJoinVO>, Integer> searchForIsosInternal(ListIsosCmd cmd) {
        TemplateFilter isoFilter = TemplateFilter.valueOf(cmd.getIsoFilter());
        Long id = cmd.getId();
        Map<String, String> tags = cmd.getTags();
        boolean showRemovedISO = cmd.getShowRemoved();
        Account caller = CallContext.current().getCallingAccount();

        boolean listAll = false;
        if (isoFilter != null && isoFilter == TemplateFilter.all) {
            if (caller.getType() == Account.Type.NORMAL) {
                throw new InvalidParameterValueException("Filter " + TemplateFilter.all + " can be specified by admin only");
            }
            listAll = true;
        }


        List<Long> permittedAccountIds = new ArrayList<>();
        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<>(cmd.getDomainId(), cmd.isRecursive(), null);
        accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccountIds, domainIdRecursiveListProject, listAll, false);
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        List<Account> permittedAccounts = new ArrayList<>();
        for (Long accountId : permittedAccountIds) {
            permittedAccounts.add(accountMgr.getAccount(accountId));
        }

        HypervisorType hypervisorType = HypervisorType.getType(cmd.getHypervisor());

        return searchForTemplatesInternal(cmd.getId(), cmd.getIsoName(), cmd.getKeyword(), isoFilter, true, cmd.isBootable(),
                cmd.getPageSizeVal(), cmd.getStartIndex(), cmd.getZoneId(), cmd.getStoragePoolId(), cmd.getImageStoreId(),
                hypervisorType, true, cmd.listInReadyState(), permittedAccounts, caller, listProjectResourcesCriteria,
                tags, showRemovedISO, null, null, cmd.getShowUnique(), null, null, cmd.getArch());
    }

    @Override
    public DetailOptionsResponse listDetailOptions(final ListDetailOptionsCmd cmd) {
        final ResourceObjectType type = cmd.getResourceType();
        final String resourceUuid = cmd.getResourceId();
        final Map<String, List<String>> options = new HashMap<>();
        switch (type) {
            case Template:
            case UserVm:
                HypervisorType hypervisorType = HypervisorType.None;
                if (StringUtils.isNotEmpty(resourceUuid) && ResourceObjectType.Template.equals(type)) {
                    hypervisorType = _templateDao.findByUuid(resourceUuid).getHypervisorType();
                }
                if (StringUtils.isNotEmpty(resourceUuid) && ResourceObjectType.UserVm.equals(type)) {
                    hypervisorType = _vmInstanceDao.findByUuid(resourceUuid).getHypervisorType();
                }
                fillVMOrTemplateDetailOptions(options, hypervisorType);
                break;
            case VnfTemplate:
                fillVnfTemplateDetailOptions(options);
                return new DetailOptionsResponse(options);
            default:
                throw new CloudRuntimeException("Resource type not supported.");
        }
        if (CallContext.current().getCallingAccount().getType() != Account.Type.ADMIN) {
            final List<String> userDenyListedSettings = Stream.of(QueryService.UserVMDeniedDetails.value().split(","))
                    .map(item -> (item).trim())
                    .collect(Collectors.toList());
            for (final String detail : userDenyListedSettings) {
                if (options.containsKey(detail)) {
                    options.remove(detail);
                }
            }
        }
        return new DetailOptionsResponse(options);
    }

    @Override
    public ListResponse<ResourceIconResponse> listResourceIcons(ListResourceIconCmd cmd) {
        ListResponse<ResourceIconResponse> responses = new ListResponse<>();
        responses.setResponses(resourceIconDao.listResourceIcons(cmd.getResourceIds(), cmd.getResourceType()));
        return responses;
    }

    private void fillVnfTemplateDetailOptions(final Map<String, List<String>> options) {
        for (VNF.AccessDetail detail : VNF.AccessDetail.values()) {
            if (VNF.AccessDetail.ACCESS_METHODS.equals(detail)) {
                options.put(detail.name().toLowerCase(), Arrays.stream(VNF.AccessMethod.values()).map(method -> method.toString()).sorted().collect(Collectors.toList()));
            } else {
                options.put(detail.name().toLowerCase(), Collections.emptyList());
            }
        }
        for (VNF.VnfDetail detail : VNF.VnfDetail.values()) {
            options.put(detail.name().toLowerCase(), Collections.emptyList());
        }
    }

    private void fillVMOrTemplateDetailOptions(final Map<String, List<String>> options, final HypervisorType hypervisorType) {
        if (options == null) {
            throw new CloudRuntimeException("Invalid/null detail-options response object passed");
        }

        options.put(ApiConstants.BootType.UEFI.toString(), Arrays.asList(ApiConstants.BootMode.LEGACY.toString(),
            ApiConstants.BootMode.SECURE.toString()));
        options.put(VmDetailConstants.KEYBOARD, Arrays.asList("uk", "us", "jp", "fr"));
        options.put(VmDetailConstants.CPU_CORE_PER_SOCKET, Collections.emptyList());
        options.put(VmDetailConstants.ROOT_DISK_SIZE, Collections.emptyList());

        if (HypervisorType.KVM.equals(hypervisorType)) {
            options.put(VmDetailConstants.CPU_THREAD_PER_CORE, Collections.emptyList());
            options.put(VmDetailConstants.NIC_ADAPTER, Arrays.asList("e1000", "virtio", "rtl8139", "vmxnet3", "ne2k_pci"));
            options.put(VmDetailConstants.ROOT_DISK_CONTROLLER, Arrays.asList("osdefault", "ide", "scsi", "virtio", "virtio-blk"));
            options.put(VmDetailConstants.VIDEO_HARDWARE, Arrays.asList("cirrus", "vga", "qxl", "virtio"));
            options.put(VmDetailConstants.VIDEO_RAM, Collections.emptyList());
            options.put(VmDetailConstants.IO_POLICY, Arrays.asList("threads", "native", "io_uring", "storage_specific"));
            options.put(VmDetailConstants.IOTHREADS, Arrays.asList("enabled"));
            options.put(VmDetailConstants.NIC_MULTIQUEUE_NUMBER, Collections.emptyList());
            options.put(VmDetailConstants.NIC_PACKED_VIRTQUEUES_ENABLED, Arrays.asList("true", "false"));
        }

        if (HypervisorType.VMware.equals(hypervisorType)) {
            options.put(VmDetailConstants.NIC_ADAPTER, Arrays.asList("E1000", "PCNet32", "Vmxnet2", "Vmxnet3"));
            options.put(VmDetailConstants.ROOT_DISK_CONTROLLER, Arrays.asList("osdefault", "ide", "scsi", "lsilogic", "lsisas1068", "buslogic", "pvscsi"));
            options.put(VmDetailConstants.DATA_DISK_CONTROLLER, Arrays.asList("osdefault", "ide", "scsi", "lsilogic", "lsisas1068", "buslogic", "pvscsi"));
            options.put(VmDetailConstants.NESTED_VIRTUALIZATION_FLAG, Arrays.asList("true", "false"));
            options.put(VmDetailConstants.SVGA_VRAM_SIZE, Collections.emptyList());
            options.put(VmDetailConstants.RAM_RESERVATION, Collections.emptyList());
        }
    }

    @Override
    public ListResponse<AffinityGroupResponse> searchForAffinityGroups(ListAffinityGroupsCmd cmd) {
        Pair<List<AffinityGroupJoinVO>, Integer> result = searchForAffinityGroupsInternal(cmd);
        ListResponse<AffinityGroupResponse> response = new ListResponse<AffinityGroupResponse>();
        List<AffinityGroupResponse> agResponses = ViewResponseHelper.createAffinityGroupResponses(result.first());
        response.setResponses(agResponses, result.second());
        return response;
    }

    public Pair<List<AffinityGroupJoinVO>, Integer> searchForAffinityGroupsInternal(ListAffinityGroupsCmd cmd) {

        final Long affinityGroupId = cmd.getId();
        final String affinityGroupName = cmd.getAffinityGroupName();
        final String affinityGroupType = cmd.getAffinityGroupType();
        final Long vmId = cmd.getVirtualMachineId();
        final String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        final Long projectId = cmd.getProjectId();
        Boolean isRecursive = cmd.isRecursive();
        final Boolean listAll = cmd.listAll();
        final Long startIndex = cmd.getStartIndex();
        final Long pageSize = cmd.getPageSizeVal();
        final String keyword = cmd.getKeyword();

        Account caller = CallContext.current().getCallingAccount();

        if (vmId != null) {
            UserVmVO userVM = userVmDao.findById(vmId);
            if (userVM == null) {
                throw new InvalidParameterValueException("Unable to list affinity groups for virtual machine instance " + vmId + "; instance not found.");
            }
            accountMgr.checkAccess(caller, null, true, userVM);
            return listAffinityGroupsByVM(vmId.longValue(), startIndex, pageSize);
        }

        List<Long> permittedAccounts = new ArrayList<>();
        Ternary<Long, Boolean, ListProjectResourcesCriteria> ternary = new Ternary<>(domainId, isRecursive, null);

        accountMgr.buildACLSearchParameters(caller, affinityGroupId, accountName, projectId, permittedAccounts, ternary, listAll, false);

        domainId = ternary.first();
        isRecursive = ternary.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = ternary.third();

        Filter searchFilter = new Filter(AffinityGroupJoinVO.class, ID_FIELD, true, startIndex, pageSize);

        SearchCriteria<AffinityGroupJoinVO> sc = buildAffinityGroupSearchCriteria(domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria, affinityGroupId, affinityGroupName,
                affinityGroupType, keyword);

        Pair<List<AffinityGroupJoinVO>, Integer> uniqueGroupsPair = _affinityGroupJoinDao.searchAndCount(sc, searchFilter);

        // search group details by ids
        List<AffinityGroupJoinVO> affinityGroups = new ArrayList<>();

        Integer count = uniqueGroupsPair.second();
        if (count.intValue() != 0) {
            List<AffinityGroupJoinVO> uniqueGroups = uniqueGroupsPair.first();
            Long[] vrIds = new Long[uniqueGroups.size()];
            int i = 0;
            for (AffinityGroupJoinVO v : uniqueGroups) {
                vrIds[i++] = v.getId();
            }
            affinityGroups = _affinityGroupJoinDao.searchByIds(vrIds);
        }

        if (!permittedAccounts.isEmpty()) {
            // add domain level affinity groups
            if (domainId != null) {
                SearchCriteria<AffinityGroupJoinVO> scDomain = buildAffinityGroupSearchCriteria(null, isRecursive, new ArrayList<>(), listProjectResourcesCriteria, affinityGroupId,
                        affinityGroupName, affinityGroupType, keyword);
                Pair<List<AffinityGroupJoinVO>, Integer> groupsPair = listDomainLevelAffinityGroups(scDomain, searchFilter, domainId);
                affinityGroups.addAll(groupsPair.first());
                count += groupsPair.second();
            } else {

                for (Long permAcctId : permittedAccounts) {
                    Account permittedAcct = _accountDao.findById(permAcctId);
                    SearchCriteria<AffinityGroupJoinVO> scDomain = buildAffinityGroupSearchCriteria(null, isRecursive, new ArrayList<>(), listProjectResourcesCriteria, affinityGroupId,
                            affinityGroupName, affinityGroupType, keyword);
                    Pair<List<AffinityGroupJoinVO>, Integer> groupsPair = listDomainLevelAffinityGroups(scDomain, searchFilter, permittedAcct.getDomainId());
                    affinityGroups.addAll(groupsPair.first());
                    count += groupsPair.second();
                }
            }
        } else if (((permittedAccounts.isEmpty()) && (domainId != null) && isRecursive)) {
            // list all domain level affinity groups for the domain admin case
            SearchCriteria<AffinityGroupJoinVO> scDomain = buildAffinityGroupSearchCriteria(null, isRecursive, new ArrayList<>(), listProjectResourcesCriteria, affinityGroupId, affinityGroupName,
                    affinityGroupType, keyword);
            Pair<List<AffinityGroupJoinVO>, Integer> groupsPair = listDomainLevelAffinityGroups(scDomain, searchFilter, domainId);
            affinityGroups.addAll(groupsPair.first());
            count += groupsPair.second();
        }

        return new Pair<>(affinityGroups, count);

    }

    private void buildAffinityGroupViewSearchBuilder(SearchBuilder<AffinityGroupJoinVO> sb, Long domainId, boolean isRecursive, List<Long> permittedAccounts,
            ListProjectResourcesCriteria listProjectResourcesCriteria) {

        sb.and("accountIdIN", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);

        if (((permittedAccounts.isEmpty()) && (domainId != null) && isRecursive)) {
            // if accountId isn't specified, we can do a domain match for the
            // admin case if isRecursive is true
            sb.and("domainPath", sb.entity().getDomainPath(), SearchCriteria.Op.LIKE);
        }

        if (listProjectResourcesCriteria != null) {
            if (listProjectResourcesCriteria == Project.ListProjectResourcesCriteria.ListProjectResourcesOnly) {
                sb.and("accountType", sb.entity().getAccountType(), SearchCriteria.Op.EQ);
            } else if (listProjectResourcesCriteria == Project.ListProjectResourcesCriteria.SkipProjectResources) {
                sb.and("accountType", sb.entity().getAccountType(), SearchCriteria.Op.NEQ);
            }
        }

    }

    private void buildAffinityGroupViewSearchCriteria(SearchCriteria<AffinityGroupJoinVO> sc, Long domainId, boolean isRecursive, List<Long> permittedAccounts,
            ListProjectResourcesCriteria listProjectResourcesCriteria) {

        if (listProjectResourcesCriteria != null) {
            sc.setParameters("accountType", Account.Type.PROJECT);
        }

        if (!permittedAccounts.isEmpty()) {
            sc.setParameters("accountIdIN", permittedAccounts.toArray());
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            if (isRecursive) {
                sc.setParameters("domainPath", domain.getPath() + "%");
            } else {
                sc.setParameters("domainId", domainId);
            }
        }
    }

    private SearchCriteria<AffinityGroupJoinVO> buildAffinityGroupSearchCriteria(Long domainId, boolean isRecursive, List<Long> permittedAccounts,
            ListProjectResourcesCriteria listProjectResourcesCriteria, Long affinityGroupId, String affinityGroupName, String affinityGroupType, String keyword) {

        SearchBuilder<AffinityGroupJoinVO> groupSearch = _affinityGroupJoinDao.createSearchBuilder();
        buildAffinityGroupViewSearchBuilder(groupSearch, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        groupSearch.select(null, Func.DISTINCT, groupSearch.entity().getId()); // select
        // distinct

        SearchCriteria<AffinityGroupJoinVO> sc = groupSearch.create();
        buildAffinityGroupViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (affinityGroupId != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, affinityGroupId);
        }

        if (affinityGroupName != null) {
            sc.addAnd("name", SearchCriteria.Op.EQ, affinityGroupName);
        }

        if (affinityGroupType != null) {
            sc.addAnd("type", SearchCriteria.Op.EQ, affinityGroupType);
        }

        if (keyword != null) {
            SearchCriteria<AffinityGroupJoinVO> ssc = _affinityGroupJoinDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("type", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        return sc;
    }

    private Pair<List<AffinityGroupJoinVO>, Integer> listAffinityGroupsByVM(long vmId, long pageInd, long pageSize) {
        Filter sf = new Filter(SecurityGroupVMMapVO.class, null, true, pageInd, pageSize);
        Pair<List<AffinityGroupVMMapVO>, Integer> agVmMappingPair = _affinityGroupVMMapDao.listByInstanceId(vmId, sf);
        Integer count = agVmMappingPair.second();
        if (count.intValue() == 0) {
            // handle empty result cases
            return new Pair<>(new ArrayList<>(), count);
        }
        List<AffinityGroupVMMapVO> agVmMappings = agVmMappingPair.first();
        Long[] agIds = new Long[agVmMappings.size()];
        int i = 0;
        for (AffinityGroupVMMapVO agVm : agVmMappings) {
            agIds[i++] = agVm.getAffinityGroupId();
        }
        List<AffinityGroupJoinVO> ags = _affinityGroupJoinDao.searchByIds(agIds);
        return new Pair<>(ags, count);
    }

    private Pair<List<AffinityGroupJoinVO>, Integer> listDomainLevelAffinityGroups(SearchCriteria<AffinityGroupJoinVO> sc, Filter searchFilter, long domainId) {
        List<Long> affinityGroupIds = new ArrayList<>();
        Set<Long> allowedDomains = _domainMgr.getDomainParentIds(domainId);
        List<AffinityGroupDomainMapVO> maps = _affinityGroupDomainMapDao.listByDomain(allowedDomains.toArray());

        for (AffinityGroupDomainMapVO map : maps) {
            boolean subdomainAccess = map.isSubdomainAccess();
            if (map.getDomainId() == domainId || subdomainAccess) {
                affinityGroupIds.add(map.getAffinityGroupId());
            }
        }

        if (!affinityGroupIds.isEmpty()) {
            SearchCriteria<AffinityGroupJoinVO> domainSC = _affinityGroupJoinDao.createSearchCriteria();
            domainSC.addAnd("id", SearchCriteria.Op.IN, affinityGroupIds.toArray());
            domainSC.addAnd("aclType", SearchCriteria.Op.EQ, ACLType.Domain.toString());

            sc.addAnd("id", SearchCriteria.Op.SC, domainSC);

            Pair<List<AffinityGroupJoinVO>, Integer> uniqueGroupsPair = _affinityGroupJoinDao.searchAndCount(sc, searchFilter);
            // search group by ids
            Integer count = uniqueGroupsPair.second();
            if (count.intValue() == 0) {
                // empty result
                return new Pair<>(new ArrayList<>(), 0);
            }
            List<AffinityGroupJoinVO> uniqueGroups = uniqueGroupsPair.first();
            Long[] vrIds = new Long[uniqueGroups.size()];
            int i = 0;
            for (AffinityGroupJoinVO v : uniqueGroups) {
                vrIds[i++] = v.getId();
            }
            List<AffinityGroupJoinVO> vrs = _affinityGroupJoinDao.searchByIds(vrIds);
            return new Pair<>(vrs, count);
        } else {
            return new Pair<>(new ArrayList<>(), 0);
        }
    }

    @Override
    public List<ResourceDetailResponse> listResourceDetails(ListResourceDetailsCmd cmd) {
        String key = cmd.getKey();
        Boolean forDisplay = cmd.getDisplay();
        ResourceTag.ResourceObjectType resourceType = cmd.getResourceType();
        String resourceIdStr = cmd.getResourceId();
        String value = cmd.getValue();
        Long resourceId = null;

        //Validation - 1.1 - resourceId and value can't be null.
        if (resourceIdStr == null && value == null) {
            throw new InvalidParameterValueException("Insufficient parameters passed for listing by resourceId OR key,value pair. Please check your params and try again.");
        }

        //Validation - 1.2 - Value has to be passed along with key.
        if (value != null && key == null) {
            throw new InvalidParameterValueException("Listing by (key, value) but key is null. Please check the params and try again");
        }

        //Validation - 1.3
        if (resourceIdStr != null) {
            resourceId = resourceManagerUtil.getResourceId(resourceIdStr, resourceType);
            if (resourceId == null) {
                throw new InvalidParameterValueException("Cannot find resource with resourceId " + resourceIdStr + " and of resource type " + resourceType);
            }
        }

        List<? extends ResourceDetail> detailList = new ArrayList<>();
        ResourceDetail requestedDetail = null;

        if (key == null) {
            detailList = _resourceMetaDataMgr.getDetailsList(resourceId, resourceType, forDisplay);
        } else if (value == null) {
            requestedDetail = _resourceMetaDataMgr.getDetail(resourceId, resourceType, key);
            if (requestedDetail != null && forDisplay != null && requestedDetail.isDisplay() != forDisplay) {
                requestedDetail = null;
            }
        } else {
            detailList = _resourceMetaDataMgr.getDetails(resourceType, key, value, forDisplay);
        }

        List<ResourceDetailResponse> responseList = new ArrayList<>();
        if (requestedDetail != null) {
            ResourceDetailResponse detailResponse = createResourceDetailsResponse(requestedDetail, resourceType);
            responseList.add(detailResponse);
        } else {
            for (ResourceDetail detail : detailList) {
                ResourceDetailResponse detailResponse = createResourceDetailsResponse(detail, resourceType);
                responseList.add(detailResponse);
            }
        }

        return responseList;
    }

    protected ResourceDetailResponse createResourceDetailsResponse(ResourceDetail requestedDetail, ResourceTag.ResourceObjectType resourceType) {
        ResourceDetailResponse resourceDetailResponse = new ResourceDetailResponse();
        resourceDetailResponse.setResourceId(resourceManagerUtil.getUuid(String.valueOf(requestedDetail.getResourceId()), resourceType));
        resourceDetailResponse.setName(requestedDetail.getName());
        resourceDetailResponse.setValue(requestedDetail.getValue());
        resourceDetailResponse.setForDisplay(requestedDetail.isDisplay());
        resourceDetailResponse.setResourceType(resourceType.toString().toString());
        resourceDetailResponse.setObjectName("resourcedetail");
        return resourceDetailResponse;
    }

    @Override
    public ListResponse<ManagementServerResponse> listManagementServers(ListMgmtsCmd cmd) {
        ListResponse<ManagementServerResponse> response = new ListResponse<>();
        Pair<List<ManagementServerJoinVO>, Integer> result = listManagementServersInternal(cmd);
        List<ManagementServerResponse> hostResponses = new ArrayList<>();

        for (ManagementServerJoinVO host : result.first()) {
            ManagementServerResponse hostResponse = createManagementServerResponse(host, cmd.getPeers());
            hostResponses.add(hostResponse);
        }

        response.setResponses(hostResponses);
        return response;
    }

    protected Pair<List<ManagementServerJoinVO>, Integer> listManagementServersInternal(ListMgmtsCmd cmd) {
        Long id = cmd.getId();
        String name = cmd.getHostName();

        SearchBuilder<ManagementServerJoinVO> sb = managementServerJoinDao.createSearchBuilder();
        SearchCriteria<ManagementServerJoinVO> sc = sb.create();
        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }
        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.EQ, name);
        }
        return managementServerJoinDao.searchAndCount(sc, null);
    }

    protected ManagementServerResponse createManagementServerResponse(ManagementServerJoinVO mgmt, boolean listPeers) {
        ManagementServerResponse mgmtResponse = new ManagementServerResponse();
        mgmtResponse.setId(mgmt.getUuid());
        mgmtResponse.setName(mgmt.getName());
        mgmtResponse.setState(mgmt.getState());
        mgmtResponse.setVersion(mgmt.getVersion());
        mgmtResponse.setJavaVersion(mgmt.getJavaVersion());
        mgmtResponse.setJavaDistribution(mgmt.getJavaName());
        mgmtResponse.setOsDistribution(mgmt.getOsDistribution());
        mgmtResponse.setLastServerStart(mgmt.getLastJvmStart());
        mgmtResponse.setLastServerStop(mgmt.getLastJvmStop());
        mgmtResponse.setLastBoot(mgmt.getLastSystemBoot());
        if (listPeers) {
            List<ManagementServerHostPeerJoinVO> peers = mshostPeerJoinDao.listByOwnerMshostId(mgmt.getId());
            for (ManagementServerHostPeerJoinVO peer: peers) {
                mgmtResponse.addPeer(createPeerManagementServerNodeResponse(peer));
            }
        }
        mgmtResponse.setIpAddress(mgmt.getServiceIP());
        mgmtResponse.setObjectName("managementserver");
        return mgmtResponse;
    }

    private PeerManagementServerNodeResponse createPeerManagementServerNodeResponse(ManagementServerHostPeerJoinVO peer) {
        PeerManagementServerNodeResponse response = new PeerManagementServerNodeResponse();

        response.setState(peer.getPeerState());
        response.setLastUpdated(peer.getLastUpdateTime());

        response.setPeerId(peer.getPeerMshostUuid());
        response.setPeerName(peer.getPeerMshostName());
        response.setPeerMsId(String.valueOf(peer.getPeerMshostMsId()));
        response.setPeerRunId(String.valueOf(peer.getPeerMshostRunId()));
        response.setPeerState(peer.getPeerMshostState());
        response.setPeerServiceIp(peer.getPeerMshostServiceIp());
        response.setPeerServicePort(String.valueOf(peer.getPeerMshostServicePort()));

        response.setObjectName("peermanagementserver");
        return response;
    }

    @Override
    public List<RouterHealthCheckResultResponse> listRouterHealthChecks(GetRouterHealthCheckResultsCmd cmd) {
        logger.info("Executing health check command " + cmd);
        long routerId = cmd.getRouterId();
        if (!VirtualNetworkApplianceManager.RouterHealthChecksEnabled.value()) {
            throw new CloudRuntimeException("Router health checks are not enabled for router " + routerId);
        }

        if (cmd.shouldPerformFreshChecks()) {
            Pair<Boolean, String> healthChecksresult = routerService.performRouterHealthChecks(routerId);
            if (healthChecksresult == null) {
                throw new CloudRuntimeException("Failed to initiate fresh checks on router.");
            } else if (!healthChecksresult.first()) {
                throw new CloudRuntimeException("Unable to perform fresh checks on router - " + healthChecksresult.second());
            }
        }

        List<RouterHealthCheckResult> result = new ArrayList<>(routerHealthCheckResultDao.getHealthCheckResults(routerId));
        if (result == null || result.size() == 0) {
            throw new CloudRuntimeException("No health check results found for the router. This could happen for " +
                    "a newly created router. Please wait for periodic results to populate or manually call for checks to execute.");
        }

        return responseGenerator.createHealthCheckResponse(_routerDao.findById(routerId), result);
    }

    @Override
    public ListResponse<SecondaryStorageHeuristicsResponse> listSecondaryStorageSelectors(ListSecondaryStorageSelectorsCmd cmd) {
        ListResponse<SecondaryStorageHeuristicsResponse> response = new ListResponse<>();
        Pair<List<HeuristicVO>, Integer> result = listSecondaryStorageSelectorsInternal(cmd.getZoneId(), cmd.getType(), cmd.isShowRemoved());
        List<SecondaryStorageHeuristicsResponse> listOfSecondaryStorageHeuristicsResponses = new ArrayList<>();

        for (Heuristic heuristic : result.first()) {
            SecondaryStorageHeuristicsResponse secondaryStorageHeuristicsResponse = responseGenerator.createSecondaryStorageSelectorResponse(heuristic);
            listOfSecondaryStorageHeuristicsResponses.add(secondaryStorageHeuristicsResponse);
        }

        response.setResponses(listOfSecondaryStorageHeuristicsResponses);
        return response;
    }

    private Pair<List<HeuristicVO>, Integer> listSecondaryStorageSelectorsInternal(Long zoneId, String type, boolean showRemoved) {
        SearchBuilder<HeuristicVO> searchBuilder = secondaryStorageHeuristicDao.createSearchBuilder();

        searchBuilder.and("zoneId", searchBuilder.entity().getZoneId(), SearchCriteria.Op.EQ);
        searchBuilder.and("type", searchBuilder.entity().getType(), SearchCriteria.Op.EQ);

        searchBuilder.done();

        SearchCriteria<HeuristicVO> searchCriteria = searchBuilder.create();
        searchCriteria.setParameters("zoneId", zoneId);
        searchCriteria.setParametersIfNotNull("type", type);

        return secondaryStorageHeuristicDao.searchAndCount(searchCriteria, null, showRemoved);
    }

    @Override
    public ListResponse<IpQuarantineResponse> listQuarantinedIps(ListQuarantinedIpsCmd cmd) {
        ListResponse<IpQuarantineResponse> response = new ListResponse<>();
        Pair<List<PublicIpQuarantineVO>, Integer> result = listQuarantinedIpsInternal(cmd.isShowRemoved(), cmd.isShowInactive());
        List<IpQuarantineResponse> ipsQuarantinedResponses = new ArrayList<>();

        for (PublicIpQuarantine quarantinedIp : result.first()) {
            IpQuarantineResponse ipsInQuarantineResponse = responseGenerator.createQuarantinedIpsResponse(quarantinedIp);
            ipsQuarantinedResponses.add(ipsInQuarantineResponse);
        }

        response.setResponses(ipsQuarantinedResponses);
        return response;
    }

    /**
     * It lists the quarantine IPs that the caller account is allowed to see by filtering the domain path of the caller account.
     * Furthermore, it lists inactive and removed quarantined IPs according to the command parameters.
     */
    private Pair<List<PublicIpQuarantineVO>, Integer> listQuarantinedIpsInternal(boolean showRemoved, boolean showInactive) {
        String callingAccountDomainPath = _domainDao.findById(CallContext.current().getCallingAccount().getDomainId()).getPath();

        SearchBuilder<AccountJoinVO> filterAllowedOnly = _accountJoinDao.createSearchBuilder();
        filterAllowedOnly.and("path", filterAllowedOnly.entity().getDomainPath(), SearchCriteria.Op.LIKE);

        SearchBuilder<PublicIpQuarantineVO> listAllPublicIpsInQuarantineAllowedToTheCaller = publicIpQuarantineDao.createSearchBuilder();
        listAllPublicIpsInQuarantineAllowedToTheCaller.join("listQuarantinedJoin", filterAllowedOnly,
                listAllPublicIpsInQuarantineAllowedToTheCaller.entity().getPreviousOwnerId(),
                filterAllowedOnly.entity().getId(), JoinBuilder.JoinType.INNER);

        if (!showInactive) {
            listAllPublicIpsInQuarantineAllowedToTheCaller.and("endDate", listAllPublicIpsInQuarantineAllowedToTheCaller.entity().getEndDate(), SearchCriteria.Op.GT);
        }

        filterAllowedOnly.done();
        listAllPublicIpsInQuarantineAllowedToTheCaller.done();

        SearchCriteria<PublicIpQuarantineVO> searchCriteria = listAllPublicIpsInQuarantineAllowedToTheCaller.create();
        searchCriteria.setJoinParameters("listQuarantinedJoin", "path", callingAccountDomainPath + "%");
        searchCriteria.setParametersIfNotNull("endDate", new Date());

        return publicIpQuarantineDao.searchAndCount(searchCriteria, null, showRemoved);
    }

    public ListResponse<SnapshotResponse> listSnapshots(ListSnapshotsCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Pair<List<SnapshotJoinVO>, Integer> result = searchForSnapshotsWithParams(cmd.getId(), cmd.getIds(),
                cmd.getVolumeId(), cmd.getSnapshotName(), cmd.getKeyword(), cmd.getTags(),
                cmd.getSnapshotType(), cmd.getIntervalType(), cmd.getZoneId(), cmd.getLocationType(),
                cmd.isShowUnique(), cmd.getAccountName(), cmd.getDomainId(), cmd.getProjectId(), cmd.getStoragePoolId(),
                cmd.getImageStoreId(), cmd.getStartIndex(), cmd.getPageSizeVal(), cmd.listAll(), cmd.isRecursive(), caller);
        ListResponse<SnapshotResponse> response = new ListResponse<>();
        ResponseView respView = ResponseView.Restricted;
        if (cmd instanceof ListSnapshotsCmdByAdmin) {
            respView = ResponseView.Full;
        }
        List<SnapshotResponse> templateResponses = ViewResponseHelper.createSnapshotResponse(respView, cmd.isShowUnique(), result.first().toArray(new SnapshotJoinVO[result.first().size()]));
        response.setResponses(templateResponses, result.second());
        return response;
    }

    @Override
    public SnapshotResponse listSnapshot(CopySnapshotCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        List<Long> zoneIds = cmd.getDestinationZoneIds();
        Pair<List<SnapshotJoinVO>, Integer> result = searchForSnapshotsWithParams(cmd.getId(), null,
                null, null, null, null,
                null, null, zoneIds.get(0), Snapshot.LocationType.SECONDARY.name(),
                false, null, null, null, null, null,
                null, null, true, false, caller);
        ResponseView respView = ResponseView.Restricted;
        if (CallContext.current().getCallingAccount().getType() == Account.Type.ADMIN) {
            respView = ResponseView.Full;
        }
        List<SnapshotResponse> templateResponses = ViewResponseHelper.createSnapshotResponse(respView, false, result.first().get(0));
        return templateResponses.get(0);
    }



    private Pair<List<SnapshotJoinVO>, Integer> searchForSnapshotsWithParams(final Long id, List<Long> ids,
            final Long volumeId, final String name, final String keyword, final Map<String, String> tags,
            final String snapshotTypeStr, final String intervalTypeStr, final Long zoneId, final String locationTypeStr,
            final boolean isShowUnique, final String accountName, Long domainId, final Long projectId, final Long storagePoolId, final Long imageStoreId,
            final Long startIndex, final Long pageSize,final boolean listAll, boolean isRecursive, final Account caller) {
        ids = getIdsListFromCmd(id, ids);
        Snapshot.LocationType locationType = null;
        if (locationTypeStr != null) {
            try {
                locationType = Snapshot.LocationType.valueOf(locationTypeStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidParameterValueException(String.format("Invalid %s specified, %s", ApiConstants.LOCATION_TYPE, locationTypeStr));
            }
        }

        Filter searchFilter = new Filter(SnapshotJoinVO.class, "snapshotStorePair", SortKeyAscending.value(), startIndex, pageSize);

        List<Long> permittedAccountIds = new ArrayList<>();
        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<>(domainId, isRecursive, null);
        accountMgr.buildACLSearchParameters(caller, id, accountName, projectId, permittedAccountIds, domainIdRecursiveListProject, listAll, false);
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        domainId = domainIdRecursiveListProject.first();
        isRecursive = domainIdRecursiveListProject.second();
        // Verify parameters
        if (volumeId != null) {
            VolumeVO volume = volumeDao.findById(volumeId);
            if (volume != null) {
                accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, volume);
            }
        }

        SearchBuilder<SnapshotJoinVO> sb = snapshotJoinDao.createSearchBuilder();
        if (isShowUnique) {
            sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct snapshotId
        } else {
            sb.select(null, Func.DISTINCT, sb.entity().getSnapshotStorePair()); // select distinct (snapshotId, store_role, store_id) key
        }
        accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccountIds, listProjectResourcesCriteria);
        sb.and("statusNEQ", sb.entity().getStatus(), SearchCriteria.Op.NEQ); //exclude those Destroyed snapshot, not showing on UI
        sb.and("volumeId", sb.entity().getVolumeId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("idIN", sb.entity().getId(), SearchCriteria.Op.IN);
        sb.and("snapshotTypeEQ", sb.entity().getSnapshotType(), SearchCriteria.Op.IN);
        sb.and("snapshotTypeNEQ", sb.entity().getSnapshotType(), SearchCriteria.Op.NIN);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("locationType", sb.entity().getStoreRole(), SearchCriteria.Op.EQ);
        sb.and("imageStoreId", sb.entity().getStoreId(), SearchCriteria.Op.EQ);

        if (tags != null && !tags.isEmpty()) {
            SearchBuilder<ResourceTagVO> tagSearch = resourceTagDao.createSearchBuilder();
            for (int count = 0; count < tags.size(); count++) {
                tagSearch.or().op("key" + String.valueOf(count), tagSearch.entity().getKey(), SearchCriteria.Op.EQ);
                tagSearch.and("value" + String.valueOf(count), tagSearch.entity().getValue(), SearchCriteria.Op.EQ);
                tagSearch.cp();
            }
            tagSearch.and("resourceType", tagSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
            sb.groupBy(sb.entity().getId());
            sb.join("tagSearch", tagSearch, sb.entity().getId(), tagSearch.entity().getResourceId(), JoinBuilder.JoinType.INNER);
        }

        if (storagePoolId != null) {
            SearchBuilder<SnapshotDataStoreVO> storagePoolSb = snapshotDataStoreDao.createSearchBuilder();
            storagePoolSb.and("poolId", storagePoolSb.entity().getDataStoreId(), SearchCriteria.Op.EQ);
            storagePoolSb.and("role", storagePoolSb.entity().getRole(), SearchCriteria.Op.EQ);
            sb.join("storagePoolSb", storagePoolSb, sb.entity().getId(), storagePoolSb.entity().getSnapshotId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<SnapshotJoinVO> sc = sb.create();
        accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccountIds, listProjectResourcesCriteria);

        sc.setParameters("statusNEQ", Snapshot.State.Destroyed);

        if (imageStoreId != null) {
            sc.setParameters("imageStoreId", imageStoreId);
            locationType = Snapshot.LocationType.SECONDARY;
        }

        if (storagePoolId != null) {
            sc.setJoinParameters("storagePoolSb", "poolId", storagePoolId);
            sc.setJoinParameters("storagePoolSb", "role", DataStoreRole.Image);
        }

        if (volumeId != null) {
            sc.setParameters("volumeId", volumeId);
        }

        if (tags != null && !tags.isEmpty()) {
            int count = 0;
            sc.setJoinParameters("tagSearch", "resourceType", ResourceObjectType.Snapshot.toString());
            for (String key : tags.keySet()) {
                sc.setJoinParameters("tagSearch", "key" + String.valueOf(count), key);
                sc.setJoinParameters("tagSearch", "value" + String.valueOf(count), tags.get(key));
                count++;
            }
        }

        if (zoneId != null) {
            sc.setParameters("dataCenterId", zoneId);
        }

        setIdsListToSearchCriteria(sc, ids);

        if (name != null) {
            sc.setParameters("name", name);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (locationType != null) {
            sc.setParameters("locationType", Snapshot.LocationType.PRIMARY.equals(locationType) ? locationType.name() : DataStoreRole.Image.name());
        }

        if (keyword != null) {
            SearchCriteria<SnapshotJoinVO> ssc = snapshotJoinDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (snapshotTypeStr != null) {
            Snapshot.Type snapshotType = SnapshotVO.getSnapshotType(snapshotTypeStr);
            if (snapshotType == null) {
                throw new InvalidParameterValueException("Unsupported snapshot type " + snapshotTypeStr);
            }
            if (snapshotType == Snapshot.Type.RECURRING) {
                sc.setParameters("snapshotTypeEQ", Snapshot.Type.HOURLY.ordinal(), Snapshot.Type.DAILY.ordinal(), Snapshot.Type.WEEKLY.ordinal(), Snapshot.Type.MONTHLY.ordinal());
            } else {
                sc.setParameters("snapshotTypeEQ", snapshotType.ordinal());
            }
        } else if (intervalTypeStr != null && volumeId != null) {
            Snapshot.Type type = SnapshotVO.getSnapshotType(intervalTypeStr);
            if (type == null) {
                throw new InvalidParameterValueException("Unsupported snapshot interval type " + intervalTypeStr);
            }
            sc.setParameters("snapshotTypeEQ", type.ordinal());
        } else {
            // Show only MANUAL and RECURRING snapshot types
            sc.setParameters("snapshotTypeNEQ", Snapshot.Type.TEMPLATE.ordinal(), Snapshot.Type.GROUP.ordinal());
        }

        Pair<List<SnapshotJoinVO>, Integer> snapshotDataPair;
        if (isShowUnique) {
            snapshotDataPair = snapshotJoinDao.searchAndDistinctCount(sc, searchFilter, new String[]{"snapshot_view.id"});
        } else {
            snapshotDataPair = snapshotJoinDao.searchAndDistinctCount(sc, searchFilter, new String[]{"snapshot_view.snapshot_store_pair"});
        }

        Integer count = snapshotDataPair.second();
        if (count == 0) {
            return snapshotDataPair;
        }
        List<SnapshotJoinVO> snapshotData = snapshotDataPair.first();
        List<SnapshotJoinVO> snapshots;
        if (isShowUnique) {
            snapshots = snapshotJoinDao.findByDistinctIds(zoneId, snapshotData.stream().map(SnapshotJoinVO::getId).toArray(Long[]::new));
        } else {
            snapshots = snapshotJoinDao.searchBySnapshotStorePair(snapshotData.stream().map(SnapshotJoinVO::getSnapshotStorePair).toArray(String[]::new));
        }
        return new Pair<>(snapshots, count);
    }

    public ListResponse<ObjectStoreResponse> searchForObjectStores(ListObjectStoragePoolsCmd cmd) {
        Pair<List<ObjectStoreVO>, Integer> result = searchForObjectStoresInternal(cmd);
        ListResponse<ObjectStoreResponse> response = new ListResponse<>();

        List<ObjectStoreResponse> poolResponses = ViewResponseHelper.createObjectStoreResponse(result.first().toArray(new ObjectStoreVO[result.first().size()]));
        response.setResponses(poolResponses, result.second());
        return response;
    }

    private Pair<List<ObjectStoreVO>, Integer> searchForObjectStoresInternal(ListObjectStoragePoolsCmd cmd) {

        Object id = cmd.getId();
        Object name = cmd.getStoreName();
        String provider = cmd.getProvider();
        Object keyword = cmd.getKeyword();
        Long startIndex = cmd.getStartIndex();
        Long pageSize = cmd.getPageSizeVal();

        Filter searchFilter = new Filter(ObjectStoreVO.class, "id", Boolean.TRUE, startIndex, pageSize);

        SearchBuilder<ObjectStoreVO> sb = objectStoreDao.createSearchBuilder();
        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct
        // ids
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("provider", sb.entity().getProviderName(), SearchCriteria.Op.EQ);

        SearchCriteria<ObjectStoreVO> sc = sb.create();

        if (keyword != null) {
            SearchCriteria<ObjectStoreVO> ssc = objectStoreDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("providerName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (name != null) {
            sc.setParameters("name", name);
        }

        if (provider != null) {
            sc.setParameters("provider", provider);
        }

        // search Store details by ids
        Pair<List<ObjectStoreVO>, Integer> uniqueStorePair = objectStoreDao.searchAndCount(sc, searchFilter);
        Integer count = uniqueStorePair.second();
        if (count.intValue() == 0) {
            // empty result
            return uniqueStorePair;
        }
        List<ObjectStoreVO> uniqueStores = uniqueStorePair.first();
        Long[] osIds = new Long[uniqueStores.size()];
        int i = 0;
        for (ObjectStoreVO v : uniqueStores) {
            osIds[i++] = v.getId();
        }
        List<ObjectStoreVO> objectStores = objectStoreDao.searchByIds(osIds);
        return new Pair<>(objectStores, count);
    }


    @Override
    public ListResponse<BucketResponse> searchForBuckets(ListBucketsCmd listBucketsCmd) {
        List<BucketVO> buckets = searchForBucketsInternal(listBucketsCmd);
        List<BucketResponse> bucketResponses = new ArrayList<>();
        for (BucketVO bucket : buckets) {
            bucketResponses.add(responseGenerator.createBucketResponse(bucket));
        }
        ListResponse<BucketResponse> response = new ListResponse<>();
        response.setResponses(bucketResponses, bucketResponses.size());
        return response;
    }

    private List<BucketVO> searchForBucketsInternal(ListBucketsCmd cmd) {

        Long id = cmd.getId();
        String name = cmd.getBucketName();
        Long storeId = cmd.getObjectStorageId();
        String keyword = cmd.getKeyword();
        Long startIndex = cmd.getStartIndex();
        Long pageSize = cmd.getPageSizeVal();
        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<>();

        // Verify parameters
        if (id != null) {
            BucketVO bucket = bucketDao.findById(id);
            if (bucket != null) {
                accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, bucket);
            }
        }

        List<Long> ids = getIdsListFromCmd(cmd.getId(), cmd.getIds());

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<>(cmd.getDomainId(),
                cmd.isRecursive(), null);
        accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(BucketVO.class, "id", Boolean.TRUE, startIndex, pageSize);

        SearchBuilder<BucketVO> sb = bucketDao.createSearchBuilder();
        accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct
        // ids
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);

        SearchCriteria<BucketVO> sc = sb.create();
        accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (keyword != null) {
            SearchCriteria<BucketVO> ssc = bucketDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (name != null) {
            sc.setParameters("name", name);
        }

        setIdsListToSearchCriteria(sc, ids);

        // search Volume details by ids
        Pair<List<BucketVO>, Integer> uniqueBktPair = bucketDao.searchAndCount(sc, searchFilter);
        Integer count = uniqueBktPair.second();
        if (count.intValue() == 0) {
            // empty result
            return uniqueBktPair.first();
        }
        List<BucketVO> uniqueBkts = uniqueBktPair.first();
        Long[] bktIds = new Long[uniqueBkts.size()];
        int i = 0;
        for (BucketVO b : uniqueBkts) {
            bktIds[i++] = b.getId();
        }

        return bucketDao.searchByIds(bktIds);
    }

    @Override
    public String getConfigComponentName() {
        return QueryService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {AllowUserViewDestroyedVM, UserVMDeniedDetails, UserVMReadOnlyDetails, SortKeyAscending,
                AllowUserViewAllDomainAccounts, SharePublicTemplatesWithOtherDomains, ReturnVmStatsOnVmList};
    }
}
