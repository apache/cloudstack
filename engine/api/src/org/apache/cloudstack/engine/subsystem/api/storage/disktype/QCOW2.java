package org.apache.cloudstack.engine.subsystem.api.storage.disktype;

import org.springframework.stereotype.Component;

@Component
public class QCOW2 extends VolumeDiskTypeBase {
	public QCOW2() {
		this.type = "QCOW2";
	}
}
