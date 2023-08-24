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

import com.cloud.agent.api.to.DatadiskTO;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class LibvirtDownloadCommandWrapperTest {

    private final LibvirtDownloadCommandWrapper wrapper = new LibvirtDownloadCommandWrapper();

    private static final long accountId = 2;
    private static final long templateId = 202;
    private static final String mountPoint = String.format("/mnt/%s", UUID.randomUUID());
    private static final String templateRelativePath = String.format("template/tmpl/%s/%s", accountId, templateId);

    @Test
    public void testRemoveMountPointFromDiskPath() {
        String basePath = String.format("%s/%s", mountPoint, templateRelativePath);
        String disk1Name = "disk1";
        String path1 = String.format("%s/%s", basePath, disk1Name);
        String relativePath = wrapper.removeMountPointFromDiskPath(path1, mountPoint);
        Assert.assertFalse(relativePath.contains(mountPoint));
        String expectedPath = String.format("%s/%s", templateRelativePath, disk1Name);
        Assert.assertEquals(expectedPath, relativePath);
    }

    @Test
    public void testRemoveMountPointFromDiskPathTemplateDir() {
        String disk1Name = "disk1";
        String path1 = String.format("%s/%s", templateRelativePath, disk1Name);
        String relativePath = wrapper.removeMountPointFromDiskPath(path1, templateRelativePath);
        Assert.assertFalse(relativePath.contains(templateRelativePath));
        Assert.assertEquals(disk1Name, relativePath);
    }

    @Test
    public void testVMDataDisksMultidisk() {
        String basePath = String.format("%s/%s", mountPoint, templateRelativePath);
        String path1 = String.format("%s/%s", basePath, "disk1");
        String path2 = String.format("%s/%s", basePath, "disk2");
        long size1 = 111111;
        long fileSize1 = 111111111;
        long size2 = 222222;
        long fileSize2 = 22222222;

        DatadiskTO disk1 = new DatadiskTO(path1, size1, fileSize1, 0);
        DatadiskTO disk2 = new DatadiskTO(path2, size2, fileSize2, 1);
        List<DatadiskTO> vmDisks = Arrays.asList(disk1, disk2);
        List<DatadiskTO> dataDisks = wrapper.getVMDataDisks(vmDisks);
        Assert.assertEquals(1, dataDisks.size());
        Assert.assertEquals(dataDisks.get(0).getPath(), path2);
    }

    @Test
    public void testVMDataDisksSingleDisk() {
        String basePath = String.format("%s/%s", mountPoint, templateRelativePath);
        String path1 = String.format("%s/%s", basePath, "disk1");
        long size1 = 111111;
        long fileSize1 = 111111111;

        DatadiskTO disk1 = new DatadiskTO(path1, size1, fileSize1, 0);
        List<DatadiskTO> vmDisks = List.of(disk1);
        List<DatadiskTO> dataDisks = wrapper.getVMDataDisks(vmDisks);
        Assert.assertTrue(CollectionUtils.isEmpty(dataDisks));
    }
}
