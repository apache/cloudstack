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
package com.cloud.server;

import com.cloud.host.DetailVO;
import com.cloud.host.HostVO;
import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.utils.Pair;
import com.cloud.utils.component.PluggableService;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;

/**
 */
public interface ManagementServer extends ManagementService, PluggableService {

    /**
     * returns the instance id of this management server.
     *
     * @return id of the management server
     */
    long getId();

    /**
     * Fetches the version of cloud stack
    */
    @Override
    String getVersion();

    /**
     * Retrieves a host by id
     *
     * @param hostId
     * @return Host
     */
    HostVO getHostBy(long hostId);

    DetailVO findDetail(long hostId, String name);

    Pair<Boolean, String> setConsoleAccessForVm(long vmId, String sessionUuid);

    String getConsoleAccessUrlRoot(long vmId);

    String getConsoleAccessAddress(long vmId);

    GuestOSVO getGuestOs(Long guestOsId);

    GuestOSHypervisorVO getGuestOsHypervisor(Long guestOsHypervisorId);

    /**
     * Returns the vnc port of the vm.
     *
     * @param VirtualMachine vm
     * @return the vnc port if found; -1 if unable to find.
     */
    Pair<String, Integer> getVncPort(VirtualMachine vm);

    public long getMemoryOrCpuCapacityByHost(Long hostId, short capacityType);

    Pair<Boolean, String> updateSystemVM(VMInstanceVO systemVM, boolean forced);

}
