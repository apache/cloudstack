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
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.affinity.AffinityGroupResponse;
import org.apache.cloudstack.affinity.AffinityGroupVMMapVO;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.command.admin.host.ListHostsCmd;
import org.apache.cloudstack.api.command.admin.internallb.ListInternalLBVMsCmd;
import org.apache.cloudstack.api.command.admin.router.ListRoutersCmd;
import org.apache.cloudstack.api.command.admin.storage.ListCacheStoresCmd;
import org.apache.cloudstack.api.command.admin.storage.ListImageStoresCmd;
import org.apache.cloudstack.api.command.admin.storage.ListStoragePoolsCmd;
import org.apache.cloudstack.api.command.admin.user.ListUsersCmd;
import org.apache.cloudstack.api.command.user.account.ListAccountsCmd;
import org.apache.cloudstack.api.command.user.account.ListProjectAccountsCmd;
import org.apache.cloudstack.api.command.user.event.ListEventsCmd;
import org.apache.cloudstack.api.command.user.iso.ListIsosCmd;
import org.apache.cloudstack.api.command.user.job.ListAsyncJobsCmd;
import org.apache.cloudstack.api.command.user.offering.ListDiskOfferingsCmd;
import org.apache.cloudstack.api.command.user.offering.ListServiceOfferingsCmd;
import org.apache.cloudstack.api.command.user.project.ListProjectInvitationsCmd;
import org.apache.cloudstack.api.command.user.project.ListProjectsCmd;
import org.apache.cloudstack.api.command.user.securitygroup.ListSecurityGroupsCmd;
import org.apache.cloudstack.api.command.user.tag.ListTagsCmd;
import org.apache.cloudstack.api.command.user.template.ListTemplatesCmd;
import org.apache.cloudstack.api.command.user.vm.ListVMsCmd;
import org.apache.cloudstack.api.command.user.vmgroup.ListVMGroupsCmd;
import org.apache.cloudstack.api.command.user.volume.ListResourceDetailsCmd;
import org.apache.cloudstack.api.command.user.volume.ListVolumesCmd;
import org.apache.cloudstack.api.command.user.zone.ListZonesByCmd;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.AsyncJobResponse;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.api.response.EventResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ImageStoreResponse;
import org.apache.cloudstack.api.response.InstanceGroupResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ProjectAccountResponse;
import org.apache.cloudstack.api.response.ProjectInvitationResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ResourceDetailResponse;
import org.apache.cloudstack.api.response.ResourceTagResponse;
import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateState;
import org.apache.cloudstack.query.QueryService;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.api.query.dao.AccountJoinDao;
import com.cloud.api.query.dao.AffinityGroupJoinDao;
import com.cloud.api.query.dao.AsyncJobJoinDao;
import com.cloud.api.query.dao.DataCenterJoinDao;
import com.cloud.api.query.dao.DiskOfferingJoinDao;
import com.cloud.api.query.dao.DomainRouterJoinDao;
import com.cloud.api.query.dao.HostJoinDao;
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
import com.cloud.api.query.vo.DomainRouterJoinVO;
import com.cloud.api.query.vo.EventJoinVO;
import com.cloud.api.query.vo.HostJoinVO;
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
import com.cloud.configuration.dao.ConfigurationDao;
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
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.security.SecurityGroupVMMapVO;
import com.cloud.network.security.dao.SecurityGroupVMMapDao;
import com.cloud.org.Grouping;
import com.cloud.projects.Project;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.projects.ProjectInvitation;
import com.cloud.projects.ProjectManager;
import com.cloud.projects.dao.ProjectAccountDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.resource.ResourceManager;
import com.cloud.server.Criteria;
import com.cloud.server.ResourceMetaDataService;
import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.server.TaggedResourceService;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.template.VirtualMachineTemplate.TemplateFilter;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicDetailVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDetailDao;
import com.cloud.vm.dao.UserVmDao;

@Component
@Local(value = { QueryService.class })
public class QueryManagerImpl extends ManagerBase implements QueryService {

    public static final Logger s_logger = Logger.getLogger(QueryManagerImpl.class);

    // public static ViewResponseHelper _responseGenerator;

    @Inject
    private AccountManager _accountMgr;

    @Inject
    private ProjectManager _projectMgr;

    @Inject
    private DomainDao _domainDao;

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
    private ImageStoreJoinDao _imageStoreJoinDao;

    @Inject
    private DiskOfferingJoinDao _diskOfferingJoinDao;

    @Inject
    private ServiceOfferingJoinDao _srvOfferingJoinDao;

    @Inject
    private ServiceOfferingDao _srvOfferingDao;

    @Inject
    private DataCenterJoinDao _dcJoinDao;

    @Inject
    private DomainRouterDao _routerDao;

    @Inject
    private VolumeDetailsDao _volumeDetailDao;

    @Inject
    private NicDetailDao _nicDetailDao;

    @Inject
    private HighAvailabilityManager _haMgr;

    @Inject
    private VMTemplateDao _templateDao;

    @Inject
    private TemplateJoinDao _templateJoinDao;

    @Inject
    ResourceManager _resourceMgr;
    @Inject
    private ResourceMetaDataService _resourceMetaDataMgr;

    @Inject
    private TaggedResourceService _taggedResourceMgr;

    @Inject
    AffinityGroupVMMapDao _affinityGroupVMMapDao;

    @Inject
    private AffinityGroupJoinDao _affinityGroupJoinDao;

    @Inject
    private DedicatedResourceDao _dedicatedDao;

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
        List<UserResponse> userResponses = ViewResponseHelper.createUserResponse(result.first().toArray(
                new UserAccountJoinVO[result.first().size()]));
        response.setResponses(userResponses, result.second());
        return response;
    }

    private Pair<List<UserAccountJoinVO>, Integer> searchForUsersInternal(ListUsersCmd cmd)
            throws PermissionDeniedException {
        Account caller = CallContext.current().getCallingAccount();

        // TODO: Integrate with ACL checkAccess refactoring
        Long domainId = cmd.getDomainId();
        if (domainId != null) {
            Domain domain = _domainDao.findById(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Unable to find domain by id=" + domainId);
            }

            _accountMgr.checkAccess(caller, domain);
        } else {
            // default domainId to the caller's domain
            domainId = caller.getDomainId();
        }

        Filter searchFilter = new Filter(UserAccountJoinVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        Long id = cmd.getId();
        Object username = cmd.getUsername();
        Object type = cmd.getAccountType();
        Object accountName = cmd.getAccountName();
        Object state = cmd.getState();
        Object keyword = cmd.getKeyword();

        SearchBuilder<UserAccountJoinVO> sb = _userAccountJoinDao.createSearchBuilder();
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
        if (keyword != null) {
            SearchCriteria<UserAccountJoinVO> ssc = _userAccountJoinDao.createSearchCriteria();
            ssc.addOr("username", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("firstname", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("lastname", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("email", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("accountName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("type", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("accountState", SearchCriteria.Op.LIKE, "%" + keyword + "%");

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
        List<EventResponse> eventResponses = ViewResponseHelper.createEventResponse(result.first().toArray(
                new EventJoinVO[result.first().size()]));
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

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(
                cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts,
                domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(EventJoinVO.class, "createDate", false, cmd.getStartIndex(),
                cmd.getPageSizeVal());
        SearchBuilder<EventJoinVO> sb = _eventJoinDao.createSearchBuilder();
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("levelL", sb.entity().getLevel(), SearchCriteria.Op.LIKE);
        sb.and("levelEQ", sb.entity().getLevel(), SearchCriteria.Op.EQ);
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);
        sb.and("createDateB", sb.entity().getCreateDate(), SearchCriteria.Op.BETWEEN);
        sb.and("createDateG", sb.entity().getCreateDate(), SearchCriteria.Op.GTEQ);
        sb.and("createDateL", sb.entity().getCreateDate(), SearchCriteria.Op.LTEQ);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.NEQ);
        sb.and("startId", sb.entity().getStartId(), SearchCriteria.Op.EQ);
        sb.and("createDate", sb.entity().getCreateDate(), SearchCriteria.Op.BETWEEN);
        sb.and("archived", sb.entity().getArchived(), SearchCriteria.Op.EQ);

        SearchCriteria<EventJoinVO> sc = sb.create();
        // building ACL condition
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);

        if (id != null) {
            sc.setParameters("id", id);
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
        List<ResourceTagResponse> tagResponses = ViewResponseHelper.createResourceTagResponse(false, tags.first()
                .toArray(new ResourceTagJoinVO[tags.first().size()]));
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

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(
                cmd.getDomainId(), cmd.isRecursive(), null);

        _accountMgr.buildACLSearchParameters(caller, null, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts,
                domainIdRecursiveListProject, listAll, false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        Filter searchFilter = new Filter(ResourceTagJoinVO.class, "resourceType", false, cmd.getStartIndex(),
                cmd.getPageSizeVal());

        SearchBuilder<ResourceTagJoinVO> sb = _resourceTagJoinDao.createSearchBuilder();
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);

        sb.and("key", sb.entity().getKey(), SearchCriteria.Op.EQ);
        sb.and("value", sb.entity().getValue(), SearchCriteria.Op.EQ);

        if (resourceId != null) {
            sb.and().op("resourceId", sb.entity().getResourceId(), SearchCriteria.Op.EQ);
            sb.or("resourceUuid", sb.entity().getResourceUuid(), SearchCriteria.Op.EQ);
            sb.cp();
        }

        sb.and("resourceType", sb.entity().getResourceType(), SearchCriteria.Op.EQ);
        sb.and("customer", sb.entity().getCustomer(), SearchCriteria.Op.EQ);

        // now set the SC criteria...
        SearchCriteria<ResourceTagJoinVO> sc = sb.create();
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);

        if (key != null) {
            sc.setParameters("key", key);
        }

        if (value != null) {
            sc.setParameters("value", value);
        }

        if (resourceId != null) {
            sc.setParameters("resourceId", resourceId);
            sc.setParameters("resourceUuid", resourceId);
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
        List<InstanceGroupResponse> grpResponses = ViewResponseHelper.createInstanceGroupResponse(groups.first()
                .toArray(new InstanceGroupJoinVO[groups.first().size()]));
        response.setResponses(grpResponses, groups.second());
        return response;
    }

    private Pair<List<InstanceGroupJoinVO>, Integer> searchForVmGroupsInternal(ListVMGroupsCmd cmd) {
        Long id = cmd.getId();
        String name = cmd.getGroupName();
        String keyword = cmd.getKeyword();

        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<Long>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(
                cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts,
                domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(InstanceGroupJoinVO.class, "id", true, cmd.getStartIndex(),
                cmd.getPageSizeVal());

        SearchBuilder<InstanceGroupJoinVO> sb = _vmGroupJoinDao.createSearchBuilder();
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);

        SearchCriteria<InstanceGroupJoinVO> sc = sb.create();
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);

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
        List<UserVmResponse> vmResponses = ViewResponseHelper.createUserVmResponse("virtualmachine", cmd.getDetails(),
                result.first().toArray(new UserVmJoinVO[result.first().size()]));
        response.setResponses(vmResponses, result.second());
        return response;
    }

    private Pair<List<UserVmJoinVO>, Integer> searchForUserVMsInternal(ListVMsCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<Long>();
        String hypervisor = cmd.getHypervisor();
        boolean listAll = cmd.listAll();
        Long id = cmd.getId();
        Map<String, String> tags = cmd.getTags();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(
                cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts,
                domainIdRecursiveListProject, listAll, false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Criteria c = new Criteria("id", Boolean.TRUE, cmd.getStartIndex(), cmd.getPageSizeVal());
        // Criteria c = new Criteria(null, Boolean.FALSE, cmd.getStartIndex(),
        // cmd.getPageSizeVal()); //version without default sorting
        c.addCriteria(Criteria.KEYWORD, cmd.getKeyword());
        c.addCriteria(Criteria.ID, cmd.getId());
        c.addCriteria(Criteria.NAME, cmd.getInstanceName());
        c.addCriteria(Criteria.STATE, cmd.getState());
        c.addCriteria(Criteria.DATACENTERID, cmd.getZoneId());
        c.addCriteria(Criteria.GROUPID, cmd.getGroupId());
        c.addCriteria(Criteria.FOR_VIRTUAL_NETWORK, cmd.getForVirtualNetwork());
        c.addCriteria(Criteria.NETWORKID, cmd.getNetworkId());
        c.addCriteria(Criteria.TEMPLATE_ID, cmd.getTemplateId());
        c.addCriteria(Criteria.ISO_ID, cmd.getIsoId());
        c.addCriteria(Criteria.VPC_ID, cmd.getVpcId());
        c.addCriteria(Criteria.AFFINITY_GROUP_ID, cmd.getAffinityGroupId());

        if (domainId != null) {
            c.addCriteria(Criteria.DOMAINID, domainId);
        }

        if (HypervisorType.getType(hypervisor) != HypervisorType.None) {
            c.addCriteria(Criteria.HYPERVISOR, hypervisor);
        } else if (hypervisor != null) {
            throw new InvalidParameterValueException("Invalid HypervisorType " + hypervisor);
        }

        // ignore these search requests if it's not an admin
        if (_accountMgr.isAdmin(caller.getType())) {
            c.addCriteria(Criteria.PODID, cmd.getPodId());
            c.addCriteria(Criteria.HOSTID, cmd.getHostId());
            c.addCriteria(Criteria.STORAGE_ID, cmd.getStorageId());
        }

        if (!permittedAccounts.isEmpty()) {
            c.addCriteria(Criteria.ACCOUNTID, permittedAccounts.toArray());
        }
        c.addCriteria(Criteria.ISADMIN, _accountMgr.isAdmin(caller.getType()));

        return searchForUserVMsByCriteria(c, caller, domainId, isRecursive, permittedAccounts, listAll,
                listProjectResourcesCriteria, tags);
    }

    private Pair<List<UserVmJoinVO>, Integer> searchForUserVMsByCriteria(Criteria c, Account caller, Long domainId,
            boolean isRecursive, List<Long> permittedAccounts, boolean listAll,
            ListProjectResourcesCriteria listProjectResourcesCriteria, Map<String, String> tags) {
        Filter searchFilter = new Filter(UserVmJoinVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(),
                c.getLimit());

        // first search distinct vm id by using query criteria and pagination
        SearchBuilder<UserVmJoinVO> sb = _userVmJoinDao.createSearchBuilder();
        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct
        // ids
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);

        Object id = c.getCriteria(Criteria.ID);
        Object name = c.getCriteria(Criteria.NAME);
        Object state = c.getCriteria(Criteria.STATE);
        Object notState = c.getCriteria(Criteria.NOTSTATE);
        Object zoneId = c.getCriteria(Criteria.DATACENTERID);
        Object pod = c.getCriteria(Criteria.PODID);
        Object hostId = c.getCriteria(Criteria.HOSTID);
        Object hostName = c.getCriteria(Criteria.HOSTNAME);
        Object keyword = c.getCriteria(Criteria.KEYWORD);
        Object isAdmin = c.getCriteria(Criteria.ISADMIN);
        assert c.getCriteria(Criteria.IPADDRESS) == null : "We don't support search by ip address on VM any more.  If you see this assert, it means we have to find a different way to search by the nic table.";
        Object groupId = c.getCriteria(Criteria.GROUPID);
        Object networkId = c.getCriteria(Criteria.NETWORKID);
        Object hypervisor = c.getCriteria(Criteria.HYPERVISOR);
        Object storageId = c.getCriteria(Criteria.STORAGE_ID);
        Object templateId = c.getCriteria(Criteria.TEMPLATE_ID);
        Object isoId = c.getCriteria(Criteria.ISO_ID);
        Object vpcId = c.getCriteria(Criteria.VPC_ID);
        Object affinityGroupId = c.getCriteria(Criteria.AFFINITY_GROUP_ID);

        sb.and("displayName", sb.entity().getDisplayName(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getHostName(), SearchCriteria.Op.LIKE);
        sb.and("stateEQ", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("stateNEQ", sb.entity().getState(), SearchCriteria.Op.NEQ);
        sb.and("stateNIN", sb.entity().getState(), SearchCriteria.Op.NIN);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        sb.and("hypervisorType", sb.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        sb.and("hostIdEQ", sb.entity().getHostId(), SearchCriteria.Op.EQ);
        sb.and("hostName", sb.entity().getHostName(), SearchCriteria.Op.LIKE);
        sb.and("templateId", sb.entity().getTemplateId(), SearchCriteria.Op.EQ);
        sb.and("isoId", sb.entity().getIsoId(), SearchCriteria.Op.EQ);
        sb.and("instanceGroupId", sb.entity().getInstanceGroupId(), SearchCriteria.Op.EQ);

        if (groupId != null && (Long) groupId != -1) {
            sb.and("instanceGroupId", sb.entity().getInstanceGroupId(), SearchCriteria.Op.EQ);
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

        // populate the search criteria with the values passed in
        SearchCriteria<UserVmJoinVO> sc = sb.create();

        // building ACL condition
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);

        if (tags != null && !tags.isEmpty()) {
            SearchCriteria<UserVmJoinVO> tagSc = _userVmJoinDao.createSearchCriteria();
            for (String key : tags.keySet()) {
                SearchCriteria<UserVmJoinVO> tsc = _userVmJoinDao.createSearchCriteria();
                tsc.addAnd("tagKey", SearchCriteria.Op.EQ, key);
                tsc.addAnd("tagValue", SearchCriteria.Op.EQ, tags.get(key));
                tagSc.addOr("tagKey", SearchCriteria.Op.SC, tsc);
            }
            sc.addAnd("tagKey", SearchCriteria.Op.SC, tagSc);
        }

        if (groupId != null && (Long) groupId != -1) {
            sc.setParameters("instanceGroupId", groupId);
        }

        if (keyword != null) {
            SearchCriteria<UserVmJoinVO> ssc = _userVmJoinDao.createSearchCriteria();
            ssc.addOr("displayName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("hostName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("instanceName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.EQ, keyword);

            sc.addAnd("displayName", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (templateId != null) {
            sc.setParameters("templateId", templateId);
        }

        if (isoId != null) {
            sc.setParameters("isoId", isoId);
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
            if (notState != null && (Boolean) notState == true) {
                sc.setParameters("stateNEQ", state);
            } else {
                sc.setParameters("stateEQ", state);
            }
        }

        if (hypervisor != null) {
            sc.setParameters("hypervisorType", hypervisor);
        }

        // Don't show Destroyed and Expunging vms to the end user
        if ((isAdmin != null) && ((Boolean) isAdmin != true)) {
            sc.setParameters("stateNIN", "Destroyed", "Expunging");
        }

        if (zoneId != null) {
            sc.setParameters("dataCenterId", zoneId);

            if (state == null) {
                sc.setParameters("stateNEQ", "Destroyed");
            }
        }

        if (pod != null) {
            sc.setParameters("podId", pod);

            if (state == null) {
                sc.setParameters("stateNEQ", "Destroyed");
            }
        }

        if (hostId != null) {
            sc.setParameters("hostIdEQ", hostId);
        } else {
            if (hostName != null) {
                sc.setParameters("hostName", hostName);
            }
        }

        if (storageId != null) {
            sc.setParameters("poolId", storageId);
        }

        if (affinityGroupId != null) {
            sc.setParameters("affinityGroupId", affinityGroupId);
        }

        // search vm details by ids
        Pair<List<UserVmJoinVO>, Integer> uniqueVmPair = _userVmJoinDao.searchAndCount(sc, searchFilter);
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

    private Pair<List<SecurityGroupJoinVO>, Integer> searchForSecurityGroupsInternal(ListSecurityGroupsCmd cmd)
            throws PermissionDeniedException, InvalidParameterValueException {
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
                throw new InvalidParameterValueException("Unable to list network groups for virtual machine instance "
                        + instanceId + "; instance not found.");
            }
            _accountMgr.checkAccess(caller, null, true, userVM);
            return listSecurityGroupRulesByVM(instanceId.longValue(), cmd.getStartIndex(), cmd.getPageSizeVal());
        }

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(
                cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts,
                domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(SecurityGroupJoinVO.class, "id", true, cmd.getStartIndex(),
                cmd.getPageSizeVal());
        SearchBuilder<SecurityGroupJoinVO> sb = _securityGroupJoinDao.createSearchBuilder();
        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct
        // ids
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);


        SearchCriteria<SecurityGroupJoinVO> sc = sb.create();
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);

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
        Pair<List<DomainRouterJoinVO>, Integer> result = searchForRoutersInternal(cmd, cmd.getId(), cmd.getRouterName(),
                cmd.getState(), cmd.getZoneId(), cmd.getPodId(), cmd.getHostId(), cmd.getKeyword(), cmd.getNetworkId(),
                cmd.getVpcId(), cmd.getForVpc(), cmd.getRole());
        ListResponse<DomainRouterResponse> response = new ListResponse<DomainRouterResponse>();

        List<DomainRouterResponse> routerResponses = ViewResponseHelper.createDomainRouterResponse(result.first()
                .toArray(new DomainRouterJoinVO[result.first().size()]));
        response.setResponses(routerResponses, result.second());
        return response;
    }

    @Override
    public ListResponse<DomainRouterResponse> searchForInternalLbVms(ListInternalLBVMsCmd cmd) {
        Pair<List<DomainRouterJoinVO>, Integer> result = searchForRoutersInternal(cmd, cmd.getId(), cmd.getRouterName(),
                cmd.getState(), cmd.getZoneId(), cmd.getPodId(), cmd.getHostId(), cmd.getKeyword(), cmd.getNetworkId(),
                cmd.getVpcId(), cmd.getForVpc(), cmd.getRole());
        ListResponse<DomainRouterResponse> response = new ListResponse<DomainRouterResponse>();

        List<DomainRouterResponse> routerResponses = ViewResponseHelper.createDomainRouterResponse(result.first()
                .toArray(new DomainRouterJoinVO[result.first().size()]));
        response.setResponses(routerResponses, result.second());
        return response;
    }

    private Pair<List<DomainRouterJoinVO>, Integer> searchForRoutersInternal(BaseListProjectAndAccountResourcesCmd cmd, Long id,
            String name, String state, Long zoneId, Long podId, Long hostId, String keyword, Long networkId, Long vpcId, Boolean forVpc, String role) {

        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<Long>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(
                cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts,
                domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        Filter searchFilter = new Filter(DomainRouterJoinVO.class, "id", true, cmd.getStartIndex(),
                cmd.getPageSizeVal());
        // Filter searchFilter = new Filter(DomainRouterJoinVO.class, null,
        // true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<DomainRouterJoinVO> sb = _routerJoinDao.createSearchBuilder();
        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct
        // ids to get
        // number of
        // records with
        // pagination
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);

        sb.and("name", sb.entity().getInstanceName(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        sb.and("hostId", sb.entity().getHostId(), SearchCriteria.Op.EQ);
        sb.and("vpcId", sb.entity().getVpcId(), SearchCriteria.Op.EQ);
        sb.and("role", sb.entity().getRole(), SearchCriteria.Op.EQ);

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
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);

        if (keyword != null) {
            SearchCriteria<DomainRouterJoinVO> ssc = _routerJoinDao.createSearchCriteria();
            ssc.addOr("hostName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("instanceName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("hostName", SearchCriteria.Op.SC, ssc);
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
        List<ProjectResponse> projectResponses = ViewResponseHelper.createProjectResponse(projects.first().toArray(
                new ProjectJoinVO[projects.first().size()]));
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
        Map<String, String> tags = cmd.getTags();

        Account caller = CallContext.current().getCallingAccount();
        Long accountId = null;
        String path = null;

        Filter searchFilter = new Filter(ProjectJoinVO.class, "id", false, startIndex, pageSize);
        SearchBuilder<ProjectJoinVO> sb = _projectJoinDao.createSearchBuilder();
        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct
        // ids

        if (_accountMgr.isAdmin(caller.getType())) {
            if (domainId != null) {
                DomainVO domain = _domainDao.findById(domainId);
                if (domain == null) {
                    throw new InvalidParameterValueException("Domain id=" + domainId + " doesn't exist in the system");
                }

                _accountMgr.checkAccess(caller, domain);

                if (accountName != null) {
                    Account owner = _accountMgr.getActiveAccountByName(accountName, domainId);
                    if (owner == null) {
                        throw new InvalidParameterValueException("Unable to find account " + accountName
                                + " in domain " + domainId);
                    }
                    accountId = owner.getId();
                }
            } else { // domainId == null
                if (accountName != null) {
                    throw new InvalidParameterValueException("could not find account " + accountName
                            + " because domain is not specified");
                }

            }
        } else {
            if (accountName != null && !accountName.equals(caller.getAccountName())) {
                throw new PermissionDeniedException("Can't list account " + accountName + " projects; unauthorized");
            }

            if (domainId != null && domainId.equals(caller.getDomainId())) {
                throw new PermissionDeniedException("Can't list domain id= " + domainId + " projects; unauthorized");
            }

            accountId = caller.getId();
        }

        if (domainId == null && accountId == null && (caller.getType() == Account.ACCOUNT_TYPE_NORMAL || !listAll)) {
            accountId = caller.getId();
        } else if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN || (isRecursive && !listAll)) {
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
        List<ProjectInvitationResponse> projectInvitationResponses = ViewResponseHelper
                .createProjectInvitationResponse(invites.first().toArray(
                        new ProjectInvitationJoinVO[invites.first().size()]));

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

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(
                domainId, isRecursive, null);
        _accountMgr.buildACLSearchParameters(caller, id, accountName, projectId, permittedAccounts,
                domainIdRecursiveListProject, listAll, true);
        domainId = domainIdRecursiveListProject.first();
        isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(ProjectInvitationJoinVO.class, "id", true, startIndex, pageSizeVal);
        SearchBuilder<ProjectInvitationJoinVO> sb = _projectInvitationJoinDao.createSearchBuilder();
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);

        sb.and("projectId", sb.entity().getProjectId(), SearchCriteria.Op.EQ);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("created", sb.entity().getCreated(), SearchCriteria.Op.GT);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);

        SearchCriteria<ProjectInvitationJoinVO> sc = sb.create();
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);

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
            sc.setParameters("created",
                    new Date((DateUtil.currentGMTTime().getTime()) - _projectMgr.getInvitationTimeout()));
        }

        return _projectInvitationJoinDao.searchAndCount(sc, searchFilter);

    }

    @Override
    public ListResponse<ProjectAccountResponse> listProjectAccounts(ListProjectAccountsCmd cmd) {
        Pair<List<ProjectAccountJoinVO>, Integer> projectAccounts = listProjectAccountsInternal(cmd);
        ListResponse<ProjectAccountResponse> response = new ListResponse<ProjectAccountResponse>();
        List<ProjectAccountResponse> projectResponses = ViewResponseHelper.createProjectAccountResponse(projectAccounts
                .first().toArray(new ProjectAccountJoinVO[projectAccounts.first().size()]));
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
        if (!_accountMgr.isAdmin(caller.getType())
                && _projectAccountDao.findByProjectIdAccountId(projectId, caller.getAccountId()) == null) {
            throw new PermissionDeniedException("Account " + caller
                    + " is not authorized to list users of the project id=" + projectId);
        }

        Filter searchFilter = new Filter(ProjectAccountJoinVO.class, "id", false, startIndex, pageSizeVal);
        SearchBuilder<ProjectAccountJoinVO> sb = _projectAccountJoinDao.createSearchBuilder();
        sb.and("accountRole", sb.entity().getAccountRole(), Op.EQ);
        sb.and("projectId", sb.entity().getProjectId(), Op.EQ);

        SearchBuilder<AccountVO> accountSearch;
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
        List<HostResponse> hostResponses = ViewResponseHelper.createHostResponse(cmd.getDetails(), hosts.first()
                .toArray(new HostJoinVO[hosts.first().size()]));
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
        Object resourceState = cmd.getResourceState();
        Object haHosts = cmd.getHaHost();
        Long startIndex = cmd.getStartIndex();
        Long pageSize = cmd.getPageSizeVal();

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
        sb.and("resourceState", sb.entity().getResourceState(), SearchCriteria.Op.EQ);

        String haTag = _haMgr.getHaTag();
        if (haHosts != null && haTag != null && !haTag.isEmpty()) {
            if ((Boolean) haHosts) {
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

        if (resourceState != null) {
            sc.setParameters("resourceState", resourceState);
        }

        if (haHosts != null && haTag != null && !haTag.isEmpty()) {
            sc.setJoinParameters("hostTagSearch", "tag", haTag);
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

        List<VolumeResponse> volumeResponses = ViewResponseHelper.createVolumeResponse(result.first().toArray(
                new VolumeJoinVO[result.first().size()]));
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

        Long zoneId = cmd.getZoneId();
        Long podId = null;
        if (_accountMgr.isAdmin(caller.getType())) {
            podId = cmd.getPodId();
        }

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(
                cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts,
                domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        Filter searchFilter = new Filter(VolumeJoinVO.class, "created", false, cmd.getStartIndex(),
                cmd.getPageSizeVal());

        // hack for now, this should be done better but due to needing a join I
        // opted to
        // do this quickly and worry about making it pretty later
        SearchBuilder<VolumeJoinVO> sb = _volumeJoinDao.createSearchBuilder();
        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct
        // ids to get
        // number of
        // records with
        // pagination
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);

        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("volumeType", sb.entity().getVolumeType(), SearchCriteria.Op.LIKE);
        sb.and("instanceId", sb.entity().getVmId(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        // Only return volumes that are not destroyed
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.NEQ);
        sb.and("systemUse", sb.entity().isSystemUse(), SearchCriteria.Op.NEQ);
        // display UserVM volumes only
        sb.and().op("type", sb.entity().getVmType(), SearchCriteria.Op.NIN);
        sb.or("nulltype", sb.entity().getVmType(), SearchCriteria.Op.NULL);
        sb.cp();


        // now set the SC criteria...
        SearchCriteria<VolumeJoinVO> sc = sb.create();
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);

        if (keyword != null) {
            SearchCriteria<VolumeJoinVO> ssc = _volumeJoinDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("volumeType", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.setParameters("name", name);
        }

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

        // Don't return DomR and ConsoleProxy volumes
        sc.setParameters("type", VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm,
                VirtualMachine.Type.DomainRouter);

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
    public ListResponse<AccountResponse> searchForAccounts(ListAccountsCmd cmd) {
        Pair<List<AccountJoinVO>, Integer> result = searchForAccountsInternal(cmd);
        ListResponse<AccountResponse> response = new ListResponse<AccountResponse>();
        List<AccountResponse> accountResponses = ViewResponseHelper.createAccountResponse(result.first().toArray(
                new AccountJoinVO[result.first().size()]));
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
        Boolean listForDomain = false;

        if (accountId != null) {
            Account account = _accountDao.findById(accountId);
            if (account == null || account.getId() == Account.ACCOUNT_ID_SYSTEM) {
                throw new InvalidParameterValueException("Unable to find account by id " + accountId);
            }

            _accountMgr.checkAccess(caller, null, true, account);
        }

        if (domainId != null) {
            Domain domain = _domainDao.findById(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Domain id=" + domainId + " doesn't exist");
            }

            _accountMgr.checkAccess(caller, domain);

            if (accountName != null) {
                Account account = _accountDao.findActiveAccount(accountName, domainId);
                if (account == null || account.getId() == Account.ACCOUNT_ID_SYSTEM) {
                    throw new InvalidParameterValueException("Unable to find account by name " + accountName
                            + " in domain " + domainId);
                }
                _accountMgr.checkAccess(caller, null, true, account);
            }
        }

        if (accountId == null) {
            if (_accountMgr.isAdmin(caller.getType()) && listAll && domainId == null) {
                listForDomain = true;
                isRecursive = true;
                if (domainId == null) {
                    domainId = caller.getDomainId();
                }
            } else if (_accountMgr.isAdmin(caller.getType()) && domainId != null) {
                listForDomain = true;
            } else {
                accountId = caller.getAccountId();
            }
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

        if (listForDomain && isRecursive) {
            sb.and("path", sb.entity().getDomainPath(), SearchCriteria.Op.LIKE);
        }

        SearchCriteria<AccountJoinVO> sc = sb.create();

        sc.setParameters("idNEQ", Account.ACCOUNT_ID_SYSTEM);

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

        // don't return account of type project to the end user
        sc.setParameters("typeNEQ", 5);

        if (accountId != null) {
            sc.setParameters("id", accountId);
        }

        if (listForDomain) {
            if (isRecursive) {
                Domain domain = _domainDao.findById(domainId);
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
        List<AsyncJobResponse> jobResponses = ViewResponseHelper.createAsyncJobResponse(result.first().toArray(
                new AsyncJobJoinVO[result.first().size()]));
        response.setResponses(jobResponses, result.second());
        return response;
    }

    private Pair<List<AsyncJobJoinVO>, Integer> searchForAsyncJobsInternal(ListAsyncJobsCmd cmd) {

        Account caller = CallContext.current().getCallingAccount();

        List<Long> permittedAccounts = new ArrayList<Long>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(
                cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, null, cmd.getAccountName(), null, permittedAccounts,
                domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(AsyncJobJoinVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<AsyncJobJoinVO> sb = _jobJoinDao.createSearchBuilder();
        sb.and("accountIdIN", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        SearchBuilder<AccountVO> accountSearch = null;
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

        List<StoragePoolResponse> poolResponses = ViewResponseHelper.createStoragePoolResponse(result.first().toArray(
                new StoragePoolJoinVO[result.first().size()]));
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
    public ListResponse<ImageStoreResponse> searchForImageStores(ListImageStoresCmd cmd) {
        Pair<List<ImageStoreJoinVO>, Integer> result = searchForImageStoresInternal(cmd);
        ListResponse<ImageStoreResponse> response = new ListResponse<ImageStoreResponse>();

        List<ImageStoreResponse> poolResponses = ViewResponseHelper.createImageStoreResponse(result.first().toArray(
                new ImageStoreJoinVO[result.first().size()]));
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
    public ListResponse<ImageStoreResponse> searchForCacheStores(ListCacheStoresCmd cmd) {
        Pair<List<ImageStoreJoinVO>, Integer> result = searchForCacheStoresInternal(cmd);
        ListResponse<ImageStoreResponse> response = new ListResponse<ImageStoreResponse>();

        List<ImageStoreResponse> poolResponses = ViewResponseHelper.createImageStoreResponse(result.first().toArray(
                new ImageStoreJoinVO[result.first().size()]));
        response.setResponses(poolResponses, result.second());
        return response;
    }

    private Pair<List<ImageStoreJoinVO>, Integer> searchForCacheStoresInternal(ListCacheStoresCmd cmd) {

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
        List<DiskOfferingResponse> offeringResponses = ViewResponseHelper.createDiskOfferingResponse(result.first()
                .toArray(new DiskOfferingJoinVO[result.first().size()]));
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

        Boolean isAscending = Boolean.parseBoolean(_configDao.getValue("sortkey.algorithm"));
        isAscending = (isAscending == null ? true : isAscending);
        Filter searchFilter = new Filter(DiskOfferingJoinVO.class, "sortKey", isAscending, cmd.getStartIndex(),
                cmd.getPageSizeVal());
        SearchCriteria<DiskOfferingJoinVO> sc = _diskOfferingJoinDao.createSearchCriteria();

        Account account = CallContext.current().getCallingAccount();
        Object name = cmd.getDiskOfferingName();
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();
        Long domainId = cmd.getDomainId();
        // Keeping this logic consistent with domain specific zones
        // if a domainId is provided, we just return the disk offering
        // associated with this domain
        if (domainId != null) {
            if (account.getType() == Account.ACCOUNT_TYPE_ADMIN || isPermissible(account.getDomainId(), domainId)) {
                // check if the user's domain == do's domain || user's domain is
                // a child of so's domain for non-root users
                sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
                return _diskOfferingJoinDao.searchAndCount(sc, searchFilter);
            } else {
                throw new PermissionDeniedException("The account:" + account.getAccountName()
                        + " does not fall in the same domain hierarchy as the disk offering");
            }
        }

        List<Long> domainIds = null;
        // For non-root users, only return all offerings for the user's domain,
        // and everything above till root
        if ((account.getType() == Account.ACCOUNT_TYPE_NORMAL || account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN)
                || account.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
            // find all domain Id up to root domain for this account
            domainIds = new ArrayList<Long>();
            DomainVO domainRecord = _domainDao.findById(account.getDomainId());
            if (domainRecord == null) {
                s_logger.error("Could not find the domainId for account:" + account.getAccountName());
                throw new CloudAuthenticationException("Could not find the domainId for account:"
                        + account.getAccountName());
            }
            domainIds.add(domainRecord.getId());
            while (domainRecord.getParent() != null) {
                domainRecord = _domainDao.findById(domainRecord.getParent());
                domainIds.add(domainRecord.getId());
            }

            SearchCriteria<DiskOfferingJoinVO> spc = _diskOfferingJoinDao.createSearchCriteria();

            spc.addOr("domainId", SearchCriteria.Op.IN, domainIds.toArray());
            spc.addOr("domainId", SearchCriteria.Op.NULL); // include public
            // offering as where
            sc.addAnd("domainId", SearchCriteria.Op.SC, spc);
            sc.addAnd("systemUse", SearchCriteria.Op.EQ, false); // non-root
            // users should
            // not see
            // system
            // offering at
            // all

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

        return _diskOfferingJoinDao.searchAndCount(sc, searchFilter);
    }

    @Override
    public ListResponse<ServiceOfferingResponse> searchForServiceOfferings(ListServiceOfferingsCmd cmd) {
        Pair<List<ServiceOfferingJoinVO>, Integer> result = searchForServiceOfferingsInternal(cmd);
        ListResponse<ServiceOfferingResponse> response = new ListResponse<ServiceOfferingResponse>();
        List<ServiceOfferingResponse> offeringResponses = ViewResponseHelper.createServiceOfferingResponse(result
                .first().toArray(new ServiceOfferingJoinVO[result.first().size()]));
        response.setResponses(offeringResponses, result.second());
        return response;
    }

    private Pair<List<ServiceOfferingJoinVO>, Integer> searchForServiceOfferingsInternal(ListServiceOfferingsCmd cmd) {
        // Note
        // The list method for offerings is being modified in accordance with
        // discussion with Will/Kevin
        // For now, we will be listing the following based on the usertype
        // 1. For root, we will list all offerings
        // 2. For domainAdmin and regular users, we will list everything in
        // their domains+parent domains ... all the way
        // till
        // root
        Boolean isAscending = Boolean.parseBoolean(_configDao.getValue("sortkey.algorithm"));
        isAscending = (isAscending == null ? true : isAscending);
        Filter searchFilter = new Filter(ServiceOfferingJoinVO.class, "sortKey", isAscending, cmd.getStartIndex(),
                cmd.getPageSizeVal());
        SearchCriteria<ServiceOfferingJoinVO> sc = _srvOfferingJoinDao.createSearchCriteria();

        Account caller = CallContext.current().getCallingAccount();
        Object name = cmd.getServiceOfferingName();
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();
        Long vmId = cmd.getVirtualMachineId();
        Long domainId = cmd.getDomainId();
        Boolean isSystem = cmd.getIsSystem();
        String vmTypeStr = cmd.getSystemVmType();

        if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN && isSystem) {
            throw new InvalidParameterValueException("Only ROOT admins can access system's offering");
        }

        // Keeping this logic consistent with domain specific zones
        // if a domainId is provided, we just return the so associated with this
        // domain
        if (domainId != null && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
            // check if the user's domain == so's domain || user's domain is a
            // child of so's domain
            if (!isPermissible(caller.getDomainId(), domainId)) {
                throw new PermissionDeniedException("The account:" + caller.getAccountName()
                        + " does not fall in the same domain hierarchy as the service offering");
            }
        }

        // boolean includePublicOfferings = false;
        if ((caller.getType() == Account.ACCOUNT_TYPE_NORMAL || caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN)
                || caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
            // For non-root users.
            if (isSystem) {
                throw new InvalidParameterValueException("Only root admins can access system's offering");
            }
            // find all domain Id up to root domain for this account
            List<Long> domainIds = new ArrayList<Long>();
            DomainVO domainRecord = _domainDao.findById(caller.getDomainId());
            if (domainRecord == null) {
                s_logger.error("Could not find the domainId for account:" + caller.getAccountName());
                throw new CloudAuthenticationException("Could not find the domainId for account:"
                        + caller.getAccountName());
            }
            domainIds.add(domainRecord.getId());
            while (domainRecord.getParent() != null) {
                domainRecord = _domainDao.findById(domainRecord.getParent());
                domainIds.add(domainRecord.getId());
            }
            SearchCriteria<ServiceOfferingJoinVO> spc = _srvOfferingJoinDao.createSearchCriteria();

            spc.addOr("domainId", SearchCriteria.Op.IN, domainIds.toArray());
            spc.addOr("domainId", SearchCriteria.Op.NULL); // include public
            // offering as where
            sc.addAnd("domainId", SearchCriteria.Op.SC, spc);

        } else {
            // for root users
            if (caller.getDomainId() != 1 && isSystem) { // NON ROOT admin
                throw new InvalidParameterValueException("Non ROOT admins cannot access system's offering");
            }
            if (domainId != null) {
                sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
            }
        }

        if (keyword != null) {
            SearchCriteria<ServiceOfferingJoinVO> ssc = _srvOfferingJoinDao.createSearchCriteria();
            ssc.addOr("displayText", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        } else if (vmId != null) {
            UserVmVO vmInstance = _userVmDao.findById(vmId);
            if ((vmInstance == null) || (vmInstance.getRemoved() != null)) {
                InvalidParameterValueException ex = new InvalidParameterValueException(
                        "unable to find a virtual machine with specified id");
                ex.addProxyObject(vmId.toString(), "vmId");
                throw ex;
            }

            _accountMgr.checkAccess(caller, null, true, vmInstance);

            ServiceOfferingVO offering = _srvOfferingDao.findByIdIncludingRemoved(vmInstance.getServiceOfferingId());
            sc.addAnd("id", SearchCriteria.Op.NEQ, offering.getId());

            // Only return offerings with the same Guest IP type and storage
            // pool preference
            // sc.addAnd("guestIpType", SearchCriteria.Op.EQ,
            // offering.getGuestIpType());
            sc.addAnd("useLocalStorage", SearchCriteria.Op.EQ, offering.getUseLocalStorage());
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
            sc.addAnd("vm_type", SearchCriteria.Op.EQ, vmTypeStr);
        }

        return _srvOfferingJoinDao.searchAndCount(sc, searchFilter);

    }

    @Override
    public ListResponse<ZoneResponse> listDataCenters(ListZonesByCmd cmd) {
        Pair<List<DataCenterJoinVO>, Integer> result = listDataCentersInternal(cmd);
        ListResponse<ZoneResponse> response = new ListResponse<ZoneResponse>();
        List<ZoneResponse> dcResponses = ViewResponseHelper.createDataCenterResponse(cmd.getShowCapacities(), result
                .first().toArray(new DataCenterJoinVO[result.first().size()]));
        response.setResponses(dcResponses, result.second());
        return response;
    }

    private Pair<List<DataCenterJoinVO>, Integer> listDataCentersInternal(ListZonesByCmd cmd) {
        Account account = CallContext.current().getCallingAccount();
        Long domainId = cmd.getDomainId();
        Long id = cmd.getId();
        String keyword = cmd.getKeyword();
        String name = cmd.getName();
        String networkType = cmd.getNetworkType();

        Filter searchFilter = new Filter(DataCenterJoinVO.class, null, false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchCriteria<DataCenterJoinVO> sc = _dcJoinDao.createSearchCriteria();

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
             * dedicated resources of other account if (domainId != null) { //
             * for domainId != null // right now, we made the decision to only
             * list zones associated // with this domain, private zone
             * sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId); } else
             */
            if (account.getType() == Account.ACCOUNT_TYPE_NORMAL) {
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
                    throw new CloudAuthenticationException("Could not find the domainId for account:"
                            + account.getAccountName());
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
                sc.addAnd("domain", SearchCriteria.Op.SC, sdc);

                // remove disabled zones
                sc.addAnd("allocationState", SearchCriteria.Op.NEQ, Grouping.AllocationState.Disabled);

                // remove Dedicated zones not dedicated to this domainId or
                // subdomainId
                List<Long> dedicatedZoneIds = removeDedicatedZoneNotSuitabe(domainIds);
                if (!dedicatedZoneIds.isEmpty()) {
                    sdc.addAnd("id", SearchCriteria.Op.NIN,
                            dedicatedZoneIds.toArray(new Object[dedicatedZoneIds.size()]));
                }

            } else if (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN
                    || account.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
                // it was decided to return all zones for the domain admin, and
                // everything above till root, as well as zones till the domain
                // leaf
                List<Long> domainIds = new ArrayList<Long>();
                DomainVO domainRecord = _domainDao.findById(account.getDomainId());
                if (domainRecord == null) {
                    s_logger.error("Could not find the domainId for account:" + account.getAccountName());
                    throw new CloudAuthenticationException("Could not find the domainId for account:"
                            + account.getAccountName());
                }
                domainIds.add(domainRecord.getId());
                // find all domain Ids till leaf
                List<DomainVO> allChildDomains = _domainDao.findAllChildren(domainRecord.getPath(),
                        domainRecord.getId());
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
                sc.addAnd("domain", SearchCriteria.Op.SC, sdc);

                // remove disabled zones
                sc.addAnd("allocationState", SearchCriteria.Op.NEQ, Grouping.AllocationState.Disabled);

                // remove Dedicated zones not dedicated to this domainId or
                // subdomainId
                List<Long> dedicatedZoneIds = removeDedicatedZoneNotSuitabe(domainIds);
                if (!dedicatedZoneIds.isEmpty()) {
                    sdc.addAnd("id", SearchCriteria.Op.NIN,
                            dedicatedZoneIds.toArray(new Object[dedicatedZoneIds.size()]));
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
                        sc.addAnd("idIn", SearchCriteria.Op.IN, dcIds.toArray());
                    }

                }
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

        List<TemplateResponse> templateResponses = ViewResponseHelper.createTemplateResponse(result.first().toArray(
                new TemplateJoinVO[result.first().size()]));
        response.setResponses(templateResponses, result.second());
        return response;
    }

    private Pair<List<TemplateJoinVO>, Integer> searchForTemplatesInternal(ListTemplatesCmd cmd) {
        TemplateFilter templateFilter = TemplateFilter.valueOf(cmd.getTemplateFilter());
        Long id = cmd.getId();
        Map<String, String> tags = cmd.getTags();
        Account caller = CallContext.current().getCallingAccount();

        boolean listAll = false;
        if (templateFilter != null && templateFilter == TemplateFilter.all) {
            if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL) {
                throw new InvalidParameterValueException("Filter " + TemplateFilter.all
                        + " can be specified by admin only");
            }
            listAll = true;
        }

        List<Long> permittedAccountIds = new ArrayList<Long>();
        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(
                cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccountIds,
                domainIdRecursiveListProject, listAll, false);
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        List<Account> permittedAccounts = new ArrayList<Account>();
        for (Long accountId : permittedAccountIds) {
            permittedAccounts.add(_accountMgr.getAccount(accountId));
        }

        boolean showDomr = ((templateFilter != TemplateFilter.selfexecutable) && (templateFilter != TemplateFilter.featured));
        HypervisorType hypervisorType = HypervisorType.getType(cmd.getHypervisor());

        return searchForTemplatesInternal(id, cmd.getTemplateName(), cmd.getKeyword(), templateFilter, false, null,
                cmd.getPageSizeVal(), cmd.getStartIndex(), cmd.getZoneId(), hypervisorType, showDomr,
                cmd.listInReadyState(), permittedAccounts, caller, listProjectResourcesCriteria, tags);
    }

    private Pair<List<TemplateJoinVO>, Integer> searchForTemplatesInternal(Long templateId, String name,
            String keyword, TemplateFilter templateFilter, boolean isIso, Boolean bootable, Long pageSize,
            Long startIndex, Long zoneId, HypervisorType hyperType, boolean showDomr, boolean onlyReady,
            List<Account> permittedAccounts, Account caller, ListProjectResourcesCriteria listProjectResourcesCriteria,
            Map<String, String> tags) {
        VMTemplateVO template = null;

        Boolean isAscending = Boolean.parseBoolean(_configDao.getValue("sortkey.algorithm"));
        isAscending = (isAscending == null ? true : isAscending);
        Filter searchFilter = new Filter(TemplateJoinVO.class, "sortKey", isAscending, startIndex, pageSize);

        SearchBuilder<TemplateJoinVO> sb = _templateJoinDao.createSearchBuilder();
        sb.select(null, Func.DISTINCT, sb.entity().getTempZonePair()); // select distinct (templateId, zoneId) pair
        SearchCriteria<TemplateJoinVO> sc = sb.create();

        // verify templateId parameter and specially handle it
        if (templateId != null) {
            template = _templateDao.findById(templateId);
            if (template == null) {
                throw new InvalidParameterValueException("Please specify a valid template ID.");
            }// If ISO requested then it should be ISO.
            if (isIso && template.getFormat() != ImageFormat.ISO) {
                s_logger.error("Template Id " + templateId + " is not an ISO");
                InvalidParameterValueException ex = new InvalidParameterValueException(
                        "Specified Template Id is not an ISO");
                ex.addProxyObject(template.getUuid(), "templateId");
                throw ex;
            }// If ISO not requested then it shouldn't be an ISO.
            if (!isIso && template.getFormat() == ImageFormat.ISO) {
                s_logger.error("Incorrect format of the template id " + templateId);
                InvalidParameterValueException ex = new InvalidParameterValueException("Incorrect format "
                        + template.getFormat() + " of the specified template id");
                ex.addProxyObject(template.getUuid(), "templateId");
                throw ex;
            }

            // if template is not public, perform permission check here
            if (!template.isPublicTemplate() && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                Account owner = _accountMgr.getAccount(template.getAccountId());
                _accountMgr.checkAccess(caller, null, true, owner);
            }

            // if templateId is specified, then we will just use the id to
            // search and ignore other query parameters
            sc.addAnd("id", SearchCriteria.Op.EQ, templateId);
        } else {

            DomainVO domain = null;
            if (!permittedAccounts.isEmpty()) {
                domain = _domainDao.findById(permittedAccounts.get(0).getDomainId());
            } else {
                domain = _domainDao.findById(DomainVO.ROOT_DOMAIN);
            }

            List<HypervisorType> hypers = null;
            if (!isIso) {
                hypers = _resourceMgr.listAvailHypervisorInZone(null, null);
            }

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
                    DomainVO accountDomain = _domainDao.findById(account.getDomainId());

                    // get all parent domain ID's all the way till root domain
                    DomainVO domainTreeNode = accountDomain;
                    relatedDomainIds.add(domainTreeNode.getId());
                    while (domainTreeNode.getParent() != null) {
                        domainTreeNode = _domainDao.findById(domainTreeNode.getParent());
                        relatedDomainIds.add(domainTreeNode.getId());
                    }

                    // get all child domain ID's
                    if (_accountMgr.isAdmin(account.getType())) {
                        List<DomainVO> allChildDomains = _domainDao.findAllChildren(accountDomain.getPath(),
                                accountDomain.getId());
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
                SearchCriteria<TemplateJoinVO> scc = _templateJoinDao.createSearchCriteria();
                scc.addOr("accountId", SearchCriteria.Op.IN, permittedAccountIds.toArray());
                scc.addOr("sharedAccountId", SearchCriteria.Op.IN, permittedAccountIds.toArray());
                sc.addAnd("accountId", SearchCriteria.Op.SC, scc);
            } else if (templateFilter == TemplateFilter.executable) {
                SearchCriteria<TemplateJoinVO> scc = _templateJoinDao.createSearchCriteria();
                scc.addOr("publicTemplate", SearchCriteria.Op.EQ, true);
                if (!permittedAccounts.isEmpty()) {
                    scc.addOr("accountId", SearchCriteria.Op.IN, permittedAccountIds.toArray());
                }
                sc.addAnd("publicTemplate", SearchCriteria.Op.SC, scc);
            }

            // add tags criteria
            if (tags != null && !tags.isEmpty()) {
                SearchCriteria<TemplateJoinVO> scc = _templateJoinDao.createSearchCriteria();
                int count = 0;
                for (String key : tags.keySet()) {
                    SearchCriteria<TemplateJoinVO> scTag = _templateJoinDao.createSearchCriteria();
                    scTag.addAnd("tagKey", SearchCriteria.Op.EQ, key);
                    scTag.addAnd("tagValue", SearchCriteria.Op.EQ, tags.get(key));
                    if (isIso) {
                        scTag.addAnd("tagResourceType", SearchCriteria.Op.EQ, TaggedResourceType.ISO);
                    } else {
                        scTag.addAnd("tagResourceType", SearchCriteria.Op.EQ, TaggedResourceType.Template);
                    }
                    scc.addOr("tagKey", SearchCriteria.Op.SC, scTag);
                    count++;
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

            if (zoneId != null) {
                SearchCriteria<TemplateJoinVO> zoneSc = _templateJoinDao.createSearchCriteria();
                zoneSc.addOr("dataCenterId", SearchCriteria.Op.EQ, zoneId);
                zoneSc.addOr("dataStoreScope", SearchCriteria.Op.EQ, ScopeType.REGION);
                // handle the case where xs-tools.iso and vmware-tools.iso do not have data_center information in template_view
                SearchCriteria<TemplateJoinVO> isoPerhostSc = _templateJoinDao.createSearchCriteria();
                isoPerhostSc.addAnd("format", SearchCriteria.Op.EQ, ImageFormat.ISO);
                isoPerhostSc.addAnd("templateType", SearchCriteria.Op.EQ, TemplateType.PERHOST);
                zoneSc.addOr("templateType", SearchCriteria.Op.SC, isoPerhostSc);
                sc.addAnd("dataCenterId", SearchCriteria.Op.SC, zoneSc);
            }

            if (!showDomr) {
                // excluding system template
                sc.addAnd("templateType", SearchCriteria.Op.NEQ, Storage.TemplateType.SYSTEM);
            }
        }

        // don't return removed template, this should not be needed since we
        // changed annotation for removed field in TemplateJoinVO.
        // sc.addAnd("removed", SearchCriteria.Op.NULL);

        // search unique templates and find details by Ids
        Pair<List<TemplateJoinVO>, Integer> uniqueTmplPair = _templateJoinDao.searchAndCount(sc, searchFilter);
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
        List<TemplateJoinVO> vrs = _templateJoinDao.searchByTemplateZonePair(tzIds);
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

        List<TemplateResponse> templateResponses = ViewResponseHelper.createIsoResponse(result.first().toArray(
                new TemplateJoinVO[result.first().size()]));
        response.setResponses(templateResponses, result.second());
        return response;
    }

    private Pair<List<TemplateJoinVO>, Integer> searchForIsosInternal(ListIsosCmd cmd) {
        TemplateFilter isoFilter = TemplateFilter.valueOf(cmd.getIsoFilter());
        Long id = cmd.getId();
        Map<String, String> tags = cmd.getTags();
        Account caller = CallContext.current().getCallingAccount();

        boolean listAll = false;
        if (isoFilter != null && isoFilter == TemplateFilter.all) {
            if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL) {
                throw new InvalidParameterValueException("Filter " + TemplateFilter.all
                        + " can be specified by admin only");
            }
            listAll = true;
        }

        List<Long> permittedAccountIds = new ArrayList<Long>();
        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(
                cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccountIds,
                domainIdRecursiveListProject, listAll, false);
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        List<Account> permittedAccounts = new ArrayList<Account>();
        for (Long accountId : permittedAccountIds) {
            permittedAccounts.add(_accountMgr.getAccount(accountId));
        }

        HypervisorType hypervisorType = HypervisorType.getType(cmd.getHypervisor());

        return searchForTemplatesInternal(cmd.getId(), cmd.getIsoName(), cmd.getKeyword(), isoFilter, true,
                cmd.isBootable(), cmd.getPageSizeVal(), cmd.getStartIndex(), cmd.getZoneId(), hypervisorType, true,
                cmd.listInReadyState(), permittedAccounts, caller, listProjectResourcesCriteria, tags);
    }

    @Override
    public ListResponse<AffinityGroupResponse> listAffinityGroups(Long affinityGroupId, String affinityGroupName,
            String affinityGroupType, Long vmId, String accountName, Long domainId, boolean isRecursive,
            boolean listAll, Long startIndex, Long pageSize) {
        Pair<List<AffinityGroupJoinVO>, Integer> result = listAffinityGroupsInternal(affinityGroupId,
                affinityGroupName, affinityGroupType, vmId, accountName, domainId, isRecursive, listAll, startIndex,
                pageSize);
        ListResponse<AffinityGroupResponse> response = new ListResponse<AffinityGroupResponse>();
        List<AffinityGroupResponse> agResponses = ViewResponseHelper.createAffinityGroupResponses(result.first());
        response.setResponses(agResponses, result.second());
        return response;
    }

    public Pair<List<AffinityGroupJoinVO>, Integer> listAffinityGroupsInternal(Long affinityGroupId,
            String affinityGroupName, String affinityGroupType, Long vmId, String accountName, Long domainId,
            boolean isRecursive, boolean listAll, Long startIndex, Long pageSize) {

        Account caller = CallContext.current().getCallingAccount();

        Long accountId = caller.getAccountId();

        if (vmId != null) {
            UserVmVO userVM = _userVmDao.findById(vmId);
            if (userVM == null) {
                throw new InvalidParameterValueException("Unable to list affinity groups for virtual machine instance "
                        + vmId + "; instance not found.");
            }
            _accountMgr.checkAccess(caller, null, true, userVM);
            return listAffinityGroupsByVM(vmId.longValue(), startIndex, pageSize);
        }

        List<Long> permittedAccounts = new ArrayList<Long>();
        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(
                domainId, isRecursive, null);
        _accountMgr.buildACLSearchParameters(caller, affinityGroupId, accountName, null, permittedAccounts,
                domainIdRecursiveListProject, listAll, true);
        domainId = domainIdRecursiveListProject.first();
        isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(AffinityGroupJoinVO.class, "id", true, startIndex, pageSize);
        SearchBuilder<AffinityGroupJoinVO> groupSearch = _affinityGroupJoinDao.createSearchBuilder();
        _accountMgr.buildACLViewSearchBuilder(groupSearch, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);

        groupSearch.select(null, Func.DISTINCT, groupSearch.entity().getId()); // select
        // distinct

        SearchCriteria<AffinityGroupJoinVO> sc = groupSearch.create();
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);

        if (affinityGroupId != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, affinityGroupId);
        }

        if (affinityGroupName != null) {
            sc.addAnd("name", SearchCriteria.Op.EQ, affinityGroupName);
        }

        if (affinityGroupType != null) {
            sc.addAnd("type", SearchCriteria.Op.EQ, affinityGroupType);
        }

        Pair<List<AffinityGroupJoinVO>, Integer> uniqueGroupsPair = _affinityGroupJoinDao.searchAndCount(sc,
                searchFilter);
        // search group details by ids
        Integer count = uniqueGroupsPair.second();
        if (count.intValue() == 0) {
            // empty result
            return uniqueGroupsPair;
        }
        List<AffinityGroupJoinVO> uniqueGroups = uniqueGroupsPair.first();
        Long[] vrIds = new Long[uniqueGroups.size()];
        int i = 0;
        for (AffinityGroupJoinVO v : uniqueGroups) {
            vrIds[i++] = v.getId();
        }
        List<AffinityGroupJoinVO> vrs = _affinityGroupJoinDao.searchByIds(vrIds);
        return new Pair<List<AffinityGroupJoinVO>, Integer>(vrs, count);

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

    @Override
    public List<ResourceDetailResponse> listResource(ListResourceDetailsCmd cmd) {

        String key = cmd.getKey();
        ResourceTag.TaggedResourceType resourceType = cmd.getResourceType();
        String resourceId = cmd.getResourceId();
        Long id = _taggedResourceMgr.getResourceId(resourceId, resourceType);

        if (resourceType == ResourceTag.TaggedResourceType.Volume) {

            List<VolumeDetailVO> volumeDetailList;
            if (key == null) {
                volumeDetailList = _volumeDetailDao.findDetails(id);
            } else {
                VolumeDetailVO volumeDetail = _volumeDetailDao.findDetail(id, key);
                volumeDetailList = new LinkedList<VolumeDetailVO>();
                volumeDetailList.add(volumeDetail);
            }

            List<ResourceDetailResponse> volumeDetailResponseList = new ArrayList<ResourceDetailResponse>();
            for (VolumeDetailVO volumeDetail : volumeDetailList) {
                ResourceDetailResponse volumeDetailResponse = new ResourceDetailResponse();
                volumeDetailResponse.setResourceId(id.toString());
                volumeDetailResponse.setName(volumeDetail.getName());
                volumeDetailResponse.setValue(volumeDetail.getValue());
                volumeDetailResponse.setResourceType(ResourceTag.TaggedResourceType.Volume.toString());
                volumeDetailResponse.setObjectName("volumedetail");
                volumeDetailResponseList.add(volumeDetailResponse);
            }

            return volumeDetailResponseList;

        } else {

            List<NicDetailVO> nicDetailList;
            if (key == null) {
                nicDetailList = _nicDetailDao.findDetails(id);
            } else {
                NicDetailVO nicDetail = _nicDetailDao.findDetail(id, key);
                nicDetailList = new LinkedList<NicDetailVO>();
                nicDetailList.add(nicDetail);
            }

            List<ResourceDetailResponse> nicDetailResponseList = new ArrayList<ResourceDetailResponse>();
            for (NicDetailVO nicDetail : nicDetailList) {
                ResourceDetailResponse nicDetailResponse = new ResourceDetailResponse();
                // String uuid = ApiDBUtils.findN
                nicDetailResponse.setName(nicDetail.getName());
                nicDetailResponse.setValue(nicDetail.getValue());
                nicDetailResponse.setResourceType(ResourceTag.TaggedResourceType.Nic.toString());
                nicDetailResponse.setObjectName("nicdetail");
                nicDetailResponseList.add(nicDetailResponse);
            }

            return nicDetailResponseList;

        }

    }

}
