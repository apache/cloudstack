package com.cloud.storage.orchestra; 
import com.cloud.storage.storageCore.DataStore;
import com.cloud.storage.storageCore.DataStore.StoreType;
import com.cloud.storage.storageCore.Snapshot;
import com.cloud.storage.storageCore.StorageCore;
import com.cloud.storage.storageCore.Template;
import com.cloud.storage.storageCore.Volume;
  import com.cloud.utils.component.Inject;
  import com.cloud.utils.exception.CloudRuntimeException;
  
public class StorageOrchestraEngineImpl implements StorageOrchestraEngine {
  	@Inject
	StorageCore _storeCore;
  
  	@Override
 	public Volume createVolume(Volume volume, DataStore storage) {
 		// TODO Auto-generated method stub
  		
 		if (_storeCore.exists(volume)) 
 			return volume;
  		
 		if (volume.getTemplateId() != null) {
 			//copy template to storage at first
 			Template template = null; // get template from volume.gettemplateid
 			Volume baseVolume = (Volume)_storeCore.copy(template, template.getStore(), storage);
 			return (Volume)_storeCore.copy(baseVolume, volume, baseVolume.getStore(), storage);
 		} else {
 			return (Volume)_storeCore.create(volume, storage);
  		}
  	}
  
  	@Override
 	public Volume createVolumeFromSnapshot(Volume volume, Snapshot snapshot) {
 		return (Volume)_storeCore.copy(snapshot, volume, snapshot.getStore(), volume.getStore());
  	}
  
  	@Override
 	public Volume copyVolume(Volume srcVol, DataStore destPool) {
 		if (!_storeCore.exists(srcVol)) {
 			throw new CloudRuntimeException("volume: " + srcVol + " doesn't exist");
 		}
 		
 		Volume vol = (Volume)_storeCore.copy(srcVol, srcVol.getStore(), destPool);
 		if (vol != null) {
 			return vol;
 		}
 		
 		//If failed, try another one
 		if (destPool.getType() != StoreType.Backup) {
 			DataStore backupStore = _storeCore.getStore(StoreType.Backup, destPool.getScope());
 			Volume tempVol = (Volume)_storeCore.copy(srcVol, srcVol.getStore(), backupStore);
 			Volume destVol = (Volume)_storeCore.copy(tempVol, tempVol.getStore(), destPool);
			return destVol;
 		}
 		return null;
  	}
  
  	@Override
 	public Volume moveVolume(Volume srcVol, DataStore destPool) {
 		Volume destVol = (Volume)this.copyVolume(srcVol, destPool);
 		_storeCore.delete(srcVol, srcVol.getStore());
 		return destVol;
  	}
  
  	@Override
 	public Volume createVolumeFromTemplate(Volume volume, Template template) {
 		
  		// TODO Auto-generated method stub
  		return null;
  	}
  
  	@Override
 	public Template createTemplateFromVolume(Template template, Volume volume) {
 		// TODO Auto-generated method stub
 		return null;
  	}
}