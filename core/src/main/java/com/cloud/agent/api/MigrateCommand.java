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

package com.cloud.agent.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.agent.api.to.VirtualMachineTO;

public class MigrateCommand extends Command {
    private String vmName;
    private String destIp;
    private Map<String, MigrateDiskInfo> migrateStorage;
    private boolean migrateStorageManaged;
    private boolean autoConvergence;
    private String hostGuid;
    private boolean isWindows;
    private VirtualMachineTO vmTO;
    private boolean executeInSequence = false;
    private List<MigrateDiskInfo> migrateDiskInfoList = new ArrayList<>();

    protected MigrateCommand() {
    }

    public MigrateCommand(String vmName, String destIp, boolean isWindows, VirtualMachineTO vmTO, boolean executeInSequence) {
        this.vmName = vmName;
        this.destIp = destIp;
        this.isWindows = isWindows;
        this.vmTO = vmTO;
        this.executeInSequence = executeInSequence;
    }

    public void setMigrateStorage(Map<String, MigrateDiskInfo> migrateStorage) {
        this.migrateStorage = migrateStorage;
    }

    public Map<String, MigrateDiskInfo> getMigrateStorage() {
        return migrateStorage != null ? new HashMap<>(migrateStorage) : new HashMap<String, MigrateDiskInfo>();
    }

    public boolean isMigrateStorageManaged() {
        return migrateStorageManaged;
    }

    public void setMigrateStorageManaged(boolean migrateStorageManaged) {
        this.migrateStorageManaged = migrateStorageManaged;
    }

    public void setAutoConvergence(boolean autoConvergence) {
        this.autoConvergence = autoConvergence;
    }

    public boolean isAutoConvergence() {
        return autoConvergence;
    }

    public boolean isWindows() {
        return isWindows;
    }

    public VirtualMachineTO getVirtualMachine() {
        return vmTO;
    }

    public String getDestinationIp() {
        return destIp;
    }

    public String getVmName() {
        return vmName;
    }

    public void setHostGuid(String guid) {
        this.hostGuid = guid;
    }

    public String getHostGuid() {
        return this.hostGuid;
    }

    @Override
    public boolean executeInSequence() {
        return executeInSequence;
    }

    public List<MigrateDiskInfo> getMigrateDiskInfoList() {
        return migrateDiskInfoList;
    }

    public void setMigrateDiskInfoList(List<MigrateDiskInfo> migrateDiskInfoList) {
        this.migrateDiskInfoList = migrateDiskInfoList;
    }

    public static class MigrateDiskInfo {
        public enum DiskType {
            FILE, BLOCK;

            @Override
            public String toString() {
                return name().toLowerCase();
            }
        }

        public enum DriverType {
            QCOW2, RAW;

            @Override
            public String toString() {
                return name().toLowerCase();
            }
        }

        public enum Source {
            FILE, DEV;

            @Override
            public String toString() {
                return name().toLowerCase();
            }
        }

        private final String serialNumber;
        private final DiskType diskType;
        private final DriverType driverType;
        private final Source source;
        private final String sourceText;
        private boolean isSourceDiskOnStorageFileSystem;

        public MigrateDiskInfo(final String serialNumber, final DiskType diskType, final DriverType driverType, final Source source, final String sourceText) {
            this.serialNumber = serialNumber;
            this.diskType = diskType;
            this.driverType = driverType;
            this.source = source;
            this.sourceText = sourceText;
        }

        public String getSerialNumber() {
            return serialNumber;
        }

        public DiskType getDiskType() {
            return diskType;
        }

        public DriverType getDriverType() {
            return driverType;
        }

        public Source getSource() {
            return source;
        }

        public String getSourceText() {
            return sourceText;
        }

        public boolean isSourceDiskOnStorageFileSystem() {
            return isSourceDiskOnStorageFileSystem;
        }

        public void setSourceDiskOnStorageFileSystem(boolean isDiskOnFileSystemStorage) {
            this.isSourceDiskOnStorageFileSystem = isDiskOnFileSystemStorage;
        }
    }
}
