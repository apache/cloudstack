package org.apache.cloudstack.storage.lifecycle;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreEndPoint;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreEndPointSelector;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.alert.AlertManager;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;

public class DefaultPrimaryDataStoreLifeCycle implements DataStoreLifeCycle {
	 private static final Logger s_logger = Logger.getLogger(DataStoreLifeCycle.class);
	private DataStore _ds;
	@Inject
	StoragePoolDao _storagePoolDao;
	@Inject
	StoragePoolHostDao _poolHostDao;
	public DefaultPrimaryDataStoreLifeCycle(DataStore ds) {
		this._ds = ds;
	}
	

	protected boolean createStoragePool(DataStoreEndPoint ep, StoragePoolVO pool) {
		DataStoreDriver dsDriver = _ds.getDataStoreDriver();
		CreateStoragePoolCommand cmd = new CreateStoragePoolCommand(true, pool);
		final Answer answer = dsDriver.sendMessage(ep, cmd);
		if (answer != null && answer.getResult()) {
			return true;
		} else {
			throw new CloudRuntimeException(answer.getDetails());
		}
	}
	
	 protected void connectHostToSharedPool(DataStoreEndPoint ep, StoragePoolVO pool) throws StorageUnavailableException {
		 DataStoreDriver dsDriver = _ds.getDataStoreDriver();
		 long hostId = ep.getHostId();
		 ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(true, pool);
		 final Answer answer = dsDriver.sendMessage(ep, cmd);

		 if (answer == null) {
			 throw new StorageUnavailableException("Unable to get an answer to the modify storage pool command", pool.getId());
		 }

		 if (!answer.getResult()) {
			 throw new StorageUnavailableException("Unable establish connection from storage head to storage pool " + pool.getId() + " due to " + answer.getDetails(), pool.getId());
		 }

		 assert (answer instanceof ModifyStoragePoolAnswer) : "Well, now why won't you actually return the ModifyStoragePoolAnswer when it's ModifyStoragePoolCommand? Pool=" + pool.getId();
		 ModifyStoragePoolAnswer mspAnswer = (ModifyStoragePoolAnswer) answer;

		 StoragePoolHostVO poolHost = _poolHostDao.findByPoolHost(pool.getId(), hostId);
		 if (poolHost == null) {
			 poolHost = new StoragePoolHostVO(pool.getId(), hostId, mspAnswer.getPoolInfo().getLocalPath().replaceAll("//", "/"));
			 _poolHostDao.persist(poolHost);
		 } else {
			 poolHost.setLocalPath(mspAnswer.getPoolInfo().getLocalPath().replaceAll("//", "/"));
		 }
		 pool.setAvailableBytes(mspAnswer.getPoolInfo().getAvailableBytes());
		 pool.setCapacityBytes(mspAnswer.getPoolInfo().getCapacityBytes());
		 _storagePoolDao.update(pool.getId(), pool);
	 }

	 public void add() {
		 DataStoreEndPointSelector dseps = _ds.getEndPointSelector();
		 List<DataStoreEndPoint> dsep = dseps.getEndPoints();
		 boolean success = false;
		 StoragePoolVO spool = _storagePoolDao.findById(_ds.getId());
		 for (DataStoreEndPoint ep : dsep) {
			 success = createStoragePool(ep, spool);
			 if (success) {
				 break;
			 }
		 }

		 List<DataStoreEndPoint> poolHosts = new ArrayList<DataStoreEndPoint>();
		 for (DataStoreEndPoint ep : dsep) {
			 try {
				 connectHostToSharedPool(ep, spool);
				 poolHosts.add(ep);
			 } catch (Exception e) {
				 s_logger.debug("Failed to add storage on this ep: " + ep.getHostId());
			 }
		 }
	}

	public void delete() {
		// TODO Auto-generated method stub

	}

	public void enable() {
		// TODO Auto-generated method stub

	}

	public void disable() {
		// TODO Auto-generated method stub

	}

	public void processEvent(DataStoreEvent event, Object... objs) {
		// TODO Auto-generated method stub

	}

}
