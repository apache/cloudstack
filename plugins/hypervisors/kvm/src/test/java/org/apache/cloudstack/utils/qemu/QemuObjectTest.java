// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.utils.qemu;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class QemuObjectTest {
    @Test
    public void ToStringTest() {
        Map<QemuObject.ObjectParameter, String> params = new HashMap<>();
        params.put(QemuObject.ObjectParameter.ID, "sec0");
        params.put(QemuObject.ObjectParameter.FILE, "/dev/shm/file");
        QemuObject qObject = new QemuObject(QemuObject.ObjectType.SECRET, params);

        String[] flag = qObject.toCommandFlag();
        Assert.assertEquals(2, flag.length);
        Assert.assertEquals("--object", flag[0]);
        Assert.assertEquals("secret,file=/dev/shm/file,id=sec0", flag[1]);
    }
}
