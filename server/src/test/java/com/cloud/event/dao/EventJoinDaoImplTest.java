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

package com.cloud.event.dao;

import com.cloud.api.query.vo.EventJoinVO;
import com.cloud.utils.db.EntityManager;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.response.EventResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class EventJoinDaoImplTest {

    @Mock
    protected EntityManager entityManager;

    @InjectMocks
    private EventJoinDaoImpl dao = new EventJoinDaoImpl();

    @Test
    public void testNewEventViewResource() {
        final Long resourceId = 1L;
        final String resourceType = ApiCommandResourceType.VirtualMachine.toString();
        final String resourceUuid = UUID.randomUUID().toString();
        final String resourceName = "TestVM";
        EventJoinVO event = Mockito.mock(EventJoinVO.class);
        Mockito.when(event.getResourceId()).thenReturn(resourceId);
        Mockito.when(event.getResourceType()).thenReturn(resourceType);
        Mockito.when(event.getResourceType()).thenReturn(resourceType);
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(vm.getUuid()).thenReturn(resourceUuid);
        Mockito.when(vm.getHostName()).thenReturn(null);
        Mockito.when(vm.getName()).thenReturn(resourceName);
        Mockito.when(entityManager.validEntityType(VirtualMachine.class)).thenReturn(true);
        Mockito.when(entityManager.findByIdIncludingRemoved(VirtualMachine.class, resourceId)).thenReturn(vm);
        EventResponse response = dao.newEventResponse(event);
        Assert.assertEquals(response.getResourceId(), resourceUuid);
        Assert.assertEquals(response.getResourceType(), resourceType);
        Assert.assertEquals(response.getResourceName(), resourceName);
    }
}
