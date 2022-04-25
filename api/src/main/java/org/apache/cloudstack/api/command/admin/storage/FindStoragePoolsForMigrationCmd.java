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
package org.apache.cloudstack.api.command.admin.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.storage.StoragePool;
import com.cloud.utils.Pair;

@APICommand(name = "findStoragePoolsForMigration", description = "Lists storage pools available for migration of a volume.", responseObject = StoragePoolResponse.class,
requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class FindStoragePoolsForMigrationCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(FindStoragePoolsForMigrationCmd.class.getName());

    private static final String s_name = "findstoragepoolsformigrationresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = VolumeResponse.class, required = true, description = "the ID of the volume")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.StoragePool;
    }

    @Override
    public void execute() {
        Pair<List<? extends StoragePool>, List<? extends StoragePool>> pools = _mgr.listStoragePoolsForMigrationOfVolume(getId());
        ListResponse<StoragePoolResponse> response = new ListResponse<StoragePoolResponse>();
        List<StoragePoolResponse> poolResponses = new ArrayList<StoragePoolResponse>();

        List<? extends StoragePool> allPools = pools.first();
        List<? extends StoragePool> suitablePoolList = pools.second();
        for (StoragePool pool : allPools) {
            StoragePoolResponse poolResponse = _responseGenerator.createStoragePoolForMigrationResponse(pool);
            Boolean suitableForMigration = false;
            for (StoragePool suitablePool : suitablePoolList) {
                if (StringUtils.equals(suitablePool.getUuid(), pool.getUuid())) {
                    suitableForMigration = true;
                    break;
                }
            }
            poolResponse.setSuitableForMigration(suitableForMigration);
            poolResponse.setObjectName("storagepool");
            poolResponses.add(poolResponse);
        }
        sortPoolsBySuitabilityAndName(poolResponses);
        response.setResponses(poolResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    protected void sortPoolsBySuitabilityAndName(List<StoragePoolResponse> poolResponses) {
        Collections.sort(poolResponses, new Comparator<StoragePoolResponse>() {
            @Override
            public int compare(StoragePoolResponse o1, StoragePoolResponse o2) {
                if (o1.getSuitableForMigration() && o2.getSuitableForMigration()) {
                    return o1.getName().compareTo(o2.getName());
                }
                if (o1.getSuitableForMigration()) {
                    return -1;
                }
                if (o2.getSuitableForMigration()) {
                    return 1;
                }
                return 0;
            }
        });
    }
}
