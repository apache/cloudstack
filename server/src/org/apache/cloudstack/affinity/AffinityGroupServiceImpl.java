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
import java.util.Map.Entry;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.Primary;



import com.cloud.deploy.DeploymentPlanner;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.network.security.SecurityGroup;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.StateListener;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmDao;

@Local(value = { AffinityGroupService.class })
public class AffinityGroupServiceImpl extends ManagerBase implements AffinityGroupService, Manager,
        StateListener<State, VirtualMachine.Event, VirtualMachine> {

    public static final Logger s_logger = Logger.getLogger(AffinityGroupServiceImpl.class);
    private String _name;

    @Inject
    AccountManager _accountMgr;

    @Inject
    AffinityGroupDao _affinityGroupDao;

    @Inject
    AffinityGroupVMMapDao _affinityGroupVMMapDao;

    @Inject
    private UserVmDao _userVmDao;

    protected List<AffinityGroupProcessor> _affinityProcessors;

    public List<AffinityGroupProcessor> getAffinityGroupProcessors() {
        return _affinityProcessors;
    }

    public void setAffinityGroupProcessors(List<AffinityGroupProcessor> affinityProcessors) {
        this._affinityProcessors = affinityProcessors;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_AFFINITY_GROUP_CREATE, eventDescription = "Creating Affinity Group", create = true)
    public AffinityGroup createAffinityGroup(String account, Long domainId, String affinityGroupName,
            String affinityGroupType, String description) {

        Account caller = CallContext.current().getCallingAccount();
        Account owner = _accountMgr.finalizeOwner(caller, account, domainId, null);

        if (_affinityGroupDao.isNameInUse(owner.getAccountId(), owner.getDomainId(), affinityGroupName)) {
            throw new InvalidParameterValueException("Unable to create affinity group, a group with name "
                    + affinityGroupName
                    + " already exisits.");
        }


        //validate the affinityGroupType
        Map<String, AffinityGroupProcessor> typeProcessorMap = getAffinityTypeToProcessorMap();
        if (typeProcessorMap != null && !typeProcessorMap.isEmpty()) {
            if (!typeProcessorMap.containsKey(affinityGroupType)) {
                throw new InvalidParameterValueException("Unable to create affinity group, invalid affinity group type"
                        + affinityGroupType);
            }
        } else {
            throw new InvalidParameterValueException(
                    "Unable to create affinity group, no Affinity Group Types configured");
        }

        if (domainId == null) {
            domainId = owner.getDomainId();
        }

        AffinityGroupVO group = new AffinityGroupVO(affinityGroupName, affinityGroupType, description, domainId,
                owner.getId());
        _affinityGroupDao.persist(group);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Created affinity group =" + affinityGroupName);
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
                throw new InvalidParameterValueException("Unable to find affinity group: " + affinityGroupId
                        + "; failed to delete group.");
            }
        } else if (affinityGroupName != null) {
            group = _affinityGroupDao.findByAccountAndName(owner.getAccountId(), affinityGroupName);
            if (group == null) {
                throw new InvalidParameterValueException("Unable to find affinity group: " + affinityGroupName
                        + "; failed to delete group.");
            }
        } else {
            throw new InvalidParameterValueException(
                    "Either the affinity group Id or group name must be specified to delete the group");
        }
        if (affinityGroupId == null) {
            affinityGroupId = group.getId();
        }
        // check permissions
        _accountMgr.checkAccess(caller, null, true, group);

        final Transaction txn = Transaction.currentTxn();
        txn.start();

        group = _affinityGroupDao.lockRow(affinityGroupId, true);
        if (group == null) {
            throw new InvalidParameterValueException("Unable to find affinity group by id " + affinityGroupId);
        }

        List<AffinityGroupVMMapVO> affinityGroupVmMap = _affinityGroupVMMapDao.listByAffinityGroup(affinityGroupId);
        if (!affinityGroupVmMap.isEmpty()) {
            SearchBuilder<AffinityGroupVMMapVO> listByAffinityGroup = _affinityGroupVMMapDao.createSearchBuilder();
            listByAffinityGroup.and("affinityGroupId", listByAffinityGroup.entity().getAffinityGroupId(),
                    SearchCriteria.Op.EQ);
            listByAffinityGroup.done();
            SearchCriteria<AffinityGroupVMMapVO> sc = listByAffinityGroup.create();
            sc.setParameters("affinityGroupId", affinityGroupId);

            _affinityGroupVMMapDao.lockRows(sc, null, true);
            _affinityGroupVMMapDao.remove(sc);
        }

        _affinityGroupDao.expunge(affinityGroupId);
        txn.commit();

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Deleted affinity group id=" + affinityGroupId);
        }
        return true;
    }

    @Override
    public Pair<List<? extends AffinityGroup>, Integer> listAffinityGroups(Long affinityGroupId, String affinityGroupName, String affinityGroupType, Long vmId, Long startIndex, Long pageSize) {
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
                throw new InvalidParameterValueException("Unable to list affinity groups for virtual machine instance "
                        + vmId + "; instance not found.");
            }
            _accountMgr.checkAccess(caller, null, true, userVM);
            // add join to affinity_groups_vm_map
            groupSearch.join("vmInstanceSearch", vmInstanceSearch, groupSearch.entity().getId(), vmInstanceSearch
                    .entity().getAffinityGroupId(), JoinBuilder.JoinType.INNER);
            sc.setJoinParameters("vmInstanceSearch", "instanceId", vmId);
        }

        Pair<List<AffinityGroupVO>, Integer> result =  _affinityGroupDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends AffinityGroup>, Integer>(result.first(), result.second());
    }


    @Override
    public List<String> listAffinityGroupTypes() {
        List<String> types = new ArrayList<String>();
        Map<String, AffinityGroupProcessor> componentMap = ComponentContext.getComponentsOfType(AffinityGroupProcessor.class);

        if (componentMap.size() > 0) {
            for (Entry<String, AffinityGroupProcessor> entry : componentMap.entrySet()) {
                types.add(entry.getValue().getType());
            }

        }
        return types;
    }

    protected Map<String, AffinityGroupProcessor> getAffinityTypeToProcessorMap() {
        Map<String, AffinityGroupProcessor> typeProcessorMap = new HashMap<String, AffinityGroupProcessor>();
        Map<String, AffinityGroupProcessor> componentMap = ComponentContext
                .getComponentsOfType(AffinityGroupProcessor.class);

        if (componentMap.size() > 0) {
            for (Entry<String, AffinityGroupProcessor> entry : componentMap.entrySet()) {
                typeProcessorMap.put(entry.getValue().getType(), entry.getValue());
            }
        }
        return typeProcessorMap;
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
    public boolean preStateTransitionEvent(State oldState, Event event, State newState, VirtualMachine vo,
            boolean status, Object opaque) {
        return true;
    }

    @Override
    public boolean postStateTransitionEvent(State oldState, Event event, State newState, VirtualMachine vo,
            boolean status, Object opaque) {
        if (!status) {
            return false;
        }
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
            s_logger.warn("Unable to update affinity groups of the virtual machine " + vmInstance.toString()
                    + " in state " + vmInstance.getState());
            throw new InvalidParameterValueException("Unable update affinity groups of the virtual machine "
                    + vmInstance.toString() + " " + "in state " + vmInstance.getState()
                    + "; make sure the virtual machine is stopped and not in an error state before updating.");
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
                if (caller.getId() == Account.ACCOUNT_ID_SYSTEM || _accountMgr.isRootAdmin(caller.getType())) {
                    if (ag.getAccountId() != owner.getAccountId()) {
                        throw new PermissionDeniedException("Affinity Group " + ag
                                + " does not belong to the VM's account");
                    }
                }
            }
        }
        _affinityGroupVMMapDao.updateMap(vmId, affinityGroupIds);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Updated VM :" + vmId + " affinity groups to =" + affinityGroupIds);
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

}
