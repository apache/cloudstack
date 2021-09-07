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
//

package com.cloud.template;


import com.cloud.agent.AgentManager;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.configuration.Resource;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.storage.Storage;
import com.cloud.storage.TemplateProfile;
import com.cloud.projects.ProjectManager;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.LaunchPermissionDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateDetailsDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.command.user.template.CreateTemplateCmd;
import org.apache.cloudstack.api.command.user.template.DeleteTemplateCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageCacheManager;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageStrategyFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.test.utils.SpringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
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

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class TemplateManagerImplTest {

    @Inject
    TemplateManagerImpl templateManager = new TemplateManagerImpl();

    @Inject
    DataStoreManager dataStoreManager;

    @Inject
    VMTemplateDao vmTemplateDao;

    @Inject
    VMTemplatePoolDao vmTemplatePoolDao;

    @Inject
    TemplateDataStoreDao templateDataStoreDao;

    @Inject
    StoragePoolHostDao storagePoolHostDao;

    @Inject
    PrimaryDataStoreDao primaryDataStoreDao;

    @Inject
    ResourceLimitService resourceLimitMgr;

    @Inject
    ImageStoreDao imgStoreDao;

    @Inject
    GuestOSDao guestOSDao;

    @Inject
    VMTemplateDao tmpltDao;

    @Inject
    SnapshotDao snapshotDao;

    @Inject
    VMTemplateDetailsDao tmpltDetailsDao;

    @Inject
    StorageStrategyFactory storageStrategyFactory;

    @Inject
    VMInstanceDao _vmInstanceDao;

    @Inject
    private VMTemplateDao _tmpltDao;

    @Inject
    HypervisorGuruManager _hvGuruMgr;

    public class CustomThreadPoolExecutor extends ThreadPoolExecutor {
        AtomicInteger ai = new AtomicInteger(0);
        public CustomThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                        BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            ai.addAndGet(1);
        }

        public int getCount() {
            try {
                // Wait for some time to give before execute to run. Otherwise the tests that
                // assert and check that template seeding has been scheduled may fail. If tests
                // are seen to fail, consider increasing the sleep time.
                Thread.sleep(1000);
                return ai.get();
            } catch (Exception e) {
                return -1;
            }
        }
    }

    @Before
    public void setUp() {
        ComponentContext.initComponentsLifeCycle();
        AccountVO account = new AccountVO("admin", 1L, "networkDomain", Account.ACCOUNT_TYPE_NORMAL, "uuid");
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
    }

    @After
    public void tearDown() {
        CallContext.unregister();
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testVerifyTemplateIdOfSystemTemplate() {
        templateManager.verifyTemplateId(1L);
    }

    public void testVerifyTemplateIdOfNonSystemTemplate() {
        templateManager.verifyTemplateId(1L);
    }

    @Test
    public void testForceDeleteTemplate() {
        //In this Unit test all the conditions related to "force template delete flag" are tested.

        DeleteTemplateCmd cmd = mock(DeleteTemplateCmd.class);
        VMTemplateVO template = mock(VMTemplateVO.class);
        TemplateAdapter templateAdapter = mock(TemplateAdapter.class);
        TemplateProfile templateProfile = mock(TemplateProfile.class);


        List<VMInstanceVO> vmInstanceVOList  = new ArrayList<VMInstanceVO>();
        List<TemplateAdapter> adapters  = new ArrayList<TemplateAdapter>();
        adapters.add(templateAdapter);
        when(cmd.getId()).thenReturn(0L);
        when(_tmpltDao.findById(cmd.getId())).thenReturn(template);
        when(cmd.getZoneId()).thenReturn(null);

        when(template.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.None);
        when(template.getFormat()).thenReturn(Storage.ImageFormat.VMDK);
        templateManager.setTemplateAdapters(adapters);
        when(templateAdapter.getName()).thenReturn(TemplateAdapter.TemplateAdapterType.Hypervisor.getName().toString());
        when(templateAdapter.prepareDelete(any(DeleteTemplateCmd.class))).thenReturn(templateProfile);
        when(templateAdapter.delete(templateProfile)).thenReturn(true);

        //case 1: When Force delete flag is 'true' but VM instance VO list is empty.
        when(cmd.isForced()).thenReturn(true);
        templateManager.deleteTemplate(cmd);

        //case 2.1: When Force delete flag is 'false' and VM instance VO list is empty.
        when(cmd.isForced()).thenReturn(false);
        templateManager.deleteTemplate(cmd);

        //case 2.2: When Force delete flag is 'false' and VM instance VO list is non empty.
        when(cmd.isForced()).thenReturn(false);
        VMInstanceVO vmInstanceVO = mock(VMInstanceVO.class);
        when(vmInstanceVO.getInstanceName()).thenReturn("mydDummyVM");
        vmInstanceVOList.add(vmInstanceVO);
        when(_vmInstanceDao.listNonExpungedByTemplate(anyLong())).thenReturn(vmInstanceVOList);
        try {
            templateManager.deleteTemplate(cmd);
        } catch(Exception e) {
            assertTrue("Invalid Parameter Value Exception is expected", (e instanceof InvalidParameterValueException));
        }
    }
    @Test
    public void testPrepareTemplateIsSeeded() {
        VMTemplateVO mockTemplate = mock(VMTemplateVO.class);
        when(mockTemplate.getId()).thenReturn(202l);

        StoragePoolVO mockPool = mock(StoragePoolVO.class);
        when(mockPool.getId()).thenReturn(2l);

        PrimaryDataStore mockPrimaryDataStore = mock(PrimaryDataStore.class);
        when(mockPrimaryDataStore.getId()).thenReturn(2l);

        VMTemplateStoragePoolVO mockTemplateStore = mock(VMTemplateStoragePoolVO.class);
        when(mockTemplateStore.getDownloadState()).thenReturn(VMTemplateStorageResourceAssoc.Status.DOWNLOADED);

        when(dataStoreManager.getPrimaryDataStore(anyLong())).thenReturn(mockPrimaryDataStore);
        when(vmTemplateDao.findById(anyLong(), anyBoolean())).thenReturn(mockTemplate);
        when(vmTemplatePoolDao.findByPoolTemplate(anyLong(), anyLong(), nullable(String.class))).thenReturn(mockTemplateStore);

        doNothing().when(mockTemplateStore).setMarkedForGC(anyBoolean());

        VMTemplateStoragePoolVO returnObject = templateManager.prepareTemplateForCreate(mockTemplate, (StoragePool) mockPrimaryDataStore);
        assertTrue("Test template is already seeded", returnObject == mockTemplateStore);
    }

    @Test
    public void testPrepareTemplateNotDownloaded() {
        VMTemplateVO mockTemplate = mock(VMTemplateVO.class);
        when(mockTemplate.getId()).thenReturn(202l);

        StoragePoolVO mockPool = mock(StoragePoolVO.class);
        when(mockPool.getId()).thenReturn(2l);

        PrimaryDataStore mockPrimaryDataStore = mock(PrimaryDataStore.class);
        when(mockPrimaryDataStore.getId()).thenReturn(2l);
        when(mockPrimaryDataStore.getDataCenterId()).thenReturn(1l);

        when(dataStoreManager.getPrimaryDataStore(anyLong())).thenReturn(mockPrimaryDataStore);
        when(vmTemplateDao.findById(anyLong(), anyBoolean())).thenReturn(mockTemplate);
        when(vmTemplatePoolDao.findByPoolTemplate(anyLong(), anyLong(), nullable(String.class))).thenReturn(null);
        when(templateDataStoreDao.findByTemplateZoneDownloadStatus(202l, 1l, VMTemplateStorageResourceAssoc.Status.DOWNLOADED)).thenReturn(null);

        VMTemplateStoragePoolVO returnObject = templateManager.prepareTemplateForCreate(mockTemplate, (StoragePool) mockPrimaryDataStore);
        assertTrue("Test template is not ready", returnObject == null);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testPrepareTemplateNoHostConnectedToPool() {
        VMTemplateVO mockTemplate = mock(VMTemplateVO.class);
        when(mockTemplate.getId()).thenReturn(202l);

        StoragePoolVO mockPool = mock(StoragePoolVO.class);
        when(mockPool.getId()).thenReturn(2l);

        PrimaryDataStore mockPrimaryDataStore = mock(PrimaryDataStore.class);
        when(mockPrimaryDataStore.getId()).thenReturn(2l);
        when(mockPrimaryDataStore.getDataCenterId()).thenReturn(1l);

        TemplateDataStoreVO mockTemplateDataStore = mock(TemplateDataStoreVO.class);

        when(dataStoreManager.getPrimaryDataStore(anyLong())).thenReturn(mockPrimaryDataStore);
        when(vmTemplateDao.findById(anyLong(), anyBoolean())).thenReturn(mockTemplate);
        when(vmTemplatePoolDao.findByPoolTemplate(anyLong(), anyLong(), nullable(String.class))).thenReturn(null);
        when(templateDataStoreDao.findByTemplateZoneDownloadStatus(202l, 1l, VMTemplateStorageResourceAssoc.Status.DOWNLOADED)).thenReturn(mockTemplateDataStore);
        when(storagePoolHostDao.listByHostStatus(2l, Status.Up)).thenReturn(null);

        templateManager.prepareTemplateForCreate(mockTemplate, (StoragePool) mockPrimaryDataStore);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testPrepareTemplateInvalidTemplate() {
        when(vmTemplateDao.findById(anyLong())).thenReturn(null);
        templateManager.prepareTemplate(202, 1, null);
    }

    @Test
    public void testTemplateScheduledForDownloadInOnePool() {
        VMTemplateVO mockTemplate = mock(VMTemplateVO.class);
        StoragePoolVO mockPool = mock(StoragePoolVO.class);
        PrimaryDataStore mockPrimaryDataStore = mock(PrimaryDataStore.class);
        VMTemplateStoragePoolVO mockTemplateStore = mock(VMTemplateStoragePoolVO.class);

        when(mockPrimaryDataStore.getId()).thenReturn(2l);
        when(mockPool.getId()).thenReturn(2l);
        when(mockPool.getStatus()).thenReturn(StoragePoolStatus.Up);
        when(mockPool.getDataCenterId()).thenReturn(1l);
        when(mockTemplate.getId()).thenReturn(202l);
        when(mockTemplateStore.getDownloadState()).thenReturn(VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
        when(vmTemplateDao.findById(anyLong())).thenReturn(mockTemplate);
        when(dataStoreManager.getPrimaryDataStore(anyLong())).thenReturn(mockPrimaryDataStore);
        when(vmTemplateDao.findById(anyLong(), anyBoolean())).thenReturn(mockTemplate);
        when(vmTemplatePoolDao.findByPoolTemplate(anyLong(), anyLong(), nullable(String.class))).thenReturn(mockTemplateStore);
        when(primaryDataStoreDao.findById(anyLong())).thenReturn(mockPool);

        doNothing().when(mockTemplateStore).setMarkedForGC(anyBoolean());

        ExecutorService preloadExecutor = new CustomThreadPoolExecutor(8, 8, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(),
                new NamedThreadFactory("Template-Preloader"));
        templateManager._preloadExecutor = preloadExecutor;

        templateManager.prepareTemplate(202, 1, 2l);
        assertTrue("Test template is scheduled for seeding to on pool", ((CustomThreadPoolExecutor)preloadExecutor).getCount() == 1);
    }

    @Test
    public void testTemplateScheduledForDownloadInDisabledPool() {
        VMTemplateVO mockTemplate = mock(VMTemplateVO.class);
        StoragePoolVO mockPool = mock(StoragePoolVO.class);
        PrimaryDataStore mockPrimaryDataStore = mock(PrimaryDataStore.class);
        VMTemplateStoragePoolVO mockTemplateStore = mock(VMTemplateStoragePoolVO.class);

        when(mockPrimaryDataStore.getId()).thenReturn(2l);
        when(mockPool.getId()).thenReturn(2l);
        when(mockPool.getStatus()).thenReturn(StoragePoolStatus.Disabled);
        when(mockPool.getDataCenterId()).thenReturn(1l);
        when(mockTemplate.getId()).thenReturn(202l);
        when(mockTemplateStore.getDownloadState()).thenReturn(VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
        when(vmTemplateDao.findById(anyLong())).thenReturn(mockTemplate);
        when(dataStoreManager.getPrimaryDataStore(anyLong())).thenReturn(mockPrimaryDataStore);
        when(vmTemplateDao.findById(anyLong(), anyBoolean())).thenReturn(mockTemplate);
        when(vmTemplatePoolDao.findByPoolTemplate(anyLong(), anyLong(), nullable(String.class))).thenReturn(mockTemplateStore);
        when(primaryDataStoreDao.findById(anyLong())).thenReturn(mockPool);

        doNothing().when(mockTemplateStore).setMarkedForGC(anyBoolean());

        ExecutorService preloadExecutor = new CustomThreadPoolExecutor(8, 8, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(),
                new NamedThreadFactory("Template-Preloader"));
        templateManager._preloadExecutor = preloadExecutor;

        templateManager.prepareTemplate(202, 1, 2l);
        assertTrue("Test template is not scheduled for seeding on disabled pool", ((CustomThreadPoolExecutor)preloadExecutor).getCount() == 0);
    }

    @Test
    public void testTemplateScheduledForDownloadInMultiplePool() {
        VMTemplateVO mockTemplate = mock(VMTemplateVO.class);
        PrimaryDataStore mockPrimaryDataStore = mock(PrimaryDataStore.class);
        VMTemplateStoragePoolVO mockTemplateStore = mock(VMTemplateStoragePoolVO.class);
        List<StoragePoolVO> pools = new ArrayList<StoragePoolVO>();

        StoragePoolVO mockPool1 = mock(StoragePoolVO.class);
        when(mockPool1.getId()).thenReturn(2l);
        when(mockPool1.getStatus()).thenReturn(StoragePoolStatus.Up);
        when(mockPool1.getDataCenterId()).thenReturn(1l);
        StoragePoolVO mockPool2 = mock(StoragePoolVO.class);
        when(mockPool2.getId()).thenReturn(3l);
        when(mockPool2.getStatus()).thenReturn(StoragePoolStatus.Up);
        when(mockPool2.getDataCenterId()).thenReturn(1l);
        StoragePoolVO mockPool3 = mock(StoragePoolVO.class);
        when(mockPool3.getId()).thenReturn(4l);
        when(mockPool3.getStatus()).thenReturn(StoragePoolStatus.Up);
        when(mockPool3.getDataCenterId()).thenReturn(2l);
        pools.add(mockPool1);
        pools.add(mockPool2);
        pools.add(mockPool3);

        when(mockPrimaryDataStore.getId()).thenReturn(2l);
        when(mockTemplate.getId()).thenReturn(202l);
        when(mockTemplateStore.getDownloadState()).thenReturn(VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
        when(vmTemplateDao.findById(anyLong())).thenReturn(mockTemplate);
        when(dataStoreManager.getPrimaryDataStore(anyLong())).thenReturn(mockPrimaryDataStore);
        when(vmTemplateDao.findById(anyLong(), anyBoolean())).thenReturn(mockTemplate);
        when(vmTemplatePoolDao.findByPoolTemplate(anyLong(), anyLong(), nullable(String.class))).thenReturn(mockTemplateStore);
        when(primaryDataStoreDao.findById(2l)).thenReturn(mockPool1);
        when(primaryDataStoreDao.findById(3l)).thenReturn(mockPool2);
        when(primaryDataStoreDao.findById(4l)).thenReturn(mockPool3);
        when(primaryDataStoreDao.listByStatus(StoragePoolStatus.Up)).thenReturn(pools);

        doNothing().when(mockTemplateStore).setMarkedForGC(anyBoolean());

        ExecutorService preloadExecutor = new CustomThreadPoolExecutor(8, 8, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(),
                new NamedThreadFactory("Template-Preloader"));
        templateManager._preloadExecutor = preloadExecutor;

        templateManager.prepareTemplate(202, 1, null);
        assertTrue("Test template is scheduled for seeding to on pool", ((CustomThreadPoolExecutor) preloadExecutor).getCount() == 2);
    }

    @Test
    public void testCreatePrivateTemplateRecordForRegionStore() throws ResourceAllocationException {

        CreateTemplateCmd mockCreateCmd = mock(CreateTemplateCmd.class);
        when(mockCreateCmd.getTemplateName()).thenReturn("test");
        when(mockCreateCmd.getTemplateTag()).thenReturn(null);
        when(mockCreateCmd.getBits()).thenReturn(64);
        when(mockCreateCmd.getRequiresHvm()).thenReturn(true);
        when(mockCreateCmd.isPasswordEnabled()).thenReturn(false);
        when(mockCreateCmd.isPublic()).thenReturn(false);
        when(mockCreateCmd.isFeatured()).thenReturn(false);
        when(mockCreateCmd.isDynamicallyScalable()).thenReturn(false);
        when(mockCreateCmd.getVolumeId()).thenReturn(null);
        when(mockCreateCmd.getSnapshotId()).thenReturn(1L);
        when(mockCreateCmd.getOsTypeId()).thenReturn(1L);
        when(mockCreateCmd.getEventDescription()).thenReturn("test");
        when(mockCreateCmd.getDetails()).thenReturn(null);

        Account mockTemplateOwner = mock(Account.class);

        SnapshotVO mockSnapshot = mock(SnapshotVO.class);
        when(snapshotDao.findById(anyLong())).thenReturn(mockSnapshot);

        when(mockSnapshot.getVolumeId()).thenReturn(1L);
        when(mockSnapshot.getState()).thenReturn(Snapshot.State.BackedUp);
        when(mockSnapshot.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.XenServer);

        doNothing().when(resourceLimitMgr).checkResourceLimit(any(Account.class), eq(Resource.ResourceType.template));
        doNothing().when(resourceLimitMgr).checkResourceLimit(any(Account.class), eq(Resource.ResourceType.secondary_storage), anyLong());

        GuestOSVO mockGuestOS = mock(GuestOSVO.class);
        when(guestOSDao.findById(anyLong())).thenReturn(mockGuestOS);

        when(tmpltDao.getNextInSequence(eq(Long.class), eq("id"))).thenReturn(1L);

        List<ImageStoreVO> mockRegionStores = new ArrayList<>();
        ImageStoreVO mockRegionStore = mock(ImageStoreVO.class);
        mockRegionStores.add(mockRegionStore);
        when(imgStoreDao.findRegionImageStores()).thenReturn(mockRegionStores);

        when(tmpltDao.persist(any(VMTemplateVO.class))).thenAnswer(new Answer<VMTemplateVO>() {
            @Override
            public VMTemplateVO answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                return (VMTemplateVO)args[0];
            }
        });

        VMTemplateVO template = templateManager.createPrivateTemplateRecord(mockCreateCmd, mockTemplateOwner);
        assertTrue("Template in a region store should have cross zones set", template.isCrossZones());
    }

    @Configuration
    @ComponentScan(basePackageClasses = {TemplateManagerImpl.class},
            includeFilters = {@ComponentScan.Filter(value = TestConfiguration.Library.class, type = FilterType.CUSTOM)},
            useDefaultFilters = false)
    public static class TestConfiguration extends SpringUtils.CloudStackTestConfiguration {

        @Bean
        public DataStoreManager dataStoreManager() {
            return Mockito.mock(DataStoreManager.class);
        }

        @Bean
        public VMTemplateDao vmTemplateDao() {
            return Mockito.mock(VMTemplateDao.class);
        }

        @Bean
        public StorageStrategyFactory storageStrategyFactory() {
            return Mockito.mock(StorageStrategyFactory.class);
        }

        @Bean
        public VMTemplatePoolDao vmTemplatePoolDao() {
            return Mockito.mock(VMTemplatePoolDao.class);
        }

        @Bean
        public TemplateDataStoreDao templateDataStoreDao() {
            return Mockito.mock(TemplateDataStoreDao.class);
        }

        @Bean
        public VMTemplateZoneDao vmTemplateZoneDao() {
            return Mockito.mock(VMTemplateZoneDao.class);
        }

        @Bean
        public VMInstanceDao vmInstanceDao() {
            return Mockito.mock(VMInstanceDao.class);
        }

        @Bean
        public PrimaryDataStoreDao primaryDataStoreDao() {
            return Mockito.mock(PrimaryDataStoreDao.class);
        }

        @Bean
        public StoragePoolHostDao storagePoolHostDao() {
            return Mockito.mock(StoragePoolHostDao.class);
        }

        @Bean
        public AccountDao accountDao() {
            return Mockito.mock(AccountDao.class);
        }

        @Bean
        public AgentManager agentMgr() {
            return Mockito.mock(AgentManager.class);
        }

        @Bean
        public AccountManager accountManager() {
            return Mockito.mock(AccountManager.class);
        }

        @Bean
        public HostDao hostDao() {
            return Mockito.mock(HostDao.class);
        }

        @Bean
        public DataCenterDao dcDao() {
            return Mockito.mock(DataCenterDao.class);
        }

        @Bean
        public UserVmDao userVmDao() {
            return Mockito.mock(UserVmDao.class);
        }

        @Bean
        public VolumeDao volumeDao() {
            return Mockito.mock(VolumeDao.class);
        }

        @Bean
        public SnapshotDao snapshotDao() {
            return Mockito.mock(SnapshotDao.class);
        }

        @Bean
        public ConfigurationDao configDao() {
            return Mockito.mock(ConfigurationDao.class);
        }

        @Bean
        public DomainDao domainDao() {
            return Mockito.mock(DomainDao.class);
        }

        @Bean
        public GuestOSDao guestOSDao() {
            return Mockito.mock(GuestOSDao.class);
        }

        @Bean
        public StorageManager storageManager() {
            return Mockito.mock(StorageManager.class);
        }

        @Bean
        public UsageEventDao usageEventDao() {
            return Mockito.mock(UsageEventDao.class);
        }

        @Bean
        public ResourceLimitService resourceLimitMgr() {
            return Mockito.mock(ResourceLimitService.class);
        }

        @Bean
        public LaunchPermissionDao launchPermissionDao() {
            return Mockito.mock(LaunchPermissionDao.class);
        }

        @Bean
        public ProjectManager projectMgr() {
            return Mockito.mock(ProjectManager.class);
        }

        @Bean
        public VolumeDataFactory volFactory() {
            return Mockito.mock(VolumeDataFactory.class);
        }

        @Bean
        public TemplateDataFactory tmplFactory() {
            return Mockito.mock(TemplateDataFactory.class);
        }

        @Bean
        public SnapshotDataFactory snapshotFactory() {
            return Mockito.mock(SnapshotDataFactory.class);
        }

        @Bean
        public TemplateService tmpltSvr() {
            return Mockito.mock(TemplateService.class);
        }

        @Bean
        public VolumeOrchestrationService volumeMgr() {
            return Mockito.mock(VolumeOrchestrationService.class);
        }

        @Bean
        public EndPointSelector epSelector() {
            return Mockito.mock(EndPointSelector.class);
        }

        @Bean
        public UserVmJoinDao userVmJoinDao() {
            return Mockito.mock(UserVmJoinDao.class);
        }

        @Bean
        public SnapshotDataStoreDao snapshotStoreDao() {
            return Mockito.mock(SnapshotDataStoreDao.class);
        }

        @Bean
        public ImageStoreDao imageStoreDao() {
            return Mockito.mock(ImageStoreDao.class);
        }

        @Bean
        public MessageBus messageBus() {
            return Mockito.mock(MessageBus.class);
        }

        @Bean
        public StorageCacheManager cacheMgr() {
            return Mockito.mock(StorageCacheManager.class);
        }

        @Bean
        public TemplateAdapter templateAdapter() {
            return Mockito.mock(TemplateAdapter.class);
        }

        @Bean
        public VMTemplateDetailsDao vmTemplateDetailsDao() {
            return Mockito.mock(VMTemplateDetailsDao.class);
        }

        @Bean
        public HypervisorGuruManager hypervisorGuruManager() {
            return Mockito.mock(HypervisorGuruManager.class);
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
