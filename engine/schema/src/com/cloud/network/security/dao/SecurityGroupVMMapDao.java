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
package com.cloud.network.security.dao;

import java.util.List;

import com.cloud.network.security.SecurityGroupVMMapVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDao;
import com.cloud.vm.VirtualMachine.State;

public interface SecurityGroupVMMapDao extends GenericDao<SecurityGroupVMMapVO, Long> {
    List<SecurityGroupVMMapVO> listByIpAndInstanceId(String ipAddress, long instanceId);

    List<SecurityGroupVMMapVO> listByInstanceId(long instanceId);

    Pair<List<SecurityGroupVMMapVO>, Integer> listByInstanceId(long instanceId, Filter filter);

    List<SecurityGroupVMMapVO> listByIp(String ipAddress);

    List<SecurityGroupVMMapVO> listBySecurityGroup(long securityGroupId);

    List<SecurityGroupVMMapVO> listBySecurityGroup(long securityGroupId, State... vmStates);

    int deleteVM(long instanceid);

    List<Long> listVmIdsBySecurityGroup(long securityGroupId);

    SecurityGroupVMMapVO findByVmIdGroupId(long instanceId, long securityGroupId);

    long countSGForVm(long instanceId);
}
