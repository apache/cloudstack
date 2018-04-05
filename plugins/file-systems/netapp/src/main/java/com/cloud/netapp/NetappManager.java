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
package com.cloud.netapp;

import java.net.UnknownHostException;
import java.rmi.ServerException;
import java.util.List;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.PluggableService;

public interface NetappManager extends Manager, PluggableService {
    enum AlgorithmType {
        RoundRobin, LeastFull
    }

    void destroyVolumeOnFiler(String ipAddress, String aggrName, String volName) throws ServerException, InvalidParameterValueException, ResourceInUseException;

    void createVolumeOnFiler(String ipAddress, String aggName, String poolName, String volName, String volSize, String snapshotPolicy, Integer snapshotReservation,
        String username, String password) throws UnknownHostException, ServerException, InvalidParameterValueException;

    public String[] associateLun(String guestIqn, String path) throws ServerException, InvalidParameterValueException;

    void disassociateLun(String iGroup, String path) throws ServerException, InvalidParameterValueException;

    List<LunVO> listLunsOnFiler(String poolName);

    void destroyLunOnFiler(String path) throws ServerException, InvalidParameterValueException;

    List<NetappVolumeVO> listVolumesOnFiler(String poolName);

    List<NetappVolumeVO> listVolumesAscending(String poolName);

    long returnAvailableVolumeSize(String volName, String userName, String password, String serverIp) throws ServerException;

    void createPool(String poolName, String algorithm) throws InvalidParameterValueException;

    void modifyPool(String poolName, String algorithm) throws InvalidParameterValueException;

    void deletePool(String poolName) throws InvalidParameterValueException, ResourceInUseException;

    List<PoolVO> listPools();

    public String[] createLunOnFiler(String poolName, Long lunSize) throws InvalidParameterValueException, ServerException, ResourceAllocationException;

}
