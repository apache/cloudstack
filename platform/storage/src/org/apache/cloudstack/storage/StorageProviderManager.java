package org.apache.cloudstack.storage;

import org.apache.cloudstack.platform.subsystem.api.storage.StorageProvider;

public interface StorageProviderManager {
	StorageProvider getProvider(String uuid);
}
