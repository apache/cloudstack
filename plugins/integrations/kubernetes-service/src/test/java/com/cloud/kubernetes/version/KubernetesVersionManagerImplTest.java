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
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.api.query.dao.TemplateJoinDao;
import com.cloud.api.query.vo.TemplateJoinVO;
import com.cloud.cpu.CPU;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InvalidParameterValueException;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesVersionManagerImplTest {

    @Mock
    TemplateJoinDao templateJoinDao;

    @Mock
    ImageStoreDao imageStoreDao;

    @Mock
    DataCenterDao dataCenterDao;

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
}
