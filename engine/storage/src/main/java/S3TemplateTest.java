// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information regarding copyright ownership.
// The ASF licenses this file to you under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and limitations under the License.

package org.apache.cloudstack.storage.test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.engine.subsystem.api.storage.*;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.storage.LocalHostEndpoint;
import org.apache.cloudstack.storage.MockLocalNfsSecondaryStorageResource;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.image.datastore.ImageStoreHelper;

import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.storage.*;
import com.cloud.utils.component.ComponentContext;

@ContextConfiguration(locations = {"classpath:/storageContext.xml"})
public class S3TemplateTest extends CloudStackTestNGBase {

    private static final Logger logger = LoggerFactory.getLogger(S3TemplateTest.class);

    @Inject
    private ImageStoreDao imageStoreDao;

    @Inject
    private TemplateService templateSvr;

    @Inject
    private VMTemplateDao templateDao;

    @Inject
    private TemplateDataFactory templateFactory;

    @Inject
    private DataStoreManager dataStoreMgr;

    @Inject
    private EndPointSelector epSelector;

    @Inject
    private ImageStoreHelper imageStoreHelper;

    @Inject
    private StorageCacheManager cacheMgr;

    private long dcId;
    private long templateId;

    @Test(priority = -1)
    public void setUp() {
        ComponentContext.initComponentsLifeCycle();

        logger.info("Setting up data center and storage for tests.");

        // Create a data center
        DataCenterVO dc = new DataCenterVO(
                UUID.randomUUID().toString(), "test", "8.8.8.8", null,
                "10.0.0.1", null, "10.0.0.1/24", null, null, NetworkType.Basic,
                null, null, true, true, null, null
        );
        dc = dcDao.persist(dc);
        dcId = dc.getId();

        logger.info("Data center created with ID: {}", dcId);

        // Add S3 image store
        Map<String, Object> sParams = new HashMap<>();
        sParams.put("name", "test");
        sParams.put("protocol", "http");
        sParams.put("providerName", "S3");
        sParams.put("scope", ScopeType.REGION);
        sParams.put("role", DataStoreRole.Image);

        Map<String, String> sDetails = new HashMap<>();
        sDetails.put(ApiConstants.S3_ACCESS_KEY, getS3AccessKey());
        sDetails.put(ApiConstants.S3_SECRET_KEY, getS3SecretKey());
        sDetails.put(ApiConstants.S3_BUCKET_NAME, getS3TemplateBucket());
        sDetails.put(ApiConstants.S3_END_POINT, getS3EndPoint());

        imageStoreHelper.createImageStore(sParams, sDetails);
        logger.info("S3 image store added.");

        // Add NFS cache storage
        Map<String, Object> cParams = new HashMap<>();
        cParams.put("name", "testCache");
        cParams.put("protocol", "nfs");
        cParams.put("providerName", DataStoreProvider.NFS_IMAGE);
        cParams.put("scope", ScopeType.ZONE);
        cParams.put("role", DataStoreRole.ImageCache);
        cParams.put("url", getSecondaryStorage());
        cParams.put("zoneId", dcId);

        imageStoreHelper.createImageStore(cParams);
        logger.info("NFS cache storage added.");

        // Initialize a VM template
        VMTemplateVO image = new VMTemplateVO();
        image.setTemplateType(TemplateType.SYSTEM);
        image.setUrl(getTemplateUrl());
        image.setUniqueName(UUID.randomUUID().toString());
        image.setName(UUID.randomUUID().toString());
        image.setPublicTemplate(false);
        image.setFeatured(false);
        image.setRequiresHvm(false);
        image.setBits(64);
        image.setFormat(Storage.ImageFormat.VHD);
        image.setEnablePassword(false);
        image.setEnableSshKey(false);
        image.setGuestOSId(133);
        image.setBootable(true);
        image.setPrepopulate(true);
        image.setCrossZones(true);
        image.setExtractable(true);
        image.setAccountId(2);

        image = templateDao.persist(image);
        templateId = image.getId();

        logger.info("Template initialized with ID: {}", templateId);

        // Inject Mockito endpoint
        LocalHostEndpoint ep = new LocalHostEndpoint();
        ep.setResource(new MockLocalNfsSecondaryStorageResource());

        Mockito.when(epSelector.select(Matchers.any(DataObject.class))).thenReturn(ep);
        Mockito.when(epSelector.select(Matchers.any(DataStore.class))).thenReturn(ep);
        Mockito.when(epSelector.select(Matchers.any(DataObject.class), Matchers.any(DataObject.class))).thenReturn(ep);
    }

    @Test(priority = 1)
    public void registerTemplate() {
        logger.info("Starting template registration test.");

        TemplateInfo template = templateFactory.getTemplate(templateId, DataStoreRole.Image);
        DataStore store = dataStoreMgr.getImageStore(dcId);
        AsyncCallFuture<TemplateApiResult> future = new AsyncCallFuture<>();

        templateSvr.createTemplateAsync(template, store, future);

        try {
            TemplateApiResult result = future.get();
            assertTrue(result.isSuccess(), "Failed to register template: " + result.getResult());
            logger.info("Template registered successfully.");
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error during template registration", e);
            assertTrue(false, "Exception occurred: " + e.getMessage());
        }
    }

    @Test(priority = 2)
    public void copyTemplateToCache() {
        logger.info("Starting template copy to cache test.");

        TemplateInfo template = templateFactory.getTemplate(templateId, DataStoreRole.Image);
        DataObject cacheObj = cacheMgr.createCacheObject(template, new ZoneScope(dcId));
        assertNotNull(cacheObj, "Failed to create cache object");

        logger.info("Template copied to cache successfully.");
    }

    private String getStorageIpForTest() {
        // Mock implementation to simulate storage IP assignment
        return "10.143.51.196";  // Example IP
    }
}






