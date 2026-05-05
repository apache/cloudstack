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
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.veeam.RouteHandler;
import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.VeeamControlServlet;
import org.apache.cloudstack.veeam.adapter.ServerAdapter;
import org.apache.cloudstack.veeam.api.dto.Api;
import org.apache.cloudstack.veeam.api.dto.ApiSummary;
import org.apache.cloudstack.veeam.api.dto.EmptyElement;
import org.apache.cloudstack.veeam.api.dto.Link;
import org.apache.cloudstack.veeam.api.dto.ProductInfo;
import org.apache.cloudstack.veeam.api.dto.Ref;
import org.apache.cloudstack.veeam.api.dto.SummaryCount;
import org.apache.cloudstack.veeam.api.dto.Version;
import org.apache.cloudstack.veeam.utils.Negotiation;

import com.cloud.utils.component.ManagerBase;

public class ApiRouteHandler extends ManagerBase implements RouteHandler {
    public static final String BASE_ROUTE = "/api";

    @Inject
    ServerAdapter serverAdapter;

    @Inject
    VeeamControlService veeamControlService;

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
        io.notFound(resp, null, outFormat);
    }

    private void handleRootApiRequest(HttpServletRequest req, HttpServletResponse resp, Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        io.getWriter().write(resp, HttpServletResponse.SC_OK,
                createApiObject(VeeamControlService.ContextPath.value() + BASE_ROUTE),
                outFormat);
    }

    protected Api createApiObject(String basePath) {
        Api api = new Api();

        /* ---------------- Links ---------------- */
        List<Link> links = new ArrayList<>();
        add(links, basePath + "/clusters", "clusters");
        add(links, basePath + "/clusters?search={query}", "clusters/search");
        add(links, basePath + "/datacenters", "datacenters");
        add(links, basePath + "/datacenters?search={query}", "datacenters/search");
        add(links, basePath + "/hosts", "hosts");
        add(links, basePath + "/hosts?search={query}", "hosts/search");
        add(links, basePath + "/networks", "networks");
        add(links, basePath + "/networks?search={query}", "networks/search");
        add(links, basePath + "/storagedomains", "storagedomains");
        add(links, basePath + "/storagedomains?search={query}", "storagedomains/search");
        add(links, basePath + "/vms", "vms");
        add(links, basePath + "/vms?search={query}", "vms/search");
        add(links, basePath + "/disks", "disks");
        add(links, basePath + "/disks?search={query}", "disks/search");

        api.setLink(links);

        /* ---------------- Engine backup ---------------- */
        api.setEngineBackup(new EmptyElement());

        /* ---------------- Product info ---------------- */
        ProductInfo productInfo = new ProductInfo();
        productInfo.setInstanceId(veeamControlService.getInstanceId());
        productInfo.name = VeeamControlService.PLUGIN_NAME;

        productInfo.version = Version.fromPackageAndCSVersion(true);
        api.setProductInfo(productInfo);

        /* ---------------- Summary ---------------- */
        ApiSummary summary = new ApiSummary();
        summary.setHosts(new SummaryCount(1, 1));
        summary.setStorageDomains(new SummaryCount(1, 2));
        summary.setUsers(new SummaryCount(1, 1));
        summary.setVms(new SummaryCount(1, 8));
        api.setSummary(summary);

        /* ---------------- Time ---------------- */
        api.setTime(System.currentTimeMillis());

        /* ---------------- Users ---------------- */
        String userId = serverAdapter.getServiceAccount().first().getUuid();
        api.setAuthenticatedUser(Ref.of(basePath + "/users/" + userId, userId));
        api.setEffectiveUser(Ref.of(basePath + "/users/" + userId, userId));

        return api;
    }

    private static void add(List<Link> links, String href, String rel) {
        links.add(Link.of(href, rel));
    }
}
