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

package org.apache.cloudstack.veeam.api.converter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.api.ApiService;
import org.apache.cloudstack.veeam.api.dto.Actions;
import org.apache.cloudstack.veeam.api.dto.Disk;
import org.apache.cloudstack.veeam.api.dto.DiskAttachment;
import org.apache.cloudstack.veeam.api.dto.Link;
import org.apache.cloudstack.veeam.api.dto.Ref;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.vo.VolumeJoinVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeStats;

public class VolumeJoinVOToDiskConverter {
    public static Disk toDisk(final VolumeJoinVO vol) {
        final Disk disk = new Disk();
        final String apiBase = VeeamControlService.ContextPath.value() + ApiService.BASE_ROUTE;

        final String diskId = vol.getUuid();
        final String diskHref = apiBase + "/disks/" + diskId;

        disk.id = diskId;
        disk.href = diskHref;

        // Names
        disk.name = vol.getName();
        disk.alias = vol.getName();
        disk.description = "";

        // Sizes (bytes)
        final long size = vol.getSize();
        final long actualSize = vol.getVolumeStoreSize();

        disk.provisionedSize = String.valueOf(size);
        disk.actualSize = String.valueOf(actualSize);
        disk.totalSize = String.valueOf(size);
        VolumeStats vs = null;
        if (List.of(Storage.ImageFormat.VHD, Storage.ImageFormat.QCOW2, Storage.ImageFormat.RAW).contains(vol.getFormat())) {
            if (vol.getPath() != null) {
                vs = ApiDBUtils.getVolumeStatistics(vol.getPath());
            }
        } else if (vol.getFormat() == Storage.ImageFormat.OVA) {
            if (vol.getChainInfo() != null) {
                vs = ApiDBUtils.getVolumeStatistics(vol.getChainInfo());
            }
        }
        if (vs != null) {
            disk.totalSize = String.valueOf(vs.getVirtualSize());
            disk.actualSize = String.valueOf(vs.getPhysicalSize());
        }

        // Disk format
        disk.format = mapFormat(vol.getFormat());
        disk.qcowVersion = "qcow2_v3";

        // Content & storage
        disk.contentType = "data";
        disk.storageType = "image";
        disk.sparse = "true";
        disk.shareable = "false";

        // Status
        disk.status = mapStatus(vol.getState());

        // Backup-related flags (safe defaults)
        disk.backup = "none";
        disk.propagateErrors = "false";
        disk.wipeAfterDelete = "false";

        // Image ID (best-effort)
        disk.imageId = vol.getPath(); // acceptable placeholder

        // Disk profile (optional)
        disk.diskProfile = Ref.of(
                apiBase + "/diskprofiles/" + vol.getDiskOfferingId(),
                String.valueOf(vol.getDiskOfferingId())
        );

        // Storage domains
        if (vol.getPoolUuid() != null) {
            Disk.StorageDomains sds = new Disk.StorageDomains();
            sds.storageDomain = List.of(
                    Ref.of(
                            apiBase + "/storagedomains/" + vol.getPoolUuid(),
                            vol.getPoolUuid()
                    )
            );
            disk.storageDomains = sds;
        }

        // Actions (Veeam checks presence, not behavior)
        disk.actions = defaultDiskActions(diskHref);

        // Links
        disk.link = List.of(
                new Link("disksnapshots", diskHref + "/disksnapshots")
        );

        return disk;
    }

    public static List<Disk> toDiskList(final List<VolumeJoinVO> srcList) {
        return srcList.stream()
                .map(VolumeJoinVOToDiskConverter::toDisk)
                .collect(Collectors.toList());
    }

    public static  DiskAttachment toDiskAttachment(final VolumeJoinVO vol) {
        final DiskAttachment da = new DiskAttachment();
        final String apiBase = VeeamControlService.ContextPath.value() + ApiService.BASE_ROUTE;

        final String diskAttachmentId = vol.getUuid();
        final String diskAttachmentHref = apiBase + "/diskattachments/" + diskAttachmentId;

        da.id = diskAttachmentId;
        da.href = diskAttachmentHref;

        // Links
        da.disk = Ref.of(
                apiBase + "/disks/" + vol.getUuid(),
                vol.getUuid()
        );
        da.vm = Ref.of(
                apiBase + "/vms/" + vol.getVmUuid(),
                vol.getVmUuid()
        );

        // Properties
        da.active = "true";
        da.bootable = "false";
        da.iface = "virtio_scsi";
        da.logicalName = vol.getName();
        da.readOnly = "false";
        da.passDiscard = "false";

        return da;
    }

    public static List<DiskAttachment> toDiskAttachmentList(final List<VolumeJoinVO> srcList) {
        return srcList.stream()
                .map(VolumeJoinVOToDiskConverter::toDiskAttachment)
                .collect(Collectors.toList());
    }

    private static String mapFormat(final Storage.ImageFormat format) {
        if (format == null) {
            return "cow";
        }
        switch (format) {
            case RAW:
                return "raw";
            case QCOW2:
            default:
                return "cow";
        }
    }

    private static String mapStatus(final Volume.State state) {
        if (state == null) {
            return "ok";
        }
        switch (state.name().toLowerCase()) {
            case "ready":
            case "allocated":
                return "ok";
            default:
                return "locked";
        }
    }

    private static Actions defaultDiskActions(final String diskHref) {
        return new Actions(Collections.emptyList());
    }
}
