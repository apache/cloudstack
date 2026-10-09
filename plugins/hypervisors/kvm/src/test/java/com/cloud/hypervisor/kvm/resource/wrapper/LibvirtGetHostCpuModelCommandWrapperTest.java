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

package com.cloud.hypervisor.kvm.resource.wrapper;

import org.junit.Assert;
import org.junit.Test;

public class LibvirtGetHostCpuModelCommandWrapperTest {

    @Test
    public void extractsHostCpuElementFromCapabilities() {
        String caps = "<capabilities><host><uuid>x</uuid>"
                + "<cpu><arch>x86_64</arch><model>Skylake-Server-IBRS</model><vendor>Intel</vendor></cpu>"
                + "</host><guest></guest></capabilities>";
        Assert.assertEquals(
                "<cpu><arch>x86_64</arch><model>Skylake-Server-IBRS</model><vendor>Intel</vendor></cpu>",
                LibvirtGetHostCpuModelCommandWrapper.extractHostCpu(caps));
    }

    @Test
    public void returnsNullWhenNoCpuElement() {
        Assert.assertNull(LibvirtGetHostCpuModelCommandWrapper.extractHostCpu("<capabilities><host></host></capabilities>"));
        Assert.assertNull(LibvirtGetHostCpuModelCommandWrapper.extractHostCpu(""));
        Assert.assertNull(LibvirtGetHostCpuModelCommandWrapper.extractHostCpu(null));
    }
}
