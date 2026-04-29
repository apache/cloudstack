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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class MemStat {
    /*
        Gather Memory Statistics of the current node by opening /proc/meminfo
        which contains the memory information in KiloBytes.

        Convert this all to bytes and return Long as a type with the information
        in bytes
     */
    protected final static String MEMINFO_FILE = "/proc/meminfo";
    protected final static String FREE_KEY = "MemFree";
    protected final static String CACHE_KEY = "Cached";
    protected final static String TOTAL_KEY = "MemTotal";
    protected final static String BUFFER_KEY = "Buffers";
    long reservedMemory;
    long overCommitMemory;

    private final Map<String, Long> _memStats = new HashMap<>();

    public MemStat() {
        this(0,0);
    }

    public MemStat(long reservedMemory, long overCommitMemory) {
        this.reservedMemory = reservedMemory;
        this.overCommitMemory = overCommitMemory;
        if (System.getProperty("os.name").equals("Linux")) {
            this.refresh();
        }
    }

    public long getTotal() {
        return _memStats.get(TOTAL_KEY) - reservedMemory + overCommitMemory;
    }

    public long getAvailable() {
        return getFree() + getCache() + getBuffer();
    }

    public long getFree() {
        return _memStats.get(FREE_KEY) - reservedMemory + overCommitMemory;
    }

    public long getCache() {
        return _memStats.get(CACHE_KEY);
    }

    public long getBuffer() {
        return _memStats.get(BUFFER_KEY);
    }

    public void refresh() {
        File f = new File(MEMINFO_FILE);
        try (Scanner scanner = new Scanner(f,"UTF-8")) {
            parseFromScanner(scanner);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("File " + MEMINFO_FILE + " not found:" + ex.toString());
        }
    }

    protected void parseFromScanner(Scanner scanner) {
        scanner.useDelimiter("\\n");
        while(scanner.hasNext()) {
            String[] stats = scanner.next().split("\\:\\s+");
            if (stats.length == 2) {
                _memStats.put(stats[0], Long.valueOf(stats[1].replaceAll("\\s+\\w+","")) * 1024L);
            }
        }
    }
}
