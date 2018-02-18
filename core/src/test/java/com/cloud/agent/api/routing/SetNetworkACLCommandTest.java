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

package com.cloud.agent.api.routing;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.cloud.agent.api.to.NetworkACLTO;
import com.google.common.collect.Lists;

public class SetNetworkACLCommandTest {

    @Test
    public void testNetworkAclRuleOrdering(){

        //given
        List<NetworkACLTO> aclList = Lists.newArrayList();

        aclList.add(new NetworkACLTO(3, null, null, null, null, false, false, null, null, null, null, false, 3));
        aclList.add(new NetworkACLTO(1, null, null, null, null, false, false, null, null, null, null, false, 1));
        aclList.add(new NetworkACLTO(2, null, null, null, null, false, false, null, null, null, null, false, 2));

        SetNetworkACLCommand cmd = new SetNetworkACLCommand(aclList, null);

        //when
        cmd.orderNetworkAclRulesByRuleNumber(aclList);

        //then
        for(int i=0; i< aclList.size();i++){
            assertEquals(aclList.get(i).getNumber(), i+1);
        }
    }
}
