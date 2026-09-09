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
package org.apache.cloudstack.outofbandmanagement;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.junit.Assert;
import org.junit.Test;

import com.cloud.host.Host;

public class PowerOperationTaskTest {

    @Test
    public void testTaskRunsWithinManagedContextAndExecutesOperation() {
        OutOfBandManagementService service = mock(OutOfBandManagementService.class);
        Host host = mock(Host.class);
        PowerOperationTask task = new PowerOperationTask(service, host, OutOfBandManagement.PowerOperation.STATUS);

        Assert.assertTrue("Background task must run within a managed context so its DB connection is released to the pool",
                task instanceof ManagedContextRunnable);

        task.run();

        verify(service).executePowerOperation(host, OutOfBandManagement.PowerOperation.STATUS, null);
    }
}
