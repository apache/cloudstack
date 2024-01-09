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
package com.cloud.hypervisor.kvm.resource.wrapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtGetUnmanagedInstancesCommandWrapperTest {

    @Spy
    private LibvirtGetUnmanagedInstancesCommandWrapper wrapper = new LibvirtGetUnmanagedInstancesCommandWrapper();

    @Test
    public void testGetDiskRelativePathNullPath() {
        Assert.assertNull(wrapper.getDiskRelativePath(null));
    }

    @Test
    public void testGetDiskRelativePathWithoutSlashes() {
        String imagePath = UUID.randomUUID().toString();
        Assert.assertEquals(imagePath, wrapper.getDiskRelativePath(imagePath));
    }

    @Test
    public void testGetDiskRelativePathFullPath() {
        String relativePath = "ea4b2296-d349-4968-ab72-c8eb523b556e";
        String imagePath = String.format("/mnt/97e4c9ed-e3bc-3e26-b103-7967fc9feae1/%s", relativePath);
        Assert.assertEquals(relativePath, wrapper.getDiskRelativePath(imagePath));
    }
}
