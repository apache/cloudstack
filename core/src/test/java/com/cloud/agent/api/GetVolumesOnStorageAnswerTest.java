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
package com.cloud.agent.api;

import com.cloud.hypervisor.Hypervisor;
import org.apache.cloudstack.storage.volume.VolumeOnStorageTO;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

public class GetVolumesOnStorageAnswerTest {

    private static String path = "path";
    private static String name = "name";
    private static String fullPath = "fullPath";
    private static String format = "qcow2";
    private static long size = 10;
    private static long virtualSize = 20;
    private static String encryptFormat = "LUKS";

    private static GetVolumesOnStorageCommand command = Mockito.mock(GetVolumesOnStorageCommand.class);

    @Test
    public void testGetVolumesOnStorageAnswer() {
        VolumeOnStorageTO volumeOnStorageTO = new VolumeOnStorageTO(Hypervisor.HypervisorType.KVM, path, name, fullPath,
                format, size, virtualSize);
        volumeOnStorageTO.setQemuEncryptFormat(encryptFormat);

        List<VolumeOnStorageTO> volumesOnStorageTO = new ArrayList<>();
        volumesOnStorageTO.add(volumeOnStorageTO);

        GetVolumesOnStorageAnswer answer = new GetVolumesOnStorageAnswer(command, volumesOnStorageTO);
        List<VolumeOnStorageTO> volumes = answer.getVolumes();

        Assert.assertEquals(1, volumes.size());
        VolumeOnStorageTO volume = volumes.get(0);

        Assert.assertEquals(Hypervisor.HypervisorType.KVM, volume.getHypervisorType());
        Assert.assertEquals(path, volume.getPath());
        Assert.assertEquals(name, volume.getName());
        Assert.assertEquals(fullPath, volume.getFullPath());
        Assert.assertEquals(format, volume.getFormat());
        Assert.assertEquals(size, volume.getSize());
        Assert.assertEquals(virtualSize, volume.getVirtualSize());
        Assert.assertEquals(encryptFormat, volume.getQemuEncryptFormat());
        Assert.assertEquals(path, volume.getPath());
    }

    @Test
    public void testGetVolumesOnStorageAnswer2() {
        String details = "details";
        GetVolumesOnStorageAnswer answer = new GetVolumesOnStorageAnswer(command, false, details);
        Assert.assertFalse(answer.getResult());
        Assert.assertEquals(details, answer.getDetails());
    }
}
