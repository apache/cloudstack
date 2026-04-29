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
package org.apache.cloudstack.storage.template;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DownloadManagerImplTest {

    @InjectMocks
    DownloadManagerImpl downloadManager = new DownloadManagerImpl();

    @Test
    public void testGetSnapshotInstallNameFromDownloadUrl() {
        Map<String, String> urlNames = Map.of(
                "http://HOST/copy/SecStorage/e7d75b93-08f3-3488-8089-632c5c3854bf/snapshots/2/8/8d4cd8d8-c66f-4cbe-88ce-0bf99e26fe79.vhd", "8d4cd8d8-c66f-4cbe-88ce-0bf99e26fe79.vhd",
                "http://HOST/copy/SecStorage/24492d16-66a6-34df-84ea-cc335e7d5b4a/snapshots/2/6/a84ee92d-43cf-4151-908d-1e8ea6c43d35", "a84ee92d-43cf-4151-908d-1e8ea6c43d35",
                "http://HOST/copy/SecStorage/0e3ec9a5-e23d-3edc-bc0f-ce6e641e12c3/snapshots/2/28/ce0e1e42-9268-414c-a874-1802d2d7b429/ce0e1e42-9268-414c-a874-1802d2d7b429.vmdk", "ce0e1e42-9268-414c-a874-1802d2d7b429/ce0e1e42-9268-414c-a874-1802d2d7b429.vmdk"
        );
        for (Map.Entry<String, String> entry: urlNames.entrySet()) {
            String url = entry.getKey();
            String filename = entry.getValue();
            String name = downloadManager.getSnapshotInstallNameFromDownloadUrl(url);
            Assert.assertEquals(filename, name);
        }
    }
}
