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
// SPDX-License-Identifier: Apache-2.0
package org.apache.cloudstack.storage.datastore.lifecycle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.datastore.util.SeaweedFSObjectStoreUtil;
import org.apache.cloudstack.storage.object.ObjectStoreEntity;
import org.apache.cloudstack.storage.object.datastore.ObjectStoreHelper;
import org.apache.cloudstack.storage.object.datastore.ObjectStoreProviderManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class SeaweedFSObjectStoreLifeCycleImplTest {

    @Spy
    SeaweedFSObjectStoreLifeCycleImpl lifecycle = new SeaweedFSObjectStoreLifeCycleImpl();

    @Mock
    ObjectStoreHelper objectStoreHelper;
    @Mock
    ObjectStoreProviderManager objectStoreMgr;
    @Mock
    ObjectStoreVO objectStoreVo;
    @Mock
    ObjectStoreEntity objectStoreEntity;

    static String TEST_STORE_NAME = "testStore";
    static String TEST_URL = "http://s3-endpoint";
    static String TEST_PROVIDER_NAME = "SeaweedFS";
    static String TEST_ACCESS_KEY = "admin-access-key";
    static String TEST_SECRET_KEY = "admin-secret-key";
    static String TEST_S3_URL_OVERRIDE = "http://s3-override:8333";
    static String TEST_IAM_URL_OVERRIDE = "http://iam-override:8111";
    static String TEST_METRICS_URL = "http://seaweedfs-s3:9327";

    Map<String, String> detailsMap;
    Map<String, Object> dsInfos;

    MockedStatic<SeaweedFSObjectStoreUtil> mockStatic;

    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);

        mockStatic = Mockito.mockStatic(SeaweedFSObjectStoreUtil.class);
        mockStatic.when(() -> SeaweedFSObjectStoreUtil.validateS3Url(org.mockito.ArgumentMatchers.anyString())).thenAnswer(i -> null);
        mockStatic.when(() -> SeaweedFSObjectStoreUtil.validateIAMUrl(org.mockito.ArgumentMatchers.anyString())).thenAnswer(i -> null);
        // hasPathPrefix is a pure function over java.net.URI; run the real
        // implementation so the path-prefix rejection tests exercise the
        // actual validation rather than the mocked default (false).
        mockStatic.when(() -> SeaweedFSObjectStoreUtil.hasPathPrefix(org.mockito.ArgumentMatchers.anyString())).thenCallRealMethod();

        lifecycle.objectStoreHelper = objectStoreHelper;
        lifecycle.objectStoreMgr = objectStoreMgr;

        detailsMap = new HashMap<>();
        detailsMap.put(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_ACCESS_KEY, TEST_ACCESS_KEY);
        detailsMap.put(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_SECRET_KEY, TEST_SECRET_KEY);

        dsInfos = new HashMap<>();
        dsInfos.put(SeaweedFSObjectStoreUtil.STORE_KEY_NAME, TEST_STORE_NAME);
        dsInfos.put(SeaweedFSObjectStoreUtil.STORE_KEY_URL, TEST_URL);
        dsInfos.put(SeaweedFSObjectStoreUtil.STORE_KEY_PROVIDER_NAME, TEST_PROVIDER_NAME);
        dsInfos.put(SeaweedFSObjectStoreUtil.STORE_KEY_SIZE, 0L);
        dsInfos.put(SeaweedFSObjectStoreUtil.STORE_KEY_DETAILS, detailsMap);

        when(objectStoreVo.getId()).thenReturn(1L);
        when(objectStoreHelper.createObjectStore(anyMap(), anyMap())).thenReturn(objectStoreVo);
        when(objectStoreMgr.getObjectStore(1L)).thenReturn(objectStoreEntity);
    }

    @After
    public void tearDown() throws Exception {
        mockStatic.close();
        closeable.close();
    }

    @Test
    public void testInitializeDefaultEndpointsNotPersisted() {
        // No s3Url/iamUrl in details — should default to store URL and NOT persist
        DataStore ds = lifecycle.initialize(dsInfos);
        assertNotNull(ds);

        ArgumentCaptor<Map<String, String>> detailsArg = ArgumentCaptor.forClass((Class<Map<String, String>>) (Class<?>) Map.class);
        verify(objectStoreHelper).createObjectStore(anyMap(), detailsArg.capture());
        Map<String, String> persistedDetails = detailsArg.getValue();
        // Defaulted endpoints must not be persisted so the driver resolves
        // them from the current store URL at runtime.
        assertFalse(persistedDetails.containsKey(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_S3_URL));
        assertFalse(persistedDetails.containsKey(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_IAM_URL));
    }

    @Test
    public void testInitializeExplicitEndpointsPersisted() {
        detailsMap.put(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_S3_URL, TEST_S3_URL_OVERRIDE);
        detailsMap.put(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_IAM_URL, TEST_IAM_URL_OVERRIDE);

        DataStore ds = lifecycle.initialize(dsInfos);
        assertNotNull(ds);

        ArgumentCaptor<Map<String, String>> detailsArg = ArgumentCaptor.forClass((Class<Map<String, String>>) (Class<?>) Map.class);
        verify(objectStoreHelper).createObjectStore(anyMap(), detailsArg.capture());
        Map<String, String> persistedDetails = detailsArg.getValue();
        assertEquals(TEST_S3_URL_OVERRIDE, persistedDetails.get(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_S3_URL));
        assertEquals(TEST_IAM_URL_OVERRIDE, persistedDetails.get(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_IAM_URL));
    }

    @Test
    public void testInitializeOnlyS3UrlExplicit() {
        // s3Url explicit, iamUrl defaulted — only s3Url should be persisted
        detailsMap.put(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_S3_URL, TEST_S3_URL_OVERRIDE);

        DataStore ds = lifecycle.initialize(dsInfos);
        assertNotNull(ds);

        ArgumentCaptor<Map<String, String>> detailsArg = ArgumentCaptor.forClass((Class<Map<String, String>>) (Class<?>) Map.class);
        verify(objectStoreHelper).createObjectStore(anyMap(), detailsArg.capture());
        Map<String, String> persistedDetails = detailsArg.getValue();
        assertEquals(TEST_S3_URL_OVERRIDE, persistedDetails.get(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_S3_URL));
        assertFalse(persistedDetails.containsKey(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_IAM_URL));
    }

    @Test
    public void testInitializeMetricsUrlPersisted() {
        detailsMap.put(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_METRICS_URL, TEST_METRICS_URL);

        DataStore ds = lifecycle.initialize(dsInfos);
        assertNotNull(ds);

        ArgumentCaptor<Map<String, String>> detailsArg = ArgumentCaptor.forClass((Class<Map<String, String>>) (Class<?>) Map.class);
        verify(objectStoreHelper).createObjectStore(anyMap(), detailsArg.capture());
        // The metrics endpoint must survive initialization, otherwise
        // getMetricsUrl() returns null and usage reporting always falls back to
        // the O(total objects) ListObjectsV2 scan.
        assertEquals(TEST_METRICS_URL, detailsArg.getValue().get(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_METRICS_URL));
    }

    @Test
    public void testInitializeMetricsUrlOmittedNotPersisted() {
        DataStore ds = lifecycle.initialize(dsInfos);
        assertNotNull(ds);

        ArgumentCaptor<Map<String, String>> detailsArg = ArgumentCaptor.forClass((Class<Map<String, String>>) (Class<?>) Map.class);
        verify(objectStoreHelper).createObjectStore(anyMap(), detailsArg.capture());
        assertFalse(detailsArg.getValue().containsKey(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_METRICS_URL));
    }

    @Test
    public void testInitializeValidatesSuppliedCredentials() {
        DataStore ds = lifecycle.initialize(dsInfos);
        assertNotNull(ds);

        // validateS3Url/validateIAMUrl only probe that the endpoint behaves
        // like the service (using deliberately bad credentials), so the
        // configured admin credentials must be verified separately or a store
        // with bad credentials is accepted and only fails later.
        mockStatic.verify(() -> SeaweedFSObjectStoreUtil.validateCredentials(
                TEST_URL, TEST_URL, TEST_ACCESS_KEY, TEST_SECRET_KEY));
    }

    @Test
    public void testInitializeMissingCredentials() {
        detailsMap.remove(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_ACCESS_KEY);
        detailsMap.remove(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_SECRET_KEY);

        CloudRuntimeException thrown = assertThrows(CloudRuntimeException.class, () -> lifecycle.initialize(dsInfos));
        assertTrue(thrown.getMessage().contains("missing"));
    }

    @Test
    public void testInitializeUnexpectedProviderName() {
        dsInfos.put(SeaweedFSObjectStoreUtil.STORE_KEY_PROVIDER_NAME, "bad provider");

        CloudRuntimeException thrown = assertThrows(CloudRuntimeException.class, () -> lifecycle.initialize(dsInfos));
        assertTrue(thrown.getMessage().contains("Unexpected providerName"));
    }

    @Test
    public void testInitializeMissingDetails() {
        dsInfos.remove(SeaweedFSObjectStoreUtil.STORE_KEY_DETAILS);

        CloudRuntimeException thrown = assertThrows(CloudRuntimeException.class, () -> lifecycle.initialize(dsInfos));
        assertTrue(thrown.getMessage().contains("details"));
    }

    @Test
    public void testInitializeRejectsPathPrefixedS3Url() {
        // The object-store browser builds its MinIO client from only the host
        // and port of the stored bucket URL and drops any path prefix, so a
        // path-prefixed s3Url (e.g. behind a reverse proxy at /s3) would work
        // server-side but break the browser. Reject it at registration.
        detailsMap.put(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_S3_URL, "http://s3-endpoint/s3");

        CloudRuntimeException thrown = assertThrows(CloudRuntimeException.class, () -> lifecycle.initialize(dsInfos));
        assertTrue(thrown.getMessage().contains("path prefix"));
    }

    @Test
    public void testInitializeRejectsPathPrefixedStoreUrl() {
        // No explicit s3Url: the store URL itself carries the path prefix, so
        // the same rejection must apply to the resolved S3 endpoint.
        dsInfos.put(SeaweedFSObjectStoreUtil.STORE_KEY_URL, "http://s3-endpoint/s3");

        CloudRuntimeException thrown = assertThrows(CloudRuntimeException.class, () -> lifecycle.initialize(dsInfos));
        assertTrue(thrown.getMessage().contains("path prefix"));
    }

    @Test
    public void testInitializeAcceptsRootOnlyS3Url() {
        // A URL with no path (or just "/") must be accepted.
        detailsMap.put(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_S3_URL, "http://s3-endpoint:8333/");

        DataStore ds = lifecycle.initialize(dsInfos);
        assertNotNull(ds);
    }
}
