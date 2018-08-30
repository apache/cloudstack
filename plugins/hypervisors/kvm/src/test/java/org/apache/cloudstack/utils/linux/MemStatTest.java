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
    @Test
    public void getMemInfoParseTest() {
        String memInfo = "MemTotal:        5830236 kB\n" +
                         "MemFree:          156752 kB\n" +
                         "Buffers:          326836 kB\n" +
                         "Cached:          2606764 kB\n" +
                         "SwapCached:            0 kB\n" +
                         "Active:          4260808 kB\n" +
                         "Inactive:         949392 kB\n";

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

        Assert.assertEquals(memStat.getTotal(), Double.valueOf(5830236));
        Assert.assertEquals(memStat.getAvailable(), Double.valueOf(2763516));
        Assert.assertEquals(memStat.getFree(), Double.valueOf(156752));
        Assert.assertEquals(memStat.getCache(), Double.valueOf(2606764));
    }
}
