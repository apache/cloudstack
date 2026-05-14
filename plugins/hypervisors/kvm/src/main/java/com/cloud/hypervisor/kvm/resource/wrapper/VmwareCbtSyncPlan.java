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
package com.cloud.hypervisor.kvm.resource.wrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.api.to.VmwareCbtChangedBlockRangeTO;
import com.cloud.agent.api.to.VmwareCbtDiskTO;

class VmwareCbtSyncPlan {

    private final boolean valid;
    private final String validationError;
    private final List<DiskPlan> diskPlans;
    private final int changedRangeCount;
    private final long changedBytes;

    private VmwareCbtSyncPlan(boolean valid, String validationError, List<DiskPlan> diskPlans,
                              int changedRangeCount, long changedBytes) {
        this.valid = valid;
        this.validationError = validationError;
        this.diskPlans = diskPlans;
        this.changedRangeCount = changedRangeCount;
        this.changedBytes = changedBytes;
    }

    static VmwareCbtSyncPlan create(List<VmwareCbtDiskTO> disks, List<VmwareCbtChangedBlockRangeTO> changedBlocks) {
        if (CollectionUtils.isEmpty(changedBlocks)) {
            return new VmwareCbtSyncPlan(true, null, Collections.emptyList(), 0, 0);
        }

        Map<String, DiskPlanBuilder> diskPlansById = getDiskPlansById(disks);
        long changedBytes = 0;
        int changedRangeCount = 0;

        for (VmwareCbtChangedBlockRangeTO changedBlock : changedBlocks) {
            ValidationResult validationResult = validateChangedBlock(changedBlock, diskPlansById);
            if (!validationResult.valid) {
                return invalid(validationResult.error);
            }

            DiskPlanBuilder diskPlan = diskPlansById.get(changedBlock.getDiskId());
            long rangeEnd = changedBlock.getStartOffset() + changedBlock.getLength();
            if (rangeEnd < changedBlock.getStartOffset()) {
                return invalid(String.format("Changed block range for disk %s overflows the virtual disk address space.",
                        changedBlock.getDiskId()));
            }
            if (diskPlan.disk.getCapacityBytes() > 0 && rangeEnd > diskPlan.disk.getCapacityBytes()) {
                return invalid(String.format("Changed block range for disk %s exceeds disk capacity.", changedBlock.getDiskId()));
            }

            diskPlan.addChangedBlock(changedBlock);
            changedBytes += changedBlock.getLength();
            changedRangeCount++;
        }

        return new VmwareCbtSyncPlan(true, null, getPopulatedDiskPlans(diskPlansById), changedRangeCount, changedBytes);
    }

    private static Map<String, DiskPlanBuilder> getDiskPlansById(List<VmwareCbtDiskTO> disks) {
        Map<String, DiskPlanBuilder> diskPlansById = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(disks)) {
            return diskPlansById;
        }

        for (VmwareCbtDiskTO disk : disks) {
            if (disk != null && StringUtils.isNotBlank(disk.getDiskId())) {
                diskPlansById.put(disk.getDiskId(), new DiskPlanBuilder(disk));
            }
        }
        return diskPlansById;
    }

    private static ValidationResult validateChangedBlock(VmwareCbtChangedBlockRangeTO changedBlock,
                                                         Map<String, DiskPlanBuilder> diskPlansById) {
        if (changedBlock == null) {
            return ValidationResult.invalid("Changed block range cannot be null.");
        }
        if (StringUtils.isBlank(changedBlock.getDiskId())) {
            return ValidationResult.invalid("Changed block range is missing a disk id.");
        }

        DiskPlanBuilder diskPlan = diskPlansById.get(changedBlock.getDiskId());
        if (diskPlan == null) {
            return ValidationResult.invalid(String.format("Changed block range references unknown disk %s.", changedBlock.getDiskId()));
        }
        if (StringUtils.isBlank(diskPlan.disk.getTargetPath())) {
            return ValidationResult.invalid(String.format("Changed block range references disk %s, but no target path is known. " +
                    "The initial full sync must create and persist the KVM target disk before CBT delta sync can run.",
                    changedBlock.getDiskId()));
        }
        if (changedBlock.getStartOffset() < 0) {
            return ValidationResult.invalid(String.format("Changed block range for disk %s has a negative start offset.",
                    changedBlock.getDiskId()));
        }
        if (changedBlock.getLength() <= 0) {
            return ValidationResult.invalid(String.format("Changed block range for disk %s has a non-positive length.",
                    changedBlock.getDiskId()));
        }

        return ValidationResult.valid();
    }

    private static List<DiskPlan> getPopulatedDiskPlans(Map<String, DiskPlanBuilder> diskPlansById) {
        List<DiskPlan> diskPlans = new ArrayList<>();
        for (DiskPlanBuilder diskPlan : diskPlansById.values()) {
            if (CollectionUtils.isNotEmpty(diskPlan.changedBlocks)) {
                diskPlans.add(diskPlan.build());
            }
        }
        return diskPlans;
    }

    private static VmwareCbtSyncPlan invalid(String validationError) {
        return new VmwareCbtSyncPlan(false, validationError, Collections.emptyList(), 0, 0);
    }

    boolean isValid() {
        return valid;
    }

    String getValidationError() {
        return validationError;
    }

    List<DiskPlan> getDiskPlans() {
        return diskPlans;
    }

    int getChangedRangeCount() {
        return changedRangeCount;
    }

    long getChangedBytes() {
        return changedBytes;
    }

    static class DiskPlan {

        private final VmwareCbtDiskTO disk;
        private final List<VmwareCbtChangedBlockRangeTO> changedBlocks;
        private final long changedBytes;

        DiskPlan(VmwareCbtDiskTO disk, List<VmwareCbtChangedBlockRangeTO> changedBlocks, long changedBytes) {
            this.disk = disk;
            this.changedBlocks = Collections.unmodifiableList(changedBlocks);
            this.changedBytes = changedBytes;
        }

        VmwareCbtDiskTO getDisk() {
            return disk;
        }

        List<VmwareCbtChangedBlockRangeTO> getChangedBlocks() {
            return changedBlocks;
        }

        long getChangedBytes() {
            return changedBytes;
        }
    }

    private static class DiskPlanBuilder {

        private final VmwareCbtDiskTO disk;
        private final List<VmwareCbtChangedBlockRangeTO> changedBlocks = new ArrayList<>();
        private long changedBytes;

        DiskPlanBuilder(VmwareCbtDiskTO disk) {
            this.disk = disk;
        }

        void addChangedBlock(VmwareCbtChangedBlockRangeTO changedBlock) {
            changedBlocks.add(changedBlock);
            changedBytes += changedBlock.getLength();
        }

        DiskPlan build() {
            return new DiskPlan(disk, new ArrayList<>(changedBlocks), changedBytes);
        }
    }

    private static class ValidationResult {

        private final boolean valid;
        private final String error;

        private ValidationResult(boolean valid, String error) {
            this.valid = valid;
            this.error = error;
        }

        static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        static ValidationResult invalid(String error) {
            return new ValidationResult(false, error);
        }
    }
}
