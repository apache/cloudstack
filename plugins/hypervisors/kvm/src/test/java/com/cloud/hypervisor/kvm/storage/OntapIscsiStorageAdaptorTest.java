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
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.hypervisor.kvm.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Set;

import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.junit.Test;
import org.reflections.Reflections;

import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.exception.CloudRuntimeException;

public class OntapIscsiStorageAdaptorTest {

    @Test
    public void getStoragePoolTypeReturnsOntapIscsi() {
        assertEquals(StoragePoolType.OntapiSCSI, new OntapIscsiStorageAdaptor().getStoragePoolType());
    }

    @Test
    public void createdPoolCarriesOntapIscsiTypeAndRawFormat() {
        OntapIscsiStorageAdaptor adaptor = new OntapIscsiStorageAdaptor();

        KVMStoragePool pool = adaptor.createStoragePool("ontap-iscsi-pool-uuid", "10.0.0.1", 3260, null, null,
                StoragePoolType.OntapiSCSI, null, true);

        assertEquals(StoragePoolType.OntapiSCSI, pool.getType());
        // Attach builds a block-based disk off the physical disk format rather than the pool type,
        // which is why splitting OntapiSCSI out of Iscsi leaves the generated domain XML unchanged.
        assertEquals(PhysicalDiskFormat.RAW, pool.getDefaultFormat());
        assertSame(pool, adaptor.getStoragePool("ontap-iscsi-pool-uuid"));
    }

    /**
     * KVMStoragePoolManager discovers adaptors by a Reflections scan of its own package, instantiating
     * each concrete implementation through a no-arg constructor and keying it on getStoragePoolType().
     * A type with no adaptor silently falls back to LibvirtStorageAdaptor instead of failing at
     * startup, so this reproduces the discovery preconditions rather than waiting for the symptom.
     * The manager itself is not constructed here because doing so also instantiates
     * MultipathSCSIAdapterBase, which requires agent scripts resolvable from the working directory.
     */
    @Test
    public void adaptorSatisfiesThePoolManagerDiscoveryContract() throws ReflectiveOperationException {
        String scannedPackage = KVMStoragePoolManager.class.getPackage().getName();
        Set<Class<? extends StorageAdaptor>> discovered =
                new Reflections(scannedPackage).getSubTypesOf(StorageAdaptor.class);

        assertTrue("OntapIscsiStorageAdaptor must live in " + scannedPackage + " to be discovered",
                discovered.contains(OntapIscsiStorageAdaptor.class));
        assertFalse("An abstract adaptor is skipped by the scan",
                Modifier.isAbstract(OntapIscsiStorageAdaptor.class.getModifiers()));

        StorageAdaptor adaptor = OntapIscsiStorageAdaptor.class.getDeclaredConstructor().newInstance();
        assertEquals(StoragePoolType.OntapiSCSI, adaptor.getStoragePoolType());
        assertEquals("IscsiAdmStorageAdaptor must keep serving the other iSCSI vendors",
                StoragePoolType.Iscsi, new IscsiAdmStorageAdaptor().getStoragePoolType());
    }

    /**
     * The two adaptors are deliberately unrelated: device naming runs through nearly every method
     * that does real work, so inheriting IscsiAdmStorageAdaptor coupled them through behaviour
     * neither could change safely. Registration only requires implementing StorageAdaptor, and
     * KVMStoragePoolManager keys adaptors on getStoragePoolType(), so nothing depends on a shared
     * superclass. Asserting it keeps a later "reuse" refactor from quietly restoring by-path
     * naming through an un-overridden inherited method.
     */
    @Test
    public void adaptorDoesNotInheritTheByPathBasedIscsiAdaptor() {
        assertTrue("The adaptor must implement StorageAdaptor directly",
                StorageAdaptor.class.isAssignableFrom(OntapIscsiStorageAdaptor.class));
        assertFalse("The adaptor must not extend the by-path based iSCSI adaptor",
                IscsiAdmStorageAdaptor.class.isAssignableFrom(OntapIscsiStorageAdaptor.class));
        assertEquals("It should sit directly on StorageAdaptor, with no intermediate base class",
                Object.class, OntapIscsiStorageAdaptor.class.getSuperclass());
    }

    /**
     * The WWID occupies the component the superclass reads a logical unit number from, which is what
     * keeps the path at the two components every hypervisor resource's '/targetIQN/LUN' parsing
     * demands. ONTAP serial numbers contain '/' (byte 0x2f), so carrying the raw serial here instead
     * of its hex WWID would split into four components and throw.
     */
    @Test
    public void physicalDiskIsNamedByLunWwidRatherThanLogicalUnitNumber() {
        OntapIscsiStorageAdaptor adaptor = new OntapIscsiStorageAdaptor();
        KVMStoragePool pool = adaptor.createStoragePool("ontap-iscsi-pool-uuid", "10.196.37.157", 3260, null, null,
                StoragePoolType.OntapiSCSI, null, true);

        String targetIqn = "iqn.1992-08.com.netapp:sn.45048fd9b65111f1b106005056bd83cf:vs.3";
        String lunWwid = "600a098078304d2d383f2f6b45734a54";

        KVMPhysicalDisk disk = adaptor.getPhysicalDisk("/" + targetIqn + "/" + lunWwid, pool);

        assertEquals("/dev/disk/by-id/wwn-0x" + lunWwid, disk.getPath());
        assertFalse("The reported path must carry nothing host-specific", disk.getPath().contains("lun-"));
        assertFalse("The reported path must not be derived from by-path", disk.getPath().contains("by-path"));
        assertEquals(PhysicalDiskFormat.RAW, disk.getFormat());
    }

    @Test
    public void malformedVolumePathIsRejected() {
        OntapIscsiStorageAdaptor adaptor = new OntapIscsiStorageAdaptor();
        KVMStoragePool pool = adaptor.createStoragePool("ontap-iscsi-pool-uuid", "10.196.37.157", 3260, null, null,
                StoragePoolType.OntapiSCSI, null, true);

        // A raw ONTAP serial such as 'x0M-8?/kEsJT' would land here, splitting into four components.
        assertThrows(CloudRuntimeException.class,
                () -> adaptor.getPhysicalDisk("/iqn.1992-08.com.netapp:sn.abc:vs.3/x0M-8?/kEsJT", pool));
        assertThrows(CloudRuntimeException.class,
                () -> adaptor.getPhysicalDisk("/iqn.1992-08.com.netapp:sn.abc:vs.3", pool));
    }

    /**
     * KVMStoragePoolManager.disconnectPhysicalDiskByPath walks every registered adaptor and stops at
     * the first one returning true, so an adaptor that over-claims tears down another vendor's
     * session. By-path devices still belong to the superclass, which serves the other iSCSI vendors.
     */
    @Test
    public void disconnectByPathOnlyClaimsTheDevicesThisAdaptorHandsOut() {
        OntapIscsiStorageAdaptor adaptor = new OntapIscsiStorageAdaptor();

        assertFalse("A by-path device is the superclass's to disconnect", adaptor.disconnectPhysicalDiskByPath(
                "/dev/disk/by-path/ip-10.0.0.1:3260-iscsi-iqn.1992-08.com.netapp:sn.abc:vs.3-lun-0"));
        assertFalse("PowerFlex publishes its own by-id names",
                adaptor.disconnectPhysicalDiskByPath("/dev/disk/by-id/emc-vol-1235dc1s0-4a2f6b45"));
        assertFalse("A partition is not the LUN itself",
                adaptor.disconnectPhysicalDiskByPath("/dev/disk/by-id/wwn-0x600a098078304d2d383f2f6b45734a51-part1"));
        assertFalse("Only the wwn-0x form is handed out, never the scsi-3 alias",
                adaptor.disconnectPhysicalDiskByPath("/dev/disk/by-id/scsi-3600a098078304d2d383f2f6b45734a51"));
        assertFalse("A bare kernel name carries no identity", adaptor.disconnectPhysicalDiskByPath("/dev/sdb"));
        assertFalse(adaptor.disconnectPhysicalDiskByPath(null));
    }

    /**
     * Other vendors publish the same wwn-0x plus 32 hex shape. Only NetApp's NAA prefix is ours, so a
     * different array's LUN or a local RAID volume must fall through to the next adaptor.
     */
    @Test
    public void disconnectByPathDoesNotClaimOtherVendorsWwnDevices() {
        OntapIscsiStorageAdaptor adaptor = new OntapIscsiStorageAdaptor();

        assertFalse("Pure FlashArray also publishes wwn-0x names",
                adaptor.disconnectPhysicalDiskByPath("/dev/disk/by-id/wwn-0x624a9370f1f4f5a4b0d3c2e1000113aa"));
        assertFalse("HPE Primera also publishes wwn-0x names",
                adaptor.disconnectPhysicalDiskByPath("/dev/disk/by-id/wwn-0x60002ac0000000000000012300020a1b"));
        assertFalse("A local RAID volume also publishes wwn-0x names",
                adaptor.disconnectPhysicalDiskByPath("/dev/disk/by-id/wwn-0x600508b1001c4d2e8a0f6b1c2d3e4f50"));
        assertFalse("NetApp E-Series is not managed by this adaptor",
                adaptor.disconnectPhysicalDiskByPath("/dev/disk/by-id/wwn-0x600a0b800029e3a4000012345f6a7b8c"));
    }

    /**
     * A claimed device that has already gone reports success: there is no session left to tear down,
     * and returning false would send the manager on to adaptors that would mishandle the path.
     */
    @Test
    public void disconnectByPathSucceedsWhenTheClaimedDeviceIsAlreadyGone() {
        OntapIscsiStorageAdaptor adaptor = new OntapIscsiStorageAdaptor();

        assertTrue(adaptor.disconnectPhysicalDiskByPath("/dev/disk/by-id/wwn-0x600a098078304d2d383f2f6b45734a51"));
    }
}
