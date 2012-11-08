package org.apache.cloudstack.engine.subsystem.api.storage.disktype;

import org.springframework.stereotype.Component;

@Component
public class VHD extends VolumeDiskTypeBase {
	public VHD() {
		this.type = "VHD";
	}
}
