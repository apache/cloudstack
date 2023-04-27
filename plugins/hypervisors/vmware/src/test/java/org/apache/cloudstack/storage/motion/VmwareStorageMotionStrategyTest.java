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
package org.apache.cloudstack.storage.motion;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcContext;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.test.utils.SpringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.MigrateWithStorageAnswer;
import com.cloud.agent.api.MigrateWithStorageCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class VmwareStorageMotionStrategyTest {

    @Inject
    VmwareStorageMotionStrategy strategy = new VmwareStorageMotionStrategy();
    @Inject
    AgentManager agentMgr;
    @Inject
    VolumeDao volDao;
    @Inject
    VolumeDataFactory volFactory;
    @Inject
    PrimaryDataStoreDao storagePoolDao;
    @Inject
    VMInstanceDao instanceDao;
    @Inject
    HostDao hostDao;
    @Inject
    VirtualMachineManager vmManager;

    CopyCommandResult result;

    @BeforeClass
    public static void setUp() throws ConfigurationException {
    }

    @Before
    public void testSetUp() {
        ComponentContext.initComponentsLifeCycle();
    }

    @Test
    public void testStrategyHandlesVmwareHosts() throws Exception {
        Host srcHost = mock(Host.class);
        Host destHost = mock(Host.class);
        when(srcHost.getHypervisorType()).thenReturn(HypervisorType.VMware);
        when(destHost.getHypervisorType()).thenReturn(HypervisorType.VMware);
        Map<VolumeInfo, DataStore> volumeMap = new HashMap<VolumeInfo, DataStore>();
        StrategyPriority canHandle = strategy.canHandle(volumeMap, srcHost, destHost);
        assertTrue("The strategy is only supposed to handle vmware hosts", canHandle == StrategyPriority.HYPERVISOR);
    }

    @Test
    public void testStrategyDoesnotHandlesNonVmwareHosts() throws Exception {
        Host srcHost = mock(Host.class);
        Host destHost = mock(Host.class);
        when(srcHost.getHypervisorType()).thenReturn(HypervisorType.XenServer);
        when(destHost.getHypervisorType()).thenReturn(HypervisorType.XenServer);
        Map<VolumeInfo, DataStore> volumeMap = new HashMap<VolumeInfo, DataStore>();
        StrategyPriority canHandle = strategy.canHandle(volumeMap, srcHost, destHost);
        assertFalse("The strategy is only supposed to handle vmware hosts", canHandle == StrategyPriority.HYPERVISOR);
    }

    @Test
    public void testMigrateWithinClusterSuccess() throws Exception {
        Host srcHost = mock(Host.class);
        Host destHost = mock(Host.class);
        when(srcHost.getClusterId()).thenReturn(1L);
        when(destHost.getClusterId()).thenReturn(1L);
        Map<VolumeInfo, DataStore> volumeMap = new HashMap<VolumeInfo, DataStore>();
        VirtualMachineTO to = mock(VirtualMachineTO.class);
        when(to.getId()).thenReturn(6L);
        VMInstanceVO instance = mock(VMInstanceVO.class);
        when(instanceDao.findById(6L)).thenReturn(instance);

        MockContext<CommandResult> context = new MockContext<CommandResult>(null, null, volumeMap);
        AsyncCallbackDispatcher<VmwareStorageMotionStrategyTest, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().mockCallBack(null, null)).setContext(context);

        MigrateWithStorageAnswer migAnswerMock = mock(MigrateWithStorageAnswer.class);
        when(migAnswerMock.getResult()).thenReturn(true);
        when(agentMgr.send(anyLong(), isA(MigrateWithStorageCommand.class))).thenReturn(migAnswerMock);

        strategy.copyAsync(volumeMap, to, srcHost, destHost, caller);
        assertTrue("Migration within cluster isn't successful.", result.isSuccess());
    }

    @Test
    public void testMigrateWithinClusterFailure() throws Exception {
        Host srcHost = mock(Host.class);
        Host destHost = mock(Host.class);
        when(srcHost.getClusterId()).thenReturn(1L);
        when(destHost.getClusterId()).thenReturn(1L);
        Map<VolumeInfo, DataStore> volumeMap = new HashMap<VolumeInfo, DataStore>();
        VirtualMachineTO to = mock(VirtualMachineTO.class);
        when(to.getId()).thenReturn(6L);
        VMInstanceVO instance = mock(VMInstanceVO.class);
        when(instanceDao.findById(6L)).thenReturn(instance);

        MockContext<CommandResult> context = new MockContext<CommandResult>(null, null, volumeMap);
        AsyncCallbackDispatcher<VmwareStorageMotionStrategyTest, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().mockCallBack(null, null)).setContext(context);

        MigrateWithStorageAnswer migAnswerMock = mock(MigrateWithStorageAnswer.class);
        when(migAnswerMock.getResult()).thenReturn(false);
        when(agentMgr.send(anyLong(), isA(MigrateWithStorageCommand.class))).thenReturn(migAnswerMock);

        strategy.copyAsync(volumeMap, to, srcHost, destHost, caller);
        assertFalse("Migration within cluster didn't fail.", result.isSuccess());
    }

    @Test
    public void testMigrateAcrossClusterSuccess() throws Exception {
        Host srcHost = mock(Host.class);
        Host destHost = mock(Host.class);
        when(srcHost.getClusterId()).thenReturn(1L);
        when(destHost.getClusterId()).thenReturn(2L);
        Map<VolumeInfo, DataStore> volumeMap = new HashMap<VolumeInfo, DataStore>();
        VirtualMachineTO to = mock(VirtualMachineTO.class);
        when(to.getId()).thenReturn(6L);
        VMInstanceVO instance = mock(VMInstanceVO.class);
        when(instanceDao.findById(6L)).thenReturn(instance);

        MockContext<CommandResult> context = new MockContext<CommandResult>(null, null, volumeMap);
        AsyncCallbackDispatcher<VmwareStorageMotionStrategyTest, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().mockCallBack(null, null)).setContext(context);

        MigrateWithStorageAnswer migAnswerMock = mock(MigrateWithStorageAnswer.class);
        when(migAnswerMock.getResult()).thenReturn(true);
        when(agentMgr.send(anyLong(), isA(MigrateWithStorageCommand.class))).thenReturn(migAnswerMock);

        strategy.copyAsync(volumeMap, to, srcHost, destHost, caller);
        assertTrue("Migration across cluster isn't successful.", result.isSuccess());
    }

    @Test
    public void testMigrateAcrossClusterFailure() throws Exception {
        Host srcHost = mock(Host.class);
        Host destHost = mock(Host.class);
        when(srcHost.getClusterId()).thenReturn(1L);
        when(destHost.getClusterId()).thenReturn(2L);
        Map<VolumeInfo, DataStore> volumeMap = new HashMap<VolumeInfo, DataStore>();
        VirtualMachineTO to = mock(VirtualMachineTO.class);
        when(to.getId()).thenReturn(6L);
        VMInstanceVO instance = mock(VMInstanceVO.class);
        when(instanceDao.findById(6L)).thenReturn(instance);

        MockContext<CommandResult> context = new MockContext<CommandResult>(null, null, volumeMap);
        AsyncCallbackDispatcher<VmwareStorageMotionStrategyTest, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().mockCallBack(null, null)).setContext(context);

        MigrateWithStorageAnswer migAnswerMock = mock(MigrateWithStorageAnswer.class);
        when(migAnswerMock.getResult()).thenReturn(false);
        when(agentMgr.send(anyLong(), isA(MigrateWithStorageCommand.class))).thenReturn(migAnswerMock);

        strategy.copyAsync(volumeMap, to, srcHost, destHost, caller);
        assertFalse("Migration across cluster didn't fail.", result.isSuccess());
    }

    private class MockContext<T> extends AsyncRpcContext<T> {
        /**
         * @param callback
         */
        public MockContext(AsyncCompletionCallback<T> callback, AsyncCallFuture<CommandResult> future, Map<VolumeInfo, DataStore> volumeToPool) {
            super(callback);
        }
    }

    protected Void mockCallBack(AsyncCallbackDispatcher<VmwareStorageMotionStrategyTest, CopyCommandResult> callback, MockContext<CommandResult> context) {
        result = callback.getResult();
        return null;
    }

    @Configuration
    @ComponentScan(basePackageClasses = {VmwareStorageMotionStrategy.class},
                   includeFilters = {@Filter(value = TestConfiguration.Library.class, type = FilterType.CUSTOM)},
                   useDefaultFilters = false)
    public static class TestConfiguration extends SpringUtils.CloudStackTestConfiguration {

        @Bean
        public VolumeDao volumeDao() {
            return Mockito.mock(VolumeDao.class);
        }

        @Bean
        public VolumeDataFactory volumeDataFactory() {
            return Mockito.mock(VolumeDataFactory.class);
        }

        @Bean
        public PrimaryDataStoreDao primaryDataStoreDao() {
            return Mockito.mock(PrimaryDataStoreDao.class);
        }

        @Bean
        public VMInstanceDao vmInstanceDao() {
            return Mockito.mock(VMInstanceDao.class);
        }

        @Bean
        public AgentManager agentManager() {
            return Mockito.mock(AgentManager.class);
        }

        @Bean
        public HostDao hostDao() {
            return Mockito.mock(HostDao.class);
        }

        @Bean
        public VirtualMachineManager vmManager() {
            return Mockito.mock(VirtualMachineManager.class);
        }

        public static class Library implements TypeFilter {
            @Override
            public boolean match(MetadataReader mdr, MetadataReaderFactory arg1) throws IOException {
                ComponentScan cs = TestConfiguration.class.getAnnotation(ComponentScan.class);
                return SpringUtils.includedInBasePackageClasses(mdr.getClassMetadata().getClassName(), cs);
            }
        }
    }
}
