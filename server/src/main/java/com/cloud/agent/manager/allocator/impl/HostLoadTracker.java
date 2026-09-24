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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;

import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.server.StatsCollector;
import com.cloud.deploy.DeploymentClusterPlanner;
import com.cloud.deploy.DeploymentPlanner.AllocationAlgorithm;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;

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
 *
 * What getCpuUtilization means depends on the hypervisor, and only KVM reports what this class
 * assumes:
 *
 * <ul>
 *   <li>KVM reports busy time as a percentage of the host's cores, which is what is wanted.</li>
 *   <li>VMware reports the share of CPU that is reserved rather than the share that is busy, so
 *       the CPU term becomes a second allocation signal there rather than a load signal.</li>
 *   <li>XenServer sums per-core averages without dividing by core count, so the value ranges up to
 *       the number of cores and is under-reported here by roughly that factor.</li>
 * </ul>
 *
 * Memory is taken as used over total and is sound everywhere.
 */
public class HostLoadTracker extends ManagerBase implements Configurable {

    public static final ConfigKey<Integer> HostLoadSampleInterval = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Integer.class, "host.load.sample.interval", "60",
            "Seconds between samples of host CPU and memory utilisation, for placement algorithms that " +
                    "consider actual load. Should not be shorter than host.stats.interval.",
            false, ConfigKey.Scope.Global);

    public static final ConfigKey<Integer> HostLoadStaleAfter = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Integer.class, "host.load.stale.after", "600",
            "Seconds after which a host's utilisation average is considered out of date and stops being used " +
                    "for placement. A host whose agent stops reporting would otherwise keep vouching for itself " +
                    "with figures that never change.",
            true, ConfigKey.Scope.Global);

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

    private ScheduledExecutorService executor;

    @Override
    public boolean start() {
        int interval = Math.max(1, HostLoadSampleInterval.value());
        executor = Executors.newSingleThreadScheduledExecutor(
                new NamedThreadFactory("HostLoadTracker"));
        // catch Throwable: an escaping error would cancel all future runs, and the failure would be
        // silent - placement would quietly go back to ranking on allocation alone
        executor.scheduleWithFixedDelay(new ManagedContextRunnable() {
            @Override
            protected void runInContext() {
                try {
                    sampleAllHosts();
                } catch (Throwable t) {
                    logger.warn("Unable to sample host load", t);
                }
            }
        }, interval, interval, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
        return true;
    }

    protected void sampleAllHosts() {
        if (!isInUse()) {
            samples.clear();
            return;
        }
        for (HostVO host : hostDao.listByType(com.cloud.host.Host.Type.Routing)) {
            if (host.getStatus() != Status.Up) {
                samples.remove(host.getId());
                continue;
            }
            record(host.getId(), statsCollector.getHostStats(host.getId()));
        }
    }

    /**
     * Only the placement algorithms that read these figures pay for collecting them.
     */
    protected boolean isInUse() {
        return AllocationAlgorithm.balancedweighted.toString()
                .equals(DeploymentClusterPlanner.VmAllocationAlgorithm.value());
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

        Sample previous = samples.get(hostId);
        if (previous != null && previous.isSameReadingAs(stats)) {
            // StatsCollector keeps the previous entry when a poll fails, so an unchanged object is
            // a reading we have already folded, not a fresh measurement
            return;
        }

        // getCpuUtilization is a percentage of the host's real cores. That holds for KVM; see the
        // class javadoc for what it means on other hypervisors.
        double cpu = clamp(stats.getCpuUtilization() / 100.0);
        double memory = clamp((totalMemory - stats.getFreeMemoryKBs()) / totalMemory);
        int halfLife = HostLoadHalfLife.value();

        samples.compute(hostId, (id, current) -> current == null
                ? new Sample(cpu, memory, now, stats)
                : current.fold(cpu, memory, now, halfLife, stats));
    }

    public HostLoad getLoad(long hostId) {
        return getLoad(hostId, System.currentTimeMillis());
    }

    protected HostLoad getLoad(long hostId, long now) {
        Sample sample = samples.get(hostId);
        if (sample == null) {
            return HostLoad.UNKNOWN;
        }
        long staleAfter = Math.max(1, HostLoadStaleAfter.value()) * 1000L;
        if (now - sample.updatedAt > staleAfter) {
            // the host has stopped reporting; stop letting its last known figures speak for it
            return HostLoad.UNKNOWN;
        }
        return sample.toHostLoad();
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
        return new ConfigKey<?>[] {HostLoadSampleInterval, HostLoadHalfLife, HostLoadStaleAfter};
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
        private final HostStats reading;

        private Sample(double cpu, double memory, long updatedAt, HostStats reading) {
            this(cpu, memory, updatedAt, 1, reading);
        }

        private Sample(double cpu, double memory, long updatedAt, long count, HostStats reading) {
            this.cpu = cpu;
            this.memory = memory;
            this.updatedAt = updatedAt;
            this.count = count;
            this.reading = reading;
        }

        private boolean isSameReadingAs(HostStats stats) {
            return reading == stats;
        }

        private Sample fold(double newCpu, double newMemory, long now, int halfLifeSeconds, HostStats reading) {
            double alpha = alpha(now - updatedAt, halfLifeSeconds);
            return new Sample(cpu + alpha * (newCpu - cpu), memory + alpha * (newMemory - memory), now,
                    count + 1, reading);
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
