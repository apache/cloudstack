package org.apache.cloudstack.storage.datastore.provider;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.driver.ElastistorPrimaryDataStoreDriver;
import org.apache.cloudstack.storage.datastore.lifecycle.ElastistorPrimaryDataStoreLifeCycle;
import org.apache.cloudstack.storage.datastore.service.ElastistorVolumeServiceImpl;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.alert.AlertManager;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * This is the starting point of the elastistor storage plugin. This bean will
 * be detected by Spring container & initialized. This will be one of the
 * providers available via {@link DataStoreProviderManagerImpl} object.
 * 
 * @author amit.das@cloudbyte.com
 * 
 */
@Component
public class ElastistorPrimaryDataStoreProvider implements PrimaryDataStoreProvider {
	
	 private static final Logger s_logger = Logger.getLogger(DefaultHostListener.class);
	    
	
    private ElastistorPrimaryDataStoreLifeCycle lifecycle;
    private PrimaryDataStoreDriver driver;
    private HypervisorHostListener listener;     
    private ElastistorVolumeServiceImpl elastistorVolumeService;
    private String esmanagementip;      
	private String esapikey;     
    private String esaccountid;    
    private String espoolid;
    private String esdefaultgateway;    
    private String essubnet;
    private String estntinterface; 
   
    @Inject
    AgentManager agentMgr;
    @Inject
    DataStoreManager dataStoreMgr;  
    @Inject
    AlertManager alertMgr;
    @Inject
    StoragePoolHostDao storagePoolHostDao;
    @Inject
    PrimaryDataStoreDao primaryStoreDao;
    

    @Override
    public String getName() {
        return ElastistorUtil.ES_PROVIDER_NAME;    	
    }

    @Override
    public DataStoreLifeCycle getDataStoreLifeCycle() {
        return lifecycle;
    }

    @Override
    public PrimaryDataStoreDriver getDataStoreDriver() {
        return driver;
    }

    @Override
    public HypervisorHostListener getHostListener() {
        return listener;
    }
    

    public ElastistorVolumeServiceImpl getElastistorVolumeService() {
        return elastistorVolumeService;
    }

    @Override
    public boolean configure(Map<String, Object> params) {
    	
        lifecycle = ComponentContext.inject(ElastistorPrimaryDataStoreLifeCycle.class);
        driver = ComponentContext.inject(ElastistorPrimaryDataStoreDriver.class);
        listener = ComponentContext.inject(ElastistorHostListener.class);
        elastistorVolumeService = ComponentContext.inject(ElastistorVolumeServiceImpl.class);
        
        s_logger.info("elastistor_poolid: " + espoolid );
        s_logger.info("elastistor_accountid: " + esaccountid );
        s_logger.info("elastistor_esmanagmentip: " + esmanagementip );
        s_logger.info("elastistor_apikey: " + esapikey );
        s_logger.info("elastistor_subnet: " + essubnet );
        s_logger.info("elastistor_defaultgateway: " + esdefaultgateway );
        s_logger.info("elastistor_interface: " + estntinterface );

        ElastistorUtil.setElastistorAccountId(esaccountid);
        ElastistorUtil.setElastistorApiKey(esapikey);
        ElastistorUtil.setElastistorManagementIp(esmanagementip);
        ElastistorUtil.setElastistorPoolId(espoolid);
        ElastistorUtil.setElastistorGateway(esdefaultgateway);
        ElastistorUtil.setElastistorInterface(estntinterface);
        ElastistorUtil.setElastistorSubnet(essubnet);
        
        ElastistorUtil.setElastistorRestClient(esmanagementip, esapikey);
        
		return true;
    }

    @Override
    public Set<DataStoreProviderType> getTypes() {
        Set<DataStoreProviderType> types = new HashSet<DataStoreProviderType>();

        types.add(DataStoreProviderType.PRIMARY);

        return types;
    }
    public String getEspoolid() {
		return espoolid;
	}


	public void setEspoolid(String espoolid) {
		this.espoolid = espoolid;
	}


	public String getEsmanagementip() {
		return esmanagementip;
	}


	public void setEsmanagementip(String esmanagementip) {
		this.esmanagementip = esmanagementip;
	}


	public String getEsaccountid() {
		return esaccountid;
	}


	public void setEsaccountid(String esaccountid) {
		this.esaccountid = esaccountid;
	}


	public String getEsapikey() {
		return esapikey;
	}


	public void setEsapikey(String esapikey) {
		this.esapikey = esapikey;
	}
	
	public String getesdefaultgateway() {
		return esdefaultgateway;
	}

	public void setesdefaultgateway(String esdefaultgateway) {
		this.esdefaultgateway = esdefaultgateway;
	}
	public String getEssubnet() {
		return essubnet;
	}


	public void setEssubnet(String essubnet) {
		this.essubnet = essubnet;
	}


	public String getEstntinterface() {
		return estntinterface;
	}


	public void setEstntinterface(String estntinterface) {
		this.estntinterface = estntinterface;
	}
}
