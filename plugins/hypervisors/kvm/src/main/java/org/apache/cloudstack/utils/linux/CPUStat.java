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
import java.util.Scanner;

import org.apache.log4j.Logger;

public class CPUStat {
    private static final Logger s_logger = Logger.getLogger(CPUStat.class);

    private Integer _cores;
    private UptimeStats _lastStats;
    private final String _sysfsCpuDir = "/sys/devices/system/cpu";
    private final String _uptimeFile = "/proc/uptime";
    private final String _loadavgFile = "/proc/loadavg";

    class UptimeStats {
        public Double upTime = 0d;
        public Double cpuIdleTime = 0d;

        public UptimeStats(Double upTime, Double cpuIdleTime) {
            this.upTime = upTime;
            this.cpuIdleTime = cpuIdleTime;
        }
    }

    public CPUStat () {
        init();
    }

    private void init() {
        _cores = getCoresFromLinux();
        _lastStats = getUptimeAndCpuIdleTime();
    }

    private UptimeStats getUptimeAndCpuIdleTime() {
        UptimeStats uptime = new UptimeStats(0d, 0d);
        File f = new File(_uptimeFile);
        try (Scanner scanner = new Scanner(f,"UTF-8");) {
            String[] stats = scanner.useDelimiter("\\Z").next().split("\\s+");
            uptime = new UptimeStats(Double.parseDouble(stats[0]), Double.parseDouble(stats[1]));
        } catch (FileNotFoundException ex) {
            s_logger.warn("File " + _uptimeFile + " not found:" + ex.toString());
        }
        return uptime;
    }

    private Integer getCoresFromLinux() {
        Integer cpus = 0;
        File cpuDir = new File(_sysfsCpuDir);
        File[] files = cpuDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().matches("cpu\\d+")) {
                    cpus++;
                }
            }
        }
        return cpus;
    }

    public Integer getCores() {
        return _cores;
    }

    public Double getCpuLoadAverage() {
        File f = new File(_loadavgFile);
        String[] load = {"0.0"};
        try (Scanner scanner = new Scanner(f,"UTF-8");) {
            load = scanner.useDelimiter("\\Z").next().split("\\s+");
        } catch (FileNotFoundException ex) {
            s_logger.warn("File " + _uptimeFile + " not found:" + ex.toString());
        }
        return Double.parseDouble(load[0]);
    }

    public Double getCpuUsedPercent() {
        Double cpuUsed = 0d;
        if (_cores == null || _cores == 0) {
            _cores = getCoresFromLinux();
        }

        UptimeStats currentStats = getUptimeAndCpuIdleTime();
        if (currentStats == null) {
            return cpuUsed;
        }

        Double timeElapsed = currentStats.upTime - _lastStats.upTime;
        Double cpuElapsed = (currentStats.cpuIdleTime - _lastStats.cpuIdleTime) / _cores;
        if (timeElapsed > 0) {
            cpuUsed = (1 - (cpuElapsed / timeElapsed)) * 100;
        }
        if (cpuUsed < 0) {
            cpuUsed = 0d;
        }
        _lastStats = currentStats;
        return cpuUsed;
    }
}
