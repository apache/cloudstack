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
package org.apache.cloudstack.storage.test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageCacheManager;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService.TemplateApiResult;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.storage.LocalHostEndpoint;
import org.apache.cloudstack.storage.MockLocalNfsSecondaryStorageResource;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDetailVO;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.image.datastore.ImageStoreHelper;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.download.DownloadMonitorImpl;
import com.cloud.utils.component.ComponentContext;

@ContextConfiguration(locations = { "classpath:/storageContext.xml" })
public class S3TemplateTest extends CloudStackTestNGBase {
    @Inject
    DataCenterDao dcDao;
    ImageStoreVO imageStore;
    ImageStoreDetailVO imageStoreDetail;
    @Inject
    ImageStoreDao imageStoreDao;
    @Inject
    TemplateService templateSvr;
    @Inject
    VMTemplateDao templateDao;
    @Inject
    TemplateDataFactory templateFactory;
    @Inject
    DataStoreManager dataStoreMgr;
    @Inject
    EndPointSelector epSelector;
    @Inject
    DownloadMonitorImpl downloadMonitor;
    @Inject
    ImageStoreHelper imageStoreHelper;
    @Inject
    StorageCacheManager cacheMgr;
    long dcId;
    long templateId;

    @Test(priority = -1)
    public void setUp() {
        ComponentContext.initComponentsLifeCycle();
        // create data center
        DataCenterVO dc = new DataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null,
                "10.0.0.1/24", null, null, NetworkType.Basic, null, null, true, true, null, null);
        dc = dcDao.persist(dc);
        dcId = dc.getId();

        // add s3 image store
        Map<String, Object> sParams = new HashMap<String, Object>();
        sParams.put("name", "test");
        sParams.put("protocol", "http");
        sParams.put("providerName", "S3");
        sParams.put("scope", ScopeType.REGION);
        sParams.put("role", DataStoreRole.Image);
        Map<String, String> sDetails = new HashMap<String, String>();
        sDetails.put(ApiConstants.S3_ACCESS_KEY, this.getS3AccessKey());
        sDetails.put(ApiConstants.S3_SECRET_KEY, this.getS3SecretKey());
        sDetails.put(ApiConstants.S3_BUCKET_NAME, this.getS3TemplateBucket());
        sDetails.put(ApiConstants.S3_END_POINT, this.getS3EndPoint());
        this.imageStoreHelper.createImageStore(sParams, sDetails);

        // add nfs cache storage
        Map<String, Object> cParams = new HashMap<String, Object>();
        cParams.put("name", "testCache");
        cParams.put("protocol", "nfs");
        cParams.put("providerName", DataStoreProvider.NFS_IMAGE);
        cParams.put("scope", ScopeType.ZONE);
        cParams.put("role", DataStoreRole.ImageCache);
        cParams.put("url", this.getSecondaryStorage());
        cParams.put("zoneId", dcId);
        this.imageStoreHelper.createImageStore(cParams);

        VMTemplateVO image = new VMTemplateVO();
        image.setTemplateType(TemplateType.SYSTEM);
        image.setUrl(this.getTemplateUrl());
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

        // inject mockito
        LocalHostEndpoint ep = new LocalHostEndpoint();
        ep.setResource(new MockLocalNfsSecondaryStorageResource());
        Mockito.when(epSelector.select(Matchers.any(DataObject.class))).thenReturn(ep);
        Mockito.when(epSelector.select(Matchers.any(DataStore.class))).thenReturn(ep);
        Mockito.when(epSelector.select(Matchers.any(DataObject.class), Matchers.any(DataObject.class))).thenReturn(ep);
    }

    @Test(priority = 1)
    public void registerTemplate() {
        TemplateInfo template = templateFactory.getTemplate(templateId, DataStoreRole.Image);
        DataStore store = dataStoreMgr.getImageStore(dcId);
        AsyncCallFuture<TemplateApiResult> future = new AsyncCallFuture<TemplateApiResult>();
        templateSvr.createTemplateAsync(template, store, future);
        try {
            TemplateApiResult result = future.get();
            assertTrue(result.isSuccess(), "failed to register template: " + result.getResult());
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assertTrue(false, e.getMessage());
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assertTrue(false, e.getMessage());
        }
    }

    @Test(priority = 2)
    public void copyTemplateToCache() {
        TemplateInfo template = templateFactory.getTemplate(templateId, DataStoreRole.Image);
        DataObject cacheObj = this.cacheMgr.createCacheObject(template, new ZoneScope(dcId));
        assertNotNull(cacheObj, "failed to create cache object");
    }

}
