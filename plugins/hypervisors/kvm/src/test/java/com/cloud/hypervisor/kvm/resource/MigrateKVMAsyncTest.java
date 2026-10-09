//
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
//

package com.cloud.hypervisor.kvm.resource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.TypedParameter;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class MigrateKVMAsyncTest {

    @Mock
    private LibvirtComputingResource libvirtComputingResource;
    @Mock
    private Connect connect;
    @Mock
    private Domain domain;


    @Test
    public void createTypedParameterListTestNoMigrateDiskLabels() {
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "testxml",
                false, false, false, false, false, false, "", 0, "tst", "1.1.1.1", null, null);

        Mockito.doReturn(10).when(libvirtComputingResource).getMigrateSpeed();

        TypedParameter[] result = migrateKVMAsync.createTypedParameterList(6000000L);

        Assert.assertEquals(4, result.length);

        Assert.assertEquals("tst", result[0].getValueAsString());
        Assert.assertEquals("testxml", result[1].getValueAsString());
        Assert.assertEquals("tcp:1.1.1.1", result[2].getValueAsString());
        Assert.assertEquals("10", result[3].getValueAsString());

    }

    @Test
    public void createTypedParameterListTestWithMigrateDiskLabels() {
        Set<String> labels = Set.of("vda", "vdb");
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "testxml",
                false, false, false, false, false, false, "", 0, "tst", "1.1.1.1", null, labels);

        Mockito.doReturn(10).when(libvirtComputingResource).getMigrateSpeed();

        TypedParameter[] result = migrateKVMAsync.createTypedParameterList(6000000L);

        Assert.assertEquals(6, result.length);

        Assert.assertEquals("tst", result[0].getValueAsString());
        Assert.assertEquals("testxml", result[1].getValueAsString());
        Assert.assertEquals("tcp:1.1.1.1", result[2].getValueAsString());
        Assert.assertEquals("10", result[3].getValueAsString());

        Assert.assertEquals(labels, Set.of(result[4].getValueAsString(), result[5].getValueAsString()));
    }

    @Test
    public void buildMigrateFlagsSetsTlsWhenEncryptionEnabled() {
        // with migrate encryption enabled and a TLS-capable libvirt, VIR_MIGRATE_TLS (1<<16) is set.
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "xml",
                false, false, false, true, false, false, "", 0, "tst", "1.1.1.1", null, null);
        long flags = migrateKVMAsync.buildMigrateFlags(9000000L);
        Assert.assertTrue("VIR_MIGRATE_TLS must be set when encryption is enabled", (flags & 65536L) != 0L);
    }

    @Test(expected = com.cloud.utils.exception.CloudRuntimeException.class)
    public void buildMigrateFlagsFailsClosedWhenEncryptionRequiredButLibvirtTooOld() {
        // encryption Required on libvirt < 3.2.0 must FAIL, not silently send the memory stream in plaintext.
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "xml",
                false, false, false, true, false, false, "", 0, "tst", "1.1.1.1", null, null);
        migrateKVMAsync.buildMigrateFlags(3000000L);
    }

    @Test
    public void buildMigrateFlagsOmitsTlsWhenEncryptionDisabled() {
        // with encryption disabled, VIR_MIGRATE_TLS must not be set (plaintext, prior behavior).
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "xml",
                false, false, false, false, false, false, "", 0, "tst", "1.1.1.1", null, null);
        long flags = migrateKVMAsync.buildMigrateFlags(9000000L);
        Assert.assertEquals("VIR_MIGRATE_TLS must NOT be set when encryption is disabled", 0L, (flags & 65536L));
    }

    @Test
    public void buildMigrateFlagsSetsParallelWhenEnabled() {
        // with parallel migration enabled and a capable libvirt, VIR_MIGRATE_PARALLEL (1<<17) is set.
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "xml",
                false, false, false, false, true, false, "", 0, "tst", "1.1.1.1", null, null);
        long flags = migrateKVMAsync.buildMigrateFlags(9000000L);
        Assert.assertTrue("VIR_MIGRATE_PARALLEL must be set when parallel migration is enabled", (flags & 131072L) != 0L);
        // legacy xbzrle compression must NOT be combined with multifd (QEMU refuses it).
        Assert.assertEquals("legacy VIR_MIGRATE_COMPRESSED must be OFF with multifd", 0L, (flags & 2048L));
    }

    @Test
    public void buildMigrateFlagsLegacyCompressionOnlyWhenNotParallel() {
        // VIR_MIGRATE_COMPRESSED (2048) is set for a normal migration but omitted with multifd.
        MigrateKVMAsync notParallel = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "xml",
                false, false, false, false, false, false, "", 0, "tst", "1.1.1.1", null, null);
        Assert.assertTrue("compression on for non-parallel migration", (notParallel.buildMigrateFlags(9000000L) & 2048L) != 0L);
        MigrateKVMAsync parallel = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "xml",
                false, false, false, false, true, false, "", 0, "tst", "1.1.1.1", null, null);
        Assert.assertEquals("compression off for multifd migration", 0L, (parallel.buildMigrateFlags(9000000L) & 2048L));
    }

    @Test
    public void buildMigrateFlagsSetsUnsafeWhenAllowed() {
        // with migrate.allow.unsafe, VIR_MIGRATE_UNSAFE (1<<9 = 512) is set so writeback-cache
        // VMs on coherent storage (Ceph) can live-migrate instead of being refused by libvirt.
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "xml",
                false, false, false, false, false, true, "", 0, "tst", "1.1.1.1", null, null);
        long flags = migrateKVMAsync.buildMigrateFlags(9000000L);
        Assert.assertTrue("VIR_MIGRATE_UNSAFE must be set when migrate.allow.unsafe is on", (flags & 512L) != 0L);
    }

    @Test
    public void createTypedParameterListIncludesCompressionWhenMethodSet() {
        // a configured compression method adds the VIR_MIGRATE_PARAM_COMPRESSION ("compression") param.
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "xml",
                false, false, false, false, false, false, "xbzrle", 0, "tst", "1.1.1.1", null, null);
        Mockito.doReturn(10).when(libvirtComputingResource).getMigrateSpeed();
        TypedParameter[] result = migrateKVMAsync.createTypedParameterList(6000000L);
        Assert.assertEquals("5 fixed params when a compression method is set", 5, result.length);
        Assert.assertEquals("xbzrle", result[4].getValueAsString());
    }

    @Test
    public void createTypedParameterListOmitsCompressionWhenMethodBlank() {
        // a blank compression method preserves the prior 4-param behaviour.
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "xml",
                false, false, false, false, false, false, "", 0, "tst", "1.1.1.1", null, null);
        Mockito.doReturn(10).when(libvirtComputingResource).getMigrateSpeed();
        TypedParameter[] result = migrateKVMAsync.createTypedParameterList(6000000L);
        Assert.assertEquals("no compression param when method blank", 4, result.length);
    }

    @Test
    public void createTypedParameterListBindsListenAddressWhenDedicatedMigrationNetwork() {
        // a dedicated migration address adds the VIR_MIGRATE_PARAM_LISTEN_ADDRESS ("listen_address") param
        // and the data URI targets that address, so the stream lands on the migration NIC.
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "xml",
                false, false, false, false, false, false, "", 0, "tst", "192.0.2.17", "192.0.2.17", null);
        Mockito.doReturn(10).when(libvirtComputingResource).getMigrateSpeed();
        TypedParameter[] result = migrateKVMAsync.createTypedParameterList(6000000L);
        Assert.assertEquals("5 fixed params when a migration listen address is set", 5, result.length);
        Assert.assertEquals("tcp:192.0.2.17", result[2].getValueAsString());
        Assert.assertEquals("192.0.2.17", result[4].getValueAsString());
    }

    @Test
    public void createTypedParameterListOmitsListenAddressWhenBlank() {
        // with no dedicated migration address, the prior 4-param behaviour is preserved.
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "xml",
                false, false, false, false, false, false, "", 0, "tst", "1.1.1.1", null, null);
        Mockito.doReturn(10).when(libvirtComputingResource).getMigrateSpeed();
        TypedParameter[] result = migrateKVMAsync.createTypedParameterList(6000000L);
        Assert.assertEquals("no listen-address param when blank", 4, result.length);
    }

    @Test
    public void createTypedParameterListIncludesParallelConnectionsWhenParallelEnabled() {
        // parallel migration enabled with an explicit channel count adds the parallel.connections param.
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "xml",
                false, false, false, false, true, false, "", 4, "tst", "1.1.1.1", null, null);
        Mockito.doReturn(10).when(libvirtComputingResource).getMigrateSpeed();
        TypedParameter[] result = migrateKVMAsync.createTypedParameterList(6000000L);
        Assert.assertEquals("extra param for parallel connections", 5, result.length);
        Assert.assertEquals(4, ((org.libvirt.TypedIntParameter) result[4]).value);
    }

    @Test
    public void createTypedParameterListOmitsParallelConnectionsWhenParallelDisabled() {
        // a channel count is ignored unless parallel migration is enabled (libvirt ignores it anyway).
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "xml",
                false, false, false, false, false, false, "", 4, "tst", "1.1.1.1", null, null);
        Mockito.doReturn(10).when(libvirtComputingResource).getMigrateSpeed();
        TypedParameter[] result = migrateKVMAsync.createTypedParameterList(6000000L);
        Assert.assertEquals("no parallel-connections param when parallel disabled", 4, result.length);
    }

    @Test
    public void createTypedParameterListOmitsParallelConnectionsOnOldLibvirt() {
        // libvirt < 5.2.0 does not set VIR_MIGRATE_PARALLEL, so the connections param must be omitted too,
        // otherwise libvirt rejects the migration with "Turn parallel migration on to tune it".
        MigrateKVMAsync migrateKVMAsync = new MigrateKVMAsync(libvirtComputingResource, domain, connect, "xml",
                false, false, false, false, true, false, "", 4, "tst", "1.1.1.1", null, null);
        Mockito.doReturn(10).when(libvirtComputingResource).getMigrateSpeed();
        TypedParameter[] result = migrateKVMAsync.createTypedParameterList(5001000L);
        Assert.assertEquals("no parallel-connections param on old libvirt", 4, result.length);
    }

}
