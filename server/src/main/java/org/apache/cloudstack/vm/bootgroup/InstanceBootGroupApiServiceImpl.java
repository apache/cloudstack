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

package org.apache.cloudstack.vm.bootgroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.api.command.user.bootgroup.AddMemberToInstanceBootGroupCmd;
import org.apache.cloudstack.api.command.user.bootgroup.CreateInstanceBootGroupCmd;
import org.apache.cloudstack.api.command.user.bootgroup.CreateInstanceBootGroupReadinessRuleCmd;
import org.apache.cloudstack.api.command.user.bootgroup.DeleteInstanceBootGroupCmd;
import org.apache.cloudstack.api.command.user.bootgroup.DeleteInstanceBootGroupReadinessRuleCmd;
import org.apache.cloudstack.api.command.user.bootgroup.ListInstanceBootGroupMembersCmd;
import org.apache.cloudstack.api.command.user.bootgroup.ListInstanceBootGroupReadinessRulesCmd;
import org.apache.cloudstack.api.command.user.bootgroup.ListInstanceBootGroupsCmd;
import org.apache.cloudstack.api.command.user.bootgroup.RebootInstanceBootGroupCmd;
import org.apache.cloudstack.api.command.user.bootgroup.RemoveInstanceBootGroupMemberCmd;
import org.apache.cloudstack.api.command.user.bootgroup.StartInstanceBootGroupCmd;
import org.apache.cloudstack.api.command.user.bootgroup.StopInstanceBootGroupCmd;
import org.apache.cloudstack.api.command.user.bootgroup.UpdateInstanceBootGroupCmd;
import org.apache.cloudstack.api.command.user.bootgroup.UpdateInstanceBootGroupMemberCmd;
import org.apache.cloudstack.api.command.user.bootgroup.UpdateInstanceBootGroupReadinessRuleCmd;
import org.apache.cloudstack.api.query.dao.InstanceBootGroupJoinDao;
import org.apache.cloudstack.api.query.vo.InstanceBootGroupJoinVO;
import org.apache.cloudstack.api.response.InstanceBootGroupMemberChildResponse;
import org.apache.cloudstack.api.response.InstanceBootGroupMemberResponse;
import org.apache.cloudstack.api.response.InstanceBootGroupReadinessRuleResponse;
import org.apache.cloudstack.api.response.InstanceBootGroupResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.vm.bootgroup.readiness.InstanceBootGroupReadinessRule;
import org.apache.cloudstack.vm.bootgroup.readiness.InstanceBootGroupReadinessRuleService;
import org.apache.cloudstack.vm.bootgroup.readiness.ReadinessChecker;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.cloud.api.ApiResponseHelper;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.projects.Project;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.vm.InstanceGroupVMMapVO;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.InstanceBootGroupDao;
import com.cloud.vm.dao.InstanceBootGroupDetailsDao;
import com.cloud.vm.dao.InstanceBootGroupMemberDao;
import com.cloud.vm.dao.InstanceBootGroupReadinessCheckResultDao;
import com.cloud.vm.dao.InstanceBootGroupReadinessRuleDao;
import com.cloud.vm.dao.InstanceBootGroupReadinessRuleDetailsDao;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.InstanceGroupVMMapDao;
import com.cloud.vm.dao.UserVmDao;

/**
 * API-facing half of the Instance Boot Group feature: ACL, param validation, response building,
 * command registration. Delegates orchestration/hypervisor work to {@link InstanceBootGroupManager}
 * and membership eligibility checks to {@link InstanceBootGroupMembershipGuard}.
 */
@Component
public class InstanceBootGroupApiServiceImpl implements InstanceBootGroupService, PluggableService {

    @Inject
    private InstanceBootGroupDao instanceBootGroupDao;

    @Inject
    private InstanceBootGroupJoinDao instanceBootGroupJoinDao;

    @Inject
    private InstanceBootGroupMemberDao instanceBootGroupMemberDao;

    @Inject
    private AccountManager accountManager;

    @Inject
    private UserVmDao userVmDao;

    @Inject
    private InstanceGroupDao instanceGroupDao;

    @Inject
    private InstanceBootGroupManager instanceBootGroupManager;

    @Inject
    private InstanceBootGroupMembershipGuard instanceBootGroupMembershipGuard;

    @Inject
    private InstanceBootGroupReadinessRuleService instanceBootGroupReadinessRuleService;

    @Inject
    private InstanceBootGroupReadinessRuleDao instanceBootGroupReadinessRuleDao;

    @Inject
    private InstanceBootGroupReadinessRuleDetailsDao instanceBootGroupReadinessRuleDetailsDao;

    @Inject
    private InstanceBootGroupReadinessCheckResultDao instanceBootGroupReadinessCheckResultDao;

    @Inject
    private InstanceBootGroupDetailsDao instanceBootGroupDetailsDao;

    @Inject
    private InstanceGroupVMMapDao instanceGroupVMMapDao;

    @NotNull
    protected InstanceBootGroupVO getGroupAndCheckAccess(long id) {
        InstanceBootGroupVO group = instanceBootGroupDao.findById(id);
        if (group == null) {
            throw new InvalidParameterValueException("Unable to find instance boot group with ID: " + id);
        }
        Account caller = CallContext.current().getCallingAccount();
        accountManager.checkAccess(caller, null, true, group);
        return group;
    }

    protected InstanceBootGroupResponse createInstanceBootGroupResponse(InstanceBootGroupJoinVO bootGroup) {
        InstanceBootGroupResponse response = new InstanceBootGroupResponse();
        response.setId(bootGroup.getUuid());
        response.setName(bootGroup.getName());
        response.setDescription(bootGroup.getDescription());
        response.setCreated(bootGroup.getCreated());
        ApiResponseHelper.populateOwner(response, bootGroup);

        String timeoutOverride = instanceBootGroupDetailsDao.getDetail(bootGroup.getId(), InstanceBootGroupManagerImpl.ReadinessAttemptTimeoutSeconds.key());
        response.setReadinessAttemptTimeoutSeconds(timeoutOverride != null ? Long.parseLong(timeoutOverride) : InstanceBootGroupManagerImpl.ReadinessAttemptTimeoutSeconds.value());
        String maxRetryOverride = instanceBootGroupDetailsDao.getDetail(bootGroup.getId(), InstanceBootGroupManagerImpl.ReadinessMaxRetryAttempts.key());
        response.setReadinessMaxRetryAttempts(maxRetryOverride != null ? Long.parseLong(maxRetryOverride) : InstanceBootGroupManagerImpl.ReadinessMaxRetryAttempts.value());
        String rebootOnRetryOverride = instanceBootGroupDetailsDao.getDetail(bootGroup.getId(), InstanceBootGroupManagerImpl.ReadinessRebootOnRetry.key());
        response.setReadinessRebootOnRetry(rebootOnRetryOverride != null ? Boolean.parseBoolean(rebootOnRetryOverride) : InstanceBootGroupManagerImpl.ReadinessRebootOnRetry.value());
        String initialDelayOverride = instanceBootGroupDetailsDao.getDetail(bootGroup.getId(), InstanceBootGroupManagerImpl.ReadinessInitialDelaySeconds.key());
        response.setReadinessInitialDelaySeconds(initialDelayOverride != null ? Long.parseLong(initialDelayOverride) : InstanceBootGroupManagerImpl.ReadinessInitialDelaySeconds.value());

        response.setObjectName("instancebootgroup");
        return response;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_INSTANCE_BOOT_GROUP_CREATE, eventDescription = "creating Instance Boot Group")
    public InstanceBootGroup createInstanceBootGroup(CreateInstanceBootGroupCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Account owner = accountManager.finalizeOwner(caller, cmd.getAccountName(), cmd.getDomainId(), cmd.getProjectId());

        if (instanceBootGroupDao.isNameInUse(owner.getId(), cmd.getName())) {
            throw new InvalidParameterValueException("An instance boot group with name '" + cmd.getName() + "' already exists in this account");
        }

        return Transaction.execute((TransactionCallback<InstanceBootGroupVO>) status -> {
            InstanceBootGroupVO group = new InstanceBootGroupVO(cmd.getName(), cmd.getDescription(), owner.getId(), owner.getDomainId());
            group = instanceBootGroupDao.persist(group);
            CallContext.current().setEventResourceId(group.getId());

            if (cmd.getReadinessAttemptTimeoutSeconds() != null) {
                setOrClearOverride(group.getId(), InstanceBootGroupManagerImpl.ReadinessAttemptTimeoutSeconds.key(), cmd.getReadinessAttemptTimeoutSeconds());
            }
            if (cmd.getReadinessMaxRetryAttempts() != null) {
                setOrClearOverride(group.getId(), InstanceBootGroupManagerImpl.ReadinessMaxRetryAttempts.key(), cmd.getReadinessMaxRetryAttempts());
            }
            if (cmd.getReadinessRebootOnRetry() != null) {
                instanceBootGroupDetailsDao.setDetail(group.getId(), InstanceBootGroupManagerImpl.ReadinessRebootOnRetry.key(), String.valueOf(cmd.getReadinessRebootOnRetry()));
            }
            if (cmd.getReadinessInitialDelaySeconds() != null) {
                setOrClearOverride(group.getId(), InstanceBootGroupManagerImpl.ReadinessInitialDelaySeconds.key(), cmd.getReadinessInitialDelaySeconds());
            }

            return group;
        });
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_INSTANCE_BOOT_GROUP_DELETE, eventDescription = "deleting Instance Boot Group")
    public boolean deleteInstanceBootGroup(DeleteInstanceBootGroupCmd cmd) {
        InstanceBootGroupVO group = getGroupAndCheckAccess(cmd.getId());
        return Transaction.execute((TransactionCallback<Boolean>) status -> {
            instanceBootGroupMemberDao.deleteByBootGroupId(group.getId());
            instanceBootGroupDao.remove(group.getId());
            return true;
        });
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_INSTANCE_BOOT_GROUP_UPDATE, eventDescription = "updating Instance Boot Group")
    public InstanceBootGroup updateInstanceBootGroup(UpdateInstanceBootGroupCmd cmd) {
        InstanceBootGroupVO group = getGroupAndCheckAccess(cmd.getId());

        if (cmd.getName() != null && !Objects.equals(cmd.getName(), group.getName())) {
            Account owner = accountManager.getAccount(group.getAccountId());
            if (instanceBootGroupDao.isNameInUse(owner.getId(), cmd.getName())) {
                throw new InvalidParameterValueException("An instance boot group with name '" + cmd.getName() + "' already exists in this account");
            }
            group.setName(cmd.getName());
        }
        if (cmd.getDescription() != null) {
            group.setDescription(cmd.getDescription());
        }

        return Transaction.execute((TransactionCallback<InstanceBootGroupVO>) status -> {
            if (cmd.getReadinessAttemptTimeoutSeconds() != null) {
                setOrClearOverride(group.getId(), InstanceBootGroupManagerImpl.ReadinessAttemptTimeoutSeconds.key(), cmd.getReadinessAttemptTimeoutSeconds());
            }
            if (cmd.getReadinessMaxRetryAttempts() != null) {
                setOrClearOverride(group.getId(), InstanceBootGroupManagerImpl.ReadinessMaxRetryAttempts.key(), cmd.getReadinessMaxRetryAttempts());
            }
            if (cmd.getReadinessRebootOnRetry() != null) {
                instanceBootGroupDetailsDao.setDetail(group.getId(), InstanceBootGroupManagerImpl.ReadinessRebootOnRetry.key(), String.valueOf(cmd.getReadinessRebootOnRetry()));
            }
            if (cmd.getReadinessInitialDelaySeconds() != null) {
                setOrClearOverride(group.getId(), InstanceBootGroupManagerImpl.ReadinessInitialDelaySeconds.key(), cmd.getReadinessInitialDelaySeconds());
            }

            instanceBootGroupDao.update(group.getId(), group);
            return instanceBootGroupDao.findById(group.getId());
        });
    }

    private void setOrClearOverride(long bootGroupId, String key, long value) {
        if (value < 0) {
            instanceBootGroupDetailsDao.setDetail(bootGroupId, key, null);
        } else {
            instanceBootGroupDetailsDao.setDetail(bootGroupId, key, String.valueOf(value));
        }
    }

    @Override
    public ListResponse<InstanceBootGroupResponse> listInstanceBootGroups(ListInstanceBootGroupsCmd cmd) {
        final CallContext ctx = CallContext.current();
        final Account caller = ctx.getCallingAccount();
        final Long id = cmd.getId();
        final String keyword = cmd.getKeyword();

        List<InstanceBootGroupResponse> responsesList = new ArrayList<>();
        List<Long> permittedAccounts = new ArrayList<>();
        Ternary<Long, Boolean, Project.ListProjectResourcesCriteria> domainIdRecursiveListProject =
                new Ternary<>(cmd.getDomainId(), cmd.isRecursive(), null);
        accountManager.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(),
                permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        Project.ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(InstanceBootGroupJoinVO.class, "id", true, cmd.getStartIndex(),
                cmd.getPageSizeVal());
        SearchBuilder<InstanceBootGroupJoinVO> sb = instanceBootGroupJoinDao.createSearchBuilder();
        accountManager.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("keyword", sb.entity().getName(), SearchCriteria.Op.LIKE);
        SearchCriteria<InstanceBootGroupJoinVO> sc = sb.create();
        accountManager.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);
        if (keyword != null) {
            sc.setParameters("keyword", "%" + keyword + "%");
        }
        if (id != null) {
            sc.setParameters("id", id);
        }
        Pair<List<InstanceBootGroupJoinVO>, Integer> bootGroupsAndCount = instanceBootGroupJoinDao.searchAndCount(sc, searchFilter);
        for (InstanceBootGroupJoinVO bootGroup : bootGroupsAndCount.first()) {
            InstanceBootGroupResponse response = createInstanceBootGroupResponse(bootGroup);
            responsesList.add(response);
        }
        ListResponse<InstanceBootGroupResponse> response = new ListResponse<>();
        response.setResponses(responsesList, bootGroupsAndCount.second());
        return response;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_INSTANCE_BOOT_GROUP_MEMBER_ADD, eventDescription = "adding Instance Boot Group member")
    public InstanceBootGroupMember addMemberToInstanceBootGroup(AddMemberToInstanceBootGroupCmd cmd) {
        InstanceBootGroupVO group = getGroupAndCheckAccess(cmd.getId());

        if (cmd.getOrder() < 0) {
            throw new InvalidParameterValueException("Order value must be 0 or greater");
        }

        InstanceBootGroupMember.MemberType memberType;
        long memberId;

        validateEitherVirtualMachineIdOrInstanceGroupIdParam(cmd.getVirtualMachineId(), cmd.getInstanceGroupId());

        if (cmd.getVirtualMachineId() != null) {
            UserVm vm = getValidatedVmForAddMember(group, cmd.getVirtualMachineId());
            memberType = InstanceBootGroupMember.MemberType.VirtualMachine;
            memberId = vm.getId();
        } else {
            InstanceGroupVO instanceGroup = getValidatedInstanceGroupAddMember(group, cmd.getInstanceGroupId());
            memberType = InstanceBootGroupMember.MemberType.InstanceGroup;
            memberId = instanceGroup.getId();
        }

        if (instanceBootGroupMemberDao.findByMember(memberType, memberId) != null) {
            throw new InvalidParameterValueException(String.format("This %s already belongs to an instance boot group", memberType.name()));
        }

        long groupMembersCount = instanceBootGroupMemberDao.countByBootGroupId(group.getId());
        long maxMembers = InstanceBootGroupManagerImpl.MaxMembersPerBootGroup.valueIn(group.getDomainId());
        if (groupMembersCount >= maxMembers) {
            throw new InvalidParameterValueException(String.format(
                    "Instance boot group %s already has the maximum of %d member(s) allowed", group, maxMembers));
        }

        return Transaction.execute((TransactionCallback<InstanceBootGroupMemberVO>) status -> {
            shiftSiblingOrdersForInsert(group.getId(), cmd.getOrder());
            InstanceBootGroupMemberVO member = new InstanceBootGroupMemberVO(group.getId(), memberType, memberId, cmd.getOrder());
            return instanceBootGroupMemberDao.persist(member);
        });
    }

    /**
     * Makes room for a new member at {@code order} by bumping every existing member already at or
     * past it up by one slot, rather than letting the new member silently share that order.
     */
    private void shiftSiblingOrdersForInsert(long groupId, int order) {
        List<InstanceBootGroupMemberVO> siblings =
                instanceBootGroupMemberDao.listByBootGroupIdAndEqualOrHigherOrder(groupId, order);
        for (InstanceBootGroupMemberVO sibling : siblings) {
            sibling.setBootOrder(sibling.getBootOrder() + 1);
            instanceBootGroupMemberDao.update(sibling.getId(), sibling);
        }
    }

    @NotNull
    private UserVm getValidatedVmForAddMember(InstanceBootGroupVO group, long virtualMachineId) {
        UserVm vm = userVmDao.findById(virtualMachineId);
        if (vm == null) {
            throw new InvalidParameterValueException("Unable to find Instance with the specified ID");
        }
        validateMemberAccount(vm.getAccountId(), group.getAccountId());
        instanceBootGroupMembershipGuard.validateVmEligibleForGroupMembership(vm.getId());
        return vm;
    }

    @NotNull
    private InstanceGroupVO getValidatedInstanceGroupAddMember(InstanceBootGroupVO group, long instanceGroupId) {
        InstanceGroupVO instanceGroup = instanceGroupDao.findById(instanceGroupId);
        if (instanceGroup == null) {
            throw new InvalidParameterValueException("Unable to find instance group with the specified ID");
        }
        validateMemberAccount(instanceGroup.getAccountId(), group.getAccountId());
        instanceBootGroupMembershipGuard.validateInstanceGroupEligibleForBootGroupMembership(instanceGroup.getId());
        return instanceGroup;
    }

    protected static void validateEitherVirtualMachineIdOrInstanceGroupIdParam(Long virtualMachineId, Long instanceGroupId) {
        if (virtualMachineId != null && instanceGroupId != null) {
            throw new InvalidParameterValueException("Only one of virtualmachineid or instancegroupid may be specified");
        }
        if (virtualMachineId == null && instanceGroupId == null) {
            throw new InvalidParameterValueException("Either virtualmachineid or instancegroupid must be specified");
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_INSTANCE_BOOT_GROUP_MEMBER_REMOVE, eventDescription = "removing Instance Boot Group member")
    public boolean removeInstanceBootGroupMember(RemoveInstanceBootGroupMemberCmd cmd) {
        InstanceBootGroupMemberVO member = instanceBootGroupMemberDao.findById(cmd.getId());
        if (member == null) {
            throw new InvalidParameterValueException("Unable to find boot group member with ID: " + cmd.getId());
        }
        getGroupAndCheckAccess(member.getBootGroupId());
        instanceBootGroupMemberDao.expunge(member.getId());
        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_INSTANCE_BOOT_GROUP_MEMBER_REORDER, eventDescription = "reordering Instance Boot Group member")
    public InstanceBootGroupMember updateInstanceBootGroupMember(UpdateInstanceBootGroupMemberCmd cmd) {
        InstanceBootGroupMemberVO member = instanceBootGroupMemberDao.findById(cmd.getId());
        if (member == null) {
            throw new InvalidParameterValueException("Unable to find boot group member with ID: " + cmd.getId());
        }
        int newOrder = cmd.getOrder();
        if (newOrder < 0) {
            throw new InvalidParameterValueException("Order value must be 0 or greater");
        }
        getGroupAndCheckAccess(member.getBootGroupId());

        int oldOrder = member.getBootOrder();
        if (newOrder == oldOrder) {
            return member;
        }
        return Transaction.execute((TransactionCallback<InstanceBootGroupMemberVO>) status -> {
            shiftSiblingOrders(member, oldOrder, newOrder);
            member.setBootOrder(newOrder);
            instanceBootGroupMemberDao.update(member.getId(), member);
            return instanceBootGroupMemberDao.findById(member.getId());
        });
    }

    /**
     * Shifts every other member between the old and new position by one slot — list-reorder
     * semantics, not just moving the single member whose order was explicitly given.
     */
    private void shiftSiblingOrders(InstanceBootGroupMemberVO member, int oldOrder, int newOrder) {
        int low = Math.min(oldOrder, newOrder);
        int high = Math.max(oldOrder, newOrder);
        int delta = newOrder > oldOrder ? -1 : 1;
        List<InstanceBootGroupMemberVO> siblings =
                instanceBootGroupMemberDao.listByBootGroupIdAndOrderRange(member.getBootGroupId(), low, high);
        for (InstanceBootGroupMemberVO sibling : siblings) {
            if (sibling.getId() == member.getId()) {
                continue;
            }
            sibling.setBootOrder(sibling.getBootOrder() + delta);
            instanceBootGroupMemberDao.update(sibling.getId(), sibling);
        }
    }

    @Override
    public ListResponse<InstanceBootGroupMemberResponse> listInstanceBootGroupMembers(ListInstanceBootGroupMembersCmd cmd) {
        InstanceBootGroupVO group = getGroupAndCheckAccess(cmd.getBootGroupId());

        Pair<List<InstanceBootGroupMemberVO>, Integer> result;
        if (cmd.getMemberType() != null) {
            InstanceBootGroupMember.MemberType type = InstanceBootGroupMember.MemberType.valueOf(cmd.getMemberType());
            result = instanceBootGroupMemberDao.searchAndCountByBootGroupIdAndType(group.getId(), type);
        } else {
            result = instanceBootGroupMemberDao.searchAndCountByBootGroupId(group.getId());
        }

        List<InstanceBootGroupMemberVO> members = result.first();
        members.sort(Comparator.comparingInt(InstanceBootGroupMemberVO::getBootOrder));
        boolean includeReadiness = cmd.isReadinessDetailRequested();
        boolean includeChildren = cmd.isChildrenDetailRequested();
        boolean ignoreVmState = cmd.isIgnoreInstanceState();
        ListResponse<InstanceBootGroupMemberResponse> response = new ListResponse<>();
        response.setResponses(members.stream().map(member -> createInstanceBootGroupMemberResponse(member, includeReadiness, includeChildren, ignoreVmState)).collect(Collectors.toList()), result.second());
        return response;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_INSTANCE_BOOT_GROUP_START, eventDescription = "starting Instance Boot Group", async = true)
    public InstanceBootGroup startInstanceBootGroup(final StartInstanceBootGroupCmd cmd) {
        InstanceBootGroupVO group = getGroupAndCheckAccess(cmd.getId());
        instanceBootGroupManager.startInstanceBootGroup(group);
        return group;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_INSTANCE_BOOT_GROUP_STOP, eventDescription = "stopping Instance Boot Group", async = true)
    public InstanceBootGroup stopInstanceBootGroup(final StopInstanceBootGroupCmd cmd) {
        InstanceBootGroupVO group = getGroupAndCheckAccess(cmd.getId());
        instanceBootGroupManager.stopInstanceBootGroup(group, cmd.isForced());
        return group;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_INSTANCE_BOOT_GROUP_REBOOT, eventDescription = "rebooting Instance Boot Group", async = true)
    public InstanceBootGroup rebootInstanceBootGroup(final RebootInstanceBootGroupCmd cmd) {
        InstanceBootGroupVO group = getGroupAndCheckAccess(cmd.getId());
        instanceBootGroupManager.rebootInstanceBootGroup(group, cmd.isForced());
        return group;
    }

    @Override
    public InstanceBootGroupResponse createInstanceBootGroupResponse(long id) {
        return createInstanceBootGroupResponse(instanceBootGroupJoinDao.findById(id));
    }

    @Override
    public InstanceBootGroupMemberResponse createInstanceBootGroupMemberResponse(InstanceBootGroupMember member) {
        return createInstanceBootGroupMemberResponse(member, false, false, false);
    }

    private InstanceBootGroupMemberResponse createInstanceBootGroupMemberResponse(InstanceBootGroupMember member, boolean includeReadiness, boolean includeChildren, boolean ignoreVmState) {
        InstanceBootGroupMemberResponse response = new InstanceBootGroupMemberResponse();
        response.setId(member.getUuid());
        InstanceBootGroupVO group = instanceBootGroupDao.findById(member.getBootGroupId());
        if (group != null) {
            response.setBootGroupId(group.getUuid());
        }
        response.setMemberType(member.getMemberType().name());
        response.setOrder(member.getBootOrder());
        response.setCreated(member.getCreated());

        List<Long> childVmIds = new ArrayList<>();
        if (member.getMemberType() == InstanceBootGroupMember.MemberType.VirtualMachine) {
            UserVmVO vm = userVmDao.findById(member.getMemberId());
            if (vm != null) {
                response.setMemberId(vm.getUuid());
                response.setMemberName(StringUtils.defaultIfEmpty(vm.getDisplayName(), vm.getHostName()));
                response.setMemberState(vm.getState().toString());
            }
        } else {
            InstanceGroupVO instanceGroup = instanceGroupDao.findById(member.getMemberId());
            if (instanceGroup != null) {
                response.setMemberId(instanceGroup.getUuid());
                response.setMemberName(instanceGroup.getName());
            }
            if (includeReadiness || includeChildren) {
                childVmIds = instanceGroupVMMapDao.listByGroupId(member.getMemberId()).stream()
                        .map(InstanceGroupVMMapVO::getInstanceId)
                        .collect(Collectors.toList());
            }
        }

        if (includeReadiness) {
            response.setReadinessMode(computeReadinessMode(member.getMemberType(), member.getBootGroupId(), member.getMemberId()));
            ReadinessChecker.Result readinessResult = member.getMemberType() == InstanceBootGroupMember.MemberType.VirtualMachine
                    ? computeCachedVmReadinessResult(member.getBootGroupId(), member.getMemberId(), ignoreVmState)
                    : computeCachedInstanceGroupReadinessResult(member.getBootGroupId(), member.getMemberId(), childVmIds, ignoreVmState);
            response.setReadinessStatus(readinessResult.getStatus().name());
            response.setReadinessMessage(readinessResult.getMessage());
        }

        if (includeChildren && member.getMemberType() == InstanceBootGroupMember.MemberType.InstanceGroup) {
            response.setChildren(buildChildrenResponses(member.getBootGroupId(), childVmIds, includeReadiness, ignoreVmState));
        }

        response.setObjectName("instancebootgroupmember");
        return response;
    }

    /**
     * Basic VM fields only (id/name/state, all off UserVmDao) — fetched in a single batch via
     * listByIds rather than one findById per VM, since an InstanceGroup can hold many VMs.
     */
    private List<InstanceBootGroupMemberChildResponse> buildChildrenResponses(long bootGroupId, List<Long> vmIds, boolean includeReadiness, boolean ignoreVmState) {
        List<InstanceBootGroupMemberChildResponse> children = new ArrayList<>();
        if (vmIds.isEmpty()) {
            return children;
        }
        for (UserVmVO vm : userVmDao.listByIds(vmIds)) {
            InstanceBootGroupMemberChildResponse child = new InstanceBootGroupMemberChildResponse();
            child.setId(vm.getUuid());
            child.setName(StringUtils.defaultIfEmpty(vm.getDisplayName(), vm.getHostName()));
            child.setState(vm.getState().toString());
            if (includeReadiness) {
                child.setReadinessMode(computeReadinessMode(InstanceBootGroupMember.MemberType.VirtualMachine, bootGroupId, vm.getId()));
                ReadinessChecker.Result readinessResult = computeCachedVmReadinessResult(bootGroupId, vm.getId(), ignoreVmState);
                child.setReadinessStatus(readinessResult.getStatus().name());
                child.setReadinessMessage(readinessResult.getMessage());
            }
            children.add(child);
        }
        return children;
    }

    private String computeReadinessMode(InstanceBootGroupMember.MemberType itemType, long bootGroupId, long itemId) {
        boolean hasRules = !instanceBootGroupReadinessRuleDao.listEnabledByItem(bootGroupId, itemType, itemId).isEmpty();
        boolean hasInheritedRules = itemType == InstanceBootGroupMember.MemberType.VirtualMachine
                && !instanceBootGroupReadinessRuleService.findInheritedGroupRules(bootGroupId, itemId).isEmpty();
        InstanceBootGroupMember.ReadinessMode readinessMode = InstanceBootGroupMember.ReadinessMode.None;
        if (hasRules || hasInheritedRules) {
            readinessMode = InstanceBootGroupMember.ReadinessMode.RuleBased;
        } else if (InstanceBootGroupMember.MemberType.InstanceGroup.equals(itemType)) {
            readinessMode = InstanceBootGroupMember.ReadinessMode.ChildDependent;
        }
        return readinessMode.name();
    }

    /**
     * Reads cached results only — viewing/polling the member list must never itself dispatch a remote
     * check as a side effect. Combines the VM's own direct rules (cached at vmId 0) with any rules it
     * inherits from its owning InstanceGroup (cached per-member at this VM's own id).
     * @param ignoreVmState if false (the normal case), a non-Running VM is always NotReady regardless of
     *        any cached rule result, so a VM that stopped can't keep reporting a stale Ready; pass true
     *        to see the raw last-cached rule result for diagnosing what happened before it stopped.
     */
    private ReadinessChecker.Result computeCachedVmReadinessResult(long bootGroupId, long vmId, boolean ignoreVmState) {
        List<InstanceBootGroupReadinessRuleVO> directRules = instanceBootGroupReadinessRuleDao.listEnabledByItem(bootGroupId, InstanceBootGroupMember.MemberType.VirtualMachine, vmId);
        List<InstanceBootGroupReadinessRule> inheritedRules = instanceBootGroupReadinessRuleService.findInheritedGroupRules(bootGroupId, vmId);

        UserVmVO vm = userVmDao.findById(vmId);
        boolean running = vm != null && vm.getState() == com.cloud.vm.VirtualMachine.State.Running;
        String vmState = vm != null ? vm.getState().toString() : "unknown";

        if (directRules.isEmpty() && inheritedRules.isEmpty()) {
            InstanceBootGroupReadinessRule.Status status = running ? InstanceBootGroupReadinessRule.Status.Ready : InstanceBootGroupReadinessRule.Status.NotReady;
            return new ReadinessChecker.Result(status, "Instance state is " + vmState + ". No readiness rules attached");
        }
        if (!running && !ignoreVmState) {
            return new ReadinessChecker.Result(InstanceBootGroupReadinessRule.Status.NotReady, "Instance state is " + vmState);
        }

        List<Pair<InstanceBootGroupReadinessRule, Long>> ruleAndCacheVmIds = new ArrayList<>();
        for (InstanceBootGroupReadinessRuleVO rule : directRules) {
            ruleAndCacheVmIds.add(new Pair<>(rule, 0L));
        }
        for (InstanceBootGroupReadinessRule rule : inheritedRules) {
            ruleAndCacheVmIds.add(new Pair<>(rule, vmId));
        }
        return combineCachedRuleResults(ruleAndCacheVmIds);
    }

    /**
     * With a MemberQuorum rule, neither a member's own status nor any other member-targeted group
     * rule's all-members aggregate gates {@code ownResult} — the quorum rule's own tolerance-aware
     * verdict is the one that counts. Without one, every member (and every group rule) must be ready.
     * Unless {@code ignoreVmState}, the group's own rule rows are refreshed first — a group-scope
     * rule (MemberQuorum in particular) isn't tied to any one VM's state, so nothing else re-derives
     * it once a member stops outside of active boot-group orchestration.
     */
    private ReadinessChecker.Result computeCachedInstanceGroupReadinessResult(long bootGroupId, long instanceGroupId, List<Long> memberVmIds, boolean ignoreVmState) {
        List<InstanceBootGroupReadinessRuleVO> groupRules = instanceBootGroupReadinessRuleDao.listEnabledByItem(bootGroupId, InstanceBootGroupMember.MemberType.InstanceGroup, instanceGroupId);
        if (!ignoreVmState && !groupRules.isEmpty()) {
            instanceBootGroupReadinessRuleService.evaluateInstanceGroupReadiness(bootGroupId, instanceGroupId, Collections.emptySet());
        }
        boolean hasMemberQuorumRule = groupRules.stream().anyMatch(rule -> rule.getRuleType() == InstanceBootGroupReadinessRule.RuleType.MemberQuorum);
        List<Pair<InstanceBootGroupReadinessRule, Long>> ownRuleAndCacheVmIds = new ArrayList<>();
        for (InstanceBootGroupReadinessRuleVO rule : groupRules) {
            if (hasMemberQuorumRule && rule.getRuleType().isMemberTargeted()) {
                continue;
            }
            ownRuleAndCacheVmIds.add(new Pair<>(rule, 0L));
        }
        ReadinessChecker.Result ownResult = combineCachedRuleResults(ownRuleAndCacheVmIds);

        boolean anyError = ownResult.getStatus() == InstanceBootGroupReadinessRule.Status.Error;
        boolean anyNotReady = !anyError && ownResult.getStatus() != InstanceBootGroupReadinessRule.Status.Ready;

        int notReadyChildren = 0;
        for (Long vmId : memberVmIds) {
            InstanceBootGroupReadinessRule.Status vmStatus = computeCachedVmReadinessResult(bootGroupId, vmId, ignoreVmState).getStatus();
            if (vmStatus != InstanceBootGroupReadinessRule.Status.Ready) {
                notReadyChildren++;
            }
            if (hasMemberQuorumRule) {
                continue;
            }
            if (vmStatus == InstanceBootGroupReadinessRule.Status.Error) {
                anyError = true;
            } else if (vmStatus != InstanceBootGroupReadinessRule.Status.Ready) {
                anyNotReady = true;
            }
        }

        InstanceBootGroupReadinessRule.Status aggregateStatus = anyError ? InstanceBootGroupReadinessRule.Status.Error
                : (anyNotReady ? InstanceBootGroupReadinessRule.Status.NotReady : InstanceBootGroupReadinessRule.Status.Ready);

        List<String> messageParts = new ArrayList<>();
        if (!groupRules.isEmpty() && ownResult.getStatus() != InstanceBootGroupReadinessRule.Status.Ready) {
            messageParts.add(ownResult.getMessage());
        }
        if (notReadyChildren > 0) {
            messageParts.add(String.format("%d of %d member VM(s) not ready", notReadyChildren, memberVmIds.size()));
        }
        String message = messageParts.isEmpty() ? "all readiness rules ready" : String.join("; ", messageParts);

        return new ReadinessChecker.Result(aggregateStatus, message);
    }

    /**
     * Reduces multiple rules to one status (any Error wins, else any non-Ready means NotReady) plus
     * the cached message(s) of whichever rule(s) are at that worst severity, rule-type-prefixed.
     * @param ruleAndCacheVmIds cache-vmId is 0 for the rule's own row, or a member's vmId if inherited.
     */
    private ReadinessChecker.Result combineCachedRuleResults(List<Pair<InstanceBootGroupReadinessRule, Long>> ruleAndCacheVmIds) {
        boolean anyError = false;
        boolean anyNotReady = false;
        List<String> errorMessages = new ArrayList<>();
        List<String> notReadyMessages = new ArrayList<>();
        for (Pair<InstanceBootGroupReadinessRule, Long> entry : ruleAndCacheVmIds) {
            InstanceBootGroupReadinessRule rule = entry.first();
            InstanceBootGroupReadinessCheckResultVO result = instanceBootGroupReadinessCheckResultDao.findByRuleAndVm(rule.getId(), entry.second());
            InstanceBootGroupReadinessRule.Status status = (result != null && result.getStatus() != null) ? result.getStatus() : InstanceBootGroupReadinessRule.Status.Unknown;
            String detail = result != null ? result.getMessage() : null;
            String labeledMessage = rule.getRuleType().name() + (StringUtils.isNotBlank(detail) ? (": " + detail) : "");
            if (status == InstanceBootGroupReadinessRule.Status.Error) {
                anyError = true;
                errorMessages.add(labeledMessage);
            } else if (status != InstanceBootGroupReadinessRule.Status.Ready) {
                anyNotReady = true;
                notReadyMessages.add(labeledMessage);
            }
        }
        if (anyError) {
            return new ReadinessChecker.Result(InstanceBootGroupReadinessRule.Status.Error, String.join("; ", errorMessages));
        }
        if (anyNotReady) {
            return new ReadinessChecker.Result(InstanceBootGroupReadinessRule.Status.NotReady, String.join("; ", notReadyMessages));
        }
        return new ReadinessChecker.Result(InstanceBootGroupReadinessRule.Status.Ready, "All readiness rules ready");
    }

    private void validateMemberAccount(long memberAccountId, long groupAccountId) {
        if (memberAccountId != groupAccountId) {
            throw new PermissionDeniedException("Member must belong to the same account as the boot group");
        }
    }

    /**
     * Resolves the mutually-exclusive virtualmachineid/instancegroupid item params and checks access
     * on the resolved item (in addition to the boot group, already checked via getGroupAndCheckAccess).
     */
    private Pair<InstanceBootGroupMember.MemberType, Long> resolveAndCheckAccessToItem(Long virtualMachineId, Long instanceGroupId) {
        validateEitherVirtualMachineIdOrInstanceGroupIdParam(virtualMachineId, instanceGroupId);

        Account caller = CallContext.current().getCallingAccount();
        if (virtualMachineId != null) {
            UserVmVO vm = userVmDao.findById(virtualMachineId);
            if (vm == null) {
                throw new InvalidParameterValueException("Unable to find virtual machine with ID: " + virtualMachineId);
            }
            accountManager.checkAccess(caller, null, true, vm);
            return new Pair<>(InstanceBootGroupMember.MemberType.VirtualMachine, vm.getId());
        }
        InstanceGroupVO instanceGroup = instanceGroupDao.findById(instanceGroupId);
        if (instanceGroup == null || instanceGroup.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find instance group with ID: " + instanceGroupId);
        }
        accountManager.checkAccess(caller, null, true, instanceGroup);
        return new Pair<>(InstanceBootGroupMember.MemberType.InstanceGroup, instanceGroup.getId());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_INSTANCE_BOOT_GROUP_READINESS_RULE_CREATE, eventDescription = "creating Instance Boot Group readiness rule")
    public InstanceBootGroupReadinessRule createInstanceBootGroupReadinessRule(CreateInstanceBootGroupReadinessRuleCmd cmd) {
        getGroupAndCheckAccess(cmd.getBootGroupId());
        Pair<InstanceBootGroupMember.MemberType, Long> item = resolveAndCheckAccessToItem(cmd.getVirtualMachineId(), cmd.getInstanceGroupId());

        InstanceBootGroupReadinessRule.RuleType ruleType = EnumUtils.getEnumIgnoreCase(InstanceBootGroupReadinessRule.RuleType.class, cmd.getRuleType());
        if (ruleType == null) {
            throw new InvalidParameterValueException("Invalid rule type: " + cmd.getRuleType());
        }

        return instanceBootGroupReadinessRuleService.createReadinessRule(cmd.getBootGroupId(), item.first(), item.second(),
                ruleType, cmd.getName(), cmd.isEnabled(), cmd.getDetails());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_INSTANCE_BOOT_GROUP_READINESS_RULE_UPDATE, eventDescription = "updating Instance Boot Group readiness rule")
    public InstanceBootGroupReadinessRule updateInstanceBootGroupReadinessRule(UpdateInstanceBootGroupReadinessRuleCmd cmd) {
        InstanceBootGroupReadinessRule rule = instanceBootGroupReadinessRuleDao.findById(cmd.getId());
        if (rule == null) {
            throw new InvalidParameterValueException("Unable to find a readiness rule with ID: " + cmd.getId());
        }
        getGroupAndCheckAccess(rule.getBootGroupId());

        return instanceBootGroupReadinessRuleService.updateReadinessRule(cmd.getId(), cmd.getName(), cmd.getEnabled(), cmd.getDetails());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_INSTANCE_BOOT_GROUP_READINESS_RULE_DELETE, eventDescription = "deleting Instance Boot Group readiness rule")
    public boolean deleteInstanceBootGroupReadinessRule(DeleteInstanceBootGroupReadinessRuleCmd cmd) {
        InstanceBootGroupReadinessRule rule = instanceBootGroupReadinessRuleDao.findById(cmd.getId());
        if (rule == null) {
            throw new InvalidParameterValueException("Unable to find a readiness rule with ID: " + cmd.getId());
        }
        getGroupAndCheckAccess(rule.getBootGroupId());

        return instanceBootGroupReadinessRuleService.deleteReadinessRule(cmd.getId());
    }

    /**
     * When filtering by VM, also surfaces Ping/PortCheck/GuestAgentLiveness rules the VM inherits from
     * its owning InstanceGroup (marked {@code inherited=true}), not just its own direct rules.
     */
    @Override
    public ListResponse<InstanceBootGroupReadinessRuleResponse> listInstanceBootGroupReadinessRules(ListInstanceBootGroupReadinessRulesCmd cmd) {
        getGroupAndCheckAccess(cmd.getBootGroupId());

        if (cmd.getVirtualMachineId() != null && cmd.getInstanceGroupId() != null) {
            throw new InvalidParameterValueException("Only one of virtualmachineid or instancegroupid may be specified");
        }
        InstanceBootGroupReadinessRule.RuleType ruleType = null;
        if (cmd.getRuleType() != null) {
            ruleType = EnumUtils.getEnumIgnoreCase(InstanceBootGroupReadinessRule.RuleType.class, cmd.getRuleType());
            if (ruleType == null) {
                throw new InvalidParameterValueException("Invalid rule type: " + cmd.getRuleType());
            }
        }

        InstanceBootGroupMember.MemberType memberType = null;
        Long memberId = null;
        if (cmd.getVirtualMachineId() != null) {
            memberType = InstanceBootGroupMember.MemberType.VirtualMachine;
            memberId = cmd.getVirtualMachineId();
        } else if (cmd.getInstanceGroupId() != null) {
            memberType = InstanceBootGroupMember.MemberType.InstanceGroup;
            memberId = cmd.getInstanceGroupId();
        }
        Pair<List<InstanceBootGroupReadinessRuleVO>, Integer> rulesAndCount = instanceBootGroupReadinessRuleDao.searchAndCountByBootGroupId(
                cmd.getBootGroupId(),
                cmd.getId(),
                memberType,
                memberId,
                ruleType,
                cmd.getKeyword(),
                cmd.getStartIndex(),
                cmd.getPageSizeVal());

        List<InstanceBootGroupReadinessRuleResponse> responsesList = rulesAndCount.first().stream()
                .map(rule -> createInstanceBootGroupReadinessRuleResponse(rule, false, 0))
                .collect(Collectors.toList());
        int totalCount = rulesAndCount.second();

        if (cmd.getId() == null && cmd.getVirtualMachineId() != null) {
            for (InstanceBootGroupReadinessRule rule : instanceBootGroupReadinessRuleService.findInheritedGroupRules(cmd.getBootGroupId(), cmd.getVirtualMachineId())) {
                if (ruleType != null && rule.getRuleType() != ruleType) {
                    continue;
                }
                if (cmd.getKeyword() != null && (rule.getName() == null || !rule.getName().toLowerCase().contains(cmd.getKeyword().toLowerCase()))) {
                    continue;
                }
                responsesList.add(createInstanceBootGroupReadinessRuleResponse(rule, true, cmd.getVirtualMachineId()));
                totalCount++;
            }
        }

        ListResponse<InstanceBootGroupReadinessRuleResponse> response = new ListResponse<>();
        response.setResponses(responsesList, totalCount);
        return response;
    }

    @Override
    public InstanceBootGroupReadinessRuleResponse createInstanceBootGroupReadinessRuleResponse(InstanceBootGroupReadinessRule rule) {
        return createInstanceBootGroupReadinessRuleResponse(rule, false, 0);
    }

    @Override
    public Long getInstanceBootGroupIdForMember(long memberId) {
        InstanceBootGroupMember member = instanceBootGroupMemberDao.findById(memberId);
        return member == null ? null : member.getBootGroupId();
    }

    private InstanceBootGroupReadinessRuleResponse createInstanceBootGroupReadinessRuleResponse(InstanceBootGroupReadinessRule rule, boolean inherited, long statusVmId) {
        InstanceBootGroupReadinessRuleResponse response = new InstanceBootGroupReadinessRuleResponse();
        response.setId(rule.getUuid());
        response.setName(rule.getName());
        InstanceBootGroupVO group = instanceBootGroupDao.findById(rule.getBootGroupId());
        if (group != null) {
            response.setBootGroupId(group.getUuid());
        }
        response.setItemType(rule.getItemType().name());
        response.setEnabled(rule.isEnabled());
        response.setRuleType(rule.getRuleType().name());
        response.setCreated(rule.getCreated());
        response.setDetails(instanceBootGroupReadinessRuleDetailsDao.getDetails(rule.getId()));
        response.setInherited(inherited);

        if (rule.getItemType() == InstanceBootGroupMember.MemberType.VirtualMachine) {
            UserVmVO vm = userVmDao.findById(rule.getItemId());
            if (vm != null) {
                response.setItemId(vm.getUuid());
                response.setItemName(StringUtils.defaultIfEmpty(vm.getDisplayName(), vm.getHostName()));
            }
        } else {
            InstanceGroupVO instanceGroup = instanceGroupDao.findById(rule.getItemId());
            if (instanceGroup != null) {
                response.setItemId(instanceGroup.getUuid());
                response.setItemName(instanceGroup.getName());
            }
        }

        InstanceBootGroupReadinessCheckResultVO result = instanceBootGroupReadinessCheckResultDao.findByRuleAndVm(rule.getId(), statusVmId);
        if (result != null) {
            response.setStatus(result.getStatus() == null ? null : result.getStatus().name());
            response.setStatusMessage(result.getMessage());
            response.setCheckedOn(result.getCheckedOn());
        }

        response.setObjectName("instancebootgroupreadinessrule");
        return response;
    }

    @Override
    public List<Class<?>> getCommands() {
        return List.of(
                CreateInstanceBootGroupCmd.class,
                DeleteInstanceBootGroupCmd.class,
                UpdateInstanceBootGroupCmd.class,
                ListInstanceBootGroupsCmd.class,
                AddMemberToInstanceBootGroupCmd.class,
                RemoveInstanceBootGroupMemberCmd.class,
                UpdateInstanceBootGroupMemberCmd.class,
                ListInstanceBootGroupMembersCmd.class,
                StartInstanceBootGroupCmd.class,
                StopInstanceBootGroupCmd.class,
                RebootInstanceBootGroupCmd.class,
                CreateInstanceBootGroupReadinessRuleCmd.class,
                UpdateInstanceBootGroupReadinessRuleCmd.class,
                DeleteInstanceBootGroupReadinessRuleCmd.class,
                ListInstanceBootGroupReadinessRulesCmd.class
        );
    }
}
