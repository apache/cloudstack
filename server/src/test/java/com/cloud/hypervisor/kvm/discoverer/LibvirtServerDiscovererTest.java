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

package com.cloud.hypervisor.kvm.discoverer;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtServerDiscovererTest {

    @Spy
    private LibvirtServerDiscoverer libvirtServerDiscoverer;

    @Test
    public void validateCompatibleOses() {
        validateCompatibleOs("Rocky Linux", "Rocky Linux", true);
        validateCompatibleOs("Rocky", "Rocky Linux", true);
        validateCompatibleOs("Red", "Red Hat Enterprise Linux", true);
        validateCompatibleOs("Oracle", "Oracle Linux Server", true);
        validateCompatibleOs("Rocky Linux", "Red Hat Enterprise Linux", true);
        validateCompatibleOs("AlmaLinux", "Red Hat Enterprise Linux", true);

        validateCompatibleOs("Windows", "Rocky Linux", false);
        validateCompatibleOs("SUSE", "Rocky Linux", false);
    }

    private void validateCompatibleOs(String hostOsInCluster, String hostOs, boolean expected) {
        if (expected) {
            Assert.assertTrue(libvirtServerDiscoverer.isHostOsCompatibleWithOtherHost(hostOsInCluster, hostOs));
        } else {
            Assert.assertFalse(libvirtServerDiscoverer.isHostOsCompatibleWithOtherHost(hostOsInCluster, hostOs));
        }
    }
}
