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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.cloudstack.diagnostics.fileprocessor.DiagnosticsFilesList;
import org.apache.cloudstack.diagnostics.fileprocessor.DiagnosticsFilesListFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;

/**
 * The router diagnostics defaults collect the FRR log and a copy of frr.conf with the
 * passwords masked. The plain /etc/frr/frr.conf holds peer passwords and is never a default.
 */
@RunWith(MockitoJUnitRunner.class)
public class DiagnosticsFilesListFrrTest {

    private static final String FRR_LOG = "/var/log/frr/frr.log";
    private static final String FRR_CONF_MASKED = "/var/cache/cloud/frr.conf.masked";

    @Mock
    private VMInstanceVO router;

    private List<String> routerDefaults() {
        return Arrays.asList(DiagnosticsFilesList.RouterDefaultSupportedFiles.defaultValue().split(","));
    }

    @Test
    public void routerDefaultsIncludeFrrFilesAndNotPlainConf() {
        List<String> defaults = routerDefaults();
        Assert.assertTrue(defaults.contains(FRR_LOG));
        Assert.assertTrue(defaults.contains(FRR_CONF_MASKED));
        Assert.assertFalse(defaults.contains("/etc/frr/frr.conf"));
    }

    // A request with no file list gets both new files.
    @Test
    public void defaultRouterFileListCarriesFrrFiles() {
        Mockito.when(router.getType()).thenReturn(VirtualMachine.Type.DomainRouter);
        List<String> files = Objects.requireNonNull(DiagnosticsFilesListFactory.getDiagnosticsFilesList(null, router)).generateFileList();
        Assert.assertTrue(files.contains(FRR_LOG));
        Assert.assertTrue(files.contains(FRR_CONF_MASKED));
    }
}
