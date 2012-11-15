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
package org.apache.cloudstack.storage.image.provider;

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.storage.image.TemplateObject;
import org.apache.cloudstack.storage.image.db.ImageDataDao;
import org.apache.cloudstack.storage.image.db.ImageDataStoreDao;
import org.apache.cloudstack.storage.image.db.ImageDataStoreProviderDao;
import org.apache.cloudstack.storage.image.db.ImageDataStoreProviderVO;
import org.apache.cloudstack.storage.image.db.ImageDataStoreVO;
import org.apache.cloudstack.storage.image.db.ImageDataVO;
import org.apache.cloudstack.storage.image.store.ImageDataStore;
import org.springframework.stereotype.Component;

@Component
public class ImageDataStoreProviderManagerImpl implements ImageDataStoreProviderManager {
	@Inject
	ImageDataStoreProviderDao providerDao;
	@Inject
	ImageDataStoreDao dataStoreDao;
	@Inject
	ImageDataDao imageDataDao;
	@Inject
	List<ImageDataStoreProvider> providers;
	@Override
	public ImageDataStoreProvider getProvider(long providerId) {
		
		return null;
	}
	
	protected ImageDataStoreProvider getProvider(String name) {
		for (ImageDataStoreProvider provider : providers) {
			if (provider.getName().equalsIgnoreCase(name)) {
				return provider;
			}
		}
		return null;
	}
	
	@Override
	public ImageDataStore getDataStore(long dataStoreId) {
		ImageDataStoreVO idsv = dataStoreDao.findById(dataStoreId);
		long providerId = idsv.getProvider();
		ImageDataStoreProviderVO idspv = providerDao.findById(providerId);
		ImageDataStoreProvider provider = getProvider(idspv.getName());
		return provider.getImageDataStore(dataStoreId);
	}

	@Override
	public ImageDataStore getDataStoreFromTemplateId(long templateId) {
		ImageDataVO iddv = imageDataDao.findById(templateId);
		return getDataStore(iddv.getId());
	}

	@Override
	public TemplateObject getTemplate(long templateId) {
		// TODO Auto-generated method stub
		return null;
	}
}
