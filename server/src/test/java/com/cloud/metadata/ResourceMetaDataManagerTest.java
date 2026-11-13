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

package com.cloud.metadata;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingDetailsVO;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDetailsDao;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.tags.ResourceManagerUtilImpl;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import org.apache.cloudstack.api.command.user.volume.AddResourceDetailCmd;
import org.apache.cloudstack.api.command.user.volume.RemoveResourceDetailCmd;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import javax.naming.ConfigurationException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ResourceMetaDataManagerTest {

    private static final long RESOURCE_ID = 42L;
    private static final String RESOURCE_UUID = "resource-uuid";
    private static final List<String> PROFILE_KEYS = List.of("NsxIpDiscoveryProfileId", "NsxMacDiscoveryProfileId", "NsxSegmentSecurityProfileId");

    private ResourceMetaDataManagerImpl manager;
    private ResourceManagerUtilImpl resourceManagerUtil;
    private NetworkOfferingDetailsDao offeringDetailsDao;
    private VolumeDetailsDao volumeDetailsDao;
    private MockedStatic<Transaction> transaction;

    @Before
    public void setup() throws ConfigurationException {
        EntityManager entities = mock(EntityManager.class);
        NetworkOfferingVO offering = mock(NetworkOfferingVO.class);
        VolumeVO volume = mock(VolumeVO.class);
        when(offering.getId()).thenReturn(RESOURCE_ID);
        when(volume.getId()).thenReturn(RESOURCE_ID);
        when(entities.findByUuid(NetworkOfferingVO.class, RESOURCE_UUID)).thenReturn(offering);
        when(entities.findByUuid(VolumeVO.class, RESOURCE_UUID)).thenReturn(volume);
        resourceManagerUtil = new ResourceManagerUtilImpl();
        ReflectionTestUtils.setField(resourceManagerUtil, "entityMgr", entities);

        offeringDetailsDao = mock(NetworkOfferingDetailsDao.class);
        volumeDetailsDao = mock(VolumeDetailsDao.class);
        manager = new ResourceMetaDataManagerImpl();
        manager._networkOfferingDetailsDao = offeringDetailsDao;
        manager._volumeDetailDao = volumeDetailsDao;
        manager.resourceManagerUtil = resourceManagerUtil;
        manager.configure("ResourceMetaDataManagerTest", Collections.emptyMap());

        transaction = mockStatic(Transaction.class);
        transaction.when(() -> Transaction.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @After
    public void tearDown() {
        transaction.close();
    }

    @Test
    public void addRejectsProfileDetailsBeforeWritingAnyEntryInTheBatch() {
        for (String key : PROFILE_KEYS) {
            for (boolean display : List.of(false, true)) {
                Map<Integer, Map<String, String>> details = new LinkedHashMap<>();
                details.put(0, Map.of("key", "RelatedNetworkOffering", "value", "related-offering"));
                details.put(1, Map.of("key", key, "value", "profile-A"));
                AddResourceDetailCmd command = addCommand(ResourceObjectType.NetworkOffering, details, display);

                // The real parser must leave a writable entry ahead of the protected one.
                assertEquals("RelatedNetworkOffering", command.getDetails().keySet().iterator().next());
                assertThrows(InvalidParameterValueException.class, command::execute);
            }
        }

        verify(offeringDetailsDao, never()).addDetail(anyLong(), anyString(), anyString(), anyBoolean());
    }

    @Test
    public void addRejectsCaseAliasesAndMalformedProfileValues() {
        List<Map<String, String>> details = List.of(
                Map.of("key", "nsxipdiscoveryprofileid", "value", "profile-A"),
                Map.of("key", "NSXMACDISCOVERYPROFILEID", "value", " "),
                Map.of("key", "nSxSegmentSecurityProfileId", "value", " padded "),
                Map.of("key", "NsxIpDiscoveryProfileId", "value", "x".repeat(256)));
        for (Map<String, String> detail : details) {
            for (boolean display : List.of(false, true)) {
                AddResourceDetailCmd command = addCommand(ResourceObjectType.NetworkOffering, Map.of(0, detail), display);
                assertThrows(InvalidParameterValueException.class, command::execute);
            }
        }

        verify(offeringDetailsDao, never()).addDetail(anyLong(), anyString(), anyString(), anyBoolean());
    }

    @Test
    public void namedRemovalRejectsAllProfileKeysAndCaseAliasesEvenWhenAbsent() {
        for (String key : PROFILE_KEYS) {
            for (String requestedKey : List.of(key, key.toLowerCase(Locale.ROOT), key.toUpperCase(Locale.ROOT))) {
                RemoveResourceDetailCmd command = removeCommand(ResourceObjectType.NetworkOffering, requestedKey);
                assertThrows(InvalidParameterValueException.class, command::execute);
            }
        }

        verifyNoInteractions(offeringDetailsDao);
    }

    @Test
    public void namedRemovalRejectsAnAliasThatTheDatabaseMatchesToAProfile() {
        String alias = "NsxSegmentSecurityProfileId ";
        NetworkOfferingDetailsVO profile = new NetworkOfferingDetailsVO(RESOURCE_ID,
                NetworkOffering.Detail.NsxSegmentSecurityProfileId, "security-profile", false);
        when(offeringDetailsDao.findDetails(RESOURCE_ID, alias)).thenReturn(List.of(profile));
        RemoveResourceDetailCmd command = removeCommand(ResourceObjectType.NetworkOffering, alias);

        assertThrows(InvalidParameterValueException.class, command::execute);

        verify(offeringDetailsDao).findDetails(RESOURCE_ID, alias);
        verify(offeringDetailsDao, never()).removeDetail(anyLong(), anyString());
    }

    @Test
    public void clearAllRejectsDisplayedAndHiddenProfileRows() {
        NetworkOfferingDetailsVO unrelated = new NetworkOfferingDetailsVO(RESOURCE_ID,
                NetworkOffering.Detail.RelatedNetworkOffering, "related-offering", true);
        for (String key : PROFILE_KEYS) {
            for (boolean display : List.of(false, true)) {
                NetworkOfferingDetailsVO profile = new NetworkOfferingDetailsVO(RESOURCE_ID,
                        NetworkOffering.Detail.valueOf(key), "profile-A", display);
                when(offeringDetailsDao.listDetails(RESOURCE_ID)).thenReturn(List.of(unrelated, profile));
                RemoveResourceDetailCmd command = removeCommand(ResourceObjectType.NetworkOffering, null);

                assertThrows(InvalidParameterValueException.class, command::execute);
            }
        }

        verify(offeringDetailsDao, never()).removeDetails(anyLong());
        verify(offeringDetailsDao, never()).removeDetail(anyLong(), anyString());
    }

    @Test
    public void unrelatedOfferingDetailsCanBeAddedAndRemovedWhenProfilesExist() {
        NetworkOfferingDetailsVO profile = new NetworkOfferingDetailsVO(RESOURCE_ID,
                NetworkOffering.Detail.NsxIpDiscoveryProfileId, "ip-profile", true);
        NetworkOfferingDetailsVO unrelated = new NetworkOfferingDetailsVO(RESOURCE_ID,
                NetworkOffering.Detail.RelatedNetworkOffering, "related-offering", true);
        when(offeringDetailsDao.listDetails(RESOURCE_ID)).thenReturn(List.of(profile, unrelated));
        when(offeringDetailsDao.findDetails(RESOURCE_ID, "RelatedNetworkOffering")).thenReturn(List.of(unrelated));

        addCommand(ResourceObjectType.NetworkOffering,
                Map.of(0, Map.of("key", "PromiscuousMode", "value", "false")), true).execute();
        removeCommand(ResourceObjectType.NetworkOffering, "RelatedNetworkOffering").execute();

        verify(offeringDetailsDao).addDetail(RESOURCE_ID, "PromiscuousMode", "false", true);
        verify(offeringDetailsDao).removeDetail(RESOURCE_ID, "RelatedNetworkOffering");
        verify(offeringDetailsDao, never()).removeDetails(anyLong());
    }

    @Test
    public void clearAllStillWorksWhenNoProfileIsStored() {
        NetworkOfferingDetailsVO detail = new NetworkOfferingDetailsVO(RESOURCE_ID,
                NetworkOffering.Detail.PromiscuousMode, "false", false);
        when(offeringDetailsDao.listDetails(RESOURCE_ID)).thenReturn(List.of(detail), Collections.emptyList());

        removeCommand(ResourceObjectType.NetworkOffering, null).execute();
        removeCommand(ResourceObjectType.NetworkOffering, null).execute();

        verify(offeringDetailsDao, times(2)).removeDetails(RESOURCE_ID);
    }

    @Test
    public void theSameDetailNamesRemainMutableOnOtherResourceTypes() {
        Map<Integer, Map<String, String>> details = new LinkedHashMap<>();
        for (String key : PROFILE_KEYS) {
            details.put(details.size(), Map.of("key", key, "value", "volume-value"));
        }

        addCommand(ResourceObjectType.Volume, details, false).execute();
        for (String key : PROFILE_KEYS) {
            removeCommand(ResourceObjectType.Volume, key).execute();
            verify(volumeDetailsDao).addDetail(RESOURCE_ID, key, "volume-value", false);
            verify(volumeDetailsDao).removeDetail(RESOURCE_ID, key);
        }
        removeCommand(ResourceObjectType.Volume, null).execute();

        verify(volumeDetailsDao).removeDetails(RESOURCE_ID);
        verifyNoInteractions(offeringDetailsDao);
    }

    private AddResourceDetailCmd addCommand(ResourceObjectType resourceType, Map<Integer, Map<String, String>> details, boolean display) {
        AddResourceDetailCmd command = new AddResourceDetailCmd();
        command.resourceManagerUtil = resourceManagerUtil;
        command._resourceMetaDataService = manager;
        ReflectionTestUtils.setField(command, "resourceType", resourceType.name());
        ReflectionTestUtils.setField(command, "resourceId", RESOURCE_UUID);
        ReflectionTestUtils.setField(command, "details", details);
        ReflectionTestUtils.setField(command, "display", display);
        return command;
    }

    private RemoveResourceDetailCmd removeCommand(ResourceObjectType resourceType, String key) {
        RemoveResourceDetailCmd command = new RemoveResourceDetailCmd();
        command.resourceManagerUtil = resourceManagerUtil;
        command._resourceMetaDataService = manager;
        ReflectionTestUtils.setField(command, "resourceType", resourceType.name());
        ReflectionTestUtils.setField(command, "resourceId", RESOURCE_UUID);
        ReflectionTestUtils.setField(command, "key", key);
        return command;
    }
}
