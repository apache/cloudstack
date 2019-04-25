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

    public static enum SOUniqueName {
      CONSOLEPROXY("Cloud.com-ConsoleProxy"),
      CONSOLEPROXY_LOCAL("Cloud.com-ConsoleProxy-Local"),
      SECONDARYSTORAGE("Cloud.com-SecondaryStorage"),
      SECONDARYSTORAGE_LOCAL("Cloud.com-SecondaryStorage-Local"),
      INTERNALLBVM("Cloud.Com-InternalLBVm"),
      INTERNALLBVM_LOCAL("Cloud.Com-InternalLBVm-Local"),
      ELASTICLBVM("Cloud.Com-ElasticLBVm"),
      ELASTICLBVM_LOCAL("Cloud.Com-ElasticLBVm-Local"),
      SOFTWAREROUTER("Cloud.Com-SoftwareRouter"),
      SOFTWAREROUTER_LOCAL("Cloud.Com-SoftwareRouter-Local");

      private final String uniqueName;

      private SOUniqueName(String uniqueName){
        this.uniqueName = uniqueName;
      }

      public String toString(){
        return this.uniqueName;
      }

      public static SOUniqueName getDefaultUseUniqueName(String systemVMType, boolean isLocal){
        switch(systemVMType.toLowerCase()){
          case "domainrouter":
            if(isLocal){
              return SOUniqueName.SOFTWAREROUTER_LOCAL;
            } else {
              return SOUniqueName.SOFTWAREROUTER;
            }
          case "consoleproxy":
            if(isLocal){
              return SOUniqueName.CONSOLEPROXY_LOCAL;
            } else {
              return SOUniqueName.CONSOLEPROXY;
            }
          case "secondarystoragevm":
            if(isLocal){
              return SOUniqueName.SECONDARYSTORAGE_LOCAL;
            } else {
              return SOUniqueName.SECONDARYSTORAGE;
            }
          case "internalloadbalancervm":
            if(isLocal){
              return SOUniqueName.INTERNALLBVM_LOCAL;
            } else {
              return SOUniqueName.INTERNALLBVM;
            }
          case "elasticloadbalancervm":
            if(isLocal){
              return SOUniqueName.ELASTICLBVM_LOCAL;
            } else {
              return SOUniqueName.ELASTICLBVM;
            }
          default:
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
        Filesystem(false, true), // local directory
        NetworkFilesystem(true, true), // NFS
        IscsiLUN(true, false), // shared LUN, with a clusterfs overlay
        Iscsi(true, false), // for e.g., ZFS Comstar
        ISO(false, false), // for iso image
        LVM(false, false), // XenServer local LVM SR
        CLVM(true, false),
        RBD(true, true), // http://libvirt.org/storage.html#StorageBackendRBD
        SharedMountPoint(true, false),
        VMFS(true, true), // VMware VMFS storage
        PreSetup(true, true), // for XenServer, Storage Pool is set up by customers.
        EXT(false, true), // XenServer local EXT SR
        OCFS2(true, false),
        SMB(true, false),
        Gluster(true, false),
        ManagedNFS(true, false);

        private final boolean shared;
        private final boolean overprovisioning;

        StoragePoolType(boolean shared, boolean overprovisioning) {
            this.shared = shared;
            this.overprovisioning = overprovisioning;
        }

        public boolean isShared() {
            return shared;
        }

        public boolean supportsOverProvisioning() {
            return overprovisioning;
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
