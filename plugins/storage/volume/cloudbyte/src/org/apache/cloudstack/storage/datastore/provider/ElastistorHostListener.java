package org.apache.cloudstack.storage.datastore.provider;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.alert.AlertManager;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class ElastistorHostListener implements HypervisorHostListener {
    private static final Logger s_logger = Logger.getLogger(DefaultHostListener.class);
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
    public boolean hostConnect(long hostId, long poolId) {
        StoragePool pool = (StoragePool) this.dataStoreMgr.getDataStore(poolId, DataStoreRole.Primary);
        ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(true, pool);
        final Answer answer = agentMgr.easySend(hostId, cmd);

        if (answer == null) {
            throw new CloudRuntimeException("Unable to get an answer to the modify storage pool command" + pool.getId());
        }

        if (!answer.getResult()) {
            String msg = "Unable to attach storage pool" + poolId + " to the host" + hostId;

            alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST,pool.getDataCenterId(), pool.getPodId(), msg, msg);

            throw new CloudRuntimeException("Unable establish connection from storage head to storage pool " + pool.getId() + " due to " + answer.getDetails() + pool.getId());
        }

        assert (answer instanceof ModifyStoragePoolAnswer) : "Well, now why won't you actually return the ModifyStoragePoolAnswer when it's ModifyStoragePoolCommand? Pool=" + pool.getId() + "Host=" + hostId;

        s_logger.info("Connection established between " + pool + " host + " + hostId);
        return true;
    }

    @Override
    public boolean hostDisconnected(long hostId, long poolId) {
        StoragePoolHostVO storagePoolHost = storagePoolHostDao.findByPoolHost(
                poolId, hostId);

        if (storagePoolHost != null) {
            storagePoolHostDao.deleteStoragePoolHostDetails(hostId, poolId);
        }
        return false;
    }

}
