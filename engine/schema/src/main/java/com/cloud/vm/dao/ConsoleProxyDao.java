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
import java.util.List;

import com.cloud.info.ConsoleProxyLoadInfo;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.VirtualMachine.State;

public interface ConsoleProxyDao extends GenericDao<ConsoleProxyVO, Long> {

    public void update(long id, int activeSession, Date updateTime, byte[] sessionDetails);

    public List<ConsoleProxyVO> getProxyListInStates(long dataCenterId, State... states);

    public List<ConsoleProxyVO> getProxyListInStates(State... states);

    public List<ConsoleProxyVO> listByHostId(long hostId);

    public List<ConsoleProxyVO> listByLastHostId(long hostId);

    public List<ConsoleProxyVO> listUpByHostId(long hostId);

    public List<ConsoleProxyLoadInfo> getDatacenterProxyLoadMatrix();

    public List<ConsoleProxyLoadInfo> getDatacenterVMLoadMatrix();

    public List<ConsoleProxyLoadInfo> getDatacenterSessionLoadMatrix();

    public List<Pair<Long, Integer>> getDatacenterStoragePoolHostInfo(long dcId, boolean countAllPoolTypes);

    public List<Pair<Long, Integer>> getProxyLoadMatrix();

    public int getProxyStaticLoad(long proxyVmId);

    public int getProxyActiveLoad(long proxyVmId);

    public List<Long> getRunningProxyListByMsid(long msid);
}
