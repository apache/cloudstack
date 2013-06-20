/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.cache.manager;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.component.ComponentContext;
import junit.framework.Assert;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/storageContext.xml")
public class StorageCacheReplacementAlgorithmLRUTest {
    @Inject
    VMTemplateDao templateDao;
    @Inject
    ImageStoreDao imageStoreDao;
    @Inject
    TemplateDataStoreDao templateDataStoreDao;
    @Inject
    StorageCacheReplacementAlgorithmLRU cacheReplacementAlgorithm;
    @Inject
    DataStoreManager dataStoreManager;
    @Before
    public void setup() throws Exception {
        ComponentContext.initComponentsLifeCycle();
    }
    @Test
    public void testSelectObject() {
        cacheReplacementAlgorithm.setUnusedTimeInterval(1);
        try {
            VMTemplateVO template = new VMTemplateVO();
            template.setTemplateType(Storage.TemplateType.USER);
            template.setUrl(UUID.randomUUID().toString());
            template.setUniqueName(UUID.randomUUID().toString());
            template.setName(UUID.randomUUID().toString());
            template.setPublicTemplate(true);
            template.setFeatured(true);
            template.setRequiresHvm(true);
            template.setBits(64);
            template.setFormat(Storage.ImageFormat.VHD);
            template.setEnablePassword(true);
            template.setEnableSshKey(true);
            template.setGuestOSId(1);
            template.setBootable(true);
            template.setPrepopulate(true);
            template.setCrossZones(true);
            template.setExtractable(true);
            template = templateDao.persist(template);

            VMTemplateVO template2 = new VMTemplateVO();
            template2.setTemplateType(Storage.TemplateType.USER);
            template2.setUrl(UUID.randomUUID().toString());
            template2.setUniqueName(UUID.randomUUID().toString());
            template2.setName(UUID.randomUUID().toString());
            template2.setPublicTemplate(true);
            template2.setFeatured(true);
            template2.setRequiresHvm(true);
            template2.setBits(64);
            template2.setFormat(Storage.ImageFormat.VHD);
            template2.setEnablePassword(true);
            template2.setEnableSshKey(true);
            template2.setGuestOSId(1);
            template2.setBootable(true);
            template2.setPrepopulate(true);
            template2.setCrossZones(true);
            template2.setExtractable(true);
            template2 = templateDao.persist(template2);

            ImageStoreVO imageStoreVO = new ImageStoreVO();
            imageStoreVO.setRole(DataStoreRole.ImageCache);
            imageStoreVO.setName(UUID.randomUUID().toString());
            imageStoreVO.setProviderName(DataStoreProvider.NFS_IMAGE);
            imageStoreVO.setProtocol("nfs");
            imageStoreVO.setUrl(UUID.randomUUID().toString());
            imageStoreVO = imageStoreDao.persist(imageStoreVO);

            Calendar cal = Calendar.getInstance();
            cal.setTime(DateUtil.now());
            cal.add(Calendar.DAY_OF_MONTH, -2);
            Date date = cal.getTime();

            TemplateDataStoreVO templateStoreVO1 = new TemplateDataStoreVO();
            templateStoreVO1.setLastUpdated(date);
            templateStoreVO1.setDataStoreRole(DataStoreRole.ImageCache);
            templateStoreVO1.setDataStoreId(imageStoreVO.getId());
            templateStoreVO1.setState(ObjectInDataStoreStateMachine.State.Ready);
            templateStoreVO1.setCopy(true);
            templateStoreVO1.setTemplateId(template.getId());
            templateDataStoreDao.persist(templateStoreVO1);

            TemplateDataStoreVO templateStoreVO2 = new TemplateDataStoreVO();
            templateStoreVO2.setLastUpdated(date);
            templateStoreVO2.setDataStoreRole(DataStoreRole.ImageCache);
            templateStoreVO2.setDataStoreId(imageStoreVO.getId());
            templateStoreVO2.setState(ObjectInDataStoreStateMachine.State.Ready);
            templateStoreVO2.setCopy(true);
            templateStoreVO2.setTemplateId(template2.getId());
            templateDataStoreDao.persist(templateStoreVO2);

            DataStore store = dataStoreManager.getDataStore(imageStoreVO.getId(), DataStoreRole.ImageCache);
            Assert.assertNotNull(cacheReplacementAlgorithm.chooseOneToBeReplaced(store));

        } catch (Exception e) {
           Assert.fail();
        }
    }


    @Test
    public void testSelectObjectFailed() {
        cacheReplacementAlgorithm.setUnusedTimeInterval(1);
        try {
            VMTemplateVO template = new VMTemplateVO();
            template.setTemplateType(Storage.TemplateType.USER);
            template.setUrl(UUID.randomUUID().toString());
            template.setUniqueName(UUID.randomUUID().toString());
            template.setName(UUID.randomUUID().toString());
            template.setPublicTemplate(true);
            template.setFeatured(true);
            template.setRequiresHvm(true);
            template.setBits(64);
            template.setFormat(Storage.ImageFormat.VHD);
            template.setEnablePassword(true);
            template.setEnableSshKey(true);
            template.setGuestOSId(1);
            template.setBootable(true);
            template.setPrepopulate(true);
            template.setCrossZones(true);
            template.setExtractable(true);
            template = templateDao.persist(template);

            VMTemplateVO template2 = new VMTemplateVO();
            template2.setTemplateType(Storage.TemplateType.USER);
            template2.setUrl(UUID.randomUUID().toString());
            template2.setUniqueName(UUID.randomUUID().toString());
            template2.setName(UUID.randomUUID().toString());
            template2.setPublicTemplate(true);
            template2.setFeatured(true);
            template2.setRequiresHvm(true);
            template2.setBits(64);
            template2.setFormat(Storage.ImageFormat.VHD);
            template2.setEnablePassword(true);
            template2.setEnableSshKey(true);
            template2.setGuestOSId(1);
            template2.setBootable(true);
            template2.setPrepopulate(true);
            template2.setCrossZones(true);
            template2.setExtractable(true);
            template2 = templateDao.persist(template2);

            ImageStoreVO imageStoreVO = new ImageStoreVO();
            imageStoreVO.setRole(DataStoreRole.ImageCache);
            imageStoreVO.setName(UUID.randomUUID().toString());
            imageStoreVO.setProviderName(DataStoreProvider.NFS_IMAGE);
            imageStoreVO.setProtocol("nfs");
            imageStoreVO.setUrl(UUID.randomUUID().toString());
            imageStoreVO = imageStoreDao.persist(imageStoreVO);


            Date date = DateUtil.now();

            TemplateDataStoreVO templateStoreVO1 = new TemplateDataStoreVO();
            templateStoreVO1.setLastUpdated(date);
            templateStoreVO1.setDataStoreRole(DataStoreRole.ImageCache);
            templateStoreVO1.setDataStoreId(imageStoreVO.getId());
            templateStoreVO1.setState(ObjectInDataStoreStateMachine.State.Ready);
            templateStoreVO1.setCopy(true);
            templateStoreVO1.setTemplateId(template.getId());
            templateDataStoreDao.persist(templateStoreVO1);

            TemplateDataStoreVO templateStoreVO2 = new TemplateDataStoreVO();
            templateStoreVO2.setLastUpdated(date);
            templateStoreVO2.setDataStoreRole(DataStoreRole.ImageCache);
            templateStoreVO2.setDataStoreId(imageStoreVO.getId());
            templateStoreVO2.setState(ObjectInDataStoreStateMachine.State.Ready);
            templateStoreVO2.setCopy(true);
            templateStoreVO2.setTemplateId(template2.getId());
            templateDataStoreDao.persist(templateStoreVO2);

            DataStore store = dataStoreManager.getDataStore(imageStoreVO.getId(), DataStoreRole.ImageCache);
            Assert.assertNull(cacheReplacementAlgorithm.chooseOneToBeReplaced(store));

        } catch (Exception e) {
            Assert.fail();
        }
    }


}
