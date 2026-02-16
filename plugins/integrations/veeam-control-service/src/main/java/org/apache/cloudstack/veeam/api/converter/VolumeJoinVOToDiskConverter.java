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
import org.apache.cloudstack.veeam.api.DisksRouteHandler;
import org.apache.cloudstack.veeam.api.VmsRouteHandler;
import org.apache.cloudstack.veeam.api.dto.Actions;
import org.apache.cloudstack.veeam.api.dto.Disk;
import org.apache.cloudstack.veeam.api.dto.DiskAttachment;
import org.apache.cloudstack.veeam.api.dto.Link;
import org.apache.cloudstack.veeam.api.dto.Ref;
import org.apache.cloudstack.veeam.api.dto.StorageDomain;
import org.apache.cloudstack.veeam.api.dto.StorageDomains;
import org.apache.cloudstack.veeam.api.dto.Vm;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.vo.VolumeJoinVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeStats;

public class VolumeJoinVOToDiskConverter {
    public static Disk toDisk(final VolumeJoinVO vol) {
        final Disk disk = new Disk();
        final String basePath = VeeamControlService.ContextPath.value();
        final String apiBasePath = basePath + ApiService.BASE_ROUTE;
        final String diskId = vol.getUuid();
        final String diskHref = basePath + DisksRouteHandler.BASE_ROUTE + "/" + diskId;

        disk.setId(diskId);
        disk.setHref(diskHref);
        disk.setBootable(String.valueOf(Volume.Type.ROOT.equals(vol.getVolumeType())));

        // Names
        disk.setName(vol.getName());
        disk.setAlias(vol.getName());
        disk.setDescription(vol.getName());

        // Sizes (bytes)
        final long size = vol.getSize();
        final long actualSize = vol.getVolumeStoreSize();

        disk.setProvisionedSize(String.valueOf(size));
        disk.setActualSize(String.valueOf(actualSize));
        disk.setTotalSize(String.valueOf(size));
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
            disk.setTotalSize(String.valueOf(vs.getVirtualSize()));
            disk.setActualSize(String.valueOf(vs.getPhysicalSize()));
        }

        // Disk format
        disk.setFormat(mapFormat(vol.getFormat()));
        disk.setQcowVersion("qcow2_v3");

        // Content & storage
        disk.setContentType("data");
        disk.setStorageType("image");
        disk.setSparse("true");
        disk.setShareable("false");

        // Status
        disk.setStatus(mapStatus(vol.getState()));

        // Backup-related flags (safe defaults)
        disk.setBackup("none");
        disk.setPropagateErrors("false");
        disk.setWipeAfterDelete("false");

        // Image ID (best-effort)
        disk.setImageId(vol.getPath()); // acceptable placeholder

        // Disk profile (optional)
        disk.setDiskProfile(Ref.of(
                apiBasePath + "/diskprofiles/" + vol.getDiskOfferingUuid(),
                String.valueOf(vol.getDiskOfferingUuid())
        ));

        // Storage domains
        if (vol.getPoolUuid() != null) {
            StorageDomains sds = new StorageDomains();
            StorageDomain sd = new StorageDomain();
            sd.setHref(apiBasePath + "/storagedomains/" + vol.getPoolUuid());
            sd.setId(vol.getPoolUuid());
            sds.setStorageDomain(List.of(sd));
            disk.setStorageDomains(sds);
        }

        // Actions (Veeam checks presence, not behavior)
        disk.setActions(defaultDiskActions(diskHref));

        // Links
        disk.setLink(List.of(
                Link.of("disksnapshots", diskHref + "/disksnapshots")
        ));

        return disk;
    }

    public static List<Disk> toDiskList(final List<VolumeJoinVO> srcList) {
        return srcList.stream()
                .map(VolumeJoinVOToDiskConverter::toDisk)
                .collect(Collectors.toList());
    }

    public static  DiskAttachment toDiskAttachment(final VolumeJoinVO vol) {
        final DiskAttachment da = new DiskAttachment();
        final String basePath = VeeamControlService.ContextPath.value();

        final String diskAttachmentId = vol.getUuid();
        da.setVm(Vm.of(basePath + VmsRouteHandler.BASE_ROUTE + "/" + vol.getVmUuid(), vol.getVmUuid()));

        da.setId(diskAttachmentId);
        da.setHref(da.getVm().getHref() + "/diskattachments/" + diskAttachmentId);;

        // Links
        da.setDisk(toDisk(vol));

        // Properties
        da.setActive("true");
        da.setBootable(String.valueOf(Volume.Type.ROOT.equals(vol.getVolumeType())));
        da.setIface("virtio_scsi");
        da.setLogicalName(vol.getName());
        da.setReadOnly("false");
        da.setPassDiscard("false");

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
