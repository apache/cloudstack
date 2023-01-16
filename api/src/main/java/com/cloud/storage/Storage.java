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
        HARDWARE_ACCELERATION("HARDWARE_ACCELERATION");

        private final String capability;

        private Capability(String capability) {
            this.capability = capability;
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
        BUILTIN, /* buildin template */
        PERHOST, /* every host has this template, don't need to install it in secondary storage */
        USER, /* User supplied template/iso */
        DATADISK, /* Template corresponding to a datadisk(non root disk) present in an OVA */
        ISODISK /* Template corresponding to a iso (non root disk) present in an OVA */
    }

    public static enum StoragePoolType {
        Filesystem(false, true, true), // local directory
        NetworkFilesystem(true, true, true), // NFS
        IscsiLUN(true, false, false), // shared LUN, with a clusterfs overlay
        Iscsi(true, false, false), // for e.g., ZFS Comstar
        ISO(false, false, false), // for iso image
        LVM(false, false, false), // XenServer local LVM SR
        CLVM(true, false, false),
        RBD(true, true, false), // http://libvirt.org/storage.html#StorageBackendRBD
        SharedMountPoint(true, false, true),
        VMFS(true, true, false), // VMware VMFS storage
        PreSetup(true, true, false), // for XenServer, Storage Pool is set up by customers.
        EXT(false, true, false), // XenServer local EXT SR
        OCFS2(true, false, false),
        SMB(true, false, false),
        Gluster(true, false, false),
        PowerFlex(true, true, true), // Dell EMC PowerFlex/ScaleIO (formerly VxFlexOS)
        ManagedNFS(true, false, false),
        Linstor(true, true, false),
        DatastoreCluster(true, true, false), // for VMware, to abstract pool of clusters
        StorPool(true, true, false);

        private final boolean shared;
        private final boolean overprovisioning;
        private final boolean encryption;

        StoragePoolType(boolean shared, boolean overprovisioning, boolean encryption) {
            this.shared = shared;
            this.overprovisioning = overprovisioning;
            this.encryption = encryption;
        }

        public boolean isShared() {
            return shared;
        }

        public boolean supportsOverProvisioning() {
            return overprovisioning;
        }

        public boolean supportsEncryption() { return encryption; }
    }

    public static List<StoragePoolType> getNonSharedStoragePoolTypes() {
        List<StoragePoolType> nonSharedStoragePoolTypes = new ArrayList<StoragePoolType>();
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
