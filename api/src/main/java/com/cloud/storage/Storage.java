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
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        BUILTIN, /* buildin template */
        PERHOST, /* every host has this template, don't need to install it in secondary storage */
        USER, /* User supplied template/iso */
        VNF,    /* VNFs (virtual network functions) template */
        DATADISK, /* Template corresponding to a datadisk(non root disk) present in an OVA */
        ISODISK /* Template corresponding to a iso (non root disk) present in an OVA */
    }

    public static class StoragePoolType {
        private static final Map<String, StoragePoolType> map = new LinkedHashMap<>();

        public static final StoragePoolType Filesystem = new StoragePoolType("Filesystem", false, true, true);
        public static final StoragePoolType NetworkFilesystem = new StoragePoolType("NetworkFilesystem", true, true, true);
        public static final StoragePoolType IscsiLUN = new StoragePoolType("IscsiLUN", true, false, false);
        public static final StoragePoolType Iscsi = new StoragePoolType("Iscsi", true, false, false);
        public static final StoragePoolType ISO = new StoragePoolType("ISO", false, false, false);
        public static final StoragePoolType LVM = new StoragePoolType("LVM", false, false, false);
        public static final StoragePoolType CLVM = new StoragePoolType("CLVM", true, false, false);
        public static final StoragePoolType RBD = new StoragePoolType("RBD", true, true, false);
        public static final StoragePoolType SharedMountPoint = new StoragePoolType("SharedMountPoint", true, false, true);
        public static final StoragePoolType VMFS = new StoragePoolType("VMFS", true, true, false);
        public static final StoragePoolType PreSetup = new StoragePoolType("PreSetup", true, true, false);
        public static final StoragePoolType EXT = new StoragePoolType("EXT", false, true, false);
        public static final StoragePoolType OCFS2 = new StoragePoolType("OCFS2", true, false, false);
        public static final StoragePoolType SMB = new StoragePoolType("SMB", true, false, false);
        public static final StoragePoolType Gluster = new StoragePoolType("Gluster", true, false, false);
        public static final StoragePoolType PowerFlex = new StoragePoolType("PowerFlex", true, true, true);
        public static final StoragePoolType ManagedNFS = new StoragePoolType("ManagedNFS", true, false, false);
        public static final StoragePoolType Linstor = new StoragePoolType("Linstor", true, true, false);
        public static final StoragePoolType DatastoreCluster = new StoragePoolType("DatastoreCluster", true, true, false);
        public static final StoragePoolType StorPool = new StoragePoolType("StorPool", true,true,true);
        public static final StoragePoolType FiberChannel = new StoragePoolType("FiberChannel", true,true,false);


        private final String name;
        private final boolean shared;
        private final boolean overprovisioning;
        private final boolean encryption;

        public StoragePoolType(String name, boolean shared, boolean overprovisioning, boolean encryption) {
            this.name = name;
            this.shared = shared;
            this.overprovisioning = overprovisioning;
            this.encryption = encryption;
            addStoragePoolType(this);
        }

        public boolean isShared() {
            return shared;
        }

        public boolean supportsOverProvisioning() {
            return overprovisioning;
        }

        public boolean supportsEncryption() { return encryption; }

        private static void addStoragePoolType(StoragePoolType storagePoolType) {
            map.putIfAbsent(storagePoolType.name, storagePoolType);
        }

        public static StoragePoolType[] values() {
            return map.values().toArray(StoragePoolType[]::new).clone();
        }

        public static StoragePoolType valueOf(String name) {
            if (StringUtils.isBlank(name)) {
                return null;
            }

            StoragePoolType storage = map.get(name);
            if (storage == null) {
                throw new IllegalArgumentException("StoragePoolType '" + name + "' not found");
            }
            return storage;
        }

        @Override
        public String toString() {
            return name;
        }

        public String name() {
            return name;
        }
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
