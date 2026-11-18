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
package com.cloud.agent.transport;

import com.cloud.agent.api.BadCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.GetStorageStatsCommand;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class LoggingExclusionStrategyTest {

    @Mock
    Logger loggerMock;
    @Spy
    @InjectMocks
    LoggingExclusionStrategy loggingExclusionStrategySpy;

    @Test
    public void shouldSkipClassTestArrayClazz() {
        List<Integer> array = new ArrayList<>();

        boolean result = loggingExclusionStrategySpy.shouldSkipClass(array.getClass());

        Assert.assertFalse(result);
    }

    @Test
    public void shouldSkipClassTestNotSubclassOfCommand() {
        Integer integer = 1;

        boolean result = loggingExclusionStrategySpy.shouldSkipClass(integer.getClass());

        Assert.assertFalse(result);
    }

    @Test
    public void shouldSkipClassTestNullClassAnnotation() {
        Command cmd = new BadCommand();
        Mockito.doReturn(true).when(loggerMock).isEnabled(Level.DEBUG);

        boolean result = loggingExclusionStrategySpy.shouldSkipClass(cmd.getClass());

        Assert.assertFalse(result);
    }

    @Test
    public void shouldSkipClassTestWithClassAnnotation() {
        Command cmd = new GetStorageStatsCommand();
        Mockito.doReturn(true).when(loggerMock).isEnabled(Level.TRACE);

        boolean result = loggingExclusionStrategySpy.shouldSkipClass(cmd.getClass());

        Assert.assertFalse(result);
    }

}
