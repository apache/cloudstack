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
package org.apache.cloudstack.solidfire;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

// import org.apache.log4j.Logger;
import org.apache.cloudstack.acl.APIChecker;
import org.apache.cloudstack.storage.datastore.util.SolidFireUtil;
import org.apache.cloudstack.api.command.user.solidfire.GetSolidFireAccountIdCmd;
import org.apache.cloudstack.api.command.user.solidfire.GetSolidFireVolumeAccessGroupIdCmd;
import org.apache.cloudstack.api.command.user.solidfire.GetSolidFireVolumeIscsiNameCmd;
import org.apache.cloudstack.api.command.user.solidfire.GetSolidFireVolumeSizeCmd;
import org.apache.cloudstack.api.response.ApiSolidFireAccountIdResponse;
import org.apache.cloudstack.api.response.ApiSolidFireVolumeAccessGroupIdResponse;
import org.apache.cloudstack.api.response.ApiSolidFireVolumeIscsiNameResponse;
import org.apache.cloudstack.api.response.ApiSolidFireVolumeSizeResponse;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.springframework.stereotype.Component;

import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.AccountDetailVO;
import com.cloud.user.User;
import com.cloud.utils.component.AdapterBase;

@Component
public class ApiSolidFireServiceImpl extends AdapterBase implements APIChecker, ApiSolidFireService {
    // private static final Logger s_logger = Logger.getLogger(ApiSolidFireServiceImpl.class);

    @Inject private AccountDetailsDao _accountDetailsDao;
    @Inject private DataStoreProviderManager _dataStoreProviderMgr;
    @Inject private ClusterDetailsDao _clusterDetailsDao;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        return true;
    }

    @Override
    public ApiSolidFireAccountIdResponse getSolidFireAccountId(Long csAccountId, Long storagePoolId) {
        AccountDetailVO accountDetail = _accountDetailsDao.findDetail(csAccountId, SolidFireUtil.getAccountKey(storagePoolId));
        String sfAccountId = accountDetail.getValue();

        return new ApiSolidFireAccountIdResponse(Long.parseLong(sfAccountId));
    }

    @Override
    public ApiSolidFireVolumeSizeResponse getSolidFireVolumeSize(Volume volume, StoragePool storagePool) {
        PrimaryDataStoreDriver primaryStoreDriver = null;

        try {
            DataStoreProvider storeProvider = _dataStoreProviderMgr.getDataStoreProvider(storagePool.getStorageProviderName());
            DataStoreDriver storeDriver = storeProvider.getDataStoreDriver();

            if (storeDriver instanceof PrimaryDataStoreDriver) {
                primaryStoreDriver = (PrimaryDataStoreDriver)storeDriver;
            }
        }
        catch (InvalidParameterValueException e) {
            throw new InvalidParameterValueException("Invalid Storage Driver Type");
        }

        return new ApiSolidFireVolumeSizeResponse(primaryStoreDriver.getVolumeSizeIncludingHypervisorSnapshotReserve(volume, storagePool));
    }

    @Override
    public ApiSolidFireVolumeAccessGroupIdResponse getSolidFireVolumeAccessGroupId(Long csClusterId, Long storagePoolId) {
        ClusterDetailsVO clusterDetails = _clusterDetailsDao.findDetail(csClusterId, SolidFireUtil.getVagKey(storagePoolId));
        String sfVagId = clusterDetails.getValue();

        return new ApiSolidFireVolumeAccessGroupIdResponse(Long.parseLong(sfVagId));
    }

    @Override
    public ApiSolidFireVolumeIscsiNameResponse getSolidFireVolumeIscsiName(Volume volume) {
        return new ApiSolidFireVolumeIscsiNameResponse(volume.get_iScsiName());
    }


    @Override
    public boolean checkAccess(User user, String apiCommandName) throws PermissionDeniedException {
        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();

        cmdList.add(GetSolidFireAccountIdCmd.class);
        cmdList.add(GetSolidFireVolumeSizeCmd.class);
        cmdList.add(GetSolidFireVolumeAccessGroupIdCmd.class);
        cmdList.add(GetSolidFireVolumeIscsiNameCmd.class);

        return cmdList;
    }
}
