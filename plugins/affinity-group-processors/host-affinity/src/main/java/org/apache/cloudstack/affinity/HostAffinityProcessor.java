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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import org.apache.commons.collections.CollectionUtils;

import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;

import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.exception.AffinityConflictException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.VMInstanceDao;

public class HostAffinityProcessor extends AffinityProcessorBase implements AffinityGroupProcessor {


    @Inject
    protected VMInstanceDao _vmInstanceDao;
    @Inject
    protected AffinityGroupDao _affinityGroupDao;
    @Inject
    protected AffinityGroupVMMapDao _affinityGroupVMMapDao;

    @Override
    public void process(VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid, List<VirtualMachine> vmList) throws AffinityConflictException {
        VirtualMachine vm = vmProfile.getVirtualMachine();
        List<AffinityGroupVMMapVO> vmGroupMappings = _affinityGroupVMMapDao.findByVmIdType(vm.getId(), getType());
        if (CollectionUtils.isNotEmpty(vmGroupMappings)) {
            List<Long> affinityGroupIdList = vmGroupMappings.stream().map(AffinityGroupVMMapVO::getAffinityGroupId).collect(Collectors.toList());
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    if (!affinityGroupIdList.isEmpty()) {
                        _affinityGroupDao.listByIds(affinityGroupIdList, true);
                    }
                    for (AffinityGroupVMMapVO vmGroupMapping : vmGroupMappings) {
                        processAffinityGroup(vmGroupMapping, plan, vm, vmList);
                    }
                }
            });
        }
    }


    /**
     * Process Affinity Group for VM deployment
     */
    protected void processAffinityGroup(AffinityGroupVMMapVO vmGroupMapping, DeploymentPlan plan, VirtualMachine vm, List<VirtualMachine> vmList) {
        AffinityGroupVO group = _affinityGroupDao.findById(vmGroupMapping.getAffinityGroupId());
        logger.debug("Processing affinity group " + group.getName() + " for VM Id: " + vm.getId());

        List<Long> groupVMIds = _affinityGroupVMMapDao.listVmIdsByAffinityGroup(group.getId());
        groupVMIds.remove(vm.getId());

        List<Long> preferredHosts = getPreferredHostsFromGroupVMIds(groupVMIds, vmList);
        plan.setPreferredHosts(preferredHosts);
    }


    /**
     * Process Affinity Group for VM deployment
     */
    protected void processAffinityGroup(AffinityGroupVMMapVO vmGroupMapping, DeploymentPlan plan, VirtualMachine vm) {
        processAffinityGroup(vmGroupMapping, plan, vm, Collections.emptyList());
    }

    /**
     * Get host ids set from vm ids list
     */

    protected Set<Long> getHostIdSet(List<Long> vmIds) {
        return getHostIdSet(vmIds, Collections.emptyList());
    }

    protected Set<Long> getHostIdSet(List<Long> vmIds, List<VirtualMachine> vmList) {
        Set<Long> hostIds = new HashSet<>();
        Map<Long, VirtualMachine> vmIdVmMap = getVmIdVmMap(vmList);
        for (Long groupVMId : vmIds) {
            VirtualMachine groupVM = vmIdVmMap.get(groupVMId);
            if (groupVM == null) {
                groupVM = _vmInstanceDao.findById(groupVMId);
            }

            if (groupVM != null && groupVM.getHostId() != null) {
                hostIds.add(groupVM.getHostId());
            }
        }
        return hostIds;
    }

    protected Map<Long, VirtualMachine> getVmIdVmMap(List<VirtualMachine> vmList) {
        Map<Long, VirtualMachine> vmIdVmMap = new HashMap<>();
        for (VirtualMachine vm : vmList) {
            vmIdVmMap.put(vm.getId(), vm);
        }
        return vmIdVmMap;
    }

    /**
     * Get preferred host ids list from the affinity group VMs
     */
    protected List<Long> getPreferredHostsFromGroupVMIds(List<Long> vmIds, List<VirtualMachine> vmList) {
        return new ArrayList<>(getHostIdSet(vmIds, vmList));
    }

    @Override
    public boolean check(VirtualMachineProfile vmProfile, DeployDestination plannedDestination) throws AffinityConflictException {
        if (plannedDestination.getHost() == null) {
            return true;
        }
        long plannedHostId = plannedDestination.getHost().getId();
        VirtualMachine vm = vmProfile.getVirtualMachine();
        List<AffinityGroupVMMapVO> vmGroupMappings = _affinityGroupVMMapDao.findByVmIdType(vm.getId(), getType());
        if (CollectionUtils.isEmpty(vmGroupMappings)) {
            return true;
        }
        List<Long> affinityGroupIds = vmGroupMappings.stream().map(AffinityGroupVMMapVO::getAffinityGroupId).collect(Collectors.toList());
        return Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                if (!affinityGroupIds.isEmpty()) {
                    _affinityGroupDao.listByIds(affinityGroupIds, true);
                }
                for (AffinityGroupVMMapVO vmGroupMapping : vmGroupMappings) {
                    if (!checkAffinityGroup(vmGroupMapping, vm, plannedHostId)) {
                        return false;
                    }

                }
                return true;
            }
        });
    }

    /**
     * Check Affinity Group
     */
    protected boolean checkAffinityGroup(AffinityGroupVMMapVO vmGroupMapping, VirtualMachine vm, long plannedHostId) {
        List<Long> groupVMIds = _affinityGroupVMMapDao.listVmIdsByAffinityGroup(vmGroupMapping.getAffinityGroupId());
        groupVMIds.remove(vm.getId());

        Set<Long> hostIds = getHostIdSet(groupVMIds);
        return CollectionUtils.isEmpty(hostIds) || hostIds.contains(plannedHostId);
    }

}
