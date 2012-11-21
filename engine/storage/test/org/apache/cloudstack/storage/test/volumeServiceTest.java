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
package org.apache.cloudstack.storage.test;

import static org.junit.Assert.*;

import java.awt.List;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.cloud.entity.api.TemplateEntity;
import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.QCOW2;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VHD;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VMDK;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskType;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskTypeHelper;
import org.apache.cloudstack.engine.subsystem.api.storage.type.RootDisk;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeTypeHelper;
import org.apache.cloudstack.storage.datastore.DefaultPrimaryDataStoreImpl;
import org.apache.cloudstack.storage.datastore.lifecycle.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.storage.datastore.provider.DefaultPrimaryDatastoreProviderImpl;
import org.apache.cloudstack.storage.datastore.provider.PrimaryDataStoreProvider;
import org.apache.cloudstack.storage.datastore.provider.PrimaryDataStoreProviderManager;
import org.apache.cloudstack.storage.image.ImageService;
import org.apache.cloudstack.storage.image.db.ImageDataDao;
import org.apache.cloudstack.storage.image.db.ImageDataVO;
import org.apache.cloudstack.storage.image.format.ISO;
import org.apache.cloudstack.storage.image.format.ImageFormat;
import org.apache.cloudstack.storage.image.format.ImageFormatHelper;
import org.apache.cloudstack.storage.image.format.OVA;
import org.apache.cloudstack.storage.image.format.Unknown;
import org.apache.cloudstack.storage.image.provider.ImageDataStoreProvider;
import org.apache.cloudstack.storage.image.provider.ImageDataStoreProviderManager;
import org.apache.cloudstack.storage.image.store.ImageDataStore;
import org.apache.cloudstack.storage.volume.VolumeService;
import org.apache.cloudstack.storage.volume.db.VolumeDao;
import org.apache.cloudstack.storage.volume.db.VolumeVO;

import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.mockito.Mockito.*;


import com.cloud.storage.Storage.TemplateType;
import com.cloud.utils.component.ComponentInject;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:/resource/storageContext.xml")
public class volumeServiceTest {
    @Inject
    ImageDataStoreProviderManager imageProviderMgr;
    @Inject
    ImageService imageService;
    @Inject
    VolumeService volumeService;
    @Inject
    ImageDataDao imageDataDao;
    @Inject
    VolumeDao volumeDao;
    @Inject
    PrimaryDataStoreProviderManager primaryDataStoreProviderMgr;
	@Before
	public void setUp() {
		
	}
	
	private ImageDataVO createImageData() {
	    ImageDataVO image = new ImageDataVO();
        image.setTemplateType(TemplateType.USER);
        image.setUrl("http://testurl/test.vhd");
        image.setUniqueName(UUID.randomUUID().toString());
        image.setName(UUID.randomUUID().toString());
        image.setPublicTemplate(true);
        image.setFeatured(true);
        image.setRequireHvm(true);
        image.setBits(64);
        image.setFormat(new VHD().toString());
        image.setAccountId(1);
        image.setEnablePassword(true);
        image.setEnableSshKey(true);
        image.setGuestOSId(1);
        image.setBootable(true);
        image.setPrepopulate(true);
        image.setCrossZones(true);
        image.setExtractable(true);
        image = imageDataDao.persist(image);
        return image;
	}
	
	private TemplateEntity createTemplate() {
	    try {
            imageProviderMgr.configure("image Provider", new HashMap<String, Object>());
            ImageDataVO image = createImageData();
            ImageDataStoreProvider defaultProvider = imageProviderMgr.getProvider("DefaultProvider");
            ImageDataStore store = defaultProvider.registerDataStore("defaultHttpStore", new HashMap<String, String>());
            imageService.registerTemplate(image.getId(), store.getImageDataStoreId());
            TemplateEntity te = imageService.getTemplateEntity(image.getId());
            return te;
        } catch (ConfigurationException e) {
            return null;
        }
	}
	
	@Test
	public void createTemplateTest() {
	    createTemplate();
	}
	
	private PrimaryDataStoreInfo createPrimaryDataStore() {
	    try {
            primaryDataStoreProviderMgr.configure("primary data store mgr", new HashMap<String, Object>());
            PrimaryDataStoreProvider provider = primaryDataStoreProviderMgr.getDataStoreProvider("default primary data store provider");
            PrimaryDataStoreLifeCycle lifeCycle = provider.getDataStoreLifeCycle();
            Map<String, String> params = new HashMap<String, String>();
            params.put("url", "nfs://test/test");
            params.put("dcId", "1");
            params.put("name", "my primary data store");
            PrimaryDataStoreInfo primaryDataStoreInfo = lifeCycle.registerDataStore(params);
            return primaryDataStoreInfo;
        } catch (ConfigurationException e) {
            return null;
        }
	}
	
	@Test
	public void createPrimaryDataStoreTest() {
	    createPrimaryDataStore();
	}
	
	private VolumeVO createVolume(long templateId) {
	    VolumeVO volume = new VolumeVO(1000, new RootDisk().toString(), UUID.randomUUID().toString(), templateId);
	    volume = volumeDao.persist(volume);
	    return volume;
	    
	}
	
	@Test
	public void createVolumeFromTemplate() {
	    TemplateEntity te = createTemplate();
	    PrimaryDataStoreInfo dataStoreInfo = createPrimaryDataStore();
	    VolumeVO volume = createVolume(te.getId());
	    VolumeEntity ve = volumeService.getVolumeEntity(volume.getId());
	    ve.createVolumeFromTemplate(dataStoreInfo.getId(), new VHD(), te);
	}
	
	//@Test
	public void test1() {
		System.out.println(VolumeTypeHelper.getType("Root"));
		System.out.println(VolumeDiskTypeHelper.getDiskType("vmdk"));
		System.out.println(ImageFormatHelper.getFormat("ova"));
		assertFalse(new VMDK().equals(new VHD()));
		VMDK vmdk = new VMDK();
		assertTrue(vmdk.equals(vmdk));
		VMDK newvmdk = new VMDK();
		assertTrue(vmdk.equals(newvmdk));
		
		ImageFormat ova = new OVA();
		ImageFormat iso = new ISO();
		assertTrue(ova.equals(new OVA()));
		assertFalse(ova.equals(iso));
		assertTrue(ImageFormatHelper.getFormat("test").equals(new Unknown()));
		
		VolumeDiskType qcow2 = new QCOW2();
		ImageFormat qcow2format = new org.apache.cloudstack.storage.image.format.QCOW2();
		assertFalse(qcow2.equals(qcow2format));
		
	}
	
	//@Test
	public void testStaticBean() {
		DefaultPrimaryDatastoreProviderImpl provider = ComponentInject.inject(DefaultPrimaryDatastoreProviderImpl.class);
		//assertNotNull(provider.dataStoreDao);
		
		DefaultPrimaryDataStoreImpl dpdsi = new DefaultPrimaryDataStoreImpl(null, null, null);
		ComponentInject.inject(dpdsi);
		//assertNotNull(dpdsi.volumeDao);
	}

}
