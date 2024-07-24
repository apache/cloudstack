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
import java.util.List;

import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FindStoragePoolsForMigrationCmdTest {

    private FindStoragePoolsForMigrationCmd findStoragePoolsForMigrationCmd = new FindStoragePoolsForMigrationCmd();

    @Test
    public void sortPoolsBySuitability() {
        List<StoragePoolResponse> storagePoolsResponse = new ArrayList<>();
        StoragePoolResponse storagePoolResponse1 = new StoragePoolResponse();
        storagePoolResponse1.setSuitableForMigration(true);
        storagePoolResponse1.setId("1");
        storagePoolResponse1.setName("1");

        StoragePoolResponse storagePoolResponse2 = new StoragePoolResponse();
        storagePoolResponse2.setSuitableForMigration(false);
        storagePoolResponse2.setId("2");
        storagePoolResponse2.setName("2");

        StoragePoolResponse storagePoolResponse3 = new StoragePoolResponse();
        storagePoolResponse3.setSuitableForMigration(true);
        storagePoolResponse3.setId("3");
        storagePoolResponse3.setName("3");

        storagePoolsResponse.add(storagePoolResponse3);
        storagePoolsResponse.add(storagePoolResponse2);
        storagePoolsResponse.add(storagePoolResponse1);

        findStoragePoolsForMigrationCmd.sortPoolsBySuitabilityAndName(storagePoolsResponse);

        Assert.assertEquals("1", storagePoolsResponse.get(0).getId());
        Assert.assertEquals("3", storagePoolsResponse.get(1).getId());
        Assert.assertEquals("2", storagePoolsResponse.get(2).getId());

    }

}
