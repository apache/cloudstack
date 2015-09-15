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
package org.apache.cloudstack.affinity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.utils.fsm.StateMachine2;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupDomainMapDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.PublishScope;

import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.fsm.StateListener;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmDao;

@Local(value = {AffinityGroupService.class})
public class AffinityGroupServiceImpl extends ManagerBase implements AffinityGroupService, Manager, StateListener<State, VirtualMachine.Event, VirtualMachine> {

    private String _name;

    @Inject
    AccountManager _accountMgr;

    @Inject
    AffinityGroupDao _affinityGroupDao;

    @Inject
    AffinityGroupVMMapDao _affinityGroupVMMapDao;

    @Inject
    AffinityGroupDomainMapDao _affinityGroupDomainMapDao;

    @Inject
    private UserVmDao _userVmDao;

    @Inject
    DomainDao _domainDao;

    @Inject
    DomainManager _domainMgr;

    @Inject
    MessageBus _messageBus;

    protected List<AffinityGroupProcessor> _affinityProcessors;

    public List<AffinityGroupProcessor> getAffinityGroupProcessors() {
        return _affinityProcessors;
    }

    public void setAffinityGroupProcessors(List<AffinityGroupProcessor> affinityProcessors) {
        _affinityProcessors = affinityProcessors;
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_AFFINITY_GROUP_CREATE, eventDescription = "Creating Affinity Group", create = true)
    public AffinityGroup createAffinityGroup(String account, Long domainId, String affinityGroupName, String affinityGroupType, String description) {

        Account caller = CallContext.current().getCallingAccount();

        //validate the affinityGroupType
        Map<String, AffinityGroupProcessor> typeProcessorMap = getAffinityTypeToProcessorMap();
        if (typeProcessorMap != null && !typeProcessorMap.isEmpty()) {
            if (!typeProcessorMap.containsKey(affinityGroupType)) {
                throw new InvalidParameterValueException("Unable to create affinity group, invalid affinity group type" + affinityGroupType);
            }
        } else {
            throw new InvalidParameterValueException("Unable to create affinity group, no Affinity Group Types configured");
        }

        AffinityGroupProcessor processor = typeProcessorMap.get(affinityGroupType);

        if (processor.isAdminControlledGroup()) {
            throw new PermissionDeniedException("Cannot create the affinity group");
        }

        return createAffinityGroupInternal(account, domainId, affinityGroupName, affinityGroupType, description);
    }

    @DB
    @Override
    public AffinityGroup createAffinityGroupInternal(String account, final Long domainId, final String affinityGroupName, final String affinityGroupType,
        final String description) {

        Account caller = CallContext.current().getCallingAccount();

        // validate the affinityGroupType
        Map<String, AffinityGroupProcessor> typeProcessorMap = getAffinityTypeToProcessorMap();
        if (typeProcessorMap != null && !typeProcessorMap.isEmpty()) {
            if (!typeProcessorMap.containsKey(affinityGroupType)) {
                throw new InvalidParameterValueException("Unable to create affinity group, invalid affinity group type" + affinityGroupType);
            }
        } else {
            throw new InvalidParameterValueException("Unable to create affinity group, no Affinity Group Types configured");
        }

        final AffinityGroupProcessor processor = typeProcessorMap.get(affinityGroupType);

        if (processor.isAdminControlledGroup() && !_accountMgr.isRootAdmin(caller.getId())) {
            throw new PermissionDeniedException("Cannot create the affinity group");
        }

        ControlledEntity.ACLType aclType = null;
        Account owner = null;
        boolean domainLevel = false;

        if (account != null && domainId != null) {

            owner = _accountMgr.finalizeOwner(caller, account, domainId, null);
            aclType = ControlledEntity.ACLType.Account;

        } else if (domainId != null && account == null) {

            if (!_accountMgr.isRootAdmin(caller.getId())) {
                // non root admin need to pass both account and domain
                throw new InvalidParameterValueException("Unable to create affinity group, account name must be passed with the domainId");
            } else if (!processor.canBeSharedDomainWide()) {
                // cannot be domain level
                throw new InvalidParameterValueException("Unable to create affinity group, account name is needed");
            }

            DomainVO domain = _domainDao.findById(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Unable to find domain by specified id");
            }
            _accountMgr.checkAccess(caller, domain);

            // domain level group, owner is SYSTEM.
            owner = _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM);
            aclType = ControlledEntity.ACLType.Domain;
            domainLevel = true;

        } else {
            owner = caller;
            aclType = ControlledEntity.ACLType.Account;
        }

        if (_affinityGroupDao.isNameInUse(owner.getAccountId(), owner.getDomainId(), affinityGroupName)) {
            throw new InvalidParameterValueException("Unable to create affinity group, a group with name " + affinityGroupName + " already exists.");
        }
        if (domainLevel && _affinityGroupDao.findDomainLevelGroupByName(domainId, affinityGroupName) != null) {
            throw new InvalidParameterValueException("Unable to create affinity group, a group with name " + affinityGroupName + " already exists under the domain.");
        }

        final Account ownerFinal = owner;
        final ControlledEntity.ACLType aclTypeFinal = aclType;
        AffinityGroupVO group = Transaction.execute(new TransactionCallback<AffinityGroupVO>() {
            @Override
            public AffinityGroupVO doInTransaction(TransactionStatus status) {
                AffinityGroupVO group =
                    new AffinityGroupVO(affinityGroupName, affinityGroupType, description, ownerFinal.getDomainId(), ownerFinal.getId(), aclTypeFinal);
                _affinityGroupDao.persist(group);

                if (domainId != null && aclTypeFinal == ACLType.Domain) {
                    boolean subDomainAccess = false;
                    subDomainAccess = processor.subDomainAccess();
                    AffinityGroupDomainMapVO domainMap = new AffinityGroupDomainMapVO(group.getId(), domainId,
                            subDomainAccess);
                    _affinityGroupDomainMapDao.persist(domainMap);
                    //send event for storing the domain wide resource access
                    Map<String, Object> params = new HashMap<String, Object>();
                    params.put(ApiConstants.ENTITY_TYPE, AffinityGroup.class);
                    params.put(ApiConstants.ENTITY_ID, group.getId());
                    params.put(ApiConstants.DOMAIN_ID, domainId);
                    params.put(ApiConstants.SUBDOMAIN_ACCESS, subDomainAccess);
                    _messageBus.publish(_name, EntityManager.MESSAGE_ADD_DOMAIN_WIDE_ENTITY_EVENT, PublishScope.LOCAL,
                            params);
                }

                return group;
            }
        });

        if (logger.isDebugEnabled()) {
            logger.debug("Created affinity group =" + affinityGroupName);
        }

        return group;
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_AFFINITY_GROUP_DELETE, eventDescription = "Deleting affinity group")
    public boolean deleteAffinityGroup(Long affinityGroupId, String account, Long domainId, String affinityGroupName) {

        Account caller = CallContext.current().getCallingAccount();
        Account owner = _accountMgr.finalizeOwner(caller, account, domainId, null);

        AffinityGroupVO group = null;
        if (affinityGroupId != null) {
            group = _affinityGroupDao.findById(affinityGroupId);
            if (group == null) {
                throw new InvalidParameterValueException("Unable to find affinity group: " + affinityGroupId + "; failed to delete group.");
            }
        } else if (affinityGroupName != null) {
            group = _affinityGroupDao.findByAccountAndName(owner.getAccountId(), affinityGroupName);
            if (group == null) {
                throw new InvalidParameterValueException("Unable to find affinity group: " + affinityGroupName + "; failed to delete group.");
            }
        } else {
            throw new InvalidParameterValueException("Either the affinity group Id or group name must be specified to delete the group");
        }
        if (affinityGroupId == null) {
            affinityGroupId = group.getId();
        }
        // check permissions
        _accountMgr.checkAccess(caller, AccessType.OperateEntry, true, group);

        final Long affinityGroupIdFinal = affinityGroupId;
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {

                AffinityGroupVO group = _affinityGroupDao.lockRow(affinityGroupIdFinal, true);
                if (group == null) {
                            throw new InvalidParameterValueException("Unable to find affinity group by id " + affinityGroupIdFinal);
                }

                List<AffinityGroupVMMapVO> affinityGroupVmMap = _affinityGroupVMMapDao.listByAffinityGroup(affinityGroupIdFinal);
                if (!affinityGroupVmMap.isEmpty()) {
                    SearchBuilder<AffinityGroupVMMapVO> listByAffinityGroup = _affinityGroupVMMapDao.createSearchBuilder();
                            listByAffinityGroup.and("affinityGroupId", listByAffinityGroup.entity().getAffinityGroupId(), SearchCriteria.Op.EQ);
                    listByAffinityGroup.done();
                    SearchCriteria<AffinityGroupVMMapVO> sc = listByAffinityGroup.create();
                            sc.setParameters("affinityGroupId", affinityGroupIdFinal);

                    _affinityGroupVMMapDao.lockRows(sc, null, true);
                    _affinityGroupVMMapDao.remove(sc);
                }

                // call processor to handle the group delete
                AffinityGroupProcessor processor = getAffinityGroupProcessorForType(group.getType());
                if (processor != null) {
                    processor.handleDeleteGroup(group);
                }

                if(_affinityGroupDao.expunge(affinityGroupIdFinal)){
                    AffinityGroupDomainMapVO groupDomain = _affinityGroupDomainMapDao
                            .findByAffinityGroup(affinityGroupIdFinal);
                    if (groupDomain != null) {
                        _affinityGroupDomainMapDao.remove(groupDomain.getId());
                    }
                }
            }
        });

        // remove its related ACL permission
        Pair<Class<?>, Long> params = new Pair<Class<?>, Long>(AffinityGroup.class, affinityGroupIdFinal);
        _messageBus.publish(_name, EntityManager.MESSAGE_REMOVE_ENTITY_EVENT, PublishScope.LOCAL, params);

        if (logger.isDebugEnabled()) {
            logger.debug("Deleted affinity group id=" + affinityGroupId);
        }
        return true;
    }

    @Override
    public Pair<List<? extends AffinityGroup>, Integer> listAffinityGroups(Long affinityGroupId, String affinityGroupName, String affinityGroupType, Long vmId,
        Long startIndex, Long pageSize) {
        Filter searchFilter = new Filter(AffinityGroupVO.class, "id", Boolean.TRUE, startIndex, pageSize);

        Account caller = CallContext.current().getCallingAccount();

        Long accountId = caller.getAccountId();
        Long domainId = caller.getDomainId();

        SearchBuilder<AffinityGroupVMMapVO> vmInstanceSearch = _affinityGroupVMMapDao.createSearchBuilder();
        vmInstanceSearch.and("instanceId", vmInstanceSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);

        SearchBuilder<AffinityGroupVO> groupSearch = _affinityGroupDao.createSearchBuilder();

        SearchCriteria<AffinityGroupVO> sc = groupSearch.create();

        if (accountId != null) {
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        }

        if (domainId != null) {
            sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        }

        if (affinityGroupId != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, affinityGroupId);
        }

        if (affinityGroupName != null) {
            sc.addAnd("name", SearchCriteria.Op.EQ, affinityGroupName);
        }

        if (affinityGroupType != null) {
            sc.addAnd("type", SearchCriteria.Op.EQ, affinityGroupType);
        }

        if (vmId != null) {
            UserVmVO userVM = _userVmDao.findById(vmId);
            if (userVM == null) {
                throw new InvalidParameterValueException("Unable to list affinity groups for virtual machine instance " + vmId + "; instance not found.");
            }
            _accountMgr.checkAccess(caller, null, true, userVM);
            // add join to affinity_groups_vm_map
            groupSearch.join("vmInstanceSearch", vmInstanceSearch, groupSearch.entity().getId(), vmInstanceSearch.entity().getAffinityGroupId(),
                JoinBuilder.JoinType.INNER);
            sc.setJoinParameters("vmInstanceSearch", "instanceId", vmId);
        }

        Pair<List<AffinityGroupVO>, Integer> result =  _affinityGroupDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends AffinityGroup>, Integer>(result.first(), result.second());
    }

    @Override
    public List<String> listAffinityGroupTypes() {
        List<String> types = new ArrayList<String>();

        for (AffinityGroupProcessor processor : _affinityProcessors) {
                if (processor.isAdminControlledGroup()) {
                    continue; // we dont list the type if this group can be
                              // created only as an admin/system operation.
                }
                types.add(processor.getType());
            }

        return types;
    }

    protected Map<String, AffinityGroupProcessor> getAffinityTypeToProcessorMap() {
        Map<String, AffinityGroupProcessor> typeProcessorMap = new HashMap<String, AffinityGroupProcessor>();

        for (AffinityGroupProcessor processor : _affinityProcessors) {
            typeProcessorMap.put(processor.getType(), processor);
        }

        return typeProcessorMap;
    }

    @Override
    public boolean isAdminControlledGroup(AffinityGroup group) {

        if (group != null) {
            String affinityGroupType = group.getType();
            Map<String, AffinityGroupProcessor> typeProcessorMap = getAffinityTypeToProcessorMap();
            if (typeProcessorMap != null && !typeProcessorMap.isEmpty()) {
                AffinityGroupProcessor processor = typeProcessorMap.get(affinityGroupType);
                if (processor != null) {
                    return processor.isAdminControlledGroup();
                }
            }
        }
        return false;

    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _name = name;
        VirtualMachine.State.getStateMachine().registerListener(this);
        return true;
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

    @Override
    public AffinityGroup getAffinityGroup(Long groupId) {
        return _affinityGroupDao.findById(groupId);
    }

    @Override
    public boolean preStateTransitionEvent(State oldState, Event event, State newState, VirtualMachine vo, boolean status, Object opaque) {
        return true;
    }

    @Override
    public boolean postStateTransitionEvent(StateMachine2.Transition<State, Event> transition, VirtualMachine vo, boolean status, Object opaque) {
      if (!status) {
        return false;
      }
      State newState = transition.getToState();
      if ((newState == State.Expunging) || (newState == State.Error)) {
        // cleanup all affinity groups associations of the Expunged VM
        SearchCriteria<AffinityGroupVMMapVO> sc = _affinityGroupVMMapDao.createSearchCriteria();
        sc.addAnd("instanceId", SearchCriteria.Op.EQ, vo.getId());
        _affinityGroupVMMapDao.expunge(sc);
      }
      return true;
    }

  @Override
    public UserVm updateVMAffinityGroups(Long vmId, List<Long> affinityGroupIds) {
        // Verify input parameters
        UserVmVO vmInstance = _userVmDao.findById(vmId);
        if (vmInstance == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine with id " + vmId);
        }

        // Check that the VM is stopped
        if (!vmInstance.getState().equals(State.Stopped)) {
            logger.warn("Unable to update affinity groups of the virtual machine " + vmInstance.toString() + " in state " + vmInstance.getState());
            throw new InvalidParameterValueException("Unable update affinity groups of the virtual machine " + vmInstance.toString() + " " + "in state " +
                vmInstance.getState() + "; make sure the virtual machine is stopped and not in an error state before updating.");
        }

        Account caller = CallContext.current().getCallingAccount();
        Account owner = _accountMgr.getAccount(vmInstance.getAccountId());

        // check that the affinity groups exist
        for (Long affinityGroupId : affinityGroupIds) {
            AffinityGroupVO ag = _affinityGroupDao.findById(affinityGroupId);
            if (ag == null) {
                throw new InvalidParameterValueException("Unable to find affinity group by id " + affinityGroupId);
            } else {
                // verify permissions
                _accountMgr.checkAccess(caller, null, true, owner, ag);
                // Root admin has access to both VM and AG by default, but make sure the
                // owner of these entities is same
                if (caller.getId() == Account.ACCOUNT_ID_SYSTEM || _accountMgr.isRootAdmin(caller.getId())) {
                    if (ag.getAccountId() != owner.getAccountId()) {
                        throw new PermissionDeniedException("Affinity Group " + ag + " does not belong to the VM's account");
                    }
                }
            }
        }
        _affinityGroupVMMapDao.updateMap(vmId, affinityGroupIds);
        if (logger.isDebugEnabled()) {
            logger.debug("Updated VM :" + vmId + " affinity groups to =" + affinityGroupIds);
        }
        // APIResponseHelper will pull out the updated affinitygroups.
        return vmInstance;

    }

    @Override
    public boolean isAffinityGroupProcessorAvailable(String affinityGroupType) {
        for (AffinityGroupProcessor processor : _affinityProcessors) {
            if (affinityGroupType != null && affinityGroupType.equals(processor.getType())) {
                return true;
            }
        }
        return false;
    }

    private AffinityGroupProcessor getAffinityGroupProcessorForType(String affinityGroupType) {
        for (AffinityGroupProcessor processor : _affinityProcessors) {
            if (affinityGroupType != null && affinityGroupType.equals(processor.getType())) {
                return processor;
            }
        }
        return null;
    }

    @Override
    public boolean isAffinityGroupAvailableInDomain(long affinityGroupId, long domainId) {
        Long groupDomainId = null;

        AffinityGroupDomainMapVO domainMap = _affinityGroupDomainMapDao.findByAffinityGroup(affinityGroupId);
        if (domainMap == null) {
            return false;
        } else {
            groupDomainId = domainMap.getDomainId();
        }

        if (domainId == groupDomainId.longValue()) {
            return true;
        }

        if (domainMap.subdomainAccess) {
            Set<Long> parentDomains = _domainMgr.getDomainParentIds(domainId);
            if (parentDomains.contains(groupDomainId)) {
                return true;
            }
        }

        return false;
    }

}
