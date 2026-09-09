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
package com.cloud.agent.manager.allocator.impl;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;

import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.server.StatsCollector;
import com.cloud.utils.component.ManagerBase;

/**
 * Keeps a smoothed view of how hard each host is actually working.
 *
 * StatsCollector already polls every host, but it keeps only the newest sample and nothing uses it
 * for placement. A single sample is too noisy to rank on: a host can look idle moments before a
 * batch of VMs starts work. This folds those samples into an exponentially weighted moving average
 * so ranking reflects a trend rather than an instant.
 *
 * The average is per management server and is not persisted. Every management server polls every
 * host, so all of them converge on the same picture, and a restarted server simply reports nothing
 * usable until it has sampled - callers then fall back to allocation figures.
 */
public class HostLoadTracker extends ManagerBase implements Configurable {

    public static final ConfigKey<Integer> HostLoadSampleInterval = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Integer.class, "host.load.sample.interval", "60",
            "Seconds between samples of host CPU and memory utilisation, for placement algorithms that " +
                    "consider actual load. Should not be shorter than host.stats.interval.",
            false, ConfigKey.Scope.Global);

    public static final ConfigKey<Integer> HostLoadHalfLife = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Integer.class, "host.load.half.life", "300",
            "Half life in seconds of the moving average of host utilisation. Larger values react more " +
                    "slowly and are less affected by short spikes or by guests periodically releasing memory.",
            true, ConfigKey.Scope.Global);

    @Inject
    private HostDao hostDao;

    @Inject
    private StatsCollector statsCollector;

    private final Map<Long, Sample> samples = new ConcurrentHashMap<>();

    private Timer timer;

    @Override
    public boolean start() {
        int interval = Math.max(1, HostLoadSampleInterval.value()) * 1000;
        TimerTask task = new ManagedContextTimerTask() {
            @Override
            protected void runInContext() {
                try {
                    sampleAllHosts();
                } catch (Exception e) {
                    logger.warn("Unable to sample host load", e);
                }
            }
        };
        timer = new Timer("HostLoadTracker");
        timer.schedule(task, interval, interval);
        return true;
    }

    @Override
    public boolean stop() {
        if (timer != null) {
            timer.cancel();
        }
        return true;
    }

    protected void sampleAllHosts() {
        for (HostVO host : hostDao.listByType(com.cloud.host.Host.Type.Routing)) {
            if (host.getStatus() != Status.Up) {
                samples.remove(host.getId());
                continue;
            }
            record(host.getId(), statsCollector.getHostStats(host.getId()));
        }
    }

    protected void record(long hostId, HostStats stats) {
        record(hostId, stats, System.currentTimeMillis());
    }

    protected void record(long hostId, HostStats stats, long now) {
        if (stats == null) {
            return;
        }
        double totalMemory = stats.getTotalMemoryKBs();
        if (totalMemory <= 0) {
            return;
        }
        // getCpuUtilization is a percentage of the host's real cores
        double cpu = clamp(stats.getCpuUtilization() / 100.0);
        double memory = clamp((totalMemory - stats.getFreeMemoryKBs()) / totalMemory);

        samples.compute(hostId, (id, previous) -> previous == null
                ? new Sample(cpu, memory, now)
                : previous.fold(cpu, memory, now, HostLoadHalfLife.value()));
    }

    public HostLoad getLoad(long hostId) {
        Sample sample = samples.get(hostId);
        return sample == null ? HostLoad.UNKNOWN : sample.toHostLoad();
    }

    protected void clear() {
        samples.clear();
    }

    private static double clamp(double value) {
        if (Double.isNaN(value) || value < 0) {
            return 0;
        }
        return Math.min(value, 1);
    }

    @Override
    public String getConfigComponentName() {
        return HostLoadTracker.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {HostLoadSampleInterval, HostLoadHalfLife};
    }

    /**
     * One host's running average. Weighting is by elapsed time rather than by sample count, so a
     * missed poll decays the old value by the right amount instead of over-weighting it.
     */
    private static final class Sample {
        private final double cpu;
        private final double memory;
        private final long updatedAt;
        private final long count;

        private Sample(double cpu, double memory, long updatedAt) {
            this(cpu, memory, updatedAt, 1);
        }

        private Sample(double cpu, double memory, long updatedAt, long count) {
            this.cpu = cpu;
            this.memory = memory;
            this.updatedAt = updatedAt;
            this.count = count;
        }

        private Sample fold(double newCpu, double newMemory, long now, int halfLifeSeconds) {
            double alpha = alpha(now - updatedAt, halfLifeSeconds);
            return new Sample(cpu + alpha * (newCpu - cpu), memory + alpha * (newMemory - memory), now, count + 1);
        }

        private static double alpha(long elapsedMillis, int halfLifeSeconds) {
            if (halfLifeSeconds <= 0 || elapsedMillis <= 0) {
                return 1;
            }
            return 1 - Math.exp(-(elapsedMillis / 1000.0) * Math.log(2) / halfLifeSeconds);
        }

        private HostLoad toHostLoad() {
            return new HostLoad(cpu, memory, count);
        }
    }
}
