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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.utils.Pair;

import junit.framework.TestCase;

@RunWith(PowerMockRunner.class)
public class LibvirtUtilitiesHelperTest extends TestCase {

    LibvirtUtilitiesHelper libvirtUtilitiesHelperSpy = Mockito.spy(LibvirtUtilitiesHelper.class);

    @Mock
    Connect connectMock;

    @Test
    public void validateIsLibvirtVersionEqualOrHigherThanVersionInParameterExceptionOnRetrievingLibvirtVersionReturnsFalse() throws LibvirtException {
        Mockito.doThrow(LibvirtException.class).when(connectMock).getLibVirVersion();
        Pair<String, Boolean> result = LibvirtUtilitiesHelper.isLibvirtVersionEqualOrHigherThanVersionInParameter(connectMock, 0l);

        Assert.assertEquals("Unknow due to [null]", result.first());
        Assert.assertFalse(result.second());
    }

    @Test
    public void validateIsLibvirtVersionEqualOrHigherThanVersionInParameterLibvirtVersionIsLowerThanParameterReturnsFalse() throws LibvirtException {
        long libvirtVersion = 9l;
        Mockito.doReturn(libvirtVersion).when(connectMock).getLibVirVersion();
        Pair<String, Boolean> result = LibvirtUtilitiesHelper.isLibvirtVersionEqualOrHigherThanVersionInParameter(connectMock, 10l);

        Assert.assertEquals(String.valueOf(libvirtVersion), result.first());
        Assert.assertFalse(result.second());
    }

    @Test
    public void validateIsLibvirtVersionEqualOrHigherThanVersionInParameterLibvirtVersionIsEqualsToParameterReturnsTrue() throws LibvirtException {
        long libvirtVersion = 10l;
        Mockito.doReturn(libvirtVersion).when(connectMock).getLibVirVersion();
        Pair<String, Boolean> result = LibvirtUtilitiesHelper.isLibvirtVersionEqualOrHigherThanVersionInParameter(connectMock, 10l);

        Assert.assertEquals(String.valueOf(libvirtVersion), result.first());
        Assert.assertTrue(result.second());
    }

    @Test
    public void validateIsLibvirtVersionEqualOrHigherThanVersionInParameterLibvirtVersionIsHigherThanParameterReturnsTrue() throws LibvirtException {
        long libvirtVersion = 11l;
        Mockito.doReturn(libvirtVersion).when(connectMock).getLibVirVersion();
        Pair<String, Boolean> result = LibvirtUtilitiesHelper.isLibvirtVersionEqualOrHigherThanVersionInParameter(connectMock, 10l);

        Assert.assertEquals(String.valueOf(libvirtVersion), result.first());
        Assert.assertTrue(result.second());
    }
}
