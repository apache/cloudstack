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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckConvertInstanceCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtCheckConvertInstanceCommandWrapperTest {

    @Spy
    private LibvirtCheckConvertInstanceCommandWrapper checkConvertInstanceCommandWrapper = Mockito.spy(LibvirtCheckConvertInstanceCommandWrapper.class);

    @Mock
    private LibvirtComputingResource libvirtComputingResourceMock;

    @Mock
    CheckConvertInstanceCommand checkConvertInstanceCommandMock;

    @Before
    public void setUp() {
    }

    @Test
    public void testIsInstanceConversionSupportedOnHost() {
        try (MockedStatic<Script> ignored = Mockito.mockStatic(Script.class)) {
            Mockito.when(Script.runSimpleBashScriptForExitValue(LibvirtConvertInstanceCommandWrapper.checkIfConversionIsSupportedCommand)).thenReturn(0);
            boolean supported = checkConvertInstanceCommandWrapper.isInstanceConversionSupportedOnHost();
            assertTrue(supported);
        }
    }

    @Test
    public void testCheckInstanceCommand_success() {
        try (MockedStatic<Script> ignored = Mockito.mockStatic(Script.class)) {
            Mockito.when(Script.runSimpleBashScriptForExitValue(LibvirtConvertInstanceCommandWrapper.checkIfConversionIsSupportedCommand)).thenReturn(0);
            Answer answer = checkConvertInstanceCommandWrapper.execute(checkConvertInstanceCommandMock, libvirtComputingResourceMock);
            assertTrue(answer.getResult());
        }
    }

    @Test
    public void testCheckInstanceCommand_failure() {
        try (MockedStatic<Script> ignored = Mockito.mockStatic(Script.class)) {
            Mockito.when(Script.runSimpleBashScriptForExitValue(LibvirtConvertInstanceCommandWrapper.checkIfConversionIsSupportedCommand)).thenReturn(1);
            Answer answer = checkConvertInstanceCommandWrapper.execute(checkConvertInstanceCommandMock, libvirtComputingResourceMock);
            assertFalse(answer.getResult());
            assertTrue(StringUtils.isNotBlank(answer.getDetails()));
        }
    }
}
