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
package org.apache.cloudstack.storage.vmsnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.VMSnapshotStrategy;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.test.utils.SpringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreateVMSnapshotAnswer;
import com.cloud.agent.api.DeleteVMSnapshotAnswer;
import com.cloud.agent.api.RevertToVMSnapshotAnswer;
import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.GuestOSHypervisorDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class VMSnapshotStrategyTest extends TestCase {
    @Inject
    VMSnapshotStrategy vmSnapshotStrategy;
    @Inject
    VMSnapshotHelper vmSnapshotHelper;
    @Inject
    UserVmDao userVmDao;
    @Inject
    GuestOSDao guestOSDao;
    @Inject
    GuestOSHypervisorDao guestOsHypervisorDao;
    @Inject
    AgentManager agentMgr;
    @Inject
    VMSnapshotDao vmSnapshotDao;
    @Inject
    HostDao hostDao;
    @Inject
    PrimaryDataStoreDao primaryDataStoreDao;

    @Override
    @Before
    public void setUp() {
        ComponentContext.initComponentsLifeCycle();
    }

    @Test
    public void testCreateVMSnapshot() throws AgentUnavailableException, OperationTimedoutException {
        Long hostId = 1L;
        Long vmId = 1L;
        Long guestOsId = 1L;
        HypervisorType hypervisorType = HypervisorType.Any;
        String hypervisorVersion = "default";
        String guestOsName = "Other";
        List<VolumeObjectTO> volumeObjectTOs = new ArrayList<VolumeObjectTO>();
        VMSnapshotVO vmSnapshot = Mockito.mock(VMSnapshotVO.class);
        UserVmVO userVmVO = Mockito.mock(UserVmVO.class);
        Mockito.when(userVmVO.getGuestOSId()).thenReturn(guestOsId);
        Mockito.when(vmSnapshot.getVmId()).thenReturn(vmId);
        Mockito.when(vmSnapshotHelper.pickRunningHost(ArgumentMatchers.anyLong())).thenReturn(hostId);
        Mockito.when(vmSnapshotHelper.getVolumeTOList(ArgumentMatchers.anyLong())).thenReturn(volumeObjectTOs);
        Mockito.when(userVmDao.findById(ArgumentMatchers.anyLong())).thenReturn(userVmVO);
        GuestOSVO guestOSVO = Mockito.mock(GuestOSVO.class);
        Mockito.when(guestOSDao.findById(ArgumentMatchers.anyLong())).thenReturn(guestOSVO);
        GuestOSHypervisorVO guestOSHypervisorVO = Mockito.mock(GuestOSHypervisorVO.class);
        Mockito.when(guestOSHypervisorVO.getGuestOsName()).thenReturn(guestOsName);
        Mockito.when(guestOsHypervisorDao.findById(ArgumentMatchers.anyLong())).thenReturn(guestOSHypervisorVO);
        Mockito.when(guestOsHypervisorDao.findByOsIdAndHypervisor(ArgumentMatchers.anyLong(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(guestOSHypervisorVO);
        Mockito.when(agentMgr.send(ArgumentMatchers.anyLong(), ArgumentMatchers.any(Command.class))).thenReturn(null);
        HostVO hostVO = Mockito.mock(HostVO.class);
        Mockito.when(hostDao.findById(ArgumentMatchers.anyLong())).thenReturn(hostVO);
        Mockito.when(hostVO.getHypervisorType()).thenReturn(hypervisorType);
        Mockito.when(hostVO.getHypervisorVersion()).thenReturn(hypervisorVersion);
        Exception e = null;
        try {
            vmSnapshotStrategy.takeVMSnapshot(vmSnapshot);
        } catch (CloudRuntimeException e1) {
            e = e1;
        }

        assertNotNull(e);
        CreateVMSnapshotAnswer answer = Mockito.mock(CreateVMSnapshotAnswer.class);
        Mockito.when(answer.getResult()).thenReturn(true);
        Mockito.when(agentMgr.send(ArgumentMatchers.anyLong(), ArgumentMatchers.any(Command.class))).thenReturn(answer);
        Mockito.when(vmSnapshotDao.findById(ArgumentMatchers.anyLong())).thenReturn(vmSnapshot);
        VMSnapshot snapshot = null;
        snapshot = vmSnapshotStrategy.takeVMSnapshot(vmSnapshot);
        assertNotNull(snapshot);
    }

    @Test
    public void testRevertSnapshot() throws AgentUnavailableException, OperationTimedoutException {
        Long hostId = 1L;
        Long vmId = 1L;
        Long guestOsId = 1L;
        HypervisorType hypervisorType = HypervisorType.Any;
        String hypervisorVersion = "default";
        String guestOsName = "Other";
        List<VolumeObjectTO> volumeObjectTOs = new ArrayList<VolumeObjectTO>();
        VMSnapshotVO vmSnapshot = Mockito.mock(VMSnapshotVO.class);
        UserVmVO userVmVO = Mockito.mock(UserVmVO.class);
        Mockito.when(userVmVO.getGuestOSId()).thenReturn(guestOsId);
        Mockito.when(vmSnapshot.getVmId()).thenReturn(vmId);
        Mockito.when(vmSnapshotHelper.pickRunningHost(ArgumentMatchers.anyLong())).thenReturn(hostId);
        Mockito.when(vmSnapshotHelper.getVolumeTOList(ArgumentMatchers.anyLong())).thenReturn(volumeObjectTOs);
        Mockito.when(userVmDao.findById(ArgumentMatchers.anyLong())).thenReturn(userVmVO);
        GuestOSVO guestOSVO = Mockito.mock(GuestOSVO.class);
        Mockito.when(guestOSDao.findById(ArgumentMatchers.anyLong())).thenReturn(guestOSVO);
        GuestOSHypervisorVO guestOSHypervisorVO = Mockito.mock(GuestOSHypervisorVO.class);
        Mockito.when(guestOSHypervisorVO.getGuestOsName()).thenReturn(guestOsName);
        Mockito.when(guestOsHypervisorDao.findById(ArgumentMatchers.anyLong())).thenReturn(guestOSHypervisorVO);
        Mockito.when(guestOsHypervisorDao.findByOsIdAndHypervisor(ArgumentMatchers.anyLong(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(guestOSHypervisorVO);
        VMSnapshotTO vmSnapshotTO = Mockito.mock(VMSnapshotTO.class);
        Mockito.when(vmSnapshotHelper.getSnapshotWithParents(ArgumentMatchers.any(VMSnapshotVO.class))).thenReturn(vmSnapshotTO);
        Mockito.when(vmSnapshotDao.findById(ArgumentMatchers.anyLong())).thenReturn(vmSnapshot);
        Mockito.when(vmSnapshot.getId()).thenReturn(1L);
        Mockito.when(vmSnapshot.getCreated()).thenReturn(new Date());
        Mockito.when(agentMgr.send(ArgumentMatchers.anyLong(), ArgumentMatchers.any(Command.class))).thenReturn(null);
        HostVO hostVO = Mockito.mock(HostVO.class);
        Mockito.when(hostDao.findById(ArgumentMatchers.anyLong())).thenReturn(hostVO);
        Mockito.when(hostVO.getHypervisorType()).thenReturn(hypervisorType);
        Mockito.when(hostVO.getHypervisorVersion()).thenReturn(hypervisorVersion);
        Exception e = null;
        try {
            vmSnapshotStrategy.revertVMSnapshot(vmSnapshot);
        } catch (CloudRuntimeException e1) {
            e = e1;
        }

        assertNotNull(e);

        RevertToVMSnapshotAnswer answer = Mockito.mock(RevertToVMSnapshotAnswer.class);
        Mockito.when(answer.getResult()).thenReturn(Boolean.TRUE);
        Mockito.when(agentMgr.send(ArgumentMatchers.anyLong(), ArgumentMatchers.any(Command.class))).thenReturn(answer);
        boolean result = vmSnapshotStrategy.revertVMSnapshot(vmSnapshot);
        assertTrue(result);
    }

    @Test
    public void testDeleteVMSnapshot() throws AgentUnavailableException, OperationTimedoutException {
        Long hostId = 1L;
        Long vmId = 1L;
        Long guestOsId = 1L;
        HypervisorType hypervisorType = HypervisorType.Any;
        String hypervisorVersion = "default";
        String guestOsName = "Other";
        List<VolumeObjectTO> volumeObjectTOs = new ArrayList<VolumeObjectTO>();
        VMSnapshotVO vmSnapshot = Mockito.mock(VMSnapshotVO.class);
        UserVmVO userVmVO = Mockito.mock(UserVmVO.class);
        Mockito.when(userVmVO.getGuestOSId()).thenReturn(guestOsId);
        Mockito.when(vmSnapshot.getVmId()).thenReturn(vmId);
        Mockito.when(vmSnapshotHelper.pickRunningHost(ArgumentMatchers.anyLong())).thenReturn(hostId);
        Mockito.when(vmSnapshotHelper.getVolumeTOList(ArgumentMatchers.anyLong())).thenReturn(volumeObjectTOs);
        Mockito.when(userVmDao.findById(ArgumentMatchers.anyLong())).thenReturn(userVmVO);
        GuestOSVO guestOSVO = Mockito.mock(GuestOSVO.class);
        Mockito.when(guestOSDao.findById(ArgumentMatchers.anyLong())).thenReturn(guestOSVO);
        GuestOSHypervisorVO guestOSHypervisorVO = Mockito.mock(GuestOSHypervisorVO.class);
        Mockito.when(guestOSHypervisorVO.getGuestOsName()).thenReturn(guestOsName);
        Mockito.when(guestOsHypervisorDao.findById(ArgumentMatchers.anyLong())).thenReturn(guestOSHypervisorVO);
        Mockito.when(guestOsHypervisorDao.findByOsIdAndHypervisor(ArgumentMatchers.anyLong(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(guestOSHypervisorVO);
        VMSnapshotTO vmSnapshotTO = Mockito.mock(VMSnapshotTO.class);
        Mockito.when(vmSnapshotHelper.getSnapshotWithParents(ArgumentMatchers.any(VMSnapshotVO.class))).thenReturn(vmSnapshotTO);
        Mockito.when(vmSnapshotDao.findById(ArgumentMatchers.anyLong())).thenReturn(vmSnapshot);
        Mockito.when(vmSnapshot.getId()).thenReturn(1L);
        Mockito.when(vmSnapshot.getCreated()).thenReturn(new Date());
        Mockito.when(agentMgr.send(ArgumentMatchers.anyLong(), ArgumentMatchers.any(Command.class))).thenReturn(null);
        HostVO hostVO = Mockito.mock(HostVO.class);
        Mockito.when(hostDao.findById(ArgumentMatchers.anyLong())).thenReturn(hostVO);
        Mockito.when(hostVO.getHypervisorType()).thenReturn(hypervisorType);
        Mockito.when(hostVO.getHypervisorVersion()).thenReturn(hypervisorVersion);

        Exception e = null;
        try {
            vmSnapshotStrategy.deleteVMSnapshot(vmSnapshot);
        } catch (CloudRuntimeException e1) {
            e = e1;
        }

        assertNotNull(e);

        DeleteVMSnapshotAnswer answer = Mockito.mock(DeleteVMSnapshotAnswer.class);
        Mockito.when(answer.getResult()).thenReturn(true);
        Mockito.when(agentMgr.send(ArgumentMatchers.anyLong(), ArgumentMatchers.any(Command.class))).thenReturn(answer);

        boolean result = vmSnapshotStrategy.deleteVMSnapshot(vmSnapshot);
        assertTrue(result);
    }

    @Configuration
    @ComponentScan(basePackageClasses = {NetUtils.class, DefaultVMSnapshotStrategy.class}, includeFilters = {
            @ComponentScan.Filter(value = TestConfiguration.Library.class, type = FilterType.CUSTOM)}, useDefaultFilters = false)
    public static class TestConfiguration extends SpringUtils.CloudStackTestConfiguration {

        public static class Library implements TypeFilter {
            @Override
            public boolean match(MetadataReader mdr, MetadataReaderFactory arg1) throws IOException {
                mdr.getClassMetadata().getClassName();
                ComponentScan cs = TestConfiguration.class.getAnnotation(ComponentScan.class);
                return SpringUtils.includedInBasePackageClasses(mdr.getClassMetadata().getClassName(), cs);
            }
        }

        @Bean
        public VMSnapshotHelper vmSnapshotHelper() {
            return Mockito.mock(VMSnapshotHelper.class);
        }

        @Bean
        public GuestOSDao guestOSDao() {
            return Mockito.mock(GuestOSDao.class);
        }

        @Bean
        public GuestOSHypervisorDao guestOsHypervisorDao() {
            return Mockito.mock(GuestOSHypervisorDao.class);
        }

        @Bean
        public UserVmDao userVmDao() {
            return Mockito.mock(UserVmDao.class);
        }

        @Bean
        public VMSnapshotDao vmSnapshotDao() {
            return Mockito.mock(VMSnapshotDao.class);
        }

        @Bean
        public ConfigurationDao configurationDao() {
            return Mockito.mock(ConfigurationDao.class);
        }

        @Bean
        public AgentManager agentManager() {
            return Mockito.mock(AgentManager.class);
        }

        @Bean
        public VolumeDao volumeDao() {
            return Mockito.mock(VolumeDao.class);
        }

        @Bean
        public DiskOfferingDao diskOfferingDao() {
            return Mockito.mock(DiskOfferingDao.class);
        }

        @Bean
        public HostDao hostDao() {
            return Mockito.mock(HostDao.class);
        }

        @Bean
        public PrimaryDataStoreDao primaryDataStoreDao() {
            return Mockito.mock(PrimaryDataStoreDao.class);
        }
    }
}
