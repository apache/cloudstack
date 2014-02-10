package org.apache.cloudstack.storage.datastore.driver;

import static org.apache.cloudstack.storage.datastore.util.ElastistorUtil.ELASTISTOR_ACCOUNT_MISSING;
import java.util.HashMap;
import java.util.Map;

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
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil.ElastistorAccount;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil.ElastistorConnectionInfo;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil.ElastistorFilesystem;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.AccountManager;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * The implementation class for <code>PrimaryDataStoreDriver</code>. This
 * directs the public interface methods to use CloudByte's Elastistor based
 * volumes.
 *
 * @author amit.das@cloudbyte.com
 * @author punith.s@cloudbyte.com
 *
 */
public class ElastistorPrimaryDataStoreDriver extends
        CloudStackPrimaryDataStoreDriverImpl implements PrimaryDataStoreDriver {

    @Inject
    private PrimaryDataStoreDao _storagePoolDao;
    @Inject
    private VolumeDao _volumeDao;
    @Inject
    private AccountDetailsDao _accountDetailsDao;
    @Inject
    AccountManager _accountMgr;

    @Override
    public DataTO getTO(DataObject data) {
        return null;
    }

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        return null;
    }

    @Override
    public void createAsync(DataStore dataStore, DataObject dataObject,AsyncCompletionCallback<CreateCmdResult> callback) {
        // super.createAsync(dataStore, dataObject, callback);

        String iqn = null;
        String errMsg = null;

        if (dataObject.getType() == DataObjectType.VOLUME) {
            VolumeInfo volumeInfo = (VolumeInfo) dataObject;
            long storagePoolId = dataStore.getId();
            VolumeVO volume = this._volumeDao.findById(volumeInfo.getId());
            StoragePoolVO storagePool = (StoragePoolVO) this._storagePoolDao
                    .findById(dataStore.getId());

            try {
                volume = ElastistorUtil.createElastistorVolume(storagePool,
                        volume, ElastistorUtil.esip,
                        ElastistorUtil.esapikey);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            volume.setPoolType(StoragePoolType.IscsiLUN);
            volume.setPoolId(storagePoolId);

            _volumeDao.update(volume.getId(), volume);

            long capacityBytes = storagePool.getCapacityBytes();
            long usedBytes = storagePool.getUsedBytes();

            usedBytes += volume.getSize();

            storagePool.setUsedBytes(usedBytes > capacityBytes ? capacityBytes
                    : usedBytes);

            _storagePoolDao.update(storagePoolId, storagePool);
        } else {
            errMsg = "Invalid DataObjectType (" + dataObject.getType()
                    + ") passed to createAsync";
        }

        CreateCmdResult result = new CreateCmdResult(iqn, new Answer(null,
                errMsg == null, errMsg));

        result.setResult(errMsg);

        callback.complete(result);

    }

    private void accountSync(Account csAccount, ElastistorAccount esAccount) {

        long csAccountId = csAccount.getAccountId();

        // update the account created at elastistor in cloudstack database
        Map<String, String> accountDetails = new HashMap<String, String>();
        accountDetails.put(ElastistorUtil.ES_ACCOUNT_ID,
                String.valueOf(esAccount.getId()));
        accountDetails.put(ElastistorUtil.ES_ACCOUNT_NAME, esAccount.getName());

        // persist or update
        _accountDetailsDao.update(csAccountId, accountDetails);
    }

    /**
     * This will delete the volume created at elastistor & update the
     * corresponding information at cloudstack. This should be invoked only when
     * a DataDisk is not attached to any VM i.e. DataDisk should be in detached
     * state.
     */
    @Override
    public void deleteAsync(DataStore dataStore, DataObject dataObject,
            AsyncCompletionCallback<CommandResult> callback) {
    }

    @Override
    public void copyAsync(DataObject srcdata, DataObject destData,
            AsyncCompletionCallback<CopyCommandResult> callback) {
        throw new UnsupportedOperationException();

    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        return false;
    }

    @Override
    public void resize(DataObject data,
            AsyncCompletionCallback<CreateCmdResult> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChapInfo getChapInfo(VolumeInfo volumeInfo) {
        return super.getChapInfo(volumeInfo);
    }

    @Override
    public void takeSnapshot(SnapshotInfo snapshot,
            AsyncCompletionCallback<CreateCmdResult> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void revertSnapshot(SnapshotInfo snapshot,
            AsyncCompletionCallback<CommandResult> callback) {
        throw new UnsupportedOperationException();
    }

    private ElastistorConnectionInfo getEsConnectionInfo(long storagePoolId) {
        return null;
    }

    /**
     * This synchronizes account info retrieved from elastistore into
     * cloudstack.
     *
     * @param csAccountId
     *            is understood in cloudstack
     * @param esAccountName
     *            is understood in elastistor
     * @param esConnectionInfo
     *            has the details for making an elastistor connection
     * @return
     */
    private ElastistorAccount checkElastistorAccount(Account csAccount,ElastistorConnectionInfo esConnectionInfo) {

        // both elastistor & cloudstack should have same account name
        String esAccountName = csAccount.getAccountName();

        // Check if an elastistor account exists? Make a REST Api call to
        // elastistor in order to do so
        ElastistorAccount esAccount = ElastistorUtil
                .getElastistorAccountByName(esConnectionInfo, esAccountName);

        if (null == esAccount || null == esAccount.getName()) {
            // Account needs to exist in elastistor prior to making this call
            throw new CloudRuntimeException(String.format(
                    ELASTISTOR_ACCOUNT_MISSING, esAccountName));
        }

        // finally return the created account
        return esAccount;
    }

    /**
     * This synchronizes elastistor volume information into cloudstack.
     *
     * @param csVolumeInfo
     *            represents the current cloudstack volume info
     * @param esVolume
     *            represents the volume info of elastistor
     * @param dataStore
     *            represents the primary datastore of cloudstack
     */
    private void volumeSync(VolumeInfo csVolumeInfo,
            ElastistorFilesystem esFilesystem, DataStore dataStore) {
    }

    /**
     * This updates the PrimaryDataStore with the updated volume information
     *
     * @param csVolumeInfo
     *            the updated cloudstack volume
     * @param dataStore
     *            the PrimaryDataStore to be updated
     */
    private void updatePrimaryStore(VolumeInfo csVolumeInfo, DataStore dataStore) {

        long storagePoolId = dataStore.getId();
        StoragePoolVO storagePool = _storagePoolDao.findById(storagePoolId);

        long capacityBytes = storagePool.getCapacityBytes();
        long usedBytes = storagePool.getUsedBytes();

        usedBytes += csVolumeInfo.getSize();

        storagePool.setUsedBytes(usedBytes > capacityBytes ? capacityBytes
                : usedBytes);

        _storagePoolDao.update(storagePoolId, storagePool);
    }

    private void deleteElastistorVolume(VolumeInfo csVolumeInfo,
            ElastistorConnectionInfo esConnectionInfo) {
    }
}
