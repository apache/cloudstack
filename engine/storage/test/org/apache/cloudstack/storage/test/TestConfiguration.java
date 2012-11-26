package org.apache.cloudstack.storage.test;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDaoImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDaoImpl;

@Configuration
public class TestConfiguration {
	@Bean
	public HostDao hostDao() {
		return new HostDaoImpl();
	}
	@Bean
	public PrimaryDataStoreDao primaryDataStoreDao() {
		return new PrimaryDataStoreDaoImpl();
	}
}
