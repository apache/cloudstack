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
package com.cloud.api.query.dao;

import com.cloud.api.query.vo.VolumeJoinVO;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class VolumeJoinDaoImplTest extends GenericDaoBaseWithTagInformationBaseTest<VolumeJoinVO, VolumeResponse> {

    @InjectMocks
    private VolumeJoinDaoImpl _volumeJoinDaoImpl;

    private VolumeJoinVO volume = new VolumeJoinVO();
    private VolumeResponse volumeResponse = new VolumeResponse();

    @Before
    public void setup() {
        prepareSetup();
    }

    @Test
    public void testUpdateVolumeTagInfo(){
        testUpdateTagInformation(_volumeJoinDaoImpl, volume, volumeResponse);
    }

    @Test
    public void testSetThrottleRatesMapsReadAndWriteDistinctly() {
        VolumeJoinVO vol = Mockito.mock(VolumeJoinVO.class);
        Mockito.when(vol.getBytesReadRate()).thenReturn(100L);
        Mockito.when(vol.getBytesWriteRate()).thenReturn(200L);
        Mockito.when(vol.getIopsReadRate()).thenReturn(300L);
        Mockito.when(vol.getIopsWriteRate()).thenReturn(400L);

        VolumeResponse response = new VolumeResponse();
        _volumeJoinDaoImpl.setThrottleRates(response, vol);

        Assert.assertEquals(Long.valueOf(100L), response.getBytesReadRate());
        Assert.assertEquals(Long.valueOf(200L), response.getBytesWriteRate());
        Assert.assertEquals(Long.valueOf(300L), response.getIopsReadRate());
        Assert.assertEquals(Long.valueOf(400L), response.getIopsWriteRate());
    }

}
