package org.apache.cloudstack.engine.cloud.entity.api;

import org.apache.cloudstack.engine.cloud.entity.api.db.VMEntityVO;

public interface VMEntityManager {

	VMEntityVO loadVirtualMachine(String vmId);

	void saveVirtualMachine(VMEntityVO vmInstanceVO);
}
