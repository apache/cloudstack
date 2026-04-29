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

import com.cloud.bgp.ASNumberRange;
import com.cloud.bgp.BGPService;

import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.response.ASNRangeResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ListASNRangesCmdTest {

    BGPService bgpService = Mockito.spy(BGPService.class);
    ResponseGenerator _responseGenerator = Mockito.spy(ResponseGenerator.class);

    @Test
    public void testListASNRangesCmdTest() {
        Long zoneId = 1L;

        ListASNRangesCmd cmd = new ListASNRangesCmd();
        ReflectionTestUtils.setField(cmd, "zoneId", zoneId);
        ReflectionTestUtils.setField(cmd,"bgpService", bgpService);
        ReflectionTestUtils.setField(cmd,"_responseGenerator", _responseGenerator);

        Assert.assertEquals(zoneId, cmd.getZoneId());
        Assert.assertEquals(1L, cmd.getEntityOwnerId());

        List<ASNumberRange> ranges = new ArrayList<>();
        ASNumberRange asnRange = Mockito.mock(ASNumberRange.class);
        ranges.add(asnRange);

        ASNRangeResponse asnRangeResponse = Mockito.mock(ASNRangeResponse.class);
        Mockito.when(_responseGenerator.createASNumberRangeResponse(asnRange)).thenReturn(asnRangeResponse);

        Mockito.when(bgpService.listASNumberRanges(zoneId)).thenReturn(ranges);
        try {
            cmd.execute();
        } catch (Exception ignored) {
        }

        Object response = cmd.getResponseObject();
        Assert.assertTrue(response instanceof ListResponse);
        ListResponse listResponse = (ListResponse) response;
        Assert.assertEquals(1L, (long) listResponse.getCount());
        Assert.assertTrue(listResponse.getResponses().get(0) instanceof ASNRangeResponse);
        ASNRangeResponse firstResponse = (ASNRangeResponse) listResponse.getResponses().get(0);
        Assert.assertEquals(asnRangeResponse, firstResponse);
    }
}
