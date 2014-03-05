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
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.lifecycle.ElastistorPrimaryDataStoreLifeCycle;
import org.apache.cloudstack.storage.datastore.response.UpdateFileSystemCmdResponse;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil.ElastistorStoragePoolType;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.ResizeVolumeAnswer;
import com.cloud.agent.api.storage.ResizeVolumeCommand;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.ResizeVolumePayload;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
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
public class ElastistorPrimaryDataStoreDriver  extends CloudStackPrimaryDataStoreDriverImpl implements PrimaryDataStoreDriver{

    private static final Logger s_logger = Logger.getLogger(ElastistorPrimaryDataStoreDriver.class);
    
    @Inject
    private PrimaryDataStoreDao _storagePoolDao;
    @Inject
    private StoragePoolDetailsDao _storagePoolDetailsDao;
    @Inject
    private VolumeDao _volumeDao;   
    @Inject
    private AccountDetailsDao _accountDetailsDao;
    @Inject
    private VolumeDetailsDao _volumeDetailsDao;
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

    @Override
    public void createAsync(DataStore dataStore, DataObject dataObject, AsyncCompletionCallback<CreateCmdResult> callback) {
         super.createAsync(dataStore, dataObject, callback);
    /*  
        String iqn = null;
        String errMsg = null;
        
        if (dataObject.getType() == DataObjectType.VOLUME) {
            VolumeInfo volumeInfo = (VolumeInfo)dataObject;
            long storagePoolId = dataStore.getId();
            VolumeVO volume = this._volumeDao.findById(volumeInfo.getId());
            StoragePoolVO  storagePool = (StoragePoolVO ) this._storagePoolDao.findById(dataStore.getId());
            
        
            try {
                volume = ElastistorUtil.createElastistorVolume(storagePool,volume, ElastistorUtil.ES_IP_VAL, ElastistorUtil.ES_API_KEY_VAL);
            } 
            catch (Throwable e) {
                e.printStackTrace();
            }       
        
            volume.setPoolType(StoragePoolType.IscsiLUN);
            volume.setPoolId(storagePoolId);
        
            _volumeDao.update(volume.getId(), volume);
            

            long capacityBytes = storagePool.getCapacityBytes();
            long usedBytes = storagePool.getUsedBytes();

            usedBytes += volume.getSize();

            storagePool.setUsedBytes(usedBytes > capacityBytes ? capacityBytes : usedBytes);

            _storagePoolDao.update(storagePoolId, storagePool);
        }else {
            errMsg = "Invalid DataObjectType (" + dataObject.getType() + ") passed to createAsync";
        }
        
        CreateCmdResult result = new CreateCmdResult(iqn, new Answer(null, errMsg == null, errMsg));

        result.setResult(errMsg);

        callback.complete(result);
        
        */    
    }



    /**
     * This will delete the volume created at elastistor & update the
     * corresponding information at cloudstack. This should be invoked only when
     * a DataDisk is not attached to any VM i.e. DataDisk should be in detached
     * state.
     */
    @Override
    public void deleteAsync(DataStore dataStore, DataObject dataObject, AsyncCompletionCallback<CommandResult> callback) {
       
        super.deleteAsync(dataStore, dataObject, callback);
        
        /*String errMsg = null;

        if (dataObject.getType() == DataObjectType.VOLUME) {

            VolumeInfo csVolumeInfo = (VolumeInfo) dataObject;
            long storagePoolId = dataStore.getId();

            ElastistorConnectionInfo esConnectionInfo = getEsConnectionInfo(storagePoolId);

            deleteElastistorVolume(csVolumeInfo, esConnectionInfo);

            _volumeDao.deleteVolumesByInstance(csVolumeInfo.getId());

            // delete the info in the volume_details table that's related to the
            // elastistor volume
            _volumeDetailsDao.deleteDetails(csVolumeInfo.getId());

            StoragePoolVO storagePool = _storagePoolDao.findById(storagePoolId);

            long usedBytes = storagePool.getUsedBytes();

            usedBytes -= csVolumeInfo.getSize();

            storagePool.setUsedBytes(usedBytes < 0 ? 0 : usedBytes);

            _storagePoolDao.update(storagePoolId, storagePool);
        } else {
            errMsg = "Invalid DataObjectType (" + dataObject.getType() + ") passed to deleteAsync";
        }

        CommandResult result = new CommandResult();

        result.setResult(errMsg);

        callback.complete(result); */
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
       

        VolumeObject vol = (VolumeObject) data;
        StoragePool pool = (StoragePool) data.getDataStore();
                             vol.getDiskOfferingId();
        ResizeVolumePayload resizeParameter = (ResizeVolumePayload) vol.getpayload();
       
        CreateCmdResult result = new CreateCmdResult(null, null);
        
        
        
        // added by punith.cloudbyte
        
        
        /***
         * 
         *     POINT TO BE NOTED ..........i'm using resizeParameter.newSize as a parameter for IOPS
         * 
         */
        
        int changeVolumeIops = 0;
        
        if( resizeParameter.newSize != null && resizeParameter.instanceName.equals("changevolumeiops") ){
        
            changeVolumeIops = 1;
            System.out.println(" New iops is :" + resizeParameter.newSize);
            
        }      
        
        
        switch (changeVolumeIops) {
        
        
        
        case 1:
            
            
              s_logger.info(" update elastistor volume QOS started");
              
                
               
                
                try {
                    
                    
                    Answer answer = ElastistorUtil.updateElastistorVolumeQosGroup(vol, pool, resizeParameter.newSize);
                
                    
                    if(answer != null && answer.getResult())
                        {

                             StoragePoolVO poolVO = _storagePoolDao.findById(pool.getId());
                             
                               poolVO.setCapacityIops(resizeParameter.newSize);
                               _storagePoolDao.update(pool.getId(), poolVO);
                               
                              DiskOfferingVO diskOfferingVO = _diskOfferingDao.findById(vol.getDiskOfferingId());
                              diskOfferingVO.setMaxIops(resizeParameter.newSize);
                              _diskOfferingDao.update(vol.getDiskOfferingId(), diskOfferingVO);
                              
                              vol.getVolume().setMaxIops(resizeParameter.newSize);
                              vol.update();
                              
                                s_logger.info(" update elastistor volume QOS COMPLETE");
                                
                                callback.complete(result);
                                
                                  break;
                        }
                    else if (answer != null){
                        result.setResult(answer.getDetails());
                        s_logger.info(answer.getDetails());
                        
                        System.out.println(answer.getDetails());
                        
                        callback.complete(result);
                        
                          break;
                    }
                    else{
                            s_logger.info(" update elastistor volume QOS INCOMPLETE");
                    
                            result.setResult("update elastistor volume QOS INCOMPLETE");
                            
                            callback.complete(result);
                            
                              break;
                            
                        }
                  
                } catch (Throwable e1) {
                    s_logger.debug("update Elastistor volume QOS failed , please contact elastistor admin ", e1);
                    e1.printStackTrace();
                    result.setResult(e1.toString());
                }               
                
                  callback.complete(result);
                
                  break;
                        
                
        default:    
            
                s_logger.info(" update elastistor volume SIZE started");                
                Boolean status3 = false;
                
                try {
                    
                    
                    status3 = ElastistorUtil.updateElastistorVolumeSize(vol, pool, resizeParameter.newSize);
                
                  
                } catch (Throwable e1) {
                    s_logger.debug("update Elastistor volume failed , please contact elastistor admin ", e1);
                    e1.printStackTrace();
                }
                
                if(status3){
                s_logger.info(" update elastistor volume SIZE COMPLETE");
                }
                else{
                    s_logger.info(" update elastistor volume SIZE INCOMPLETE");
                    
                    callback.complete(result);
                     break;
                }

                ResizeVolumeCommand resizeCmd = new ResizeVolumeCommand(vol.getPath(), new StorageFilerTO(pool), vol.getSize(),
                        resizeParameter.newSize, resizeParameter.shrinkOk, resizeParameter.instanceName);
                
                try {
                    ResizeVolumeAnswer answer = (ResizeVolumeAnswer) storageMgr.sendToPool(pool, resizeParameter.hosts, resizeCmd);
                    if (answer != null && answer.getResult()) {
                        long finalSize = answer.getNewSize();
                        s_logger.debug("Resize: volume started at size " + vol.getSize() + " and ended at size " + finalSize);
                       
                      
                        
                       // added by punith.cloudbyte 
                       StoragePoolVO poolVO2 = _storagePoolDao.findById(pool.getId());
                        
                       poolVO2.setCapacityBytes(finalSize);
                       poolVO2.setCapacityIops(resizeParameter.newSize);
                       _storagePoolDao.update(pool.getId(), poolVO2);
                       
                       DiskOfferingVO diskOfferingVO2 = _diskOfferingDao.findById(vol.getDiskOfferingId());
                          diskOfferingVO2.setMaxIops(resizeParameter.newSize);
                          _diskOfferingDao.update(vol.getDiskOfferingId(), diskOfferingVO2);
                       
                        vol.setSize(finalSize);                                                                     
                        vol.getVolume().setMaxIops(resizeParameter.newSize);
                        vol.update();
                        
                    } else if (answer != null) {
                        result.setResult(answer.getDetails());
                    } else {
                        s_logger.debug("return a null answer, mark it as failed for unknown reason");
                        result.setResult("return a null answer, mark it as failed for unknown reason");
                    }

                } catch (Exception e) {
                    s_logger.debug("sending resize command failed", e);
                    result.setResult(e.toString());
                }

                callback.complete(result);
            
                break;
            
             }
            
                
                
                
                

        
            
            
        
    
        
    }

    @Override
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

        storagePool.setUsedBytes(usedBytes > capacityBytes ? capacityBytes : usedBytes);

        _storagePoolDao.update(storagePoolId, storagePool);
    }


}
