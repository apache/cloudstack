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
package com.cloud.storage;

import org.apache.commons.lang.NotImplementedException;

import java.util.ArrayList;
import java.util.List;

public class Storage {
    public static enum ImageFormat {
        QCOW2(true, true, false, "qcow2"),
        RAW(false, false, false, "raw"),
        VHD(true, true, true, "vhd"),
        ISO(false, false, false, "iso"),
        OVA(true, true, true, "ova"),
        VHDX(true, true, true, "vhdx"),
        BAREMETAL(false, false, false, "BAREMETAL"),
        EXTERNAL(false, false, false, "EXTERNAL"),
        VMDK(true, true, false, "vmdk"),
        VDI(true, true, false, "vdi"),
        TAR(false, false, false, "tar"),
        ZIP(false, false, false, "zip"),
        DIR(false, false, false, "dir");

        private final boolean supportThinProvisioning;
        private final boolean supportSparse;
        private final boolean supportSnapshot;
        private final String fileExtension;

        private ImageFormat(boolean supportThinProvisioning, boolean supportSparse, boolean supportSnapshot) {
            this.supportThinProvisioning = supportThinProvisioning;
            this.supportSparse = supportSparse;
            this.supportSnapshot = supportSnapshot;
            fileExtension = null;
        }

        private ImageFormat(boolean supportThinProvisioning, boolean supportSparse, boolean supportSnapshot, String fileExtension) {
            this.supportThinProvisioning = supportThinProvisioning;
            this.supportSparse = supportSparse;
            this.supportSnapshot = supportSnapshot;
            this.fileExtension = fileExtension;
        }

        public boolean supportThinProvisioning() {
            return supportThinProvisioning;
        }

        public boolean supportsSparse() {
            return supportSparse;
        }

        public boolean supportSnapshot() {
            return supportSnapshot;
        }

        public String getFileExtension() {
            if (fileExtension == null)
                return toString().toLowerCase();

            return fileExtension;
        }

    }

    public static enum Capability {
        HARDWARE_ACCELERATION("HARDWARE_ACCELERATION"),
        ALLOW_MIGRATE_OTHER_POOLS("ALLOW_MIGRATE_OTHER_POOLS");

        private final String capability;

        private Capability(String capability) {
            this.capability = capability;
        }

        public String toString() {
            return this.capability;
        }
    }

    public static enum ProvisioningType {
        THIN("thin"),
        SPARSE("sparse"),
        FAT("fat");

        private final String provisionType;

        private ProvisioningType(String provisionType){
            this.provisionType = provisionType;
        }

        public String toString(){
            return this.provisionType;
        }

        public static ProvisioningType getProvisioningType(String provisioningType){

            if(provisioningType.equals(THIN.provisionType)){
                return ProvisioningType.THIN;
            } else if(provisioningType.equals(SPARSE.provisionType)){
                return ProvisioningType.SPARSE;
            } else if (provisioningType.equals(FAT.provisionType)){
                return ProvisioningType.FAT;
            } else{
                    throw new NotImplementedException();
            }
        }
    }

    public static enum FileSystem {
        Unknown, ext3, ntfs, fat, fat32, ext2, ext4, cdfs, hpfs, ufs, hfs, hfsp
    }

    public static enum TemplateType {
        ROUTING, // Router template
        SYSTEM, /* routing, system vm template */
        BUILTIN, /* builtin template */
        PERHOST, /* every host has this template, don't need to install it in secondary storage */
        USER, /* User supplied template/iso */
        VNF,    /* VNFs (virtual network functions) template */
        DATADISK, /* Template corresponding to a datadisk(non root disk) present in an OVA */
        ISODISK /* Template corresponding to a iso (non root disk) present in an OVA */
    }

    public enum EncryptionSupport {
        /**
         * Encryption not supported.
         */
        Unsupported,
        /**
         * Will use hypervisor encryption driver (qemu -> luks)
         */
        Hypervisor,
        /**
         * Storage pool handles encryption and just provides an encrypted volume
         */
        Storage
    }

    /**
     * StoragePoolTypes carry some details about the format and capabilities of a storage pool. While not necessarily a
     * 1:1 with PrimaryDataStoreDriver (and for KVM agent, KVMStoragePool and StorageAdaptor) implementations, it is
     * often used to decide which storage plugin or storage command to call, so it may be necessary for new storage
     * plugins to add a StoragePoolType.  This can be done by adding it below, or by creating a new public static final
     * instance of StoragePoolType in the plugin itself, which registers it with the map.
     *
     * Note that if the StoragePoolType is for KVM and defined in plugin code rather than below, care must be taken to
     * ensure this is available on the agent side as well. This is best done by defining the StoragePoolType in a common
     * package available on both management server and agent plugin jars.
     */
    public static enum StoragePoolType {
        Filesystem(false, true, EncryptionSupport.Hypervisor), // local directory
        NetworkFilesystem(true, true, EncryptionSupport.Hypervisor), // NFS
        IscsiLUN(true, false, EncryptionSupport.Unsupported), // shared LUN, with a clusterfs overlay
        Iscsi(true, false, EncryptionSupport.Unsupported), // for e.g., ZFS Comstar
        ISO(false, false, EncryptionSupport.Unsupported), // for iso image
        LVM(false, false, EncryptionSupport.Unsupported), // XenServer local LVM SR
        CLVM(true, false, EncryptionSupport.Unsupported),
        RBD(true, true, EncryptionSupport.Unsupported), // http://libvirt.org/storage.html#StorageBackendRBD
        SharedMountPoint(true, true, EncryptionSupport.Hypervisor),
        VMFS(true, true, EncryptionSupport.Unsupported), // VMware VMFS storage
        PreSetup(true, true, EncryptionSupport.Unsupported), // for XenServer, Storage Pool is set up by customers.
        EXT(false, true, EncryptionSupport.Unsupported), // XenServer local EXT SR
        OCFS2(true, false, EncryptionSupport.Unsupported),
        SMB(true, false, EncryptionSupport.Unsupported),
        Gluster(true, false, EncryptionSupport.Unsupported),
        PowerFlex(true, true, EncryptionSupport.Hypervisor), // Dell EMC PowerFlex/ScaleIO (formerly VxFlexOS)
        ManagedNFS(true, false, EncryptionSupport.Unsupported),
        Linstor(true, true, EncryptionSupport.Storage),
        DatastoreCluster(true, true, EncryptionSupport.Unsupported), // for VMware, to abstract pool of clusters
        StorPool(true, true, EncryptionSupport.Hypervisor),
        FiberChannel(true, true, EncryptionSupport.Unsupported); // Fiber Channel Pool for KVM hypervisors is used to find the volume by WWN value (/dev/disk/by-id/wwn-<wwnvalue>)

        private final boolean shared;
        private final boolean overProvisioning;
        private final EncryptionSupport encryption;

        StoragePoolType(boolean shared, boolean overProvisioning, EncryptionSupport encryption) {
            this.shared = shared;
            this.overProvisioning = overProvisioning;
            this.encryption = encryption;
        }

        public boolean isShared() {
            return shared;
        }

        public boolean supportsOverProvisioning() {
            return overProvisioning;
        }

        public boolean supportsEncryption() {
            return encryption == EncryptionSupport.Hypervisor || encryption == EncryptionSupport.Storage;
        }

        public EncryptionSupport encryptionSupportMode() {
            return encryption;
        }
    }

    public static List<StoragePoolType> getNonSharedStoragePoolTypes() {
        List<StoragePoolType> nonSharedStoragePoolTypes = new ArrayList<>();
        for (StoragePoolType storagePoolType : StoragePoolType.values()) {
            if (!storagePoolType.isShared()) {
                nonSharedStoragePoolTypes.add(storagePoolType);
            }
        }
        return nonSharedStoragePoolTypes;

    }

    public static enum StorageResourceType {
        STORAGE_POOL, STORAGE_HOST, SECONDARY_STORAGE, LOCAL_SECONDARY_STORAGE
    }
}
