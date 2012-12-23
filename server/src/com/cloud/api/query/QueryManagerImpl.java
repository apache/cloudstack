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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ApiConstants.HostDetails;
import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.command.admin.host.ListHostsCmd;
import org.apache.cloudstack.api.command.admin.router.ListRoutersCmd;
import org.apache.cloudstack.api.command.admin.user.ListUsersCmd;
import org.apache.cloudstack.api.command.user.account.ListProjectAccountsCmd;
import org.apache.cloudstack.api.command.user.event.ListEventsCmd;
import org.apache.cloudstack.api.command.user.project.ListProjectInvitationsCmd;
import org.apache.cloudstack.api.command.user.project.ListProjectsCmd;
import org.apache.cloudstack.api.command.user.securitygroup.ListSecurityGroupsCmd;
import org.apache.cloudstack.api.command.user.tag.ListTagsCmd;
import org.apache.cloudstack.api.command.user.vm.ListVMsCmd;
import org.apache.cloudstack.api.command.user.vmgroup.ListVMGroupsCmd;
import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.api.response.EventResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.InstanceGroupResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ProjectAccountResponse;
import org.apache.cloudstack.api.response.ProjectInvitationResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ResourceTagResponse;
import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.query.QueryService;
import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.query.dao.DomainRouterJoinDao;
import com.cloud.api.query.dao.HostJoinDao;
import com.cloud.api.query.dao.InstanceGroupJoinDao;
import com.cloud.api.query.dao.ProjectAccountJoinDao;
import com.cloud.api.query.dao.ProjectInvitationJoinDao;
import com.cloud.api.query.dao.ProjectJoinDao;
import com.cloud.api.query.dao.ResourceTagJoinDao;
import com.cloud.api.query.dao.SecurityGroupJoinDao;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.vo.DomainRouterJoinVO;
import com.cloud.api.query.vo.EventJoinVO;
import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.api.query.vo.InstanceGroupJoinVO;
import com.cloud.api.query.vo.ProjectAccountJoinVO;
import com.cloud.api.query.vo.ProjectInvitationJoinVO;
import com.cloud.api.query.vo.ProjectJoinVO;
import com.cloud.api.query.vo.ResourceTagJoinVO;
import com.cloud.api.query.vo.SecurityGroupJoinVO;
import com.cloud.api.query.vo.UserAccountJoinVO;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.async.AsyncJob;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.dao.EventJoinDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.Host;
import com.cloud.host.HostTagVO;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.security.SecurityGroupVMMapVO;
import com.cloud.network.security.dao.SecurityGroupVMMapDao;
import com.cloud.projects.ProjectInvitation;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectManager;
import com.cloud.projects.ProjectService;
import com.cloud.projects.dao.ProjectAccountDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.server.Criteria;
import com.cloud.storage.Snapshot;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountManagerImpl;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.DomainManager;
import com.cloud.user.UserContext;
import com.cloud.user.dao.UserAccountJoinDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;

/**
 * @author minc
 *
 */
@Local(value = {QueryService.class })
public class QueryManagerImpl implements QueryService, Manager {

    public static final Logger s_logger = Logger.getLogger(QueryManagerImpl.class);

    private String _name;

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
    private HighAvailabilityManager _haMgr;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
       // _responseGenerator = new ViewResponseHelper();
        return false;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    /* (non-Javadoc)
     * @see com.cloud.api.query.QueryService#searchForUsers(org.apache.cloudstack.api.command.admin.user.ListUsersCmd)
     */
    @Override
    public ListResponse<UserResponse> searchForUsers(ListUsersCmd cmd) throws PermissionDeniedException {
        Pair<List<UserAccountJoinVO>, Integer> result = searchForUsersInternal(cmd);
        ListResponse<UserResponse> response = new ListResponse<UserResponse>();
        List<UserResponse> userResponses = ViewResponseHelper.createUserResponse(result.first().toArray(new UserAccountJoinVO[result.first().size()]));
        response.setResponses(userResponses, result.second());
        return response;
    }

    private Pair<List<UserAccountJoinVO>, Integer> searchForUsersInternal(ListUsersCmd cmd) throws PermissionDeniedException {
        Account caller = UserContext.current().getCaller();

        //TODO: Integrate with ACL checkAccess refactoring
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
            // this condition is used to exclude system user from the search results
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
        List<EventResponse> eventResponses = ViewResponseHelper.createEventResponse(result.first().toArray(new EventJoinVO[result.first().size()]));
        response.setResponses(eventResponses, result.second());
        return response;
    }

    private Pair<List<EventJoinVO>, Integer> searchForEventsInternal(ListEventsCmd cmd) {
        Account caller = UserContext.current().getCaller();
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
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject,
                cmd.listAll(), false);
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
        sb.and("startId", sb.entity().getStartId(), SearchCriteria.Op.EQ);
        sb.and("createDate", sb.entity().getCreateDate(), SearchCriteria.Op.BETWEEN);

        SearchCriteria<EventJoinVO> sc = sb.create();
        // building ACL condition
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

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

        Pair<List<EventJoinVO>, Integer> eventPair = null;
        // event_view will not have duplicate rows for each event, so searchAndCount should be good enough.
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
        Account caller = UserContext.current().getCaller();
        List<Long> permittedAccounts = new ArrayList<Long>();
        String key = cmd.getKey();
        String value = cmd.getValue();
        String resourceId = cmd.getResourceId();
        String resourceType = cmd.getResourceType();
        String customerName = cmd.getCustomer();
        boolean listAll = cmd.listAll();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject =
                new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);

        _accountMgr.buildACLSearchParameters(caller, null, cmd.getAccountName(),
                cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, listAll, false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        Filter searchFilter = new Filter(ResourceTagJoinVO.class, "resourceType", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        SearchBuilder<ResourceTagJoinVO> sb = _resourceTagJoinDao.createSearchBuilder();
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

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
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

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
        List<InstanceGroupResponse> grpResponses = ViewResponseHelper.createInstanceGroupResponse(groups.first().toArray(new InstanceGroupJoinVO[groups.first().size()]));
        response.setResponses(grpResponses, groups.second());
        return response;
    }

    private Pair<List<InstanceGroupJoinVO>, Integer> searchForVmGroupsInternal(ListVMGroupsCmd cmd) {
        Long id = cmd.getId();
        String name = cmd.getGroupName();
        String keyword = cmd.getKeyword();

        Account caller = UserContext.current().getCaller();
        List<Long> permittedAccounts = new ArrayList<Long>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(
                cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject,
                cmd.listAll(), false);
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
        List<UserVmResponse> vmResponses = ViewResponseHelper.createUserVmResponse("virtualmachine", cmd.getDetails(), result.first().toArray(new UserVmJoinVO[result.first().size()]));
        response.setResponses(vmResponses, result.second());
        return response;
    }

    private Pair<List<UserVmJoinVO>, Integer> searchForUserVMsInternal(ListVMsCmd cmd) {
        Account caller = UserContext.current().getCaller();
        List<Long> permittedAccounts = new ArrayList<Long>();
        String hypervisor = cmd.getHypervisor();
        boolean listAll = cmd.listAll();
        Long id = cmd.getId();
        Map<String, String> tags = cmd.getTags();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, listAll, false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        // removing order by, orderBy shouold be specified in ListVMsCmd parameters
        //Criteria c = new Criteria("id", Boolean.TRUE, cmd.getStartIndex(), cmd.getPageSizeVal());
        Criteria c = new Criteria(null, Boolean.FALSE, cmd.getStartIndex(), cmd.getPageSizeVal());
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

        return searchForUserVMsByCriteria(c, caller, domainId, isRecursive,
                permittedAccounts, listAll, listProjectResourcesCriteria, tags);
    }


    private Pair<List<UserVmJoinVO>, Integer> searchForUserVMsByCriteria(Criteria c, Account caller, Long domainId, boolean isRecursive,
            List<Long> permittedAccounts, boolean listAll, ListProjectResourcesCriteria listProjectResourcesCriteria, Map<String, String> tags) {
        Filter searchFilter = new Filter(UserVmJoinVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());

        //first search distinct vm id by using query criteria and pagination
        SearchBuilder<UserVmJoinVO> sb = _userVmJoinDao.createSearchBuilder();
        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct ids
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        Object id = c.getCriteria(Criteria.ID);
        Object name = c.getCriteria(Criteria.NAME);
        Object state = c.getCriteria(Criteria.STATE);
        Object notState = c.getCriteria(Criteria.NOTSTATE);
        Object zone = c.getCriteria(Criteria.DATACENTERID);
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

        if (tags != null && !tags.isEmpty()) {
            for (int count=0; count < tags.size(); count++) {
                sb.or().op("key" + String.valueOf(count), sb.entity().getTagKey(), SearchCriteria.Op.EQ);
                sb.and("value" + String.valueOf(count), sb.entity().getTagValue(), SearchCriteria.Op.EQ);
                sb.cp();
            }
        }

        if (networkId != null) {
            sb.and("networkId", sb.entity().getNetworkId(), SearchCriteria.Op.EQ);
        }

        if(vpcId != null && networkId == null){
            sb.and("vpcId", sb.entity().getVpcId(), SearchCriteria.Op.EQ);
        }

        if (storageId != null) {
            sb.and("poolId", sb.entity().getPoolId(), SearchCriteria.Op.EQ);
        }

        // populate the search criteria with the values passed in
        SearchCriteria<UserVmJoinVO> sc = sb.create();

        // building ACL condition
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (tags != null && !tags.isEmpty()) {
            int count = 0;
             for (String key : tags.keySet()) {
                sc.setParameters("key" + String.valueOf(count), key);
                sc.setParameters("value" + String.valueOf(count), tags.get(key));
                count++;
            }
        }

        if (groupId != null && (Long)groupId != -1) {
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

        if(vpcId != null && networkId == null){
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

        if (zone != null) {
            sc.setParameters("dataCenterId", zone);

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

        // search vm details by ids
        Pair<List<UserVmJoinVO>, Integer> uniqueVmPair =  _userVmJoinDao.searchAndCount(sc, searchFilter);
        Integer count = uniqueVmPair.second();
        if ( count.intValue() == 0 ){
            // handle empty result cases
            return uniqueVmPair;
        }
        List<UserVmJoinVO> uniqueVms = uniqueVmPair.first();
        Long[] vmIds = new Long[uniqueVms.size()];
        int i = 0;
        for (UserVmJoinVO v : uniqueVms ){
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
        Account caller = UserContext.current().getCaller();
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
        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct ids
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);

        if (tags != null && !tags.isEmpty()) {
            for (int count=0; count < tags.size(); count++) {
                sb.or().op("key" + String.valueOf(count), sb.entity().getTagKey(), SearchCriteria.Op.EQ);
                sb.and("value" + String.valueOf(count), sb.entity().getTagValue(), SearchCriteria.Op.EQ);
                sb.cp();
            }
        }

        SearchCriteria<SecurityGroupJoinVO> sc = sb.create();
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (tags != null && !tags.isEmpty()) {
            int count = 0;
            for (String key : tags.keySet()) {
               sc.setParameters("key" + String.valueOf(count), key);
               sc.setParameters("value" + String.valueOf(count), tags.get(key));
               count++;
           }
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
        Pair<List<SecurityGroupJoinVO>, Integer> uniqueSgPair =  _securityGroupJoinDao.searchAndCount(sc, searchFilter);
        Integer count = uniqueSgPair.second();
        if ( count.intValue() == 0 ){
            // handle empty result cases
            return uniqueSgPair;
        }

        List<SecurityGroupJoinVO> uniqueSgs = uniqueSgPair.first();
        Long[] sgIds = new Long[uniqueSgs.size()];
        int i = 0;
        for (SecurityGroupJoinVO v : uniqueSgs ){
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
        Pair<List<DomainRouterJoinVO>, Integer> result = searchForRoutersInternal(cmd);
        ListResponse<DomainRouterResponse> response = new ListResponse<DomainRouterResponse>();

        List<DomainRouterResponse> routerResponses = ViewResponseHelper.createDomainRouterResponse(result.first().toArray(new DomainRouterJoinVO[result.first().size()]));
        response.setResponses(routerResponses, result.second());
        return response;
    }


    private Pair<List<DomainRouterJoinVO>, Integer> searchForRoutersInternal(ListRoutersCmd cmd) {
        Long id = cmd.getId();
        String name = cmd.getRouterName();
        String state = cmd.getState();
        Long zone = cmd.getZoneId();
        Long pod = cmd.getPodId();
        Long hostId = cmd.getHostId();
        String keyword = cmd.getKeyword();
        Long networkId = cmd.getNetworkId();
        Long vpcId = cmd.getVpcId();
        Boolean forVpc = cmd.getForVpc();

        Account caller = UserContext.current().getCaller();
        List<Long> permittedAccounts = new ArrayList<Long>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(
                cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject,
                cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        // no default orderBy
        // Filter searchFilter = new Filter(DomainRouterJoinVO.class, "id",
        // true, cmd.getStartIndex(), cmd.getPageSizeVal());
        Filter searchFilter = new Filter(DomainRouterJoinVO.class, null, true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<DomainRouterJoinVO> sb = _routerJoinDao.createSearchBuilder();
        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct
                                                             // ids to get
                                                             // number of
                                                             // records with
                                                             // pagination
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("name", sb.entity().getHostName(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        sb.and("hostId", sb.entity().getHostId(), SearchCriteria.Op.EQ);
        sb.and("vpcId", sb.entity().getVpcId(), SearchCriteria.Op.EQ);

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

        if (zone != null) {
            sc.setParameters("dataCenterId", zone);
        }

        if (pod != null) {
            sc.setParameters("podId", pod);
        }

        if (hostId != null) {
            sc.setParameters("hostId", hostId);
        }

        if (networkId != null) {
            sc.setJoinParameters("nicSearch", "networkId", networkId);
        }

        if (vpcId != null) {
            sc.setParameters("vpcId", vpcId);
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
        List<ProjectResponse> projectResponses = ViewResponseHelper.createProjectResponse(projects.first().toArray(new ProjectJoinVO[projects.first().size()]));
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

        Account caller = UserContext.current().getCaller();
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

        if (tags != null && !tags.isEmpty()) {
            for (int count = 0; count < tags.size(); count++) {
                sb.or().op("key" + String.valueOf(count), sb.entity().getTagKey(), SearchCriteria.Op.EQ);
                sb.and("value" + String.valueOf(count), sb.entity().getTagValue(), SearchCriteria.Op.EQ);
                sb.cp();
            }
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

        if (tags != null && !tags.isEmpty()) {
            int count = 0;
            for (String key : tags.keySet()) {
                sc.setParameters("key" + String.valueOf(count), key);
                sc.setParameters("value" + String.valueOf(count), tags.get(key));
                count++;
            }
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
        List<ProjectInvitationResponse> projectInvitationResponses =
                ViewResponseHelper.createProjectInvitationResponse(invites.first().toArray(new ProjectInvitationJoinVO[invites.first().size()]));

        response.setResponses(projectInvitationResponses, invites.second());
        return response;
    }

    public Pair<List<ProjectInvitationJoinVO>, Integer> listProjectInvitationsInternal(ListProjectInvitationsCmd cmd){
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

        Account caller = UserContext.current().getCaller();
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


        if (projectId != null){
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

            //long projectId, String accountName, String role, Long startIndex, Long pageSizeVal) {
        Account caller = UserContext.current().getCaller();

        //check that the project exists
        Project project = _projectDao.findById(projectId);

        if (project == null) {
            throw new InvalidParameterValueException("Unable to find the project id=" + projectId);
        }

        //verify permissions - only accounts belonging to the project can list project's account
        if (!_accountMgr.isAdmin(caller.getType()) && _projectAccountDao.findByProjectIdAccountId(projectId, caller.getAccountId()) == null) {
            throw new PermissionDeniedException("Account " + caller + " is not authorized to list users of the project id=" + projectId);
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

        return  _projectAccountJoinDao.searchAndCount(sc, searchFilter);
    }

    @Override
    public ListResponse<HostResponse> searchForServers(ListHostsCmd cmd) {
        //FIXME: do we need to support list hosts with VmId, maybe we should create another command just for this
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

        Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(UserContext.current().getCaller(), cmd.getZoneId());
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
        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct ids
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
                sb.and("tag", sb.entity().getTag(), SearchCriteria.Op.NEQ);
                //FIXME: should we have another condition say tag = null?
                //hostTagSearch.or("tagNull", hostTagSearch.entity().getTag(), SearchCriteria.Op.NULL);
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
        Pair<List<HostJoinVO>, Integer> uniqueHostPair =  _hostJoinDao.searchAndCount(sc, searchFilter);
        Integer count = uniqueHostPair.second();
        if ( count.intValue() == 0 ){
            // handle empty result cases
            return uniqueHostPair;
        }
        List<HostJoinVO> uniqueHosts = uniqueHostPair.first();
        Long[] hostIds = new Long[uniqueHosts.size()];
        int i = 0;
        for (HostJoinVO v : uniqueHosts ){
            hostIds[i++] = v.getId();
        }
        List<HostJoinVO> hosts = _hostJoinDao.searchByIds(hostIds);
        return new Pair<List<HostJoinVO>, Integer>(hosts, count);

    }

}
