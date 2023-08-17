// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.utils.linux;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MemStatTest {
    final String memInfo = "MemTotal:        5830236 kB\n" +
                           "MemFree:          156752 kB\n" +
                           "Buffers:          326836 kB\n" +
                           "Cached:          2606764 kB\n" +
                           "SwapCached:            0 kB\n" +
                           "Active:          4260808 kB\n" +
                           "Inactive:         949392 kB\n";

    MockedConstruction<Scanner> scanner;

    @Before
    public void setup() throws Exception {
        scanner = Mockito.mockConstruction(Scanner.class, (mock, context) -> {
            String[] memInfoLines = memInfo.split("\\n");
            List<Boolean> hasNextReturnList = Arrays.stream(memInfoLines).map(line -> true).collect(
                    Collectors.toList());
            hasNextReturnList.add(false);
            Mockito.when(mock.next()).thenReturn(memInfoLines[0], Arrays.copyOfRange(memInfoLines, 1,
                    memInfoLines.length));
            Mockito.when(mock.hasNext()).thenReturn(true,
                    Arrays.copyOfRange(hasNextReturnList.toArray(new Boolean[0]), 1, hasNextReturnList.size()));
        });

    }

    @After
    public void tearDown() {
        scanner.close();
    }

    @Test
    public void getMemInfoParseTest() {
        MemStat memStat = new MemStat();
        if (!System.getProperty("os.name").equals("Linux")) {
            return;
        }

        Assert.assertEquals(memStat.getTotal(), 5970161664L);
        Assert.assertEquals(memStat.getAvailable(), 3164520448L);
        Assert.assertEquals(memStat.getFree(), 160514048L);
        Assert.assertEquals(memStat.getCache(), 2669326336L);
    }

    @Test
    public void reservedMemoryTest() {
        MemStat memStat = new MemStat(1024, 2048);
        if (!System.getProperty("os.name").equals("Linux")) {
            return;
        }
        Assert.assertEquals(memStat.getTotal(), 5970162688L);
    }
}
