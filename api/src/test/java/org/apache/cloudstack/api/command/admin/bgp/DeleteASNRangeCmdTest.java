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

package org.apache.cloudstack.api.command.admin.bgp;

import com.cloud.bgp.BGPService;

import org.apache.cloudstack.api.response.SuccessResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class DeleteASNRangeCmdTest {

    BGPService bgpService = Mockito.spy(BGPService.class);

    @Test
    public void testDeleteASNRangeCmd() {
        Long id = 200L;

        DeleteASNRangeCmd cmd = new DeleteASNRangeCmd();
        ReflectionTestUtils.setField(cmd, "id", id);
        ReflectionTestUtils.setField(cmd,"bgpService", bgpService);

        Assert.assertEquals(id, cmd.getId());

        Mockito.when(bgpService.deleteASRange(id)).thenReturn(true);
        try {
            cmd.execute();
        } catch (Exception ignored) {
        }

        Object response = cmd.getResponseObject();
        Assert.assertTrue(response instanceof SuccessResponse);

    }
}
