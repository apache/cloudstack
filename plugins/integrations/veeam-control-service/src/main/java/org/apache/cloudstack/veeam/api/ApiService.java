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

package org.apache.cloudstack.veeam.api;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.veeam.RouteHandler;
import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.VeeamControlServlet;
import org.apache.cloudstack.veeam.api.dto.Api;
import org.apache.cloudstack.veeam.api.dto.EmptyElement;
import org.apache.cloudstack.veeam.api.dto.Link;
import org.apache.cloudstack.veeam.api.dto.ProductInfo;
import org.apache.cloudstack.veeam.api.dto.Ref;
import org.apache.cloudstack.veeam.api.dto.SpecialObjectRef;
import org.apache.cloudstack.veeam.api.dto.SpecialObjects;
import org.apache.cloudstack.veeam.api.dto.Summary;
import org.apache.cloudstack.veeam.api.dto.SummaryCount;
import org.apache.cloudstack.veeam.api.dto.Version;
import org.apache.cloudstack.veeam.utils.Negotiation;

import com.cloud.utils.component.ManagerBase;

public class ApiService extends ManagerBase implements RouteHandler {
    public static final String BASE_ROUTE = "/api";

    @Override
    public boolean canHandle(String method, String path) {
        return getSanitizedPath(path).startsWith("/api");
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp, String path, Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        final String sanitizedPath = getSanitizedPath(path);
        if (sanitizedPath.equals(BASE_ROUTE)) {
            handleRootApiRequest(req, resp, outFormat, io);
            return;
        }
        io.getWriter().writeFault(resp, HttpServletResponse.SC_NOT_FOUND, "Not found", null, outFormat);
    }

    private void handleRootApiRequest(HttpServletRequest req, HttpServletResponse resp, Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        io.getWriter().write(resp, 200,
                createDummyApi(VeeamControlService.ContextPath.value() + BASE_ROUTE),
                outFormat);
    }

    private static Api createDummyApi(String basePath) {
        Api api = new Api();

        /* ---------------- Links ---------------- */
        List<Link> links = new ArrayList<>();
        add(links, basePath + "/clusters", "clusters");
        add(links, basePath + "/clusters?search={query}", "clusters/search");
        add(links, basePath + "/datacenters", "datacenters");
        add(links, basePath + "/datacenters?search={query}", "datacenters/search");
        add(links, basePath + "/events", "events");
        add(links, basePath + "/events;from={event_id}?search={query}", "events/search");
        add(links, basePath + "/hosts", "hosts");
        add(links, basePath + "/hosts?search={query}", "hosts/search");
        add(links, basePath + "/networks", "networks");
        add(links, basePath + "/networks?search={query}", "networks/search");
        add(links, basePath + "/storagedomains", "storagedomains");
        add(links, basePath + "/storagedomains?search={query}", "storagedomains/search");
        add(links, basePath + "/templates", "templates");
        add(links, basePath + "/templates?search={query}", "templates/search");
        add(links, basePath + "/vms", "vms");
        add(links, basePath + "/vms?search={query}", "vms/search");
        add(links, basePath + "/disks", "disks");
        add(links, basePath + "/disks?search={query}", "disks/search");

        api.link = links;

        /* ---------------- Engine backup ---------------- */
        api.engineBackup = new EmptyElement();

        /* ---------------- Product info ---------------- */
        ProductInfo productInfo = new ProductInfo();
        productInfo.instanceId = UUID.randomUUID().toString();
        productInfo.name = "oVirt Engine";

        Version version = new Version();
        version.build = "8";
        version.fullVersion = "4.5.8-0.master.fake.el9";
        version.major = 4;
        version.minor = 5;
        version.revision = 0;

        productInfo.version = version;
        api.productInfo = productInfo;

        /* ---------------- Special objects ---------------- */
        SpecialObjects specialObjects = new SpecialObjects();
        specialObjects.blankTemplate = new SpecialObjectRef(
                basePath + "/templates/00000000-0000-0000-0000-000000000000",
                "00000000-0000-0000-0000-000000000000"
        );
        specialObjects.rootTag = new SpecialObjectRef(
                basePath + "/tags/00000000-0000-0000-0000-000000000000",
                "00000000-0000-0000-0000-000000000000"
        );
        api.specialObjects = specialObjects;

        /* ---------------- Summary ---------------- */
        Summary summary = new Summary();
        summary.hosts = new SummaryCount(1, 1);
        summary.storageDomains = new SummaryCount(1, 2);
        summary.users = new SummaryCount(1, 1);
        summary.vms = new SummaryCount(1, 8);
        api.summary = summary;

        /* ---------------- Time ---------------- */
        api.time = OffsetDateTime.now(ZoneOffset.ofHours(2)).toInstant().toEpochMilli();

        /* ---------------- Users ---------------- */
        String userId = UUID.randomUUID().toString();
        api.authenticatedUser = Ref.of(basePath + "/users/" + userId, userId);
        api.effectiveUser = Ref.of(basePath + "/users/" + userId, userId);

        return api;
    }

    private static void add(List<Link> links, String href, String rel) {
        links.add(new Link(href, rel));
    }
}
