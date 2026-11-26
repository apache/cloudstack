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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.Test;

public class IscsiAdmStorageAdaptorTest {

    private static final String IQN = "iqn.1992-08.com.netapp:sn.abc:vs.3";

    @Test
    public void disconnectPhysicalDiskByPathParsesAnIscsiByPath() {
        IscsiAdmStorageAdaptor adaptor = spy(new IscsiAdmStorageAdaptor());
        String path = "/dev/disk/by-path/ip-10.0.0.1:3260-iscsi-" + IQN + "-lun-0";
        doReturn(true).when(adaptor).disconnectPhysicalDisk("10.0.0.1", 3260, IQN, "0");

        assertTrue(adaptor.disconnectPhysicalDiskByPath(path));
        verify(adaptor).disconnectPhysicalDisk("10.0.0.1", 3260, IQN, "0");
    }

    @Test
    public void disconnectPhysicalDiskByPathParsesAnIpv6Portal() {
        IscsiAdmStorageAdaptor adaptor = spy(new IscsiAdmStorageAdaptor());
        String host = "2001:db8::1";
        String path = "/dev/disk/by-path/ip-" + host + ":3260-iscsi-" + IQN + "-lun-0";
        doReturn(true).when(adaptor).disconnectPhysicalDisk(host, 3260, IQN, "0");

        assertTrue(adaptor.disconnectPhysicalDiskByPath(path));
        verify(adaptor).disconnectPhysicalDisk(host, 3260, IQN, "0");
    }

    @Test
    public void disconnectPhysicalDiskByPathUsesTheLastLunSeparator() {
        IscsiAdmStorageAdaptor adaptor = spy(new IscsiAdmStorageAdaptor());
        String iqnWithLunText = "iqn.1992-08.com.netapp:sn.abc-lun-2:vs.3";
        String path = "/dev/disk/by-path/ip-10.0.0.1:3260-iscsi-" + iqnWithLunText + "-lun-7";
        doReturn(true).when(adaptor).disconnectPhysicalDisk("10.0.0.1", 3260, iqnWithLunText, "7");

        assertTrue(adaptor.disconnectPhysicalDiskByPath(path));
        verify(adaptor).disconnectPhysicalDisk("10.0.0.1", 3260, iqnWithLunText, "7");
    }

    @Test
    public void disconnectPhysicalDiskByPathRejectsPathsItDoesNotOwn() {
        IscsiAdmStorageAdaptor adaptor = new IscsiAdmStorageAdaptor();

        assertFalse(adaptor.disconnectPhysicalDiskByPath(null));
        assertFalse(adaptor.disconnectPhysicalDiskByPath("/dev/disk/by-id/wwn-0x600a098078304d2d383f2f6b45734c6a"));
        assertFalse(adaptor.disconnectPhysicalDiskByPath("/dev/disk/by-id/wwn-0x600a098078304d2d383f2f6b45734c6a-iscsi-" + IQN + "-lun-0"));
        assertFalse(adaptor.disconnectPhysicalDiskByPath("/dev/disk/by-path/ip-10.0.0.1-iscsi-" + IQN + "-lun-0"));
        assertFalse(adaptor.disconnectPhysicalDiskByPath("/dev/disk/by-path/ip-10.0.0.1:3260-iscsi-" + IQN + "-lun-0-part1"));
        assertFalse(adaptor.disconnectPhysicalDiskByPath("/dev/disk/by-path/ip-10.0.0.1:3260-iscsi-" + IQN + "-lun-abc"));
        assertFalse(adaptor.disconnectPhysicalDiskByPath("/dev/disk/by-path/ip-10.0.0.1:99999999999-iscsi-" + IQN + "-lun-0"));
    }
}
