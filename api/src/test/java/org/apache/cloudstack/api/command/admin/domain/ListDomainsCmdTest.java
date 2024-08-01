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
package org.apache.cloudstack.api.command.admin.domain;

import java.util.List;

import org.apache.cloudstack.api.response.DomainResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.user.ResourceLimitService;

@RunWith(MockitoJUnitRunner.class)
public class ListDomainsCmdTest {

    @Mock
    ResourceLimitService resourceLimitService;


    @Test
    public void testGetShowIcon() {
        ListDomainsCmd cmd = new ListDomainsCmd();
        ReflectionTestUtils.setField(cmd, "showIcon", null);
        Assert.assertFalse(cmd.getShowIcon());
        ReflectionTestUtils.setField(cmd, "showIcon", false);
        Assert.assertFalse(cmd.getShowIcon());
        ReflectionTestUtils.setField(cmd, "showIcon", true);
        Assert.assertTrue(cmd.getShowIcon());
    }

    @Test
    public void testGetTag() {
        ListDomainsCmd cmd = new ListDomainsCmd();
        ReflectionTestUtils.setField(cmd, "tag", null);
        Assert.assertNull(cmd.getTag());
        String tag = "ABC";
        ReflectionTestUtils.setField(cmd, "tag", tag);
        Assert.assertEquals(tag, cmd.getTag());
    }

    @Test
    public void testUpdateDomainResponseNoDomains() {
        ListDomainsCmd cmd = new ListDomainsCmd();
        cmd._resourceLimitService = resourceLimitService;
        cmd.updateDomainResponse(null);
        Mockito.verify(resourceLimitService, Mockito.never()).updateTaggedResourceLimitsAndCountsForDomains(Mockito.anyList(), Mockito.anyString());
    }

    @Test
    public void testUpdateDomainResponseWithDomains() {
        ListDomainsCmd cmd = new ListDomainsCmd();
        cmd._resourceLimitService = resourceLimitService;
        ReflectionTestUtils.setField(cmd, "tag", "abc");
        cmd.updateDomainResponse(List.of(Mockito.mock(DomainResponse.class)));
        Mockito.verify(resourceLimitService, Mockito.times(1)).updateTaggedResourceLimitsAndCountsForDomains(Mockito.any(), Mockito.any());
    }

}
