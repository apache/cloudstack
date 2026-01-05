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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.backup.BackupOfferingDetailsVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class BackupOfferingDetailsDaoImplTest {

    @Spy
    @InjectMocks
    private BackupOfferingDetailsDaoImpl backupOfferingDetailsDao;

    private static final long RESOURCE_ID = 1L;
    private static final long OFFERING_ID = 100L;
    private static final String TEST_KEY = "testKey";
    private static final String TEST_VALUE = "testValue";

    @Test
    public void testAddDetail() {
        BackupOfferingDetailsVO detailVO = new BackupOfferingDetailsVO(RESOURCE_ID, TEST_KEY, TEST_VALUE, true);

        Assert.assertEquals("Resource ID should match", RESOURCE_ID, detailVO.getResourceId());
        Assert.assertEquals("Detail name/key should match", TEST_KEY, detailVO.getName());
        Assert.assertEquals("Detail value should match", TEST_VALUE, detailVO.getValue());
        Assert.assertTrue("Display flag should be true", detailVO.isDisplay());

        BackupOfferingDetailsVO detailVOHidden = new BackupOfferingDetailsVO(RESOURCE_ID, "hiddenKey", "hiddenValue", false);
        Assert.assertFalse("Display flag should be false", detailVOHidden.isDisplay());
    }

    @Test
    public void testFindDomainIdsWithMultipleDomains() {
        List<BackupOfferingDetailsVO> mockDetails = Arrays.asList(
                createDetailVO(RESOURCE_ID, ApiConstants.DOMAIN_ID, "1", false),
                createDetailVO(RESOURCE_ID, ApiConstants.DOMAIN_ID, "2", false),
                createDetailVO(RESOURCE_ID, ApiConstants.DOMAIN_ID, "3", false)
        );

        Mockito.doReturn(mockDetails).when(backupOfferingDetailsDao)
                .findDetails(RESOURCE_ID, ApiConstants.DOMAIN_ID);

        List<Long> domainIds = backupOfferingDetailsDao.findDomainIds(RESOURCE_ID);

        Assert.assertNotNull(domainIds);
        Assert.assertEquals(3, domainIds.size());
        Assert.assertEquals(Arrays.asList(1L, 2L, 3L), domainIds);
    }

    @Test
    public void testFindDomainIdsWithEmptyList() {
        Mockito.doReturn(Collections.emptyList()).when(backupOfferingDetailsDao)
                .findDetails(RESOURCE_ID, ApiConstants.DOMAIN_ID);

        List<Long> domainIds = backupOfferingDetailsDao.findDomainIds(RESOURCE_ID);

        Assert.assertNotNull(domainIds);
        Assert.assertTrue(domainIds.isEmpty());
    }

    @Test
    public void testFindDomainIdsExcludesZeroOrNegativeValues() {
        List<BackupOfferingDetailsVO> mockDetails = Arrays.asList(
                createDetailVO(RESOURCE_ID, ApiConstants.DOMAIN_ID, "1", false),
                createDetailVO(RESOURCE_ID, ApiConstants.DOMAIN_ID, "0", false),
                createDetailVO(RESOURCE_ID, ApiConstants.DOMAIN_ID, "-1", false),
                createDetailVO(RESOURCE_ID, ApiConstants.DOMAIN_ID, "2", false)
        );

        Mockito.doReturn(mockDetails).when(backupOfferingDetailsDao)
                .findDetails(RESOURCE_ID, ApiConstants.DOMAIN_ID);

        List<Long> domainIds = backupOfferingDetailsDao.findDomainIds(RESOURCE_ID);

        Assert.assertNotNull(domainIds);
        Assert.assertEquals(2, domainIds.size());
        Assert.assertEquals(Arrays.asList(1L, 2L), domainIds);
    }

    @Test
    public void testFindZoneIdsWithMultipleZones() {
        List<BackupOfferingDetailsVO> mockDetails = Arrays.asList(
                createDetailVO(RESOURCE_ID, ApiConstants.ZONE_ID, "10", false),
                createDetailVO(RESOURCE_ID, ApiConstants.ZONE_ID, "20", false),
                createDetailVO(RESOURCE_ID, ApiConstants.ZONE_ID, "30", false)
        );

        Mockito.doReturn(mockDetails).when(backupOfferingDetailsDao)
                .findDetails(RESOURCE_ID, ApiConstants.ZONE_ID);

        List<Long> zoneIds = backupOfferingDetailsDao.findZoneIds(RESOURCE_ID);

        Assert.assertNotNull(zoneIds);
        Assert.assertEquals(3, zoneIds.size());
        Assert.assertEquals(Arrays.asList(10L, 20L, 30L), zoneIds);
    }

    @Test
    public void testFindZoneIdsWithEmptyList() {
        Mockito.doReturn(Collections.emptyList()).when(backupOfferingDetailsDao)
                .findDetails(RESOURCE_ID, ApiConstants.ZONE_ID);

        List<Long> zoneIds = backupOfferingDetailsDao.findZoneIds(RESOURCE_ID);

        Assert.assertNotNull(zoneIds);
        Assert.assertTrue(zoneIds.isEmpty());
    }

    @Test
    public void testFindZoneIdsExcludesZeroOrNegativeValues() {
        List<BackupOfferingDetailsVO> mockDetails = Arrays.asList(
                createDetailVO(RESOURCE_ID, ApiConstants.ZONE_ID, "10", false),
                createDetailVO(RESOURCE_ID, ApiConstants.ZONE_ID, "0", false),
                createDetailVO(RESOURCE_ID, ApiConstants.ZONE_ID, "-5", false),
                createDetailVO(RESOURCE_ID, ApiConstants.ZONE_ID, "20", false)
        );

        Mockito.doReturn(mockDetails).when(backupOfferingDetailsDao)
                .findDetails(RESOURCE_ID, ApiConstants.ZONE_ID);

        List<Long> zoneIds = backupOfferingDetailsDao.findZoneIds(RESOURCE_ID);

        Assert.assertNotNull(zoneIds);
        Assert.assertEquals(2, zoneIds.size());
        Assert.assertEquals(Arrays.asList(10L, 20L), zoneIds);
    }

    @Test
    public void testGetDetailWhenDetailExists() {
        BackupOfferingDetailsVO mockDetail = createDetailVO(OFFERING_ID, TEST_KEY, TEST_VALUE, true);

        Mockito.doReturn(mockDetail).when(backupOfferingDetailsDao)
                .findDetail(OFFERING_ID, TEST_KEY);

        String detailValue = backupOfferingDetailsDao.getDetail(OFFERING_ID, TEST_KEY);

        Assert.assertNotNull(detailValue);
        Assert.assertEquals(TEST_VALUE, detailValue);
    }

    @Test
    public void testGetDetailWhenDetailDoesNotExist() {
        Mockito.doReturn(null).when(backupOfferingDetailsDao)
                .findDetail(OFFERING_ID, TEST_KEY);

        String detailValue = backupOfferingDetailsDao.getDetail(OFFERING_ID, TEST_KEY);

        Assert.assertNull(detailValue);
    }

    @Test
    public void testFindOfferingIdsByDomainIds() {
        List<Long> domainIds = Arrays.asList(1L, 2L, 3L);
        List<Long> expectedOfferingIds = Arrays.asList(100L, 101L, 102L);

        Mockito.doReturn(expectedOfferingIds).when(backupOfferingDetailsDao)
                .findResourceIdsByNameAndValueIn(Mockito.eq("domainid"), Mockito.any(Object[].class));

        List<Long> offeringIds = backupOfferingDetailsDao.findOfferingIdsByDomainIds(domainIds);

        Assert.assertNotNull(offeringIds);
        Assert.assertEquals(expectedOfferingIds, offeringIds);
        Mockito.verify(backupOfferingDetailsDao).findResourceIdsByNameAndValueIn(
                Mockito.eq("domainid"), Mockito.any(Object[].class));
    }

    @Test
    public void testFindOfferingIdsByDomainIdsWithEmptyList() {
        List<Long> domainIds = Collections.emptyList();
        List<Long> expectedOfferingIds = Collections.emptyList();

        Mockito.doReturn(expectedOfferingIds).when(backupOfferingDetailsDao)
                .findResourceIdsByNameAndValueIn(Mockito.eq("domainid"), Mockito.any(Object[].class));

        List<Long> offeringIds = backupOfferingDetailsDao.findOfferingIdsByDomainIds(domainIds);

        Assert.assertNotNull(offeringIds);
        Assert.assertTrue(offeringIds.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateBackupOfferingDomainIdsDetail() {
        List<Long> newDomainIds = Arrays.asList(1L, 2L, 3L);

        Mockito.doReturn(0).when(backupOfferingDetailsDao).remove(Mockito.any(SearchCriteria.class));
        Mockito.doReturn(null).when(backupOfferingDetailsDao).persist(Mockito.any(BackupOfferingDetailsVO.class));

        backupOfferingDetailsDao.updateBackupOfferingDomainIdsDetail(OFFERING_ID, newDomainIds);

        Mockito.verify(backupOfferingDetailsDao, Mockito.times(3)).persist(Mockito.any(BackupOfferingDetailsVO.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateBackupOfferingDomainIdsDetailWithEmptyList() {
        List<Long> emptyDomainIds = Collections.emptyList();

        Mockito.doReturn(0).when(backupOfferingDetailsDao).remove(Mockito.any(SearchCriteria.class));

        backupOfferingDetailsDao.updateBackupOfferingDomainIdsDetail(OFFERING_ID, emptyDomainIds);

        Mockito.verify(backupOfferingDetailsDao, Mockito.never()).persist(Mockito.any(BackupOfferingDetailsVO.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateBackupOfferingDomainIdsDetailWithSingleDomain() {
        List<Long> singleDomainId = Collections.singletonList(5L);

        Mockito.doReturn(0).when(backupOfferingDetailsDao).remove(Mockito.any(SearchCriteria.class));
        Mockito.doReturn(null).when(backupOfferingDetailsDao).persist(Mockito.any(BackupOfferingDetailsVO.class));

        backupOfferingDetailsDao.updateBackupOfferingDomainIdsDetail(OFFERING_ID, singleDomainId);

        Mockito.verify(backupOfferingDetailsDao, Mockito.times(1)).persist(Mockito.any(BackupOfferingDetailsVO.class));
    }

    private BackupOfferingDetailsVO createDetailVO(long resourceId, String name, String value, boolean display) {
        return new BackupOfferingDetailsVO(resourceId, name, value, display);
    }
}
