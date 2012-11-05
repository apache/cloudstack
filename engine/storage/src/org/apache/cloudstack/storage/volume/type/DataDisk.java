package org.apache.cloudstack.storage.volume.type;

import org.springframework.stereotype.Component;

@Component
public class DataDisk extends VolumeTypeBase {
	public DataDisk() {
		this.type = "DataDisk";
	}
}
