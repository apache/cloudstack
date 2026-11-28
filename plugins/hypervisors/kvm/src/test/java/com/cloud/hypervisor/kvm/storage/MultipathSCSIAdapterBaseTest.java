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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.cloud.utils.script.Script;

public class MultipathSCSIAdapterBaseTest {

    private static final String PATH = "/dev/mapper/3624a937049646854b0ae410b00011266";

    @BeforeClass
    public static void loadAdapterClass() {
        // the adapter looks up its helper scripts when the class loads
        try (MockedStatic<Script> script = mockStatic(Script.class, Mockito.CALLS_REAL_METHODS)) {
            script.when(() -> Script.findScript(anyString(), anyString())).thenReturn("/bin/true");
            new FiberChannelAdapter();
        }
    }

    private FiberChannelAdapter adapter(boolean blockDevice, boolean activePath) {
        FiberChannelAdapter adapter = spy(new FiberChannelAdapter());
        doReturn(blockDevice).when(adapter).isBlockDevice(PATH);
        doReturn(activePath).when(adapter).hasActivePath(PATH);
        return adapter;
    }

    @Test
    public void isConnectedWhenDeviceHasAnActivePath() {
        Assert.assertTrue(adapter(true, true).isConnected(PATH));
    }

    @Test
    public void isNotConnectedWhenMapHasNoActivePaths() {
        Assert.assertFalse(adapter(true, false).isConnected(PATH));
    }

    @Test
    public void isNotConnectedWhenDeviceIsMissing() {
        FiberChannelAdapter adapter = adapter(false, false);
        Assert.assertFalse(adapter.isConnected(PATH));
        verify(adapter, never()).hasActivePath(PATH);
    }
}
