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
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.PrimaryDataStoreImpl;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DiskProfile;

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
}
