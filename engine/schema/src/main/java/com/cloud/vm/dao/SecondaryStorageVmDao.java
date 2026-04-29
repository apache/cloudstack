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

import java.util.List;

import com.cloud.utils.db.GenericDao;
import com.cloud.vm.SecondaryStorageVm;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.VirtualMachine.State;

public interface SecondaryStorageVmDao extends GenericDao<SecondaryStorageVmVO, Long> {

    public List<SecondaryStorageVmVO> getSecStorageVmListInStates(SecondaryStorageVm.Role role, long dataCenterId, State... states);

    public List<SecondaryStorageVmVO> getSecStorageVmListInStates(SecondaryStorageVm.Role role, State... states);

    public List<SecondaryStorageVmVO> listByHostId(SecondaryStorageVm.Role role, long hostId);

    public List<SecondaryStorageVmVO> listByLastHostId(SecondaryStorageVm.Role role, long hostId);

    public List<SecondaryStorageVmVO> listUpByHostId(SecondaryStorageVm.Role role, long hostId);

    public List<SecondaryStorageVmVO> listByZoneId(SecondaryStorageVm.Role role, long zoneId);

    public List<Long> getRunningSecStorageVmListByMsid(SecondaryStorageVm.Role role, long msid);

    public List<Long> listRunningSecStorageOrderByLoad(SecondaryStorageVm.Role role, long zoneId);

    SecondaryStorageVmVO findByInstanceName(String instanceName);
}
