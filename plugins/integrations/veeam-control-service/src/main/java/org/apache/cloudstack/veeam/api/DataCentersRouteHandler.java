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

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.veeam.RouteHandler;
import org.apache.cloudstack.veeam.VeeamControlServlet;
import org.apache.cloudstack.veeam.api.converter.DataCenterJoinVOToDataCenterConverter;
import org.apache.cloudstack.veeam.api.converter.NetworkVOToNetworkConverter;
import org.apache.cloudstack.veeam.api.converter.StoreVOToStorageDomainConverter;
import org.apache.cloudstack.veeam.api.dto.DataCenter;
import org.apache.cloudstack.veeam.api.dto.DataCenters;
import org.apache.cloudstack.veeam.api.dto.Network;
import org.apache.cloudstack.veeam.api.dto.Networks;
import org.apache.cloudstack.veeam.api.dto.StorageDomain;
import org.apache.cloudstack.veeam.api.dto.StorageDomains;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.apache.cloudstack.veeam.utils.PathUtil;

import com.cloud.api.query.dao.DataCenterJoinDao;
import com.cloud.api.query.dao.ImageStoreJoinDao;
import com.cloud.api.query.dao.StoragePoolJoinDao;
import com.cloud.api.query.vo.DataCenterJoinVO;
import com.cloud.api.query.vo.ImageStoreJoinVO;
import com.cloud.api.query.vo.StoragePoolJoinVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;

public class DataCentersRouteHandler extends ManagerBase implements RouteHandler {
    public static final String BASE_ROUTE = "/api/datacenters";
    private static final int DEFAULT_MAX = 50;
    private static final int HARD_CAP_MAX = 1000;
    private static final int DEFAULT_PAGE = 1;

    @Inject
    DataCenterJoinDao dataCenterJoinDao;

    @Inject
    StoragePoolJoinDao storagePoolJoinDao;

    @Inject
    ImageStoreJoinDao imageStoreJoinDao;

    @Inject
    NetworkDao networkDao;

    @Override
    public boolean start() {
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
        if (!"GET".equalsIgnoreCase(method)) {
            io.methodNotAllowed(resp, "GET", outFormat);
            return;
        }
        final String sanitizedPath = getSanitizedPath(path);
        if (sanitizedPath.equals(BASE_ROUTE)) {
            handleGet(req, resp, outFormat, io);
            return;
        }

        Pair<String, String> idAndSubPath = PathUtil.extractIdAndSubPath(sanitizedPath, BASE_ROUTE);
        if (idAndSubPath != null) {
            // /api/datacenters/{id}
            if (idAndSubPath.first() != null) {
                if (idAndSubPath.second() == null) {
                    handleGetById(idAndSubPath.first(), resp, outFormat, io);
                    return;
                }
                if ("storagedomains".equals(idAndSubPath.second())) {
                    handleGetStorageDomainsByDcId(idAndSubPath.first(), resp, outFormat, io);
                    return;
                }
                if ("networks".equals(idAndSubPath.second())) {
                    handleGetNetworksByDcId(idAndSubPath.first(), resp, outFormat, io);
                    return;
                }
            }
        }

        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Not found");
    }

    public void handleGet(final HttpServletRequest req, final HttpServletResponse resp,
                          Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        final List<DataCenter> result = DataCenterJoinVOToDataCenterConverter.toDCList(listDCs());
        final DataCenters response = new DataCenters(result);

        io.getWriter().write(resp, 200, response, outFormat);
    }

    protected List<DataCenterJoinVO> listDCs() {
        return dataCenterJoinDao.listAll();
    }

    public void handleGetById(final String id, final HttpServletResponse resp, final Negotiation.OutFormat outFormat,
                              final VeeamControlServlet io) throws IOException {
        final DataCenterJoinVO dataCenterVO = dataCenterJoinDao.findByUuid(id);
        if (dataCenterVO == null) {
            io.notFound(resp, "DataCenter not found: " + id, outFormat);
            return;
        }
        DataCenter response = DataCenterJoinVOToDataCenterConverter.toDataCenter(dataCenterVO);

        io.getWriter().write(resp, 200, response, outFormat);
    }

    protected List<StoragePoolJoinVO> listStoragePoolsByDcId(final long dcId) {
        return storagePoolJoinDao.listAll();
    }

    protected List<ImageStoreJoinVO> listImageStoresByDcId(final long dcId) {
        return imageStoreJoinDao.listAll();
    }

    protected List<NetworkVO> listNetworksByDcId(final long dcId) {
        return networkDao.listAll();
    }

    public void handleGetStorageDomainsByDcId(final String id, final HttpServletResponse resp, final Negotiation.OutFormat outFormat,
              final VeeamControlServlet io) throws IOException {
        final DataCenterJoinVO dataCenterVO = dataCenterJoinDao.findByUuid(id);
        if (dataCenterVO == null) {
            io.notFound(resp, "DataCenter not found: " + id, outFormat);
            return;
        }
        List<StorageDomain> storageDomains = StoreVOToStorageDomainConverter.toStorageDomainListFromPools(listStoragePoolsByDcId(dataCenterVO.getId()));
        storageDomains.addAll(StoreVOToStorageDomainConverter.toStorageDomainListFromStores(listImageStoresByDcId(dataCenterVO.getId())));

        StorageDomains response = new StorageDomains(storageDomains);

        io.getWriter().write(resp, 200, response, outFormat);
    }

    public void handleGetNetworksByDcId(final String id, final HttpServletResponse resp, final Negotiation.OutFormat outFormat,
              final VeeamControlServlet io) throws IOException {
        final DataCenterJoinVO dataCenterVO = dataCenterJoinDao.findByUuid(id);
        if (dataCenterVO == null) {
            io.notFound(resp, "DataCenter not found: " + id, outFormat);
            return;
        }
        List<Network> networks = NetworkVOToNetworkConverter.toNetworkList(listNetworksByDcId(dataCenterVO.getId()), (dcId) -> dataCenterVO);

        Networks response = new Networks(networks);

        io.getWriter().write(resp, 200, response, outFormat);
    }
}
