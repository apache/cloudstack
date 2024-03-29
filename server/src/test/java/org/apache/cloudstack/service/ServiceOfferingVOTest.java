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
package org.apache.cloudstack.service;

import com.cloud.service.ServiceOfferingVO;
import com.cloud.vm.VirtualMachine;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

public class ServiceOfferingVOTest {
    ServiceOfferingVO offeringCustom;
    ServiceOfferingVO offering;
    private AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        offeringCustom = new ServiceOfferingVO("custom", null, null, 500, 10, 10, false, "custom", false, VirtualMachine.Type.User, false);
        offering = new ServiceOfferingVO("normal", 1, 1000, 500, 10, 10, false, "normal", false, VirtualMachine.Type.User, false);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    // Test restoreVm when VM state not in running/stopped case
    @Test
    public void isDynamic() {
        Assert.assertTrue(offeringCustom.isDynamic());
    }

    @Test
    public void notDynamic() {
        Assert.assertTrue(!offering.isDynamic());
    }

}
