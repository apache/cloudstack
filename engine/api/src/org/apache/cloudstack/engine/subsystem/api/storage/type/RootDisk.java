package org.apache.cloudstack.engine.subsystem.api.storage.type;

import org.springframework.stereotype.Component;

@Component
public class RootDisk extends VolumeTypeBase {
	public RootDisk() {
		this.type = "Root";
	}
}
