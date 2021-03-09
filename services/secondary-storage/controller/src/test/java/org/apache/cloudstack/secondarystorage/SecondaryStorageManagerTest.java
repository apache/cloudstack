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

package org.apache.cloudstack.secondarystorage;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineProfile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.when;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Mockito.eq;

public class SecondaryStorageManagerTest {
    @Mock
    DataCenterDao _dcDao;

    @Mock
    NetworkDao _networkDao;

    @InjectMocks
    SecondaryStorageManagerImpl _ssMgr = new SecondaryStorageManagerImpl();

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getDefaultNetworkForAdvancedNonSG() {
        DataCenterVO dc = mock(DataCenterVO.class);
        when(dc.getNetworkType()).thenReturn(NetworkType.Advanced);
        when(dc.isSecurityGroupEnabled()).thenReturn(false);

        when(_dcDao.findById(Mockito.anyLong())).thenReturn(dc);

        NetworkVO network = Mockito.mock(NetworkVO.class);
        NetworkVO badNetwork = Mockito.mock(NetworkVO.class);
        when(_networkDao.listByZoneAndTrafficType(anyLong(), eq(TrafficType.Public)))
                    .thenReturn(Collections.singletonList(network));

        when(_networkDao.listByZoneAndTrafficType(anyLong(), not(eq(TrafficType.Public))))
        .thenReturn(Collections.singletonList(badNetwork));

        when(_networkDao.listByZoneSecurityGroup(anyLong()))
                    .thenReturn(Collections.singletonList(badNetwork));

        NetworkVO returnedNetwork = _ssMgr.getDefaultNetworkForAdvancedZone(dc);

        Assert.assertNotNull(returnedNetwork);
        Assert.assertEquals(network, returnedNetwork);
        Assert.assertNotEquals(badNetwork, returnedNetwork);
    }

    @Test
    public void getDefaultNetworkForAdvancedSG() {
        DataCenterVO dc = Mockito.mock(DataCenterVO.class);
        when(dc.getNetworkType()).thenReturn(NetworkType.Advanced);
        when(dc.isSecurityGroupEnabled()).thenReturn(true);

        when(_dcDao.findById(Mockito.anyLong())).thenReturn(dc);

        NetworkVO network = Mockito.mock(NetworkVO.class);
        NetworkVO badNetwork = Mockito.mock(NetworkVO.class);
        when(_networkDao.listByZoneAndTrafficType(anyLong(), any(TrafficType.class)))
                    .thenReturn(Collections.singletonList(badNetwork));

        when(_networkDao.listByZoneSecurityGroup(anyLong()))
                    .thenReturn(Collections.singletonList(network));

        NetworkVO returnedNetwork = _ssMgr.getDefaultNetworkForAdvancedZone(dc);

        Assert.assertEquals(network, returnedNetwork);
        Assert.assertNotEquals(badNetwork, returnedNetwork);
    }

    @Test
    public void getDefaultNetworkForBasicNonSG() {
        DataCenterVO dc = Mockito.mock(DataCenterVO.class);
        when(dc.getNetworkType()).thenReturn(NetworkType.Basic);
        when(dc.isSecurityGroupEnabled()).thenReturn(false);

        when(_dcDao.findById(Mockito.anyLong())).thenReturn(dc);

        NetworkVO network = Mockito.mock(NetworkVO.class);
        NetworkVO badNetwork = Mockito.mock(NetworkVO.class);
        when(_networkDao.listByZoneAndTrafficType(anyLong(), eq(TrafficType.Guest)))
                    .thenReturn(Collections.singletonList(network));

        when(_networkDao.listByZoneAndTrafficType(anyLong(), not(eq(TrafficType.Guest))))
                    .thenReturn(Collections.singletonList(badNetwork));

        NetworkVO returnedNetwork = _ssMgr.getDefaultNetworkForBasicZone(dc);

        Assert.assertNotNull(returnedNetwork);
        Assert.assertEquals(network, returnedNetwork);
        Assert.assertNotEquals(badNetwork, returnedNetwork);
    }

    @Test
    public void getDefaultNetworkForBasicSG() {
        DataCenterVO dc = Mockito.mock(DataCenterVO.class);
        when(dc.getNetworkType()).thenReturn(NetworkType.Basic);
        when(dc.isSecurityGroupEnabled()).thenReturn(true);

        when(_dcDao.findById(Mockito.anyLong())).thenReturn(dc);

        NetworkVO network = Mockito.mock(NetworkVO.class);
        NetworkVO badNetwork = Mockito.mock(NetworkVO.class);
        when(_networkDao.listByZoneAndTrafficType(anyLong(), eq(TrafficType.Guest)))
                    .thenReturn(Collections.singletonList(network));

        when(_networkDao.listByZoneAndTrafficType(anyLong(), not(eq(TrafficType.Guest))))
                    .thenReturn(Collections.singletonList(badNetwork));

        NetworkVO returnedNetwork = _ssMgr.getDefaultNetworkForBasicZone(dc);

        Assert.assertNotNull(returnedNetwork);
        Assert.assertEquals(network, returnedNetwork);
        Assert.assertNotEquals(badNetwork, returnedNetwork);
    }

    //also test invalid input
    @Test(expected=CloudRuntimeException.class)
    public void getDefaultNetworkForBasicSGWrongZoneType() {
        DataCenterVO dc = Mockito.mock(DataCenterVO.class);
        when(dc.getNetworkType()).thenReturn(NetworkType.Advanced);
        when(dc.isSecurityGroupEnabled()).thenReturn(true);

        when(_dcDao.findById(Mockito.anyLong())).thenReturn(dc);

        NetworkVO network = Mockito.mock(NetworkVO.class);
        NetworkVO badNetwork = Mockito.mock(NetworkVO.class);
        when(_networkDao.listByZoneAndTrafficType(anyLong(), eq(TrafficType.Guest)))
                    .thenReturn(Collections.singletonList(network));

        when(_networkDao.listByZoneAndTrafficType(anyLong(), not(eq(TrafficType.Guest))))
                    .thenReturn(Collections.singletonList(badNetwork));

        _ssMgr.getDefaultNetworkForBasicZone(dc);
    }

    @Test(expected=CloudRuntimeException.class)
    public void getDefaultNetworkForAdvancedWrongZoneType() {
        DataCenterVO dc = Mockito.mock(DataCenterVO.class);
        when(dc.getNetworkType()).thenReturn(NetworkType.Basic);
        when(dc.isSecurityGroupEnabled()).thenReturn(true);

        when(_dcDao.findById(Mockito.anyLong())).thenReturn(dc);

        NetworkVO network = Mockito.mock(NetworkVO.class);
        NetworkVO badNetwork = Mockito.mock(NetworkVO.class);
        when(_networkDao.listByZoneAndTrafficType(anyLong(), any(TrafficType.class)))
                    .thenReturn(Collections.singletonList(badNetwork));

        when(_networkDao.listByZoneSecurityGroup(anyLong()))
                    .thenReturn(Collections.singletonList(network));

        _ssMgr.getDefaultNetworkForAdvancedZone(dc);
    }

    @Test
    public void validateVerifySshAccessOnManagementNicForSystemVm(){
        Hypervisor.HypervisorType[] hypervisorTypesArray = Hypervisor.HypervisorType.values();
        List<Hypervisor.HypervisorType> hypervisorTypesThatMustReturnManagementNic = new ArrayList<>(Arrays.asList(Hypervisor.HypervisorType.Hyperv));

        for (Hypervisor.HypervisorType hypervisorType: hypervisorTypesArray) {
            VirtualMachineProfile virtualMachineProfileMock = Mockito.mock(VirtualMachineProfile.class);
            NicProfile controlNic = Mockito.mock(NicProfile.class);
            NicProfile managementNic = Mockito.mock(NicProfile.class);

            Mockito.doReturn(hypervisorType).when(virtualMachineProfileMock).getHypervisorType();

            NicProfile expectedResult = controlNic;
            if (hypervisorTypesThatMustReturnManagementNic.contains(hypervisorType)) {
                expectedResult = managementNic;
            }

            NicProfile result = _ssMgr.verifySshAccessOnManagementNicForSystemVm(virtualMachineProfileMock, controlNic, managementNic);

            Assert.assertEquals(expectedResult, result);
        }
    }
}
