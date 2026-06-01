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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.cloudian.client.CloudianClient;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.datastore.util.CloudianHyperStoreUtil;
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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.s3.AmazonS3;
import com.cloud.utils.exception.CloudRuntimeException;


@RunWith(MockitoJUnitRunner.class)
public class CloudianHyperStoreObjectStoreLifeCycleImplTest {

    @Spy
    CloudianHyperStoreObjectStoreLifeCycleImpl cloudianHyperStoreObjectStoreLifeCycleImpl = new CloudianHyperStoreObjectStoreLifeCycleImpl();

    @Mock
    CloudianClient cloudianClient;
    @Mock
    AmazonS3 s3Client;
    @Mock
    AmazonIdentityManagement iamClient;
    @Mock
    ObjectStoreHelper objectStoreHelper;
    @Mock
    ObjectStoreProviderManager objectStoreMgr;
    @Mock
    ObjectStoreVO objectStoreVo;
    @Mock
    ObjectStoreEntity objectStoreEntity;

    static String TEST_STORE_NAME = "testStore";
    static String TEST_ADMIN_URL = "https://admin-service:19443";
    static String TEST_PROVIDER_NAME = "Cloudian HyperStore";
    static String TEST_ADMIN_USERNAME = "test_admin";
    static String TEST_ADMIN_PASSWORD = "test_pass";
    static String TEST_VALIDATE_SSL = "false";
    static String TEST_S3_URL = "https://s3-endpoint";
    static String TEST_IAM_URL = "https://iam-endpoint";

    Map<String, String> guiDetailMap;
    Map<String, Object> guiDataStoreMap;

    MockedStatic<CloudianHyperStoreUtil> mockStatic;

    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);

        mockStatic = Mockito.mockStatic(CloudianHyperStoreUtil.class);

        cloudianHyperStoreObjectStoreLifeCycleImpl.objectStoreHelper = objectStoreHelper;
        cloudianHyperStoreObjectStoreLifeCycleImpl.objectStoreMgr = objectStoreMgr;

        guiDetailMap = new HashMap<String, String>();
        guiDetailMap.put("accesskey", TEST_ADMIN_USERNAME);
        guiDetailMap.put("secretkey", TEST_ADMIN_PASSWORD);
        guiDetailMap.put("validateSSL", TEST_VALIDATE_SSL);
        guiDetailMap.put("s3Url", TEST_S3_URL);
        guiDetailMap.put("iamUrl", TEST_IAM_URL);
        guiDataStoreMap = new HashMap<String, Object>();
        guiDataStoreMap.put("name", TEST_STORE_NAME);
        guiDataStoreMap.put("url", TEST_ADMIN_URL);
        guiDataStoreMap.put("providerName", TEST_PROVIDER_NAME);
        guiDataStoreMap.put("details", guiDetailMap);
    }

    @After
    public void tearDown() throws Exception {
        mockStatic.close();
        closeable.close();
    }

    @Test
    public void testInitializeValidation() {
        mockStatic.when(() -> CloudianHyperStoreUtil.getCloudianClient(anyString(), anyString(), anyString(), anyBoolean())).thenReturn(cloudianClient);
        mockStatic.when(() -> CloudianHyperStoreUtil.getS3Client(anyString(), anyString(), anyString())).thenReturn(s3Client);
        mockStatic.when(() -> CloudianHyperStoreUtil.getIAMClient(anyString(), anyString(), anyString())).thenReturn(iamClient);
        // Ensure real validation methods are called (as everything was mocked). These ones we need.
        mockStatic.when(() -> CloudianHyperStoreUtil.validateS3Url(anyString())).thenCallRealMethod();
        mockStatic.when(() -> CloudianHyperStoreUtil.validateIAMUrl(anyString())).thenCallRealMethod();

        // Admin, S3 and IAM will be invoked to validate the urls/connectivity
        when(cloudianClient.getServerVersion()).thenReturn("Test Version");
        // S3 and IAM validation is done with an unknown key.
        AmazonServiceException ase = new AmazonServiceException("Test Amazon Service Exception");
        ase.setErrorCode("InvalidAccessKeyId");
        when(s3Client.listBuckets()).thenThrow(ase);
        when(iamClient.listAccessKeys()).thenThrow(ase);

        when(objectStoreVo.getId()).thenReturn(99L);
        when(objectStoreHelper.createObjectStore(anyMap(), anyMap())).thenReturn(objectStoreVo);
        when(objectStoreMgr.getObjectStore(anyLong())).thenReturn(objectStoreEntity);

        // Test initialization
        DataStore ds = cloudianHyperStoreObjectStoreLifeCycleImpl.initialize(guiDataStoreMap);
        assertNotNull(ds);

        // Verify everything was called to test the connections
        verify(cloudianClient, times(1)).getServerVersion();
        verify(s3Client, times(1)).listBuckets();
        verify(iamClient, times(1)).listAccessKeys();

        // Validate the store details were propagated correctly.
        ArgumentCaptor<Map<String,Object>> paramsArg = ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
        ArgumentCaptor<Map<String,String>> detailsArg = ArgumentCaptor.forClass((Class<Map<String, String>>) (Class<?>) Map.class);
        verify(objectStoreHelper, times(1)).createObjectStore(paramsArg.capture(), detailsArg.capture());
        Map<String, Object> updatedParams = paramsArg.getValue();
        assertEquals(3, updatedParams.size());
        assertEquals(TEST_STORE_NAME, updatedParams.get(CloudianHyperStoreUtil.STORE_KEY_NAME));
        assertEquals(TEST_ADMIN_URL, updatedParams.get(CloudianHyperStoreUtil.STORE_KEY_URL));
        assertEquals(TEST_PROVIDER_NAME, updatedParams.get(CloudianHyperStoreUtil.STORE_KEY_PROVIDER_NAME));
        Map<String, String> updatedDetails = detailsArg.getValue();
        assertEquals(5, updatedDetails.size());
        assertEquals(TEST_ADMIN_USERNAME, updatedDetails.get(CloudianHyperStoreUtil.STORE_DETAILS_KEY_USER_NAME));
        assertEquals(TEST_ADMIN_PASSWORD, updatedDetails.get(CloudianHyperStoreUtil.STORE_DETAILS_KEY_PASSWORD));
        assertEquals(TEST_VALIDATE_SSL, updatedDetails.get(CloudianHyperStoreUtil.STORE_DETAILS_KEY_VALIDATE_SSL));
        assertEquals(TEST_S3_URL, updatedDetails.get(CloudianHyperStoreUtil.STORE_DETAILS_KEY_S3_URL));
        assertEquals(TEST_IAM_URL, updatedDetails.get(CloudianHyperStoreUtil.STORE_DETAILS_KEY_IAM_URL));
    }

    @Test
    public void testInitializeEmptyMap() {
        // Pass an empty configuration map. No URL, name, details etc.
        guiDataStoreMap.clear();

        // Test initialization - should complain about providerName not matching
        CloudRuntimeException thrown = assertThrows(CloudRuntimeException.class, () -> cloudianHyperStoreObjectStoreLifeCycleImpl.initialize(guiDataStoreMap));
        assertTrue(thrown.getMessage().contains("providerName"));
    }

    @Test
    public void testInitializeUnexpectedProviderName() {
        // Use a bad provider name
        guiDataStoreMap.replace("providerName", "bad provider name");

        // Test initialization - should complain about providerName not matching
        CloudRuntimeException thrown = assertThrows(CloudRuntimeException.class, () -> cloudianHyperStoreObjectStoreLifeCycleImpl.initialize(guiDataStoreMap));
        assertTrue(thrown.getMessage().contains("Unexpected providerName"));
    }

    @Test
    public void testInitializeMissingDetails() {
        // Don't pass in the details map
        guiDataStoreMap.remove("details");

        // Test initialization - should complain about details
        CloudRuntimeException thrown = assertThrows(CloudRuntimeException.class, () -> cloudianHyperStoreObjectStoreLifeCycleImpl.initialize(guiDataStoreMap));
        assertTrue(thrown.getMessage().contains("details"));
    }

    @Test
    public void testInitializeBadURL() {
        // Admin connectivity is done first. As everything in Util is mocked, this time we use real implementation.
        mockStatic.when(() -> CloudianHyperStoreUtil.getCloudianClient(anyString(), anyString(), anyString(), anyBoolean())).thenCallRealMethod();

        // Override the URL for this test
        guiDataStoreMap.put("url", "bad_url");

        // Test initialization
        CloudRuntimeException thrown = assertThrows(CloudRuntimeException.class, () -> cloudianHyperStoreObjectStoreLifeCycleImpl.initialize(guiDataStoreMap));
        assertEquals(MalformedURLException.class, thrown.getCause().getClass());
    }

    @Test
    public void testInitializeBadCredentials() {
        // Admin connectivity is done first.
        mockStatic.when(() -> CloudianHyperStoreUtil.getCloudianClient(anyString(), anyString(), anyString(), anyBoolean())).thenReturn(cloudianClient);
        ServerApiException sae = new ServerApiException(ApiErrorCode.UNAUTHORIZED, "bad credentials");
        when(cloudianClient.getServerVersion()).thenThrow(sae);

        // Test initialization
        ServerApiException thrown = assertThrows(ServerApiException.class, () -> cloudianHyperStoreObjectStoreLifeCycleImpl.initialize(guiDataStoreMap));
        assertEquals(ApiErrorCode.UNAUTHORIZED, thrown.getErrorCode());
        verify(cloudianClient, times(1)).getServerVersion();
    }
}
