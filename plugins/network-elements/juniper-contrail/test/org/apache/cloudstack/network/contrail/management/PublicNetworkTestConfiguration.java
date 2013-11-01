package org.apache.cloudstack.network.contrail.management;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublicNetworkTestConfiguration {
	@Bean
	ServerDBSync getServerDBSync() {
		return Mockito.mock(ServerDBSync.class);
	}
}
