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
package com.cloud.vm.dao;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;

public interface UserVmDao extends GenericDao<UserVmVO, Long> {
    List<UserVmVO> listByAccountId(long id);

    List<UserVmVO> listByAccountAndPod(long accountId, long podId);

    List<UserVmVO> listByAccountAndDataCenter(long accountId, long dcId);

    List<UserVmVO> listByHostId(Long hostId);

    List<UserVmVO> listByLastHostId(Long hostId);

    List<UserVmVO> listUpByHostId(Long hostId);

    /**
     * Updates display name and group for vm; enables/disables ha
     * @param id vm id.
     * @param userData updates the userData of the vm
     * @param displayVm updates the displayvm attribute signifying whether it has to be displayed to the end user or not.
     * @param customId
     * @param hostName TODO
     * @param instanceName
     */
    void updateVM(long id, String displayName, boolean enable, Long osTypeId, String userData, boolean displayVm, boolean isDynamicallyScalable, String customId, String hostName, String instanceName);

    List<UserVmVO> findDestroyedVms(Date date);

    /**
     * List running VMs on the specified host
     * @param id
     * @return
     */
    public List<UserVmVO> listRunningByHostId(long hostId);

    /**
     * List user vm instances with virtualized networking (i.e. not direct attached networking) for the given account and datacenter
     * @param accountId will search for vm instances belonging to this account
     * @return the list of vm instances owned by the account in the given data center that have virtualized networking (not direct attached networking)
     */
    List<UserVmVO> listVirtualNetworkInstancesByAcctAndNetwork(long accountId, long networkId);

    List<UserVmVO> listByNetworkIdAndStates(long networkId, State... states);

    List<UserVmVO> listByAccountIdAndHostId(long accountId, long hostId);

    void loadDetails(UserVmVO vm);

    void saveDetails(UserVmVO vm);

    List<Long> listPodIdsHavingVmsforAccount(long zoneId, long accountId);

    public Long countAllocatedVMsForAccount(long accountId);

    Hashtable<Long, UserVmData> listVmDetails(Hashtable<Long, UserVmData> userVmData);

    List<UserVmVO> listByIsoId(Long isoId);

    List<Pair<Pair<String, VirtualMachine.Type>, Pair<Long, String>>> getVmsDetailByNames(Set<String> vmNames, String detail);
}
