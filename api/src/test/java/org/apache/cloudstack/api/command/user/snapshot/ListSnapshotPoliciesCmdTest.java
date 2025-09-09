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
package org.apache.cloudstack.api.command.user.snapshot;

import com.cloud.storage.snapshot.SnapshotApiService;
import com.cloud.storage.snapshot.SnapshotPolicy;
import com.cloud.utils.Pair;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.SnapshotPolicyResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

public class ListSnapshotPoliciesCmdTest {
    private ListSnapshotPoliciesCmd cmd;
    private SnapshotApiService snapshotService;
    private ResponseGenerator responseGenerator;

    @Before
    public void setUp() {
        cmd = new ListSnapshotPoliciesCmd();
        snapshotService = Mockito.mock(SnapshotApiService.class);
        responseGenerator = Mockito.mock(ResponseGenerator.class);

        cmd._snapshotService = snapshotService;
        cmd._responseGenerator = responseGenerator;
    }

    @Test
    public void testExecuteWithPolicies() {
        SnapshotPolicy policy = Mockito.mock(SnapshotPolicy.class);
        SnapshotPolicyResponse policyResponse = Mockito.mock(SnapshotPolicyResponse.class);
        List<SnapshotPolicy> policies = new ArrayList<>();
        policies.add(policy);

        Mockito.when(snapshotService.listSnapshotPolicies(cmd))
                .thenReturn(new Pair<>(policies, 1));
        Mockito.when(responseGenerator.createSnapshotPolicyResponse(policy))
                .thenReturn(policyResponse);

        cmd.execute();

        ListResponse<?> response = (ListResponse<?>) cmd.getResponseObject();
        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.getResponses().size());
        Assert.assertEquals(policyResponse, response.getResponses().get(0));
    }

    @Test
    public void testExecuteWithNoPolicies() {
        Mockito.when(snapshotService.listSnapshotPolicies(cmd))
                .thenReturn(new Pair<>(new ArrayList<>(), 0));

        cmd.execute();

        ListResponse<?> response = (ListResponse<?>) cmd.getResponseObject();
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getResponses().isEmpty());
    }
}
