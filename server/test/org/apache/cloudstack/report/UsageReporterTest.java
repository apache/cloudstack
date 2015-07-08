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
package org.apache.cloudstack.report;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage.ProvisioningType;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.upgrade.dao.VersionDao;
import com.cloud.upgrade.dao.VersionVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.util.concurrent.AtomicLongMap;

import junit.framework.Assert;

@SuppressWarnings("deprecation")
public class UsageReporterTest {

    Map<String, String> configs;
    UsageReporter reporter;
    List<ClusterVO> clusters = new ArrayList<ClusterVO>();
    List<VersionVO> versions = new ArrayList<VersionVO>();
    List<DataCenterVO> datacenters = new ArrayList<DataCenterVO>();
    List<HostVO> hosts = new ArrayList<HostVO>();
    List<StoragePoolVO> pools = new ArrayList<StoragePoolVO>();
    List<DiskOfferingVO> diskofferings = new ArrayList<DiskOfferingVO>();

    @Before
    public void setUp() {
        configs = new HashMap<String, String>();
        reporter = UsageReporter.getInstance(configs);

        reporter._versionDao = Mockito.mock(VersionDao.class);
        reporter._userVmDao = Mockito.mock(UserVmDao.class);
        reporter._hostDao = Mockito.mock(HostDao.class);
        reporter._clusterDao = Mockito.mock(ClusterDao.class);
        reporter._storagePoolDao = Mockito.mock(PrimaryDataStoreDao.class);
        reporter._dataCenterDao = Mockito.mock(DataCenterDao.class);
        reporter._vmInstance = Mockito.mock(VMInstanceDao.class);
        reporter._diskOfferingDao = Mockito.mock(DiskOfferingDao.class);

        VersionVO version;
        for (int i = 0; i < 10; i++) {
            /* 1443650400 = 01-10-2015 00:00:00 */
            long date = 1443650400 + i;
            version = new VersionVO("1.2." + i);
            version.setUpdated(new Date(date * 1000));
            versions.add(version);
        }

        DataCenterVO dc;
        for (int i = 0; i < 3; i++) {
            dc = new DataCenterVO(i, "Zone-" + i, "Mocked Zone", "8.8.8.8", "8.8.4.4", "", "", "24", "cloud.local", (long) i,
                                  NetworkType.Basic, "", "");
            dc.setDnsProvider("VirtualRouter");
            dc.setDhcpProvider("VirtualRouter");
            dc.setUserDataProvider("VirtualRouter");
            dc.setLoadBalancerProvider("ElasticLoadBalancerVm");
            if (i == 1) {
                dc.setNetworkType(NetworkType.Advanced);
            }
            datacenters.add(dc);
        }

        HostVO host;
        for (int i = 0; i < 100; i++) {
            host = new HostVO(UUID.randomUUID().toString());
            host.setType(Host.Type.Routing);
            host.setHypervisorType(HypervisorType.KVM);
            host.setVersion("1.2.3");
            hosts.add(host);

            List<UserVmVO> vms = new ArrayList<UserVmVO>();
            UserVmVO vm = null;
            VMInstanceVO instance;
            for (int j = 0; j < 100; j++) {
                boolean haEnabled = false;
                if (j % 2 == 0) {
                    haEnabled = true;
                }

                String instanceName = "i-" + j;
                HypervisorType hypervisorType = HypervisorType.KVM;

                Type instanceType = null;
                switch (j) {
                    case 10:
                        instanceType = Type.ConsoleProxy;
                        break;
                    case 11:
                        instanceType = Type.DomainRouter;
                        break;
                    case 12:
                        instanceType = Type.SecondaryStorageVm;
                        break;
                    default:
                        instanceType = Type.User;
                }

                vm = new UserVmVO(j, instanceName, instanceName, (long) j, hypervisorType, (long) j, haEnabled,
                                  false, (long) j, (long) j, (long) j, (long) j, "", instanceName, (long) j);
                instance = new VMInstanceVO(j, (long) j, instanceName, instanceName, instanceType, (long) j, hypervisorType, (long) j,
                                            (long) j, (long) j, (long) j, haEnabled);
                instance.setState(State.Running);

                Mockito.when(reporter._vmInstance.findById(Matchers.eq((long)j))).thenReturn(instance);

                vms.add(vm);
            }

            Mockito.when(reporter._userVmDao.listByLastHostId(Matchers.eq((long)i))).thenReturn(vms);
        }

        ClusterVO cluster;
        for (int i = 0; i < 10; i++) {
            cluster = new ClusterVO();
            cluster.setHypervisorType("KVM");
            clusters.add(cluster);
        }

        StoragePoolVO pool;
        for (int i = 0; i < 5; i++) {
            pool = new StoragePoolVO();
            pool.setPoolType(StoragePoolType.NetworkFilesystem);
            pool.setStorageProviderName("DefaultPrimary");
            pool.setScope(ScopeType.ZONE);
            pools.add(pool);
        }

        DiskOfferingVO offering;
        for (int i = 0; i < 10; i++) {
            offering = new DiskOfferingVO();
            offering.setProvisioningType(ProvisioningType.THIN);
            if (i == 6) {
                offering.setSystemUse(true);
                offering.setUseLocalStorage(true);
            }
            offering.setDiskSize(i * 1024);
            diskofferings.add(offering);
        }

        Mockito.when(reporter._versionDao.getAllVersions()).thenReturn(versions);
        Mockito.when(reporter._versionDao.getCurrentVersion()).thenReturn(versions.get(versions.size() - 1).getVersion());
        Mockito.when(reporter._hostDao.search(Matchers.any(SearchCriteria.class), Matchers.any(Filter.class))).thenReturn(hosts);
        Mockito.when(reporter._clusterDao.search(Matchers.any(SearchCriteria.class), Matchers.any(Filter.class))).thenReturn(clusters);
        Mockito.when(reporter._storagePoolDao.listAll()).thenReturn(pools);
        Mockito.when(reporter._dataCenterDao.listAllZones()).thenReturn(datacenters);
        Mockito.when(reporter._diskOfferingDao.findPrivateDiskOffering()).thenReturn(diskofferings);
        Mockito.when(reporter._diskOfferingDao.findPublicDiskOfferings()).thenReturn(diskofferings);
    }

    @Test
    public void testGetUniqueId() {
        String uniqueID = reporter.getUniqueId();
        // The UniqueID is calculated based on the first installation version + date. It's hashed with SHA256.
        Assert.assertTrue("Unique ID does not match", "cad45ae9662ed75de88a2a383cd09c03c37ef6862f96803592bde11a60c99e3e".equals(uniqueID));
    }

    @Test
    public void testGetVersionReport() {
        Map<String, String> report = reporter.getVersionReport();
        Assert.assertEquals(versions.size(), report.size());
    }

    @Test
    public void testGetCurrentVersion() {
        Assert.assertTrue("Current version does not match", "1.2.9".equals(reporter.getCurrentVersion()));
    }

    @Test
    public void testGetHostReport() {
        Map<String, AtomicLongMap> report = reporter.getHostReport();
        Assert.assertEquals(3, report.size());
    }

    @Test
    public void testGetClusterReport() {
        Map<String, AtomicLongMap> report = reporter.getClusterReport();
        Assert.assertEquals(2, report.size());
    }

    @Test
    public void testGetStoragePoolReport() {
        Map<String, AtomicLongMap> report = reporter.getStoragePoolReport();
        Assert.assertEquals(3, report.size());
    }

    @Test
    public void testGetDataCenterReport() {
        Map<String, AtomicLongMap> report = reporter.getDataCenterReport();
        Assert.assertEquals(8, report.size());
    }

    @Test
    public void testGetInstanceReport() {
        Map<String, AtomicLongMap> report = reporter.getInstanceReport();
        Assert.assertEquals(5, report.size());
    }

    @Test
    public void testGetDiskOfferingReport() {
        Map<String, Object> report = reporter.getDiskOfferingReport();
        Assert.assertEquals(4, report.size());
    }

    @Test
    public void testGetReport() {
        Map<String, Object> report = reporter.getReport();
        Assert.assertEquals(9, report.size());
    }
}
