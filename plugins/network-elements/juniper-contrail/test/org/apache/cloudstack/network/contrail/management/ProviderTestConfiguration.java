package org.apache.cloudstack.network.contrail.management;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProviderTestConfiguration {
	@Bean
	ServerDBSync getServerDBSync() {
		return new ServerDBSyncImpl();
	}
}
