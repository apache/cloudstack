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

package org.apache.cloudstack.storage.fileshare.query.dao;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.FileShareResponse;
import org.apache.cloudstack.storage.fileshare.FileShare;
import org.apache.cloudstack.storage.fileshare.FileShareVO;
import org.apache.cloudstack.storage.fileshare.query.vo.FileShareJoinVO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.api.ApiDBUtils;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.storage.Storage;
import com.cloud.storage.VolumeStats;
import com.cloud.user.VmDiskStatisticsVO;
import com.cloud.user.dao.VmDiskStatisticsDao;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.NicVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.NicDao;

@RunWith(MockitoJUnitRunner.class)
public class FileShareJoinDaoImplTest {
    @Mock
    NicDao nicDao;

    @Mock
    NetworkDao networkDao;

    @Mock
    private VmDiskStatisticsDao vmDiskStatsDao;

    @Spy
    @InjectMocks
    FileShareJoinDaoImpl fileShareJoinDao;

    private AutoCloseable closeable;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testNewFileShareView() {
        FileShareVO fileshare = mock(FileShareVO.class);
        Long id = 1L;
        when(fileshare.getId()).thenReturn(id);
        FileShareJoinVO fileShareJoinVO = mock(FileShareJoinVO.class);

        SearchBuilder<FileShareVO> sb = Mockito.mock(SearchBuilder.class);
        ReflectionTestUtils.setField(fileShareJoinDao, "fsSearch", sb);
        SearchCriteria<FileShareVO> sc = Mockito.mock(SearchCriteria.class);
        Mockito.when(sb.create()).thenReturn(sc);
        Mockito.doReturn(List.of(fileShareJoinVO)).when(fileShareJoinDao).searchIncludingRemoved(
                Mockito.any(SearchCriteria.class), Mockito.eq(null), Mockito.eq(null),
                Mockito.eq(false));

        fileShareJoinDao.newFileShareView(fileshare);

        Mockito.verify(sc).setParameters("id", id);
        Mockito.verify(fileShareJoinDao, Mockito.times(1)).searchIncludingRemoved(
                Mockito.any(SearchCriteria.class), Mockito.eq(null), Mockito.eq(null),
                Mockito.eq(false));
    }

    @Test
    public void newFileShareResponse() {
        Long s_ownerId = 1L;
        Long s_zoneId = 2L;
        Long s_volumeId = 3L;
        Long s_vmId = 4L;
        Long s_networkId = 5L;
        String s_fsFormat = "EXT4";
        FileShare.State state = FileShare.State.Ready;
        VirtualMachine.State vmState = VirtualMachine.State.Running;
        Storage.ProvisioningType provisioningType = Storage.ProvisioningType.THIN;

        FileShareJoinVO fileShareJoinVO = mock(FileShareJoinVO.class);
        when(fileShareJoinVO.getAccountId()).thenReturn(s_ownerId);
        when(fileShareJoinVO.getZoneId()).thenReturn(s_zoneId);
        when(fileShareJoinVO.getVolumeId()).thenReturn(s_volumeId);
        when(fileShareJoinVO.getInstanceId()).thenReturn(s_vmId);
        when(fileShareJoinVO.getState()).thenReturn(state);
        when(fileShareJoinVO.getFsType()).thenReturn(FileShare.FileSystemType.valueOf(s_fsFormat));
        when(fileShareJoinVO.getInstanceState()).thenReturn(vmState);
        when(fileShareJoinVO.getProvisioningType()).thenReturn(provisioningType);

        NicVO nic = mock(NicVO.class);
        NetworkVO network = mock(NetworkVO.class);
        when(nic.getNetworkId()).thenReturn(s_networkId);
        when(nicDao.listByVmId(s_vmId)).thenReturn(List.of(nic));
        when(networkDao.findById(s_networkId)).thenReturn(network);

        VmDiskStatisticsVO diskStats = mock(VmDiskStatisticsVO.class);
        when(vmDiskStatsDao.findBy(s_ownerId, s_zoneId, s_vmId, s_volumeId)).thenReturn(diskStats);

        VolumeStats vs = mock(VolumeStats.class);
        String path = "volumepath";
        when(fileShareJoinVO.getVolumeFormat()).thenReturn(Storage.ImageFormat.QCOW2);
        when(fileShareJoinVO.getVolumePath()).thenReturn(path);

        try (MockedStatic<ApiDBUtils> apiDBUtilsMocked = Mockito.mockStatic(ApiDBUtils.class)) {
            when(ApiDBUtils.getVolumeStatistics(path)).thenReturn(vs);
            FileShareResponse response = fileShareJoinDao.newFileShareResponse(ResponseObject.ResponseView.Restricted, fileShareJoinVO);
            Assert.assertEquals(ReflectionTestUtils.getField(response, "state"), state.toString());
            Assert.assertEquals(ReflectionTestUtils.getField(response, "virtualMachineState"), vmState.toString());
            Assert.assertEquals(ReflectionTestUtils.getField(response, "provisioningType"), provisioningType.toString());
        }

    }
}
