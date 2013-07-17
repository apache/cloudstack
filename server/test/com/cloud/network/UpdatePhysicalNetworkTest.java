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
package com.cloud.network;
import com.cloud.capacity.CapacityManagerImpl;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterVnetDao;
import com.cloud.network.NetworkServiceImpl;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Transaction;
import org.junit.Test;
import org.junit.*;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class UpdatePhysicalNetworkTest {
    private PhysicalNetworkDao _physicalNetworkDao = mock(PhysicalNetworkDao.class);
    private DataCenterVnetDao _DatacenterVnetDao = mock(DataCenterVnetDao.class);
    private DataCenterDao _datacenterDao = mock(DataCenterDao.class);
    private DataCenterVO datacentervo = mock(DataCenterVO.class);
    private PhysicalNetworkVO physicalNetworkVO = mock(PhysicalNetworkVO.class);
    List<String> existingRange = new ArrayList<String>();
    ArgumentCaptor<String> argumentCaptor =  ArgumentCaptor.forClass(String.class);

    public NetworkServiceImpl setUp() {
        NetworkServiceImpl networkService = new NetworkServiceImpl();
        ((NetworkServiceImpl)networkService)._dcDao= _datacenterDao;
        networkService._physicalNetworkDao = _physicalNetworkDao;
        networkService._datacneter_vnet = _DatacenterVnetDao;
        return networkService;
    }

    @Test
    public void updatePhysicalNetworkTest(){
        Transaction txn = Transaction.open("updatePhysicalNetworkTest");
        NetworkServiceImpl networkService = setUp();
        existingRange.add("524");
        when(_physicalNetworkDao.findById(anyLong())).thenReturn(physicalNetworkVO);
        when(_datacenterDao.findById(anyLong())).thenReturn(datacentervo);
        when(_physicalNetworkDao.update(anyLong(), any(physicalNetworkVO.getClass()))).thenReturn(true);
        when(_DatacenterVnetDao.listVnetsByPhysicalNetworkAndDataCenter(anyLong(), anyLong())).thenReturn(existingRange);
        networkService.updatePhysicalNetwork(1l, null, null, "525-530", null, null);
        txn.close("updatePhysicalNetworkTest");
        verify(physicalNetworkVO).setVnet(argumentCaptor.capture());
        assertEquals("524-530", argumentCaptor.getValue());
    }

}
