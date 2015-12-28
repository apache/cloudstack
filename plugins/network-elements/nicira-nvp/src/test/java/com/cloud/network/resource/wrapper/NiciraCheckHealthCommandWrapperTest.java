//
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
//

package com.cloud.network.resource.wrapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.network.nicira.ControlClusterStatus;
import com.cloud.network.nicira.NiciraNvpApi;
import com.cloud.network.nicira.NiciraNvpApiException;
import com.cloud.network.resource.NiciraNvpResource;

public class NiciraCheckHealthCommandWrapperTest {

    private final NiciraNvpResource niciraResource = mock(NiciraNvpResource.class);
    private final NiciraNvpApi niciraApi = mock(NiciraNvpApi.class);

    @Before
    public void setup() {
        when(niciraResource.getNiciraNvpApi()).thenReturn(niciraApi);
    }

    @Test
    public void tetsExecuteWhenClusterIsNotStable() throws Exception {
        when(niciraApi.getControlClusterStatus()).thenReturn(new ControlClusterStatus());

        final NiciraCheckHealthCommandWrapper commandWrapper = new NiciraCheckHealthCommandWrapper();
        final Answer answer = commandWrapper.execute(new CheckHealthCommand(), niciraResource);

        assertThat(answer.getResult(), equalTo(false));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void tetsExecuteWhenApiThrowsException() throws Exception {
        when(niciraApi.getControlClusterStatus()).thenThrow(NiciraNvpApiException.class);

        final NiciraCheckHealthCommandWrapper commandWrapper = new NiciraCheckHealthCommandWrapper();
        final Answer answer = commandWrapper.execute(new CheckHealthCommand(), niciraResource);

        assertThat(answer.getResult(), equalTo(false));
    }

    @Test
    public void tetsExecuteWhenClusterIsStable() throws Exception {
        final ControlClusterStatus statusValue = mock(ControlClusterStatus.class);
        when(statusValue.getClusterStatus()).thenReturn("stable");
        when(niciraApi.getControlClusterStatus()).thenReturn(statusValue);

        final NiciraCheckHealthCommandWrapper commandWrapper = new NiciraCheckHealthCommandWrapper();
        final Answer answer = commandWrapper.execute(new CheckHealthCommand(), niciraResource);

        assertThat(answer.getResult(), equalTo(true));
    }

}
