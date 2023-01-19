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
package org.apache.cloudstack.storage.datastore.util;

import com.linbit.linstor.api.ApiClient;
import com.linbit.linstor.api.ApiException;
import com.linbit.linstor.api.Configuration;
import com.linbit.linstor.api.DevelopersApi;
import com.linbit.linstor.api.model.ProviderKind;
import com.linbit.linstor.api.model.ResourceGroup;
import com.linbit.linstor.api.model.StoragePool;

import java.util.Collections;
import java.util.List;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class LinstorUtil {
    protected static Logger LOGGER = LogManager.getLogger(LinstorUtil.class);

    public static final String RSC_PREFIX = "cs-";
    public static final String RSC_GROUP = "resourceGroup";

    public static final String TEMP_VOLUME_ID = "tempVolumeId";

    public static final String CLUSTER_DEFAULT_MIN_IOPS = "clusterDefaultMinIops";
    public static final String CLUSTER_DEFAULT_MAX_IOPS = "clusterDefaultMaxIops";

    public static DevelopersApi getLinstorAPI(String linstorUrl) {
        ApiClient client = Configuration.getDefaultApiClient();
        client.setBasePath(linstorUrl);
        return new DevelopersApi(client);
    }

    public static long getCapacityBytes(String linstorUrl, String rscGroupName) {
        DevelopersApi linstorApi = getLinstorAPI(linstorUrl);
        try {
            List<ResourceGroup> rscGrps = linstorApi.resourceGroupList(
                Collections.singletonList(rscGroupName),
                null,
                null,
                null);

            if (rscGrps.isEmpty()) {
                final String errMsg = String.format("Linstor: Resource group '%s' not found", rscGroupName);
                LOGGER.error(errMsg);
                throw new CloudRuntimeException(errMsg);
            }

            List<StoragePool> storagePools = linstorApi.viewStoragePools(
                Collections.emptyList(),
                rscGrps.get(0).getSelectFilter().getStoragePoolList(),
                null,
                null,
                null
            );

            return storagePools.stream()
                .filter(sp -> sp.getProviderKind() != ProviderKind.DISKLESS)
                .mapToLong(StoragePool::getTotalCapacity).sum() * 1024;  // linstor uses kiB
        } catch (ApiException apiEx) {
            LOGGER.error(apiEx.getMessage());
            throw new CloudRuntimeException(apiEx);
        }
    }
}
