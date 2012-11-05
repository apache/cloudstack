package org.apache.cloudstack.engine.subsystem.api.storage;

import java.util.List;

public interface DataStoreEndPointSelector {
	List<DataStoreEndPoint> getEndPoints(StorageEvent event);
}
