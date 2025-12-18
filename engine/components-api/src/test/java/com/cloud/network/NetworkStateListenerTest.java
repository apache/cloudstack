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

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.events.EventDistributor;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

public class NetworkStateListenerTest {
    @InjectMocks
    NetworkStateListener networkStateListener = new NetworkStateListener(Mockito.mock(ConfigurationDao.class));

    @Test
    public void testSetEventDistributor() {
        EventDistributor eventDistributor = null;
        networkStateListener.setEventDistributor(eventDistributor);
        Assert.assertNull(ReflectionTestUtils.getField(networkStateListener, "eventDistributor"));
        eventDistributor = Mockito.mock(EventDistributor.class);
        networkStateListener.setEventDistributor(eventDistributor);
        Assert.assertEquals(eventDistributor, ReflectionTestUtils.getField(networkStateListener, "eventDistributor"));
    }
}
