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
package org.apache.cloudstack.diagnostics;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.cloudstack.diagnostics.fileprocessor.DiagnosticsFilesListFactory;
import org.apache.cloudstack.diagnostics.fileprocessor.DomainRouterDiagnosticsFiles;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;

@RunWith(MockitoJUnitRunner.class)
public class DiagnosticsFilesListFactoryTest {

    private DomainRouterDiagnosticsFiles proxyDiagnosticFiles;

    @Mock
    private VMInstanceVO vmInstance;

    @InjectMocks
    private DiagnosticsFilesListFactory listFactory = new DiagnosticsFilesListFactory();

    @Before
    public void setUp() throws Exception {
        Mockito.when(vmInstance.getType()).thenReturn(VirtualMachine.Type.DomainRouter);
    }

    @After
    public void tearDown() throws Exception {
        Mockito.reset(vmInstance);
    }

    @Test
    public void testgetDiagnosticsFilesListCpVmDataTypeList() {
        List<String> dataTypeList = new ArrayList<>();
        dataTypeList.add("/var/log/auth.log");
        dataTypeList.add("/etc/dnsmasq.conf");
        dataTypeList.add("iptables");
        dataTypeList.add("ipaddr");

        List<String> files = Objects.requireNonNull(DiagnosticsFilesListFactory.getDiagnosticsFilesList(dataTypeList, vmInstance)).generateFileList();

        assertEquals(files, dataTypeList);
    }

    @Test
    public void testDiagnosticsFileListDefaultsRouter() {
        List<String> filesList = Objects.requireNonNull(DiagnosticsFilesListFactory.getDiagnosticsFilesList(null, vmInstance)).generateFileList();

        ConfigKey configKey = proxyDiagnosticFiles.RouterDefaultSupportedFiles;
        String[] defaultFileArray = configKey.defaultValue().split(",");

        assertEquals(filesList.size(), defaultFileArray.length);
    }
}
