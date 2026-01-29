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
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.veeam.RouteHandler;
import org.apache.cloudstack.veeam.VeeamControlServlet;
import org.apache.cloudstack.veeam.api.converter.UserVmJoinVOToVmConverter;
import org.apache.cloudstack.veeam.api.converter.VolumeJoinVOToDiskConverter;
import org.apache.cloudstack.veeam.api.dto.DiskAttachment;
import org.apache.cloudstack.veeam.api.dto.DiskAttachments;
import org.apache.cloudstack.veeam.api.dto.Vm;
import org.apache.cloudstack.veeam.api.request.VmListQuery;
import org.apache.cloudstack.veeam.api.request.VmSearchExpr;
import org.apache.cloudstack.veeam.api.request.VmSearchFilters;
import org.apache.cloudstack.veeam.api.request.VmSearchParser;
import org.apache.cloudstack.veeam.api.response.VmCollectionResponse;
import org.apache.cloudstack.veeam.api.response.VmEntityResponse;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.apache.cloudstack.veeam.utils.PathUtil;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.api.query.dao.HostJoinDao;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.dao.VolumeJoinDao;
import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.utils.component.ManagerBase;

public class VmsRouteHandler extends ManagerBase implements RouteHandler {
    public static final String BASE_ROUTE = "/api/vms";
    private static final int DEFAULT_MAX = 50;
    private static final int HARD_CAP_MAX = 1000;
    private static final int DEFAULT_PAGE = 1;

    @Inject
    UserVmJoinDao userVmJoinDao;

    @Inject
    HostJoinDao hostJoinDao;

    @Inject
    VolumeJoinDao volumeJoinDao;

    private VmSearchParser searchParser;

    @Override
    public boolean start() {

        this.searchParser = new VmSearchParser(Set.of(
                "id", "name", "status", "cluster", "host", "template"
        ));
        return true;
    }

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public boolean canHandle(String method, String path) {
        return getSanitizedPath(path).startsWith(BASE_ROUTE);
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp, String path, Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        final String method = req.getMethod();
        final String sanitizedPath = getSanitizedPath(path);
        if (sanitizedPath.equals(BASE_ROUTE)) {
            if (!"GET".equalsIgnoreCase(method)) {
                io.methodNotAllowed(resp, "GET", outFormat);
                return;
            }
            handleGet(req, resp, outFormat, io);
            return;
        }

        List<String> idAndSubPath = PathUtil.extractIdAndSubPath(sanitizedPath, BASE_ROUTE);
        if (CollectionUtils.isNotEmpty(idAndSubPath)) {
            String id = idAndSubPath.get(0);
            if (idAndSubPath.size() == 1) {
                handleGetById(id, resp, outFormat, io);
                return;
            } else if (idAndSubPath.size() == 2) {
                String subPath = idAndSubPath.get(1);
                if ("diskattachments".equals(subPath)) {
                    handleGetDisAttachmentsByVmId(id, resp, outFormat, io);
                    return;
                }
            }
        }

        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Not found");
    }

    protected void handleGet(final HttpServletRequest req, final HttpServletResponse resp,
          Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        final VmListQuery q = fromRequest(req);

        // Validate max/page early (optional strictness)
        if (q.getMax() != null && q.getMax() <= 0) {
            io.notFound(resp, "Invalid 'max' (must be > 0)", outFormat);
            return;
        }
        if (q.getPage() != null && q.getPage() <= 0) {
            io.notFound(resp, "Invalid 'page' (must be > 0)", outFormat);
            return;
        }

        final int limit = q.resolvedMax(DEFAULT_MAX, HARD_CAP_MAX);
        final int offset = q.offset(DEFAULT_MAX, HARD_CAP_MAX, DEFAULT_PAGE);

        final VmSearchExpr expr;
        try {
            expr = searchParser.parse(q.getSearch());
        } catch (VmSearchParser.VmSearchParseException e) {
            io.notFound(resp, "Invalid search: " + e.getMessage(), outFormat);
            return;
        }

        final VmSearchFilters filters;
        try {
            filters = VmSearchFilters.fromAndOnly(expr); // AND-only v1
        } catch (VmSearchParser.VmSearchParseException e) {
            io.notFound(resp, "Unsupported search: " + e.getMessage(), outFormat);
            return;
        }

        final List<Vm> result = UserVmJoinVOToVmConverter.toVmList(listUserVms(), this::getHostById);
        final VmCollectionResponse response = new VmCollectionResponse(result);

        io.getWriter().write(resp, 200, response, outFormat);
    }

    protected static VmListQuery fromRequest(final HttpServletRequest req) {
        final VmListQuery q = new VmListQuery();
        q.setSearch(req.getParameter("search"));
        q.setMax(parseIntOrNull(req.getParameter("max")));
        q.setPage(parseIntOrNull(req.getParameter("page")));
        return q;
    }

    protected static Integer parseIntOrNull(final String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return Integer.valueOf(-1); // will be rejected by validation above
        }
    }

    protected List<UserVmJoinVO> listUserVms() {
        // Todo: add filtering, pagination
        return userVmJoinDao.listAll();
    }

    protected void handleGetById(final String id, final HttpServletResponse resp, final Negotiation.OutFormat outFormat,
          final VeeamControlServlet io) throws IOException {
        final UserVmJoinVO userVmJoinVO = userVmJoinDao.findByUuid(id);
        if (userVmJoinVO == null) {
            io.notFound(resp, "VM not found: " + id, outFormat);
            return;
        }
        VmEntityResponse response = new VmEntityResponse(UserVmJoinVOToVmConverter.toVm(userVmJoinVO, this::getHostById));

        io.getWriter().write(resp, 200, response, outFormat);
    }

    protected void handleGetDisAttachmentsByVmId(final String id, final HttpServletResponse resp, final Negotiation.OutFormat outFormat,
                                              final VeeamControlServlet io) throws IOException {
        final UserVmJoinVO userVmJoinVO = userVmJoinDao.findByUuid(id);
        if (userVmJoinVO == null) {
            io.notFound(resp, "VM not found: " + id, outFormat);
            return;
        }
        List<DiskAttachment> disks = VolumeJoinVOToDiskConverter.toDiskAttachmentList(
              volumeJoinDao.listByInstanceId(userVmJoinVO.getId()));
        DiskAttachments response = new DiskAttachments(disks);

        io.getWriter().write(resp, 200, response, outFormat);
    }

    protected HostJoinVO getHostById(Long hostId) {
        if (hostId == null) {
            return null;
        }
        return hostJoinDao.findById(hostId);
    }
}
