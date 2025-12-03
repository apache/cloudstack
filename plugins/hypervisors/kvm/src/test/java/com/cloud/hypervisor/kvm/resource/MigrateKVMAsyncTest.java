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

package com.cloud.hypervisor.kvm.resource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.TypedParameter;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class MigrateKVMAsyncTest {

    @Mock
    private LibvirtComputingResource libvirtComputingResource;
    @Mock
    private Connect connect;
    @Mock
    private Domain domain;


    @Test
    public void createTypedParameterListTestNoMigrateDiskLabels() {
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "testxml",
                false, false, false, "tst", "1.1.1.1", null);

        Mockito.doReturn(10).when(libvirtComputingResource).getMigrateSpeed();

        TypedParameter[] result = migrateKVMAsync.createTypedParameterList();

        Assert.assertEquals(4, result.length);

        Assert.assertEquals("tst", result[0].getValueAsString());
        Assert.assertEquals("testxml", result[1].getValueAsString());
        Assert.assertEquals("tcp:1.1.1.1", result[2].getValueAsString());
        Assert.assertEquals("10", result[3].getValueAsString());

    }

    @Test
    public void createTypedParameterListTestWithMigrateDiskLabels() {
        Set<String> labels = Set.of("vda", "vdb");
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "testxml",
                false, false, false, "tst", "1.1.1.1", labels);

        Mockito.doReturn(10).when(libvirtComputingResource).getMigrateSpeed();

        TypedParameter[] result = migrateKVMAsync.createTypedParameterList();

        Assert.assertEquals(6, result.length);

        Assert.assertEquals("tst", result[0].getValueAsString());
        Assert.assertEquals("testxml", result[1].getValueAsString());
        Assert.assertEquals("tcp:1.1.1.1", result[2].getValueAsString());
        Assert.assertEquals("10", result[3].getValueAsString());

        Assert.assertEquals(labels, Set.of(result[4].getValueAsString(), result[5].getValueAsString()));
    }

}
