/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.motion;

import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;

import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.agent.api.to.DataTO;
import com.cloud.capacity.CapacityManager;
import com.cloud.hypervisor.Hypervisor.HypervisorType;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CapacityManager.class)
public class AncientDataMotionStrategyTest {

    @Spy
    @InjectMocks
    private AncientDataMotionStrategy strategy = new AncientDataMotionStrategy();

    @Mock
    DataTO dataTO;
    @Mock
    PrimaryDataStoreTO dataStoreTO;
    @Mock
    ConfigKey<Boolean> vmwareKey;
    @Mock
    StorageManager storageManager;
    @Mock
    StoragePool storagePool;

    private static final long POOL_ID = 1l;
    private static final Boolean FULL_CLONE_FLAG = true;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        replaceVmwareCreateCloneFullField();

        when(vmwareKey.valueIn(POOL_ID)).thenReturn(FULL_CLONE_FLAG);

        when(dataTO.getHypervisorType()).thenReturn(HypervisorType.VMware);
        when(dataTO.getDataStore()).thenReturn(dataStoreTO);
        when(dataStoreTO.getId()).thenReturn(POOL_ID);
        when(storageManager.getStoragePool(POOL_ID)).thenReturn(storagePool);
    }

    private void replaceVmwareCreateCloneFullField() throws Exception {
        Field field = StorageManager.class.getDeclaredField("VmwareCreateCloneFull");
        field.setAccessible(true);
        // remove final modifier from field
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, vmwareKey);
    }

    @Test
    public void testAddFullCloneFlagOnVMwareDest(){
        strategy.addFullCloneAndDiskprovisiongStrictnessFlagOnVMwareDest(dataTO);
        verify(dataStoreTO).setFullCloneFlag(FULL_CLONE_FLAG);
    }

    @Test
    public void testAddFullCloneFlagOnNotVmwareDest(){
        when(dataTO.getHypervisorType()).thenReturn(HypervisorType.Any);
        verify(dataStoreTO, never()).setFullCloneFlag(any(Boolean.class));
    }

}
