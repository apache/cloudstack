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
package com.cloud.kubernetes.version;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.api.response.KubernetesSupportedVersionResponse;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.storage.command.CheckFileInIsoAnswer;
import org.apache.cloudstack.storage.command.CheckFileInIsoCommand;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.api.Answer;
import com.cloud.api.query.dao.TemplateJoinDao;
import com.cloud.api.query.vo.TemplateJoinVO;
import com.cloud.cpu.CPU;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.kubernetes.version.dao.KubernetesSupportedVersionDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.VMTemplateDetailVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateDetailsDao;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesVersionManagerImplTest {

    @Mock
    TemplateJoinDao templateJoinDao;

    @Mock
    ImageStoreDao imageStoreDao;

    @Mock
    DataCenterDao dataCenterDao;

    @Mock
    KubernetesSupportedVersionDao kubernetesSupportedVersionDao;

    @Mock
    VMTemplateDao templateDao;

    @Mock
    VMTemplateDetailsDao templateDetailsDao;

    @Mock
    TemplateDataStoreDao templateDataStoreDao;

    @Mock
    DataStoreManager dataStoreManager;

    @Mock
    EndPointSelector endPointSelector;

    @InjectMocks
    KubernetesVersionManagerImpl kubernetesVersionManager = new KubernetesVersionManagerImpl();

    @Test
    public void testUpdateTemplateDetailsInKubernetesSupportedVersionResponseNullTemplate() {
        KubernetesSupportedVersion kubernetesSupportedVersion = Mockito.mock(KubernetesSupportedVersion.class);
        Mockito.when(kubernetesSupportedVersion.getIsoId()).thenReturn(1L);
        KubernetesSupportedVersionResponse response = new KubernetesSupportedVersionResponse();
        kubernetesVersionManager.updateTemplateDetailsInKubernetesSupportedVersionResponse(kubernetesSupportedVersion,
                response, true);
        Assert.assertNull(ReflectionTestUtils.getField(response, "isoId"));
    }

    @Test
    public void testUpdateTemplateDetailsInKubernetesSupportedVersionResponseValidTemplate() {
        KubernetesSupportedVersion kubernetesSupportedVersion = Mockito.mock(KubernetesSupportedVersion.class);
        Mockito.when(kubernetesSupportedVersion.getIsoId()).thenReturn(1L);
        KubernetesSupportedVersionResponse response = new KubernetesSupportedVersionResponse();
        TemplateJoinVO templateJoinVO = Mockito.mock(TemplateJoinVO.class);
        Mockito.when(templateJoinVO.getArch()).thenReturn(CPU.CPUArch.getDefault());
        String uuid = UUID.randomUUID().toString();
        Mockito.when(templateJoinVO.getUuid()).thenReturn(uuid);
        Mockito.when(templateJoinDao.findById(1L)).thenReturn(templateJoinVO);
        kubernetesVersionManager.updateTemplateDetailsInKubernetesSupportedVersionResponse(kubernetesSupportedVersion,
                response, true);
        Assert.assertEquals(uuid, ReflectionTestUtils.getField(response, "isoId"));
        Assert.assertNull(ReflectionTestUtils.getField(response, "isoState"));
        ObjectInDataStoreStateMachine.State state = ObjectInDataStoreStateMachine.State.Ready;
        Mockito.when(templateJoinVO.getState()).thenReturn(state);
        kubernetesVersionManager.updateTemplateDetailsInKubernetesSupportedVersionResponse(kubernetesSupportedVersion,
                response, true);
        Assert.assertEquals(state.toString(), ReflectionTestUtils.getField(response, "isoState"));
    }

    @Test
    public void testValidateImageStoreForZoneWithDirectDownload() {
        ReflectionTestUtils.invokeMethod(kubernetesVersionManager, "validateImageStoreForZone", 1L, true);
    }

    @Test
    public void testValidateImageStoreForZoneWithValidZone() {
        Long zoneId = 1L;
        List<ImageStoreVO> imageStores = Collections.singletonList(Mockito.mock(ImageStoreVO.class));
        Mockito.when(imageStoreDao.listStoresByZoneId(zoneId)).thenReturn(imageStores);

        ReflectionTestUtils.invokeMethod(kubernetesVersionManager, "validateImageStoreForZone", zoneId, false);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateImageStoreForZoneWithNoImageStore() {
        Long zoneId = 1L;
        DataCenterVO zone = Mockito.mock(DataCenterVO.class);
        Mockito.when(zone.getName()).thenReturn("test-zone");
        Mockito.when(dataCenterDao.findById(zoneId)).thenReturn(zone);
        Mockito.when(imageStoreDao.listStoresByZoneId(zoneId)).thenReturn(Collections.emptyList());

        ReflectionTestUtils.invokeMethod(kubernetesVersionManager, "validateImageStoreForZone", zoneId, false);
    }

    @Test
    public void testValidateImageStoreForAllZonesWithAllValid() {
        DataCenterVO zone1 = Mockito.mock(DataCenterVO.class);
        Mockito.when(zone1.getId()).thenReturn(1L);
        DataCenterVO zone2 = Mockito.mock(DataCenterVO.class);
        Mockito.when(zone2.getId()).thenReturn(2L);
        List<DataCenterVO> zones = Arrays.asList(zone1, zone2);
        Mockito.when(dataCenterDao.listAllZones()).thenReturn(zones);

        List<ImageStoreVO> imageStores = Collections.singletonList(Mockito.mock(ImageStoreVO.class));
        Mockito.when(imageStoreDao.listStoresByZoneId(1L)).thenReturn(imageStores);
        Mockito.when(imageStoreDao.listStoresByZoneId(2L)).thenReturn(imageStores);

        ReflectionTestUtils.invokeMethod(kubernetesVersionManager, "validateImageStoreForZone", (Long) null, false);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateImageStoreForAllZonesWithSomeMissingStorage() {
        DataCenterVO zone1 = Mockito.mock(DataCenterVO.class);
        Mockito.when(zone1.getId()).thenReturn(1L);
        DataCenterVO zone2 = Mockito.mock(DataCenterVO.class);
        Mockito.when(zone2.getId()).thenReturn(2L);
        Mockito.when(zone2.getName()).thenReturn("zone-without-storage");
        List<DataCenterVO> zones = Arrays.asList(zone1, zone2);
        Mockito.when(dataCenterDao.listAllZones()).thenReturn(zones);

        List<ImageStoreVO> imageStores = Collections.singletonList(Mockito.mock(ImageStoreVO.class));
        Mockito.when(imageStoreDao.listStoresByZoneId(1L)).thenReturn(imageStores);
        Mockito.when(imageStoreDao.listStoresByZoneId(2L)).thenReturn(Collections.emptyList());

        ReflectionTestUtils.invokeMethod(kubernetesVersionManager, "validateImageStoreForZone", (Long) null, false);
    }

    private static final long ISO_ID = 200L;
    private static final long ZONE_ID = 1L;

    private KubernetesSupportedVersionVO mockVersionWithIso() {
        KubernetesSupportedVersionVO version = Mockito.mock(KubernetesSupportedVersionVO.class);
        Mockito.when(version.getIsoId()).thenReturn(ISO_ID);
        return version;
    }

    private void mockStoredEtcdDetail(String value) {
        VMTemplateDetailVO detail = Mockito.mock(VMTemplateDetailVO.class);
        Mockito.when(detail.getValue()).thenReturn(value);
        Mockito.when(templateDetailsDao.findDetail(ISO_ID, KubernetesVersionService.ETCD_INCLUDED_ISO_DETAIL)).thenReturn(detail);
    }

    private EndPoint mockIsoReadyOnImageStore() {
        return mockIsoReadyOnImageStore(true);
    }

    private EndPoint mockIsoReadyOnImageStore(boolean inZone) {
        VMTemplateVO iso = Mockito.mock(VMTemplateVO.class);
        Mockito.when(iso.isDirectDownload()).thenReturn(false);
        Mockito.when(templateDao.findById(ISO_ID)).thenReturn(iso);
        TemplateDataStoreVO isoOnStore = Mockito.mock(TemplateDataStoreVO.class);
        Mockito.when(isoOnStore.getInstallPath()).thenReturn("template/tmpl/1/200/200-1-uuid.iso");
        Mockito.when(isoOnStore.getDataStoreId()).thenReturn(5L);
        if (inZone) {
            Mockito.when(templateDataStoreDao.findByTemplateZoneReady(ISO_ID, ZONE_ID)).thenReturn(isoOnStore);
        } else {
            Mockito.when(templateDataStoreDao.findReadyByTemplate(ISO_ID, DataStoreRole.Image)).thenReturn(isoOnStore);
        }
        DataStore store = Mockito.mock(DataStore.class);
        Mockito.when(dataStoreManager.getDataStore(5L, DataStoreRole.Image)).thenReturn(store);
        EndPoint endPoint = Mockito.mock(EndPoint.class);
        Mockito.when(endPointSelector.select(store)).thenReturn(endPoint);
        return endPoint;
    }

    private void verifyNothingStored() {
        Mockito.verify(templateDetailsDao, Mockito.never()).addDetail(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void testIsEtcdIncludedInIsoUsesStoredResult() {
        KubernetesSupportedVersionVO version = mockVersionWithIso();
        mockStoredEtcdDetail("true");
        Assert.assertEquals(Boolean.TRUE, kubernetesVersionManager.isEtcdIncludedInIso(version, ZONE_ID));
        mockStoredEtcdDetail("false");
        Assert.assertEquals(Boolean.FALSE, kubernetesVersionManager.isEtcdIncludedInIso(version, ZONE_ID));
        Mockito.verifyNoInteractions(endPointSelector);
        verifyNothingStored();
    }

    @Test
    public void testIsEtcdIncludedInIsoChecksAndStoresWhenEtcdPresent() {
        KubernetesSupportedVersionVO version = mockVersionWithIso();
        EndPoint endPoint = mockIsoReadyOnImageStore();
        Mockito.when(endPoint.sendMessage(Mockito.any(CheckFileInIsoCommand.class))).thenAnswer(invocation -> {
            CheckFileInIsoCommand cmd = invocation.getArgument(0);
            Assert.assertEquals("template/tmpl/1/200/200-1-uuid.iso", cmd.getIsoPath());
            Assert.assertEquals(KubernetesVersionService.ETCD_ARCHIVE_PATH_IN_ISO, cmd.getFilePath());
            return new CheckFileInIsoAnswer(cmd, true);
        });

        Assert.assertEquals(Boolean.TRUE, kubernetesVersionManager.isEtcdIncludedInIso(version, ZONE_ID));

        Mockito.verify(templateDetailsDao).addDetail(ISO_ID, KubernetesVersionService.ETCD_INCLUDED_ISO_DETAIL, "true", true);
    }

    @Test
    public void testIsEtcdIncludedInIsoChecksAndStoresWhenEtcdMissing() {
        KubernetesSupportedVersionVO version = mockVersionWithIso();
        EndPoint endPoint = mockIsoReadyOnImageStore();
        Mockito.when(endPoint.sendMessage(Mockito.any(CheckFileInIsoCommand.class)))
                .thenAnswer(invocation -> new CheckFileInIsoAnswer(invocation.getArgument(0), false));

        Assert.assertEquals(Boolean.FALSE, kubernetesVersionManager.isEtcdIncludedInIso(version, ZONE_ID));

        Mockito.verify(templateDetailsDao).addDetail(ISO_ID, KubernetesVersionService.ETCD_INCLUDED_ISO_DETAIL, "false", true);
    }

    @Test
    public void testIsEtcdIncludedInIsoUnknownWhenIsoCouldNotBeRead() {
        KubernetesSupportedVersionVO version = mockVersionWithIso();
        EndPoint endPoint = mockIsoReadyOnImageStore();
        Mockito.when(endPoint.sendMessage(Mockito.any(CheckFileInIsoCommand.class)))
                .thenAnswer(invocation -> new CheckFileInIsoAnswer((CheckFileInIsoCommand)invocation.getArgument(0), "isoinfo: command not found"));

        Assert.assertNull(kubernetesVersionManager.isEtcdIncludedInIso(version, ZONE_ID));

        verifyNothingStored();
    }

    @Test
    public void testIsEtcdIncludedInIsoUnknownWhenAgentDoesNotKnowTheCommand() {
        KubernetesSupportedVersionVO version = mockVersionWithIso();
        EndPoint endPoint = mockIsoReadyOnImageStore();
        Mockito.when(endPoint.sendMessage(Mockito.any(CheckFileInIsoCommand.class)))
                .thenAnswer(invocation -> Answer.createUnsupportedCommandAnswer(invocation.getArgument(0)));

        Assert.assertNull(kubernetesVersionManager.isEtcdIncludedInIso(version, ZONE_ID));

        verifyNothingStored();
    }

    @Test
    public void testIsEtcdIncludedInIsoUnknownWhenNoSecondaryStorageVm() {
        KubernetesSupportedVersionVO version = mockVersionWithIso();
        mockIsoReadyOnImageStore();
        Mockito.when(endPointSelector.select(Mockito.any(DataStore.class))).thenReturn(null);

        Assert.assertNull(kubernetesVersionManager.isEtcdIncludedInIso(version, ZONE_ID));

        verifyNothingStored();
    }

    @Test
    public void testIsEtcdIncludedInIsoUnknownForDirectDownloadIso() {
        KubernetesSupportedVersionVO version = mockVersionWithIso();
        VMTemplateVO iso = Mockito.mock(VMTemplateVO.class);
        Mockito.when(iso.isDirectDownload()).thenReturn(true);
        Mockito.when(templateDao.findById(ISO_ID)).thenReturn(iso);

        Assert.assertNull(kubernetesVersionManager.isEtcdIncludedInIso(version, ZONE_ID));

        Mockito.verifyNoInteractions(endPointSelector);
        verifyNothingStored();
    }

    @Test
    public void testIsEtcdIncludedInIsoReadsOnlyTheCopyInTheZone() {
        KubernetesSupportedVersionVO version = mockVersionWithIso();
        VMTemplateVO iso = Mockito.mock(VMTemplateVO.class);
        Mockito.when(templateDao.findById(ISO_ID)).thenReturn(iso);
        Mockito.when(templateDataStoreDao.findByTemplateZoneReady(ISO_ID, ZONE_ID)).thenReturn(null);

        Assert.assertNull(kubernetesVersionManager.isEtcdIncludedInIso(version, ZONE_ID));

        // A copy of the ISO in another zone is not used
        Mockito.verify(templateDataStoreDao, Mockito.never()).findReadyByTemplate(Mockito.anyLong(), Mockito.any());
        Mockito.verifyNoInteractions(endPointSelector);
        verifyNothingStored();
    }

    @Test
    public void testBackgroundCheckSkipsCheckedIsos() {
        KubernetesSupportedVersionVO version = mockVersionWithIso();
        Mockito.when(kubernetesSupportedVersionDao.listAll()).thenReturn(List.of(version));
        mockStoredEtcdDetail("false");

        kubernetesVersionManager.checkEtcdIncludedInKubernetesIsos();

        Mockito.verifyNoInteractions(endPointSelector);
        verifyNothingStored();
    }

    @Test
    public void testBackgroundCheckStoresResultForUncheckedIso() {
        KubernetesSupportedVersionVO version = mockVersionWithIso();
        // Two versions sharing one ISO are checked once
        Mockito.when(kubernetesSupportedVersionDao.listAll()).thenReturn(List.of(version, version));
        EndPoint endPoint = mockIsoReadyOnImageStore(false);
        Mockito.when(endPoint.sendMessage(Mockito.any(CheckFileInIsoCommand.class)))
                .thenAnswer(invocation -> new CheckFileInIsoAnswer(invocation.getArgument(0), true));

        kubernetesVersionManager.checkEtcdIncludedInKubernetesIsos();

        Mockito.verify(endPoint, Mockito.times(1)).sendMessage(Mockito.any(CheckFileInIsoCommand.class));
        Mockito.verify(templateDetailsDao).addDetail(ISO_ID, KubernetesVersionService.ETCD_INCLUDED_ISO_DETAIL, "true", true);
    }

    @Test
    public void testBackgroundCheckGivesUpAfterRepeatedFailures() {
        KubernetesSupportedVersionVO version = mockVersionWithIso();
        Mockito.when(kubernetesSupportedVersionDao.listAll()).thenReturn(List.of(version));
        EndPoint endPoint = mockIsoReadyOnImageStore(false);
        Mockito.when(endPoint.sendMessage(Mockito.any(CheckFileInIsoCommand.class)))
                .thenAnswer(invocation -> new CheckFileInIsoAnswer((CheckFileInIsoCommand)invocation.getArgument(0), "isoinfo: command not found"));

        for (int i = 0; i < KubernetesVersionManagerImpl.MAX_FAILED_BACKGROUND_ETCD_IN_ISO_CHECKS + 2; i++) {
            kubernetesVersionManager.checkEtcdIncludedInKubernetesIsos();
        }

        Mockito.verify(endPoint, Mockito.times(KubernetesVersionManagerImpl.MAX_FAILED_BACKGROUND_ETCD_IN_ISO_CHECKS))
                .sendMessage(Mockito.any(CheckFileInIsoCommand.class));
        verifyNothingStored();
    }

    @Test
    public void testBackgroundCheckWaitsForIsoToBeReady() {
        KubernetesSupportedVersionVO version = mockVersionWithIso();
        Mockito.when(kubernetesSupportedVersionDao.listAll()).thenReturn(List.of(version));
        VMTemplateVO iso = Mockito.mock(VMTemplateVO.class);
        Mockito.when(templateDao.findById(ISO_ID)).thenReturn(iso);
        Mockito.when(templateDataStoreDao.findReadyByTemplate(ISO_ID, DataStoreRole.Image)).thenReturn(null);

        // An ISO that is still downloading is not counted as a failed check, however long it takes
        for (int i = 0; i < KubernetesVersionManagerImpl.MAX_FAILED_BACKGROUND_ETCD_IN_ISO_CHECKS + 2; i++) {
            kubernetesVersionManager.checkEtcdIncludedInKubernetesIsos();
        }
        Mockito.verifyNoInteractions(endPointSelector);

        EndPoint endPoint = mockIsoReadyOnImageStore(false);
        Mockito.when(endPoint.sendMessage(Mockito.any(CheckFileInIsoCommand.class)))
                .thenAnswer(invocation -> new CheckFileInIsoAnswer(invocation.getArgument(0), true));
        kubernetesVersionManager.checkEtcdIncludedInKubernetesIsos();

        Mockito.verify(templateDetailsDao).addDetail(ISO_ID, KubernetesVersionService.ETCD_INCLUDED_ISO_DETAIL, "true", true);
    }
}
