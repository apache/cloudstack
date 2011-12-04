package com.cloud.configuration;

import com.cloud.agent.manager.MockAgentManagerImpl;
import com.cloud.agent.manager.MockStorageManagerImpl;
import com.cloud.agent.manager.MockVmManagerImpl;
import com.cloud.agent.manager.SimulatorManagerImpl;
import com.cloud.simulator.dao.MockConfigurationDaoImpl;
import com.cloud.simulator.dao.MockHostDaoImpl;
import com.cloud.simulator.dao.MockSecStorageDaoImpl;
import com.cloud.simulator.dao.MockSecurityRulesDaoImpl;
import com.cloud.simulator.dao.MockStoragePoolDaoImpl;
import com.cloud.simulator.dao.MockVMDaoImpl;
import com.cloud.simulator.dao.MockVolumeDaoImpl;

public class SimulatorComponentLibrary extends PremiumComponentLibrary {
	  @Override
	    protected void populateManagers() {
	        addManager("VM Manager", MockVmManagerImpl.class);
	        addManager("agent manager", MockAgentManagerImpl.class);
	        addManager("storage manager", MockStorageManagerImpl.class);
	        addManager("SimulatorManager", SimulatorManagerImpl.class);
	    }

	    @Override
	    protected void populateDaos() {
	        addDao("mock Host", MockHostDaoImpl.class);
	        addDao("mock secondary storage", MockSecStorageDaoImpl.class);
	        addDao("mock storage pool", MockStoragePoolDaoImpl.class);
	        addDao("mock vm", MockVMDaoImpl.class);
	        addDao("mock volume", MockVolumeDaoImpl.class);
	        addDao("mock config", MockConfigurationDaoImpl.class);
	        addDao("mock security rules", MockSecurityRulesDaoImpl.class);
	    }
}
