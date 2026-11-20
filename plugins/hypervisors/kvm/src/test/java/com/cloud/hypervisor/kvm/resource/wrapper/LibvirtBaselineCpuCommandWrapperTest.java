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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BaselineCpuCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.utils.script.Script;

public class LibvirtBaselineCpuCommandWrapperTest {

    private final LibvirtBaselineCpuCommandWrapper wrapper = new LibvirtBaselineCpuCommandWrapper();
    private final LibvirtComputingResource resource = new LibvirtComputingResource();

    @Test
    public void emptyInputReturnsFailure() {
        Answer answer = wrapper.execute(new BaselineCpuCommand(Collections.emptyList()), resource);
        Assert.assertFalse(answer.getResult());
    }

    @Test
    public void computesBaselineWhenOutputContainsModel() {
        try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
            scriptMock.when(() -> Script.runSimpleBashScriptWithFullResult(anyString(), anyInt()))
                    .thenReturn("<cpu mode='custom' match='exact'>\n  <model fallback='forbid'>Haswell-noTSX</model>\n</cpu>");
            Answer answer = wrapper.execute(new BaselineCpuCommand(Arrays.asList("<cpu><model>Skylake-Server</model></cpu>")), resource);
            Assert.assertTrue("a <cpu> with a <model> is a successful baseline", answer.getResult());
            Assert.assertTrue(answer.getDetails().contains("<model"));
        }
    }

    @Test
    public void failsWhenOutputHasNoModel() {
        try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
            scriptMock.when(() -> Script.runSimpleBashScriptWithFullResult(anyString(), anyInt()))
                    .thenReturn("error: failed to compute baseline CPU");
            Answer answer = wrapper.execute(new BaselineCpuCommand(Arrays.asList("<cpu><model>Skylake-Server</model></cpu>")), resource);
            Assert.assertFalse(answer.getResult());
        }
    }
}
