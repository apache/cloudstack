//
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
//

package org.apache.cloudstack.storage.datastore.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.springframework.stereotype.Component;

import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.provider.ElastistorPrimaryDataStoreProvider;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil.FileSystem;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil.ListInterfacesResponse;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil.ListPoolsResponse;

import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.dao.UserVmDao;

@Component
public class ElastistorVolumeApiServiceImpl extends ManagerBase implements ElastistorVolumeApiService {

    @Inject
    protected VolumeDao _volsDao;
    @Inject
    protected UserVmDao _userVmDao;
    @Inject
    protected DiskOfferingDao _diskOfferingDao;
    @Inject
    private PrimaryDataStoreDao _storagePoolDao;
    @Inject
    VolumeService volService;
    @Inject
    VolumeDataFactory volFactory;
    @Inject
    ElastistorPrimaryDataStoreProvider esProvider;
    @Inject
    VolumeDetailsDao volumeDetailsDao;

    @Override
    public List<Class<?>> getCommands() {

        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(ListElastistorVolumeCmd.class);
        cmdList.add(ListElastistorPoolCmd.class);
        cmdList.add(ListElastistorInterfaceCmd.class);

        logger.info("Commands were registered successfully with elastistor volume api service. [cmdcount:" + cmdList.size() + "]");
        return cmdList;

    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        _configParams = params;
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ListResponse<ListElastistorVolumeResponse> listElastistorVolume(ListElastistorVolumeCmd cmd) {

        try {
            FileSystem listVolumeResponse = ElastistorUtil.listVolume(cmd.getId());

            List<ListElastistorVolumeResponse> volumeResponses = new ArrayList<ListElastistorVolumeResponse>();

            ListElastistorVolumeResponse volumeResponse;

            volumeResponse = new ListElastistorVolumeResponse();

            volumeResponse.setId(listVolumeResponse.getUuid());
            volumeResponse.setName(listVolumeResponse.getName());
            volumeResponse.setGraceAllowed(listVolumeResponse.getGraceallowed());
            volumeResponse.setDeduplication(listVolumeResponse.getDeduplication());
            volumeResponse.setCompression(listVolumeResponse.getCompression());
            volumeResponse.setSync(listVolumeResponse.getSync());
            // set object name for a better json structure
            volumeResponse.setObjectName("elastistorvolume");
            volumeResponses.add(volumeResponse);

            ListResponse<ListElastistorVolumeResponse> response = new ListResponse<ListElastistorVolumeResponse>();

            response.setResponses(volumeResponses);

            return response;
        } catch (Throwable e) {
            logger.error("Unable to list elastistor volume.", e);
            throw new CloudRuntimeException("Unable to list elastistor volume. " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public ListResponse<ListElastistorPoolResponse> listElastistorPools(ListElastistorPoolCmd cmd) {

        try {
            ListPoolsResponse listPools = ElastistorUtil.ListElastistorPools();

            List<ListElastistorPoolResponse> poolResponses = new ArrayList<ListElastistorPoolResponse>();

            ListElastistorPoolResponse elastistorPoolResponse;

            for (int i = 0; i < listPools.getPools().getCount(); i++) {
                // Always instantiate inside the loop
                elastistorPoolResponse = new ListElastistorPoolResponse();

                elastistorPoolResponse.setId(listPools.getPools().getPool(i).getUuid());
                elastistorPoolResponse.setName(listPools.getPools().getPool(i).getName());
                elastistorPoolResponse.setAvailIOPS(Long.parseLong(listPools.getPools().getPool(i).getAvailIOPS()));
                elastistorPoolResponse.setCurrentAvailableSpace(Long.parseLong(listPools.getPools().getPool(i).getAvailableSpace()));
                elastistorPoolResponse.setState(listPools.getPools().getPool(i).getState());
                elastistorPoolResponse.setControllerid(listPools.getPools().getPool(i).getControllerid());
                elastistorPoolResponse.setGateway(listPools.getPools().getPool(i).getGateway());

                // set object name for a better json structure
                elastistorPoolResponse.setObjectName("elastistorpool");
                poolResponses.add(elastistorPoolResponse);
            }

            ListResponse<ListElastistorPoolResponse> response = new ListResponse<ListElastistorPoolResponse>();

            response.setResponses(poolResponses);

            return response;

        } catch (Throwable e) {
            logger.error("Unable to list elastistor pools.", e);
            throw new CloudRuntimeException("Unable to list elastistor pools. " + e.getMessage());
        }

    }

    @Override
    public ListResponse<ListElastistorInterfaceResponse> listElastistorInterfaces(ListElastistorInterfaceCmd cmd) {
        try {
            ListInterfacesResponse listInterfacesResponse = ElastistorUtil.ListElastistorInterfaces(cmd.getControllerId());

            List<ListElastistorInterfaceResponse> interfaceResponses = new ArrayList<ListElastistorInterfaceResponse>();

            ListElastistorInterfaceResponse interfaceResponse;

            for (int i = 0; i < listInterfacesResponse.getInterfaces().getCount(); i++) {
                // Always instantiate inside the loop
                interfaceResponse = new ListElastistorInterfaceResponse();

                interfaceResponse.setId(listInterfacesResponse.getInterfaces().getInterface(i).getUuid());
                interfaceResponse.setName(listInterfacesResponse.getInterfaces().getInterface(i).getName());
                interfaceResponse.setStatus(listInterfacesResponse.getInterfaces().getInterface(i).getStatus());

                // set object name for a better json structure
                interfaceResponse.setObjectName("elastistorInterface");
                interfaceResponses.add(interfaceResponse);
            }

            ListResponse<ListElastistorInterfaceResponse> response = new ListResponse<ListElastistorInterfaceResponse>();

            response.setResponses(interfaceResponses);

            return response;
        } catch (Throwable e) {
            logger.error("Unable to list elastistor interfaces.", e);
            throw new CloudRuntimeException("Unable to list elastistor interfaces. " + e.getMessage());
        }

    }

}
