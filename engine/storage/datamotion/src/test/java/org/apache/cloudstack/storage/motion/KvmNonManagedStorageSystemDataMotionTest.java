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

import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.datastore.DataStoreManagerImpl;
import org.apache.cloudstack.storage.datastore.PrimaryDataStoreImpl;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.image.datastore.ImageStoreEntity;
import org.apache.cloudstack.storage.image.store.ImageStoreImpl;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.CloudException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachineManager;

@RunWith(MockitoJUnitRunner.class)
public class KvmNonManagedStorageSystemDataMotionTest {

    @Mock
    private PrimaryDataStoreDao primaryDataStoreDao;
    @Mock
    private TemplateDataFactory templateDataFactory;
    @Mock
    private AgentManager agentManager;
    @Mock
    private DiskOfferingDao diskOfferingDao;
    @Mock
    private VirtualMachineManager virtualMachineManager;
    @Mock
    private VMTemplatePoolDao vmTemplatePoolDao;
    @Mock
    private DataStoreManagerImpl dataStoreManagerImpl;
    @Mock
    private VolumeDataFactory volumeDataFactory;

    @Spy
    @InjectMocks
    private KvmNonManagedStorageDataMotionStrategy kvmNonManagedStorageDataMotionStrategy;

    @Test
    public void canHandleTestExpectHypervisorStrategyForKvm() {
        canHandleExpectCannotHandle(HypervisorType.KVM, 1, StrategyPriority.HYPERVISOR);
    }

    @Test
    public void canHandleTestExpectCannotHandle() {
        HypervisorType[] hypervisorTypeArray = HypervisorType.values();
        for (int i = 0; i < hypervisorTypeArray.length; i++) {
            HypervisorType ht = hypervisorTypeArray[i];
            if (ht.equals(HypervisorType.KVM)) {
                continue;
            }
            canHandleExpectCannotHandle(ht, 0, StrategyPriority.CANT_HANDLE);
        }
    }

    private void canHandleExpectCannotHandle(HypervisorType hypervisorType, int times, StrategyPriority expectedStrategyPriority) {
        HostVO srcHost = new HostVO("sourceHostUuid");
        srcHost.setHypervisorType(hypervisorType);
        Mockito.doReturn(StrategyPriority.HYPERVISOR).when(kvmNonManagedStorageDataMotionStrategy).internalCanHandle(new HashMap<>());

        StrategyPriority strategyPriority = kvmNonManagedStorageDataMotionStrategy.canHandle(new HashMap<>(), srcHost, new HostVO("destHostUuid"));

        Mockito.verify(kvmNonManagedStorageDataMotionStrategy, Mockito.times(times)).internalCanHandle(new HashMap<>());
        Assert.assertEquals(expectedStrategyPriority, strategyPriority);
    }

    @Test
    public void internalCanHandleTestNonManaged() {
        StoragePoolType[] storagePoolTypeArray = StoragePoolType.values();
        for (int i = 0; i < storagePoolTypeArray.length; i++) {
            Map<VolumeInfo, DataStore> volumeMap = configureTestInternalCanHandle(false, storagePoolTypeArray[i]);
            StrategyPriority strategyPriority = kvmNonManagedStorageDataMotionStrategy.internalCanHandle(volumeMap);
            if (storagePoolTypeArray[i] == StoragePoolType.Filesystem || storagePoolTypeArray[i] == StoragePoolType.NetworkFilesystem) {
                Assert.assertEquals(StrategyPriority.HYPERVISOR, strategyPriority);
            } else {
                Assert.assertEquals(StrategyPriority.CANT_HANDLE, strategyPriority);
            }
        }
    }

    @Test
    public void internalCanHandleTestIsManaged() {
        StoragePoolType[] storagePoolTypeArray = StoragePoolType.values();
        for (int i = 0; i < storagePoolTypeArray.length; i++) {
            Map<VolumeInfo, DataStore> volumeMap = configureTestInternalCanHandle(true, storagePoolTypeArray[i]);
            StrategyPriority strategyPriority = kvmNonManagedStorageDataMotionStrategy.internalCanHandle(volumeMap);
            Assert.assertEquals(StrategyPriority.CANT_HANDLE, strategyPriority);
        }
    }

    private Map<VolumeInfo, DataStore> configureTestInternalCanHandle(boolean isManagedStorage, StoragePoolType storagePoolType) {
        VolumeObject volumeInfo = Mockito.spy(new VolumeObject());
        Mockito.doReturn(0l).when(volumeInfo).getPoolId();
        DataStore ds = Mockito.spy(new PrimaryDataStoreImpl());
        Mockito.doReturn(0l).when(ds).getId();

        Map<VolumeInfo, DataStore> volumeMap = new HashMap<>();
        volumeMap.put(volumeInfo, ds);

        StoragePoolVO storagePool = Mockito.spy(new StoragePoolVO());
        Mockito.doReturn(storagePoolType).when(storagePool).getPoolType();

        Mockito.doReturn(storagePool).when(primaryDataStoreDao).findById(0l);
        Mockito.doReturn(isManagedStorage).when(storagePool).isManaged();
        return volumeMap;
    }

    @Test
    public void getTemplateUuidTestTemplateIdNotNull() {
        String expectedTemplateUuid = prepareTestGetTemplateUuid();
        String templateUuid = kvmNonManagedStorageDataMotionStrategy.getTemplateUuid(0l);
        Assert.assertEquals(expectedTemplateUuid, templateUuid);
    }

    @Test
    public void getTemplateUuidTestTemplateIdNull() {
        prepareTestGetTemplateUuid();
        String templateUuid = kvmNonManagedStorageDataMotionStrategy.getTemplateUuid(null);
        Assert.assertEquals(null, templateUuid);
    }

    private String prepareTestGetTemplateUuid() {
        TemplateInfo templateImage = Mockito.mock(TemplateInfo.class);
        String expectedTemplateUuid = "template uuid";
        Mockito.when(templateImage.getUuid()).thenReturn(expectedTemplateUuid);
        Mockito.doReturn(templateImage).when(templateDataFactory).getTemplate(0l, DataStoreRole.Image);
        return expectedTemplateUuid;
    }

    @Test
    public void configureMigrateDiskInfoTest() {
        VolumeObject srcVolumeInfo = Mockito.spy(new VolumeObject());
        Mockito.doReturn("volume path").when(srcVolumeInfo).getPath();
        MigrateCommand.MigrateDiskInfo migrateDiskInfo = kvmNonManagedStorageDataMotionStrategy.configureMigrateDiskInfo(srcVolumeInfo, "destPath");
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.DiskType.FILE, migrateDiskInfo.getDiskType());
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.DriverType.QCOW2, migrateDiskInfo.getDriverType());
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.Source.FILE, migrateDiskInfo.getSource());
        Assert.assertEquals("destPath", migrateDiskInfo.getSourceText());
        Assert.assertEquals("volume path", migrateDiskInfo.getSerialNumber());
    }

    @Test
    public void generateDestPathTest() {
        configureAndVerifygenerateDestPathTest(true, false);
    }

    @Test(expected = CloudRuntimeException.class)
    public void generateDestPathTestExpectCloudRuntimeException() {
        configureAndVerifygenerateDestPathTest(false, false);
    }

    @Test(expected = CloudRuntimeException.class)
    public void generateDestPathTestExpectCloudRuntimeException2() {
        configureAndVerifygenerateDestPathTest(false, true);
    }

    private void configureAndVerifygenerateDestPathTest(boolean answerResult, boolean answerIsNull) {
        String uuid = "f3d49ecc-870c-475a-89fa-fd0124420a9b";
        String destPath = "/var/lib/libvirt/images/";

        VirtualMachineTO vmTO = Mockito.mock(VirtualMachineTO.class);
        Mockito.when(vmTO.getName()).thenReturn("vmName");

        VolumeVO srcVolume = Mockito.spy(new VolumeVO("name", 0l, 0l, 0l, 0l, 0l, "folder", "path", Storage.ProvisioningType.THIN, 0l, Volume.Type.ROOT));
        StoragePoolVO destStoragePool = Mockito.spy(new StoragePoolVO());

        VolumeInfo destVolumeInfo = Mockito.spy(new VolumeObject());
        Mockito.doReturn(0l).when(destVolumeInfo).getTemplateId();
        Mockito.doReturn(0l).when(destVolumeInfo).getId();
        Mockito.doReturn(Volume.Type.ROOT).when(destVolumeInfo).getVolumeType();
        Mockito.doReturn("name").when(destVolumeInfo).getName();
        Mockito.doReturn(0l).when(destVolumeInfo).getSize();
        Mockito.doReturn(uuid).when(destVolumeInfo).getUuid();

        DiskOfferingVO diskOffering = Mockito.spy(new DiskOfferingVO());
        Mockito.doReturn(0l).when(diskOffering).getId();
        Mockito.doReturn(diskOffering).when(diskOfferingDao).findById(0l);
        DiskProfile diskProfile = Mockito.spy(new DiskProfile(destVolumeInfo, diskOffering, HypervisorType.KVM));

        String templateUuid = Mockito.doReturn("templateUuid").when(kvmNonManagedStorageDataMotionStrategy).getTemplateUuid(0l);
        CreateCommand rootImageProvisioningCommand = new CreateCommand(diskProfile, templateUuid, destStoragePool, true);
        CreateAnswer createAnswer = Mockito.spy(new CreateAnswer(rootImageProvisioningCommand, "details"));
        Mockito.doReturn(answerResult).when(createAnswer).getResult();

        VolumeTO volumeTo = Mockito.mock(VolumeTO.class);
        Mockito.doReturn(destPath).when(volumeTo).getName();
        Mockito.doReturn(volumeTo).when(createAnswer).getVolume();

        if (answerIsNull) {
            Mockito.doReturn(null).when(agentManager).easySend(0l, rootImageProvisioningCommand);
        } else {
            Mockito.doReturn(createAnswer).when(agentManager).easySend(0l, rootImageProvisioningCommand);
        }

        String generatedDestPath = kvmNonManagedStorageDataMotionStrategy.generateDestPath(vmTO, srcVolume, new HostVO("sourceHostUuid"), destStoragePool, destVolumeInfo);

        Assert.assertEquals(destPath + uuid, generatedDestPath);
    }

    @Test
    public void shouldMigrateVolumeTest() {
        StoragePoolVO sourceStoragePool = Mockito.spy(new StoragePoolVO());
        HostVO destHost = new HostVO("guid");
        StoragePoolVO destStoragePool = new StoragePoolVO();
        StoragePoolType[] storagePoolTypes = StoragePoolType.values();
        for (int i = 0; i < storagePoolTypes.length; i++) {
            Mockito.doReturn(storagePoolTypes[i]).when(sourceStoragePool).getPoolType();
            boolean result = kvmNonManagedStorageDataMotionStrategy.shouldMigrateVolume(sourceStoragePool, destHost, destStoragePool);
            if (storagePoolTypes[i] == StoragePoolType.Filesystem) {
                Assert.assertTrue(result);
            } else {
                Assert.assertFalse(result);
            }
        }
    }

    @Test
    public void sendCopyCommandTest() throws AgentUnavailableException, OperationTimedoutException {
        configureAndTestSendCommandTest(null);
    }

    @Test(expected = CloudRuntimeException.class)
    public void sendCopyCommandTestThrowAgentUnavailableException() throws AgentUnavailableException, OperationTimedoutException {
        configureAndTestSendCommandTest(AgentUnavailableException.class);
    }

    @Test(expected = CloudRuntimeException.class)
    public void sendCopyCommandTestThrowOperationTimedoutException() throws AgentUnavailableException, OperationTimedoutException {
        configureAndTestSendCommandTest(OperationTimedoutException.class);
    }

    private void configureAndTestSendCommandTest(Class<? extends CloudException> exception) throws AgentUnavailableException, OperationTimedoutException {
        Host destHost = new HostVO("guid");
        TemplateObjectTO sourceTemplate = new TemplateObjectTO();
        sourceTemplate.setName("name");
        sourceTemplate.setId(0l);
        TemplateObjectTO destTemplate = new TemplateObjectTO();
        ImageStoreVO dataStoreVO = Mockito.mock(ImageStoreVO.class);
        Mockito.when(dataStoreVO.getId()).thenReturn(0l);

        ImageStoreEntity destDataStore = Mockito.mock(ImageStoreImpl.class);
        Mockito.doReturn(0l).when(destDataStore).getId();

        Answer copyCommandAnswer = Mockito.mock(Answer.class);

        if (exception == null) {
            Mockito.doReturn(copyCommandAnswer).when(agentManager).send(Mockito.anyLong(), Mockito.any(CopyCommand.class));
        } else {
            Mockito.doThrow(exception).when(agentManager).send(Mockito.anyLong(), Mockito.any(CopyCommand.class));
        }

        Mockito.doNothing().when(kvmNonManagedStorageDataMotionStrategy).logInCaseOfTemplateCopyFailure(Mockito.any(Answer.class), Mockito.any(TemplateObjectTO.class),
                Mockito.any(DataStore.class));

        kvmNonManagedStorageDataMotionStrategy.sendCopyCommand(destHost, sourceTemplate, destTemplate, destDataStore);

        InOrder verifyInOrder = Mockito.inOrder(virtualMachineManager, agentManager, kvmNonManagedStorageDataMotionStrategy);

        verifyInOrder.verify(virtualMachineManager).getExecuteInSequence(HypervisorType.KVM);
        verifyInOrder.verify(agentManager).send(Mockito.anyLong(), Mockito.any(CopyCommand.class));
        verifyInOrder.verify(kvmNonManagedStorageDataMotionStrategy).logInCaseOfTemplateCopyFailure(Mockito.any(Answer.class), Mockito.any(TemplateObjectTO.class),
                Mockito.any(DataStore.class));
    }

    @Test
    public void copyTemplateToTargetStorageIfNeededTestTemplateAlreadyOnTargetHost() throws AgentUnavailableException, OperationTimedoutException {
        Answer copyCommandAnswer = Mockito.mock(Answer.class);
        Mockito.when(copyCommandAnswer.getResult()).thenReturn(true);
        configureAndTestcopyTemplateToTargetStorageIfNeeded(new VMTemplateStoragePoolVO(0l, 0l), StoragePoolType.Filesystem, 0);
    }

    @Test
    public void migrateTemplateToTargetStorageIfNeededTestTemplateNotOnTargetHost() throws AgentUnavailableException, OperationTimedoutException {
        configureAndTestcopyTemplateToTargetStorageIfNeeded(null, StoragePoolType.Filesystem, 1);
    }

    @Test
    public void migrateTemplateToTargetStorageIfNeededTestNonDesiredStoragePoolType() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolType[] storagePoolTypeArray = StoragePoolType.values();
        for (int i = 0; i < storagePoolTypeArray.length; i++) {
            if (storagePoolTypeArray[i] == StoragePoolType.Filesystem) {
                continue;
            }
            configureAndTestcopyTemplateToTargetStorageIfNeeded(new VMTemplateStoragePoolVO(0l, 0l), storagePoolTypeArray[i], 0);
        }
    }

    private void configureAndTestcopyTemplateToTargetStorageIfNeeded(VMTemplateStoragePoolVO vmTemplateStoragePoolVO, StoragePoolType storagePoolType, int times) {
        DataStore destDataStore = Mockito.mock(DataStore.class);
        Host destHost = Mockito.mock(Host.class);

        VolumeInfo srcVolumeInfo = Mockito.mock(VolumeInfo.class);
        Mockito.when(srcVolumeInfo.getTemplateId()).thenReturn(0l);

        StoragePool srcStoragePool = Mockito.mock(StoragePool.class);

        VolumeInfo destVolumeInfo = Mockito.mock(VolumeInfo.class);
        Mockito.when(volumeDataFactory.getVolume(Mockito.anyLong(), Mockito.any(DataStore.class))).thenReturn(destVolumeInfo);

        StoragePool destStoragePool = Mockito.mock(StoragePool.class);
        Mockito.when(destStoragePool.getId()).thenReturn(0l);
        Mockito.when(destStoragePool.getPoolType()).thenReturn(storagePoolType);

        DataStore sourceTemplateDataStore = Mockito.mock(DataStore.class);
        Mockito.when(sourceTemplateDataStore.getName()).thenReturn("sourceTemplateName");

        TemplateInfo sourceTemplateInfo = Mockito.mock(TemplateInfo.class);
        Mockito.when(sourceTemplateInfo.getInstallPath()).thenReturn("installPath");
        Mockito.when(sourceTemplateInfo.getUuid()).thenReturn("uuid");
        Mockito.when(sourceTemplateInfo.getId()).thenReturn(0l);
        Mockito.when(sourceTemplateInfo.getUrl()).thenReturn("url");
        Mockito.when(sourceTemplateInfo.getDisplayText()).thenReturn("display text");
        Mockito.when(sourceTemplateInfo.getChecksum()).thenReturn("checksum");
        Mockito.when(sourceTemplateInfo.isRequiresHvm()).thenReturn(true);
        Mockito.when(sourceTemplateInfo.getAccountId()).thenReturn(0l);
        Mockito.when(sourceTemplateInfo.getUniqueName()).thenReturn("unique name");
        Mockito.when(sourceTemplateInfo.getFormat()).thenReturn(ImageFormat.QCOW2);
        Mockito.when(sourceTemplateInfo.getSize()).thenReturn(0l);
        Mockito.when(sourceTemplateInfo.getHypervisorType()).thenReturn(HypervisorType.KVM);

        Mockito.when(vmTemplatePoolDao.findByPoolTemplate(Mockito.anyLong(), Mockito.anyLong())).thenReturn(vmTemplateStoragePoolVO);
        Mockito.when(dataStoreManagerImpl.getImageStore(Mockito.anyLong())).thenReturn(sourceTemplateDataStore);
        Mockito.when(templateDataFactory.getTemplate(Mockito.anyLong(), Mockito.eq(sourceTemplateDataStore))).thenReturn(sourceTemplateInfo);
        Mockito.when(templateDataFactory.getTemplate(Mockito.anyLong(), Mockito.eq(destDataStore))).thenReturn(sourceTemplateInfo);
        kvmNonManagedStorageDataMotionStrategy.copyTemplateToTargetFilesystemStorageIfNeeded(srcVolumeInfo, srcStoragePool, destDataStore, destStoragePool, destHost);
        Mockito.doNothing().when(kvmNonManagedStorageDataMotionStrategy).updateTemplateReferenceIfSuccessfulCopy(Mockito.any(VolumeInfo.class), Mockito.any(StoragePool.class),
                Mockito.any(TemplateInfo.class), Mockito.any(DataStore.class));

        InOrder verifyInOrder = Mockito.inOrder(vmTemplatePoolDao, dataStoreManagerImpl, templateDataFactory, kvmNonManagedStorageDataMotionStrategy);
        verifyInOrder.verify(vmTemplatePoolDao, Mockito.times(1)).findByPoolTemplate(Mockito.anyLong(), Mockito.anyLong());
        verifyInOrder.verify(dataStoreManagerImpl, Mockito.times(times)).getImageStore(Mockito.anyLong());
        verifyInOrder.verify(templateDataFactory, Mockito.times(times)).getTemplate(Mockito.anyLong(), Mockito.eq(sourceTemplateDataStore));
        verifyInOrder.verify(templateDataFactory, Mockito.times(times)).getTemplate(Mockito.anyLong(), Mockito.eq(destDataStore));
        verifyInOrder.verify(kvmNonManagedStorageDataMotionStrategy, Mockito.times(times)).sendCopyCommand(Mockito.eq(destHost), Mockito.any(TemplateObjectTO.class),
                Mockito.any(TemplateObjectTO.class), Mockito.eq(destDataStore));
    }
}
