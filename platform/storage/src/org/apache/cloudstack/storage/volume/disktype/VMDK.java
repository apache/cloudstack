package org.apache.cloudstack.storage.volume.disktype;

import org.springframework.stereotype.Component;

@Component
public class VMDK extends VolumeDiskTypeBase {
	public VMDK() {
		this.type = "VMDK";
	}
}
