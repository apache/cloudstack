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
import java.util.function.Predicate;
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
import org.apache.cloudstack.api.command.admin.storage.ListImageStoresCmd;
import org.apache.cloudstack.api.command.admin.storage.ListSecondaryStagingStoresCmd;
import org.apache.cloudstack.api.command.admin.storage.ListStoragePoolsCmd;
import org.apache.cloudstack.api.command.admin.storage.ListStorageTagsCmd;
import org.apache.cloudstack.api.command.admin.template.ListTemplatesCmdByAdmin;
import org.apache.cloudstack.api.command.admin.user.ListUsersCmd;
import org.apache.cloudstack.api.command.admin.zone.ListZonesCmdByAdmin;
import org.apache.cloudstack.api.command.user.account.ListAccountsCmd;
import org.apache.cloudstack.api.command.user.account.ListProjectAccountsCmd;
import org.apache.cloudstack.api.command.user.affinitygroup.ListAffinityGroupsCmd;
import org.apache.cloudstack.api.command.user.event.ListEventsCmd;
import org.apache.cloudstack.api.command.user.iso.ListIsosCmd;
import org.apache.cloudstack.api.command.user.job.ListAsyncJobsCmd;
import org.apache.cloudstack.api.command.user.offering.ListDiskOfferingsCmd;
import org.apache.cloudstack.api.command.user.offering.ListServiceOfferingsCmd;
import org.apache.cloudstack.api.command.user.project.ListProjectInvitationsCmd;
import org.apache.cloudstack.api.command.user.project.ListProjectsCmd;
import org.apache.cloudstack.api.command.user.resource.ListDetailOptionsCmd;
import org.apache.cloudstack.api.command.user.securitygroup.ListSecurityGroupsCmd;
import org.apache.cloudstack.api.command.user.tag.ListTagsCmd;
import org.apache.cloudstack.api.command.user.template.ListTemplatesCmd;
import org.apache.cloudstack.api.command.user.vm.ListVMsCmd;
import org.apache.cloudstack.api.command.user.vmgroup.ListVMGroupsCmd;
import org.apache.cloudstack.api.command.user.volume.ListResourceDetailsCmd;
import org.apache.cloudstack.api.command.user.volume.ListVolumesCmd;
import org.apache.cloudstack.api.command.user.zone.ListZonesCmd;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.AsyncJobResponse;
import org.apache.cloudstack.api.response.DetailOptionsResponse;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.api.response.EventResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.HostTagResponse;
import org.apache.cloudstack.api.response.ImageStoreResponse;
import org.apache.cloudstack.api.response.InstanceGroupResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ManagementServerResponse;
import org.apache.cloudstack.api.response.ProjectAccountResponse;
import org.apache.cloudstack.api.response.ProjectInvitationResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ResourceDetailResponse;
import org.apache.cloudstack.api.response.ResourceIconResponse;
import org.apache.cloudstack.api.response.ResourceTagResponse;
import org.apache.cloudstack.api.response.RouterHealthCheckResultResponse;
import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.StorageTagResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateState;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.query.QueryService;
import org.apache.cloudstack.resourcedetail.dao.DiskOfferingDetailsDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.api.query.dao.AccountJoinDao;
import com.cloud.api.query.dao.AffinityGroupJoinDao;
import com.cloud.api.query.dao.AsyncJobJoinDao;
import com.cloud.api.query.dao.DataCenterJoinDao;
import com.cloud.api.query.dao.DiskOfferingJoinDao;
import com.cloud.api.query.dao.DomainJoinDao;
import com.cloud.api.query.dao.DomainRouterJoinDao;
import com.cloud.api.query.dao.HostJoinDao;
import com.cloud.api.query.dao.HostTagDao;
import com.cloud.api.query.dao.ImageStoreJoinDao;
import com.cloud.api.query.dao.InstanceGroupJoinDao;
import com.cloud.api.query.dao.ManagementServerJoinDao;
import com.cloud.api.query.dao.ProjectAccountJoinDao;
import com.cloud.api.query.dao.ProjectInvitationJoinDao;
import com.cloud.api.query.dao.ProjectJoinDao;
import com.cloud.api.query.dao.ResourceTagJoinDao;
import com.cloud.api.query.dao.SecurityGroupJoinDao;
import com.cloud.api.query.dao.ServiceOfferingJoinDao;
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
import com.cloud.api.query.vo.HostTagVO;
import com.cloud.api.query.vo.ImageStoreJoinVO;
import com.cloud.api.query.vo.InstanceGroupJoinVO;
import com.cloud.api.query.vo.ManagementServerJoinVO;
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
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DedicatedResourceVO;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.dao.EventJoinDao;
import com.cloud.exception.CloudAuthenticationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.RouterHealthCheckResult;
import com.cloud.network.VpcVirtualNetworkApplianceService;
import com.cloud.network.dao.RouterHealthCheckResultDao;
import com.cloud.network.dao.RouterHealthCheckResultVO;
import com.cloud.network.router.VirtualNetworkApplianceManager;
import com.cloud.network.security.SecurityGroupVMMapVO;
import com.cloud.network.security.dao.SecurityGroupVMMapDao;
import com.cloud.offering.DiskOffering;
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
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.StoragePoolTagVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiServiceImpl;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.StoragePoolTagsDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.template.VirtualMachineTemplate.State;
import com.cloud.template.VirtualMachineTemplate.TemplateFilter;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
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
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Component
public class QueryManagerImpl extends MutualExclusiveIdsManagerBase implements QueryService, Configurable {

    public static final Logger s_logger = Logger.getLogger(QueryManagerImpl.class);

    private static final String ID_FIELD = "id";

    @Inject
    private AccountManager _accountMgr;

    @Inject
    private ProjectManager _projectMgr;

    @Inject
    private DomainDao _domainDao;

    @Inject
    private DomainJoinDao _domainJoinDao;

    @Inject
    private UserAccountJoinDao _userAccountJoinDao;

    @Inject
    private EventJoinDao _eventJoinDao;

    @Inject
    private ResourceTagJoinDao _resourceTagJoinDao;

    @Inject
    private InstanceGroupJoinDao _vmGroupJoinDao;

    @Inject
    private UserVmJoinDao _userVmJoinDao;

    @Inject
    private UserVmDao _userVmDao;

    @Inject
    private VMInstanceDao _vmInstanceDao;

    @Inject
    private SecurityGroupJoinDao _securityGroupJoinDao;

    @Inject
    private SecurityGroupVMMapDao _securityGroupVMMapDao;

    @Inject
    private DomainRouterJoinDao _routerJoinDao;

    @Inject
    private ProjectInvitationJoinDao _projectInvitationJoinDao;

    @Inject
    private ProjectJoinDao _projectJoinDao;

    @Inject
    private ProjectDao _projectDao;

    @Inject
    private ProjectAccountDao _projectAccountDao;

    @Inject
    private ProjectAccountJoinDao _projectAccountJoinDao;

    @Inject
    private HostJoinDao _hostJoinDao;

    @Inject
    private VolumeJoinDao _volumeJoinDao;

    @Inject
    private AccountDao _accountDao;

    @Inject
    private AccountJoinDao _accountJoinDao;

    @Inject
    private AsyncJobJoinDao _jobJoinDao;

    @Inject
    private StoragePoolJoinDao _poolJoinDao;

    @Inject
    private StoragePoolTagsDao _storageTagDao;

    @Inject
    private HostTagDao _hostTagDao;

    @Inject
    private ImageStoreJoinDao _imageStoreJoinDao;

    @Inject
    private DiskOfferingJoinDao _diskOfferingJoinDao;

    @Inject
    private DiskOfferingDetailsDao _diskOfferingDetailsDao;

    @Inject
    private ServiceOfferingJoinDao _srvOfferingJoinDao;

    @Inject
    private ServiceOfferingDao _srvOfferingDao;

    @Inject
    private ServiceOfferingDetailsDao _srvOfferingDetailsDao;

    @Inject
    private DiskOfferingDao _diskOfferingDao;

    @Inject
    private DataCenterJoinDao _dcJoinDao;

    @Inject
    private DomainRouterDao _routerDao;

    @Inject
    private HighAvailabilityManager _haMgr;

    @Inject
    private VMTemplateDao _templateDao;

    @Inject
    private TemplateJoinDao _templateJoinDao;

    @Inject
    private ResourceManager _resourceMgr;
    @Inject
    private ResourceMetaDataService _resourceMetaDataMgr;

    @Inject
    private ResourceManagerUtil resourceManagerUtil;

    @Inject
    private AffinityGroupVMMapDao _affinityGroupVMMapDao;

    @Inject
    private AffinityGroupJoinDao _affinityGroupJoinDao;

    @Inject
    private DedicatedResourceDao _dedicatedDao;

    @Inject
    private DomainManager _domainMgr;

    @Inject
    private AffinityGroupDomainMapDao _affinityGroupDomainMapDao;

    @Inject
    private ResourceTagDao _resourceTagDao;

    @Inject
    private DataStoreManager dataStoreManager;

    @Inject
    ManagementServerJoinDao managementServerJoinDao;

    @Inject
    public VpcVirtualNetworkApplianceService routerService;

    @Inject
    private ResponseGenerator responseGenerator;

    @Inject
    private RouterHealthCheckResultDao routerHealthCheckResultDao;

    @Inject
    private PrimaryDataStoreDao _storagePoolDao;

    @Inject
    private StoragePoolDetailsDao _storagePoolDetailsDao;

    @Inject
    private ProjectInvitationDao projectInvitationDao;

    @Inject
    private UserDao userDao;

    @Inject
    private VirtualMachineManager virtualMachineManager;
    @Inject
    private VolumeDao volumeDao;

    @Inject
    private ResourceIconDao resourceIconDao;

    @Inject
    private ManagementServerHostDao msHostDao;


    @Inject
    EntityManager entityManager;

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
    public ListResponse<UserResponse> searchForUsers(ListUsersCmd cmd) throws PermissionDeniedException {
        Pair<List<UserAccountJoinVO>, Integer> result = searchForUsersInternal(cmd);
        ListResponse<UserResponse> response = new ListResponse<UserResponse>();
        List<UserResponse> userResponses = ViewResponseHelper.createUserResponse(CallContext.current().getCallingAccount().getDomainId(),
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

        Pair<List<UserAccountJoinVO>, Integer> result =  getUserListInternal(caller, permittedAccounts, listAll, id, username, type, accountName, state, keyword, domainId, recursive,
                null);
        ListResponse<UserResponse> response = new ListResponse<UserResponse>();
        List<UserResponse> userResponses = ViewResponseHelper.createUserResponse(CallContext.current().getCallingAccount().getDomainId(),
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

        Long domainId = cmd.getDomainId();
        boolean recursive = cmd.isRecursive();
        Long pageSizeVal = cmd.getPageSizeVal();
        Long startIndex = cmd.getStartIndex();

        Filter searchFilter = new Filter(UserAccountJoinVO.class, "id", true, startIndex, pageSizeVal);

        return getUserListInternal(caller, permittedAccounts, listAll, id, username, type, accountName, state, keyword, domainId, recursive, searchFilter);
    }

    private Pair<List<UserAccountJoinVO>, Integer> getUserListInternal(Account caller, List<Long> permittedAccounts, boolean listAll, Long id, Object username, Object type,
            String accountName, Object state, String keyword, Long domainId, boolean recursive, Filter searchFilter) {
        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(domainId, recursive, null);
        _accountMgr.buildACLSearchParameters(caller, id, accountName, null, permittedAccounts, domainIdRecursiveListProject, listAll, false);
        domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        SearchBuilder<UserAccountJoinVO> sb = _userAccountJoinDao.createSearchBuilder();
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
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

        if ((accountName == null) && (domainId != null)) {
            sb.and("domainPath", sb.entity().getDomainPath(), Op.LIKE);
        }

        SearchCriteria<UserAccountJoinVO> sc = sb.create();

        // building ACL condition
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

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

        return _userAccountJoinDao.searchAndCount(sc, searchFilter);
    }

    @Override
    public ListResponse<EventResponse> searchForEvents(ListEventsCmd cmd) {
        Pair<List<EventJoinVO>, Integer> result = searchForEventsInternal(cmd);
        ListResponse<EventResponse> response = new ListResponse<EventResponse>();
        List<EventResponse> eventResponses = ViewResponseHelper.createEventResponse(result.first().toArray(new EventJoinVO[result.first().size()]));
        response.setResponses(eventResponses, result.second());
        return response;
    }

    private Pair<List<EventJoinVO>, Integer> searchForEventsInternal(ListEventsCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        boolean isRootAdmin = _accountMgr.isRootAdmin(caller.getId());
        List<Long> permittedAccounts = new ArrayList<Long>();

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
                _accountMgr.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.ListEntry, entity.getAccountId() == caller.getId(), entity);
            }
        }

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(EventJoinVO.class, "createDate", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<EventJoinVO> sb = _eventJoinDao.createSearchBuilder();
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("levelL", sb.entity().getLevel(), SearchCriteria.Op.LIKE);
        sb.and("levelEQ", sb.entity().getLevel(), SearchCriteria.Op.EQ);
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);
        sb.and("createDateB", sb.entity().getCreateDate(), SearchCriteria.Op.BETWEEN);
        sb.and("createDateG", sb.entity().getCreateDate(), SearchCriteria.Op.GTEQ);
        sb.and("createDateL", sb.entity().getCreateDate(), SearchCriteria.Op.LTEQ);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.NEQ);
        sb.or("startId", sb.entity().getStartId(), SearchCriteria.Op.EQ);
        sb.and("createDate", sb.entity().getCreateDate(), SearchCriteria.Op.BETWEEN);
        sb.and("displayEvent", sb.entity().getDisplay(), SearchCriteria.Op.EQ);
        sb.and("archived", sb.entity().getArchived(), SearchCriteria.Op.EQ);
        sb.and("resourceId", sb.entity().getResourceId(), SearchCriteria.Op.EQ);
        sb.and("resourceType", sb.entity().getResourceType(), SearchCriteria.Op.EQ);

        SearchCriteria<EventJoinVO> sc = sb.create();
        // building ACL condition
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        // For end users display only enabled events
        if (!_accountMgr.isRootAdmin(caller.getId())) {
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
            SearchCriteria<EventJoinVO> ssc = _eventJoinDao.createSearchCriteria();
            ssc.addOr("type", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("level", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("level", SearchCriteria.Op.SC, ssc);
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

        Pair<List<EventJoinVO>, Integer> eventPair = null;
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
        } else {
            eventPair = _eventJoinDao.searchAndCount(sc, searchFilter);
        }
        return eventPair;

    }

    @Override
    public ListResponse<ResourceTagResponse> listTags(ListTagsCmd cmd) {
        Pair<List<ResourceTagJoinVO>, Integer> tags = listTagsInternal(cmd);
        ListResponse<ResourceTagResponse> response = new ListResponse<ResourceTagResponse>();
        List<ResourceTagResponse> tagResponses = ViewResponseHelper.createResourceTagResponse(false, tags.first().toArray(new ResourceTagJoinVO[tags.first().size()]));
        response.setResponses(tagResponses, tags.second());
        return response;
    }

    private Pair<List<ResourceTagJoinVO>, Integer> listTagsInternal(ListTagsCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<Long>();
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

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);

        _accountMgr.buildACLSearchParameters(caller, null, cmd.getAccountName(), projectId, permittedAccounts, domainIdRecursiveListProject, listAll, false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        Filter searchFilter = new Filter(ResourceTagJoinVO.class, "resourceType", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        SearchBuilder<ResourceTagJoinVO> sb = _resourceTagJoinDao.createSearchBuilder();
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

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
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

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

        Pair<List<ResourceTagJoinVO>, Integer> result = _resourceTagJoinDao.searchAndCount(sc, searchFilter);
        return result;
    }

    @Override
    public ListResponse<InstanceGroupResponse> searchForVmGroups(ListVMGroupsCmd cmd) {
        Pair<List<InstanceGroupJoinVO>, Integer> groups = searchForVmGroupsInternal(cmd);
        ListResponse<InstanceGroupResponse> response = new ListResponse<InstanceGroupResponse>();
        List<InstanceGroupResponse> grpResponses = ViewResponseHelper.createInstanceGroupResponse(groups.first().toArray(new InstanceGroupJoinVO[groups.first().size()]));
        response.setResponses(grpResponses, groups.second());
        return response;
    }

    private Pair<List<InstanceGroupJoinVO>, Integer> searchForVmGroupsInternal(ListVMGroupsCmd cmd) {
        Long id = cmd.getId();
        String name = cmd.getGroupName();
        String keyword = cmd.getKeyword();

        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<Long>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(InstanceGroupJoinVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        SearchBuilder<InstanceGroupJoinVO> sb = _vmGroupJoinDao.createSearchBuilder();
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);

        SearchCriteria<InstanceGroupJoinVO> sc = sb.create();
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

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
        ListResponse<UserVmResponse> response = new ListResponse<UserVmResponse>();
        ResponseView respView = ResponseView.Restricted;
        Account caller = CallContext.current().getCallingAccount();
        if (_accountMgr.isRootAdmin(caller.getId())) {
            respView = ResponseView.Full;
        }
        List<UserVmResponse> vmResponses = ViewResponseHelper.createUserVmResponse(respView, "virtualmachine", cmd.getDetails(), cmd.getAccumulate(), cmd.getShowUserData(),
                result.first().toArray(new UserVmJoinVO[result.first().size()]));

        response.setResponses(vmResponses, result.second());
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
        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<Long>();

        boolean listAll = cmd.listAll();
        Long id = cmd.getId();
        Long userId = cmd.getUserId();
        Map<String, String> tags = cmd.getTags();
        Boolean display = cmd.getDisplay();
        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, listAll, false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(UserVmJoinVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        List<Long> ids = null;
        if (cmd.getId() != null) {
            if (cmd.getIds() != null && !cmd.getIds().isEmpty()) {
                throw new InvalidParameterValueException("Specify either id or ids but not both parameters");
            }
            ids = new ArrayList<Long>();
            ids.add(cmd.getId());
        } else {
            ids = cmd.getIds();
        }

        // first search distinct vm id by using query criteria and pagination
        SearchBuilder<UserVmJoinVO> sb = _userVmJoinDao.createSearchBuilder();
        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct ids

        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        String hypervisor = cmd.getHypervisor();
        Object name = cmd.getName();
        String state = cmd.getState();
        Object zoneId = cmd.getZoneId();
        Object keyword = cmd.getKeyword();
        boolean isAdmin = false;
        boolean isRootAdmin = false;
        if (_accountMgr.isAdmin(caller.getId())) {
            isAdmin = true;
        }
        if (_accountMgr.isRootAdmin(caller.getId())) {
            isRootAdmin = true;
        }

        Object groupId = cmd.getGroupId();
        Object networkId = cmd.getNetworkId();
        if (HypervisorType.getType(hypervisor) == HypervisorType.None && hypervisor != null) {
            // invalid hypervisor type input
            throw new InvalidParameterValueException("Invalid HypervisorType " + hypervisor);
        }
        Object templateId = cmd.getTemplateId();
        Object isoId = cmd.getIsoId();
        Object vpcId = cmd.getVpcId();
        Object affinityGroupId = cmd.getAffinityGroupId();
        Object keyPairName = cmd.getKeyPairName();
        Object serviceOffId = cmd.getServiceOfferingId();
        Object securityGroupId = cmd.getSecurityGroupId();
        Object backupOfferingId = cmd.getBackupOfferingId();
        Object isHaEnabled = cmd.getHaEnabled();
        Object autoScaleVmGroupId = cmd.getAutoScaleVmGroupId();
        Object pod = null;
        Object clusterId = null;
        Object hostId = null;
        Object storageId = null;
        if (_accountMgr.isRootAdmin(caller.getId())) {
            pod = getObjectPossibleMethodValue(cmd, "getPodId");
            clusterId = getObjectPossibleMethodValue(cmd, "getClusterId");
            hostId = getObjectPossibleMethodValue(cmd, "getHostId");
            storageId = getObjectPossibleMethodValue(cmd, "getStorageId");
        }

        sb.and("displayName", sb.entity().getDisplayName(), SearchCriteria.Op.LIKE);
        sb.and("idIN", sb.entity().getId(), SearchCriteria.Op.IN);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("stateEQ", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("stateNEQ", sb.entity().getState(), SearchCriteria.Op.NEQ);
        sb.and("stateNIN", sb.entity().getState(), SearchCriteria.Op.NIN);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        sb.and("clusterId", sb.entity().getClusterId(), SearchCriteria.Op.EQ);
        sb.and("hypervisorType", sb.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        sb.and("hostIdEQ", sb.entity().getHostId(), SearchCriteria.Op.EQ);
        sb.and("templateId", sb.entity().getTemplateId(), SearchCriteria.Op.EQ);
        sb.and("isoId", sb.entity().getIsoId(), SearchCriteria.Op.EQ);
        sb.and("instanceGroupId", sb.entity().getInstanceGroupId(), SearchCriteria.Op.EQ);

        if (serviceOffId != null) {
            sb.and("serviceOfferingId", sb.entity().getServiceOfferingId(), SearchCriteria.Op.EQ);
        }

        if (backupOfferingId != null) {
            sb.and("backupOfferingId", sb.entity().getBackupOfferingId(), SearchCriteria.Op.EQ);
        }

        if (display != null) {
            sb.and("display", sb.entity().isDisplayVm(), SearchCriteria.Op.EQ);
        }

        if (isHaEnabled != null) {
            sb.and("haEnabled", sb.entity().isHaEnabled(), SearchCriteria.Op.EQ);
        }

        if (groupId != null && (Long)groupId != -1) {
            sb.and("instanceGroupId", sb.entity().getInstanceGroupId(), SearchCriteria.Op.EQ);
        }

        if (userId != null) {
            sb.and("userId", sb.entity().getUserId(), SearchCriteria.Op.EQ);
        }

        if (networkId != null) {
            sb.and("networkId", sb.entity().getNetworkId(), SearchCriteria.Op.EQ);
        }

        if (vpcId != null && networkId == null) {
            sb.and("vpcId", sb.entity().getVpcId(), SearchCriteria.Op.EQ);
        }

        if (storageId != null) {
            StoragePoolVO poolVO = _storagePoolDao.findById((Long) storageId);
            if (poolVO.getPoolType() == Storage.StoragePoolType.DatastoreCluster) {
                sb.and("poolId", sb.entity().getPoolId(), SearchCriteria.Op.IN);
            } else {
                sb.and("poolId", sb.entity().getPoolId(), SearchCriteria.Op.EQ);
            }
        }

        if (affinityGroupId != null) {
            sb.and("affinityGroupId", sb.entity().getAffinityGroupId(), SearchCriteria.Op.EQ);
        }

        if (keyPairName != null) {
            sb.and("keyPairName", sb.entity().getKeypairNames(), SearchCriteria.Op.FIND_IN_SET);
        }

        if (!isRootAdmin) {
            sb.and("displayVm", sb.entity().isDisplayVm(), SearchCriteria.Op.EQ);
        }

        if (securityGroupId != null) {
            sb.and("securityGroupId", sb.entity().getSecurityGroupId(), SearchCriteria.Op.EQ);
        }

        if (autoScaleVmGroupId != null) {
            sb.and("autoScaleVmGroupId", sb.entity().getAutoScaleVmGroupId(), SearchCriteria.Op.EQ);
        }

        // populate the search criteria with the values passed in
        SearchCriteria<UserVmJoinVO> sc = sb.create();

        // building ACL condition
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (tags != null && !tags.isEmpty()) {
            SearchCriteria<UserVmJoinVO> tagSc = _userVmJoinDao.createSearchCriteria();
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                SearchCriteria<UserVmJoinVO> tsc = _userVmJoinDao.createSearchCriteria();
                tsc.addAnd("tagKey", SearchCriteria.Op.EQ, entry.getKey());
                tsc.addAnd("tagValue", SearchCriteria.Op.EQ, entry.getValue());
                tagSc.addOr("tagKey", SearchCriteria.Op.SC, tsc);
            }
            sc.addAnd("tagKey", SearchCriteria.Op.SC, tagSc);
        }

        if (groupId != null && (Long)groupId != -1) {
            sc.setParameters("instanceGroupId", groupId);
        }

        if (keyword != null) {
            SearchCriteria<UserVmJoinVO> ssc = _userVmJoinDao.createSearchCriteria();
            String likeKeyword = String.format("%%%s%%", keyword);
            ssc.addOr("displayName", SearchCriteria.Op.LIKE, likeKeyword);
            ssc.addOr("name", SearchCriteria.Op.LIKE, likeKeyword);
            if (isRootAdmin) {
                ssc.addOr("instanceName", SearchCriteria.Op.LIKE, likeKeyword);
            }
            ssc.addOr("ipAddress", SearchCriteria.Op.LIKE, likeKeyword);
            ssc.addOr("publicIpAddress", SearchCriteria.Op.LIKE, likeKeyword);
            ssc.addOr("ip6Address", SearchCriteria.Op.LIKE, likeKeyword);
            ssc.addOr("state", SearchCriteria.Op.EQ, keyword);
            sc.addAnd("displayName", SearchCriteria.Op.SC, ssc);
        }

        if (serviceOffId != null) {
            sc.setParameters("serviceOfferingId", serviceOffId);
        }

        if (backupOfferingId != null) {
            sc.setParameters("backupOfferingId", backupOfferingId);
        }

        if (securityGroupId != null) {
            sc.setParameters("securityGroupId", securityGroupId);
        }

        if (autoScaleVmGroupId != null) {
            sc.setParameters("autoScaleVmGroupId", autoScaleVmGroupId);
        }

        if (display != null) {
            sc.setParameters("display", display);
        }

        if (isHaEnabled != null) {
            sc.setParameters("haEnabled", isHaEnabled);
        }

        if (ids != null && !ids.isEmpty()) {
            sc.setParameters("idIN", ids.toArray());
        }

        if (templateId != null) {
            sc.setParameters("templateId", templateId);
        }

        if (isoId != null) {
            sc.setParameters("isoId", isoId);
        }

        if (userId != null) {
            sc.setParameters("userId", userId);
        }

        if (networkId != null) {
            sc.setParameters("networkId", networkId);
        }

        if (vpcId != null && networkId == null) {
            sc.setParameters("vpcId", vpcId);
        }

        if (name != null) {
            sc.setParameters("name", name);
        }

        if (state != null) {
            if (state.equalsIgnoreCase("present")) {
                sc.setParameters("stateNIN", "Destroyed", "Expunging");
            } else {
                sc.setParameters("stateEQ", state);
            }
        }

        if (hypervisor != null) {
            sc.setParameters("hypervisorType", hypervisor);
        }

        // Don't show Destroyed and Expunging vms to the end user if the AllowUserViewDestroyedVM flag is not set.
        if (!isAdmin && !AllowUserViewDestroyedVM.valueIn(caller.getAccountId())) {
            sc.setParameters("stateNIN", "Destroyed", "Expunging");
        }

        if (zoneId != null) {
            sc.setParameters("dataCenterId", zoneId);
        }

        if (affinityGroupId != null) {
            sc.setParameters("affinityGroupId", affinityGroupId);
        }

        if (keyPairName != null) {
            sc.setParameters("keyPairName", keyPairName);
        }

        if (_accountMgr.isRootAdmin(caller.getId())) {
            if (pod != null) {
                sc.setParameters("podId", pod);

                if (state == null) {
                    sc.setParameters("stateNEQ", "Destroyed");
                }
            }

            if (clusterId != null) {
                sc.setParameters("clusterId", clusterId);
            }

            if (hostId != null) {
                sc.setParameters("hostIdEQ", hostId);
            }

            if (storageId != null) {
                StoragePoolVO poolVO = _storagePoolDao.findById((Long) storageId);
                if (poolVO.getPoolType() == Storage.StoragePoolType.DatastoreCluster) {
                    List<StoragePoolVO> childDatastores = _storagePoolDao.listChildStoragePoolsInDatastoreCluster((Long) storageId);
                    List<Long> childDatastoreIds = childDatastores.stream().map(mo -> mo.getId()).collect(Collectors.toList());
                    sc.setParameters("poolId", childDatastoreIds.toArray());
                } else {
                    sc.setParameters("poolId", storageId);
                }
            }
        }

        if (!isRootAdmin) {
            sc.setParameters("displayVm", 1);
        }
        // search vm details by ids
        Pair<List<UserVmJoinVO>, Integer> uniqueVmPair = _userVmJoinDao.searchAndDistinctCount(sc, searchFilter);
        Integer count = uniqueVmPair.second();
        if (count.intValue() == 0) {
            // handle empty result cases
            return uniqueVmPair;
        }
        List<UserVmJoinVO> uniqueVms = uniqueVmPair.first();
        Long[] vmIds = new Long[uniqueVms.size()];
        int i = 0;
        for (UserVmJoinVO v : uniqueVms) {
            vmIds[i++] = v.getId();
        }
        List<UserVmJoinVO> vms = _userVmJoinDao.searchByIds(vmIds);
        return new Pair<List<UserVmJoinVO>, Integer>(vms, count);
    }

    @Override
    public ListResponse<SecurityGroupResponse> searchForSecurityGroups(ListSecurityGroupsCmd cmd) {
        Pair<List<SecurityGroupJoinVO>, Integer> result = searchForSecurityGroupsInternal(cmd);
        ListResponse<SecurityGroupResponse> response = new ListResponse<SecurityGroupResponse>();
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
        List<Long> permittedAccounts = new ArrayList<Long>();
        Map<String, String> tags = cmd.getTags();

        if (instanceId != null) {
            UserVmVO userVM = _userVmDao.findById(instanceId);
            if (userVM == null) {
                throw new InvalidParameterValueException("Unable to list network groups for virtual machine instance " + instanceId + "; instance not found.");
            }
            _accountMgr.checkAccess(caller, null, true, userVM);
            return listSecurityGroupRulesByVM(instanceId.longValue(), cmd.getStartIndex(), cmd.getPageSizeVal());
        }

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(SecurityGroupJoinVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<SecurityGroupJoinVO> sb = _securityGroupJoinDao.createSearchBuilder();
        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct
        // ids
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);

        SearchCriteria<SecurityGroupJoinVO> sc = sb.create();
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

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
        return new Pair<List<SecurityGroupJoinVO>, Integer>(sgs, count);
    }

    private Pair<List<SecurityGroupJoinVO>, Integer> listSecurityGroupRulesByVM(long vmId, long pageInd, long pageSize) {
        Filter sf = new Filter(SecurityGroupVMMapVO.class, null, true, pageInd, pageSize);
        Pair<List<SecurityGroupVMMapVO>, Integer> sgVmMappingPair = _securityGroupVMMapDao.listByInstanceId(vmId, sf);
        Integer count = sgVmMappingPair.second();
        if (count.intValue() == 0) {
            // handle empty result cases
            return new Pair<List<SecurityGroupJoinVO>, Integer>(new ArrayList<SecurityGroupJoinVO>(), count);
        }
        List<SecurityGroupVMMapVO> sgVmMappings = sgVmMappingPair.first();
        Long[] sgIds = new Long[sgVmMappings.size()];
        int i = 0;
        for (SecurityGroupVMMapVO sgVm : sgVmMappings) {
            sgIds[i++] = sgVm.getSecurityGroupId();
        }
        List<SecurityGroupJoinVO> sgs = _securityGroupJoinDao.searchByIds(sgIds);
        return new Pair<List<SecurityGroupJoinVO>, Integer>(sgs, count);
    }

    @Override
    public ListResponse<DomainRouterResponse> searchForRouters(ListRoutersCmd cmd) {
        Pair<List<DomainRouterJoinVO>, Integer> result = searchForRoutersInternal(cmd, cmd.getId(), cmd.getRouterName(), cmd.getState(), cmd.getZoneId(), cmd.getPodId(), cmd.getClusterId(),
                cmd.getHostId(), cmd.getKeyword(), cmd.getNetworkId(), cmd.getVpcId(), cmd.getForVpc(), cmd.getRole(), cmd.getVersion(), cmd.isHealthCheckFailed());
        ListResponse<DomainRouterResponse> response = new ListResponse<DomainRouterResponse>();
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
        ListResponse<DomainRouterResponse> response = new ListResponse<DomainRouterResponse>();
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
        List<Long> permittedAccounts = new ArrayList<Long>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
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
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

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
                return new Pair<List<DomainRouterJoinVO>, Integer>(Collections.emptyList(), 0);
            }
        }

        SearchCriteria<DomainRouterJoinVO> sc = sb.create();
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

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
        return new Pair<List<DomainRouterJoinVO>, Integer>(vrs, count);
    }

    @Override
    public ListResponse<ProjectResponse> listProjects(ListProjectsCmd cmd) {
        Pair<List<ProjectJoinVO>, Integer> projects = listProjectsInternal(cmd);
        ListResponse<ProjectResponse> response = new ListResponse<ProjectResponse>();
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

        if (_accountMgr.isAdmin(caller.getId())) {
            if (domainId != null) {
                DomainVO domain = _domainDao.findById(domainId);
                if (domain == null) {
                    throw new InvalidParameterValueException("Domain id=" + domainId + " doesn't exist in the system");
                }

                _accountMgr.checkAccess(caller, domain);

                if (accountName != null) {
                    Account owner = _accountMgr.getActiveAccountByName(accountName, domainId);
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

        if (domainId == null && accountId == null && (_accountMgr.isNormalUser(caller.getId()) || !listAll)) {
            accountId = caller.getId();
            userId = user.getId();
        } else if (_accountMgr.isDomainAdmin(caller.getId()) || (isRecursive && !listAll)) {
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
        return new Pair<List<ProjectJoinVO>, Integer>(prjs, count);
    }

    @Override
    public ListResponse<ProjectInvitationResponse> listProjectInvitations(ListProjectInvitationsCmd cmd) {
        Pair<List<ProjectInvitationJoinVO>, Integer> invites = listProjectInvitationsInternal(cmd);
        ListResponse<ProjectInvitationResponse> response = new ListResponse<ProjectInvitationResponse>();
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
        List<Long> permittedAccounts = new ArrayList<Long>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(domainId, isRecursive, null);
        _accountMgr.buildACLSearchParameters(caller, id, accountName, projectId, permittedAccounts, domainIdRecursiveListProject, listAll, true);
        domainId = domainIdRecursiveListProject.first();
        isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(ProjectInvitationJoinVO.class, "id", true, startIndex, pageSizeVal);
        SearchBuilder<ProjectInvitationJoinVO> sb = _projectInvitationJoinDao.createSearchBuilder();
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        ProjectInvitation invitation = projectInvitationDao.findByUserIdProjectId(callingUser.getId(), callingUser.getAccountId(), projectId == null ? -1 : projectId);
        sb.and("projectId", sb.entity().getProjectId(), SearchCriteria.Op.EQ);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("created", sb.entity().getCreated(), SearchCriteria.Op.GT);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);

        SearchCriteria<ProjectInvitationJoinVO> sc = sb.create();
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

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
        ListResponse<ProjectAccountResponse> response = new ListResponse<ProjectAccountResponse>();
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
        if (!_accountMgr.isAdmin(caller.getId()) && _projectAccountDao.findByProjectIdUserId(projectId, callingUser.getAccountId(), callingUser.getId()) == null &&
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
        s_logger.debug(">>>Searching for hosts>>>");
        Pair<List<HostJoinVO>, Integer> hosts = searchForServersInternal(cmd);
        ListResponse<HostResponse> response = new ListResponse<HostResponse>();
        s_logger.debug(">>>Generating Response>>>");
        List<HostResponse> hostResponses = ViewResponseHelper.createHostResponse(cmd.getDetails(), hosts.first().toArray(new HostJoinVO[hosts.first().size()]));
        response.setResponses(hostResponses, hosts.second());
        return response;
    }

    public Pair<List<HostJoinVO>, Integer> searchForServersInternal(ListHostsCmd cmd) {

        Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), cmd.getZoneId());
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

        Filter searchFilter = new Filter(HostJoinVO.class, "id", Boolean.TRUE, startIndex, pageSize);

        SearchBuilder<HostJoinVO> sb = _hostJoinDao.createSearchBuilder();
        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct
        // ids
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.LIKE);
        sb.and("status", sb.entity().getStatus(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getZoneId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        sb.and("clusterId", sb.entity().getClusterId(), SearchCriteria.Op.EQ);
        sb.and("oobmEnabled", sb.entity().isOutOfBandManagementEnabled(), SearchCriteria.Op.EQ);
        sb.and("powerState", sb.entity().getOutOfBandManagementPowerState(), SearchCriteria.Op.EQ);
        sb.and("resourceState", sb.entity().getResourceState(), SearchCriteria.Op.EQ);
        sb.and("hypervisor_type", sb.entity().getHypervisorType(), SearchCriteria.Op.EQ);

        String haTag = _haMgr.getHaTag();
        if (haHosts != null && haTag != null && !haTag.isEmpty()) {
            if ((Boolean)haHosts) {
                sb.and("tag", sb.entity().getTag(), SearchCriteria.Op.EQ);
            } else {
                sb.and().op("tag", sb.entity().getTag(), SearchCriteria.Op.NEQ);
                sb.or("tagNull", sb.entity().getTag(), SearchCriteria.Op.NULL);
                sb.cp();
            }

        }

        SearchCriteria<HostJoinVO> sc = sb.create();

        if (keyword != null) {
            SearchCriteria<HostJoinVO> ssc = _hostJoinDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("status", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("type", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (name != null) {
            sc.setParameters("name", name);
        }
        if (type != null) {
            sc.setParameters("type", "%" + type);
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
            sc.setParameters("oobmEnabled", outOfBandManagementEnabled);
        }

        if (powerState != null) {
            sc.setParameters("powerState", powerState);
        }

        if (resourceState != null) {
            sc.setParameters("resourceState", resourceState);
        }

        if (haHosts != null && haTag != null && !haTag.isEmpty()) {
            sc.setParameters("tag", haTag);
        }

        if (hypervisorType != HypervisorType.None && hypervisorType != HypervisorType.Any) {
            sc.setParameters("hypervisor_type", hypervisorType);
        }
        // search host details by ids
        Pair<List<HostJoinVO>, Integer> uniqueHostPair = _hostJoinDao.searchAndCount(sc, searchFilter);
        Integer count = uniqueHostPair.second();
        if (count.intValue() == 0) {
            // handle empty result cases
            return uniqueHostPair;
        }
        List<HostJoinVO> uniqueHosts = uniqueHostPair.first();
        Long[] hostIds = new Long[uniqueHosts.size()];
        int i = 0;
        for (HostJoinVO v : uniqueHosts) {
            hostIds[i++] = v.getId();
        }
        List<HostJoinVO> hosts = _hostJoinDao.searchByIds(hostIds);
        return new Pair<List<HostJoinVO>, Integer>(hosts, count);

    }

    @Override
    public ListResponse<VolumeResponse> searchForVolumes(ListVolumesCmd cmd) {
        Pair<List<VolumeJoinVO>, Integer> result = searchForVolumesInternal(cmd);
        ListResponse<VolumeResponse> response = new ListResponse<VolumeResponse>();

        ResponseView respView = cmd.getResponseView();
        Account account = CallContext.current().getCallingAccount();
        if (_accountMgr.isRootAdmin(account.getAccountId())) {
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

        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<Long>();

        Long id = cmd.getId();
        Long vmInstanceId = cmd.getVirtualMachineId();
        String name = cmd.getVolumeName();
        String keyword = cmd.getKeyword();
        String type = cmd.getType();
        Map<String, String> tags = cmd.getTags();
        String storageId = cmd.getStorageId();
        Long clusterId = cmd.getClusterId();
        Long diskOffId = cmd.getDiskOfferingId();
        Boolean display = cmd.getDisplay();
        String state = cmd.getState();
        boolean shouldListSystemVms = shouldListSystemVms(cmd, caller.getId());

        Long zoneId = cmd.getZoneId();
        Long podId = cmd.getPodId();

        List<Long> ids = getIdsListFromCmd(cmd.getId(), cmd.getIds());

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        Filter searchFilter = new Filter(VolumeJoinVO.class, "created", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        // hack for now, this should be done better but due to needing a join I
        // opted to
        // do this quickly and worry about making it pretty later
        SearchBuilder<VolumeJoinVO> sb = _volumeJoinDao.createSearchBuilder();
        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct
        // ids to get
        // number of
        // records with
        // pagination
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("idIN", sb.entity().getId(), SearchCriteria.Op.IN);
        sb.and("volumeType", sb.entity().getVolumeType(), SearchCriteria.Op.LIKE);
        sb.and("uuid", sb.entity().getUuid(), SearchCriteria.Op.NNULL);
        sb.and("instanceId", sb.entity().getVmId(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        if (storageId != null) {
            StoragePoolVO poolVO = _storagePoolDao.findByUuid(storageId);
            if (poolVO.getPoolType() == Storage.StoragePoolType.DatastoreCluster) {
                sb.and("storageId", sb.entity().getPoolUuid(), SearchCriteria.Op.IN);
            } else {
                sb.and("storageId", sb.entity().getPoolUuid(), SearchCriteria.Op.EQ);
            }
        }
        sb.and("diskOfferingId", sb.entity().getDiskOfferingId(), SearchCriteria.Op.EQ);
        sb.and("display", sb.entity().isDisplayVolume(), SearchCriteria.Op.EQ);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("stateNEQ", sb.entity().getState(), SearchCriteria.Op.NEQ);

        if (!shouldListSystemVms) {
            sb.and().op("systemUse", sb.entity().isSystemUse(), SearchCriteria.Op.NEQ);
            sb.or("nulltype", sb.entity().isSystemUse(), SearchCriteria.Op.NULL);
            sb.cp();

            sb.and().op("type", sb.entity().getVmType(), SearchCriteria.Op.NIN);
            sb.or("nulltype", sb.entity().getVmType(), SearchCriteria.Op.NULL);
            sb.cp();
        }

        // now set the SC criteria...
        SearchCriteria<VolumeJoinVO> sc = sb.create();
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (keyword != null) {
            SearchCriteria<VolumeJoinVO> ssc = _volumeJoinDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("volumeType", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.setParameters("name", name);
        }

        if (display != null) {
            sc.setParameters("display", display);
        }

        setIdsListToSearchCriteria(sc, ids);

        if (!shouldListSystemVms) {
            sc.setParameters("systemUse", 1);
            sc.setParameters("type", VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm, VirtualMachine.Type.DomainRouter);
        }

        if (tags != null && !tags.isEmpty()) {
            SearchCriteria<VolumeJoinVO> tagSc = _volumeJoinDao.createSearchCriteria();
            for (String key : tags.keySet()) {
                SearchCriteria<VolumeJoinVO> tsc = _volumeJoinDao.createSearchCriteria();
                tsc.addAnd("tagKey", SearchCriteria.Op.EQ, key);
                tsc.addAnd("tagValue", SearchCriteria.Op.EQ, tags.get(key));
                tagSc.addOr("tagKey", SearchCriteria.Op.SC, tsc);
            }
            sc.addAnd("tagKey", SearchCriteria.Op.SC, tagSc);
        }

        if (diskOffId != null) {
            sc.setParameters("diskOfferingId", diskOffId);
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
        if (podId != null) {
            sc.setParameters("podId", podId);
        }

        if (storageId != null) {
            StoragePoolVO poolVO = _storagePoolDao.findByUuid(storageId);
            if (poolVO.getPoolType() == Storage.StoragePoolType.DatastoreCluster) {
                List<StoragePoolVO> childDatastores = _storagePoolDao.listChildStoragePoolsInDatastoreCluster(poolVO.getId());
                List<String> childDatastoreIds = childDatastores.stream().map(mo -> mo.getUuid()).collect(Collectors.toList());
                sc.setParameters("storageId", childDatastoreIds.toArray());
            } else {
                sc.setParameters("storageId", storageId);
            }
        }

        if (clusterId != null) {
            sc.setParameters("clusterId", clusterId);
        }

        if (state != null) {
            sc.setParameters("state", state);
        } else if (!_accountMgr.isAdmin(caller.getId())) {
            sc.setParameters("stateNEQ", Volume.State.Expunged);
        }

        // search Volume details by ids
        Pair<List<VolumeJoinVO>, Integer> uniqueVolPair = _volumeJoinDao.searchAndCount(sc, searchFilter);
        Integer count = uniqueVolPair.second();
        if (count.intValue() == 0) {
            // empty result
            return uniqueVolPair;
        }
        List<VolumeJoinVO> uniqueVols = uniqueVolPair.first();
        Long[] vrIds = new Long[uniqueVols.size()];
        int i = 0;
        for (VolumeJoinVO v : uniqueVols) {
            vrIds[i++] = v.getId();
        }
        List<VolumeJoinVO> vrs = _volumeJoinDao.searchByIds(vrIds);
        return new Pair<List<VolumeJoinVO>, Integer>(vrs, count);
    }

    private boolean shouldListSystemVms(ListVolumesCmd cmd, Long callerId) {
        return Boolean.TRUE.equals(cmd.getListSystemVms()) && _accountMgr.isRootAdmin(callerId);
    }

    @Override
    public ListResponse<DomainResponse> searchForDomains(ListDomainsCmd cmd) {
        Pair<List<DomainJoinVO>, Integer> result = searchForDomainsInternal(cmd);
        ListResponse<DomainResponse> response = new ListResponse<DomainResponse>();

        ResponseView respView = ResponseView.Restricted;
        if (cmd instanceof ListDomainsCmdByAdmin) {
            respView = ResponseView.Full;
        }

        List<DomainResponse> domainResponses = ViewResponseHelper.createDomainResponse(respView, cmd.getDetails(), result.first());
        response.setResponses(domainResponses, result.second());
        return response;
    }

    private Pair<List<DomainJoinVO>, Integer> searchForDomainsInternal(ListDomainsCmd cmd) {
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
            _accountMgr.checkAccess(caller, domain);
        } else {
            if (caller.getType() != Account.Type.ADMIN) {
                domainId = caller.getDomainId();
            }
            if (listAll) {
                isRecursive = true;
            }
        }

        Filter searchFilter = new Filter(DomainJoinVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        String domainName = cmd.getDomainName();
        Integer level = cmd.getLevel();
        Object keyword = cmd.getKeyword();

        SearchBuilder<DomainJoinVO> sb = _domainJoinDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("level", sb.entity().getLevel(), SearchCriteria.Op.EQ);
        sb.and("path", sb.entity().getPath(), SearchCriteria.Op.LIKE);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);

        SearchCriteria<DomainJoinVO> sc = sb.create();

        if (keyword != null) {
            SearchCriteria<DomainJoinVO> ssc = _domainJoinDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
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

        return _domainJoinDao.searchAndCount(sc, searchFilter);
    }

    @Override
    public ListResponse<AccountResponse> searchForAccounts(ListAccountsCmd cmd) {
        Pair<List<AccountJoinVO>, Integer> result = searchForAccountsInternal(cmd);
        ListResponse<AccountResponse> response = new ListResponse<AccountResponse>();

        ResponseView respView = ResponseView.Restricted;
        if (cmd instanceof ListAccountsCmdByAdmin) {
            respView = ResponseView.Full;
        }

        List<AccountResponse> accountResponses = ViewResponseHelper.createAccountResponse(respView, cmd.getDetails(), result.first().toArray(new AccountJoinVO[result.first().size()]));
        response.setResponses(accountResponses, result.second());
        return response;
    }

    private Pair<List<AccountJoinVO>, Integer> searchForAccountsInternal(ListAccountsCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Long domainId = cmd.getDomainId();
        Long accountId = cmd.getId();
        String accountName = cmd.getSearchName();
        boolean isRecursive = cmd.isRecursive();
        boolean listAll = cmd.listAll();
        boolean callerIsAdmin = _accountMgr.isAdmin(caller.getId());
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
            _accountMgr.checkAccess(caller, domain);
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
            _accountMgr.checkAccess(caller, null, true, account);
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
            _accountMgr.checkAccess(caller, null, true, account);
        }

        Filter searchFilter = new Filter(AccountJoinVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object type = cmd.getAccountType();
        Object state = cmd.getState();
        Object isCleanupRequired = cmd.isCleanupRequired();
        Object keyword = cmd.getKeyword();

        SearchBuilder<AccountJoinVO> sb = _accountJoinDao.createSearchBuilder();
        sb.and("accountName", sb.entity().getAccountName(), SearchCriteria.Op.EQ);
        sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("needsCleanup", sb.entity().isNeedsCleanup(), SearchCriteria.Op.EQ);
        sb.and("typeNEQ", sb.entity().getType(), SearchCriteria.Op.NEQ);
        sb.and("idNEQ", sb.entity().getId(), SearchCriteria.Op.NEQ);
        sb.and("type2NEQ", sb.entity().getType(), SearchCriteria.Op.NEQ);

        if (domainId != null && isRecursive) {
            sb.and("path", sb.entity().getDomainPath(), SearchCriteria.Op.LIKE);
        }

        SearchCriteria<AccountJoinVO> sc = sb.create();

        // don't return account of type project to the end user
        sc.setParameters("typeNEQ", Account.Type.PROJECT);

        // don't return system account...
        sc.setParameters("idNEQ", Account.ACCOUNT_ID_SYSTEM);

        // do not return account of type domain admin to the end user
        if (!callerIsAdmin) {
            sc.setParameters("type2NEQ", Account.Type.DOMAIN_ADMIN);
        }

        if (keyword != null) {
            SearchCriteria<AccountJoinVO> ssc = _accountJoinDao.createSearchCriteria();
            ssc.addOr("accountName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("accountName", SearchCriteria.Op.SC, ssc);
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
                sc.setParameters("path", domain.getPath() + "%");
            } else {
                sc.setParameters("domainId", domainId);
            }
        }

        return _accountJoinDao.searchAndCount(sc, searchFilter);
    }

    @Override
    public ListResponse<AsyncJobResponse> searchForAsyncJobs(ListAsyncJobsCmd cmd) {
        Pair<List<AsyncJobJoinVO>, Integer> result = searchForAsyncJobsInternal(cmd);
        ListResponse<AsyncJobResponse> response = new ListResponse<AsyncJobResponse>();
        List<AsyncJobResponse> jobResponses = ViewResponseHelper.createAsyncJobResponse(result.first().toArray(new AsyncJobJoinVO[result.first().size()]));
        response.setResponses(jobResponses, result.second());
        return response;
    }

    private Pair<List<AsyncJobJoinVO>, Integer> searchForAsyncJobsInternal(ListAsyncJobsCmd cmd) {

        Account caller = CallContext.current().getCallingAccount();

        List<Long> permittedAccounts = new ArrayList<Long>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, null, cmd.getAccountName(), null, permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
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
        Pair<List<StoragePoolJoinVO>, Integer> result = searchForStoragePoolsInternal(cmd);
        ListResponse<StoragePoolResponse> response = new ListResponse<StoragePoolResponse>();

        List<StoragePoolResponse> poolResponses = ViewResponseHelper.createStoragePoolResponse(result.first().toArray(new StoragePoolJoinVO[result.first().size()]));
        for (StoragePoolResponse poolResponse : poolResponses) {
            DataStore store = dataStoreManager.getPrimaryDataStore(poolResponse.getId());
            if (store != null) {
                DataStoreDriver driver = store.getDriver();
                if (driver != null && driver.getCapabilities() != null) {
                    Map<String, String> caps = driver.getCapabilities();
                    if (Storage.StoragePoolType.NetworkFilesystem.toString().equals(poolResponse.getType()) &&
                        HypervisorType.VMware.toString().equals(poolResponse.getHypervisor())) {
                        StoragePoolVO pool = _storagePoolDao.findPoolByUUID(poolResponse.getId());
                        StoragePoolDetailVO detail = _storagePoolDetailsDao.findDetail(pool.getId(), Storage.Capability.HARDWARE_ACCELERATION.toString());
                        if (detail != null) {
                            caps.put(Storage.Capability.HARDWARE_ACCELERATION.toString(), detail.getValue());
                        }
                    }
                    poolResponse.setCaps(caps);
                }
            }
        }

        response.setResponses(poolResponses, result.second());
        return response;
    }

    private Pair<List<StoragePoolJoinVO>, Integer> searchForStoragePoolsInternal(ListStoragePoolsCmd cmd) {
        ScopeType scopeType = ScopeType.validateAndGetScopeType(cmd.getScope());
        StoragePoolStatus status = StoragePoolStatus.validateAndGetStatus(cmd.getStatus());

        Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), cmd.getZoneId());
        Long id = cmd.getId();
        String name = cmd.getStoragePoolName();
        String path = cmd.getPath();
        Long pod = cmd.getPodId();
        Long cluster = cmd.getClusterId();
        String address = cmd.getIpAddress();
        String keyword = cmd.getKeyword();
        Long startIndex = cmd.getStartIndex();
        Long pageSize = cmd.getPageSizeVal();

        Filter searchFilter = new Filter(StoragePoolJoinVO.class, "id", Boolean.TRUE, startIndex, pageSize);

        // search & count Pool details by ids
        Pair<List<StoragePoolJoinVO>, Integer> uniquePoolPair = _poolJoinDao.searchAndCount(id, name, zoneId, path, pod,
                cluster, address, scopeType, status, keyword, searchFilter);

        Integer count = uniquePoolPair.second();
        if (count.intValue() == 0) {
            // empty result
            return uniquePoolPair;
        }
        List<StoragePoolJoinVO> uniquePools = uniquePoolPair.first();
        Long[] vrIds = new Long[uniquePools.size()];
        int i = 0;
        for (StoragePoolJoinVO v : uniquePools) {
            vrIds[i++] = v.getId();
        }
        List<StoragePoolJoinVO> vrs = _poolJoinDao.searchByIds(vrIds);
        return new Pair<List<StoragePoolJoinVO>, Integer>(vrs, count);

    }

    @Override
    public ListResponse<StorageTagResponse> searchForStorageTags(ListStorageTagsCmd cmd) {
        Pair<List<StoragePoolTagVO>, Integer> result = searchForStorageTagsInternal(cmd);
        ListResponse<StorageTagResponse> response = new ListResponse<StorageTagResponse>();
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

        return new Pair<List<StoragePoolTagVO>, Integer>(vrs, count);
    }

    @Override
    public ListResponse<HostTagResponse> searchForHostTags(ListHostTagsCmd cmd) {
        Pair<List<HostTagVO>, Integer> result = searchForHostTagsInternal(cmd);
        ListResponse<HostTagResponse> response = new ListResponse<HostTagResponse>();
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

        return new Pair<List<HostTagVO>, Integer>(vrs, count);
    }

    @Override
    public ListResponse<ImageStoreResponse> searchForImageStores(ListImageStoresCmd cmd) {
        Pair<List<ImageStoreJoinVO>, Integer> result = searchForImageStoresInternal(cmd);
        ListResponse<ImageStoreResponse> response = new ListResponse<ImageStoreResponse>();

        List<ImageStoreResponse> poolResponses = ViewResponseHelper.createImageStoreResponse(result.first().toArray(new ImageStoreJoinVO[result.first().size()]));
        response.setResponses(poolResponses, result.second());
        return response;
    }

    private Pair<List<ImageStoreJoinVO>, Integer> searchForImageStoresInternal(ListImageStoresCmd cmd) {

        Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), cmd.getZoneId());
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
        return new Pair<List<ImageStoreJoinVO>, Integer>(vrs, count);

    }

    @Override
    public ListResponse<ImageStoreResponse> searchForSecondaryStagingStores(ListSecondaryStagingStoresCmd cmd) {
        Pair<List<ImageStoreJoinVO>, Integer> result = searchForCacheStoresInternal(cmd);
        ListResponse<ImageStoreResponse> response = new ListResponse<ImageStoreResponse>();

        List<ImageStoreResponse> poolResponses = ViewResponseHelper.createImageStoreResponse(result.first().toArray(new ImageStoreJoinVO[result.first().size()]));
        response.setResponses(poolResponses, result.second());
        return response;
    }

    private Pair<List<ImageStoreJoinVO>, Integer> searchForCacheStoresInternal(ListSecondaryStagingStoresCmd cmd) {

        Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), cmd.getZoneId());
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
        return new Pair<List<ImageStoreJoinVO>, Integer>(vrs, count);

    }

    @Override
    public ListResponse<DiskOfferingResponse> searchForDiskOfferings(ListDiskOfferingsCmd cmd) {
        Pair<List<DiskOfferingJoinVO>, Integer> result = searchForDiskOfferingsInternal(cmd);
        ListResponse<DiskOfferingResponse> response = new ListResponse<DiskOfferingResponse>();
        List<DiskOfferingResponse> offeringResponses = ViewResponseHelper.createDiskOfferingResponse(result.first().toArray(new DiskOfferingJoinVO[result.first().size()]));
        response.setResponses(offeringResponses, result.second());
        return response;
    }

    private Pair<List<DiskOfferingJoinVO>, Integer> searchForDiskOfferingsInternal(ListDiskOfferingsCmd cmd) {
        // Note
        // The list method for offerings is being modified in accordance with
        // discussion with Will/Kevin
        // For now, we will be listing the following based on the usertype
        // 1. For root, we will list all offerings
        // 2. For domainAdmin and regular users, we will list everything in
        // their domains+parent domains ... all the way
        // till
        // root

        Filter searchFilter = new Filter(DiskOfferingJoinVO.class, "sortKey", SortKeyAscending.value(), cmd.getStartIndex(), cmd.getPageSizeVal());
        searchFilter.addOrderBy(DiskOfferingJoinVO.class, "id", true);
        SearchCriteria<DiskOfferingJoinVO> sc = _diskOfferingJoinDao.createSearchCriteria();
        sc.addAnd("computeOnly", Op.EQ, false);

        Account account = CallContext.current().getCallingAccount();
        Object name = cmd.getDiskOfferingName();
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();
        Long domainId = cmd.getDomainId();
        Boolean isRootAdmin = _accountMgr.isRootAdmin(account.getAccountId());
        Boolean isRecursive = cmd.isRecursive();
        Long zoneId = cmd.getZoneId();
        Long volumeId = cmd.getVolumeId();
        Long storagePoolId = cmd.getStoragePoolId();
        Boolean encrypt = cmd.getEncrypt();
        // Keeping this logic consistent with domain specific zones
        // if a domainId is provided, we just return the disk offering
        // associated with this domain
        if (domainId != null) {
            if (_accountMgr.isRootAdmin(account.getId()) || isPermissible(account.getDomainId(), domainId)) {
                // check if the user's domain == do's domain || user's domain is
                // a child of so's domain for non-root users
                sc.addAnd("domainId", Op.FIND_IN_SET, String.valueOf(domainId));
                if (!isRootAdmin) {
                    sc.addAnd("displayOffering", SearchCriteria.Op.EQ, 1);
                }
                return _diskOfferingJoinDao.searchAndCount(sc, searchFilter);
            } else {
                throw new PermissionDeniedException("The account:" + account.getAccountName() + " does not fall in the same domain hierarchy as the disk offering");
            }
        }

        // For non-root users, only return all offerings for the user's domain,
        // and everything above till root
        if ((_accountMgr.isNormalUser(account.getId()) || _accountMgr.isDomainAdmin(account.getId())) || account.getType() == Account.Type.RESOURCE_DOMAIN_ADMIN) {
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
            SearchCriteria<DiskOfferingJoinVO> ssc = _diskOfferingJoinDao.createSearchCriteria();
            ssc.addOr("displayText", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.EQ, name);
        }

        if (encrypt != null) {
            sc.addAnd("encrypt", SearchCriteria.Op.EQ, encrypt);
        }

        if (zoneId != null) {
            SearchBuilder<DiskOfferingJoinVO> sb = _diskOfferingJoinDao.createSearchBuilder();
            sb.and("zoneId", sb.entity().getZoneId(), Op.FIND_IN_SET);
            sb.or("zId", sb.entity().getZoneId(), Op.NULL);
            sb.done();
            SearchCriteria<DiskOfferingJoinVO> zoneSC = sb.create();
            zoneSC.setParameters("zoneId", String.valueOf(zoneId));
            sc.addAnd("zoneId", SearchCriteria.Op.SC, zoneSC);
            DataCenterJoinVO zone = _dcJoinDao.findById(zoneId);
            if (DataCenter.Type.Edge.equals(zone.getType())) {
                sc.addAnd("useLocalStorage", Op.EQ, true);
            }
        }

        DiskOffering currentDiskOffering = null;
        if (volumeId != null) {
            Volume volume = volumeDao.findById(volumeId);
            if (volume == null) {
                throw new InvalidParameterValueException(String.format("Unable to find a volume with specified id %s", volumeId));
            }
            currentDiskOffering = _diskOfferingDao.findByIdIncludingRemoved(volume.getDiskOfferingId());
            if (!currentDiskOffering.isComputeOnly() && currentDiskOffering.getDiskSizeStrictness()) {
                SearchCriteria<DiskOfferingJoinVO> ssc = _diskOfferingJoinDao.createSearchCriteria();
                ssc.addOr("diskSize", Op.EQ, volume.getSize());
                ssc.addOr("customized", SearchCriteria.Op.EQ, true);
                sc.addAnd("diskSizeOrCustomized", SearchCriteria.Op.SC, ssc);
            }
            sc.addAnd("id", SearchCriteria.Op.NEQ, currentDiskOffering.getId());
            sc.addAnd("diskSizeStrictness", Op.EQ, currentDiskOffering.getDiskSizeStrictness());
        }

        // Filter offerings that are not associated with caller's domain
        // Fetch the offering ids from the details table since theres no smart way to filter them in the join ... yet!
        Account caller = CallContext.current().getCallingAccount();
        if (caller.getType() != Account.Type.ADMIN) {
            Domain callerDomain = _domainDao.findById(caller.getDomainId());
            List<Long> domainIds = findRelatedDomainIds(callerDomain, isRecursive);

            List<Long> ids = _diskOfferingDetailsDao.findOfferingIdsByDomainIds(domainIds);
            SearchBuilder<DiskOfferingJoinVO> sb = _diskOfferingJoinDao.createSearchBuilder();
            if (ids != null && !ids.isEmpty()) {
                sb.and("id", sb.entity().getId(), Op.IN);
            }
            sb.or("domainId", sb.entity().getDomainId(), Op.NULL);
            sb.done();

            SearchCriteria<DiskOfferingJoinVO> scc = sb.create();
            if (ids != null && !ids.isEmpty()) {
                scc.setParameters("id", ids.toArray());
            }
            sc.addAnd("domainId", SearchCriteria.Op.SC, scc);
        }

        Pair<List<DiskOfferingJoinVO>, Integer> result = _diskOfferingJoinDao.searchAndCount(sc, searchFilter);
        String[] requiredTagsArray = new String[0];
        if (CollectionUtils.isNotEmpty(result.first()) && VolumeApiServiceImpl.MatchStoragePoolTagsWithDiskOffering.valueIn(zoneId)) {
            if (volumeId != null) {
                Volume volume = volumeDao.findById(volumeId);
                currentDiskOffering = _diskOfferingDao.findByIdIncludingRemoved(volume.getDiskOfferingId());
                requiredTagsArray = currentDiskOffering.getTagsArray();
            } else if (storagePoolId != null) {
                requiredTagsArray = _storageTagDao.getStoragePoolTags(storagePoolId).toArray(new String[0]);
            }
        }
        if (requiredTagsArray.length != 0) {
            ListIterator<DiskOfferingJoinVO> iteratorForTagsChecking = result.first().listIterator();
            while (iteratorForTagsChecking.hasNext()) {
                DiskOfferingJoinVO offering = iteratorForTagsChecking.next();
                String offeringTags = offering.getTags();
                String[] offeringTagsArray = (offeringTags == null || offeringTags.isEmpty()) ? new String[0] : offeringTags.split(",");
                if (!CollectionUtils.isSubCollection(Arrays.asList(requiredTagsArray), Arrays.asList(offeringTagsArray))) {
                    iteratorForTagsChecking.remove();
                }
            }
        }

        return new Pair<>(result.first(), result.second());
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
        ListResponse<ServiceOfferingResponse> response = new ListResponse<ServiceOfferingResponse>();
        List<ServiceOfferingResponse> offeringResponses = ViewResponseHelper.createServiceOfferingResponse(result.first().toArray(new ServiceOfferingJoinVO[result.first().size()]));
        response.setResponses(offeringResponses, result.second());
        return response;
    }

    private Pair<List<ServiceOfferingJoinVO>, Integer> searchForServiceOfferingsInternal(ListServiceOfferingsCmd cmd) {
        // Note
        // The filteredOfferings method for offerings is being modified in accordance with
        // discussion with Will/Kevin
        // For now, we will be listing the following based on the usertype
        // 1. For root, we will filteredOfferings all offerings
        // 2. For domainAdmin and regular users, we will filteredOfferings everything in
        // their domains+parent domains ... all the way
        // till
        // root
        Filter searchFilter = new Filter(ServiceOfferingJoinVO.class, "sortKey", SortKeyAscending.value(), cmd.getStartIndex(), cmd.getPageSizeVal());
        searchFilter.addOrderBy(ServiceOfferingJoinVO.class, "id", true);

        Account caller = CallContext.current().getCallingAccount();
        Object name = cmd.getServiceOfferingName();
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();
        Long vmId = cmd.getVirtualMachineId();
        Long domainId = cmd.getDomainId();
        Boolean isSystem = cmd.getIsSystem();
        String vmTypeStr = cmd.getSystemVmType();
        ServiceOfferingVO currentVmOffering = null;
        Boolean isRecursive = cmd.isRecursive();
        Long zoneId = cmd.getZoneId();
        Integer cpuNumber = cmd.getCpuNumber();
        Integer memory = cmd.getMemory();
        Integer cpuSpeed = cmd.getCpuSpeed();
        Boolean encryptRoot = cmd.getEncryptRoot();

        SearchCriteria<ServiceOfferingJoinVO> sc = _srvOfferingJoinDao.createSearchCriteria();
        if (!_accountMgr.isRootAdmin(caller.getId()) && isSystem) {
            throw new InvalidParameterValueException("Only ROOT admins can access system's offering");
        }

        // Keeping this logic consistent with domain specific zones
        // if a domainId is provided, we just return the so associated with this
        // domain
        if (domainId != null && !_accountMgr.isRootAdmin(caller.getId())) {
            // check if the user's domain == so's domain || user's domain is a
            // child of so's domain
            if (!isPermissible(caller.getDomainId(), domainId)) {
                throw new PermissionDeniedException("The account:" + caller.getAccountName() + " does not fall in the same domain hierarchy as the service offering");
            }
        }

        if (vmId != null) {
            VMInstanceVO vmInstance = _vmInstanceDao.findById(vmId);
            if ((vmInstance == null) || (vmInstance.getRemoved() != null)) {
                InvalidParameterValueException ex = new InvalidParameterValueException("unable to find a virtual machine with specified id");
                ex.addProxyObject(vmId.toString(), "vmId");
                throw ex;
            }

            _accountMgr.checkAccess(caller, null, true, vmInstance);

            currentVmOffering = _srvOfferingDao.findByIdIncludingRemoved(vmInstance.getId(), vmInstance.getServiceOfferingId());
            if (! currentVmOffering.isDynamic()) {
                sc.addAnd("id", SearchCriteria.Op.NEQ, currentVmOffering.getId());
            }

            if (currentVmOffering.getDiskOfferingStrictness()) {
                sc.addAnd("diskOfferingId", Op.EQ, currentVmOffering.getDiskOfferingId());
                sc.addAnd("diskOfferingStrictness", Op.EQ, true);
            } else {
                sc.addAnd("diskOfferingStrictness", Op.EQ, false);
            }

            boolean isRootVolumeUsingLocalStorage = virtualMachineManager.isRootVolumeOnLocalStorage(vmId);

            // 1. Only return offerings with the same storage type than the storage pool where the VM's root volume is allocated
            sc.addAnd("useLocalStorage", SearchCriteria.Op.EQ, isRootVolumeUsingLocalStorage);

            // 2.In case vm is running return only offerings greater than equal to current offering compute and offering's dynamic scalability should match
            if (vmInstance.getState() == VirtualMachine.State.Running) {
                Integer vmCpu = currentVmOffering.getCpu();
                Integer vmMemory = currentVmOffering.getRamSize();
                Integer vmSpeed = currentVmOffering.getSpeed();
                if ((vmCpu == null || vmMemory == null || vmSpeed == null) && VirtualMachine.Type.User.equals(vmInstance.getType())) {
                    UserVmVO userVmVO = _userVmDao.findById(vmId);
                    _userVmDao.loadDetails(userVmVO);
                    Map<String, String> details = userVmVO.getDetails();
                    vmCpu = NumbersUtil.parseInt(details.get(ApiConstants.CPU_NUMBER), 0);
                    if (vmSpeed == null) {
                        vmSpeed = NumbersUtil.parseInt(details.get(ApiConstants.CPU_SPEED), 0);
                    }
                    vmMemory = NumbersUtil.parseInt(details.get(ApiConstants.MEMORY), 0);
                }
                if (vmCpu != null && vmCpu > 0) {
                    sc.addAnd("cpu", Op.SC, getMinimumCpuServiceOfferingJoinSearchCriteria(vmCpu));
                }
                if (vmSpeed != null && vmSpeed > 0) {
                    sc.addAnd("speed", Op.SC, getMinimumCpuSpeedServiceOfferingJoinSearchCriteria(vmSpeed));
                }
                if (vmMemory != null && vmMemory > 0) {
                    sc.addAnd("ramSize", Op.SC, getMinimumMemoryServiceOfferingJoinSearchCriteria(vmMemory));
                }
                sc.addAnd("dynamicScalingEnabled", Op.EQ, currentVmOffering.isDynamicScalingEnabled());
            }
        }

        // boolean includePublicOfferings = false;
        if ((_accountMgr.isNormalUser(caller.getId()) || _accountMgr.isDomainAdmin(caller.getId())) || caller.getType() == Account.Type.RESOURCE_DOMAIN_ADMIN) {
            // For non-root users.
            if (isSystem) {
                throw new InvalidParameterValueException("Only root admins can access system's offering");
            }
            if (isRecursive) { // domain + all sub-domains
                if (caller.getType() == Account.Type.NORMAL) {
                    throw new InvalidParameterValueException("Only ROOT admins and Domain admins can list service offerings with isrecursive=true");
                }
            }
        } else {
            // for root users
            if (caller.getDomainId() != 1 && isSystem) { // NON ROOT admin
                throw new InvalidParameterValueException("Non ROOT admins cannot access system's offering");
            }
            if (domainId != null) {
                sc.addAnd("domainId", Op.FIND_IN_SET, String.valueOf(domainId));
            }
        }

        if (keyword != null) {
            SearchCriteria<ServiceOfferingJoinVO> ssc = _srvOfferingJoinDao.createSearchCriteria();
            ssc.addOr("displayText", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (isSystem != null) {
            // note that for non-root users, isSystem is always false when
            // control comes to here
            sc.addAnd("systemUse", SearchCriteria.Op.EQ, isSystem);
        }

        if (encryptRoot != null) {
            sc.addAnd("encryptRoot", SearchCriteria.Op.EQ, encryptRoot);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.EQ, name);
        }

        if (vmTypeStr != null) {
            sc.addAnd("vmType", SearchCriteria.Op.EQ, vmTypeStr);
        }

        if (zoneId != null) {
            SearchBuilder<ServiceOfferingJoinVO> sb = _srvOfferingJoinDao.createSearchBuilder();
            sb.and("zoneId", sb.entity().getZoneId(), Op.FIND_IN_SET);
            sb.or("zId", sb.entity().getZoneId(), Op.NULL);
            sb.done();
            SearchCriteria<ServiceOfferingJoinVO> zoneSC = sb.create();
            zoneSC.setParameters("zoneId", String.valueOf(zoneId));
            sc.addAnd("zoneId", SearchCriteria.Op.SC, zoneSC);
            DataCenterJoinVO zone = _dcJoinDao.findById(zoneId);
            if (DataCenter.Type.Edge.equals(zone.getType())) {
                sc.addAnd("useLocalStorage", Op.EQ, true);
            }
        }

        if (cpuNumber != null) {
            SearchCriteria<ServiceOfferingJoinVO> cpuConstraintSearchCriteria = _srvOfferingJoinDao.createSearchCriteria();
            cpuConstraintSearchCriteria.addAnd("minCpu", Op.LTEQ, cpuNumber);
            cpuConstraintSearchCriteria.addAnd("maxCpu", Op.GTEQ, cpuNumber);

            SearchCriteria<ServiceOfferingJoinVO> cpuSearchCriteria = _srvOfferingJoinDao.createSearchCriteria();
            cpuSearchCriteria.addOr("minCpu", Op.NULL);
            cpuSearchCriteria.addOr("constraints", Op.SC, cpuConstraintSearchCriteria);
            cpuSearchCriteria.addOr("minCpu", Op.GTEQ, cpuNumber);

            sc.addAnd("cpuConstraints", SearchCriteria.Op.SC, cpuSearchCriteria);
        }

        if (memory != null) {
            SearchCriteria<ServiceOfferingJoinVO> memoryConstraintSearchCriteria = _srvOfferingJoinDao.createSearchCriteria();
            memoryConstraintSearchCriteria.addAnd("minMemory", Op.LTEQ, memory);
            memoryConstraintSearchCriteria.addAnd("maxMemory", Op.GTEQ, memory);

            SearchCriteria<ServiceOfferingJoinVO> memSearchCriteria = _srvOfferingJoinDao.createSearchCriteria();
            memSearchCriteria.addOr("minMemory", Op.NULL);
            memSearchCriteria.addOr("memconstraints", Op.SC, memoryConstraintSearchCriteria);
            memSearchCriteria.addOr("minMemory", Op.GTEQ, memory);

            sc.addAnd("memoryConstraints", SearchCriteria.Op.SC, memSearchCriteria);
        }

        if (cpuSpeed != null) {
            SearchCriteria<ServiceOfferingJoinVO> cpuSpeedSearchCriteria = _srvOfferingJoinDao.createSearchCriteria();
            cpuSpeedSearchCriteria.addOr("speed", Op.NULL);
            cpuSpeedSearchCriteria.addOr("speed", Op.GTEQ, cpuSpeed);
            sc.addAnd("cpuspeedconstraints", SearchCriteria.Op.SC, cpuSpeedSearchCriteria);
        }

        // Filter offerings that are not associated with caller's domain
        // Fetch the offering ids from the details table since theres no smart way to filter them in the join ... yet!
        if (caller.getType() != Account.Type.ADMIN) {
            Domain callerDomain = _domainDao.findById(caller.getDomainId());
            List<Long> domainIds = findRelatedDomainIds(callerDomain, isRecursive);

            List<Long> ids = _srvOfferingDetailsDao.findOfferingIdsByDomainIds(domainIds);
            SearchBuilder<ServiceOfferingJoinVO> sb = _srvOfferingJoinDao.createSearchBuilder();
            if (ids != null && !ids.isEmpty()) {
                sb.and("id", sb.entity().getId(), Op.IN);
            }
            sb.or("domainId", sb.entity().getDomainId(), Op.NULL);
            sb.done();

            SearchCriteria<ServiceOfferingJoinVO> scc = sb.create();
            if (ids != null && !ids.isEmpty()) {
                scc.setParameters("id", ids.toArray());
            }
            sc.addAnd("domainId", SearchCriteria.Op.SC, scc);
        }

        if (currentVmOffering != null) {
            DiskOfferingVO diskOffering = _diskOfferingDao.findByIdIncludingRemoved(currentVmOffering.getDiskOfferingId());
            List<String> storageTags = com.cloud.utils.StringUtils.csvTagsToList(diskOffering.getTags());
            if (!storageTags.isEmpty() && VolumeApiServiceImpl.MatchStoragePoolTagsWithDiskOffering.value()) {
                SearchBuilder<ServiceOfferingJoinVO> sb = _srvOfferingJoinDao.createSearchBuilder();
                for(String tag : storageTags) {
                    sb.and(tag, sb.entity().getTags(), Op.FIND_IN_SET);
                }
                sb.done();

                SearchCriteria<ServiceOfferingJoinVO> scc = sb.create();
                for(String tag : storageTags) {
                    scc.setParameters(tag, tag);
                }
                sc.addAnd("storageTags", SearchCriteria.Op.SC, scc);
            }

            List<String> hostTags = com.cloud.utils.StringUtils.csvTagsToList(currentVmOffering.getHostTag());
            if (!hostTags.isEmpty()) {
                SearchBuilder<ServiceOfferingJoinVO> hostTagsSearchBuilder = _srvOfferingJoinDao.createSearchBuilder();
                for(String tag : hostTags) {
                    hostTagsSearchBuilder.and(tag, hostTagsSearchBuilder.entity().getHostTag(), Op.FIND_IN_SET);
                }
                hostTagsSearchBuilder.done();

                SearchCriteria<ServiceOfferingJoinVO> hostTagsSearchCriteria = hostTagsSearchBuilder.create();
                for(String tag : hostTags) {
                    hostTagsSearchCriteria.setParameters(tag, tag);
                }

                SearchCriteria<ServiceOfferingJoinVO> finalHostTagsSearchCriteria = _srvOfferingJoinDao.createSearchCriteria();
                finalHostTagsSearchCriteria.addOr("hostTag", Op.NULL);
                finalHostTagsSearchCriteria.addOr("hostTag", Op.SC, hostTagsSearchCriteria);

                sc.addAnd("hostTagsConstraint", SearchCriteria.Op.SC, finalHostTagsSearchCriteria);
            }
        }

        return _srvOfferingJoinDao.searchAndCount(sc, searchFilter);
    }

    @Override
    public ListResponse<ZoneResponse> listDataCenters(ListZonesCmd cmd) {
        Pair<List<DataCenterJoinVO>, Integer> result = listDataCentersInternal(cmd);
        ListResponse<ZoneResponse> response = new ListResponse<ZoneResponse>();

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
        String keyword = cmd.getKeyword();
        String name = cmd.getName();
        String networkType = cmd.getNetworkType();
        Map<String, String> resourceTags = cmd.getTags();

        SearchBuilder<DataCenterJoinVO> sb = _dcJoinDao.createSearchBuilder();
        if (resourceTags != null && !resourceTags.isEmpty()) {
            SearchBuilder<ResourceTagVO> tagSearch = _resourceTagDao.createSearchBuilder();
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

                if (_accountMgr.isNormalUser(account.getId())) {
                    // accountId == null (zones dedicated to a domain) or
                    // accountId = caller
                    SearchCriteria<DataCenterJoinVO> sdc = _dcJoinDao.createSearchCriteria();
                    sdc.addOr("accountId", SearchCriteria.Op.EQ, account.getId());
                    sdc.addOr("accountId", SearchCriteria.Op.NULL);

                    sc.addAnd("accountId", SearchCriteria.Op.SC, sdc);
                }

            } else if (_accountMgr.isNormalUser(account.getId())) {
                // it was decided to return all zones for the user's domain, and
                // everything above till root
                // list all zones belonging to this domain, and all of its
                // parents
                // check the parent, if not null, add zones for that parent to
                // list

                // find all domain Id up to root domain for this account
                List<Long> domainIds = new ArrayList<Long>();
                DomainVO domainRecord = _domainDao.findById(account.getDomainId());
                if (domainRecord == null) {
                    s_logger.error("Could not find the domainId for account:" + account.getAccountName());
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

            } else if (_accountMgr.isDomainAdmin(account.getId()) || account.getType() == Account.Type.RESOURCE_DOMAIN_ADMIN) {
                // it was decided to return all zones for the domain admin, and
                // everything above till root, as well as zones till the domain
                // leaf
                List<Long> domainIds = new ArrayList<Long>();
                DomainVO domainRecord = _domainDao.findById(account.getDomainId());
                if (domainRecord == null) {
                    s_logger.error("Could not find the domainId for account:" + account.getAccountName());
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
                if ((available != null) && Boolean.FALSE.equals(available)) {
                    Set<Long> dcIds = new HashSet<Long>(); // data centers with
                    // at least one VM
                    // running
                    List<DomainRouterVO> routers = _routerDao.listBy(account.getId());
                    for (DomainRouterVO router : routers) {
                        dcIds.add(router.getDataCenterId());
                    }
                    if (dcIds.size() == 0) {
                        return new Pair<List<DataCenterJoinVO>, Integer>(new ArrayList<DataCenterJoinVO>(), 0);
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
        List<Long> dedicatedZoneIds = new ArrayList<Long>();
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
        ListResponse<TemplateResponse> response = new ListResponse<TemplateResponse>();

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

        List<Long> permittedAccountIds = new ArrayList<Long>();
        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccountIds, domainIdRecursiveListProject, listAll, false);
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        List<Account> permittedAccounts = new ArrayList<Account>();
        for (Long accountId : permittedAccountIds) {
            permittedAccounts.add(_accountMgr.getAccount(accountId));
        }

        boolean showDomr = ((templateFilter != TemplateFilter.selfexecutable) && (templateFilter != TemplateFilter.featured));
        HypervisorType hypervisorType = HypervisorType.getType(cmd.getHypervisor());

        return searchForTemplatesInternal(id, cmd.getTemplateName(), cmd.getKeyword(), templateFilter, false, null, cmd.getPageSizeVal(), cmd.getStartIndex(), cmd.getZoneId(), hypervisorType,
                showDomr, cmd.listInReadyState(), permittedAccounts, caller, listProjectResourcesCriteria, tags, showRemovedTmpl, cmd.getIds(), parentTemplateId, cmd.getShowUnique());
    }

    private Pair<List<TemplateJoinVO>, Integer> searchForTemplatesInternal(Long templateId, String name, String keyword, TemplateFilter templateFilter, boolean isIso, Boolean bootable, Long pageSize,
            Long startIndex, Long zoneId, HypervisorType hyperType, boolean showDomr, boolean onlyReady, List<Account> permittedAccounts, Account caller,
            ListProjectResourcesCriteria listProjectResourcesCriteria, Map<String, String> tags, boolean showRemovedTmpl, List<Long> ids, Long parentTemplateId, Boolean showUnique) {

        // check if zone is configured, if not, just return empty list
        List<HypervisorType> hypers = null;
        if (!isIso) {
            hypers = _resourceMgr.listAvailHypervisorInZone(null, null);
            if (hypers == null || hypers.isEmpty()) {
                return new Pair<List<TemplateJoinVO>, Integer>(new ArrayList<TemplateJoinVO>(), 0);
            }
        }

        VMTemplateVO template = null;

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
        SearchCriteria<TemplateJoinVO> sc = sb.create();

        // verify templateId parameter and specially handle it
        if (templateId != null) {
            template = _templateDao.findByIdIncludingRemoved(templateId); // Done for backward compatibility - Bug-5221
            if (template == null) {
                throw new InvalidParameterValueException("Please specify a valid template ID.");
            }// If ISO requested then it should be ISO.
            if (isIso && template.getFormat() != ImageFormat.ISO) {
                s_logger.error("Template Id " + templateId + " is not an ISO");
                InvalidParameterValueException ex = new InvalidParameterValueException("Specified Template Id is not an ISO");
                ex.addProxyObject(template.getUuid(), "templateId");
                throw ex;
            }// If ISO not requested then it shouldn't be an ISO.
            if (!isIso && template.getFormat() == ImageFormat.ISO) {
                s_logger.error("Incorrect format of the template id " + templateId);
                InvalidParameterValueException ex = new InvalidParameterValueException("Incorrect format " + template.getFormat() + " of the specified template id");
                ex.addProxyObject(template.getUuid(), "templateId");
                throw ex;
            }
            if (!template.isPublicTemplate() && caller.getType() == Account.Type.DOMAIN_ADMIN) {
                Account template_acc = _accountMgr.getAccount(template.getAccountId());
                DomainVO domain = _domainDao.findById(template_acc.getDomainId());
                _accountMgr.checkAccess(caller, domain);
            }

            // if template is not public, perform permission check here
            else if (!template.isPublicTemplate() && caller.getType() != Account.Type.ADMIN) {
                _accountMgr.checkAccess(caller, null, false, template);
            } else if (template.isPublicTemplate()) {
                _accountMgr.checkAccess(caller, null, false, template);
            }

            // if templateId is specified, then we will just use the id to
            // search and ignore other query parameters
            sc.addAnd("id", SearchCriteria.Op.EQ, templateId);
        } else {

            DomainVO domain = null;
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

            List<Long> relatedDomainIds = new ArrayList<Long>();
            List<Long> permittedAccountIds = new ArrayList<Long>();
            if (!permittedAccounts.isEmpty()) {
                for (Account account : permittedAccounts) {
                    permittedAccountIds.add(account.getId());
                    boolean publicTemplates = (templateFilter == TemplateFilter.featured || templateFilter == TemplateFilter.community);

                    // get all parent domain ID's all the way till root domain
                    DomainVO domainTreeNode = null;
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
                    if (_accountMgr.isAdmin(account.getId()) || publicTemplates) {
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

        return templateChecks(isIso, hypers, tags, name, keyword, hyperType, onlyReady, bootable, zoneId, showDomr, caller,
                showRemovedTmpl, parentTemplateId, showUnique, searchFilter, sc);

    }

    private Pair<List<TemplateJoinVO>, Integer> templateChecks(boolean isIso, List<HypervisorType> hypers, Map<String, String> tags, String name, String keyword,
                                                               HypervisorType hyperType, boolean onlyReady, Boolean bootable, Long zoneId, boolean showDomr, Account caller,
                                                               boolean showRemovedTmpl, Long parentTemplateId, Boolean showUnique,
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
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
        } else if (name != null) {
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

        // don't return removed template, this should not be needed since we
        // changed annotation for removed field in TemplateJoinVO.
        // sc.addAnd("removed", SearchCriteria.Op.NULL);

        // search unique templates and find details by Ids
        Pair<List<TemplateJoinVO>, Integer> uniqueTmplPair = null;
        if (showRemovedTmpl) {
            uniqueTmplPair = _templateJoinDao.searchIncludingRemovedAndCount(sc, searchFilter);
        } else {
            sc.addAnd("templateState", SearchCriteria.Op.IN, new State[] {State.Active, State.UploadAbandoned, State.UploadError, State.NotUploaded, State.UploadInProgress});
            if (showUnique) {
                final String[] distinctColumns = {"id"};
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
        List<TemplateJoinVO> templates = null;
        if (showUnique) {
            Long[] templateIds = templateData.stream().map(template -> template.getId()).toArray(Long[]::new);
            templates = _templateJoinDao.findByDistinctIds(templateIds);
        } else {
            String[] templateZonePairs = templateData.stream().map(template -> template.getTempZonePair()).toArray(String[]::new);
            templates = _templateJoinDao.searchByTemplateZonePair(showRemoved, templateZonePairs);
        }

        if(caller.getType() != Account.Type.ADMIN) {
            templates = applyPublicTemplateRestriction(templates, caller);
            count = templates.size();
        }

        return new Pair<List<TemplateJoinVO>, Integer>(templates, count);
    }

    private List<TemplateJoinVO> applyPublicTemplateRestriction(List<TemplateJoinVO> templates, Account caller){
        List<Long> unsharableDomainIds = templates.stream()
                .map(TemplateJoinVO::getDomainId)
                .distinct()
                .filter(domainId -> domainId != caller.getDomainId())
                .filter(Predicate.not(QueryService.SharePublicTemplatesWithOtherDomains::valueIn))
                .collect(Collectors.toList());

        return templates.stream()
                .filter(Predicate.not(t -> unsharableDomainIds.contains(t.getDomainId())))
                .collect(Collectors.toList());
    }

    @Override
    public ListResponse<TemplateResponse> listIsos(ListIsosCmd cmd) {
        Pair<List<TemplateJoinVO>, Integer> result = searchForIsosInternal(cmd);
        ListResponse<TemplateResponse> response = new ListResponse<TemplateResponse>();

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

        List<Long> permittedAccountIds = new ArrayList<Long>();
        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccountIds, domainIdRecursiveListProject, listAll, false);
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        List<Account> permittedAccounts = new ArrayList<Account>();
        for (Long accountId : permittedAccountIds) {
            permittedAccounts.add(_accountMgr.getAccount(accountId));
        }

        HypervisorType hypervisorType = HypervisorType.getType(cmd.getHypervisor());

        return searchForTemplatesInternal(cmd.getId(), cmd.getIsoName(), cmd.getKeyword(), isoFilter, true, cmd.isBootable(), cmd.getPageSizeVal(), cmd.getStartIndex(), cmd.getZoneId(),
                hypervisorType, true, cmd.listInReadyState(), permittedAccounts, caller, listProjectResourcesCriteria, tags, showRemovedISO, null, null, cmd.getShowUnique());
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
            options.put(VmDetailConstants.NIC_ADAPTER, Arrays.asList("e1000", "virtio", "rtl8139", "vmxnet3", "ne2k_pci"));
            options.put(VmDetailConstants.ROOT_DISK_CONTROLLER, Arrays.asList("osdefault", "ide", "scsi", "virtio"));
            options.put(VmDetailConstants.VIDEO_HARDWARE, Arrays.asList("cirrus", "vga", "qxl", "virtio"));
            options.put(VmDetailConstants.VIDEO_RAM, Collections.emptyList());
            options.put(VmDetailConstants.IO_POLICY, Arrays.asList("threads", "native", "io_uring", "storage_specific"));
            options.put(VmDetailConstants.IOTHREADS, Arrays.asList("enabled"));
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
            UserVmVO userVM = _userVmDao.findById(vmId);
            if (userVM == null) {
                throw new InvalidParameterValueException("Unable to list affinity groups for virtual machine instance " + vmId + "; instance not found.");
            }
            _accountMgr.checkAccess(caller, null, true, userVM);
            return listAffinityGroupsByVM(vmId.longValue(), startIndex, pageSize);
        }

        List<Long> permittedAccounts = new ArrayList<Long>();
        Ternary<Long, Boolean, ListProjectResourcesCriteria> ternary = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(domainId, isRecursive, null);

        _accountMgr.buildACLSearchParameters(caller, affinityGroupId, accountName, projectId, permittedAccounts, ternary, listAll, false);

        domainId = ternary.first();
        isRecursive = ternary.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = ternary.third();

        Filter searchFilter = new Filter(AffinityGroupJoinVO.class, ID_FIELD, true, startIndex, pageSize);

        SearchCriteria<AffinityGroupJoinVO> sc = buildAffinityGroupSearchCriteria(domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria, affinityGroupId, affinityGroupName,
                affinityGroupType, keyword);

        Pair<List<AffinityGroupJoinVO>, Integer> uniqueGroupsPair = _affinityGroupJoinDao.searchAndCount(sc, searchFilter);

        // search group details by ids
        List<AffinityGroupJoinVO> affinityGroups = new ArrayList<AffinityGroupJoinVO>();

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
                SearchCriteria<AffinityGroupJoinVO> scDomain = buildAffinityGroupSearchCriteria(null, isRecursive, new ArrayList<Long>(), listProjectResourcesCriteria, affinityGroupId,
                        affinityGroupName, affinityGroupType, keyword);
                Pair<List<AffinityGroupJoinVO>, Integer> groupsPair = listDomainLevelAffinityGroups(scDomain, searchFilter, domainId);
                affinityGroups.addAll(groupsPair.first());
                count += groupsPair.second();
            } else {

                for (Long permAcctId : permittedAccounts) {
                    Account permittedAcct = _accountDao.findById(permAcctId);
                    SearchCriteria<AffinityGroupJoinVO> scDomain = buildAffinityGroupSearchCriteria(null, isRecursive, new ArrayList<Long>(), listProjectResourcesCriteria, affinityGroupId,
                            affinityGroupName, affinityGroupType, keyword);
                    Pair<List<AffinityGroupJoinVO>, Integer> groupsPair = listDomainLevelAffinityGroups(scDomain, searchFilter, permittedAcct.getDomainId());
                    affinityGroups.addAll(groupsPair.first());
                    count += groupsPair.second();
                }
            }
        } else if (((permittedAccounts.isEmpty()) && (domainId != null) && isRecursive)) {
            // list all domain level affinity groups for the domain admin case
            SearchCriteria<AffinityGroupJoinVO> scDomain = buildAffinityGroupSearchCriteria(null, isRecursive, new ArrayList<Long>(), listProjectResourcesCriteria, affinityGroupId, affinityGroupName,
                    affinityGroupType, keyword);
            Pair<List<AffinityGroupJoinVO>, Integer> groupsPair = listDomainLevelAffinityGroups(scDomain, searchFilter, domainId);
            affinityGroups.addAll(groupsPair.first());
            count += groupsPair.second();
        }

        return new Pair<List<AffinityGroupJoinVO>, Integer>(affinityGroups, count);

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
            return new Pair<List<AffinityGroupJoinVO>, Integer>(new ArrayList<AffinityGroupJoinVO>(), count);
        }
        List<AffinityGroupVMMapVO> agVmMappings = agVmMappingPair.first();
        Long[] agIds = new Long[agVmMappings.size()];
        int i = 0;
        for (AffinityGroupVMMapVO agVm : agVmMappings) {
            agIds[i++] = agVm.getAffinityGroupId();
        }
        List<AffinityGroupJoinVO> ags = _affinityGroupJoinDao.searchByIds(agIds);
        return new Pair<List<AffinityGroupJoinVO>, Integer>(ags, count);
    }

    private Pair<List<AffinityGroupJoinVO>, Integer> listDomainLevelAffinityGroups(SearchCriteria<AffinityGroupJoinVO> sc, Filter searchFilter, long domainId) {
        List<Long> affinityGroupIds = new ArrayList<Long>();
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
                return new Pair<>(new ArrayList<AffinityGroupJoinVO>(), 0);
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
            return new Pair<>(new ArrayList<AffinityGroupJoinVO>(), 0);
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

        List<? extends ResourceDetail> detailList = new ArrayList<ResourceDetail>();
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

        List<ResourceDetailResponse> responseList = new ArrayList<ResourceDetailResponse>();
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
            ManagementServerResponse hostResponse = createManagementServerResponse(host);
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

    protected ManagementServerResponse createManagementServerResponse(ManagementServerJoinVO mgmt) {
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
        mgmtResponse.setServiceIp(mgmt.getServiceIP());
        mgmtResponse.setObjectName("managementserver");
        return mgmtResponse;
    }

    @Override
    public List<RouterHealthCheckResultResponse> listRouterHealthChecks(GetRouterHealthCheckResultsCmd cmd) {
        s_logger.info("Executing health check command " + cmd);
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
    public String getConfigComponentName() {
        return QueryService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {AllowUserViewDestroyedVM, UserVMDeniedDetails, UserVMReadOnlyDetails, SortKeyAscending,
                AllowUserViewAllDomainAccounts, SharePublicTemplatesWithOtherDomains};
    }
}
