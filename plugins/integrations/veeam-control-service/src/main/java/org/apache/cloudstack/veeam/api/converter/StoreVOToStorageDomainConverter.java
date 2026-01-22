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
import org.apache.cloudstack.veeam.api.ApiService;
import org.apache.cloudstack.veeam.api.DataCentersRouteHandler;
import org.apache.cloudstack.veeam.api.dto.DataCenter;
import org.apache.cloudstack.veeam.api.dto.DataCenters;
import org.apache.cloudstack.veeam.api.dto.Link;
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
        sd.id = id;
        final String href = href(basePath, ApiService.BASE_ROUTE + "/storagedomains/" + id);
        sd.href = href;

        sd.name = pool.getName();
        sd.description = ""; // oVirt often returns empty string
        sd.comment = "";

        // oVirt sample returns numbers as strings
        sd.available = Long.toString(pool.getCapacityBytes() - pool.getUsedBytes());
        sd.used = Long.toString(pool.getUsedBytes());
        sd.committed = Long.toString(pool.getCapacityBytes());

        sd.type = "data";
        sd.status = mapPoolStatus(pool);   // "active"/"inactive"/"maintenance" (approx)
        sd.master = "true";                // if you don’t have a concept, choose stable default
        sd.backup = "false";

        sd.blockSize = "512";              // stable default unless you can compute it
        sd.externalStatus = "ok";
        sd.storageFormat = "v5";

        sd.discardAfterDelete = "false";
        sd.wipeAfterDelete = "false";
        sd.supportsDiscard = "false";
        sd.supportsDiscardZeroesData = "false";

        sd.warningLowSpaceIndicator = "10";
        sd.criticalSpaceActionBlocker = "5";

        // Nested storage (try to extract if available)
        sd.storage = buildPrimaryStorage(pool);

        // dc attachment
        String dcId = pool.getZoneUuid();
        DataCenter dc = new DataCenter();
        dc.href = href(basePath, DataCentersRouteHandler.BASE_ROUTE + "/" + dcId);
        dc.id = dcId;
        sd.dataCenters = new DataCenters(List.of(dc));

        sd.link = defaultStorageDomainLinks(href, true, /*includeTemplates*/ true);

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
        sd.id = id;
        final String href = href(basePath, ApiService.BASE_ROUTE + "/storagedomains/" + id);
        sd.href = href;

        sd.name = store.getName();
        sd.description = "";
        sd.comment = "";

        // Many image repos don’t have these values readily; keep as "0" or omit (null)
        sd.committed = "0";
        sd.available = null; // oVirt’s glance example omitted available/used
        sd.used = null;

        sd.type = "image";
        sd.status = "unattached"; // matches your sample for glance-like repo
        sd.master = "false";
        sd.backup = "false";

        sd.blockSize = "512";
        sd.externalStatus = "ok";
        sd.storageFormat = "v1";

        sd.discardAfterDelete = "false";
        sd.wipeAfterDelete = "false";
        sd.supportsDiscard = "false";
        sd.supportsDiscardZeroesData = "false";

        sd.warningLowSpaceIndicator = "0";
        sd.criticalSpaceActionBlocker = "0";

        sd.storage = buildImageStoreStorage(store);

        // Optionally include dc attachment (your first object had it; second didn’t)
        String dcId = store.getZoneUuid();
        DataCenter dc = new DataCenter();
        dc.href = href(basePath, DataCentersRouteHandler.BASE_ROUTE + "/" + dcId);
        dc.id = dcId;
        sd.dataCenters = new DataCenters(List.of(dc));

        sd.link = defaultStorageDomainLinks(href, false, /*includeTemplates*/ false);

        return sd;
    }

    public static List<StorageDomain> toStorageDomainListFromStores(final List<ImageStoreJoinVO> stores) {
        return stores.stream().map(StoreVOToStorageDomainConverter::toStorageDomain).collect(Collectors.toList());
    }

    // ----------- Helpers -----------

    private static Storage buildPrimaryStorage(StoragePoolJoinVO pool) {
        Storage st = new Storage();
        st.type = mapPrimaryStorageType(pool);

        // If you can parse details/url, fill these. If not, keep empty strings like oVirt.
        // For NFS pools in CloudStack, URL is often like: nfs://10.0.32.4/path or 10.0.32.4:/path
        String url = null;
        try {
            url = pool.getHostAddress(); // sometimes exists in VO; if not, ignore
        } catch (Exception ignored) { }

        if ("nfs".equals(st.type)) {
            // best-effort placeholders
            st.address = "";        // fill if you can parse
            st.path = "";           // fill if you can parse
            st.mountOptions = "";
            st.nfsVersion = "auto";
        }
        return st;
    }

    private static Storage buildImageStoreStorage(ImageStoreJoinVO store) {
        Storage st = new Storage();

        // Match your sample: glance store => type=glance
        // If you want "nfs" for secondary, map based on provider/protocol instead.
        st.type = mapImageStorageType(store);

        if ("nfs".equals(st.type)) {
            st.address = "";
            st.path = "";
            st.mountOptions = "";
            st.nfsVersion = "auto";
        }
        return st;
    }

    private static List<Link> defaultStorageDomainLinks(String basePath, boolean includeDisks, boolean includeTemplates) {
        // Mirrors the rels you pasted; keep stable order.
        // You can add/remove based on what endpoints you actually implement.
        List<Link> common = new java.util.ArrayList<>();
        common.add(new Link("diskprofiles", href(basePath, "/diskprofiles")));
        if (includeDisks) {
            common.add(new Link("disks", href(basePath, "/disks")));
            common.add(new Link("storageconnections", href(basePath, "/storageconnections")));
        }
        common.add(new Link("permissions", href(basePath, "/permissions")));
        if (includeTemplates) {
            common.add(new Link("templates", href(basePath, "/templates")));
            common.add(new Link("vms", href(basePath, "/vms")));
        } else {
            common.add(new Link("images", href(basePath, "/images")));
        }
        common.add(new Link("disksnapshots", href(basePath, "/disksnapshots")));
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
                if (s.contains("networkfilesystem") || s.contains("nfs")) return "nfs";
                if (s.contains("iscsi")) return "iscsi";
                if (s.contains("filesystem")) return "posixfs";
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
