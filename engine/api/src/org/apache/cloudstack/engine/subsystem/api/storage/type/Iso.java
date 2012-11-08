package org.apache.cloudstack.engine.subsystem.api.storage.type;

import org.springframework.stereotype.Component;

@Component
public class Iso extends VolumeTypeBase {
	public Iso() {
		this.type = "iso";
	}
}
