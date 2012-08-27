package org.apache.cloudstack.storage;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreConfigurator;
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
	protected DataCenterDao _dcDao;
	@Inject
	protected ClusterDao _clusterDao;
	@Inject
	protected StoragePoolDao _storagePoolDao;
	
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
	
	public DataStore createDataStore(HypervisorType hypervisor, 
			long dcId,
			long podId,
			long clusterId,
			String name,
			String url,
			Map<String, String> extra) {
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

		Map<String, DataStoreConfigurator> dscs = _supportedProtocols.get(hypervisor);
		if (dscs.isEmpty()) {
			throw new InvalidParameterValueException("Doesn't support this hypervisor");
		}

		DataStoreConfigurator dsc = dscs.get(protocol);
		if (dsc == null) {
			throw new InvalidParameterValueException("Doesn't support this protocol");
		}
		
		Map<String, String> configs = dsc.getConfigs(uri, extra);
		dsc.validate(configs);
		StoragePoolVO spool = (StoragePoolVO)dsc.getStoragePool(configs);
		DataCenterVO zone = _dcDao.findById(dcId);
		if (zone == null) {
			throw new InvalidParameterValueException("unable to find zone by id " + dcId);
		}
		StoragePoolVO existingPool = _storagePoolDao.findPoolByUUID(spool.getUuid());
		if (existingPool != null) {
			throw new InvalidParameterValueException("The same storage pool was added already");
		}
		
		long poolId = _storagePoolDao.getNextInSequence(Long.class, "id");
        spool.setId(poolId);
        spool.setDataCenterId(dcId);
        spool.setPodId(podId);
        spool.setName(name);
        spool.setClusterId(clusterId);
        spool.setStatus(StoragePoolStatus.Up);
        spool = _storagePoolDao.persist(spool, extra);
		
        DataStore ds = dsc.getDataStore(spool);
        
		return ds;
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

}
