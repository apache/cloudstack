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

package org.apache.cloudstack.storage.sharedfs.query.dao;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.SharedFSResponse;
import org.apache.cloudstack.storage.sharedfs.SharedFS;
import org.apache.cloudstack.storage.sharedfs.SharedFSVO;
import org.apache.cloudstack.storage.sharedfs.query.vo.SharedFSJoinVO;
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
public class SharedFSJoinDaoImplTest {
    @Mock
    NicDao nicDao;

    @Mock
    NetworkDao networkDao;

    @Mock
    private VmDiskStatisticsDao vmDiskStatsDao;

    @Spy
    @InjectMocks
    SharedFSJoinDaoImpl sharedFSJoinDao;

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
    public void testNewSharedFSView() {
        SharedFSVO sharedfs = mock(SharedFSVO.class);
        Long id = 1L;
        when(sharedfs.getId()).thenReturn(id);
        SharedFSJoinVO sharedFSJoinVO = mock(SharedFSJoinVO.class);

        SearchBuilder<SharedFSVO> sb = Mockito.mock(SearchBuilder.class);
        ReflectionTestUtils.setField(sharedFSJoinDao, "fsSearch", sb);
        SearchCriteria<SharedFSVO> sc = Mockito.mock(SearchCriteria.class);
        Mockito.when(sb.create()).thenReturn(sc);
        Mockito.doReturn(List.of(sharedFSJoinVO)).when(sharedFSJoinDao).searchIncludingRemoved(
                Mockito.any(SearchCriteria.class), Mockito.eq(null), Mockito.eq(null),
                Mockito.eq(false));

        sharedFSJoinDao.newSharedFSView(sharedfs);

        Mockito.verify(sc).setParameters("id", id);
        Mockito.verify(sharedFSJoinDao, Mockito.times(1)).searchIncludingRemoved(
                Mockito.any(SearchCriteria.class), Mockito.eq(null), Mockito.eq(null),
                Mockito.eq(false));
    }

    @Test
    public void newSharedFSResponse() {
        Long s_ownerId = 1L;
        Long s_zoneId = 2L;
        Long s_volumeId = 3L;
        Long s_vmId = 4L;
        Long s_networkId = 5L;
        String s_fsFormat = "EXT4";
        SharedFS.State state = SharedFS.State.Ready;
        VirtualMachine.State vmState = VirtualMachine.State.Running;
        Storage.ProvisioningType provisioningType = Storage.ProvisioningType.THIN;

        SharedFSJoinVO sharedFSJoinVO = mock(SharedFSJoinVO.class);
        when(sharedFSJoinVO.getAccountId()).thenReturn(s_ownerId);
        when(sharedFSJoinVO.getZoneId()).thenReturn(s_zoneId);
        when(sharedFSJoinVO.getVolumeId()).thenReturn(s_volumeId);
        when(sharedFSJoinVO.getInstanceId()).thenReturn(s_vmId);
        when(sharedFSJoinVO.getState()).thenReturn(state);
        when(sharedFSJoinVO.getFsType()).thenReturn(SharedFS.FileSystemType.valueOf(s_fsFormat));
        when(sharedFSJoinVO.getInstanceState()).thenReturn(vmState);
        when(sharedFSJoinVO.getProvisioningType()).thenReturn(provisioningType);

        NicVO nic = mock(NicVO.class);
        NetworkVO network = mock(NetworkVO.class);
        when(nic.getNetworkId()).thenReturn(s_networkId);
        when(nicDao.listByVmId(s_vmId)).thenReturn(List.of(nic));
        when(networkDao.findById(s_networkId)).thenReturn(network);

        VmDiskStatisticsVO diskStats = mock(VmDiskStatisticsVO.class);
        when(vmDiskStatsDao.findBy(s_ownerId, s_zoneId, s_vmId, s_volumeId)).thenReturn(diskStats);

        VolumeStats vs = mock(VolumeStats.class);
        String path = "volumepath";
        when(sharedFSJoinVO.getVolumeFormat()).thenReturn(Storage.ImageFormat.QCOW2);
        when(sharedFSJoinVO.getVolumePath()).thenReturn(path);

        try (MockedStatic<ApiDBUtils> apiDBUtilsMocked = Mockito.mockStatic(ApiDBUtils.class)) {
            when(ApiDBUtils.getVolumeStatistics(path)).thenReturn(vs);
            SharedFSResponse response = sharedFSJoinDao.newSharedFSResponse(ResponseObject.ResponseView.Restricted, sharedFSJoinVO);
            Assert.assertEquals(ReflectionTestUtils.getField(response, "state"), state.toString());
            Assert.assertEquals(ReflectionTestUtils.getField(response, "virtualMachineState"), vmState.toString());
            Assert.assertEquals(ReflectionTestUtils.getField(response, "provisioningType"), provisioningType.toString());
        }

    }
}
