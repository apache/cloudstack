package org.apache.cloudstack.storage.volume.disktype;

import org.springframework.stereotype.Component;

@Component
public class VHD extends VolumeDiskTypeBase {
	public VHD() {
		this.type = "VHD";
	}
}
