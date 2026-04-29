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
package org.apache.cloudstack.api.response;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.Storage;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public final class VolumeForImportResponseTest {

    private static String path = "path";
    private static String name = "name";
    private static String fullPath = "fullPath";
    private static String format = "qcow2";
    private static long size = 10;
    private static long virtualSize = 20;
    private static String encryptFormat = "LUKS";
    private static Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
    private static String storagePoolId = "storage pool uuid";
    private static String storagePoolName = "storage pool 1";
    private static String storagePoolType = Storage.StoragePoolType.NetworkFilesystem.name();
    private static String chainInfo = "chain info";

    @Test
    public void testVolumeForImportResponse() {
        final VolumeForImportResponse response = new VolumeForImportResponse();

        response.setPath(path);
        response.setName(name);
        response.setFullPath(fullPath);
        response.setFormat(format);
        response.setSize(size);
        response.setVirtualSize(virtualSize);
        response.setQemuEncryptFormat(encryptFormat);
        response.setStoragePoolType(storagePoolType);
        response.setStoragePoolName(storagePoolName);
        response.setStoragePoolId(storagePoolId);
        response.setChainInfo(chainInfo);
        Map<String, String> details = new HashMap<>();
        details.put("key", "value");
        response.setDetails(details);

        Assert.assertEquals(path, response.getPath());
        Assert.assertEquals(name, response.getName());
        Assert.assertEquals(fullPath, response.getFullPath());
        Assert.assertEquals(format, response.getFormat());
        Assert.assertEquals(size, response.getSize());
        Assert.assertEquals(virtualSize, response.getVirtualSize());
        Assert.assertEquals(encryptFormat, response.getQemuEncryptFormat());
        Assert.assertEquals(storagePoolType, response.getStoragePoolType());
        Assert.assertEquals(storagePoolName, response.getStoragePoolName());
        Assert.assertEquals(storagePoolId, response.getStoragePoolId());
        Assert.assertEquals(chainInfo, response.getChainInfo());
        Assert.assertEquals(details, response.getDetails());
    }
}
