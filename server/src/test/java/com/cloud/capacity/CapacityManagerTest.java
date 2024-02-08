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

package com.cloud.capacity;

import com.cloud.capacity.dao.CapacityDao;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.vm.VirtualMachine;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static  org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CapacityManagerTest {
    CapacityDao CDao = mock(CapacityDao.class);
    ServiceOfferingDao SOfferingDao = mock(ServiceOfferingDao.class);
    ClusterDetailsDao ClusterDetailsDao = mock(com.cloud.dc.ClusterDetailsDao.class);
    CapacityManagerImpl capMgr;
    private ServiceOfferingVO svo = mock(ServiceOfferingVO.class);
    private CapacityVO cvoCpu = mock(CapacityVO.class);
    private CapacityVO cvoRam = mock(CapacityVO.class);
    private VirtualMachine vm = mock(VirtualMachine.class);
    private ClusterDetailsVO clusterDetailCpu = mock(ClusterDetailsVO.class);
    private ClusterDetailsVO clusterDetailRam = mock(ClusterDetailsVO.class);

    public CapacityManagerImpl setUp() {
        CapacityManagerImpl capMgr = new CapacityManagerImpl();
        capMgr._clusterDetailsDao = ClusterDetailsDao;
        capMgr._capacityDao = CDao;
        capMgr._offeringsDao = SOfferingDao;
        return capMgr;
    }

    @Test
    public void allocateCapacityTest() {
        capMgr = setUp();
        when(vm.getHostId()).thenReturn(1l);
        when(vm.getServiceOfferingId()).thenReturn(2l);
        when(SOfferingDao.findById(anyLong(), anyLong())).thenReturn(svo);
        when(CDao.findByHostIdType(anyLong(), eq(Capacity.CAPACITY_TYPE_CPU))).thenReturn(cvoCpu);
        when(CDao.findByHostIdType(anyLong(), eq(Capacity.CAPACITY_TYPE_MEMORY))).thenReturn(cvoRam);
        when(cvoCpu.getUsedCapacity()).thenReturn(500l);
        when(cvoCpu.getTotalCapacity()).thenReturn(2000l);
        when(cvoRam.getUsedCapacity()).thenReturn(3000l);
        when(cvoRam.getTotalCapacity()).thenReturn((long)1024 * 1024 * 1024);
        when(svo.getCpu()).thenReturn(500);
        when(svo.getRamSize()).thenReturn(512);
        when(cvoCpu.getReservedCapacity()).thenReturn(0l);
        when(cvoRam.getReservedCapacity()).thenReturn(0l);
        when(clusterDetailRam.getValue()).thenReturn("1.5");
        when(clusterDetailCpu.getValue()).thenReturn("2");
        when(CDao.update(anyLong(), isA(CapacityVO.class))).thenReturn(true);
        boolean hasCapacity = capMgr.checkIfHostHasCapacity(1l, 500, 1024 * 1024 * 1024, false, 2, 2, false);
        Assert.assertTrue(hasCapacity);

    }
}
