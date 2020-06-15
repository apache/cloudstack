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

package com.cloud.hypervisor.vmware.resource;

import com.cloud.agent.api.to.NicTO;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;

import static org.junit.Assert.assertEquals;

public class StartCommandExecutorTest {

    @Spy
    @InjectMocks
    VmwareResource resource = new VmwareResource();

    @Spy
    @InjectMocks
    StartCommandExecutor starter = new StartCommandExecutor(resource);

    @Test
    public void generateMacSequence() {
        final NicTO nicTo1 = new NicTO();
        nicTo1.setMac("01:23:45:67:89:AB");
        nicTo1.setDeviceId(1);

        final NicTO nicTo2 = new NicTO();
        nicTo2.setMac("02:00:65:b5:00:03");
        nicTo2.setDeviceId(0);

        //final NicTO [] nicTOs = {nicTO1, nicTO2, nicTO3};
        //final NicTO[] nics = new NicTO[]{nic};
        final NicTO[] nics = new NicTO[] {nicTo1, nicTo2};

        String macSequence = starter.generateMacSequence(nics);
        assertEquals(macSequence, "02:00:65:b5:00:03|01:23:45:67:89:AB");
    }
}
