package org.apache.cloudstack.storage.datastore.provider;

import org.springframework.stereotype.Component;

@Component
public class SolidfirePrimaryDataStoreProvider extends
	DefaultPrimaryDatastoreProviderImpl {
	private final String name = "Solidfre Primary Data Store Provider";


	public SolidfirePrimaryDataStoreProvider() {
	    
		
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	
}
