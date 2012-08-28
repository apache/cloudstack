package org.apache.cloudstack.storage;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreConfigurator;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.platform.subsystem.api.storage.StorageProvider;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStore.StoreType;
import org.apache.cloudstack.storage.datastoreconfigurator.NfsDataStoreConfigurator;
import org.apache.cloudstack.storage.datastoreconfigurator.XenNfsDataStoreConfigurator;

import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.utils.component.Inject;

public class DefaultPrimaryStorageProvider implements StorageProvider {
	private String _name = DefaultPrimaryStorageProvider.class.toString();
	static Map<HypervisorType, Map<String, DataStoreConfigurator>> _supportedProtocols;
	@Inject
	protected ClusterDao _clusterDao;
	
	public List<HypervisorType> supportedHypervisors() {
		List<HypervisorType> hypervisors = new ArrayList<HypervisorType>();
		hypervisors.add(Hypervisor.HypervisorType.XenServer);
		return hypervisors;
	}
	
	public DefaultPrimaryStorageProvider() {
		Map<String, DataStoreConfigurator> dscs = new HashMap<String, DataStoreConfigurator>();
		DataStoreConfigurator nfsdc = new XenNfsDataStoreConfigurator();
		dscs.put(nfsdc.getProtocol(), nfsdc);
	
		_supportedProtocols.put(HypervisorType.XenServer, dscs);
	}

	public StoreType supportedStoreType() {
		return StoreType.Primary;
	}

	public void configure(Map<String, String> storeProviderInfo) {
		// TODO Auto-generated method stub

	}

	public Map<HypervisorType, Map<String,DataStoreConfigurator>> getDataStoreConfigs() {
		return _supportedProtocols;
	}

	public String getProviderName() {
		return _name;
	}

	public DataStore createDataStore(HypervisorType hypervisor,
			DataStoreConfigurator dsc) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public DataStore getDataStore(StoragePool pool) {
		ClusterVO clu = _clusterDao.findById(pool.getClusterId());
		HypervisorType hy = clu.getHypervisorType();
		Map<String, DataStoreConfigurator> dscs = _supportedProtocols.get(hy);
		DataStoreConfigurator dsc = dscs.get(pool.getPoolType().toString());
		return dsc.getDataStore(pool);
	}

	public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		// TODO Auto-generated method stub
		return false;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean start() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean stop() {
		// TODO Auto-generated method stub
		return false;
	}

	public DataStore addDataStore(StoragePool spool, String url, Map<String, String> params) {
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			throw new InvalidParameterValueException("invalide url" + url);
		}

		String protocol = uri.getScheme();
		if (protocol == null) {
			throw new InvalidParameterValueException("the protocol can't be null");
		}
		
		ClusterVO cluster = _clusterDao.findById(spool.getClusterId());

		Map<String, DataStoreConfigurator> dscs = _supportedProtocols.get(cluster.getHypervisorType());
		if (dscs.isEmpty()) {
			throw new InvalidParameterValueException("Doesn't support this hypervisor");
		}

		DataStoreConfigurator dsc = dscs.get(protocol);
		if (dsc == null) {
			throw new InvalidParameterValueException("Doesn't support this protocol");
		}
		
		Map<String, String> configs = dsc.getConfigs(uri, params);
		dsc.validate(configs);
		DataStore ds = dsc.getDataStore(spool);
		
		return ds;
	}

}
