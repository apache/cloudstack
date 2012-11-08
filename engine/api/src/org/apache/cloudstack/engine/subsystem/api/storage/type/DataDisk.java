package org.apache.cloudstack.engine.subsystem.api.storage.type;

import org.springframework.stereotype.Component;

@Component
public class DataDisk extends VolumeTypeBase {
	public DataDisk() {
		this.type = "DataDisk";
	}
}
