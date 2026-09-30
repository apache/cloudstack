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

package org.apache.cloudstack.framework.jobs.impl;

import java.lang.reflect.Field;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import com.cloud.network.Network;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.storage.Volume;
import com.cloud.utils.db.DbProperties;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.VMInstanceDao;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AsyncJobManagerImplTest {

    // db.cloud.maxActive value preloaded into DbProperties for every test in this class.
    // Chosen so the db-derived defaults (4000/2 = 2000 for api, 4000*2/3 = 2666 for worker)
    // are non-trivial and easy to reason about vs. the configured minimums under test.
    private static final int DB_CLOUD_MAX_ACTIVE = 4000;

    @Spy
    @InjectMocks
    AsyncJobManagerImpl asyncJobManager;
    @Mock
    VolumeDataFactory volFactory;
    @Mock
    VMInstanceDao vmInstanceDao;
    @Mock
    VirtualMachineManager virtualMachineManager;
    @Mock
    NetworkDao networkDao;
    @Mock
    NetworkOrchestrationService networkOrchestrationService;

    @Before
    public void preloadDbProperties() throws Exception {
        Properties props = new Properties();
        props.setProperty("db.cloud.maxActive", String.valueOf(DB_CLOUD_MAX_ACTIVE));
        // Bypass DbProperties' internal "loaded" guard so configure() reads our
        // properties instead of trying to load a real db.properties file.
        setDbPropertiesStatics(props, true);
    }

    @After
    public void tearDown() throws Exception {
        try {
            shutdownIfPresent("_apiJobExecutor");
            shutdownIfPresent("_workerJobExecutor");
        } finally {
            // Reset static state even if executor shutdown throws, so we do not
            // pollute later test classes with our stubbed ConfigKey values or
            // preloaded DbProperties.
            resetConfigValue(AsyncJobManagerImpl.ApiJobPoolSize);
            resetConfigValue(AsyncJobManagerImpl.WorkJobPoolSize);
            setDbPropertiesStatics(new Properties(), false);
        }
    }

    @Test
    public void testCleanupVolumeResource() {
        AsyncJobVO job = new AsyncJobVO();
        job.setInstanceType(ApiCommandResourceType.Volume.toString());
        job.setInstanceId(1L);
        VolumeInfo volumeInfo = Mockito.mock(VolumeInfo.class);
        when(volFactory.getVolume(Mockito.anyLong())).thenReturn(volumeInfo);
        when(volumeInfo.getState()).thenReturn(Volume.State.Attaching);
        asyncJobManager.cleanupResources(job);
        Mockito.verify(volumeInfo, Mockito.times(1)).stateTransit(Volume.Event.OperationFailed);
    }

    @Test
    public void testCleanupVmResource() throws NoTransitionException {
        AsyncJobVO job = new AsyncJobVO();
        job.setInstanceType(ApiCommandResourceType.VirtualMachine.toString());
        job.setInstanceId(1L);
        VMInstanceVO vmInstanceVO = Mockito.mock(VMInstanceVO.class);
        when(vmInstanceDao.findById(Mockito.anyLong())).thenReturn(vmInstanceVO);
        when(vmInstanceVO.getState()).thenReturn(VirtualMachine.State.Starting);
        when(vmInstanceVO.getHostId()).thenReturn(1L);
        asyncJobManager.cleanupResources(job);
        Mockito.verify(virtualMachineManager, Mockito.times(1)).stateTransitTo(vmInstanceVO, VirtualMachine.Event.OperationFailed, 1L);
    }

    @Test
    public void testCleanupNetworkResource() throws NoTransitionException {
        AsyncJobVO job = new AsyncJobVO();
        job.setInstanceType(ApiCommandResourceType.Network.toString());
        job.setInstanceId(1L);
        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        when(networkDao.findById(Mockito.anyLong())).thenReturn(networkVO);
        when(networkVO.getState()).thenReturn(Network.State.Implementing);
        asyncJobManager.cleanupResources(job);
        Mockito.verify(networkOrchestrationService, Mockito.times(1)).stateTransitTo(networkVO,
                Network.Event.OperationFailed);
    }

    @Test
    public void configureApiPoolUsesConfiguredValueWhenLargerThanDbDerived() throws Exception {
        overrideConfigValue(AsyncJobManagerImpl.ApiJobPoolSize, 3000);
        overrideConfigValue(AsyncJobManagerImpl.WorkJobPoolSize, 50);
        invokeConfigureAndSwallowPostSetupNpe();
        // apiPoolSize = max(3000, 4000/2) = 3000 (configured wins)
        Assert.assertEquals(3000, apiPoolCoreSize());
    }

    @Test
    public void configureApiPoolUsesDbDerivedWhenLargerThanConfigured() throws Exception {
        overrideConfigValue(AsyncJobManagerImpl.ApiJobPoolSize, 50);
        overrideConfigValue(AsyncJobManagerImpl.WorkJobPoolSize, 50);
        invokeConfigureAndSwallowPostSetupNpe();
        // apiPoolSize = max(50, 4000/2) = 2000 (db-derived wins)
        Assert.assertEquals(DB_CLOUD_MAX_ACTIVE / 2, apiPoolCoreSize());
    }

    @Test
    public void configureWorkerPoolUsesTwoThirdsFormula() throws Exception {
        overrideConfigValue(AsyncJobManagerImpl.ApiJobPoolSize, 50);
        overrideConfigValue(AsyncJobManagerImpl.WorkJobPoolSize, 50);
        invokeConfigureAndSwallowPostSetupNpe();
        // workPoolSize = max(50, 4000*2/3) = 2666 (db-derived wins)
        Assert.assertEquals((DB_CLOUD_MAX_ACTIVE * 2) / 3, workerPoolCoreSize());
    }

    @Test
    public void configureWorkerPoolUsesConfiguredValueWhenLargerThanDbDerived() throws Exception {
        overrideConfigValue(AsyncJobManagerImpl.ApiJobPoolSize, 50);
        overrideConfigValue(AsyncJobManagerImpl.WorkJobPoolSize, 5000);
        invokeConfigureAndSwallowPostSetupNpe();
        // workPoolSize = max(5000, 4000*2/3) = 5000 (configured wins)
        Assert.assertEquals(5000, workerPoolCoreSize());
    }

    /**
     * Invoke configure() and swallow the NPE it throws when wiring SearchBuilders
     * on the un-mocked DAOs after the pool-sizing block. The executors are created
     * inside configure()'s try/catch (before the DAO wiring), so pool sizes can
     * still be inspected after the failure.
     */
    private void invokeConfigureAndSwallowPostSetupNpe() {
        try {
            asyncJobManager.configure(null, null);
        } catch (Exception ignored) {
            // expected: DAO wiring after pool-sizing NPEs because we intentionally
            // did not mock the DAOs (they are not part of what we are testing).
        }
    }

    private int apiPoolCoreSize() throws Exception {
        Object executor = readField("_apiJobExecutor");
        Assert.assertNotNull("configure() did not create _apiJobExecutor - has DAO wiring been moved before pool sizing?", executor);
        return ((ThreadPoolExecutor) executor).getCorePoolSize();
    }

    private int workerPoolCoreSize() throws Exception {
        Object executor = readField("_workerJobExecutor");
        Assert.assertNotNull("configure() did not create _workerJobExecutor - has DAO wiring been moved before pool sizing?", executor);
        return ((ThreadPoolExecutor) executor).getCorePoolSize();
    }

    private Object readField(String name) throws Exception {
        Field f = AsyncJobManagerImpl.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(asyncJobManager);
    }

    private void shutdownIfPresent(String executorFieldName) {
        try {
            Object executor = readField(executorFieldName);
            if (executor instanceof ExecutorService) {
                ((ExecutorService) executor).shutdownNow();
            }
        } catch (Exception ignored) {
            // never populated by this test - nothing to clean up
        }
    }

    private void overrideConfigValue(final ConfigKey<?> configKey, final Object value) {
        try {
            Field f = ConfigKey.class.getDeclaredField("_value");
            f.setAccessible(true);
            f.set(configKey, value);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            Assert.fail(e.getMessage());
        }
    }

    private void resetConfigValue(final ConfigKey<?> configKey) throws Exception {
        Field f = ConfigKey.class.getDeclaredField("_value");
        f.setAccessible(true);
        f.set(configKey, null);
    }

    private void setDbPropertiesStatics(Properties props, boolean loaded) throws Exception {
        Field propsField = DbProperties.class.getDeclaredField("properties");
        Field loadedField = DbProperties.class.getDeclaredField("loaded");
        propsField.setAccessible(true);
        loadedField.setAccessible(true);
        propsField.set(null, props);
        loadedField.set(null, loaded);
    }
}
