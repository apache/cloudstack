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

package org.apache.cloudstack.api.command.admin.vpc;

import com.cloud.network.vpc.VpcService;
import com.cloud.user.AccountService;
import com.cloud.utils.db.EntityManager;
import junit.framework.TestCase;
import org.apache.cloudstack.api.ResponseGenerator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class CreateVPCCmdByAdminTest extends TestCase {

    @Mock
    public VpcService _vpcService;
    @Mock
    public EntityManager _entityMgr;
    @Mock
    public AccountService _accountService;
    private ResponseGenerator responseGenerator;
    @InjectMocks
    CreateVPCCmdByAdmin cmd = new CreateVPCCmdByAdmin();

    @Test
    public void testBgpPeerIds() {
        List<Long> bgpPeerIds = Mockito.mock(List.class);
        ReflectionTestUtils.setField(cmd, "bgpPeerIds", bgpPeerIds);
        Assert.assertEquals(bgpPeerIds, cmd.getBgpPeerIds());
    }
}
