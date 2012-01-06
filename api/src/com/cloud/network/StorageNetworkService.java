package com.cloud.network;

import java.sql.SQLException;
import java.util.List;

import com.cloud.api.commands.CreateStorageNetworkIpRangeCmd;
import com.cloud.api.commands.DeleteStorageNetworkIpRangeCmd;
import com.cloud.api.commands.UpdateStorageNetworkIpRangeCmd;
import com.cloud.api.commands.listStorageNetworkIpRangeCmd;
import com.cloud.dc.StorageNetworkIpRange;

public interface StorageNetworkService {
	StorageNetworkIpRange createIpRange(CreateStorageNetworkIpRangeCmd cmd) throws SQLException;

	void deleteIpRange(DeleteStorageNetworkIpRangeCmd cmd);

	List<StorageNetworkIpRange> listIpRange(listStorageNetworkIpRangeCmd cmd);
	
	StorageNetworkIpRange updateIpRange(UpdateStorageNetworkIpRangeCmd cmd);
}
