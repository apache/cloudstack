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
package com.cloud.host;

import com.cloud.resource.ResourceState;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;

public class ControlStateTest extends TestCase {

    void verifyHostControlState(Status hostStatus, ResourceState hostResourceState, ControlState expectedControlState) {
        Assert.assertEquals(expectedControlState, ControlState.getControlState(hostStatus, hostResourceState));
    }

    @Test
    public void testHostControlState() {
        // Unknown state
        verifyHostControlState(null, null, ControlState.Unknown);
        verifyHostControlState(null, ResourceState.Enabled, ControlState.Unknown);
        verifyHostControlState(Status.Up, null, ControlState.Unknown);
        verifyHostControlState(Status.Unknown, ResourceState.Enabled, ControlState.Unknown);

        // Host is Up and Enabled
        verifyHostControlState(Status.Creating, ResourceState.Enabled, ControlState.Enabled);
        verifyHostControlState(Status.Connecting, ResourceState.Enabled, ControlState.Enabled);
        verifyHostControlState(Status.Up, ResourceState.Enabled, ControlState.Enabled);

        // Host is Up and Disabled
        verifyHostControlState(Status.Up, ResourceState.Creating, ControlState.Disabled);
        verifyHostControlState(Status.Up, ResourceState.Disabled, ControlState.Disabled);
        verifyHostControlState(Status.Up, ResourceState.Error, ControlState.Disabled);
        verifyHostControlState(Status.Up, ResourceState.Degraded, ControlState.Disabled);

        // Host is Up and Maintenance mode
        verifyHostControlState(Status.Up, ResourceState.ErrorInPrepareForMaintenance, ControlState.Maintenance);
        verifyHostControlState(Status.Up, ResourceState.PrepareForMaintenance, ControlState.Maintenance);
        verifyHostControlState(Status.Up, ResourceState.ErrorInMaintenance, ControlState.Maintenance);
        verifyHostControlState(Status.Up, ResourceState.Maintenance, ControlState.Maintenance);

        // Host in other states
        verifyHostControlState(Status.Down, ResourceState.Enabled, ControlState.Offline);
        verifyHostControlState(Status.Disconnected, ResourceState.Enabled, ControlState.Offline);
        verifyHostControlState(Status.Alert, ResourceState.Enabled, ControlState.Offline);
        verifyHostControlState(Status.Removed, ResourceState.Enabled, ControlState.Offline);
        verifyHostControlState(Status.Error, ResourceState.Enabled, ControlState.Offline);
        verifyHostControlState(Status.Rebalancing, ResourceState.Enabled, ControlState.Offline);
    }

}
