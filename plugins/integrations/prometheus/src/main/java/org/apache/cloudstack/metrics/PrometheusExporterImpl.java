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
package org.apache.cloudstack.metrics;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.log4j.Logger;

import com.cloud.alert.AlertManager;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.dao.DomainJoinDao;
import com.cloud.api.query.dao.HostJoinDao;
import com.cloud.api.query.dao.StoragePoolJoinDao;
import com.cloud.api.query.vo.DomainJoinVO;
import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.api.query.vo.StoragePoolJoinVO;
import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityManager;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.capacity.dao.CapacityDaoImpl;
import com.cloud.configuration.Resource;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterIpAddressDao;
import com.cloud.host.Host;
import com.cloud.host.Status;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.storage.ImageStore;
import com.cloud.storage.StorageStats;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.base.Strings;

public class PrometheusExporterImpl extends ManagerBase implements PrometheusExporter, Manager {
    private static final Logger LOG = Logger.getLogger(PrometheusExporterImpl.class);

    private static final String USED = "used";
    private static final String ALLOCATED = "allocated";
    private static final String UNALLOCATED = "unallocated";
    private static final String TOTAL = "total";
    private static final String ONLINE = "online";
    private static final String OFFLINE = "offline";

    private static List<Item> metricsItems = new ArrayList<>();

    @Inject
    private DataCenterDao dcDao;
    @Inject
    private HostJoinDao hostJoinDao;
    @Inject
    private VMInstanceDao vmDao;
    @Inject
    private VolumeDao volumeDao;
    @Inject
    private IPAddressDao publicIpAddressDao;
    @Inject
    private DataCenterIpAddressDao privateIpAddressDao;
    @Inject
    private CapacityDao capacityDao;
    @Inject
    private StoragePoolJoinDao storagePoolJoinDao;
    @Inject
    private ImageStoreDao imageStoreDao;
    @Inject
    private DomainJoinDao domainDao;
    @Inject
    private AlertManager alertManager;

    public PrometheusExporterImpl() {
        super();
    }

    private void addHostMetrics(final List<Item> metricsList, final long dcId, final String zoneName, final String zoneUuid) {
        int total = 0;
        int up = 0;
        int down = 0;
        for (final HostJoinVO host : hostJoinDao.listAll()) {
            if (host == null || host.getType() != Host.Type.Routing || host.getZoneId() != dcId) {
                continue;
            }
            total++;
            if (host.getStatus() == Status.Up) {
                up++;
            } else if (host.getStatus() == Status.Disconnected || host.getStatus() == Status.Down) {
                down++;
            }

            final String cpuFactor = String.valueOf(CapacityManager.CpuOverprovisioningFactor.valueIn(host.getClusterId()));
            final CapacityVO cpuCapacity = capacityDao.findByHostIdType(host.getId(), Capacity.CAPACITY_TYPE_CPU);
            if (cpuCapacity != null) {
                metricsList.add(new ItemHostCpu(zoneName, zoneUuid, host.getName(), host.getUuid(), host.getPrivateIpAddress(), cpuFactor, USED, cpuCapacity.getUsedCapacity()));
                metricsList.add(new ItemHostCpu(zoneName, zoneUuid, host.getName(), host.getUuid(), host.getPrivateIpAddress(), cpuFactor, TOTAL, cpuCapacity.getTotalCapacity()));
            } else {
                metricsList.add(new ItemHostCpu(zoneName, zoneUuid, host.getName(), host.getUuid(), host.getPrivateIpAddress(), cpuFactor, USED, 0L));
                metricsList.add(new ItemHostCpu(zoneName, zoneUuid, host.getName(), host.getUuid(), host.getPrivateIpAddress(), cpuFactor, TOTAL, 0L));
            }

            final String memoryFactor = String.valueOf(CapacityManager.MemOverprovisioningFactor.valueIn(host.getClusterId()));
            final CapacityVO memCapacity = capacityDao.findByHostIdType(host.getId(), Capacity.CAPACITY_TYPE_MEMORY);
            if (memCapacity != null) {
                metricsList.add(new ItemHostMemory(zoneName, zoneUuid, host.getName(), host.getUuid(), host.getPrivateIpAddress(), memoryFactor, USED, memCapacity.getUsedCapacity()));
                metricsList.add(new ItemHostMemory(zoneName, zoneUuid, host.getName(), host.getUuid(), host.getPrivateIpAddress(), memoryFactor, TOTAL, memCapacity.getTotalCapacity()));
            } else {
                metricsList.add(new ItemHostMemory(zoneName, zoneUuid, host.getName(), host.getUuid(), host.getPrivateIpAddress(), memoryFactor, USED, 0L));
                metricsList.add(new ItemHostMemory(zoneName, zoneUuid, host.getName(), host.getUuid(), host.getPrivateIpAddress(), memoryFactor, TOTAL, 0L));
            }

            metricsList.add(new ItemHostVM(zoneName, zoneUuid, host.getName(), host.getUuid(), host.getPrivateIpAddress(), vmDao.listByHostId(host.getId()).size()));

            final CapacityVO coreCapacity = capacityDao.findByHostIdType(host.getId(), Capacity.CAPACITY_TYPE_CPU_CORE);
            if (coreCapacity != null) {
                metricsList.add(new ItemVMCore(zoneName, zoneUuid, host.getName(), host.getUuid(), host.getPrivateIpAddress(), USED, coreCapacity.getUsedCapacity()));
                metricsList.add(new ItemVMCore(zoneName, zoneUuid, host.getName(), host.getUuid(), host.getPrivateIpAddress(), TOTAL, coreCapacity.getTotalCapacity()));
            } else {
                metricsList.add(new ItemVMCore(zoneName, zoneUuid, host.getName(), host.getUuid(), host.getPrivateIpAddress(), USED, 0L));
                metricsList.add(new ItemVMCore(zoneName, zoneUuid, host.getName(), host.getUuid(), host.getPrivateIpAddress(), TOTAL, 0L));
            }
        }

        final List<CapacityDaoImpl.SummedCapacity> cpuCapacity = capacityDao.findCapacityBy((int) Capacity.CAPACITY_TYPE_CPU, dcId, null, null);
        if (cpuCapacity != null && cpuCapacity.size() > 0) {
            metricsList.add(new ItemHostCpu(zoneName, zoneUuid, null, null, null, null, ALLOCATED, cpuCapacity.get(0).getAllocatedCapacity() != null ? cpuCapacity.get(0).getAllocatedCapacity() : 0));
        }

        final List<CapacityDaoImpl.SummedCapacity> memCapacity = capacityDao.findCapacityBy((int) Capacity.CAPACITY_TYPE_MEMORY, dcId, null, null);
        if (memCapacity != null && memCapacity.size() > 0) {
            metricsList.add(new ItemHostMemory(zoneName, zoneUuid, null, null, null, null, ALLOCATED, memCapacity.get(0).getAllocatedCapacity() != null ? memCapacity.get(0).getAllocatedCapacity() : 0));
        }

        final List<CapacityDaoImpl.SummedCapacity> coreCapacity = capacityDao.findCapacityBy((int) Capacity.CAPACITY_TYPE_CPU_CORE, dcId, null, null);
        if (coreCapacity != null && coreCapacity.size() > 0) {
            metricsList.add(new ItemVMCore(zoneName, zoneUuid, null, null, null, ALLOCATED, coreCapacity.get(0).getAllocatedCapacity() != null ? coreCapacity.get(0).getAllocatedCapacity() : 0));
        }

        metricsList.add(new ItemHost(zoneName, zoneUuid, ONLINE, up));
        metricsList.add(new ItemHost(zoneName, zoneUuid, OFFLINE, down));
        metricsList.add(new ItemHost(zoneName, zoneUuid, TOTAL, total));
    }

    private void addVMMetrics(final List<Item> metricsList, final long dcId, final String zoneName, final String zoneUuid) {
        for (final State state : State.values()) {
            final Long count = vmDao.countByZoneAndState(dcId, state);
            if (count == null) {
                continue;
            }
            metricsList.add(new ItemVM(zoneName, zoneUuid, state.name().toLowerCase(), count));
        }
    }

    private void addVolumeMetrics(final List<Item> metricsList, final long dcId, final String zoneName, final String zoneUuid) {
        int total = 0;
        int ready = 0;
        int destroyed = 0;
        for (final VolumeVO volume : volumeDao.findByDc(dcId)) {
            if (volume == null) {
                continue;
            }
            total++;
            if (volume.getState() == Volume.State.Ready) {
                ready++;
            } else if (volume.getState() == Volume.State.Destroy) {
                destroyed++;
            }
        }
        metricsList.add(new ItemVolume(zoneName, zoneUuid, Volume.State.Ready.name().toLowerCase(), ready));
        metricsList.add(new ItemVolume(zoneName, zoneUuid, Volume.State.Destroy.name().toLowerCase(), destroyed));
        metricsList.add(new ItemVolume(zoneName, zoneUuid, TOTAL, total));
    }

    private void addStorageMetrics(final List<Item> metricsList, final long dcId, final String zoneName, final String zoneUuid) {
        for (final StoragePoolJoinVO pool: storagePoolJoinDao.listAll()) {
            if (pool == null || pool.getZoneId() != dcId) {
                continue;
            }
            final String poolName = pool.getName();
            final String poolPath = pool.getHostAddress() + ":" + pool.getPath();

            long usedCapacity = 0L;
            long allocatedCapacity = pool.getUsedCapacity() + pool.getReservedCapacity();
            final long totalCapacity = pool.getCapacityBytes();

            final StorageStats stats = ApiDBUtils.getStoragePoolStatistics(pool.getId());
            if (stats != null) {
                usedCapacity = stats.getByteUsed();
            }

            final BigDecimal poolOverProvisioningFactor = BigDecimal.valueOf(CapacityManager.StorageOverprovisioningFactor.valueIn(pool.getId()));
            final String poolFactor = poolOverProvisioningFactor.toString();

            metricsList.add(new ItemPool(zoneName, zoneUuid, poolName, poolPath, "primary", poolFactor, USED, usedCapacity));
            metricsList.add(new ItemPool(zoneName, zoneUuid, poolName, poolPath, "primary", poolFactor, ALLOCATED, allocatedCapacity));
            metricsList.add(new ItemPool(zoneName, zoneUuid, poolName, poolPath, "primary", poolFactor, UNALLOCATED, poolOverProvisioningFactor.multiply(BigDecimal.valueOf(totalCapacity)).longValue() - allocatedCapacity));
            metricsList.add(new ItemPool(zoneName, zoneUuid, poolName, poolPath, "primary", poolFactor, TOTAL, totalCapacity));
        }

        for (final ImageStore imageStore : imageStoreDao.findByZone(new ZoneScope(dcId), null)) {
            final StorageStats stats = ApiDBUtils.getSecondaryStorageStatistics(imageStore.getId());
            metricsList.add(new ItemPool(zoneName, zoneUuid, imageStore.getName(), imageStore.getUrl(), "secondary", null, USED, stats != null ? stats.getByteUsed() : 0));
            metricsList.add(new ItemPool(zoneName, zoneUuid, imageStore.getName(), imageStore.getUrl(), "secondary", null, TOTAL, stats != null ? stats.getCapacityBytes() : 0));
        }
    }

    private void addIpAddressMetrics(final List<Item> metricsList, final long dcId, final String zoneName, final String zoneUuid) {
        metricsList.add(new ItemPrivateIp(zoneName, zoneUuid, ALLOCATED, privateIpAddressDao.countIPs(dcId, true)));
        metricsList.add(new ItemPrivateIp(zoneName, zoneUuid, TOTAL, privateIpAddressDao.countIPs(dcId, false)));
        metricsList.add(new ItemPublicIp(zoneName, zoneUuid, ALLOCATED, publicIpAddressDao.countIPsForNetwork(dcId, true, Vlan.VlanType.VirtualNetwork)));
        metricsList.add(new ItemPublicIp(zoneName, zoneUuid, TOTAL, publicIpAddressDao.countIPsForNetwork(dcId, false, Vlan.VlanType.VirtualNetwork)));
        metricsList.add(new ItemSharedNetworkIp(zoneName, zoneUuid, ALLOCATED, publicIpAddressDao.countIPsForNetwork(dcId, true, Vlan.VlanType.DirectAttached)));
        metricsList.add(new ItemSharedNetworkIp(zoneName, zoneUuid, TOTAL, publicIpAddressDao.countIPsForNetwork(dcId, false, Vlan.VlanType.DirectAttached)));
    }

    private void addVlanMetrics(final List<Item> metricsList, final long dcId, final String zoneName, final String zoneUuid) {
        metricsList.add(new ItemVlan(zoneName, zoneUuid, ALLOCATED, dcDao.countZoneVlans(dcId, true)));
        metricsList.add(new ItemVlan(zoneName, zoneUuid, TOTAL, dcDao.countZoneVlans(dcId, false)));
    }

    private void addDomainLimits(final List<Item> metricsList) {
        Long totalCpuLimit = 0L;
        Long totalMemoryLimit = 0L;

        for (final DomainJoinVO domain: domainDao.listAll()) {
            if (domain == null || domain.getLevel() != 1) {
                continue;
            }
            long cpuLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getCpuLimit(), false,
                    Resource.ResourceType.cpu, domain.getId());
            if (cpuLimit > 0) {
                totalCpuLimit += cpuLimit;
            }

            long memoryLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getMemoryLimit(), false,
                    Resource.ResourceType.memory, domain.getId());
            if (memoryLimit > 0) {
                totalMemoryLimit += memoryLimit;
            }
        }
        metricsList.add(new ItemDomainLimitCpu(totalCpuLimit));
        metricsList.add(new ItemDomainLimitMemory(totalMemoryLimit));
    }

    @Override
    public void updateMetrics() {
        final List<Item> latestMetricsItems = new ArrayList<Item>();
        try {
            for (final DataCenterVO dc : dcDao.listAll()) {
                final String zoneName = dc.getName();
                final String zoneUuid = dc.getUuid();
                alertManager.recalculateCapacity();
                addHostMetrics(latestMetricsItems, dc.getId(), zoneName, zoneUuid);
                addVMMetrics(latestMetricsItems, dc.getId(), zoneName, zoneUuid);
                addVolumeMetrics(latestMetricsItems, dc.getId(), zoneName, zoneUuid);
                addStorageMetrics(latestMetricsItems, dc.getId(), zoneName, zoneUuid);
                addIpAddressMetrics(latestMetricsItems, dc.getId(), zoneName, zoneUuid);
                addVlanMetrics(latestMetricsItems, dc.getId(), zoneName, zoneUuid);
            }
            addDomainLimits(latestMetricsItems);
        } catch (Exception e) {
            LOG.warn("Getting metrics failed ", e);
        }
        metricsItems = latestMetricsItems;
    }

    @Override
    public String getMetrics() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("# Cloudstack Prometheus Metrics\n");
        for (final Item item : metricsItems) {
            stringBuilder.append(item.toMetricsString()).append("\n");
        }
        return stringBuilder.toString();
    }

    private abstract class Item {
        String name;

        public Item(final String nm) {
            name = nm;
        }

        public abstract String toMetricsString();
    }

    class ItemVM extends Item {
        String zoneName;
        String zoneUuid;
        String filter;
        long total;

        public ItemVM(final String zn, final String zu, final String st, long cnt) {
            super("cloudstack_vms_total");
            zoneName = zn;
            zoneUuid = zu;
            filter = st;
            total = cnt;
        }

        @Override
        public String toMetricsString() {
            return String.format("%s{zone=\"%s\",filter=\"%s\"} %d", name, zoneName, filter, total);
        }
    }

    class ItemVolume extends Item {
        String zoneName;
        String zoneUuid;
        String filter;
        int total;

        public ItemVolume(final String zn, final String zu, final String st, int cnt) {
            super("cloudstack_volumes_total");
            zoneName = zn;
            zoneUuid = zu;
            filter = st;
            total = cnt;
        }

        @Override
        public String toMetricsString() {
            return String.format("%s{zone=\"%s\",filter=\"%s\"} %d", name, zoneName, filter, total);
        }
    }

    class ItemHost extends Item {
        String zoneName;
        String zoneUuid;
        String state;
        int total;

        public ItemHost(final String zn, final String zu, final String st, int cnt) {
            super("cloudstack_hosts_total");
            zoneName = zn;
            zoneUuid = zu;
            state = st;
            total = cnt;
        }

        @Override
        public String toMetricsString() {
            return String.format("%s{zone=\"%s\",filter=\"%s\"} %d", name, zoneName, state, total);
        }
    }

    class ItemVMCore extends Item {
        String zoneName;
        String zoneUuid;
        String hostName;
        String uuid;
        String ip;
        String filter;
        long core = 0;

        public ItemVMCore(final String zn, final String zu, final String hn, final String hu, final String hip, final String fl, final Long cr) {
            super("cloudstack_host_vms_cores_total");
            zoneName = zn;
            zoneUuid = zu;
            hostName = hn;
            uuid = hu;
            ip = hip;
            filter = fl;
            if (cr != null) {
                core = cr;
            }
        }

        @Override
        public String toMetricsString() {
            if (Strings.isNullOrEmpty(hostName) && Strings.isNullOrEmpty(ip)) {
                return String.format("%s{zone=\"%s\",filter=\"%s\"} %d", name, zoneName, filter, core);
            }
            return String.format("%s{zone=\"%s\",hostname=\"%s\",ip=\"%s\",filter=\"%s\"} %d", name, zoneName, hostName, ip, filter, core);
        }
    }

    class ItemHostCpu extends Item {
        String zoneName;
        String zoneUuid;
        String hostName;
        String uuid;
        String ip;
        String overProvisioningFactor;
        String filter;
        double mhertz;

        public ItemHostCpu(final String zn, final String zu, final String hn, final String hu, final String hip, final String of, final String fl, final double mh) {
            super("cloudstack_host_cpu_usage_mhz_total");
            zoneName = zn;
            zoneUuid = zu;
            hostName = hn;
            uuid = hu;
            ip = hip;
            overProvisioningFactor = of;
            filter = fl;
            mhertz = mh;
        }

        @Override
        public String toMetricsString() {
            if (Strings.isNullOrEmpty(hostName) && Strings.isNullOrEmpty(ip)) {
                return String.format("%s{zone=\"%s\",filter=\"%s\"} %.2f", name, zoneName, filter, mhertz);
            }
            return String.format("%s{zone=\"%s\",hostname=\"%s\",ip=\"%s\",overprovisioningfactor=\"%s\",filter=\"%s\"} %.2f", name, zoneName, hostName, ip, overProvisioningFactor, filter, mhertz);
        }
    }

    class ItemHostMemory extends Item {
        String zoneName;
        String zoneUuid;
        String hostName;
        String uuid;
        String ip;
        String overProvisioningFactor;
        String filter;
        double miBytes;

        public ItemHostMemory(final String zn, final String zu, final String hn, final String hu, final String hip, final String of, final String fl, final double membytes) {
            super("cloudstack_host_memory_usage_mibs_total");
            zoneName = zn;
            zoneUuid = zu;
            hostName = hn;
            uuid = hu;
            ip = hip;
            overProvisioningFactor = of;
            filter = fl;
            miBytes = membytes / (1024.0 * 1024.0);
        }

        @Override
        public String toMetricsString() {
            if (Strings.isNullOrEmpty(hostName) && Strings.isNullOrEmpty(ip)) {
                return String.format("%s{zone=\"%s\",filter=\"%s\"} %.2f", name, zoneName, filter, miBytes);
            }
            return String.format("%s{zone=\"%s\",hostname=\"%s\",ip=\"%s\",overprovisioningfactor=\"%s\",filter=\"%s\"} %.2f", name, zoneName, hostName, ip, overProvisioningFactor, filter, miBytes);
        }
    }

    class ItemHostVM extends Item {
        String zoneName;
        String zoneUuid;
        String hostName;
        String hostUuid;
        String hostIp;
        int total;

        public ItemHostVM(final String zoneName, final String zoneUuid, final String hostName, final String hostUuid, final String hostIp, final int total) {
            super("cloudstack_host_vms_total");
            this.zoneName = zoneName;
            this.zoneUuid = zoneUuid;
            this.hostName = hostName;
            this.hostUuid = hostUuid;
            this.hostIp = hostIp;
            this.total = total;
        }

        @Override
        public String toMetricsString() {
            return String.format("%s{zone=\"%s\",hostname=\"%s\",address=\"%s\"} %d", name, zoneName, hostName, hostIp, total);
        }
    }

    class ItemPool extends Item {
        String zoneName;
        String zoneUuid;
        String type;
        String overProvisioningFactor;
        String filter;
        String pname;
        String address;
        double total;

        public ItemPool(final String zn, final String zu, final String pn, final String pa, final String typ, final String of, final String fl, double cnt) {
            super("cloudstack_storage_pool_gibs_total");
            zoneName = zn;
            zoneUuid = zu;
            pname = pn;
            address = pa;
            type = typ;
            overProvisioningFactor = of;
            filter = fl;
            total = cnt / (1024.0 * 1024.0 * 1024.0);
        }

        @Override
        public String toMetricsString() {
            if (Strings.isNullOrEmpty(overProvisioningFactor)) {
                return String.format("%s{zone=\"%s\",name=\"%s\",address=\"%s\",type=\"%s\",filter=\"%s\"} %.2f", name, zoneName, pname, address, type, filter, total);
            }
            return String.format("%s{zone=\"%s\",name=\"%s\",address=\"%s\",type=\"%s\",overprovisioningfactor=\"%s\",filter=\"%s\"} %.2f", name, zoneName, pname, address, type, overProvisioningFactor, filter, total);
        }
    }

    class ItemPrivateIp extends Item {
        String zoneName;
        String zoneUuid;
        String filter;
        int total;

        public ItemPrivateIp(final String zn, final String zu, final String fl, int cnt) {
            super("cloudstack_private_ips_total");
            zoneName = zn;
            zoneUuid = zu;
            filter = fl;
            total = cnt;
        }

        @Override
        public String toMetricsString() {
            return String.format("%s{zone=\"%s\",filter=\"%s\"} %d", name, zoneName, filter, total);
        }
    }

    class ItemPublicIp extends Item {
        String zoneName;
        String zoneUuid;
        String filter;
        int total;

        public ItemPublicIp(final String zn, final String zu, final String fl, int cnt) {
            super("cloudstack_public_ips_total");
            zoneName = zn;
            zoneUuid = zu;
            filter = fl;
            total = cnt;
        }

        @Override
        public String toMetricsString() {
            return String.format("%s{zone=\"%s\",filter=\"%s\"} %d", name, zoneName, filter, total);
        }
    }

    class ItemSharedNetworkIp extends Item {
        String zoneName;
        String zoneUuid;
        String filter;
        int total;

        public ItemSharedNetworkIp(final String zn, final String zu, final String fl, int cnt) {
            super("cloudstack_shared_network_ips_total");
            zoneName = zn;
            zoneUuid = zu;
            filter = fl;
            total = cnt;
        }

        @Override
        public String toMetricsString() {
            return String.format("%s{zone=\"%s\",filter=\"%s\"} %d", name, zoneName, filter, total);
        }
    }

    class ItemVlan extends Item {
        String zoneName;
        String zoneUuid;
        String filter;
        int total;

        public ItemVlan(final String zn, final String zu, final String fl, int cnt) {
            super("cloudstack_vlans_total");
            zoneName = zn;
            zoneUuid = zu;
            filter = fl;
            total = cnt;
        }

        @Override
        public String toMetricsString() {
            return String.format("%s{zone=\"%s\",filter=\"%s\"} %d", name, zoneName, filter, total);
        }
    }

    class ItemDomainLimitCpu extends Item {
        long cores;

        public ItemDomainLimitCpu(final long c) {
            super("cloudstack_domain_limit_cpu_cores_total");
            cores = c;
        }

        @Override
        public String toMetricsString() {
            return String.format("%s %d", name, cores);
        }
    }

    class ItemDomainLimitMemory extends Item {
        long miBytes;

        public ItemDomainLimitMemory(final long mb) {
            super("cloudstack_domain_limit_memory_mibs_total");
            miBytes = mb;
        }

        @Override
        public String toMetricsString() {
            return String.format("%s %d", name, miBytes);
        }
    }
}
