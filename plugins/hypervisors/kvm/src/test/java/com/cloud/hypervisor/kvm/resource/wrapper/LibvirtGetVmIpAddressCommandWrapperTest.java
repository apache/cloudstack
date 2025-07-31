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

package com.cloud.hypervisor.kvm.resource.wrapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetVmIpAddressCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.utils.Pair;
import com.cloud.utils.script.Script;

public class LibvirtGetVmIpAddressCommandWrapperTest {

    private static String VIRSH_DOMIF_OUTPUT = " Name       MAC address          Protocol     Address\n" + //
            "-------------------------------------------------------------------------------\n" + //
            " lo         00:00:00:00:00:70    ipv4         127.0.0.1/8\n" + //
            " eth0       02:0c:02:f9:00:80    ipv4         192.168.0.10/24\n" + //
            " net1 b2:41:19:69:a4:90    N/A          N/A\n" + //
            " net2 52:a2:36:cf:d1:50    ipv4         10.244.6.93/32\n" + //
            " net3 a6:1d:d3:52:d3:40    N/A          N/A\n" + //
            " net4 2e:9b:60:dc:49:30    N/A          N/A\n" + //
            " lxc5b7327203b6f 92:b2:77:0b:a9:20    N/A          N/A\n";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testExecuteWithValidVmName() {
        LibvirtComputingResource libvirtComputingResource = mock(LibvirtComputingResource.class);
        GetVmIpAddressCommand getVmIpAddressCommand = mock(GetVmIpAddressCommand.class);
        LibvirtGetVmIpAddressCommandWrapper commandWrapper = new LibvirtGetVmIpAddressCommandWrapper();
        MockedStatic<Script> scriptMock = mockStatic(Script.class);

        when(getVmIpAddressCommand.getVmName()).thenReturn("validVmName");
        when(getVmIpAddressCommand.getVmNetworkCidr()).thenReturn("192.168.0.0/24");
        when(getVmIpAddressCommand.getMacAddress()).thenReturn("02:0c:02:f9:00:80");
        when(getVmIpAddressCommand.isWindows()).thenReturn(false);
        when(Script.executePipedCommands(anyList(), anyLong())).thenReturn(new Pair<>(0, VIRSH_DOMIF_OUTPUT));

        Answer answer = commandWrapper.execute(getVmIpAddressCommand, libvirtComputingResource);

        try {
            assertTrue(answer.getResult());
            assertEquals("192.168.0.10", answer.getDetails());
        } finally {
            scriptMock.close();
        }
    }

    @Test
    public void testExecuteWithInvalidVmName() {
        LibvirtComputingResource libvirtComputingResource = mock(LibvirtComputingResource.class);
        GetVmIpAddressCommand getVmIpAddressCommand = mock(GetVmIpAddressCommand.class);
        LibvirtGetVmIpAddressCommandWrapper commandWrapper = new LibvirtGetVmIpAddressCommandWrapper();
        MockedStatic<Script> scriptMock = mockStatic(Script.class);

        when(getVmIpAddressCommand.getVmName()).thenReturn("invalidVmName!");
        when(getVmIpAddressCommand.getVmNetworkCidr()).thenReturn("192.168.0.0/24");
        when(getVmIpAddressCommand.getMacAddress()).thenReturn("02:0c:02:f9:00:80");
        when(getVmIpAddressCommand.isWindows()).thenReturn(false);
        when(Script.executePipedCommands(anyList(), anyLong())).thenReturn(new Pair<>(0, VIRSH_DOMIF_OUTPUT));

        Answer answer = commandWrapper.execute(getVmIpAddressCommand, libvirtComputingResource);

        try {
            assertFalse(answer.getResult());
            assertNull(answer.getDetails());
        } finally {
            scriptMock.close();
        }

    }

    @Test
    public void testExecuteWithWindowsVm() {
        LibvirtComputingResource libvirtComputingResource = mock(LibvirtComputingResource.class);
        GetVmIpAddressCommand getVmIpAddressCommand = mock(GetVmIpAddressCommand.class);
        LibvirtGetVmIpAddressCommandWrapper commandWrapper = new LibvirtGetVmIpAddressCommandWrapper();
        MockedStatic<Script> scriptMock = null;

        try {
            scriptMock = mockStatic(Script.class);

            when(getVmIpAddressCommand.getVmName()).thenReturn("validVmName");
            when(getVmIpAddressCommand.getVmNetworkCidr()).thenReturn("192.168.0.0/24");
            when(getVmIpAddressCommand.getMacAddress()).thenReturn("02:0c:02:f9:00:80");
            when(getVmIpAddressCommand.isWindows()).thenReturn(true);
            when(Script.executePipedCommands(anyList(), anyLong())).thenReturn(new Pair<>(0, "192.168.0.10"));

            Answer answer = commandWrapper.execute(getVmIpAddressCommand, libvirtComputingResource);

            assertTrue(answer.getResult());
            assertEquals("192.168.0.10", answer.getDetails());
        } finally {
            if (scriptMock != null)
                scriptMock.close();
        }
    }

    @Test
    public void testExecuteWithNoIpFound() {
        LibvirtComputingResource libvirtComputingResource = mock(LibvirtComputingResource.class);
        GetVmIpAddressCommand getVmIpAddressCommand = mock(GetVmIpAddressCommand.class);
        LibvirtGetVmIpAddressCommandWrapper commandWrapper = new LibvirtGetVmIpAddressCommandWrapper();
        MockedStatic<Script> scriptMock = null;
        try {
            scriptMock = mockStatic(Script.class);

            when(getVmIpAddressCommand.getVmName()).thenReturn("validVmName");
            when(getVmIpAddressCommand.getVmNetworkCidr()).thenReturn("192.168.0.0/24");
            when(getVmIpAddressCommand.isWindows()).thenReturn(false);
            when(Script.executePipedCommands(anyList(), anyLong())).thenReturn(new Pair<>(0, ""));

            Answer answer = commandWrapper.execute(getVmIpAddressCommand, libvirtComputingResource);

            assertFalse(answer.getResult());
            assertNull(answer.getDetails());
        } finally {
            if (scriptMock != null)
                scriptMock.close();
        }
    }

    @Test
    public void testExecuteWithValidVmNameAndNoIpFound() {
        LibvirtComputingResource libvirtComputingResource = mock(LibvirtComputingResource.class);
        GetVmIpAddressCommand getVmIpAddressCommand = mock(GetVmIpAddressCommand.class);
        LibvirtGetVmIpAddressCommandWrapper commandWrapper = new LibvirtGetVmIpAddressCommandWrapper();
        MockedStatic<Script> scriptMock = null;
        try {
            scriptMock = mockStatic(Script.class);

            when(getVmIpAddressCommand.getVmName()).thenReturn("validVmName");
            when(getVmIpAddressCommand.getVmNetworkCidr()).thenReturn("192.168.0.0/24");
            when(getVmIpAddressCommand.isWindows()).thenReturn(false);
            when(Script.executePipedCommands(anyList(), anyLong())).thenReturn(new Pair<>(0, ""));

            Answer answer = commandWrapper.execute(getVmIpAddressCommand, libvirtComputingResource);

            assertFalse(answer.getResult());
            assertNull(answer.getDetails());
        } finally {
            if (scriptMock != null)
                scriptMock.close();
        }
    }

    @Test
    public void testExecuteWithValidVmNameAndIpFromDhcpLeaseFile() {
        LibvirtComputingResource libvirtComputingResource = mock(LibvirtComputingResource.class);
        GetVmIpAddressCommand getVmIpAddressCommand = mock(GetVmIpAddressCommand.class);
        LibvirtGetVmIpAddressCommandWrapper commandWrapper = new LibvirtGetVmIpAddressCommandWrapper();
        MockedStatic<Script> scriptMock = null;
        try {
            scriptMock = mockStatic(Script.class);
            when(getVmIpAddressCommand.getVmName()).thenReturn("validVmName");
            when(getVmIpAddressCommand.getVmNetworkCidr()).thenReturn("192.168.0.0/24");
            when(getVmIpAddressCommand.isWindows()).thenReturn(false);
            when(Script.executePipedCommands(anyList(), anyLong())).thenReturn(new Pair<>(0, "192.168.0.10"));

            Answer answer = commandWrapper.execute(getVmIpAddressCommand, libvirtComputingResource);

            assertTrue(answer.getResult());
            assertEquals("192.168.0.10", answer.getDetails());
        } finally {
            if (scriptMock != null)
                scriptMock.close();
        }
    }

    @Test
    public void testExecuteWithValidVmNameAndIpFromWindowsRegistry() {
        LibvirtComputingResource libvirtComputingResource = mock(LibvirtComputingResource.class);
        GetVmIpAddressCommand getVmIpAddressCommand = mock(GetVmIpAddressCommand.class);
        LibvirtGetVmIpAddressCommandWrapper commandWrapper = new LibvirtGetVmIpAddressCommandWrapper();
        MockedStatic<Script> scriptMock = null;
        try {
            scriptMock = mockStatic(Script.class);
            when(getVmIpAddressCommand.getVmName()).thenReturn("validVmName");
            when(getVmIpAddressCommand.getVmNetworkCidr()).thenReturn("192.168.0.0/24");
            when(getVmIpAddressCommand.isWindows()).thenReturn(true);
            when(Script.executePipedCommands(anyList(), anyLong())).thenReturn(new Pair<>(0, "\"192.168.0.10\""));

            Answer answer = commandWrapper.execute(getVmIpAddressCommand, libvirtComputingResource);

            assertTrue(answer.getResult());
            assertEquals("192.168.0.10", answer.getDetails());
        } finally {
            if (scriptMock != null)
                scriptMock.close();
        }
    }

    @Test
    public void testIpFromDomIfCommandExecutionFailure() {
        LibvirtComputingResource libvirtComputingResource = mock(LibvirtComputingResource.class);
        GetVmIpAddressCommand getVmIpAddressCommand = mock(GetVmIpAddressCommand.class);
        LibvirtGetVmIpAddressCommandWrapper commandWrapper = new LibvirtGetVmIpAddressCommandWrapper();
        MockedStatic<Script> scriptMock = null;
        try {
            scriptMock = mockStatic(Script.class);
            when(getVmIpAddressCommand.getVmName()).thenReturn("testVm");
            when(getVmIpAddressCommand.getVmNetworkCidr()).thenReturn("192.168.1.0/24");
            when(Script.executePipedCommands(anyList(), anyLong())).thenReturn(null);

            Answer answer = commandWrapper.execute(getVmIpAddressCommand, libvirtComputingResource);

            assertFalse(answer.getResult());
            assertNull(answer.getDetails());
        } finally {
            if (scriptMock != null)
                scriptMock.close();
        }
    }

    @Test
    public void testIpFromDhcpLeaseFileCommandExecutionFailure() {
        LibvirtComputingResource libvirtComputingResource = mock(LibvirtComputingResource.class);
        GetVmIpAddressCommand getVmIpAddressCommand = mock(GetVmIpAddressCommand.class);
        LibvirtGetVmIpAddressCommandWrapper commandWrapper = new LibvirtGetVmIpAddressCommandWrapper();
        MockedStatic<Script> scriptMock = null;
        try {
            scriptMock = mockStatic(Script.class);
            when(getVmIpAddressCommand.getVmName()).thenReturn("testVm");
            when(getVmIpAddressCommand.getVmNetworkCidr()).thenReturn("192.168.1.0/24");
            when(Script.executePipedCommands(anyList(), anyLong())).thenReturn(null);

            Answer answer = commandWrapper.execute(getVmIpAddressCommand, libvirtComputingResource);

            assertFalse(answer.getResult());
            assertNull(answer.getDetails());
        } finally {
            if (scriptMock != null)
                scriptMock.close();
        }
    }

    @Test
    public void testIpFromWindowsRegistryCommandExecutionFailure() {
        LibvirtComputingResource libvirtComputingResource = mock(LibvirtComputingResource.class);
        GetVmIpAddressCommand getVmIpAddressCommand = mock(GetVmIpAddressCommand.class);
        LibvirtGetVmIpAddressCommandWrapper commandWrapper = new LibvirtGetVmIpAddressCommandWrapper();
        MockedStatic<Script> scriptMock = null;
        try {
            scriptMock = mockStatic(Script.class);
            when(getVmIpAddressCommand.getVmName()).thenReturn("testVm");
            when(getVmIpAddressCommand.getVmNetworkCidr()).thenReturn("192.168.1.0/24");
            when(getVmIpAddressCommand.isWindows()).thenReturn(true);
            when(Script.executePipedCommands(anyList(), anyLong())).thenReturn(null);

            Answer answer = commandWrapper.execute(getVmIpAddressCommand, libvirtComputingResource);

            assertFalse(answer.getResult());
            assertNull(answer.getDetails());
        } finally {
            if (scriptMock != null)
                scriptMock.close();
        }
    }

    @Test
    public void testInit() {
        MockedStatic<Script> scriptMock = null;
        try {
            scriptMock = mockStatic(Script.class);
            scriptMock.when(() -> Script.getExecutableAbsolutePath("virt-ls")).thenReturn("/usr/bin/virt-ls");
            scriptMock.when(() -> Script.getExecutableAbsolutePath("virt-cat")).thenReturn("/usr/bin/virt-cat");
            scriptMock.when(() -> Script.getExecutableAbsolutePath("virt-win-reg")).thenReturn("/usr/bin/virt-win-reg");
            scriptMock.when(() -> Script.getExecutableAbsolutePath("tail")).thenReturn("/usr/bin/tail");
            scriptMock.when(() -> Script.getExecutableAbsolutePath("grep")).thenReturn("/usr/bin/grep");
            scriptMock.when(() -> Script.getExecutableAbsolutePath("awk")).thenReturn("/usr/bin/awk");
            scriptMock.when(() -> Script.getExecutableAbsolutePath("sed")).thenReturn("/usr/bin/sed");
            scriptMock.when(() -> Script.getExecutableAbsolutePath("virsh")).thenReturn("/usr/bin/virsh");

            LibvirtGetVmIpAddressCommandWrapper.init();

            assertEquals("/usr/bin/virt-ls", LibvirtGetVmIpAddressCommandWrapper.virt_ls_path);
            assertEquals("/usr/bin/virt-cat", LibvirtGetVmIpAddressCommandWrapper.virt_cat_path);
            assertEquals("/usr/bin/virt-win-reg", LibvirtGetVmIpAddressCommandWrapper.virt_win_reg_path);
            assertEquals("/usr/bin/tail", LibvirtGetVmIpAddressCommandWrapper.tail_path);
            assertEquals("/usr/bin/grep", LibvirtGetVmIpAddressCommandWrapper.grep_path);
            assertEquals("/usr/bin/awk", LibvirtGetVmIpAddressCommandWrapper.awk_path);
            assertEquals("/usr/bin/sed", LibvirtGetVmIpAddressCommandWrapper.sed_path);
            assertEquals("/usr/bin/virsh", LibvirtGetVmIpAddressCommandWrapper.virsh_path);
        } finally {
            if (scriptMock != null)
                scriptMock.close();
        }
    }
}
