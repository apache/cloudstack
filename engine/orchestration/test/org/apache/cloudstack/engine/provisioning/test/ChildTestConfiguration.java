package org.apache.cloudstack.engine.provisioning.test;


import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.ClusterDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.DataCenterDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.HostDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.HostPodDao;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;




public class ChildTestConfiguration {
	
	@Bean
	public DataCenterDao dataCenterDao() {
		return Mockito.mock(DataCenterDao.class);
	}
	
	@Bean
	public HostPodDao hostPodDao() {
		return Mockito.mock(HostPodDao.class);
	}
	
	@Bean
	public ClusterDao clusterDao() {
		return Mockito.mock(ClusterDao.class);
	}

	@Bean
	public HostDao hostDao() {
		return Mockito.mock(HostDao.class);
	}
}
