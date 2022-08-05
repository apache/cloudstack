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
package com.cloud.storage.dao;

import com.cloud.storage.VolumeVO;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class VolumeDaoImplTest {

    @Mock
    VolumeVO volumeVoMock;

    @Mock
    VolumeVO alternateVolumeVoMock;

    @Mock
    Logger loggerMock;

    @Spy
    @InjectMocks
    VolumeDaoImpl volumeDaoSpy;

    @Test
    public void getInstanceRootVolumeTestReturningOneVolume() {
        List<VolumeVO> volumeVOList = Collections.singletonList(volumeVoMock);
        Mockito.doReturn(volumeVOList).when(volumeDaoSpy).findRootVolumesByInstance(1234);
        VolumeVO actualVo = volumeDaoSpy.getInstanceRootVolume(1234, "test-uuid");
        Assert.assertEquals(volumeVoMock, actualVo);
    }

    @Test
    public void getInstanceRootVolumeTestReturningMoreThanOneVolume() {
        List<VolumeVO> volumeVOList = Arrays.asList(volumeVoMock, alternateVolumeVoMock);
        Mockito.doReturn(volumeVOList).when(volumeDaoSpy).findRootVolumesByInstance(1234);
        VolumeVO actualVo = volumeDaoSpy.getInstanceRootVolume(1234, "test-uuid");
        Mockito.verify(loggerMock).warn("More than one ROOT volume has been found for VM [test-uuid], there is probably an inconsistency in the database.");
        Assert.assertEquals(volumeVoMock, actualVo);
    }

    @Test (expected = CloudRuntimeException.class)
    public void getInstanceRootVolumeTestReturningNoVolumes() {
        List<VolumeVO> volumeVOList = new ArrayList<>();
        Mockito.doReturn(volumeVOList).when(volumeDaoSpy).findRootVolumesByInstance(1234);
        volumeDaoSpy.getInstanceRootVolume(1234, "test-uuid");
    }
}
