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

import org.junit.Assert;
import org.junit.Test;

import java.util.Scanner;

public class MemStatTest {
    final String memInfo = "MemTotal:        5830236 kB\n" +
                           "MemFree:          156752 kB\n" +
                           "Buffers:          326836 kB\n" +
                           "Cached:          2606764 kB\n" +
                           "SwapCached:            0 kB\n" +
                           "Active:          4260808 kB\n" +
                           "Inactive:         949392 kB\n";

    @Test
    public void getMemInfoParseTest() {
        MemStat memStat = null;
        try {
            memStat = new MemStat();
        } catch (RuntimeException ex) {
            // If test isn't run on linux we'll fail creation of linux-specific MemStat class due
            // to dependency on /proc/meminfo if we don't catch here.
            // We are really only interested in testing the parsing algorithm and getters.
            if (memStat == null) {
                throw ex;
            }
        }
        Scanner scanner = new Scanner(memInfo);
        memStat.parseFromScanner(scanner);

        Assert.assertEquals(memStat.getTotal(), 5970161664L);
        Assert.assertEquals(memStat.getAvailable(), 2829840384L);
        Assert.assertEquals(memStat.getFree(), 160514048L);
        Assert.assertEquals(memStat.getCache(), 2669326336L);
    }

    @Test
    public void reservedMemoryTest() {
        MemStat memStat = null;
        try {
            memStat = new MemStat(1024, 2048);
        } catch (RuntimeException ex) {
            if (memStat == null) {
                throw ex;
            }
        }
        Scanner scanner = new Scanner(memInfo);
        memStat.parseFromScanner(scanner);

        Assert.assertEquals(memStat.getTotal(), 5970162688L);
    }
}
