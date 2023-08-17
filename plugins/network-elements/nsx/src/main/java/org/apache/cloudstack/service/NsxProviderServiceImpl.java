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
package org.apache.cloudstack.service;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.network.NsxProvider;
import com.cloud.network.dao.NsxProviderDao;
import com.cloud.network.element.NsxProviderVO;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.command.ListNsxControllersCmd;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.command.AddNsxControllerCmd;
import org.apache.cloudstack.api.response.NsxControllerResponse;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NsxProviderServiceImpl implements NsxProviderService {

    @Inject
    NsxProviderDao nsxProviderDao;
    @Inject
    DataCenterDao dataCenterDao;

    @Override
    public NsxProvider addProvider(AddNsxControllerCmd cmd) {
        NsxProviderVO nsxProvider = Transaction.execute((TransactionCallback<NsxProviderVO>) status -> {
            NsxProviderVO nsxProviderVO = new NsxProviderVO(cmd.getZoneId(), cmd.getName(), cmd.getHostname(),
                    cmd.getUsername(),  cmd.getPassword(),
                    cmd.getTier0Gateway(), cmd.getEdgeCluster());
            nsxProviderDao.persist(nsxProviderVO);
            return nsxProviderVO;
        });
        return  null;
    }

    @Override
    public NsxControllerResponse createNsxControllerResponse(NsxProvider nsxProvider) {
        DataCenterVO zone  = dataCenterDao.findById(nsxProvider.getZoneId());
        if (Objects.isNull(zone)) {
            throw new CloudRuntimeException(String.format("Failed to find zone with id %s", nsxProvider.getZoneId()));
        }
        NsxControllerResponse response = new NsxControllerResponse();
        response.setName(nsxProvider.getProviderName());
        response.setHostname(nsxProvider.getHostname());
        response.setZoneId(nsxProvider.getZoneId());
        response.setZoneName(zone.getName());
        response.setTier0Gateway(nsxProvider.getTier0Gateway());
        response.setTier0Gateway(nsxProvider.getEdgeCluster());
        return response;
    }

    @Override
    public List<BaseResponse> listNsxProviders(Long zoneId) {
        List<BaseResponse> nsxControllersResponseList = new ArrayList<>();
        if (zoneId != null) {
            NsxProviderVO nsxProviderVO = nsxProviderDao.findByZoneId(zoneId);
            nsxControllersResponseList.add(createNsxControllerResponse(nsxProviderVO));
        } else {
            List<NsxProviderVO> nsxProviderVOList = nsxProviderDao.listAll();
            for (NsxProviderVO nsxProviderVO : nsxProviderVOList) {
                nsxControllersResponseList.add(createNsxControllerResponse(nsxProviderVO));
            }
        }

        return nsxControllersResponseList;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(AddNsxControllerCmd.class);
        cmdList.add(ListNsxControllersCmd.class);
        return cmdList;
    }
}
