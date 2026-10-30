/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtMergeDiskOnlyVMSnapshotCommandWrapperTest {

    @Mock
    private LibvirtVMDef.DiskDef diskDefMock;

    @Mock
    private DataTO dataTOMock;

    @Spy
    private LibvirtMergeDiskOnlyVMSnapshotCommandWrapper wrapperSpy;

    @Test
    public void isVolumeAlreadyMergedTestChildToIsDiskPath() {
        doReturn("/path").when(diskDefMock).getDiskPath();
        doReturn("path").when(dataTOMock).getPath();

        boolean result = wrapperSpy.isVolumeAlreadyMerged(diskDefMock, dataTOMock);

        assertFalse(result);
    }

    @Test
    public void isVolumeAlreadyMergedTestChildToIsBackingFile() {
        doReturn("/aaaaaaaa").when(diskDefMock).getDiskPath();
        doReturn(List.of("/aba/bab", "/aaadadad/path", "no")).when(diskDefMock).getBackingStoreList();
        doReturn("path").when(dataTOMock).getPath();

        boolean result = wrapperSpy.isVolumeAlreadyMerged(diskDefMock, dataTOMock);

        assertFalse(result);
    }

    @Test
    public void isVolumeAlreadyMergedTestVolumeAlreadyMerged() {
        doReturn("/aaaaaaaa").when(diskDefMock).getDiskPath();
        doReturn(List.of("/aba/bab", "/aaadadad/nope", "no")).when(diskDefMock).getBackingStoreList();
        doReturn("path").when(dataTOMock).getPath();

        boolean result = wrapperSpy.isVolumeAlreadyMerged(diskDefMock, dataTOMock);

        assertTrue(result);
    }
}
