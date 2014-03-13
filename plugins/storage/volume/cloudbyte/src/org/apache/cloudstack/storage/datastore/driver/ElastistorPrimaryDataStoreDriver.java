package org.apache.cloudstack.storage.datastore.driver;


import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.log4j.Logger;

import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.user.AccountManager;

/**
 * The implementation class for <code>PrimaryDataStoreDriver</code>. This
 * directs the public interface methods to use CloudByte's Elastistor based
 * volumes.
 *
 * @author amit.das@cloudbyte.com
 * @author punith.s@cloudbyte.com
 *
 */
public class ElastistorPrimaryDataStoreDriver  extends CloudStackPrimaryDataStoreDriverImpl implements PrimaryDataStoreDriver{

    private static final Logger s_logger = Logger.getLogger(ElastistorPrimaryDataStoreDriver.class);

    @Inject
    AccountManager _accountMgr;
    @Inject
    DiskOfferingDao _diskOfferingDao;
    @Override
    public DataTO getTO(DataObject data) {
        return null;
    }

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        return null;
    }

    public void createAsync(DataStore dataStore, DataObject dataObject, AsyncCompletionCallback<CreateCmdResult> callback) {
         super.createAsync(dataStore, dataObject, callback);
    }

    public void deleteAsync(DataStore dataStore, DataObject dataObject, AsyncCompletionCallback<CommandResult> callback) {
         super.deleteAsync(dataStore, dataObject, callback);
    }

    @Override
    public void copyAsync(DataObject srcdata, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {
        throw new UnsupportedOperationException();

    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        return false;
    }

    @Override
    public void resize(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
        throw new UnsupportedOperationException();
    }

    public ChapInfo getChapInfo(VolumeInfo volumeInfo) {
        return super.getChapInfo(volumeInfo);
    }

    @Override
    public void takeSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CreateCmdResult> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void revertSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CommandResult> callback) {
        throw new UnsupportedOperationException();
    }

}
