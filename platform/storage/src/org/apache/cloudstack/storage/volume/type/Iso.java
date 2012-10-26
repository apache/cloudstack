package org.apache.cloudstack.storage.volume.type;

import org.springframework.stereotype.Component;

@Component
public class Iso extends VolumeTypeBase {
	public Iso() {
		this.type = "iso";
	}
}
