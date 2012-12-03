package org.apache.cloudstack.storage.image.store.lifecycle;

import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.storage.image.db.ImageDataStoreDao;
import org.apache.cloudstack.storage.image.db.ImageDataStoreProviderVO;
import org.apache.cloudstack.storage.image.db.ImageDataStoreVO;
import org.apache.cloudstack.storage.image.provider.ImageDataStoreProvider;
import org.apache.cloudstack.storage.image.store.ImageDataStore;

public class DefaultImageDataStoreLifeCycle implements ImageDataStoreLifeCycle {
	protected ImageDataStoreProvider provider;
	protected ImageDataStoreProviderVO providerVO;
	protected ImageDataStoreDao imageStoreDao;
	@Override
	public ImageDataStore registerDataStore(String name,
			Map<String, String> params) {
		ImageDataStoreVO dataStore = imageStoreDao.findByName(name);
		if (dataStore == null) {
			dataStore = new ImageDataStoreVO();
			dataStore.setName(name);
			dataStore.setProvider(providerVO.getId());
			dataStore = imageStoreDao.persist(dataStore);
		}
		return provider.getImageDataStore(dataStore.getId());
	}
	
	public DefaultImageDataStoreLifeCycle(ImageDataStoreProvider provider,
			ImageDataStoreProviderVO providerVO,
			ImageDataStoreDao dao) {
		this.provider = provider;
		this.providerVO = providerVO;
		this.imageStoreDao = dao;
	}

}
