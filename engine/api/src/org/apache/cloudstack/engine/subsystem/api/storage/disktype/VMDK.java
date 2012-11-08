package org.apache.cloudstack.engine.subsystem.api.storage.disktype;

import org.springframework.stereotype.Component;

@Component
public class VMDK extends VolumeDiskTypeBase {
	public VMDK() {
		this.type = "VMDK";
	}
}
