package org.apache.cloudstack.engine.provisioning.test;


import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.DataCenterDao;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;




public class ChildTestConfiguration {
	
	@Bean
	public DataCenterDao dataCenterDao() {
		return Mockito.mock(DataCenterDao.class);
	}

}
