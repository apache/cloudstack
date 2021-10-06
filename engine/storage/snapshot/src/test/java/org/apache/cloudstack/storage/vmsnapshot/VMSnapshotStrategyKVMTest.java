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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy.SnapshotOperation;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageStrategyFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.test.utils.SpringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
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
import com.cloud.agent.api.DeleteVMSnapshotAnswer;
import com.cloud.agent.api.RevertToVMSnapshotAnswer;
import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.GuestOSHypervisorDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotApiService;
import com.cloud.user.AccountService;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotDetailsVO;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDetailsDao;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class VMSnapshotStrategyKVMTest extends TestCase{
    List<StoragePoolVO> storage;
    @Inject
    VMSnapshotHelper vmSnapshotHelper;
    @Inject
    GuestOSDao guestOSDao;
    @Inject
    GuestOSHypervisorDao guestOsHypervisorDao;
    @Inject
    UserVmDao userVmDao;
    @Inject
    VMSnapshotDao vmSnapshotDao;
    @Inject
    ConfigurationDao configurationDao;
    @Inject
    AgentManager agentMgr;
    @Inject
    VolumeDao volumeDao;
    @Inject
    DiskOfferingDao diskOfferingDao;
    @Inject
    HostDao hostDao;
    @Inject
    VolumeApiService _volumeService;
    @Inject
    AccountService _accountService;
    @Inject
    VolumeDataFactory volumeDataFactory;
    @Inject
    SnapshotApiService _snapshotService;
    @Inject
    SnapshotDao _snapshotDao;
    @Inject
    StorageStrategyFactory _storageStrategyFactory;
    @Inject
    SnapshotDataFactory _snapshotDataFactory;
    @Inject
    PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    DataStoreManager _dataStoreMgr;
    @Inject
    StorageVMSnapshotStrategy vmStrategy;
    @Inject
    VMSnapshotDetailsDao vmSnapshotDetailsDao;

    @Override
    @Before
    public void setUp() throws Exception {
        ComponentContext.initComponentsLifeCycle();
    }

    @Test
    public void testCreateDiskSnapshotBasedOnStrategy() throws Exception {
        VMSnapshot vmSnapshot = Mockito.mock(VMSnapshot.class);
        List<SnapshotInfo> forRollback = new ArrayList<>();
        VolumeInfo vol = Mockito.mock(VolumeInfo.class);
        SnapshotInfo snapshotInfo = Mockito.mock(SnapshotInfo.class);
        SnapshotStrategy strategy = Mockito.mock(SnapshotStrategy.class);
        DataStore dataStore = Mockito.mock(DataStore.class);
        String volUuid = UUID.randomUUID().toString();
        String vmUuid = UUID.randomUUID().toString();
        SnapshotVO snapshot = new SnapshotVO(vol.getDataCenterId(), vol.getAccountId(), vol.getDomainId(),
                               vol.getId(),vol.getDiskOfferingId(), vmUuid + "_" + volUuid,(short) SnapshotVO.MANUAL_POLICY_ID,
                               "MANUAL",vol.getSize(),vol.getMinIops(),vol.getMaxIops(), Hypervisor.HypervisorType.KVM, null);
        PowerMockito.whenNew(SnapshotVO.class).withAnyArguments().thenReturn(snapshot);
        when(vmSnapshot.getUuid()).thenReturn(vmUuid);
        when(vol.getUuid()).thenReturn(volUuid);
        when(_snapshotDao.persist(any())).thenReturn(snapshot);
        when(vol.getDataStore()).thenReturn(dataStore);
        when(_snapshotDataFactory.getSnapshot(snapshot.getId(), vol.getDataStore())).thenReturn(snapshotInfo);
        when(_storageStrategyFactory.getSnapshotStrategy(snapshotInfo, SnapshotOperation.TAKE)).thenReturn(strategy);

        SnapshotInfo info = null;

        when(strategy.takeSnapshot(any())).thenReturn(snapshotInfo);
        VMSnapshotDetailsVO vmDetails = new VMSnapshotDetailsVO(vmSnapshot.getId(), volUuid, String.valueOf(snapshot.getId()), false);
        PowerMockito.whenNew(VMSnapshotDetailsVO.class).withAnyArguments().thenReturn(vmDetails);
        when(vmSnapshotDetailsDao.persist(any())).thenReturn(vmDetails);

        info =  vmStrategy.createDiskSnapshot(vmSnapshot, forRollback, vol);
        assertNotNull(info);
    }

    @Test
    public void testRevertVMsnapshot() throws AgentUnavailableException, OperationTimedoutException{
        Long hostId = 1L;
        Long vmId = 1L;
        Long guestOsId = 1L;
        HypervisorType hypervisorType = HypervisorType.KVM;
        String hypervisorVersion = "default";
        String guestOsName = "Other";
        List<VolumeObjectTO> volumeObjectTOs = new ArrayList<VolumeObjectTO>();
        VMSnapshotVO vmSnapshot = Mockito.mock(VMSnapshotVO.class);
        UserVmVO userVmVO = Mockito.mock(UserVmVO.class);
        Mockito.when(userVmVO.getGuestOSId()).thenReturn(guestOsId);
        Mockito.when(vmSnapshot.getVmId()).thenReturn(vmId);
        Mockito.when(vmSnapshotHelper.pickRunningHost(Matchers.anyLong())).thenReturn(hostId);
        Mockito.when(vmSnapshotHelper.getVolumeTOList(Matchers.anyLong())).thenReturn(volumeObjectTOs);
        Mockito.when(userVmDao.findById(Matchers.anyLong())).thenReturn(userVmVO);
        GuestOSVO guestOSVO = Mockito.mock(GuestOSVO.class);
        Mockito.when(guestOSDao.findById(Matchers.anyLong())).thenReturn(guestOSVO);
        GuestOSHypervisorVO guestOSHypervisorVO = Mockito.mock(GuestOSHypervisorVO.class);
        Mockito.when(guestOSHypervisorVO.getGuestOsName()).thenReturn(guestOsName);
        Mockito.when(guestOsHypervisorDao.findById(Matchers.anyLong())).thenReturn(guestOSHypervisorVO);
        Mockito.when(guestOsHypervisorDao.findByOsIdAndHypervisor(Matchers.anyLong(), Matchers.anyString(), Matchers.anyString())).thenReturn(guestOSHypervisorVO);
        VMSnapshotTO vmSnapshotTO = Mockito.mock(VMSnapshotTO.class);
        Mockito.when(vmSnapshotHelper.getSnapshotWithParents(Matchers.any(VMSnapshotVO.class))).thenReturn(vmSnapshotTO);
        Mockito.when(vmSnapshotDao.findById(Matchers.anyLong())).thenReturn(vmSnapshot);
        Mockito.when(vmSnapshot.getId()).thenReturn(1L);
        Mockito.when(vmSnapshot.getCreated()).thenReturn(new Date());
        HostVO hostVO = Mockito.mock(HostVO.class);
        Mockito.when(hostDao.findById(Matchers.anyLong())).thenReturn(hostVO);
        Mockito.when(hostVO.getHypervisorType()).thenReturn(hypervisorType);
        Mockito.when(hostVO.getHypervisorVersion()).thenReturn(hypervisorVersion);

        RevertToVMSnapshotAnswer answer = Mockito.mock(RevertToVMSnapshotAnswer.class);
        Mockito.when(answer.getResult()).thenReturn(Boolean.TRUE);
        boolean result = vmStrategy.revertVMSnapshot(vmSnapshot);
        assertTrue(result);
    }

    @Test
    public void testRevertDiskSnapshot() throws Exception {
        VMSnapshot vmSnapshot = Mockito.mock(VMSnapshot.class);
        VolumeInfo vol = Mockito.mock(VolumeInfo.class);
        SnapshotVO snapshotVO = Mockito.mock(SnapshotVO.class);
        Snapshot snap = Mockito.mock(Snapshot.class);
        DataStore dataStore = Mockito.mock(DataStore.class);

        String volUuid = UUID.randomUUID().toString();
        String vmUuid = UUID.randomUUID().toString();
        String name = vmUuid + "_" + volUuid;
        when(vol.getUuid()).thenReturn(volUuid);
        when(vmSnapshot.getUuid()).thenReturn(vmUuid);
        when(vol.getDataStore()).thenReturn(dataStore);
        when(snapshotVO.getId()).thenReturn(1L);
        when(_snapshotService.revertSnapshot(snapshotVO.getId())).thenReturn(snap);
    //    testFindSnapshotByName(name);
        vmStrategy.revertDiskSnapshot(vmSnapshot);
    }

    @Test
    public void testDeleteDiskSnapshot() {
        VMSnapshot vmSnapshot = Mockito.mock(VMSnapshot.class);
        VolumeInfo vol = Mockito.mock(VolumeInfo.class);
        SnapshotVO snapshotVO = Mockito.mock(SnapshotVO.class);
        Snapshot snap = Mockito.mock(Snapshot.class);
        SnapshotInfo info = Mockito.mock(SnapshotInfo.class);
        SnapshotStrategy strategy = Mockito.mock(SnapshotStrategy.class);
        String volUuid = UUID.randomUUID().toString();
        String vmUuid = UUID.randomUUID().toString();
        String name = vmUuid + "_" + volUuid;
        when(vol.getUuid()).thenReturn(volUuid);
        when(vmSnapshot.getUuid()).thenReturn(vmUuid);
        when(snapshotVO.getId()).thenReturn(1L);
        when( _snapshotDataFactory.getSnapshot(snapshotVO.getId(), vol.getDataStore())).thenReturn(info);
        when(_storageStrategyFactory.getSnapshotStrategy(info, SnapshotOperation.DELETE)).thenReturn(strategy);
        testFindSnapshotByName(name);
        vmStrategy.deleteDiskSnapshot(vmSnapshot);
    }

    @Test
    public void testDeleteVMsnapshot() throws AgentUnavailableException, OperationTimedoutException{
        Long hostId = 1L;
        Long vmId = 1L;
        Long guestOsId = 1L;
        HypervisorType hypervisorType = HypervisorType.KVM;
        String hypervisorVersion = "default";
        String guestOsName = "Other";
        List<VolumeObjectTO> volumeObjectTOs = new ArrayList<VolumeObjectTO>();
        VMSnapshotVO vmSnapshot = Mockito.mock(VMSnapshotVO.class);
        UserVmVO userVmVO = Mockito.mock(UserVmVO.class);
        Mockito.when(userVmVO.getGuestOSId()).thenReturn(guestOsId);
        Mockito.when(vmSnapshot.getVmId()).thenReturn(vmId);
        Mockito.when(vmSnapshotHelper.pickRunningHost(Matchers.anyLong())).thenReturn(hostId);
        Mockito.when(vmSnapshotHelper.getVolumeTOList(Matchers.anyLong())).thenReturn(volumeObjectTOs);
        Mockito.when(userVmDao.findById(Matchers.anyLong())).thenReturn(userVmVO);
        GuestOSVO guestOSVO = Mockito.mock(GuestOSVO.class);
        Mockito.when(guestOSDao.findById(Matchers.anyLong())).thenReturn(guestOSVO);
        GuestOSHypervisorVO guestOSHypervisorVO = Mockito.mock(GuestOSHypervisorVO.class);
        Mockito.when(guestOSHypervisorVO.getGuestOsName()).thenReturn(guestOsName);
        Mockito.when(guestOsHypervisorDao.findById(Matchers.anyLong())).thenReturn(guestOSHypervisorVO);
        Mockito.when(guestOsHypervisorDao.findByOsIdAndHypervisor(Matchers.anyLong(), Matchers.anyString(), Matchers.anyString())).thenReturn(guestOSHypervisorVO);
        VMSnapshotTO vmSnapshotTO = Mockito.mock(VMSnapshotTO.class);
        Mockito.when(vmSnapshotHelper.getSnapshotWithParents(Matchers.any(VMSnapshotVO.class))).thenReturn(vmSnapshotTO);
        Mockito.when(vmSnapshotDao.findById(Matchers.anyLong())).thenReturn(vmSnapshot);
        Mockito.when(vmSnapshot.getId()).thenReturn(1L);
        Mockito.when(vmSnapshot.getCreated()).thenReturn(new Date());
        HostVO hostVO = Mockito.mock(HostVO.class);
        Mockito.when(hostDao.findById(Matchers.anyLong())).thenReturn(hostVO);
        Mockito.when(hostVO.getHypervisorType()).thenReturn(hypervisorType);
        Mockito.when(hostVO.getHypervisorVersion()).thenReturn(hypervisorVersion);
        DeleteVMSnapshotAnswer answer = Mockito.mock(DeleteVMSnapshotAnswer.class);
        Mockito.when(answer.getResult()).thenReturn(true);

        boolean result = vmStrategy.deleteVMSnapshot(vmSnapshot);
        assertTrue(result);
    }

    @SuppressWarnings("unchecked")
    private SnapshotVO testFindSnapshotByName(String snapshotName) {
        SearchBuilder<SnapshotVO> sb = Mockito.mock(SearchBuilder.class);
        when(_snapshotDao.createSearchBuilder()).thenReturn(sb);
        SearchCriteria<SnapshotVO> sc = Mockito.mock(SearchCriteria.class);
        when(sb.create()).thenReturn(sc);
        SnapshotVO snap = Mockito.mock(SnapshotVO.class);
        when(_snapshotDao.findOneBy(sc)).thenReturn(snap);
        return snap;
    }

    @Configuration
    @ComponentScan(basePackageClasses = {NetUtils.class, StorageVMSnapshotStrategy.class}, includeFilters = {
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
        public VolumeApiService volumeApiService() {
            return Mockito.mock(VolumeApiService.class);
        }

        @Bean
        public AccountService accountService() {
            return Mockito.mock(AccountService.class);
        }

        @Bean
        public VolumeDataFactory volumeDataFactory() {
            return Mockito.mock(VolumeDataFactory.class);
        }

        @Bean
        public SnapshotApiService snapshotApiService() {
            return Mockito.mock(SnapshotApiService.class);
        }

        @Bean
        public SnapshotDao snapshotDao() {
            return Mockito.mock(SnapshotDao.class);
        }

        @Bean
        public StorageStrategyFactory storageStrategyFactory() {
            return Mockito.mock(StorageStrategyFactory.class);
        }

        @Bean
        public SnapshotDataFactory snapshotDataFactory() {
            return Mockito.mock(SnapshotDataFactory.class);
        }

        @Bean
        public VMSnapshotVO vmSnapshotVO() {
            return Mockito.mock(VMSnapshotVO.class);
        }

        @Bean
        protected  PrimaryDataStoreDao storagePool() {
            return Mockito.mock(PrimaryDataStoreDao.class);
        }

        @Bean
         public DataStoreManager dataStoreMgr() {
            return Mockito.mock(DataStoreManager.class);
        }

        @Bean
        public DataStoreProviderManager manager() {
            return Mockito.mock(DataStoreProviderManager.class);
        }

        @Bean
        public VMSnapshotDetailsDao vmSnapshotDetailsDao () {
            return Mockito.mock(VMSnapshotDetailsDao.class);
        }
    }
}
