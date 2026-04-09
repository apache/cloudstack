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

import java.util.List;
import java.util.stream.Collectors;

import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.api.ApiRouteHandler;
import org.apache.cloudstack.veeam.api.DataCentersRouteHandler;
import org.apache.cloudstack.veeam.api.dto.DataCenter;
import org.apache.cloudstack.veeam.api.dto.Link;
import org.apache.cloudstack.veeam.api.dto.NamedList;
import org.apache.cloudstack.veeam.api.dto.Storage;
import org.apache.cloudstack.veeam.api.dto.StorageDomain;

import com.cloud.api.query.vo.ImageStoreJoinVO;
import com.cloud.api.query.vo.StoragePoolJoinVO;

public class StoreVOToStorageDomainConverter {

    /** Primary storage -> oVirt storage_domain (type=data) */
    public static StorageDomain toStorageDomain(final StoragePoolJoinVO pool) {
        final String basePath = VeeamControlService.ContextPath.value();

        final String id = pool.getUuid();

        StorageDomain sd = new StorageDomain();
        sd.setId(id);
        final String href = href(basePath, ApiRouteHandler.BASE_ROUTE + "/storagedomains/" + id);
        sd.setHref(href);

        sd.setName(pool.getName());

        // oVirt sample returns numbers as strings
        sd.setAvailable(Long.toString(pool.getCapacityBytes() - pool.getUsedBytes()));
        sd.setUsed(Long.toString(pool.getUsedBytes()));
        sd.setCommitted(Long.toString(pool.getCapacityBytes()));

        sd.setType("data");
        sd.setStatus(mapPoolStatus(pool));   // "active"/"inactive"/"maintenance" (approx)
        sd.setMaster("true");                // if you don’t have a concept, choose stable default
        sd.setBackup("false");

        sd.setBlockSize("512");              // stable default unless you can compute it
        sd.setExternalStatus("ok");
        sd.setStorageFormat("v5");

        sd.setDiscardAfterDelete("false");
        sd.setWipeAfterDelete("false");
        sd.setSupportsDiscard("false");
        sd.setSupportsDiscardZeroesData("false");

        sd.setWarningLowSpaceIndicator("10");
        sd.setCriticalSpaceActionBlocker("5");

        // Nested storage (try to extract if available)
        sd.setStorage(buildPrimaryStorage(pool));

        // dc attachment
        String dcId = pool.getZoneUuid();
        DataCenter dc = new DataCenter();
        dc.setHref(href(basePath, DataCentersRouteHandler.BASE_ROUTE + "/" + dcId));
        dc.setId(dcId);
        sd.setDataCenters(NamedList.of("data_center", List.of(dc)));

        sd.setLink(defaultStorageDomainLinks(href, true, /*includeTemplates*/ true));

        return sd;
    }

    public static List<StorageDomain> toStorageDomainListFromPools(final List<StoragePoolJoinVO> pools) {
        return pools.stream().map(StoreVOToStorageDomainConverter::toStorageDomain).collect(Collectors.toList());
    }

    /** Secondary/Image store -> oVirt storage_domain (type=image) */
    public static StorageDomain toStorageDomain(final ImageStoreJoinVO store) {
        final String basePath = VeeamControlService.ContextPath.value();

        final String id = store.getUuid();

        StorageDomain sd = new StorageDomain();
        sd.setId(id);
        final String href = href(basePath, ApiRouteHandler.BASE_ROUTE + "/storagedomains/" + id);
        sd.setHref(href);

        sd.setName(store.getName());

        // Many image repos don’t have these values readily; keep as "0" or omit (null)
        sd.setCommitted("0");
        sd.setAvailable(null); // oVirt’s glance example omitted available/used
        sd.setUsed(null);

        sd.setType("image");
        sd.setStatus("unattached"); // matches your sample for glance-like repo
        sd.setMaster("false");
        sd.setBackup("false");

        sd.setBlockSize("512");
        sd.setExternalStatus("ok");
        sd.setStorageFormat("v1");

        sd.setDiscardAfterDelete("false");
        sd.setWipeAfterDelete("false");
        sd.setSupportsDiscard("false");
        sd.setSupportsDiscardZeroesData("false");

        sd.setWarningLowSpaceIndicator("0");
        sd.setCriticalSpaceActionBlocker("0");

        sd.setStorage(buildImageStoreStorage(store));

        // Optionally include dc attachment (your first object had it; second didn’t)
        String dcId = store.getZoneUuid();
        DataCenter dc = new DataCenter();
        dc.setHref(href(basePath, DataCentersRouteHandler.BASE_ROUTE + "/" + dcId));
        dc.setId(dcId);
        sd.setDataCenters(NamedList.of("data_center", List.of(dc)));

        sd.setLink(defaultStorageDomainLinks(href, false, /*includeTemplates*/ false));

        return sd;
    }

    public static List<StorageDomain> toStorageDomainListFromStores(final List<ImageStoreJoinVO> stores) {
        return stores.stream().map(StoreVOToStorageDomainConverter::toStorageDomain).collect(Collectors.toList());
    }

    // ----------- Helpers -----------

    private static Storage buildPrimaryStorage(StoragePoolJoinVO pool) {
        Storage st = new Storage();
        st.setType(mapPrimaryStorageType(pool));

        // If you can parse details/url, fill these. If not, keep empty strings like oVirt.
        // For NFS pools in CloudStack, URL is often like: nfs://10.0.32.4/path or 10.0.32.4:/path
        String url = null;
        try {
            url = pool.getHostAddress(); // sometimes exists in VO; if not, ignore
        } catch (Exception ignored) { }

        if ("nfs".equals(st.getType())) {
            // best-effort placeholders
            st.setAddress("");        // fill if you can parse
            st.setPath("");           // fill if you can parse
            st.setMountOptions("");
            st.setNfsVersion("auto");
        }
        return st;
    }

    private static Storage buildImageStoreStorage(ImageStoreJoinVO store) {
        Storage st = new Storage();

        // Match your sample: glance store => type=glance
        // If you want "nfs" for secondary, map based on provider/protocol instead.
        st.setType(mapImageStorageType(store));

        if ("nfs".equals(st.getType())) {
            st.setAddress("");
            st.setPath("");
            st.setMountOptions("");
            st.setNfsVersion("auto");
        }
        return st;
    }

    private static List<Link> defaultStorageDomainLinks(String basePath, boolean includeDisks, boolean includeTemplates) {
        // Mirrors the rels you pasted; keep stable order.
        // You can add/remove based on what endpoints you actually implement.
        List<Link> common = new java.util.ArrayList<>();
        common.add(Link.of("diskprofiles", href(basePath, "/diskprofiles")));
        if (includeDisks) {
            common.add(Link.of("disks", href(basePath, "/disks")));
            common.add(Link.of("storageconnections", href(basePath, "/storageconnections")));
        }
        common.add(Link.of("permissions", href(basePath, "/permissions")));
        if (includeTemplates) {
            common.add(Link.of("templates", href(basePath, "/templates")));
            common.add(Link.of("vms", href(basePath, "/vms")));
        } else {
            common.add(Link.of("images", href(basePath, "/images")));
        }
        common.add(Link.of("disksnapshots", href(basePath, "/disksnapshots")));
        return common;
    }

    private static String mapPoolStatus(StoragePoolJoinVO pool) {
        // This is approximate; adjust if you have better signals.
        try {
            Object status = pool.getStatus(); // often StoragePoolStatus enum
            if (status != null) {
                String s = status.toString().toLowerCase();
                if (s.contains("up") || s.contains("enabled")) return "active";
                if (s.contains("maintenance")) return "maintenance";
            }
        } catch (Exception ignored) { }
        return "inactive";
    }

    private static String mapPrimaryStorageType(StoragePoolJoinVO pool) {
        try {
            Object t = pool.getPoolType(); // often StoragePoolType enum
            if (t != null) {
                String s = t.toString().toLowerCase();
                if (s.contains("networkfilesystem") || s.contains("nfs") || s.contains("sharedmountpoint")) return "nfs";
                if (s.contains("iscsi")) return "iscsi";
                if (s.contains("filesystem")) return "localfs";
                if (s.contains("rbd") || s.contains("ceph")) return "cinder"; // not perfect; pick stable
            }
        } catch (Exception ignored) { }
        return "unknown";
    }

    private static String mapImageStorageType(ImageStoreJoinVO store) {
        // If your secondary store is S3/NFS/etc, you may want different mapping.
        // For your oVirt sample, "glance" is used for an image repo.
        try {
            String provider = store.getProviderName(); // may exist
            if (provider != null && provider.toLowerCase().contains("glance")) return "glance";
        } catch (Exception ignored) { }
        return "glance";
    }

    private static String href(String baseUrl, String path) {
        if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        return baseUrl + path;
    }
}
