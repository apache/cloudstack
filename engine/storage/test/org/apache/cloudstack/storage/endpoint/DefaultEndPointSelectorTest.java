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
package org.apache.cloudstack.storage.endpoint;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageAction;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.DummyEndpoint;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.host.dao.HostDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.vm.SecondaryStorageVm;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.dao.SecondaryStorageVmDao;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ComponentContext.class)
public class DefaultEndPointSelectorTest {
    @Mock
    HostDao _hostDao;

    @Mock
    SecondaryStorageVmDao _ssvmDao;

    @InjectMocks
    @Spy
    DefaultEndPointSelector selector = new DefaultEndPointSelector();

    //Special mocked endpoints that the tests can use to identify what was returned from the method.
    private EndPoint selectMethodEndpoint = mock(EndPoint.class);
    private DummyEndpoint dummyEndpoint = mock(DummyEndpoint.class);

    /**
     * The single common method that sets up the mocks to cover all possible scenarios we want to
     * test. The VM type and volume state are passed in, while a potential list of existing SSVMs
     * are also passed in. As its final action, the method calls {@link DefaultEndPointSelector#select(DataObject, StorageAction)}
     * in order to test the method's operation and returns the endpoint found.
     *
     * @param vmType - State of the VM attached to the volume
     * @param volumeState - State of the volume
     * @param ssvmVOs - A list of SSVMs to return from the DAO
     * @return The selected endpoint.
     */
    private EndPoint mockEndpointSelection(Type vmType, State volumeState, SecondaryStorageVmVO ... ssvmVOs) {
        VolumeInfo volume = mock(VolumeInfo.class);
        VirtualMachine vm = mock(VirtualMachine.class);
        Long zoneId = 1l;

        when(vm.getType()).thenReturn(vmType);
        when(vm.getState()).thenReturn(volumeState);
        when(volume.getAttachedVM()).thenReturn(vm);
        when(volume.getDataCenterId()).thenReturn(zoneId);

        List<SecondaryStorageVmVO> ssvmList = new ArrayList<>();
        if (ssvmVOs != null) {
            for (SecondaryStorageVmVO ssvm : ssvmVOs) {
                ssvmList.add(ssvm);
            }
        }

        when(_ssvmDao.listByZoneId(eq(SecondaryStorageVm.Role.templateProcessor), eq(zoneId)))
            .thenReturn(ssvmList);

        PowerMockito.mockStatic(ComponentContext.class);
        when(ComponentContext.inject(DummyEndpoint.class)).thenReturn(dummyEndpoint);

        doReturn(selectMethodEndpoint).when(selector).select(any(DataObject.class));

        return selector.select(volume, StorageAction.DELETEVOLUME);
    }

    private EndPoint executeTestSelectionWithExpunging(Type vmType, Object expectedEndPoint, SecondaryStorageVmVO ... ssvmVOs) {
        EndPoint ep = mockEndpointSelection(vmType, State.Expunging, ssvmVOs);
        Assert.assertNotNull(ep);
        Assert.assertEquals(expectedEndPoint, ep);
        return ep;
    }

    /**
     * Dummy endpoint should be selected if an SSVM volume is expunging
     * and no SSVMs are available.
     */
    @Test
    public void testSelectionWithExpungingSsvm() {
        EndPoint ep = executeTestSelectionWithExpunging(Type.SecondaryStorageVm, dummyEndpoint);
        Assert.assertTrue(ep instanceof DummyEndpoint);
    }

    /**
     * Dummy endpoint should be selected if a console proxy volume is expunging
     * and no SSVMs are available.
     */
    @Test
    public void testSelectionWithExpungingConsoleProxy() {
        EndPoint ep = executeTestSelectionWithExpunging(Type.ConsoleProxy, dummyEndpoint);
        Assert.assertTrue(ep instanceof DummyEndpoint);
    }

    /**
     * The endpoint returned by {@link DefaultEndPointSelector#select(DataObject)} should be
     * returned if we pass in an expunging User VM.
     */
    @Test
    public void testSelectionWithExpungingUserVM() {
        EndPoint ep = executeTestSelectionWithExpunging(Type.User, selectMethodEndpoint);
    }

    /**
     * The endpoint returned by {@link DefaultEndPointSelector#select(DataObject)} should be
     * returned if we pass in an expunging system VM while an SSVM exists in the zone.
     */
    @Test
    public void testSelectionWithExpungingSsvmWithSsvmExisting() {
        SecondaryStorageVmVO ssvmVO = mock(SecondaryStorageVmVO.class);
        executeTestSelectionWithExpunging(Type.SecondaryStorageVm, selectMethodEndpoint, ssvmVO);
    }

    /**
     * The endpoint returned by {@link DefaultEndPointSelector#select(DataObject)} should be
     * returned if we pass in an expunging system VM while an SSVM exists in the zone.
     */
    @Test
    public void testSelectionWithExpungingConsoleProxyWithSsvmExisting() {
        SecondaryStorageVmVO ssvmVO = mock(SecondaryStorageVmVO.class);
        executeTestSelectionWithExpunging(Type.ConsoleProxy, selectMethodEndpoint, ssvmVO);
    }

}
