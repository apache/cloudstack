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
package org.apache.cloudstack.backup.dao;

import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.backup.BackupVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class BackupDaoImplTest {
    @Spy
    @InjectMocks
    private BackupDaoImpl backupDao;

    @Mock
    BackupDetailsDao backupDetailsDao;

    @Test
    public void testLoadDetails() {
        Long backupId = 1L;
        BackupVO backup = new BackupVO();
        ReflectionTestUtils.setField(backup, "id", backupId);
        Map<String, String> details = new HashMap<>();
        details.put("key1", "value1");
        details.put("key2", "value2");

        Mockito.when(backupDetailsDao.listDetailsKeyPairs(backupId)).thenReturn(details);

        backupDao.loadDetails(backup);

        Assert.assertEquals(details, backup.getDetails());
        Mockito.verify(backupDetailsDao).listDetailsKeyPairs(backupId);
    }

    @Test
    public void testSaveDetails() {
        Long backupId = 1L;
        BackupVO backup = new BackupVO();
        ReflectionTestUtils.setField(backup, "id", backupId);
        Map<String, String> details = new HashMap<>();
        details.put("key1", "value1");
        details.put("key2", "value2");
        backup.setDetails(details);

        backupDao.saveDetails(backup);

        Mockito.verify(backupDetailsDao).saveDetails(Mockito.anyList());
    }
}
