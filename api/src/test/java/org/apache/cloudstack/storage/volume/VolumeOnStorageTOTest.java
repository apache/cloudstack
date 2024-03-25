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
package org.apache.cloudstack.storage.volume;

import com.cloud.hypervisor.Hypervisor;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class VolumeOnStorageTOTest {

    private static String path = "path";
    private static String name = "name";
    private static String fullPath = "fullPath";
    private static String format = "qcow2";
    private static long size = 10;
    private static long virtualSize = 20;
    private static String encryptFormat = "LUKS";
    private static Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
    private static String BACKING_FILE = "backing file";
    private static String BACKING_FILE_FORMAT = "qcow2";

    @Test
    public void testVolumeOnStorageTO() {
        VolumeOnStorageTO volumeOnStorageTO = new VolumeOnStorageTO(hypervisorType, path, name, fullPath,
                format, size, virtualSize);

        Assert.assertEquals(hypervisorType, volumeOnStorageTO.getHypervisorType());
        Assert.assertEquals(path, volumeOnStorageTO.getPath());
        Assert.assertEquals(name, volumeOnStorageTO.getName());
        Assert.assertEquals(fullPath, volumeOnStorageTO.getFullPath());
        Assert.assertEquals(format, volumeOnStorageTO.getFormat());
        Assert.assertEquals(size, volumeOnStorageTO.getSize());
        Assert.assertEquals(virtualSize, volumeOnStorageTO.getVirtualSize());
    }

    @Test
    public void testVolumeOnStorageTO3() {
        VolumeOnStorageTO volumeOnStorageTO = new VolumeOnStorageTO();
        volumeOnStorageTO.setHypervisorType(hypervisorType);
        volumeOnStorageTO.setPath(path);
        volumeOnStorageTO.setFullPath(fullPath);
        volumeOnStorageTO.setName(name);
        volumeOnStorageTO.setFormat(format);
        volumeOnStorageTO.setSize(size);
        volumeOnStorageTO.setVirtualSize(virtualSize);
        volumeOnStorageTO.setQemuEncryptFormat(encryptFormat);

        Map<VolumeOnStorageTO.Detail, String> details = new HashMap<>();
        details.put(VolumeOnStorageTO.Detail.BACKING_FILE, BACKING_FILE);
        volumeOnStorageTO.setDetails(details);
        volumeOnStorageTO.addDetail(VolumeOnStorageTO.Detail.BACKING_FILE_FORMAT, BACKING_FILE_FORMAT);

        Assert.assertEquals(hypervisorType, volumeOnStorageTO.getHypervisorType());
        Assert.assertEquals(path, volumeOnStorageTO.getPath());
        Assert.assertEquals(name, volumeOnStorageTO.getName());
        Assert.assertEquals(fullPath, volumeOnStorageTO.getFullPath());
        Assert.assertEquals(format, volumeOnStorageTO.getFormat());
        Assert.assertEquals(size, volumeOnStorageTO.getSize());
        Assert.assertEquals(virtualSize, volumeOnStorageTO.getVirtualSize());
        Assert.assertEquals(encryptFormat, volumeOnStorageTO.getQemuEncryptFormat());

        details = volumeOnStorageTO.getDetails();
        Assert.assertEquals(2, details.size());
        Assert.assertEquals(BACKING_FILE, details.get(VolumeOnStorageTO.Detail.BACKING_FILE));
        Assert.assertEquals(BACKING_FILE_FORMAT, details.get(VolumeOnStorageTO.Detail.BACKING_FILE_FORMAT));
    }
}
