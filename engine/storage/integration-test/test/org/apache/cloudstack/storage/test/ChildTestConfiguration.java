package org.apache.cloudstack.storage.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;

import com.cloud.agent.AgentManager;
import com.cloud.host.dao.HostDao;

public class ChildTestConfiguration extends TestConfiguration {
	
	@Override
	@Bean
	public HostDao hostDao() {
		HostDao dao = super.hostDao();
		HostDao nDao = Mockito.spy(dao);
		return nDao;
	}
	
	@Bean
	public AgentManager agentMgr() {
		return Mockito.mock(AgentManager.class);
	}
/*	@Override
	@Bean
	public PrimaryDataStoreDao primaryDataStoreDao() {
		return Mockito.mock(PrimaryDataStoreDaoImpl.class);
	}*/
}
