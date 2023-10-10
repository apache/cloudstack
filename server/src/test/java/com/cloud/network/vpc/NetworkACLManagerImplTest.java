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

package com.cloud.network.vpc;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.vpc.NetworkACLItem.State;
import com.cloud.utils.exception.CloudRuntimeException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NetworkACLManagerImplTest {

    @Spy
    @InjectMocks
    private NetworkACLManagerImpl networkACLManagerImpl;
    @Mock
    private NetworkACLItemDao networkACLItemDaoMock;

    @Test(expected = CloudRuntimeException.class)
    public void updateNetworkACLItemTestUpdateDoesNotWork() throws ResourceUnavailableException {
        NetworkACLItemVO networkACLItemVOMock = new NetworkACLItemVO();
        networkACLItemVOMock.id = 1L;

        Mockito.doReturn(false).when(networkACLItemDaoMock).update(1L, networkACLItemVOMock);

        networkACLManagerImpl.updateNetworkACLItem(networkACLItemVOMock);
    }

    @Test(expected = CloudRuntimeException.class)
    public void updateNetworkACLItemTestUpdateWorksButApplyDoesNotWork() throws ResourceUnavailableException {
        NetworkACLItemVO networkACLItemVOMock = new NetworkACLItemVO();
        networkACLItemVOMock.id = 1L;
        networkACLItemVOMock.aclId = 2L;

        Mockito.doReturn(true).when(networkACLItemDaoMock).update(1L, networkACLItemVOMock);
        Mockito.doReturn(false).when(networkACLManagerImpl).applyNetworkACL(2L);

        networkACLManagerImpl.updateNetworkACLItem(networkACLItemVOMock);
    }

    @Test
    public void updateNetworkACLItemTestHappyDay() throws ResourceUnavailableException {
        NetworkACLItemVO networkACLItemVOMock = new NetworkACLItemVO();
        networkACLItemVOMock.id = 1L;
        networkACLItemVOMock.aclId = 2L;

        Mockito.doReturn(true).when(networkACLItemDaoMock).update(1L, networkACLItemVOMock);
        Mockito.doReturn(true).when(networkACLManagerImpl).applyNetworkACL(2L);

        NetworkACLItem returnedNetworkAclItem = networkACLManagerImpl.updateNetworkACLItem(networkACLItemVOMock);

        Mockito.verify(networkACLItemDaoMock).update(1L, networkACLItemVOMock);
        Mockito.verify(networkACLManagerImpl).applyNetworkACL(2L);

        Assert.assertEquals(networkACLItemVOMock, returnedNetworkAclItem);
        Assert.assertEquals(State.Add, returnedNetworkAclItem.getState());
    }
}
