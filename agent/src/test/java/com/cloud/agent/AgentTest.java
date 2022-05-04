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
package com.cloud.agent;

import java.io.File;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.agent.properties.AgentProperties;
import com.cloud.agent.properties.AgentPropertiesFileHandler;
import com.cloud.utils.script.Script;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AgentPropertiesFileHandler.class, AgentProperties.class, Script.class})
public class AgentTest {

    @Spy
    @InjectMocks
    Agent agentSpy = new Agent();

    @Mock
    Logger loggerMock;

    @Mock
    File agentFileMock;

    @Mock
    Script scriptMock;

    @Before
    public void setup() {
        Agent.s_logger = loggerMock;
    }

    @Test
    public void setupMemoryBalloonStatsPeriodTestWithNoGuestVMs () {
        PowerMockito.mockStatic(Script.class);
        PowerMockito.when(Script.runSimpleBashScript(Mockito.eq(Agent.COMMAND_LIST_ALL_VMS))).thenReturn(null);

        agentSpy.setupMemoryBalloonStatsPeriod();

        Mockito.verify(loggerMock).debug(Mockito.eq("Skipping the memory balloon stats period setting, since there are no guest VMs."));
    }

    @Test
    public void setupMemoryBalloonStatsPeriodTestMemBalloonPorpertyDisabled() {
        PowerMockito.mockStatic(Script.class);
        PowerMockito.when(Script.runSimpleBashScript(Mockito.eq(Agent.COMMAND_LIST_ALL_VMS))).thenReturn("i-2-100-VM");
        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.eq(AgentProperties.VM_MEMBALLOON_DISABLE))).thenReturn(true);

        agentSpy.setupMemoryBalloonStatsPeriod();

        Mockito.verify(loggerMock).debug(Mockito.eq("Skipping the memory balloon stats period setting because the [vm.memballoon.disable] property is set to 'true'."));
    }

    @Test
    public void setupMemoryBalloonStatsPeriodTestErrorWhenGetMemBalloonFromXml() {
        PowerMockito.mockStatic(Script.class);
        PowerMockito.when(Script.runSimpleBashScript(Mockito.eq(Agent.COMMAND_LIST_ALL_VMS))).thenReturn("i-2-100-VM");
        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.eq(AgentProperties.VM_MEMBALLOON_DISABLE))).thenReturn(false);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.eq(AgentProperties.VM_MEMBALLOON_STATS_PERIOD))).thenReturn(60);
        PowerMockito.when(Script.runSimpleBashScript(Mockito.eq("virsh dumpxml i-2-100-VM | grep '<memballoon model='"))).thenReturn(null);

        agentSpy.setupMemoryBalloonStatsPeriod();

        Mockito.verify(loggerMock).error(Mockito.eq("Unable to get the <memballoon> tag from the XML file for the VM with name [i-2-100-VM] due to an error when running the"
                + " [virsh dumpxml i-2-100-VM | grep '<memballoon model='] command. Therefore, we cannot set the period to collect memory information for the VM. This situation "
                + "can happen if the <memballoon> tag was manually removed from the XML file of the VM."));
    }

    @Test
    public void setupMemoryBalloonStatsPeriodTestErrorWhenSetNewPeriod() {
        PowerMockito.mockStatic(Script.class);
        PowerMockito.when(Script.runSimpleBashScript(Mockito.eq(Agent.COMMAND_LIST_ALL_VMS))).thenReturn("i-2-100-VM");
        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.eq(AgentProperties.VM_MEMBALLOON_DISABLE))).thenReturn(false);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.eq(AgentProperties.VM_MEMBALLOON_STATS_PERIOD))).thenReturn(60);
        PowerMockito.when(Script.runSimpleBashScript(Mockito.eq("virsh dumpxml i-2-100-VM | grep '<memballoon model='"))).thenReturn("    <memballoon model='virtio'>");
        PowerMockito.when(Script.runSimpleBashScript(Mockito.eq("virsh dommemstat i-2-100-VM --period 60 --live"))).thenReturn("some-fake-error");

        agentSpy.setupMemoryBalloonStatsPeriod();

        Mockito.verify(loggerMock).error(Mockito.eq("Unable to set up memory balloon stats period for VM with name [i-2-100-VM] due to an error when running the [virsh dommemstat "
                + "i-2-100-VM --period 60 --live] command. Output: [some-fake-error]."));
    }

    @Test
    public void setupMemoryBalloonStatsPeriodTestSetNewPeriodSuccessfully() {
        PowerMockito.mockStatic(Script.class);
        PowerMockito.when(Script.runSimpleBashScript(Mockito.eq(Agent.COMMAND_LIST_ALL_VMS))).thenReturn("i-2-100-VM");
        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.eq(AgentProperties.VM_MEMBALLOON_DISABLE))).thenReturn(false);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.eq(AgentProperties.VM_MEMBALLOON_STATS_PERIOD))).thenReturn(60);
        PowerMockito.when(Script.runSimpleBashScript(Mockito.eq("virsh dumpxml i-2-100-VM | grep '<memballoon model='"))).thenReturn("    <memballoon model='virtio'>");
        PowerMockito.when(Script.runSimpleBashScript(Mockito.eq("virsh dommemstat i-2-100-VM --period 60 --live"))).thenReturn(null);

        agentSpy.setupMemoryBalloonStatsPeriod();

        Mockito.verify(loggerMock).debug(Mockito.eq("The memory balloon stats period [60] has been set successfully for the VM with name [i-2-100-VM]."));
    }

    @Test
    public void setupMemoryBalloonStatsPeriodTestSkip() {
        PowerMockito.mockStatic(Script.class);
        PowerMockito.when(Script.runSimpleBashScript(Mockito.eq(Agent.COMMAND_LIST_ALL_VMS))).thenReturn("i-2-100-VM");
        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.eq(AgentProperties.VM_MEMBALLOON_DISABLE))).thenReturn(false);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.eq(AgentProperties.VM_MEMBALLOON_STATS_PERIOD))).thenReturn(60);
        PowerMockito.when(Script.runSimpleBashScript(Mockito.eq("virsh dumpxml i-2-100-VM | grep '<memballoon model='"))).thenReturn("    <memballoon model='none'>");

        agentSpy.setupMemoryBalloonStatsPeriod();

        Mockito.verify(loggerMock).debug(Mockito.eq("Skipping the memory balloon stats period setting for the VM with name [i-2-100-VM] because this VM has no memory balloon."));
    }
}