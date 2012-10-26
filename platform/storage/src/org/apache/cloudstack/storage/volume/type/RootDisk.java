package org.apache.cloudstack.storage.volume.type;

import org.springframework.stereotype.Component;

@Component
public class RootDisk extends VolumeTypeBase {
	public RootDisk() {
		this.type = "Root";
	}
}
