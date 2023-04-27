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

package com.cloud.hypervisor.kvm.storage;

import org.junit.Assert;
import org.junit.Test;

public class ScaleIOStorageAdaptorTest {
    @Test
    public void getUsableBytesFromRawBytesTest() {
        Assert.assertEquals("Overhead calculated for 8Gi size", 8554774528L, ScaleIOStorageAdaptor.getUsableBytesFromRawBytes(8L << 30));
        Assert.assertEquals("Overhead calculated for 4Ti size", 4294130925568L, ScaleIOStorageAdaptor.getUsableBytesFromRawBytes(4000L << 30));
        Assert.assertEquals("Overhead calculated for 500Gi size", 536737005568L, ScaleIOStorageAdaptor.getUsableBytesFromRawBytes(500L << 30));
        Assert.assertEquals("Unsupported small size", 0, ScaleIOStorageAdaptor.getUsableBytesFromRawBytes(1L));
    }
}
