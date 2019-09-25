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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.cloud.agent.api.storage.OVFProperty;
import com.cloud.storage.TemplateOVFPropertyVO;
import com.cloud.storage.dao.TemplateOVFPropertiesDao;
import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.affinity.AffinityGroupDomainMapVO;
import org.apache.cloudstack.affinity.AffinityGroupResponse;
import org.apache.cloudstack.affinity.AffinityGroupVMMapVO;
import org.apache.cloudstack.affinity.dao.AffinityGroupDomainMapDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.ResourceDetail;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.admin.account.ListAccountsCmdByAdmin;
import org.apache.cloudstack.api.command.admin.domain.ListDomainsCmd;
import org.apache.cloudstack.api.command.admin.domain.ListDomainsCmdByAdmin;
import org.apache.cloudstack.api.command.admin.host.ListHostTagsCmd;
import org.apache.cloudstack.api.command.admin.host.ListHostsCmd;
import org.apache.cloudstack.api.command.admin.internallb.ListInternalLBVMsCmd;
import org.apache.cloudstack.api.command.admin.iso.ListIsosCmdByAdmin;
import org.apache.cloudstack.api.command.admin.management.ListMgmtsCmd;
import org.apache.cloudstack.api.command.admin.router.ListRoutersCmd;
import org.apache.cloudstack.api.command.admin.storage.ListImageStoresCmd;
import org.apache.cloudstack.api.command.admin.storage.ListSecondaryStagingStoresCmd;
import org.apache.cloudstack.api.command.admin.storage.ListStoragePoolsCmd;
import org.apache.cloudstack.api.command.admin.storage.ListStorageTagsCmd;
import org.apache.cloudstack.api.command.admin.template.ListTemplatesCmdByAdmin;
import org.apache.cloudstack.api.command.admin.user.ListUsersCmd;
import org.apache.cloudstack.api.command.admin.vm.ListVMsCmdByAdmin;
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
import org.apache.cloudstack.api.command.user.template.ListTemplateOVFProperties;
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
import org.apache.cloudstack.api.response.ResourceTagResponse;
import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.StorageTagResponse;
import org.apache.cloudstack.api.response.TemplateOVFPropertyResponse;
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
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.query.QueryService;
import org.apache.cloudstack.resourcedetail.dao.DiskOfferingDetailsDao;
import org.apache.commons.collections.CollectionUtils;
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
import com.cloud.network.security.SecurityGroupVMMapVO;
import com.cloud.network.security.dao.SecurityGroupVMMapDao;
import com.cloud.org.Grouping;
import com.cloud.projects.Project;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.projects.ProjectInvitation;
import com.cloud.projects.ProjectManager;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectAccountDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ResourceMetaDataService;
import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.server.TaggedResourceService;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.StoragePoolTagVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.dao.StoragePoolTagsDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.template.VirtualMachineTemplate.State;
import com.cloud.template.VirtualMachineTemplate.TemplateFilter;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.Ternary;
import com.cloud.utils.db.Filter;
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
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.base.Strings;

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
    private ConfigurationDao _configDao;

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
    private DiskOfferingDetailsDao diskOfferingDetailsDao;

    @Inject
    private ServiceOfferingJoinDao _srvOfferingJoinDao;

    @Inject
    private ServiceOfferingDao _srvOfferingDao;

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
    private TaggedResourceService _taggedResourceMgr;

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
    ManagementServerHostDao managementServerHostDao;

    @Inject
    TemplateOVFPropertiesDao templateOVFPropertiesDao;

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

    private Pair<List<UserAccountJoinVO>, Integer> searchForUsersInternal(ListUsersCmd cmd) throws PermissionDeniedException {
        Account caller = CallContext.current().getCallingAccount();

        List<Long> permittedAccounts = new ArrayList<Long>();

        boolean listAll = cmd.listAll();
        Long id = cmd.getId();
        if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL) {
            long currentId = CallContext.current().getCallingUser().getId();
            if (id != null && currentId != id.longValue()) {
                throw new PermissionDeniedException("Calling user is not authorized to see the user requested by id");
            }
            id = currentId;
        }
        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), null, permittedAccounts, domainIdRecursiveListProject, listAll, false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(UserAccountJoinVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object username = cmd.getUsername();
        Object type = cmd.getAccountType();
        Object accountName = cmd.getAccountName();
        Object state = cmd.getState();
        Object keyword = cmd.getKeyword();

        SearchBuilder<UserAccountJoinVO> sb = _userAccountJoinDao.createSearchBuilder();
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        sb.and("username", sb.entity().getUsername(), SearchCriteria.Op.LIKE);
        if (id != null && id == 1) {
            // system user should NOT be searchable
            List<UserAccountJoinVO> emptyList = new ArrayList<UserAccountJoinVO>();
            return new Pair<List<UserAccountJoinVO>, Integer>(emptyList, 0);
        } else if (id != null) {
            sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        } else {
            // this condition is used to exclude system user from the search
            // results
            sb.and("id", sb.entity().getId(), SearchCriteria.Op.NEQ);
        }

        sb.and("type", sb.entity().getAccountType(), SearchCriteria.Op.EQ);
        sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
        sb.and("accountName", sb.entity().getAccountName(), SearchCriteria.Op.EQ);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);

        if ((accountName == null) && (domainId != null)) {
            sb.and("domainPath", sb.entity().getDomainPath(), SearchCriteria.Op.LIKE);
        }

        SearchCriteria<UserAccountJoinVO> sc = sb.create();

        // building ACL condition
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (keyword != null) {
            SearchCriteria<UserAccountJoinVO> ssc = _userAccountJoinDao.createSearchCriteria();
            ssc.addOr("username", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("firstname", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("lastname", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("email", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("accountName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("accountType", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("username", SearchCriteria.Op.SC, ssc);
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

        sc.setParameters("archived", false);

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

        if (projectId == null && ResourceObjectType.Project.name().equalsIgnoreCase(resourceType) && !Strings.isNullOrEmpty(resourceId)) {
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
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);

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
            sc.setParameters("name", "%" + name + "%");
        }

        return _vmGroupJoinDao.searchAndCount(sc, searchFilter);
    }

    @Override
    public ListResponse<UserVmResponse> searchForUserVMs(ListVMsCmd cmd) {
        Pair<List<UserVmJoinVO>, Integer> result = searchForUserVMsInternal(cmd);
        ListResponse<UserVmResponse> response = new ListResponse<UserVmResponse>();
        ResponseView respView = ResponseView.Restricted;
        if (cmd instanceof ListVMsCmdByAdmin) {
            respView = ResponseView.Full;
        }
        List<UserVmResponse> vmResponses = ViewResponseHelper.createUserVmResponse(respView, "virtualmachine", cmd.getDetails(), result.first().toArray(new UserVmJoinVO[result.first().size()]));

        response.setResponses(vmResponses, result.second());
        return response;
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
        Object pod = null;
        Object hostId = null;
        Object storageId = null;
        if (cmd instanceof ListVMsCmdByAdmin) {
            ListVMsCmdByAdmin adCmd = (ListVMsCmdByAdmin)cmd;
            pod = adCmd.getPodId();
            hostId = adCmd.getHostId();
            storageId = adCmd.getStorageId();
        }

        sb.and("displayName", sb.entity().getDisplayName(), SearchCriteria.Op.LIKE);
        sb.and("idIN", sb.entity().getId(), SearchCriteria.Op.IN);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("stateEQ", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("stateNEQ", sb.entity().getState(), SearchCriteria.Op.NEQ);
        sb.and("stateNIN", sb.entity().getState(), SearchCriteria.Op.NIN);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        sb.and("hypervisorType", sb.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        sb.and("hostIdEQ", sb.entity().getHostId(), SearchCriteria.Op.EQ);
        sb.and("templateId", sb.entity().getTemplateId(), SearchCriteria.Op.EQ);
        sb.and("isoId", sb.entity().getIsoId(), SearchCriteria.Op.EQ);
        sb.and("instanceGroupId", sb.entity().getInstanceGroupId(), SearchCriteria.Op.EQ);

        if (serviceOffId != null) {
            sb.and("serviceOfferingId", sb.entity().getServiceOfferingId(), SearchCriteria.Op.EQ);
        }
        if (display != null) {
            sb.and("display", sb.entity().isDisplayVm(), SearchCriteria.Op.EQ);
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
            sb.and("poolId", sb.entity().getPoolId(), SearchCriteria.Op.EQ);
        }

        if (affinityGroupId != null) {
            sb.and("affinityGroupId", sb.entity().getAffinityGroupId(), SearchCriteria.Op.EQ);
        }

        if (keyPairName != null) {
            sb.and("keyPairName", sb.entity().getKeypairName(), SearchCriteria.Op.EQ);
        }

        if (!isRootAdmin) {
            sb.and("displayVm", sb.entity().isDisplayVm(), SearchCriteria.Op.EQ);
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
            ssc.addOr("displayName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("instanceName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.EQ, keyword);
            sc.addAnd("displayName", SearchCriteria.Op.SC, ssc);
        }

        if (serviceOffId != null) {
            sc.setParameters("serviceOfferingId", serviceOffId);
        }

        if (display != null) {
            sc.setParameters("display", display);
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
            sc.setParameters("name", "%" + name + "%");
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

        if (cmd instanceof ListVMsCmdByAdmin) {
            ListVMsCmdByAdmin aCmd = (ListVMsCmdByAdmin)cmd;
            if (aCmd.getPodId() != null) {
                sc.setParameters("podId", pod);

                if (state == null) {
                    sc.setParameters("stateNEQ", "Destroyed");
                }
            }

            if (hostId != null) {
                sc.setParameters("hostIdEQ", hostId);
            }

            if (storageId != null) {
                sc.setParameters("poolId", storageId);
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
                cmd.getHostId(), cmd.getKeyword(), cmd.getNetworkId(), cmd.getVpcId(), cmd.getForVpc(), cmd.getRole(), cmd.getVersion());
        ListResponse<DomainRouterResponse> response = new ListResponse<DomainRouterResponse>();

        List<DomainRouterResponse> routerResponses = ViewResponseHelper.createDomainRouterResponse(result.first().toArray(new DomainRouterJoinVO[result.first().size()]));
        response.setResponses(routerResponses, result.second());
        return response;
    }

    @Override
    public ListResponse<DomainRouterResponse> searchForInternalLbVms(ListInternalLBVMsCmd cmd) {
        Pair<List<DomainRouterJoinVO>, Integer> result = searchForRoutersInternal(cmd, cmd.getId(), cmd.getRouterName(), cmd.getState(), cmd.getZoneId(), cmd.getPodId(), null, cmd.getHostId(),
                cmd.getKeyword(), cmd.getNetworkId(), cmd.getVpcId(), cmd.getForVpc(), cmd.getRole(), null);
        ListResponse<DomainRouterResponse> response = new ListResponse<DomainRouterResponse>();

        List<DomainRouterResponse> routerResponses = ViewResponseHelper.createDomainRouterResponse(result.first().toArray(new DomainRouterJoinVO[result.first().size()]));
        response.setResponses(routerResponses, result.second());
        return response;
    }

    private Pair<List<DomainRouterJoinVO>, Integer> searchForRoutersInternal(BaseListProjectAndAccountResourcesCmd cmd, Long id, String name, String state, Long zoneId, Long podId, Long clusterId,
            Long hostId, String keyword, Long networkId, Long vpcId, Boolean forVpc, String role, String version) {

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

        sb.and("name", sb.entity().getInstanceName(), SearchCriteria.Op.LIKE);
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

        SearchCriteria<DomainRouterJoinVO> sc = sb.create();
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (keyword != null) {
            SearchCriteria<DomainRouterJoinVO> ssc = _routerJoinDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("instanceName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("networkName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("vpcName", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("instanceName", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.setParameters("name", "%" + name + "%");
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
        Long domainId = cmd.getDomainId();
        String keyword = cmd.getKeyword();
        Long startIndex = cmd.getStartIndex();
        Long pageSize = cmd.getPageSizeVal();
        boolean listAll = cmd.listAll();
        boolean isRecursive = cmd.isRecursive();
        cmd.getTags();

        Account caller = CallContext.current().getCallingAccount();
        Long accountId = null;
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
            } else { // domainId == null
                if (accountName != null) {
                    throw new InvalidParameterValueException("could not find account " + accountName + " because domain is not specified");
                }

            }
        } else {
            if (accountName != null && !accountName.equals(caller.getAccountName())) {
                throw new PermissionDeniedException("Can't list account " + accountName + " projects; unauthorized");
            }

            if (domainId != null && !domainId.equals(caller.getDomainId())) {
                throw new PermissionDeniedException("Can't list domain id= " + domainId + " projects; unauthorized");
            }

            accountId = caller.getId();
        }

        if (domainId == null && accountId == null && (_accountMgr.isNormalUser(caller.getId()) || !listAll)) {
            accountId = caller.getId();
        } else if (_accountMgr.isDomainAdmin(caller.getId()) || (isRecursive && !listAll)) {
            DomainVO domain = _domainDao.findById(caller.getDomainId());
            path = domain.getPath();
        }

        if (path != null) {
            sb.and("domainPath", sb.entity().getDomainPath(), SearchCriteria.Op.LIKE);
        }

        if (accountId != null) {
            sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
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
        boolean isRecursive = cmd.isRecursive();
        boolean listAll = cmd.listAll();

        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<Long>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(domainId, isRecursive, null);
        _accountMgr.buildACLSearchParameters(caller, id, accountName, projectId, permittedAccounts, domainIdRecursiveListProject, listAll, true);
        domainId = domainIdRecursiveListProject.first();
        isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(ProjectInvitationJoinVO.class, "id", true, startIndex, pageSizeVal);
        SearchBuilder<ProjectInvitationJoinVO> sb = _projectInvitationJoinDao.createSearchBuilder();
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("projectId", sb.entity().getProjectId(), SearchCriteria.Op.EQ);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("created", sb.entity().getCreated(), SearchCriteria.Op.GT);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);

        SearchCriteria<ProjectInvitationJoinVO> sc = sb.create();
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (projectId != null) {
            sc.setParameters("projectId", projectId);
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

        return _projectInvitationJoinDao.searchAndCount(sc, searchFilter);

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
        String role = cmd.getRole();
        Long startIndex = cmd.getStartIndex();
        Long pageSizeVal = cmd.getPageSizeVal();

        // long projectId, String accountName, String role, Long startIndex,
        // Long pageSizeVal) {
        Account caller = CallContext.current().getCallingAccount();

        // check that the project exists
        Project project = _projectDao.findById(projectId);

        if (project == null) {
            throw new InvalidParameterValueException("Unable to find the project id=" + projectId);
        }

        // verify permissions - only accounts belonging to the project can list
        // project's account
        if (!_accountMgr.isAdmin(caller.getId()) && _projectAccountDao.findByProjectIdAccountId(projectId, caller.getAccountId()) == null) {
            throw new PermissionDeniedException("Account " + caller + " is not authorized to list users of the project id=" + projectId);
        }

        Filter searchFilter = new Filter(ProjectAccountJoinVO.class, "id", false, startIndex, pageSizeVal);
        SearchBuilder<ProjectAccountJoinVO> sb = _projectAccountJoinDao.createSearchBuilder();
        sb.and("accountRole", sb.entity().getAccountRole(), Op.EQ);
        sb.and("projectId", sb.entity().getProjectId(), Op.EQ);

        if (accountName != null) {
            sb.and("accountName", sb.entity().getAccountName(), Op.EQ);
        }

        SearchCriteria<ProjectAccountJoinVO> sc = sb.create();

        sc.setParameters("projectId", projectId);

        if (role != null) {
            sc.setParameters("accountRole", role);
        }

        if (accountName != null) {
            sc.setParameters("accountName", accountName);
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
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
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
            sc.setParameters("name", "%" + name + "%");
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
        sb.and("storageId", sb.entity().getPoolUuid(), SearchCriteria.Op.EQ);
        sb.and("diskOfferingId", sb.entity().getDiskOfferingId(), SearchCriteria.Op.EQ);
        sb.and("display", sb.entity().isDisplayVolume(), SearchCriteria.Op.EQ);
        // Only return volumes that are not destroyed
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.NEQ);
        sb.and("systemUse", sb.entity().isSystemUse(), SearchCriteria.Op.NEQ);
        // display UserVM volumes only
        sb.and().op("type", sb.entity().getVmType(), SearchCriteria.Op.NIN);
        sb.or("nulltype", sb.entity().getVmType(), SearchCriteria.Op.NULL);
        sb.cp();

        // now set the SC criteria...
        SearchCriteria<VolumeJoinVO> sc = sb.create();
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (keyword != null) {
            SearchCriteria<VolumeJoinVO> ssc = _volumeJoinDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("volumeType", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.setParameters("name", name);
        }

        if (display != null) {
            sc.setParameters("display", display);
        }

        setIdsListToSearchCriteria(sc, ids);

        sc.setParameters("systemUse", 1);

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
            sc.setParameters("storageId", storageId);
        }

        if (clusterId != null) {
            sc.setParameters("clusterId", clusterId);
        }
        // Don't return DomR and ConsoleProxy volumes
        sc.setParameters("type", VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm, VirtualMachine.Type.DomainRouter);

        // Only return volumes that are not destroyed
        sc.setParameters("state", Volume.State.Destroy);

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
            if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
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
        sc.setParameters("typeNEQ", Account.ACCOUNT_TYPE_PROJECT);

        // don't return system account...
        sc.setParameters("idNEQ", Account.ACCOUNT_ID_SYSTEM);

        // do not return account of type domain admin to the end user
        if (!callerIsAdmin) {
            sc.setParameters("type2NEQ", Account.ACCOUNT_TYPE_DOMAIN_ADMIN);
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

        Object keyword = cmd.getKeyword();
        Object startDate = cmd.getStartDate();

        SearchCriteria<AsyncJobJoinVO> sc = sb.create();
        if (listProjectResourcesCriteria != null) {
            sc.setParameters("type", Account.ACCOUNT_TYPE_PROJECT);
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
                    poolResponse.setCaps(driver.getCapabilities());
                }
            }
        }

        response.setResponses(poolResponses, result.second());
        return response;
    }

    private Pair<List<StoragePoolJoinVO>, Integer> searchForStoragePoolsInternal(ListStoragePoolsCmd cmd) {
        ScopeType scopeType = null;
        if (cmd.getScope() != null) {
            try {
                scopeType = Enum.valueOf(ScopeType.class, cmd.getScope().toUpperCase());
            } catch (Exception e) {
                throw new InvalidParameterValueException("Invalid scope type: " + cmd.getScope());
            }
        }

        Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(CallContext.current().getCallingAccount(), cmd.getZoneId());
        Object id = cmd.getId();
        Object name = cmd.getStoragePoolName();
        Object path = cmd.getPath();
        Object pod = cmd.getPodId();
        Object cluster = cmd.getClusterId();
        Object address = cmd.getIpAddress();
        Object keyword = cmd.getKeyword();
        Long startIndex = cmd.getStartIndex();
        Long pageSize = cmd.getPageSizeVal();

        Filter searchFilter = new Filter(StoragePoolJoinVO.class, "id", Boolean.TRUE, startIndex, pageSize);

        SearchBuilder<StoragePoolJoinVO> sb = _poolJoinDao.createSearchBuilder();
        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct
        // ids
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("path", sb.entity().getPath(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getZoneId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        sb.and("clusterId", sb.entity().getClusterId(), SearchCriteria.Op.EQ);
        sb.and("hostAddress", sb.entity().getHostAddress(), SearchCriteria.Op.EQ);
        sb.and("scope", sb.entity().getScope(), SearchCriteria.Op.EQ);

        SearchCriteria<StoragePoolJoinVO> sc = sb.create();

        if (keyword != null) {
            SearchCriteria<StoragePoolJoinVO> ssc = _poolJoinDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("poolType", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (name != null) {
            sc.setParameters("name", name);
        }

        if (path != null) {
            sc.setParameters("path", path);
        }
        if (zoneId != null) {
            sc.setParameters("dataCenterId", zoneId);
        }
        if (pod != null) {
            sc.setParameters("podId", pod);
        }
        if (address != null) {
            sc.setParameters("hostAddress", address);
        }
        if (cluster != null) {
            sc.setParameters("clusterId", cluster);
        }
        if (scopeType != null) {
            sc.setParameters("scope", scopeType.toString());
        }

        // search Pool details by ids
        Pair<List<StoragePoolJoinVO>, Integer> uniquePoolPair = _poolJoinDao.searchAndCount(sc, searchFilter);
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
        SearchCriteria<DiskOfferingJoinVO> sc = _diskOfferingJoinDao.createSearchCriteria();
        sc.addAnd("type", Op.EQ, DiskOfferingVO.Type.Disk);

        Account account = CallContext.current().getCallingAccount();
        Object name = cmd.getDiskOfferingName();
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();
        Long domainId = cmd.getDomainId();
        Boolean isRootAdmin = _accountMgr.isRootAdmin(account.getAccountId());
        Boolean isRecursive = cmd.isRecursive();
        Long zoneId = cmd.getZoneId();
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
        if ((_accountMgr.isNormalUser(account.getId()) || _accountMgr.isDomainAdmin(account.getId())) || account.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
            if (isRecursive) { // domain + all sub-domains
                if (account.getType() == Account.ACCOUNT_TYPE_NORMAL) {
                    throw new InvalidParameterValueException("Only ROOT admins and Domain admins can list disk offerings with isrecursive=true");
                }
            } else { // domain + all ancestors
                sc.addAnd("systemUse", SearchCriteria.Op.EQ, false); // non-root users should not see system offering at all
            }

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

        if (zoneId != null) {
            SearchBuilder<DiskOfferingJoinVO> sb = _diskOfferingJoinDao.createSearchBuilder();
            sb.and("zoneId", sb.entity().getZoneId(), Op.FIND_IN_SET);
            sb.or("zId", sb.entity().getZoneId(), Op.NULL);
            sb.done();
            SearchCriteria<DiskOfferingJoinVO> zoneSC = sb.create();
            zoneSC.setParameters("zoneId", String.valueOf(zoneId));
            sc.addAnd("zoneId", SearchCriteria.Op.SC, zoneSC);
        }

        // FIXME: disk offerings should search back up the hierarchy for
        // available disk offerings...
        /*
         * sb.addAnd("domainId", sb.entity().getDomainId(),
         * SearchCriteria.Op.EQ); if (domainId != null) {
         * SearchBuilder<DomainVO> domainSearch =
         * _domainDao.createSearchBuilder(); domainSearch.addAnd("path",
         * domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
         * sb.join("domainSearch", domainSearch, sb.entity().getDomainId(),
         * domainSearch.entity().getId()); }
         */

        // FIXME: disk offerings should search back up the hierarchy for
        // available disk offerings...
        /*
         * if (domainId != null) { sc.setParameters("domainId", domainId); //
         * //DomainVO domain = _domainDao.findById((Long)domainId); // // I want
         * to join on user_vm.domain_id = domain.id where domain.path like
         * 'foo%' //sc.setJoinParameters("domainSearch", "path",
         * domain.getPath() + "%"); // }
         */

        Pair<List<DiskOfferingJoinVO>, Integer> result = _diskOfferingJoinDao.searchAndCount(sc, searchFilter);
        // Remove offerings that are not associated with caller's domain
        if (account.getType() != Account.ACCOUNT_TYPE_ADMIN && CollectionUtils.isNotEmpty(result.first())) {
            ListIterator<DiskOfferingJoinVO> it = result.first().listIterator();
            while (it.hasNext()) {
                DiskOfferingJoinVO offering = it.next();
                if(!Strings.isNullOrEmpty(offering.getDomainId())) {
                    boolean toRemove = true;
                    String[] domainIdsArray = offering.getDomainId().split(",");
                    for (String domainIdString : domainIdsArray) {
                        Long dId = Long.valueOf(domainIdString.trim());
                        if (isRecursive) {
                            if (_domainDao.isChildDomain(account.getDomainId(), dId)) {
                                toRemove = false;
                                break;
                            }
                        } else {
                            if (_domainDao.isChildDomain(dId, account.getDomainId())) {
                                toRemove = false;
                                break;
                            }
                        }
                    }
                    if (toRemove) {
                        it.remove();
                    }
                }
            }
        }
        return new Pair<>(result.first(), result.first().size());
    }

    private List<ServiceOfferingJoinVO> filterOfferingsOnCurrentTags(List<ServiceOfferingJoinVO> offerings, ServiceOfferingVO currentVmOffering) {
        if (currentVmOffering == null) {
            return offerings;
        }
        List<String> currentTagsList = StringUtils.csvTagsToList(currentVmOffering.getTags());

        // New service offering should have all the tags of the current service offering.
        List<ServiceOfferingJoinVO> filteredOfferings = new ArrayList<>();
        for (ServiceOfferingJoinVO offering : offerings) {
            List<String> newTagsList = StringUtils.csvTagsToList(offering.getTags());
            if (newTagsList.containsAll(currentTagsList)) {
                filteredOfferings.add(offering);
            }
        }
        return filteredOfferings;
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
            sc.addAnd("id", SearchCriteria.Op.NEQ, currentVmOffering.getId());

            // 1. Only return offerings with the same storage type
            sc.addAnd("useLocalStorage", SearchCriteria.Op.EQ, currentVmOffering.isUseLocalStorage());

            // 2.In case vm is running return only offerings greater than equal to current offering compute.
            if (vmInstance.getState() == VirtualMachine.State.Running) {
                sc.addAnd("cpu", Op.GTEQ, currentVmOffering.getCpu());
                sc.addAnd("speed", Op.GTEQ, currentVmOffering.getSpeed());
                sc.addAnd("ramSize", Op.GTEQ, currentVmOffering.getRamSize());
            }
        }

        // boolean includePublicOfferings = false;
        if ((_accountMgr.isNormalUser(caller.getId()) || _accountMgr.isDomainAdmin(caller.getId())) || caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
            // For non-root users.
            if (isSystem) {
                throw new InvalidParameterValueException("Only root admins can access system's offering");
            }
            if (isRecursive) { // domain + all sub-domains
                if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL) {
                    throw new InvalidParameterValueException("Only ROOT admins and Domain admins can list service offerings with isrecursive=true");
                }
            }/* else { // domain + all ancestors
                // find all domain Id up to root domain for this account
                List<Long> domainIds = new ArrayList<Long>();
                DomainVO domainRecord;
                if (vmId != null) {
                    UserVmVO vmInstance = _userVmDao.findById(vmId);
                    domainRecord = _domainDao.findById(vmInstance.getDomainId());
                    if (domainRecord == null) {
                        s_logger.error("Could not find the domainId for vmId:" + vmId);
                        throw new CloudAuthenticationException("Could not find the domainId for vmId:" + vmId);
                    }
                } else {
                    domainRecord = _domainDao.findById(caller.getDomainId());
                    if (domainRecord == null) {
                        s_logger.error("Could not find the domainId for account:" + caller.getAccountName());
                        throw new CloudAuthenticationException("Could not find the domainId for account:" + caller.getAccountName());
                    }
                }
                domainIds.add(domainRecord.getId());
                while (domainRecord.getParent() != null) {
                    domainRecord = _domainDao.findById(domainRecord.getParent());
                    domainIds.add(domainRecord.getId());
                }

                SearchCriteria<ServiceOfferingJoinVO> spc = _srvOfferingJoinDao.createSearchCriteria();
                spc.addOr("domainId", SearchCriteria.Op.IN, domainIds.toArray());
                spc.addOr("domainId", SearchCriteria.Op.NULL); // include public offering as well
                sc.addAnd("domainId", SearchCriteria.Op.SC, spc);
            }*/
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
        }

        Pair<List<ServiceOfferingJoinVO>, Integer> result = _srvOfferingJoinDao.searchAndCount(sc, searchFilter);

        //Couldn't figure out a smart way to filter offerings based on tags in sql so doing it in Java.
        List<ServiceOfferingJoinVO> filteredOfferings = filterOfferingsOnCurrentTags(result.first(), currentVmOffering);
        // Remove offerings that are not associated with caller's domain
        if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN && CollectionUtils.isNotEmpty(filteredOfferings)) {
            ListIterator<ServiceOfferingJoinVO> it = filteredOfferings.listIterator();
            while (it.hasNext()) {
                ServiceOfferingJoinVO offering = it.next();
                if(!Strings.isNullOrEmpty(offering.getDomainId())) {
                    boolean toRemove = true;
                    String[] domainIdsArray = offering.getDomainId().split(",");
                    for (String domainIdString : domainIdsArray) {
                        Long dId = Long.valueOf(domainIdString.trim());
                        if (isRecursive) {
                            if (_domainDao.isChildDomain(caller.getDomainId(), dId)) {
                                toRemove = false;
                                break;
                            }
                        } else {
                            if (_domainDao.isChildDomain(dId, caller.getDomainId())) {
                                toRemove = false;
                                break;
                            }
                        }
                    }
                    if (toRemove) {
                        it.remove();
                    }
                }
            }
        }
        return new Pair<>(filteredOfferings, filteredOfferings.size());
    }

    @Override
    public ListResponse<ZoneResponse> listDataCenters(ListZonesCmd cmd) {
        Pair<List<DataCenterJoinVO>, Integer> result = listDataCentersInternal(cmd);
        ListResponse<ZoneResponse> response = new ListResponse<ZoneResponse>();

        ResponseView respView = ResponseView.Restricted;
        if (cmd instanceof ListZonesCmdByAdmin) {
            respView = ResponseView.Full;
        }

        List<ZoneResponse> dcResponses = ViewResponseHelper.createDataCenterResponse(respView, cmd.getShowCapacities(), result.first().toArray(new DataCenterJoinVO[result.first().size()]));
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

            } else if (_accountMgr.isDomainAdmin(account.getId()) || account.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
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

        List<TemplateResponse> templateResponses = ViewResponseHelper.createTemplateResponse(respView, result.first().toArray(new TemplateJoinVO[result.first().size()]));
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
            if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL) {
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
                showDomr, cmd.listInReadyState(), permittedAccounts, caller, listProjectResourcesCriteria, tags, showRemovedTmpl, cmd.getIds(), parentTemplateId);
    }

    private Pair<List<TemplateJoinVO>, Integer> searchForTemplatesInternal(Long templateId, String name, String keyword, TemplateFilter templateFilter, boolean isIso, Boolean bootable, Long pageSize,
            Long startIndex, Long zoneId, HypervisorType hyperType, boolean showDomr, boolean onlyReady, List<Account> permittedAccounts, Account caller,
            ListProjectResourcesCriteria listProjectResourcesCriteria, Map<String, String> tags, boolean showRemovedTmpl, List<Long> ids, Long parentTemplateId) {

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
        sb.select(null, Func.DISTINCT, sb.entity().getTempZonePair()); // select distinct (templateId, zoneId) pair
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
            if (!template.isPublicTemplate() && caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
                Account template_acc = _accountMgr.getAccount(template.getAccountId());
                DomainVO domain = _domainDao.findById(template_acc.getDomainId());
                _accountMgr.checkAccess(caller, domain);
            }

            // if template is not public, perform permission check here
            else if (!template.isPublicTemplate() && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
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

            // List<HypervisorType> hypers = null;
            // if (!isIso) {
            // hypers = _resourceMgr.listAvailHypervisorInZone(null, null);
            // }

            setIdsListToSearchCriteria(sc, ids);

            // add criteria for project or not
            if (listProjectResourcesCriteria == ListProjectResourcesCriteria.SkipProjectResources) {
                sc.addAnd("accountType", SearchCriteria.Op.NEQ, Account.ACCOUNT_TYPE_PROJECT);
            } else if (listProjectResourcesCriteria == ListProjectResourcesCriteria.ListProjectResourcesOnly) {
                sc.addAnd("accountType", SearchCriteria.Op.EQ, Account.ACCOUNT_TYPE_PROJECT);
            }

            // add criteria for domain path in case of domain admin
            if ((templateFilter == TemplateFilter.self || templateFilter == TemplateFilter.selfexecutable)
                    && (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN || caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN)) {
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
            } else if (templateFilter == TemplateFilter.all && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
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

            if (isIso) {
                sc.addAnd("format", SearchCriteria.Op.EQ, "ISO");

            } else {
                sc.addAnd("format", SearchCriteria.Op.NEQ, "ISO");
            }

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
        }

        if (zoneId != null) {
            SearchCriteria<TemplateJoinVO> zoneSc = _templateJoinDao.createSearchCriteria();
            zoneSc.addOr("dataCenterId", SearchCriteria.Op.EQ, zoneId);
            zoneSc.addOr("dataStoreScope", SearchCriteria.Op.EQ, ScopeType.REGION);
            // handle the case where xs-tools.iso and vmware-tools.iso do not
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
            final String[] distinctColumns = {"temp_zone_pair"};
            uniqueTmplPair = _templateJoinDao.searchAndDistinctCount(sc, searchFilter, distinctColumns);
        }

        Integer count = uniqueTmplPair.second();
        if (count.intValue() == 0) {
            // empty result
            return uniqueTmplPair;
        }
        List<TemplateJoinVO> uniqueTmpls = uniqueTmplPair.first();
        String[] tzIds = new String[uniqueTmpls.size()];
        int i = 0;
        for (TemplateJoinVO v : uniqueTmpls) {
            tzIds[i++] = v.getTempZonePair();
        }
        List<TemplateJoinVO> vrs = _templateJoinDao.searchByTemplateZonePair(showRemovedTmpl, tzIds);
        return new Pair<List<TemplateJoinVO>, Integer>(vrs, count);

        // TODO: revisit the special logic for iso search in
        // VMTemplateDaoImpl.searchForTemplates and understand why we need to
        // specially handle ISO. The original logic is very twisted and no idea
        // about what the code was doing.

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
            if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL) {
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
                hypervisorType, true, cmd.listInReadyState(), permittedAccounts, caller, listProjectResourcesCriteria, tags, showRemovedISO, null, null);
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
                if (!Strings.isNullOrEmpty(resourceUuid) && ResourceObjectType.Template.equals(type)) {
                    hypervisorType = _templateDao.findByUuid(resourceUuid).getHypervisorType();
                }
                if (!Strings.isNullOrEmpty(resourceUuid) && ResourceObjectType.UserVm.equals(type)) {
                    hypervisorType = _vmInstanceDao.findByUuid(resourceUuid).getHypervisorType();
                }
                fillVMOrTemplateDetailOptions(options, hypervisorType);
                break;
            default:
                throw new CloudRuntimeException("Resource type not supported.");
        }
        if (CallContext.current().getCallingAccount().getType() != Account.ACCOUNT_TYPE_ADMIN) {
            final List<String> userBlacklistedSettings = Stream.of(QueryService.UserVMBlacklistedDetails.value().split(","))
                    .map(item -> (item).trim())
                    .collect(Collectors.toList());
            for (final String detail : userBlacklistedSettings) {
                if (options.containsKey(detail)) {
                    options.remove(detail);
                }
            }
        }
        return new DetailOptionsResponse(options);
    }

    private void fillVMOrTemplateDetailOptions(final Map<String, List<String>> options, final HypervisorType hypervisorType) {
        if (options == null) {
            throw new CloudRuntimeException("Invalid/null detail-options response object passed");
        }

        options.put(VmDetailConstants.KEYBOARD, Arrays.asList("uk", "us", "jp", "fr"));
        options.put(VmDetailConstants.CPU_CORE_PER_SOCKET, Collections.emptyList());
        options.put(VmDetailConstants.ROOT_DISK_SIZE, Collections.emptyList());

        if (HypervisorType.KVM.equals(hypervisorType)) {
            options.put(VmDetailConstants.ROOT_DISK_CONTROLLER, Arrays.asList("osdefault", "ide", "scsi", "virtio"));
        }

        if (HypervisorType.VMware.equals(hypervisorType)) {
            options.put(VmDetailConstants.NIC_ADAPTER, Arrays.asList("E1000", "PCNet32", "Vmxnet2", "Vmxnet3"));
            options.put(VmDetailConstants.ROOT_DISK_CONTROLLER, Arrays.asList("osdefault", "ide", "scsi", "lsilogic", "lsisas1068", "buslogic", "pvscsi"));
            options.put(VmDetailConstants.DATA_DISK_CONTROLLER, Arrays.asList("osdefault", "ide", "scsi", "lsilogic", "lsisas1068", "buslogic", "pvscsi"));
            options.put(VmDetailConstants.NESTED_VIRTUALIZATION_FLAG, Arrays.asList("true", "false"));
            options.put(VmDetailConstants.SVGA_VRAM_SIZE, Collections.emptyList());
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
                affinityGroups.addAll(listDomainLevelAffinityGroups(scDomain, searchFilter, domainId));
            } else {

                for (Long permAcctId : permittedAccounts) {
                    Account permittedAcct = _accountDao.findById(permAcctId);
                    SearchCriteria<AffinityGroupJoinVO> scDomain = buildAffinityGroupSearchCriteria(null, isRecursive, new ArrayList<Long>(), listProjectResourcesCriteria, affinityGroupId,
                            affinityGroupName, affinityGroupType, keyword);

                    affinityGroups.addAll(listDomainLevelAffinityGroups(scDomain, searchFilter, permittedAcct.getDomainId()));
                }
            }
        } else if (((permittedAccounts.isEmpty()) && (domainId != null) && isRecursive)) {
            // list all domain level affinity groups for the domain admin case
            SearchCriteria<AffinityGroupJoinVO> scDomain = buildAffinityGroupSearchCriteria(null, isRecursive, new ArrayList<Long>(), listProjectResourcesCriteria, affinityGroupId, affinityGroupName,
                    affinityGroupType, keyword);
            affinityGroups.addAll(listDomainLevelAffinityGroups(scDomain, searchFilter, domainId));
        }

        return new Pair<List<AffinityGroupJoinVO>, Integer>(affinityGroups, affinityGroups.size());

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
            sc.setParameters("accountType", Account.ACCOUNT_TYPE_PROJECT);
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

    private List<AffinityGroupJoinVO> listDomainLevelAffinityGroups(SearchCriteria<AffinityGroupJoinVO> sc, Filter searchFilter, long domainId) {
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
                return new ArrayList<AffinityGroupJoinVO>();
            }
            List<AffinityGroupJoinVO> uniqueGroups = uniqueGroupsPair.first();
            Long[] vrIds = new Long[uniqueGroups.size()];
            int i = 0;
            for (AffinityGroupJoinVO v : uniqueGroups) {
                vrIds[i++] = v.getId();
            }
            List<AffinityGroupJoinVO> vrs = _affinityGroupJoinDao.searchByIds(vrIds);
            return vrs;
        } else {
            return new ArrayList<AffinityGroupJoinVO>();
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

        //Validation - 1.1 - resourceId and value cant be null.
        if (resourceIdStr == null && value == null) {
            throw new InvalidParameterValueException("Insufficient parameters passed for listing by resourceId OR key,value pair. Please check your params and try again.");
        }

        //Validation - 1.2 - Value has to be passed along with key.
        if (value != null && key == null) {
            throw new InvalidParameterValueException("Listing by (key, value) but key is null. Please check the params and try again");
        }

        //Validation - 1.3
        if (resourceIdStr != null) {
            resourceId = _taggedResourceMgr.getResourceId(resourceIdStr, resourceType);
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
        resourceDetailResponse.setResourceId(_taggedResourceMgr.getUuid(String.valueOf(requestedDetail.getResourceId()), resourceType));
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
        List<ManagementServerResponse> result = new ArrayList<>();
        for (ManagementServerHostVO mgmt : managementServerHostDao.listAll()) {
            ManagementServerResponse mgmtResponse = new ManagementServerResponse();
            mgmtResponse.setId(mgmt.getUuid());
            mgmtResponse.setName(mgmt.getName());
            mgmtResponse.setState(mgmt.getState());
            mgmtResponse.setVersion(mgmt.getVersion());
            mgmtResponse.setObjectName("managementserver");
            result.add(mgmtResponse);
        }
        response.setResponses(result);
        return response;
    }

    @Override
    public ListResponse<TemplateOVFPropertyResponse> listTemplateOVFProperties(ListTemplateOVFProperties cmd) {
        ListResponse<TemplateOVFPropertyResponse> response = new ListResponse<>();
        List<TemplateOVFPropertyResponse> result = new ArrayList<>();
        Long templateId = cmd.getTemplateId();
        List<TemplateOVFPropertyVO> ovfProperties = templateOVFPropertiesDao.listByTemplateId(templateId);
        for (OVFProperty property : ovfProperties) {
            TemplateOVFPropertyResponse propertyResponse = new TemplateOVFPropertyResponse();
            propertyResponse.setKey(property.getKey());
            propertyResponse.setType(property.getType());
            propertyResponse.setValue(property.getValue());
            propertyResponse.setQualifiers(property.getQualifiers());
            propertyResponse.setUserConfigurable(property.isUserConfigurable());
            propertyResponse.setLabel(property.getLabel());
            propertyResponse.setDescription(property.getDescription());
            propertyResponse.setPassword(property.isPassword());
            propertyResponse.setObjectName("ovfproperty");
            result.add(propertyResponse);
        }
        response.setResponses(result);
        return response;
    }

    @Override
    public String getConfigComponentName() {
        return QueryService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {AllowUserViewDestroyedVM, UserVMBlacklistedDetails, UserVMReadOnlyUIDetails, SortKeyAscending, AllowUserViewAllDomainAccounts};
    }
}
