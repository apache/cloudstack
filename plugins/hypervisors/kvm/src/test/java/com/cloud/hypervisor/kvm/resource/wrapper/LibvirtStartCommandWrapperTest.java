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

import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.utils.Pair;
import com.cloud.utils.ssh.SshHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SshHelper.class)
public class LibvirtStartCommandWrapperTest {

    @Mock
    private File pemFile;

    @Spy
    private LibvirtStartCommandWrapper wrapper = new LibvirtStartCommandWrapper();

    private static final String CONTROL_IP = "169.254.106.231";
    private static final int SSH_PORT = Integer.parseInt(LibvirtComputingResource.DEFAULTDOMRSSHPORT);

    @Test
    @PrepareForTest(SshHelper.class)
    public void testConfigureVncPortOnCpvm() throws Exception {
        PowerMockito.mockStatic(SshHelper.class);
        PowerMockito.when(SshHelper.sshExecute(Mockito.eq(CONTROL_IP), Mockito.eq(SSH_PORT), Mockito.eq("root"),
                        Mockito.eq(pemFile), Mockito.nullable(String.class), Mockito.anyString()))
                .thenReturn(new Pair<>(true, ""));
        String port = "8081";
        PowerMockito.mockStatic(SshHelper.class);
        wrapper.configureVncPortOnCpvm(port, CONTROL_IP, pemFile);
    }
}
