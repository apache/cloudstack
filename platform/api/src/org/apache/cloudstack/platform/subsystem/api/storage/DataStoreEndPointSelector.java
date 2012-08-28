package org.apache.cloudstack.platform.subsystem.api.storage;

import java.util.List;

public interface DataStoreEndPointSelector {
	List<DataStoreEndPoint> getEndPoints();
}
